package dev.imhamid.period.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material.icons.rounded.WaterDrop
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.imhamid.period.engine.CycleEngine
import dev.imhamid.period.engine.Phase
import dev.imhamid.period.engine.toEngine
import java.time.LocalDate
import java.time.temporal.ChronoUnit

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
    val (first, len) = remember(monthStart) { Fa.jalaliMonth(monthStart) }
    val lead = weekIndex(first)
    val rows = (lead + len + 6) / 7

    Column(Modifier.fillMaxSize().background(C.Paper).statusBarsPadding().verticalScroll(rememberScrollState())) {
        Box(Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 16.dp)) {
            Row(Modifier.align(Alignment.Center), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).pressable(role = null) { monthStart = first.plusDays(len.toLong()) }, contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, "ماه قبل", tint = C.Text)
                }
                Txt(Fa.date(first, "MMMM yyyy"), 20.sp, FontWeight.Bold, modifier = Modifier.width(170.dp), align = TextAlign.Center)
                Box(Modifier.size(40.dp).pressable(role = null) { monthStart = Fa.jalaliMonth(first.minusDays(1)).first }, contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, "ماه بعد", tint = C.Text)
                }
            }
            Box(Modifier.align(Alignment.CenterEnd).size(44.dp).pressable(role = null) { monthStart = Fa.jalaliMonth(today).first; selected = today }, contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Today, "امروز", tint = C.Text)
            }
        }

        Row(Modifier.fillMaxWidth().background(C.Surface).padding(vertical = 10.dp)) {
            (0..6).forEach { Txt(weekdayLetter(it), 15.sp, FontWeight.Medium, C.Muted, Modifier.weight(1f), TextAlign.Center) }
        }

        Column(Modifier.fillMaxWidth().background(C.Surface)) {
            repeat(rows) { r ->
                Row(Modifier.fillMaxWidth()) {
                    repeat(7) { c ->
                        val idx = r * 7 + c - lead
                        val d = first.plusDays(idx.toLong())
                        val inMonth = idx in 0 until len
                        DayCell(d, inMonth, today, selected, engine, Modifier.weight(1f)) { selected = d; if (!inMonth) monthStart = Fa.jalaliMonth(d).first }
                    }
                }
            }
        }

        // ---- selected day
        Column(Modifier.fillMaxWidth().background(C.Paper).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Txt(Fa.date(selected, "d MMMM"), 20.sp, FontWeight.Bold)
                        val cd = cycleDayOf(engine, selected)
                        Txt(if (cd != null) "روز چرخه: ${Fa.n(cd)}" else Fa.date(selected, "EEEE"), 13.sp, color = C.Muted)
                    }
                    if (isHer) OutlinePill("ویرایش", { vm.push(Screen.EditPeriod) }, icon = Icons.Rounded.WaterDrop)
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
                if (isHer && !selected.isAfter(today)) {
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        BigButton("افزودن لاگ", { vm.openLog(selected) }, Modifier.width(200.dp).height(50.dp))
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun DayCell(d: LocalDate, inMonth: Boolean, today: LocalDate, selected: LocalDate, engine: CycleEngine?, modifier: Modifier, onTap: () -> Unit) {
    val phase = engine?.phaseOn(d)
    val logged = engine?.isLogged(d) == true
    val predicted = !logged && engine?.isPredictedPeriod(d) == true
    val isToday = d == today
    val bg = when {
        logged -> C.Period
        predicted -> C.Period.copy(alpha = 0.16f)
        else -> Color.Transparent
    }
    val dim = if (inMonth) 1f else 0.35f
    val textColor = if (logged) Color.White else C.Text
    Box(
        modifier.height(66.dp).border(0.5.dp, C.Line)
            .background(bg)
            .then(if (isToday) Modifier.background(C.Orange.copy(alpha = if (logged) 0.0f else 0.18f)) else Modifier)
            .then(if (d == selected) Modifier.border(2.dp, C.Pink) else Modifier)
            .pressable(role = null) { onTap() },
    ) {
        Txt(Fa.jalaliDay(d), 19.sp, if (isToday) FontWeight.Bold else FontWeight.Medium, textColor.copy(alpha = dim), Modifier.align(Alignment.TopStart).padding(start = 6.dp, top = 4.dp))
        cycleDayOf(engine, d)?.let { Txt(Fa.n(it), 10.sp, color = textColor.copy(alpha = 0.55f * dim), modifier = Modifier.align(Alignment.TopEnd).padding(end = 5.dp, top = 5.dp)) }
        Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp)) {
            when {
                logged || predicted -> Icon(Icons.Rounded.WaterDrop, null, tint = if (logged) Color.White else C.Period, modifier = Modifier.size(18.dp))
                phase == Phase.OVULATION -> Txt("🥚", 14.sp, lineHeight = 16.sp)
                phase == Phase.FERTILE -> Txt("🌼", 14.sp, lineHeight = 16.sp)
                phase != null && inMonth -> Txt("🍀", 13.sp, color = Color.Unspecified, lineHeight = 15.sp)
            }
        }
    }
}

@Composable
fun DaySummary(vm: AppVM, day: LocalDate) {
    val s = vm.snap ?: return
    val isHer = s.me.role == "her"
    val dl = s.days[day]
    val hl = if (isHer) s.health[day] else null
    val flow = Fa.flowOf(dl?.symptoms.orEmpty())?.let { Catalog.normalizeFlow(it) }
    val moods = dl?.moods.orEmpty().mapNotNull { Catalog.mood(it) }
    val syms = Fa.realSymptoms(dl?.symptoms.orEmpty()).map { id -> Catalog.symptom(id)?.let { "${it.emoji} ${it.label}" } ?: Fa.symptomName(id) }
    val meds = hl?.medicines.orEmpty().mapNotNull { Catalog.medicine(it) }
    val has = flow != null || moods.isNotEmpty() || syms.isNotEmpty() || dl?.mood != null || meds.isNotEmpty() ||
        hl?.note?.isNotBlank() == true || hl?.weightKg != null || hl?.temperatureC != null || hl?.waterMl != null
    if (!has) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Txt("رکوردی ثبت نشده", 18.sp, FontWeight.Bold)
            if (isHer && !day.isAfter(LocalDate.now())) Txt("برای افزودن لاگ بزن", 14.sp, color = C.Muted, modifier = Modifier.padding(top = 4.dp))
        }
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (flow != null) Row(verticalAlignment = Alignment.CenterVertically) {
            Drops(Catalog.drops(flow), 28.dp)
            Txt("  خونریزی ${Catalog.flows.firstOrNull { it.id == flow }?.label ?: ""}", 15.sp)
        }
        if (moods.isNotEmpty()) Txt(moods.joinToString("   ") { "${it.emoji} ${it.label}" }, 15.sp)
        else if (dl?.mood != null) Txt("${Fa.moods[dl.mood - 1]} ${Fa.moodNames[dl.mood - 1]}", 15.sp)
        if (syms.isNotEmpty()) Txt(syms.joinToString("  ·  "), 14.sp, color = C.Muted)
        if (meds.isNotEmpty()) Txt(meds.joinToString("   ") { "${it.emoji} ${it.label}" }, 14.sp)
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
