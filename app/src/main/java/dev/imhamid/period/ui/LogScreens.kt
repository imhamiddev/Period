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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LocalDrink
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate

/** Display units chosen in Preferences; values are always stored as kg / °C / mL / cm. */
object Units {
    fun weightUnit(vm: AppVM) = vm.pref("wunit", "kg")
    fun heightUnit(vm: AppVM) = vm.pref("hunit", "cm")
    fun tempUnit(vm: AppVM) = vm.pref("tunit", "c")
    fun cupUnit(vm: AppVM) = vm.pref("vunit", "ml")

    fun kgToDisp(kg: Double, unit: String) = if (unit == "lb") kg * 2.20462 else kg
    fun dispToKg(v: Double, unit: String) = if (unit == "lb") v / 2.20462 else v
    fun cToDisp(c: Double, unit: String) = if (unit == "f") c * 9 / 5 + 32 else c
    fun dispToC(v: Double, unit: String) = if (unit == "f") (v - 32) * 5 / 9 else v
    fun cmToDisp(cm: Double, unit: String) = if (unit == "ft") cm / 2.54 else cm
    fun mlToDisp(ml: Int, unit: String) = if (unit == "oz") ml / 29.5735 else ml.toDouble()

    fun weightLabel(vm: AppVM) = if (weightUnit(vm) == "lb") "پوند" else "کیلوگرم"
    fun tempLabel(vm: AppVM) = if (tempUnit(vm) == "f") "°F" else "°C"
    fun volLabel(vm: AppVM) = if (cupUnit(vm) == "oz") "اونس" else "میلی‌لیتر"
}

@Composable
private fun <T> OptGrid(items: List<T>, item: @Composable (T, Modifier) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        items.chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { item(it, Modifier.weight(1f)) }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
fun AddLogScreen(vm: AppVM) {
    val d = vm.draft
    if (d == null) { vm.pop(); return }
    val today = rememberToday()
    var allSymptoms by remember { mutableStateOf(false) }
    val s = vm.snap
    val weekStart = remember(d.day) { d.day.minusDays(weekIndex(d.day).toLong()) }
    val wu = Units.weightUnit(vm)
    val tu = Units.tempUnit(vm)

    Page {
        TopBar("افزودن لاگ", onBack = { vm.draft = null; vm.pop() }, close = true)
        // week strip
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            repeat(7) { i ->
                val day = weekStart.plusDays(i.toLong())
                val sel = day == d.day
                val future = day.isAfter(today)
                Column(
                    Modifier.pressable(enabled = !future, role = null) {
                        if (vm.draft != vm.draftFor(d.day)) vm.saveDraft()
                        vm.draft = vm.draftFor(day)
                    },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Txt(weekdayLetter(i), 13.sp, color = if (sel) C.Pink else C.Muted)
                    Box(
                        Modifier.padding(top = 4.dp).size(44.dp).clip(CircleShape).background(if (sel) C.Pink else C.Surface),
                        contentAlignment = Alignment.Center,
                    ) { Txt(Fa.jalaliDay(day), 16.sp, FontWeight.Bold, if (sel) Color.White else if (future) C.Muted.copy(alpha = 0.5f) else C.Text) }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Panel {
                Txt("جریان خونریزی", 17.sp, FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth()) {
                    Catalog.flows.forEach { f ->
                        FlowOpt(f.id, d.flow == f.id, null, { vm.updateDraft { it.copy(flow = if (it.flow == f.id) null else f.id) } }, Modifier.weight(1f))
                    }
                }
            }
            Panel {
                SectionHeader("علائم", onMore = { allSymptoms = !allSymptoms })
                Spacer(Modifier.height(12.dp))
                val list = if (allSymptoms) Catalog.symptoms else Catalog.symptoms.take(12)
                OptGrid(list) { o, m -> CircleOpt(o.icon, o.label, o.id in d.symptoms, { vm.updateDraft { it.copy(symptoms = it.symptoms.toggle(o.id)) } }, m, bg = C.PinkSoft) }
            }
            Panel {
                Txt("حال‌وهوا", 17.sp, FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                OptGrid(Catalog.moods) { o, m -> CircleOpt(o.icon, o.label, o.id in d.moods, { vm.updateDraft { it.copy(moods = it.moods.toggle(o.id)) } }, m, bg = C.PinkSoft) }
            }
            Panel {
                Txt("پیشگیری و دارو", 17.sp, FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                OptGrid(Catalog.medicines) { o, m -> CircleOpt(o.icon, o.label, o.id in d.medicines, { vm.updateDraft { it.copy(medicines = it.medicines.toggle(o.id)) } }, m, bg = C.PinkSoft) }
            }
            Panel {
                Txt("یادداشت", 17.sp, FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Field(d.note, { v -> vm.updateDraft { it.copy(note = v.take(500)) } }, "یادداشتت رو اینجا بنویس…", singleLine = false)
                Txt("یادداشت فقط برای خودته و پارتنرت نمی‌بینه.", 12.sp, color = C.Muted, modifier = Modifier.padding(top = 6.dp))
            }
            Panel(padding = 0.dp) {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    SettingRow("وزن", { vm.push(Screen.LogWeight) }, value = d.weight?.let { "${Fa.digits(fmt1(Math.round(Units.kgToDisp(it, wu) * 10) / 10.0))} ${Units.weightLabel(vm)}" })
                    RowDivider()
                    SettingRow("دمای بدن", { vm.push(Screen.LogTemp) }, value = d.temp?.let { "${Fa.digits(fmt1(Math.round(Units.cToDisp(it, tu) * 10) / 10.0))} ${Units.tempLabel(vm)}" })
                    RowDivider()
                    SettingRow("نوشیدن آب", { vm.push(Screen.LogWater) }, value = d.water?.let { "${Fa.n(Units.mlToDisp(it, Units.cupUnit(vm)).toInt())} ${Units.volLabel(vm)}" })
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        Box(Modifier.fillMaxWidth().background(C.Surface).navigationBarsPadding().padding(16.dp)) {
            BigButton("ذخیره", { if (vm.saveDraft()) vm.pop() })
        }
    }
}

private fun <T> Set<T>.toggle(x: T): Set<T> = if (x in this) this - x else this + x

// ============================================================================ weight / temperature
@Composable
fun WeightScreen(vm: AppVM) {
    val d = vm.draft ?: run { vm.pop(); return }
    val s = vm.snap
    val unit = Units.weightUnit(vm)
    val startKg = d.weight ?: s?.body?.weightKg ?: 60.0
    var v by remember { mutableStateOf(Math.round(Units.kgToDisp(startKg, unit) * 10) / 10.0) }
    val (lo, hi) = if (unit == "lb") 70 to 440 else 30 to 200
    Page {
        TopBar("وزن", onBack = { vm.pop() })
        Column(Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Row(verticalAlignment = Alignment.Bottom) {
                Txt(Fa.digits(fmt1(v)), 64.sp, FontWeight.Bold, C.Green, lineHeight = 72.sp)
                Txt(Units.weightLabel(vm), 18.sp, color = C.Muted, modifier = Modifier.padding(start = 8.dp, bottom = 14.dp))
            }
            Spacer(Modifier.height(30.dp))
            RulerPicker(v, lo, hi, { v = it }, scale = 10, color = C.Green)
        }
        Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp)) {
            BigButton("تأیید", { vm.updateDraft { it.copy(weight = Math.round(Units.dispToKg(v, unit) * 10) / 10.0) }; vm.pop() }, color = C.Green)
        }
    }
}

@Composable
fun TempScreen(vm: AppVM) {
    val d = vm.draft ?: run { vm.pop(); return }
    val unit = Units.tempUnit(vm)
    val startC = d.temp ?: 36.6
    var v by remember { mutableStateOf(Math.round(Units.cToDisp(startC, unit) * 10) / 10.0) }
    val (lo, hi) = if (unit == "f") 93 to 108 else 34 to 42
    Page {
        TopBar("دمای بدن", onBack = { vm.pop() })
        Column(Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Row(verticalAlignment = Alignment.Bottom) {
                Txt(Fa.digits(fmt1(v)), 64.sp, FontWeight.Bold, C.Orange, lineHeight = 72.sp)
                Txt(Units.tempLabel(vm), 18.sp, color = C.Muted, modifier = Modifier.padding(start = 8.dp, bottom = 14.dp))
            }
            Spacer(Modifier.height(30.dp))
            RulerPicker(v, lo, hi, { v = it }, scale = 10, color = C.Orange)
        }
        Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp)) {
            BigButton("تأیید", { vm.updateDraft { it.copy(temp = Math.round(Units.dispToC(v, unit) * 10) / 10.0) }; vm.pop() }, color = C.Orange)
        }
    }
}

// ============================================================================ water
@Composable
fun WaterScreen(vm: AppVM) {
    val d = vm.draft ?: run { vm.pop(); return }
    val s = vm.snap
    val goal = s?.body?.waterGoalMl ?: 2400
    val cup = s?.body?.cupMl ?: 200
    val total = d.water ?: 0
    var dialog by remember { mutableStateOf(false) }
    var editGoal by remember { mutableStateOf(false) }
    val frac = (total.toFloat() / goal).coerceIn(0f, 1f)
    val vu = Units.cupUnit(vm)
    fun show(ml: Int) = Fa.n(Math.round(Units.mlToDisp(ml, vu)).toInt())

    if (dialog || editGoal) {
        SettingsDialog(
            title = if (editGoal) "هدف روزانه (میلی‌لیتر)" else "اندازه‌ی لیوان (میلی‌لیتر)",
            initial = if (editGoal) goal else cup,
            min = if (editGoal) 500 else 50, max = if (editGoal) 8000 else 1000,
            onSave = { n -> vm.saveBody { b -> if (editGoal) b.copy(waterGoalMl = n) else b.copy(cupMl = n) }; dialog = false; editGoal = false },
            onDismiss = { dialog = false; editGoal = false },
        )
    }
    Page {
        TopBar("نوشیدن آب", onBack = { vm.pop() }, trailing = {
            Box(Modifier.size(44.dp).pressable(role = null) { dialog = true }, contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Settings, "تنظیمات لیوان", tint = C.Text) }
        })
        Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
            Panel(padding = 20.dp) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    WaterDrop(frac, Modifier.size(width = 190.dp, height = 220.dp))
                    Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 12.dp)) {
                        Txt(show(total), 44.sp, FontWeight.Bold, lineHeight = 50.sp)
                        Txt(Units.volLabel(vm), 15.sp, color = C.Muted, modifier = Modifier.padding(start = 6.dp, bottom = 8.dp))
                    }
                    Row(Modifier.pressable { editGoal = true }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Txt("هدف روزانه: ${show(goal)} ${Units.volLabel(vm)}", 14.sp, color = C.Muted)
                        Icon(Icons.Rounded.Edit, null, tint = C.Muted, modifier = Modifier.padding(start = 6.dp).size(16.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            Modifier.size(52.dp).clip(CircleShape).background(C.SurfaceAlt).pressable { vm.updateDraft { it.copy(water = ((it.water ?: 0) - cup).coerceAtLeast(0).takeIf { n -> n > 0 }) } },
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Rounded.Remove, "کم کن", tint = C.Blue) }
                        BigButton("بنوش (${show(cup)} ${Units.volLabel(vm)})", { vm.updateDraft { it.copy(water = (it.water ?: 0) + cup) } }, Modifier.weight(1f).height(52.dp), color = C.Blue)
                    }
                }
            }
        }
        Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp)) { BigButton("تأیید", { vm.pop() }, color = Color(0xFF1976D2)) }
    }
}

@Composable
private fun WaterDrop(frac: Float, modifier: Modifier) {
    val fill = C.Blue
    val shell = C.SurfaceAlt
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val p = Path().apply {
            moveTo(w / 2, h * 0.02f)
            cubicTo(w * 0.62f, h * 0.2f, w * 0.97f, h * 0.45f, w * 0.97f, h * 0.68f)
            cubicTo(w * 0.97f, h * 0.88f, w * 0.76f, h * 0.99f, w / 2, h * 0.99f)
            cubicTo(w * 0.24f, h * 0.99f, w * 0.03f, h * 0.88f, w * 0.03f, h * 0.68f)
            cubicTo(w * 0.03f, h * 0.45f, w * 0.38f, h * 0.2f, w / 2, h * 0.02f)
            close()
        }
        drawPath(p, shell)
        if (frac > 0f) {
            val level = h * (1f - frac)
            val rect = Path().apply { addRect(androidx.compose.ui.geometry.Rect(0f, level, w, h)) }
            val clipped = Path().apply { op(p, rect, PathOperation.Intersect) }
            drawPath(clipped, fill.copy(alpha = 0.85f))
        }
    }
}

/** Small "type a number" dialog used by several settings. */
@Composable
fun SettingsDialog(title: String, initial: Int, min: Int, max: Int, onSave: (Int) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(initial.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = C.Surface,
        title = { Txt(title, 18.sp, FontWeight.Bold) },
        text = { Field(text, { t -> text = t.filter { it.isDigit() }.take(5) }, "عدد", keyboardType = KeyboardType.Number, ltr = true) },
        confirmButton = {
            TextButton(onClick = { text.toIntOrNull()?.takeIf { it in min..max }?.let(onSave) }) { Txt("ذخیره", 16.sp, FontWeight.Bold, C.Pink) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Txt("لغو", 16.sp, color = C.Muted) } },
    )
}
