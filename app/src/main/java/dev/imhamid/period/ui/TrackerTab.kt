package dev.imhamid.period.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Circle
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.imhamid.period.data.Snapshot
import dev.imhamid.period.engine.CycleEngine
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

private class CycleStats(val avgPeriod: Int?, val avgCycle: Int?, val rows: List<CycleRow>)
class CycleRow(val start: LocalDate, val periodLen: Int, val cycleLen: Int?, val ovulationOffset: Int)

private fun cycleStats(s: Snapshot): CycleStats {
    val periods = CycleEngine.derivePeriods(s.logs).sortedBy { it.start }
    val lens = periods.zipWithNext { a, b -> ChronoUnit.DAYS.between(a.start, b.start).toInt() }.filter { it in 15..60 }
    val rows = periods.mapIndexed { i, p ->
        val next = periods.getOrNull(i + 1)
        val cl = next?.let { ChronoUnit.DAYS.between(p.start, it.start).toInt() }
        CycleRow(p.start, p.length, cl, ((cl ?: s.owner?.cycleLength ?: 28) - (s.owner?.lutealLength ?: 14)))
    }.reversed()
    return CycleStats(
        periods.takeIf { it.isNotEmpty() }?.map { it.length }?.average()?.roundToInt(),
        lens.takeIf { it.isNotEmpty() }?.average()?.roundToInt(),
        rows,
    )
}

@Composable
fun TrackerTab(vm: AppVM) {
    val s = vm.snap ?: return
    val isHer = s.me.role == "her"
    var seg by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize().background(C.Paper).statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.fillMaxWidth().height(60.dp)) { Txt("ترکر", 22.sp, FontWeight.Bold, modifier = Modifier.align(Alignment.Center)) }
        SegTabs(listOf("آمار ثبت‌ها", "سلامت"), seg, { seg = it })
        if (seg == 0) LogCountTab(vm, s) else HealthTab(vm, s)
        Spacer(Modifier.height(12.dp))
    }
}

// ============================================================================ log count
@Composable
private fun LogCountTab(vm: AppVM, s: Snapshot) {
    val isHer = s.me.role == "her"
    val cs = remember(s.logs, s.owner) { cycleStats(s) }
    val flowCounts = remember(s.days) {
        s.days.values.mapNotNull { Fa.flowOf(it.symptoms)?.let { f -> Catalog.normalizeFlow(f) } }.groupingBy { it }.eachCount()
    }
    val symCounts = remember(s.days) { s.days.values.flatMap { Fa.realSymptoms(it.symptoms) }.groupingBy { it }.eachCount().entries.sortedByDescending { it.value } }
    val moodCounts = remember(s.days) { s.days.values.flatMap { it.moods }.groupingBy { it }.eachCount().entries.sortedByDescending { it.value } }
    val medCounts = remember(s.health) { s.health.values.flatMap { it.medicines }.groupingBy { it }.eachCount().entries.sortedByDescending { it.value } }

    Panel(Modifier.pressable { vm.push(Screen.MyCycles) }) {
        SectionHeader("چرخه‌های من", onMore = { vm.push(Screen.MyCycles) })
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("میانگین پریود", cs.avgPeriod?.let { "${Fa.n(it)} روز" } ?: "—", Icons.Rounded.WaterDrop, Modifier.weight(1f))
            StatTile("میانگین چرخه", cs.avgCycle?.let { "${Fa.n(it)} روز" } ?: "—", Icons.Rounded.CalendarMonth, Modifier.weight(1f))
        }
    }

    Panel {
        Txt("جریان خونریزی", 18.sp, FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth()) { Catalog.flows.forEach { f -> FlowOpt(f.id, false, flowCounts[f.id] ?: 0, null, Modifier.weight(1f)) } }
    }

    CountPanel("علائم", symCounts.map { Triple(Catalog.symptom(it.key)?.icon ?: Icons.Rounded.Circle, Catalog.symptom(it.key)?.label ?: Fa.symptomName(it.key), it.value) }, C.PinkSoft)
    CountPanel("حال‌وهوا", moodCounts.map { Triple(Catalog.mood(it.key)?.icon ?: Icons.Rounded.Circle, Catalog.mood(it.key)?.label ?: it.key, it.value) }, C.AmberSoft)
    PatternPanel(s)
    if (isHer) CountPanel("پیشگیری و دارو", medCounts.map { Triple(Catalog.medicine(it.key)?.icon ?: Icons.Rounded.Circle, Catalog.medicine(it.key)?.label ?: it.key, it.value) }, C.SurfaceAlt)
}

@Composable
private fun PatternPanel(s: Snapshot) {
    val today = rememberToday()
    val rows = remember(s.days, s.logs, today) { Insights.patterns(s, today) }
    Panel {
        Txt("الگوی علائم در چرخه", 18.sp, FontWeight.Bold)
        Txt("هر علامت یا حال‌وهوا بیشتر تو کدوم فاز پیش میاد", 13.sp, color = C.Muted, modifier = Modifier.padding(top = 2.dp, bottom = 12.dp))
        if (rows.isEmpty()) {
            Txt("برای دیدن الگو، چند چرخه علائم و حال‌وهوا ثبت کن.", 14.sp, color = C.Muted)
            return@Panel
        }
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            rows.forEach { r ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).clip(CircleShape).background(C.PinkSoft), contentAlignment = Alignment.Center) { Icon(r.icon, null, tint = C.Pink, modifier = Modifier.size(22.dp)) }
                    Column(Modifier.padding(horizontal = 12.dp).weight(1f)) {
                        Txt(r.label, 15.sp, FontWeight.Medium)
                        Txt(r.topPhase?.let { "بیشتر در فاز ${Content.phases[it].short}" } ?: "بین فازها پراکنده‌ست", 12.sp, color = C.Muted)
                    }
                    val mx = (r.perPhase.maxOrNull() ?: 1).coerceAtLeast(1)
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.Bottom, modifier = Modifier.height(38.dp)) {
                        r.perPhase.forEachIndexed { i, n ->
                            Box(Modifier.width(13.dp).height((38 * n / mx.toFloat()).coerceAtLeast(3f).dp).clip(RoundedCornerShape(4.dp)).background(Content.phases[i].color.copy(alpha = if (n == 0) 0.25f else 1f)))
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Content.phases.forEach { LegendDot(it.color, it.short) } }
    }
}

@Composable
private fun CountPanel(title: String, items: List<Triple<androidx.compose.ui.graphics.vector.ImageVector, String, Int>>, bg: Color) {
    var all by remember { mutableStateOf(false) }
    Panel {
        Txt(title, 18.sp, FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        if (items.isEmpty()) {
            Txt("هنوز چیزی ثبت نشده.", 14.sp, color = C.Muted)
            return@Panel
        }
        val shown = if (all) items else items.take(8)
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            shown.chunked(4).forEach { row ->
                Row(Modifier.fillMaxWidth()) {
                    row.forEach { (e, l, n) -> CircleOpt(e, l, false, null, Modifier.weight(1f), count = n) }
                    repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
        if (items.size > 8) {
            Row(Modifier.fillMaxWidth().pressable { all = !all }.padding(top = 12.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Txt(if (all) "کمتر" else "نمایش بیشتر", 14.sp, FontWeight.Medium, C.Pink)
                Icon(Icons.Rounded.KeyboardArrowDown, null, tint = C.Pink)
            }
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier) {
    Box(modifier.clip(RoundedCornerShape(12.dp)).background(C.SurfaceAlt).padding(14.dp)) {
        Column {
            Txt(value, 20.sp, FontWeight.Bold)
            Txt(label, 12.sp, color = C.Muted)
        }
        Icon(icon, null, tint = C.Pink, modifier = Modifier.align(Alignment.BottomEnd).size(34.dp))
    }
}

// ============================================================================ health
@Composable
private fun HealthTab(vm: AppVM, s: Snapshot) {
    val wu = Units.weightUnit(vm)
    val tu = Units.tempUnit(vm)
    val vu = Units.cupUnit(vm)
    val today = rememberToday()
    var offset by remember { mutableIntStateOf(0) }
    val end = today.plusDays(offset * 7L)
    val days = remember(end) { (6 downTo 0).map { end.minusDays(it.toLong()) } }
    val labels = days.map { Fa.jalaliDay(it) }
    val range = "${Fa.date(days.first(), "d MMM")} – ${Fa.date(days.last(), "d MMM")}"

    val weights = days.map { s.health[it]?.weightKg?.let { v -> Units.kgToDisp(v, wu) } }
    val temps = days.map { s.health[it]?.temperatureC?.let { v -> Units.cToDisp(v, tu) } }
    val waters = days.map { s.health[it]?.waterMl?.let { v -> Units.mlToDisp(v, vu) } }
    val goal = Units.mlToDisp(s.body?.waterGoalMl ?: 2400, vu)

    ChartPanel("وزن (${Units.weightLabel(vm)})", C.Green, weights, labels, range, { offset-- }, { if (offset < 0) offset++ }, atEnd = offset >= 0, goal = null)
    if (vm.prefBool("bmi", true)) BmiPanel(vm, s)
    ChartPanel("دمای بدن (${Units.tempLabel(vm)})", C.Orange, temps, labels, range, { offset-- }, { if (offset < 0) offset++ }, atEnd = offset >= 0, goal = null, minZero = false)
    ChartPanel("آب (${Units.volLabel(vm)})", C.Blue, waters, labels, range, { offset-- }, { if (offset < 0) offset++ }, atEnd = offset >= 0, goal = goal)
}

@Composable
private fun ChartPanel(
    title: String, color: Color, values: List<Double?>, labels: List<String>, range: String,
    onPrev: () -> Unit, onNext: () -> Unit, atEnd: Boolean, goal: Double?, minZero: Boolean = true,
) {
    var line by remember { mutableStateOf(false) }
    var sel by remember(values) { mutableIntStateOf(values.indexOfLast { it != null }.let { if (it < 0) 6 else it }) }
    Panel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Txt(title, 18.sp, FontWeight.Bold, modifier = Modifier.weight(1f))
            Row(Modifier.clip(RoundedCornerShape(8.dp)).background(C.SurfaceAlt).padding(2.dp)) {
                ModeBtn(Icons.Rounded.BarChart, !line, color) { line = false }
                ModeBtn(Icons.Rounded.ShowChart, line, color) { line = true }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).pressable(role = null) { onPrev() }, contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, "قبل", tint = C.Text) }
            Txt(range, 15.sp, FontWeight.Medium, modifier = Modifier.weight(1f), align = TextAlign.Center)
            Box(Modifier.size(36.dp).pressable(enabled = !atEnd, role = null) { onNext() }, contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, "بعد", tint = if (atEnd) C.Track else C.Text)
            }
        }
        val v = values.getOrNull(sel)
        Txt(if (v != null) Fa.digits(fmt1(Math.round(v * 10) / 10.0)) else "—", 24.sp, FontWeight.Bold, color, modifier = Modifier.fillMaxWidth(), align = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        WeekChart(values, labels, color, line, goal, sel, { sel = it }, minZero)
    }
}

@Composable
private fun ModeBtn(icon: androidx.compose.ui.graphics.vector.ImageVector, on: Boolean, color: Color, onClick: () -> Unit) {
    Box(Modifier.clip(RoundedCornerShape(6.dp)).background(if (on) color else Color.Transparent).pressable { onClick() }.padding(horizontal = 8.dp, vertical = 4.dp)) {
        Icon(icon, null, tint = if (on) Color.White else C.Muted, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun WeekChart(values: List<Double?>, labels: List<String>, color: Color, line: Boolean, goal: Double?, sel: Int, onSel: (Int) -> Unit, minZero: Boolean) {
    val present = values.filterNotNull()
    val maxV = maxOf(present.maxOrNull() ?: 1.0, goal ?: 0.0) * 1.12
    val minV = if (minZero || present.isEmpty()) 0.0 else (present.min() - 1.0)
    val span = (maxV - minV).coerceAtLeast(0.1)
    val h = 150.dp
    Column {
        Box(Modifier.fillMaxWidth().height(h)) {
            if (line) {
                Canvas(Modifier.fillMaxSize()) {
                    val w = size.width
                    val hh = size.height
                    val step = w / values.size
                    fun x(i: Int) = step * (i + 0.5f)
                    fun y(v: Double) = (hh - ((v - minV) / span * hh)).toFloat()
                    val pts = values.mapIndexedNotNull { i, v -> v?.let { Offset(x(i), y(it)) } }
                    if (pts.size >= 2) {
                        val path = Path().apply { moveTo(pts[0].x, pts[0].y); pts.drop(1).forEach { lineTo(it.x, it.y) } }
                        val fill = Path().apply { addPath(path); lineTo(pts.last().x, hh); lineTo(pts.first().x, hh); close() }
                        drawPath(fill, Brush.verticalGradient(listOf(color.copy(alpha = 0.35f), color.copy(alpha = 0f))))
                        drawPath(path, color, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
                    }
                    pts.forEach { drawCircle(Color.White, 6.dp.toPx(), it); drawCircle(color, 4.dp.toPx(), it) }
                    goal?.let { gy -> drawLine(color, Offset(0f, y(gy)), Offset(w, y(gy)), 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))) }
                }
                Row(Modifier.fillMaxSize()) { values.indices.forEach { i -> Box(Modifier.weight(1f).fillMaxSize().pressable(role = null) { onSel(i) }) } }
            } else {
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                    values.forEachIndexed { i, v ->
                        Box(Modifier.weight(1f).fillMaxSize().pressable(role = null) { onSel(i) }, contentAlignment = Alignment.BottomCenter) {
                            val f = if (v == null) 0.0 else ((v - minV) / span).coerceIn(0.0, 1.0)
                            Box(
                                Modifier.fillMaxWidth().height(h * f.toFloat().coerceAtLeast(if (v == null) 0f else 0.03f))
                                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                    .background(if (i == sel) color else color.copy(alpha = 0.35f)),
                            )
                        }
                    }
                }
                goal?.let { g ->
                    Canvas(Modifier.fillMaxSize()) {
                        val gy = (size.height - ((g - minV) / span * size.height)).toFloat()
                        drawLine(color, Offset(0f, gy), Offset(size.width, gy), 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f)))
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth()) { labels.forEachIndexed { i, l -> Txt(l, 12.sp, if (i == sel) FontWeight.Bold else FontWeight.Normal, if (i == sel) color else C.Muted, Modifier.weight(1f), TextAlign.Center) } }
    }
}

// ---------------------------------------------------------------- BMI
@Composable
private fun BmiPanel(vm: AppVM, s: Snapshot) {
    val latestKg = s.health.entries.filter { it.value.weightKg != null }.maxByOrNull { it.key }?.value?.weightKg ?: s.body?.weightKg
    val bmi = Bmi.of(latestKg, s.body?.heightCm)
    Panel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Txt("شاخص توده بدنی (BMI)", 18.sp, FontWeight.Bold, modifier = Modifier.weight(1f))
            if (bmi != null) {
                val b = Bmi.band(bmi)
                Box(Modifier.clip(RoundedCornerShape(6.dp)).background(b.color).padding(horizontal = 10.dp, vertical = 2.dp)) { Txt(b.label, 12.sp, FontWeight.Medium, Color.White) }
            }
        }
        Spacer(Modifier.height(12.dp))
        if (bmi == null) {
            Txt(if (s.me.role == "her") "برای محاسبه‌ی BMI قد و وزنت رو تو اطلاعات شخصی وارد کن." else "هنوز قد و وزنی ثبت نشده.", 14.sp, color = C.Muted)
            if (s.me.role == "her") {
                Spacer(Modifier.height(10.dp))
                OutlinePill("اطلاعات شخصی", { vm.push(Screen.PersonalInfo) })
            }
            return@Panel
        }
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            BmiGauge(bmi, Modifier.size(240.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 70.dp)) {
                Txt(Fa.digits(fmt1(Math.round(bmi * 10) / 10.0)), 40.sp, FontWeight.Bold, lineHeight = 46.sp)
                Txt("kg/m²", 12.sp, color = C.Muted)
            }
        }
        Spacer(Modifier.height(10.dp))
        val cur = Bmi.band(bmi)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Bmi.bands.forEach { b ->
                val sel = b === cur
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(b.color))
                    Txt(b.label, 14.sp, if (sel) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.padding(start = 10.dp).weight(1f))
                    val r = when {
                        b.from == 0.0 -> "< ${Fa.digits(fmt1(b.to))}"
                        b.to >= 1000 -> "≥ ${Fa.digits(fmt1(b.from))}"
                        else -> "${Fa.digits(fmt1(b.from))} – ${Fa.digits(fmt1(b.to - 0.1))}"
                    }
                    Txt(r, 13.sp, if (sel) FontWeight.Bold else FontWeight.Normal, if (sel) C.Text else C.Muted)
                }
            }
        }
    }
}

@Composable
private fun BmiGauge(bmi: Double, modifier: Modifier) {
    val bg = C.Surface
    Canvas(modifier) {
        val stroke = 26.dp.toPx()
        val pad = stroke / 2 + 4.dp.toPx()
        val d = size.minDimension - pad * 2
        val tl = Offset(pad, pad)
        val sz = Size(d, d)
        val total = 270f
        val start0 = 135f
        val lo = 12.0
        val hi = 42.0
        fun frac(v: Double) = ((v.coerceIn(lo, hi) - lo) / (hi - lo)).toFloat()
        Bmi.bands.forEach { b ->
            val a = frac(b.from.coerceAtLeast(lo))
            val z = frac(b.to.coerceAtMost(hi))
            if (z > a) drawArc(b.color, start0 + a * total + 0.8f, (z - a) * total - 1.6f, false, tl, sz, style = Stroke(stroke))
        }
        val ang = Math.toRadians((start0 + frac(bmi) * total).toDouble())
        val cx = size.width / 2
        val cy = size.height / 2
        val r = d / 2
        val tip = Offset(cx + (r * 0.55f * Math.cos(ang)).toFloat(), cy + (r * 0.55f * Math.sin(ang)).toFloat())
        drawLine(Color(0xFF4CAF50), Offset(cx, cy), tip, 6.dp.toPx(), cap = StrokeCap.Round)
        drawCircle(bg, 11.dp.toPx(), Offset(cx, cy))
        drawCircle(Color(0xFF4CAF50), 11.dp.toPx(), Offset(cx, cy), style = Stroke(4.dp.toPx()))
    }
}

// ============================================================================ my cycles
@Composable
fun MyCyclesScreen(vm: AppVM) {
    val s = vm.snap ?: return
    val isHer = s.me.role == "her"
    val cs = remember(s.logs, s.owner) { cycleStats(s) }
    var filter by remember { mutableIntStateOf(0) }
    Page {
        TopBar("چرخه‌های من", onBack = { vm.pop() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Panel {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("میانگین پریود", cs.avgPeriod?.let { "${Fa.n(it)} روز" } ?: "—", Icons.Rounded.WaterDrop, Modifier.weight(1f))
                    StatTile("میانگین چرخه", cs.avgCycle?.let { "${Fa.n(it)} روز" } ?: "—", Icons.Rounded.CalendarMonth, Modifier.weight(1f))
                }
                if (isHer) {
                    Spacer(Modifier.height(12.dp))
                    BigButton("ویرایش پریود", { vm.push(Screen.EditPeriod) })
                }
            }
            if (isHer) ReportPanel(vm, s)
            Panel {
                Txt("تاریخچه", 18.sp, FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val names = listOf("همه", "پریود", "تخمک‌گذاری", "باروری")
                    items(names.size) { i -> FilterChip(names[i], filter == i) { filter = i } }
                }
                Spacer(Modifier.height(12.dp))
                if (cs.rows.isEmpty()) Txt("هنوز پریودی ثبت نشده.", 14.sp, color = C.Muted)
                cs.rows.forEach { r -> CycleHistoryRow(r, filter); Spacer(Modifier.height(14.dp)) }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun CycleHistoryRow(r: CycleRow, filter: Int) {
    val end = r.start.plusDays(r.periodLen - 1L)
    val len = r.cycleLen
    Column {
        Row(Modifier.fillMaxWidth()) {
            Txt("${Fa.date(r.start, "d MMM")} – ${Fa.date(end, "d MMM yyyy")}", 14.sp, FontWeight.Medium, modifier = Modifier.weight(1f))
            Txt(if (len != null) Fa.n(len) else "در جریان", 14.sp, FontWeight.Bold, C.Muted)
        }
        Spacer(Modifier.height(6.dp))
        val total = (len ?: 28).toFloat()
        Box(Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(7.dp)).background(C.Track.copy(alpha = 0.6f))) {
            Row(Modifier.fillMaxSize()) {
                val periodW = r.periodLen / total
                val fStart = (r.ovulationOffset - 5).coerceAtLeast(r.periodLen)
                val fEnd = r.ovulationOffset + 1
                if (filter == 0 || filter == 1) Box(Modifier.weight(periodW.coerceIn(0.05f, 0.9f)).fillMaxSize().clip(RoundedCornerShape(7.dp)).background(C.Period), contentAlignment = Alignment.Center) {
                    Txt(Fa.n(r.periodLen), 9.sp, FontWeight.Bold, Color.White, lineHeight = 10.sp)
                }
                val gap = ((fStart - r.periodLen) / total).coerceAtLeast(0f)
                if (gap > 0f) Spacer(Modifier.weight(gap))
                val fw = ((fEnd - fStart + 1) / total).coerceIn(0.04f, 0.5f)
                if (filter != 1) Box(Modifier.weight(fw).fillMaxSize().clip(RoundedCornerShape(7.dp)).background(if (filter == 2) C.Orange else C.Ovulation))
                val rest = (1f - periodW - gap - fw).coerceAtLeast(0.01f)
                Spacer(Modifier.weight(rest))
            }
        }
    }
}

@Composable
private fun ReportPanel(vm: AppVM, s: Snapshot) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var days by remember { mutableIntStateOf(90) }
    Panel {
        Txt("گزارش برای پزشک", 18.sp, FontWeight.Bold)
        Txt("یه PDF یک‌صفحه‌ای از چرخه، علائم، وزن و دمات برای نشون دادن به پزشک.", 13.sp, color = C.Muted, modifier = Modifier.padding(top = 2.dp, bottom = 12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(90 to "۳ ماه", 180 to "۶ ماه", 365 to "۱ سال").forEach { (d, l) -> FilterChip(l, days == d) { days = d } }
        }
        Spacer(Modifier.height(14.dp))
        BigButton("ساخت و اشتراک PDF", {
            runCatching { Report.build(ctx, s, days) }
                .onSuccess { Report.share(ctx, it) }
                .onFailure { vm.toast("ساخت گزارش ناموفق بود") }
        })
    }
}
