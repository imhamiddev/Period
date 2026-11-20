package dev.imhamid.period.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.ui.text.style.TextAlign
import dev.imhamid.period.data.Profile
import dev.imhamid.period.engine.CycleEngine
import dev.imhamid.period.engine.Period
import dev.imhamid.period.engine.toEngine
import dev.imhamid.period.engine.Phase
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

@Composable
fun StatsTab(vm: AppVM) {
    val s = vm.snap ?: return
    val isHer = s.me.role == "her"
    val owner = s.owner
    val today = rememberToday()
    val engine = remember(s.logs, owner, today) {
        owner?.toEngine(today, s.logs)
    }
    val ownerName = owner?.name?.takeIf { it.isNotBlank() } ?: "پارتنرت"
    var range by rememberSaveable { mutableIntStateOf(6) }
    var showAll by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(C.Paper).statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(20.dp))
        Reveal(0) { Txt("آمار و تاریخچه", 32.sp, FontWeight.Light) }
        Spacer(Modifier.height(18.dp))
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (engine == null || engine.periods.isEmpty()) {
                Reveal(1) { Card { Txt("بعد از ثبت چند پریود، نمودارها و آمار اینجا نمایش داده می‌شن.", 15.sp, color = C.Muted) } }
            } else {
                val allCycles = engine.periods.zipWithNext { a, b -> a.start to ChronoUnit.DAYS.between(a.start, b.start).toInt() }
                    .filter { it.second in CycleEngine.MIN_CYCLE..CycleEngine.MAX_CYCLE }
                val lens = allCycles.map { it.second }
                val cycles = allCycles.takeLast(range)
                val periods = engine.periods.takeLast(range)
                val avgCycle = engine.predictedCycle
                val regText = when {
                    lens.size < 3 -> "—"
                    engine.variability <= 2 -> "منظم"
                    engine.variability <= 4 -> "نسبتاً منظم"
                    else -> "نامنظم"
                }

                // ---- summary tiles
                Reveal(1) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatTile("میانگین چرخه", if (lens.isEmpty()) "—" else Fa.n(avgCycle), "روز", C.Green)
                            StatTile("میانگین پریود", Fa.n(engine.predictedPeriod), "روز", C.Coral)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatTile("نظم چرخه", regText, if (lens.size < 3) "داده‌ی بیشتر لازمه" else "±${Fa.n(engine.variability)} روز", C.Lime)
                            StatTile("پریود ثبت‌شده", Fa.n(engine.periods.size), "بار", C.Amber)
                        }
                    }
                }

                // ---- cycle insights
                if (lens.isNotEmpty()) Reveal(2) {
                    Card {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Txt("نگاه کلی به چرخه‌ها", 14.sp, color = C.Muted)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                StatBlock("کوتاه‌ترین", "${Fa.n(lens.min())} روز")
                                StatBlock("بلندترین", "${Fa.n(lens.max())} روز")
                                StatBlock("آخرین چرخه", "${Fa.n(lens.last())} روز")
                            }
                            val diff = lens.last() - avgCycle
                            Txt(
                                when {
                                    lens.size < 2 -> "با ثبت چرخه‌های بیشتر، مقایسه‌ها دقیق‌تر می‌شن."
                                    diff == 0 -> "آخرین چرخه دقیقاً به اندازه‌ی میانگین بود."
                                    diff > 0 -> "آخرین چرخه ${Fa.n(diff)} روز بلندتر از میانگین بود."
                                    else -> "آخرین چرخه ${Fa.n(-diff)} روز کوتاه‌تر از میانگین بود."
                                },
                                13.sp, FontWeight.Medium, C.Green,
                            )
                        }
                    }
                }

                // ---- prediction vs reality
                val pred = remember(s.logs, owner) { if (owner == null) emptyList() else predictionHistory(owner, s.logs, engine.periods) }
                if (pred.isNotEmpty()) Reveal(2) {
                    Card {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            val recentPred = pred.takeLast(6)
                            val meanErr = recentPred.map { kotlin.math.abs(it.err) }.average()
                            val hit = recentPred.count { kotlin.math.abs(it.err) <= 2 }
                            Txt("پیش‌بینی در برابر واقعیت", 14.sp, color = C.Muted)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                StatBlock("میانگین خطا", "${Fa.n(meanErr.roundToInt())} روز")
                                StatBlock("درست (±۲ روز)", "${Fa.n(hit)} از ${Fa.n(recentPred.size)}")
                            }
                            recentPred.reversed().forEach { r ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Txt("واقعی: ${Fa.date(r.actual, "d MMMM")}", 14.sp, FontWeight.Medium)
                                        Txt("پیش‌بینی: ${Fa.date(r.predicted, "d MMMM")}", 12.sp, color = C.Muted)
                                    }
                                    val ok = kotlin.math.abs(r.err) <= 2
                                    Box(Modifier.clip(RoundedCornerShape(12.dp)).background(if (ok) C.LimeSoft else C.AmberSoft).padding(horizontal = 10.dp, vertical = 4.dp)) {
                                        Txt(
                                            when {
                                                r.err == 0 -> "دقیق"
                                                r.err > 0 -> "${Fa.n(r.err)} روز دیرتر"
                                                else -> "${Fa.n(-r.err)} روز زودتر"
                                            },
                                            12.sp, FontWeight.Medium, if (ok) C.OnSoft else C.Text,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ---- monthly summary
                var monthStart by rememberSaveable { mutableStateOf(Fa.jalaliMonth(today).first.toString()) }
                Reveal(3) {
                    val first = LocalDate.parse(monthStart)
                    val len = Fa.jalaliMonth(first).second
                    val last = first.plusDays(len - 1L)
                    val upTo = if (last.isAfter(today)) today else last
                    val inMonth = { d: LocalDate -> !d.isBefore(first) && !d.isAfter(last) }
                    val monthDays = s.days.values.filter { inMonth(LocalDate.parse(it.day)) }
                    val bleed = engine.bleedingSet.count { inMonth(it) }
                    val starts = engine.periods.count { inMonth(it.start) }
                    val moods = monthDays.mapNotNull { it.mood }
                    val topSym = monthDays.flatMap { Fa.realSymptoms(it.symptoms) }.groupingBy { it }.eachCount().maxByOrNull { it.value }
                    val notesN = s.notes.values.count { inMonth(LocalDate.parse(it.day)) }
                    val pmsDays = if (upTo.isBefore(first)) 0 else (0..ChronoUnit.DAYS.between(first, upTo).toInt()).count { engine.phaseOn(first.plusDays(it.toLong())) == Phase.PMS }
                    Card {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RoundIcon(Icons.Rounded.ChevronRight, C.Paper, C.Text, "ماه قبل") { monthStart = Fa.jalaliMonth(first.minusDays(1)).first.toString() }
                                Txt("خلاصه‌ی ${Fa.date(first, "MMMM y")}", 16.sp, FontWeight.Medium, align = TextAlign.Center, modifier = Modifier.weight(1f))
                                RoundIcon(Icons.Rounded.ChevronLeft, C.Paper, C.Text, "ماه بعد") {
                                    if (last.isBefore(today)) monthStart = last.plusDays(1).toString()
                                }
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                StatBlock("شروع پریود", Fa.n(starts))
                                StatBlock("روز خونریزی", Fa.n(bleed))
                                StatBlock("روز PMS", Fa.n(pmsDays))
                                StatBlock("یادداشت", Fa.n(notesN))
                            }
                            if (moods.isNotEmpty()) {
                                val avg = moods.average()
                                Txt("میانگین حال‌وهوا: ${Fa.moods[(avg.roundToInt() - 1).coerceIn(0, 4)]}  ${Fa.moodNames[(avg.roundToInt() - 1).coerceIn(0, 4)]}", 14.sp)
                            }
                            if (topSym != null) Txt("بیشترین علامت: ${Fa.symptomName(topSym.key)} (${Fa.n(topSym.value)} روز)", 14.sp)
                            if (moods.isEmpty() && topSym == null && bleed == 0) Txt("این ماه چیزی ثبت نشده.", 13.sp, color = C.Muted)
                        }
                    }
                }

                // ---- charts (with range filter)
                Reveal(3) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Txt("نمودارها", 13.sp, FontWeight.Medium, C.Muted, Modifier.padding(horizontal = 6.dp))
                        Spacer(Modifier.weight(1f))
                        Chip("۶ دوره‌ی اخیر", range == 6, { range = 6 })
                        Chip("۱۲ دوره‌ی اخیر", range == 12, { range = 12 })
                    }
                }
                if (cycles.isNotEmpty()) Reveal(3) {
                    Card {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Txt("طول چرخه‌ها (روز)", 14.sp, color = C.Muted)
                                Txt("میانگین ${Fa.n(cycles.map { it.second }.average().roundToInt())}", 13.sp, FontWeight.Medium, C.Green)
                            }
                            BarChart(cycles.map { it.second }, cycles.map { Fa.date(it.first, "d MMM") }, C.Green)
                        }
                    }
                }
                Reveal(3) {
                    Card {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Txt("طول پریودها (روز)", 14.sp, color = C.Muted)
                                Txt("میانگین ${Fa.n(periods.map { it.length }.average().roundToInt())}", 13.sp, FontWeight.Medium, C.Coral)
                            }
                            BarChart(periods.map { it.length }, periods.map { Fa.date(it.start, "d MMM") }, C.Coral)
                        }
                    }
                }

                // ---- logging activity (last 30 days)
                val since = today.minusDays(29)
                val loggedDays = s.days.values.count { dl ->
                    LocalDate.parse(dl.day) >= since && (dl.mood != null || dl.energy != null || dl.symptoms.isNotEmpty())
                }
                val bleedDays = engine.bleedingSet.count { it >= since }
                val noteCount = s.notes.values.count { LocalDate.parse(it.day) >= since }
                Reveal(4) {
                    Card {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Txt("۳۰ روز اخیر", 14.sp, color = C.Muted)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                StatBlock("روز خونریزی", Fa.n(bleedDays))
                                StatBlock("روز ثبت‌شده", Fa.n(loggedDays))
                                StatBlock("یادداشت", Fa.n(noteCount))
                            }
                        }
                    }
                }

                // ---- mood
                val moodCounts = s.days.values.mapNotNull { it.mood }.groupingBy { it }.eachCount()
                if (moodCounts.isNotEmpty()) Reveal(5) {
                    Card {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            val total = moodCounts.values.sum()
                            val top = (moodCounts.values.maxOrNull() ?: 1).toFloat()
                            val avgMood = moodCounts.entries.sumOf { it.key * it.value }.toDouble() / total
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Txt("توزیع حال‌وهوا", 14.sp, color = C.Muted)
                                Txt("میانگین ${Fa.moods[(avgMood.roundToInt() - 1).coerceIn(0, 4)]}", 13.sp, FontWeight.Medium, C.Green)
                            }
                            (5 downTo 1).forEach { m ->
                                val c = moodCounts[m] ?: 0
                                HBar("${Fa.moods[m - 1]}  ${Fa.moodNames[m - 1]}", c / top, Fa.n(c), moodColor(m))
                            }
                        }
                    }
                }
                val moodByPhase = s.days.values.mapNotNull { dl ->
                    val m = dl.mood ?: return@mapNotNull null
                    val ph = engine.phaseOn(LocalDate.parse(dl.day)) ?: return@mapNotNull null
                    ph to m
                }.groupBy({ it.first }, { it.second })
                if (moodByPhase.isNotEmpty()) Reveal(5) {
                    Card {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Txt("حال‌وهوا در هر فاز", 14.sp, color = C.Muted)
                            Phase.values().filter { it in moodByPhase }.forEach { ph ->
                                val avg = moodByPhase[ph]!!.average()
                                HBar(Fa.phaseName(ph), (avg / 5.0).toFloat(), Fa.moods[(avg.roundToInt() - 1).coerceIn(0, 4)], phaseColor(ph))
                            }
                        }
                    }
                }

                // ---- symptoms
                val recent = s.days.values.filter { LocalDate.parse(it.day) >= today.minusDays(120) }
                val counts = recent.flatMap { Fa.realSymptoms(it.symptoms) }.groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.take(6)
                if (counts.isNotEmpty()) Reveal(6) {
                    Card {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Txt("شایع‌ترین علائم (۴ ماه اخیر)", 14.sp, color = C.Muted)
                            val top = counts.first().value.toFloat()
                            counts.forEach { HBar(Fa.symptomName(it.key), it.value / top, Fa.n(it.value), C.Amber) }
                            val pms = recent.filter { engine.phaseOn(LocalDate.parse(it.day)) == Phase.PMS }
                                .flatMap { Fa.realSymptoms(it.symptoms) }.groupingBy { it }.eachCount().maxByOrNull { it.value }
                            if (pms != null && pms.value >= 2) {
                                Txt(
                                    if (isHer) "در روزهای PMS بیشتر «${Fa.symptomName(pms.key)}» ثبت کردی."
                                    else "در روزهای PMS بیشتر «${Fa.symptomName(pms.key)}» برای $ownerName ثبت شده.",
                                    13.sp, FontWeight.Medium, C.Green,
                                )
                            }
                        }
                    }
                }

                // ---- bleeding intensity
                val flowCounts = s.days.values.mapNotNull { Fa.flowOf(it.symptoms) }.groupingBy { it }.eachCount()
                if (flowCounts.isNotEmpty()) Reveal(6) {
                    Card {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Txt("شدت خونریزی", 14.sp, color = C.Muted)
                            val top = (flowCounts.values.maxOrNull() ?: 1).toFloat()
                            Fa.flows.forEach { (id, label) ->
                                val c = flowCounts[id] ?: 0
                                HBar(label, c / top, Fa.n(c), C.Coral)
                            }
                        }
                    }
                }

                // ---- history
                Reveal(7) {
                    Card {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Txt("تاریخچه‌ی پریودها", 14.sp, color = C.Muted)
                                Txt("${Fa.n(engine.periods.size)} مورد", 13.sp, color = C.Muted)
                            }
                            val all = engine.periods
                            val shown = if (showAll) all.size else minOf(8, all.size)
                            for (k in 0 until shown) {
                                val idx = all.size - 1 - k
                                val p = all[idx]
                                val gap = if (idx > 0) ChronoUnit.DAYS.between(all[idx - 1].start, p.start).toInt() else null
                                if (k > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(C.Line))
                                HistoryRow(p, gap, avgCycle, engine.variability)
                            }
                            if (all.size > 8) {
                                Txt(
                                    if (showAll) "نمایش کمتر" else "نمایش همه‌ی ${Fa.n(all.size)} مورد",
                                    13.sp, FontWeight.Medium, C.Green,
                                    Modifier.align(Alignment.CenterHorizontally).pressable { showAll = !showAll }.padding(8.dp),
                                )
                            }
                        }
                    }
                }

                // ---- notes
                val notes = s.notes.values.sortedByDescending { it.day }.take(6)
                if (notes.isNotEmpty()) Reveal(8) {
                    Card {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Txt("یادداشت‌ها", 14.sp, color = C.Muted)
                            notes.forEach { n ->
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Txt(Fa.date(LocalDate.parse(n.day), "d MMMM"), 12.sp, FontWeight.Medium, C.Green)
                                        if (isHer) Txt(if (n.shared) "  ·  مشترک" else "  ·  خصوصی", 12.sp, color = C.Muted)
                                    }
                                    Txt(n.body, 14.sp, maxLines = 3)
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(130.dp))
    }
}

private data class PredRow(val actual: LocalDate, val predicted: LocalDate, val err: Int)

/** For each past period, what the engine would have predicted using only the data before it. */
private fun predictionHistory(owner: Profile, logs: Set<LocalDate>, periods: List<Period>): List<PredRow> {
    val out = mutableListOf<PredRow>()
    for (i in 1 until periods.size) {
        val cutoff = periods[i].start
        val e = owner.toEngine(periods[i - 1].end.plusDays(1), logs.filter { it < cutoff })
        val predicted = e.nextPeriod ?: continue
        out += PredRow(cutoff, predicted, ChronoUnit.DAYS.between(predicted, cutoff).toInt())
    }
    return out
}

private fun moodColor(m: Int): Color = when (m) {
    1 -> C.Coral
    2 -> C.Amber
    3 -> Color(0xFFB8C2BA)
    4 -> C.Lime
    else -> C.Green
}

@Composable
private fun RowScope.StatTile(label: String, value: String, unit: String, accent: Color) {
    Column(
        Modifier.weight(1f).clip(RoundedCornerShape(24.dp)).background(C.Surface)
            .border(1.dp, C.Line, RoundedCornerShape(24.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(accent))
            Spacer(Modifier.size(8.dp))
            Txt(label, 12.sp, color = C.Muted)
        }
        Txt(value, 26.sp, FontWeight.Light, lineHeight = 34.sp)
        Txt(unit, 12.sp, color = C.Muted)
    }
}

@Composable
private fun HistoryRow(p: dev.imhamid.period.engine.Period, gap: Int?, avg: Int, variability: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(RoundedCornerShape(5.dp)).background(C.Coral))
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Txt(Fa.date(p.start, "d MMMM y"), 15.sp, FontWeight.Medium)
            Txt(
                if (p.length > 1) "${Fa.n(p.length)} روز · تا ${Fa.date(p.end, "d MMMM")}" else "${Fa.n(p.length)} روز پریود",
                12.sp, color = C.Muted,
            )
        }
        if (gap != null) {
            val dev = gap - avg
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(Modifier.clip(RoundedCornerShape(12.dp)).background(C.LimeSoft).padding(horizontal = 10.dp, vertical = 4.dp)) {
                    Txt("چرخه ${Fa.n(gap)}", 12.sp, FontWeight.Medium, C.OnSoft)
                }
                if (dev != 0) {
                    val off = kotlin.math.abs(dev) > variability
                    Box(Modifier.clip(RoundedCornerShape(12.dp)).background(if (off) C.AmberSoft else C.Paper).padding(horizontal = 10.dp, vertical = 2.dp)) {
                        Txt(if (dev > 0) "+${Fa.n(dev)} از میانگین" else "−${Fa.n(-dev)} از میانگین", 11.sp, color = if (off) C.Text else C.Muted)
                    }
                }
            }
        }
    }
}

private fun phaseColor(p: Phase): Color = when (p) {
    Phase.MENSTRUAL -> C.Coral
    Phase.PMS -> C.Amber
    Phase.FERTILE, Phase.OVULATION -> C.Lime
    else -> C.Green
}

@Composable
private fun StatBlock(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Txt(value, 17.sp, FontWeight.Medium)
        Txt(label, 12.sp, color = C.Muted)
    }
}

@Composable
private fun BarChart(values: List<Int>, labels: List<String>, color: Color, maxHeight: Dp = 110.dp) {
    val max = (values.maxOrNull() ?: 1).coerceAtLeast(1)
    var go by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { go = true }
    Row(
        Modifier.fillMaxWidth().height(maxHeight + 52.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom,
    ) {
        values.forEachIndexed { i, v ->
            val p by animateFloatAsState(if (go) 1f else 0f, tween(800, delayMillis = i * 60, easing = FastOutSlowInEasing), label = "bar$i")
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                Txt(Fa.n(v), 11.sp, color = C.Muted)
                Spacer(Modifier.height(4.dp))
                Box(Modifier.width(16.dp).height(maxHeight * (v.toFloat() / max) * p).clip(RoundedCornerShape(8.dp)).background(color))
                Spacer(Modifier.height(4.dp))
                Txt(labels[i], 9.sp, color = C.Muted, maxLines = 1)
            }
        }
    }
}

@Composable
private fun HBar(label: String, frac: Float, trailing: String, color: Color) {
    var go by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { go = true }
    val p by animateFloatAsState(if (go) frac.coerceIn(0f, 1f) else 0f, tween(900, easing = FastOutSlowInEasing), label = "hbar")
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Txt(label, 14.sp)
            Txt(trailing, 14.sp, FontWeight.Medium)
        }
        Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(C.Paper)) {
            Box(Modifier.fillMaxWidth(p).fillMaxHeight().clip(RoundedCornerShape(5.dp)).background(color))
        }
    }
}
