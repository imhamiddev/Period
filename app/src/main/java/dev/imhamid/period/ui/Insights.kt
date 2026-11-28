package dev.imhamid.period.ui

import dev.imhamid.period.data.Snapshot
import dev.imhamid.period.engine.CycleEngine
import dev.imhamid.period.engine.toEngine
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class Insight(val id: String, val title: String, val text: String)

/** A symptom/mood and how often it showed up in each of the four phases (menstrual, follicular, ovulation, luteal). */
class PatternRow(val id: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val perPhase: List<Int>) {
    val total get() = perPhase.sum()
    val topPhase: Int? get() = perPhase.indices.maxByOrNull { perPhase[it] }?.takeIf { total > 0 && perPhase[it] * 2 >= total }
}

/** Soft health hints. Nothing here is a diagnosis; wording always points to a doctor. */
object Insights {
    private const val ADVICE = " این تشخیص پزشکی نیست؛ اگه نگرانی، با پزشک یا ماما مشورت کن."

    fun cycleGaps(logs: Collection<LocalDate>): List<Int> =
        CycleEngine.derivePeriods(logs).sortedBy { it.start }.zipWithNext { a, b -> ChronoUnit.DAYS.between(a.start, b.start).toInt() }.filter { it in 10..90 }

    fun flags(s: Snapshot, today: LocalDate): List<Insight> {
        val periods = CycleEngine.derivePeriods(s.logs).sortedBy { it.start }
        if (periods.isEmpty()) return emptyList()
        val out = mutableListOf<Insight>()
        val gaps = cycleGaps(s.logs)
        val lastStart = periods.last().start

        gaps.lastOrNull()?.let { g ->
            if (g < 21) out += Insight("short-$lastStart", "چرخه‌ی کوتاه", "آخرین چرخه‌ات ${Fa.n(g)} روز بود که کوتاه‌تر از ۲۱ روزه.$ADVICE")
            if (g > 35) out += Insight("long-$lastStart", "چرخه‌ی طولانی", "آخرین چرخه‌ات ${Fa.n(g)} روز بود که بلندتر از ۳۵ روزه.$ADVICE")
        }
        val recent = gaps.takeLast(6)
        if (recent.size >= 3 && recent.max() - recent.min() > 9) {
            out += Insight("irregular-$lastStart", "چرخه‌ی نامنظم", "طول چرخه‌هات در چند ماه اخیر بین ${Fa.n(recent.min())} تا ${Fa.n(recent.max())} روز تغییر کرده.$ADVICE")
        }
        periods.takeLast(3).firstOrNull { it.length > 7 }?.let {
            out += Insight("longperiod-${it.start}", "پریود طولانی", "یکی از پریودهای اخیرت ${Fa.n(it.length)} روز طول کشیده که بیشتر از ۷ روزه.$ADVICE")
        }
        val late = s.owner?.toEngine(today, s.logs)?.overview()?.lateDays ?: 0
        if (late >= 7) out += Insight("late-$lastStart", "تأخیر پریود", "پریودت ${Fa.n(late)} روز دیر کرده. اگه امکان بارداری هست آزمایش بده.$ADVICE")
        return out
    }

    /**
     * "Three over six" rule: three days in a row at least 0.2 °C above the highest of the six readings before them.
     * Returns the estimated ovulation day (the day before the rise) found in the last [windowDays] days.
     */
    fun bbtOvulation(s: Snapshot, today: LocalDate, windowDays: Int = 45): LocalDate? {
        val temps = s.health.entries
            .filter { it.value.temperatureC != null && !it.key.isBefore(today.minusDays(windowDays.toLong())) && !it.key.isAfter(today) }
            .sortedBy { it.key }
            .map { it.key to it.value.temperatureC!! }
        if (temps.size < 9) return null
        var found: LocalDate? = null
        for (i in 6..temps.size - 3) {
            val base = temps.subList(i - 6, i).maxOf { it.second }
            val ok = (0..2).all { temps[i + it].second >= base + 0.2 }
            val span = ChronoUnit.DAYS.between(temps[i - 6].first, temps[i + 2].first)
            if (ok && span <= 14) found = temps[i].first.minusDays(1)
        }
        return found
    }

    /** Which phase each symptom / mood usually appears in (last [days] days). */
    fun patterns(s: Snapshot, today: LocalDate, days: Int = 180, limit: Int = 5): List<PatternRow> {
        val engine = s.owner?.toEngine(today, s.logs) ?: return emptyList()
        val since = today.minusDays(days.toLong())
        val sym = HashMap<String, IntArray>()
        val mood = HashMap<String, IntArray>()
        s.days.values.forEach { dl ->
            val d = runCatching { LocalDate.parse(dl.day) }.getOrNull() ?: return@forEach
            if (d.isBefore(since) || d.isAfter(today)) return@forEach
            val ph = engine.phaseOn(d)?.let { Content.phaseIndex(it) } ?: return@forEach
            Fa.realSymptoms(dl.symptoms).forEach { sym.getOrPut(it) { IntArray(4) }[ph]++ }
            dl.moods.forEach { mood.getOrPut(it) { IntArray(4) }[ph]++ }
        }
        val rows = sym.map { (id, a) -> Catalog.symptom(id)?.let { PatternRow(id, it.label, it.icon, a.toList()) } } +
            mood.map { (id, a) -> Catalog.mood(id)?.let { PatternRow(id, it.label, it.icon, a.toList()) } }
        return rows.filterNotNull().filter { it.total >= 3 }.sortedByDescending { it.total }.take(limit)
    }
}
