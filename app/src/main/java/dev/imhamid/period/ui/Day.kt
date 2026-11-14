package dev.imhamid.period.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.imhamid.period.engine.CycleEngine
import dev.imhamid.period.engine.toEngine
import kotlinx.coroutines.delay
import java.time.LocalDate

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DaySheet(vm: AppVM, day: LocalDate, onClose: () -> Unit) {
    val s = vm.snap ?: return
    val isHer = s.me.role == "her"
    val owner = s.owner
    val today = remember { LocalDate.now() }
    val engine = remember(s.logs, owner) {
        owner?.toEngine(today, s.logs)
    }
    val dl = s.days[day]
    val note = s.notes[day]
    val logged = engine?.isLogged(day) == true
    val phase = engine?.phaseOn(day)
    val ownerName = owner?.name?.takeIf { it.isNotBlank() } ?: "پارتنرت"

    var text by remember(day) { mutableStateOf(note?.body.orEmpty()) }
    var shared by remember(day) { mutableStateOf(note?.shared ?: false) }
    var saved by remember { mutableStateOf(false) }
    LaunchedEffect(saved) { if (saved) { delay(1500); saved = false } }

    Column(
        Modifier.fillMaxWidth().fillMaxHeight(0.88f)
            .clip(RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp)).background(C.Paper)
            .navigationBarsPadding().imePadding()
            .verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Box(Modifier.align(Alignment.CenterHorizontally).size(44.dp, 5.dp).clip(CircleShape).background(C.Line))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Txt(Fa.date(day, "EEEE d MMMM"), 22.sp, FontWeight.Light)
                if (phase != null) Txt(Fa.phaseName(phase), 13.sp, color = C.Green)
            }
            RoundIcon(Icons.Rounded.Close, Color.White, C.Ink, onClose)
        }

        if (isHer) {
            PrimaryButton(
                if (logged) "پریود ثبت شده ✓  (برای حذف بزن)" else "ثبت پریود برای این روز",
                { vm.toggleDay(day) },
                kind = if (logged) BtnKind.Lime else BtnKind.Dark,
                icon = { Icon(if (logged) Icons.Rounded.Check else Icons.Rounded.WaterDrop, null, tint = if (logged) C.Deep else C.Lime, modifier = Modifier.size(20.dp)) },
            )
        } else if (logged) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(12.dp).clip(CircleShape).background(C.Coral))
                Spacer(Modifier.size(10.dp))
                Txt("$ownerName این روز پریود بوده", 15.sp)
            }
        }

        // ---- mood
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Txt("حال‌وهوا", 14.sp, color = C.Muted)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Fa.moods.forEachIndexed { i, e ->
                    MoodFace(e, selected = dl?.mood == i + 1, enabled = isHer) {
                        vm.saveDay(day, if (dl?.mood == i + 1) null else i + 1, dl?.energy, dl?.symptoms.orEmpty())
                    }
                }
            }
        }

        // ---- energy
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Txt("انرژی", 14.sp, color = C.Muted)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Fa.energyNames.forEachIndexed { i, n ->
                    Chip(n, dl?.energy == i + 1, {
                        if (isHer) vm.saveDay(day, dl?.mood, if (dl?.energy == i + 1) null else i + 1, dl?.symptoms.orEmpty())
                    })
                }
            }
        }

        // ---- symptoms
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Txt("علائم", 14.sp, color = C.Muted)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Fa.symptoms.forEach { (id, label) ->
                    val on = dl?.symptoms?.contains(id) == true
                    if (isHer || on) {
                        Chip(label, on, {
                            if (isHer) {
                                val cur = dl?.symptoms.orEmpty()
                                vm.saveDay(day, dl?.mood, dl?.energy, if (on) cur - id else cur + id)
                            }
                        })
                    }
                }
            }
            if (!isHer && dl?.symptoms.isNullOrEmpty()) Txt("علامتی ثبت نشده", 13.sp, color = C.Muted)
        }

        // ---- note
        if (isHer) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Txt("یادداشت", 14.sp, color = C.Muted)
                Field(text, { text = it }, "هر چی تو دلته…", singleLine = false, imeAction = androidx.compose.ui.text.input.ImeAction.Default)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Txt("نمایش به پارتنر", 15.sp, FontWeight.Medium)
                        Txt(if (shared) "پارتنرت می‌تونه ببینه" else "فقط برای خودت", 12.sp, color = C.Muted)
                    }
                    Switch(
                        shared, { shared = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = C.Green, checkedThumbColor = Color.White, uncheckedTrackColor = Color(0xFFD9DED9), uncheckedThumbColor = Color.White, uncheckedBorderColor = Color.Transparent),
                    )
                }
                PrimaryButton(
                    if (saved) "ذخیره شد ✓" else "ذخیره‌ی یادداشت",
                    { vm.saveNote(day, text, shared); saved = true },
                    kind = if (saved) BtnKind.Lime else BtnKind.Dark,
                )
            }
        } else if (note != null) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Txt("یادداشت $ownerName", 14.sp, color = C.Muted)
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color.White).border(1.dp, C.Line, RoundedCornerShape(20.dp)).padding(16.dp)) {
                    Txt(note.body, 15.sp)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun MoodFace(emoji: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(if (selected) C.Lime else Color.White, tween(250), label = "mf")
    val sc by animateFloatAsState(if (selected) 1.18f else 1f, spring(0.35f, 420f), label = "mfs")
    Box(
        Modifier.graphicsLayer { scaleX = sc; scaleY = sc }
            .size(54.dp).clip(CircleShape).background(bg).border(1.dp, C.Line, CircleShape)
            .then(if (enabled) Modifier.pressable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) { Txt(emoji, 26.sp, lineHeight = 30.sp) }
}
