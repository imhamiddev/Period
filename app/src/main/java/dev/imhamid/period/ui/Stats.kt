package dev.imhamid.period.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.mutableStateOf
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
import dev.imhamid.period.engine.CycleEngine
import dev.imhamid.period.engine.Phase
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

@Composable
fun StatsTab(vm: AppVM) {
    val s = vm.snap ?: return
    val isHer = s.me.role == "her"
    val owner = s.owner
    val today = remember { LocalDate.now() }
    val engine = remember(s.logs, owner) {
        owner?.let { CycleEngine(today, s.logs, it.cycleLength, it.periodLength, it.lutealLength, it.pmsDays) }
    }
    val ownerName = owner?.name?.takeIf { it.isNotBlank() } ?: "پارتنرت"

    Column(Modifier.fillMaxSize().background(C.Paper).statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(20.dp))
        Reveal(0) { Txt("آمار و تاریخچه", 32.sp, FontWeight.Light) }
        Spacer(Modifier.height(18.dp))
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (engine == null || engine.periods.isEmpty()) {
                Reveal(1) { Card { Txt("بعد از ثبت چند پریود، نمودارها و آمار اینجا نمایش داده می‌شن.", 15.sp, color = C.Muted) } }
            } else {
                val cycles = engine.periods.zipWithNext { a, b -> a.start to ChronoUnit.DAYS.between(a.start, b.start).toInt() }
                    .filter { it.second in CycleEngine.MIN_CYCLE..CycleEngine.MAX_CYCLE }.takeLast(12)
                val periods = engine.periods.takeLast(12)

                Reveal(1) {
                    Card {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            StatBlock("میانگین چرخه", if (cycles.isEmpty()) "—" else "${Fa.n(engine.predictedCycle)} روز")
                            StatBlock("میانگین پریود", "${Fa.n(engine.predictedPeriod)} روز")
                            StatBlock(
                                "نظم",
                                when {
                                    cycles.size < 3 -> "—"
                                    engine.variability <= 2 -> "منظم"
                                    engine.variability <= 4 -> "نسبتاً منظم"
                                    else -> "نامنظم"
                                },
                            )
                        }
                    }
                }
                if (cycles.isNotEmpty()) Reveal(2) {
                    Card {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Txt("طول چرخه‌ها (روز)", 14.sp, color = C.Muted)
                            BarChart(cycles.map { it.second }, cycles.map { Fa.date(it.first, "d MMM") }, C.Green)
                        }
                    }
                }
                Reveal(3) {
                    Card {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Txt("طول پریودها (روز)", 14.sp, color = C.Muted)
                            BarChart(periods.map { it.length }, periods.map { Fa.date(it.start, "d MMM") }, C.Coral)
                        }
                    }
                }

                // ---- mood by phase
                val moodByPhase = s.days.values.mapNotNull { dl ->
                    val m = dl.mood ?: return@mapNotNull null
                    val ph = engine.phaseOn(LocalDate.parse(dl.day)) ?: return@mapNotNull null
                    ph to m
                }.groupBy({ it.first }, { it.second })
                if (moodByPhase.isNotEmpty()) Reveal(4) {
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
                val counts = recent.flatMap { it.symptoms }.groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.take(5)
                if (counts.isNotEmpty()) Reveal(5) {
                    Card {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Txt("شایع‌ترین علائم (۴ ماه اخیر)", 14.sp, color = C.Muted)
                            val top = counts.first().value.toFloat()
                            counts.forEach { HBar(Fa.symptomName(it.key), it.value / top, Fa.n(it.value), C.Amber) }
                            val pms = recent.filter { engine.phaseOn(LocalDate.parse(it.day)) == Phase.PMS }
                                .flatMap { it.symptoms }.groupingBy { it }.eachCount().maxByOrNull { it.value }
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

                // ---- history
                Reveal(6) {
                    Card {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Txt("تاریخچه", 14.sp, color = C.Muted)
                            val all = engine.periods
                            all.takeLast(8).reversed().forEachIndexed { i, p ->
                                val idx = all.size - 1 - i
                                val gap = if (idx > 0) ChronoUnit.DAYS.between(all[idx - 1].start, p.start).toInt() else null
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(10.dp).clip(RoundedCornerShape(5.dp)).background(C.Coral))
                                    Spacer(Modifier.size(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Txt(Fa.date(p.start, "d MMMM y"), 15.sp, FontWeight.Medium)
                                        Txt("${Fa.n(p.length)} روز پریود", 12.sp, color = C.Muted)
                                    }
                                    if (gap != null) {
                                        Box(Modifier.clip(RoundedCornerShape(12.dp)).background(C.LimeSoft).padding(horizontal = 10.dp, vertical = 4.dp)) {
                                            Txt("چرخه ${Fa.n(gap)}", 12.sp, FontWeight.Medium, C.Deep)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ---- notes
                val notes = s.notes.values.sortedByDescending { it.day }.take(6)
                if (notes.isNotEmpty()) Reveal(7) {
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
