package dev.imhamid.period.ui

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.imhamid.period.engine.CycleEngine
import dev.imhamid.period.engine.Phase
import dev.imhamid.period.engine.toEngine
import java.time.LocalDate
import java.time.temporal.ChronoUnit

private val FertileGreen = Color(0xFF8BE36F)
private val OvulationBlue = Color(0xFF8CC8FF)

private fun chanceOf(engine: CycleEngine?, d: LocalDate): String = when (engine?.phaseOn(d)) {
    Phase.OVULATION -> "زیاد"
    Phase.FERTILE -> "متوسط"
    else -> "کم"
}

private fun cycleDayOf(engine: CycleEngine?, d: LocalDate): Int? {
    val seg = engine?.segmentFor(d) ?: return null
    return ChronoUnit.DAYS.between(seg.start, d).toInt() + 1
}

@Composable
fun CalendarTab(vm: AppVM) {
    val s = vm.snap ?: return
    val isHer = s.me.role == "her"
    val today = rememberToday()
    val engine = remember(s.owner, s.logs, today) { s.owner?.toEngine(today, s.logs) }
    var monthStart by remember { mutableStateOf(Fa.jalaliMonth(today).first) }
    var selected by remember { mutableStateOf(today) }
    var menu by remember { mutableStateOf(false) }
    val bbtDay = remember(s.health, today) { Insights.bbtOvulation(s, today) }
    val (first, len) = remember(monthStart) { Fa.jalaliMonth(monthStart) }
    val lead = weekIndex(first)
    val rows = (lead + len + 6) / 7
    val months = remember(today) {
        var f = Fa.jalaliMonth(today).first
        repeat(6) { f = Fa.jalaliMonth(f.minusDays(1)).first }
        buildList { repeat(13) { val m = Fa.jalaliMonth(f); add(m.first); f = m.first.plusDays(m.second.toLong()) } }
    }

    Column(Modifier.fillMaxSize().background(C.Paper).statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        Box(Modifier.fillMaxWidth().height(60.dp)) { Txt("تقویم", 22.sp, FontWeight.Bold, modifier = Modifier.align(Alignment.Center)) }

        // title + month picker pill
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Txt("حال‌وهوای چرخه", 26.sp, FontWeight.Bold, modifier = Modifier.weight(1f))
            Box {
                Row(
                    Modifier.clip(RoundedCornerShape(26.dp)).background(C.Surface).pressable { menu = true }.padding(start = 6.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(34.dp).clip(CircleShape).background(C.Pink), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.CalendarMonth, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
                    Txt(Fa.date(first, "MMMM yyyy"), 14.sp, FontWeight.Medium, modifier = Modifier.padding(horizontal = 10.dp))
                    Box(Modifier.size(30.dp).clip(CircleShape).background(C.SurfaceAlt), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.KeyboardArrowDown, null, tint = C.Text, modifier = Modifier.size(20.dp)) }
                }
                DropdownMenu(menu, { menu = false }) {
                    months.forEach { m ->
                        DropdownMenuItem(text = { Txt(Fa.date(m, "MMMM yyyy"), 15.sp, if (m == first) FontWeight.Bold else FontWeight.Normal, if (m == first) C.Pink else C.Text) }, onClick = { monthStart = m; menu = false })
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth()) { (0..6).forEach { Txt(weekdayLetter(it), 15.sp, FontWeight.Medium, C.Muted, Modifier.weight(1f), TextAlign.Center) } }
        Spacer(Modifier.height(10.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(rows) { r ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(7) { c ->
                        val idx = r * 7 + c - lead
                        if (idx in 0 until len) {
                            val d = first.plusDays(idx.toLong())
                            DayCell(d, today, selected, engine, d == bbtDay, Modifier.weight(1f)) { selected = d }
                        } else Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        // legend + add button
        Row(Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                LegendDot(OvulationBlue, "روزهای تخمک‌گذاری")
                LegendDot(FertileGreen, "روزهای باروری")
                LegendDot(C.Period, "روزهای پریود")
            }
            if (isHer && !selected.isAfter(today)) {
                Row(
                    Modifier.clip(RoundedCornerShape(26.dp)).background(C.Pink).pressable { vm.openLog(selected) }.padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Add, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Txt("افزودن علامت", 15.sp, FontWeight.Bold, Color.White, Modifier.padding(start = 6.dp))
                }
            }
        }

        // selected day
        Spacer(Modifier.height(16.dp))
        Panel {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Txt(Fa.date(selected, "d MMMM"), 20.sp, FontWeight.Bold)
                    val cd = cycleDayOf(engine, selected)
                    Txt(if (cd != null) "روز چرخه: ${Fa.n(cd)}" else Fa.date(selected, "EEEE"), 13.sp, color = C.Muted)
                }
                if (isHer) OutlinePill("ویرایش پریود", { vm.push(Screen.EditPeriod) }, icon = Icons.Rounded.WaterDrop)
            }
            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(C.Line))
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                ChanceChip(chanceOf(engine, selected))
                Txt("  ·  احتمال بارداری", 15.sp, color = C.Text)
            }
            Spacer(Modifier.height(16.dp))
            DaySummary(vm, selected)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun DayCell(d: LocalDate, today: LocalDate, selected: LocalDate, engine: CycleEngine?, bbtOvulation: Boolean, modifier: Modifier, onTap: () -> Unit) {
    val phase = engine?.phaseOn(d)
    val logged = engine?.isLogged(d) == true
    val predicted = !logged && engine?.isPredictedPeriod(d) == true
    val (color, kind) = when {
        logged -> C.Period to FaceKind.Sad
        predicted -> C.Period.copy(alpha = 0.55f) to FaceKind.Sad
        phase == Phase.OVULATION || bbtOvulation -> OvulationBlue to FaceKind.Calm
        phase == Phase.FERTILE -> FertileGreen to FaceKind.Smile
        else -> C.Track to FaceKind.Neutral
    }
    val isToday = d == today
    val sel = d == selected
    Column(
        modifier.height(70.dp).clip(RoundedCornerShape(22.dp))
            .background(if (sel) C.PinkSoft else C.Surface)
            .border(if (sel) 2.dp else if (isToday) 1.5.dp else 0.dp, if (sel) C.Pink else C.Pink.copy(alpha = 0.6f), RoundedCornerShape(22.dp))
            .pressable(role = null) { onTap() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Face(color, kind, 28.dp)
        Spacer(Modifier.height(6.dp))
        Txt(Fa.jalaliDay(d), 16.sp, if (isToday || sel) FontWeight.Bold else FontWeight.Medium, if (isToday) C.Pink else C.Text, lineHeight = 18.sp)
    }
}

@Composable
private fun TagChip(icon: ImageVector, text: String) {
    Row(Modifier.clip(RoundedCornerShape(20.dp)).background(C.PinkSoft).padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = C.Pink, modifier = Modifier.size(18.dp))
        Txt(text, 13.sp, FontWeight.Medium, modifier = Modifier.padding(start = 6.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DaySummary(vm: AppVM, day: LocalDate) {
    val s = vm.snap ?: return
    val isHer = s.me.role == "her"
    val dl = s.days[day]
    val hl = s.health[day]
    val flow = Fa.flowOf(dl?.symptoms.orEmpty())?.let { Catalog.normalizeFlow(it) }
    val moods = dl?.moods.orEmpty().mapNotNull { Catalog.mood(it) }
    val syms = Fa.realSymptoms(dl?.symptoms.orEmpty())
    val meds = hl?.medicines.orEmpty().mapNotNull { Catalog.medicine(it) }
    val has = flow != null || moods.isNotEmpty() || syms.isNotEmpty() || dl?.mood != null || meds.isNotEmpty() ||
        hl?.note?.isNotBlank() == true || hl?.weightKg != null || hl?.temperatureC != null || hl?.waterMl != null
    if (!has) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Txt("رکوردی ثبت نشده", 18.sp, FontWeight.Bold)
            if (isHer && !day.isAfter(LocalDate.now())) Txt("برای افزودن علامت بزن", 14.sp, color = C.Muted, modifier = Modifier.padding(top = 4.dp))
        }
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (flow != null) Row(verticalAlignment = Alignment.CenterVertically) {
            Drops(Catalog.drops(flow), 28.dp)
            Txt("  خونریزی ${Catalog.flows.firstOrNull { it.id == flow }?.label ?: ""}", 15.sp)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            moods.forEach { TagChip(it.icon, it.label) }
            if (moods.isEmpty() && dl?.mood != null) TagChip(Catalog.legacyMoodIcon(dl.mood), Fa.moodNames[dl.mood - 1])
            syms.forEach { id -> TagChip(Catalog.symptom(id)?.icon ?: Icons.Rounded.WaterDrop, Catalog.symptom(id)?.label ?: Fa.symptomName(id)) }
            meds.forEach { TagChip(it.icon, it.label) }
        }
        val stats = listOfNotNull(
            hl?.weightKg?.let { "وزن ${Fa.digits(fmt1(it))} کیلو" },
            hl?.temperatureC?.let { "دما ${Fa.digits(fmt1(it))}°" },
            hl?.waterMl?.let { "آب ${Fa.n(it)} میلی‌لیتر" },
        )
        if (stats.isNotEmpty()) Txt(stats.joinToString("  ·  "), 14.sp, color = C.Muted)
        if (!hl?.note.isNullOrBlank()) Txt(hl?.note.orEmpty(), 14.sp, color = C.Text)
    }
}

fun fmt1(v: Double): String = if (v % 1.0 == 0.0) v.toInt().toString() else String.format(java.util.Locale.US, "%.1f", v)
