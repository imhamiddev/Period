package dev.imhamid.period.engine

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Cycle engine — a Kotlin port of the model used by Bloomery (krugerhomeassistant/Bloomery).
 *  - bleeding runs with gaps <= 2 days are merged into one period
 *  - next cycle length = recency-weighted mean of the last 6 valid cycles (15..90 d),
 *    blended with the configured default while fewer than 3 cycles are known
 *  - ovulation = next_start - luteal (never before the end of a typical period)
 *  - fertile window = ovulation-5 .. ovulation+1
 *  - extra: PMS window = the last [pmsDays] days of the cycle
 */
enum class Phase { MENSTRUAL, FOLLICULAR, FERTILE, OVULATION, LUTEAL, PMS }

enum class EventType { PMS_TOMORROW, PMS_STARTS, PERIOD_SOON, PERIOD_TODAY, OVULATION }

data class Period(val start: LocalDate, val end: LocalDate) {
    val length: Int get() = ChronoUnit.DAYS.between(start, end).toInt() + 1
}

data class Segment(
    val start: LocalDate,
    val length: Int,
    val periodEnd: LocalDate,
    val ovulation: LocalDate,
    val predicted: Boolean,
) {
    val end: LocalDate get() = start.plusDays(length - 1L)
    val fertileStart: LocalDate get() = maxOf(ovulation.minusDays(5), periodEnd.plusDays(1))
    val fertileEnd: LocalDate get() = ovulation.plusDays(1)
}

data class Overview(
    val hasData: Boolean,
    val phase: Phase?,
    val cycleDay: Int?,
    val periodDay: Int?,
    val cycleLength: Int,
    val nextPeriod: LocalDate?,
    val daysToPeriod: Int?,
    val pmsStart: LocalDate?,
    val daysToPms: Int?,
    val inPms: Boolean,
    val ovulation: LocalDate?,
    val daysToOvulation: Int?,
    val lateDays: Int,
    val variability: Int,
)

class CycleEngine(
    val today: LocalDate,
    bleeding: Collection<LocalDate>,
    private val defaultCycle: Int = 28,
    private val defaultPeriod: Int = 5,
    private val luteal: Int = 14,
    val pmsDays: Int = 5,
) {
    val bleedingSet: Set<LocalDate> = bleeding.toSet()
    val periods: List<Period> = derivePeriods(bleedingSet)
    val cycleLengths: List<Int> = periods.zipWithNext { a, b -> ChronoUnit.DAYS.between(a.start, b.start).toInt() }
        .filter { it in MIN_CYCLE..MAX_CYCLE }
    val predictedCycle: Int = blend(cycleLengths, defaultCycle)
    val predictedPeriod: Int = max(1, min(blend(periods.filter { it.end < today }.map { it.length }, defaultPeriod), 12))
    val variability: Int = if (cycleLengths.size >= 2) {
        val v = cycleLengths.takeLast(6)
        val mean = v.average()
        max(1, min(7, ceil(sqrt(v.sumOf { (it - mean).pow(2) } / v.size)).toInt()))
    } else 2
    val segments: List<Segment> = buildSegments()

    private fun ovulationFor(start: LocalDate, length: Int): LocalDate {
        val a = start.plusDays((length - luteal).toLong())
        val b = start.plusDays(predictedPeriod.toLong())
        return if (a.isAfter(b)) a else b
    }

    private fun buildSegments(): List<Segment> {
        val out = mutableListOf<Segment>()
        periods.forEachIndexed { i, p ->
            if (i + 1 < periods.size) {
                val len = ChronoUnit.DAYS.between(p.start, periods[i + 1].start).toInt()
                out += Segment(p.start, len, p.end, ovulationFor(p.start, len), false)
            } else {
                val len = max(predictedCycle, ChronoUnit.DAYS.between(p.start, today).toInt() + 1)
                val pEnd = if (p.end >= today) maxOf(p.end, p.start.plusDays(predictedPeriod - 1L)) else p.end
                out += Segment(p.start, len, pEnd, ovulationFor(p.start, predictedCycle), false)
            }
        }
        if (out.isNotEmpty()) {
            var next = out.last().end.plusDays(1)
            repeat(12) {
                out += Segment(next, predictedCycle, next.plusDays(predictedPeriod - 1L), ovulationFor(next, predictedCycle), true)
                next = next.plusDays(predictedCycle.toLong())
            }
        }
        return out
    }

    val nextPeriod: LocalDate? get() = segments.firstOrNull { it.predicted }?.start

    fun segmentFor(d: LocalDate): Segment? = segments.firstOrNull { d >= it.start && d <= it.end }

    fun phaseOn(d: LocalDate): Phase? {
        val s = segmentFor(d) ?: return null
        return when {
            d <= s.periodEnd -> Phase.MENSTRUAL
            d == s.ovulation -> Phase.OVULATION
            d >= s.fertileStart && d <= s.fertileEnd -> Phase.FERTILE
            d >= s.end.minusDays(pmsDays - 1L) -> Phase.PMS
            d < s.ovulation -> Phase.FOLLICULAR
            else -> Phase.LUTEAL
        }
    }

    fun isLogged(d: LocalDate) = d in bleedingSet

    fun isPredictedPeriod(d: LocalDate): Boolean {
        val s = segmentFor(d) ?: return false
        return d <= s.periodEnd && d !in bleedingSet && (s.predicted || d > today)
    }

    private fun upcomingOvulation(): LocalDate? = segments.map { it.ovulation }.firstOrNull { it >= today }

    fun overview(): Overview {
        if (periods.isEmpty()) {
            return Overview(false, null, null, null, predictedCycle, null, null, null, null, false, null, null, 0, variability)
        }
        val seg = segmentFor(today)
        val np = nextPeriod
        val pms = np?.minusDays(pmsDays.toLong())
        val ov = upcomingOvulation()
        val lastStart = periods.last().start
        val late = ChronoUnit.DAYS.between(lastStart, today).toInt() - predictedCycle
        val phase = phaseOn(today)
        val periodDay = if (phase == Phase.MENSTRUAL && seg != null && !seg.predicted)
            ChronoUnit.DAYS.between(seg.start, today).toInt() + 1 else null
        return Overview(
            hasData = true,
            phase = phase,
            cycleDay = seg?.let { ChronoUnit.DAYS.between(it.start, today).toInt() + 1 },
            periodDay = periodDay,
            cycleLength = predictedCycle,
            nextPeriod = np,
            daysToPeriod = np?.let { ChronoUnit.DAYS.between(today, it).toInt() },
            pmsStart = pms,
            daysToPms = pms?.let { ChronoUnit.DAYS.between(today, it).toInt() },
            inPms = phase == Phase.PMS,
            ovulation = ov,
            daysToOvulation = ov?.let { ChronoUnit.DAYS.between(today, it).toInt() },
            lateDays = max(0, late),
            variability = variability,
        )
    }

    /** Events that fall exactly on [today] (used by the notification worker). */
    fun dueEvents(): List<EventType> {
        if (periods.isEmpty()) return emptyList()
        val o = overview()
        val out = mutableListOf<EventType>()
        o.daysToPms?.let { if (it == 1) out += EventType.PMS_TOMORROW; if (it == 0) out += EventType.PMS_STARTS }
        o.daysToPeriod?.let { if (it == 2) out += EventType.PERIOD_SOON; if (it == 0) out += EventType.PERIOD_TODAY }
        if (o.daysToOvulation == 0) out += EventType.OVULATION
        return out
    }

    companion object {
        const val MERGE_GAP = 2
        const val MIN_CYCLE = 15
        const val MAX_CYCLE = 90

        fun derivePeriods(days: Collection<LocalDate>): List<Period> {
            val out = mutableListOf<Period>()
            for (d in days.sorted()) {
                val last = out.lastOrNull()
                if (last != null && ChronoUnit.DAYS.between(last.end, d) <= MERGE_GAP + 1) {
                    out[out.size - 1] = last.copy(end = d)
                } else out += Period(d, d)
            }
            return out
        }

        fun blend(values: List<Int>, prior: Int): Int {
            val v = values.takeLast(6)
            if (v.isEmpty()) return prior
            var num = 0.0
            var den = 0.0
            v.forEachIndexed { i, x -> num += x * (i + 1); den += (i + 1) }
            if (v.size < 3) { num += prior * 2; den += 2 }
            return Math.round(num / den).toInt()
        }
    }
}
