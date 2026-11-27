package dev.imhamid.period.ui

import kotlinx.serialization.json.put
import android.app.TimePickerDialog
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate

/** First day of the week: java.time DayOfWeek value (6 = Saturday, 7 = Sunday, 1 = Monday). */
object WeekCfg {
    var start by androidx.compose.runtime.mutableIntStateOf(6)
}

private val letterByDow = mapOf(6 to "ش", 7 to "ی", 1 to "د", 2 to "س", 3 to "چ", 4 to "پ", 5 to "ج")
fun weekdayLetter(i: Int): String = letterByDow[(WeekCfg.start - 1 + i) % 7 + 1] ?: ""

@Composable
private fun SettingsPage(vm: AppVM, title: String, content: @Composable () -> Unit) {
    Page {
        TopBar(title, onBack = { vm.pop() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            content()
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun StepRow(label: String, value: Int, min: Int, max: Int, format: (Int) -> String, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Txt(label, 16.sp, FontWeight.Medium, modifier = Modifier.weight(1f))
        Box(Modifier.size(38.dp).clip(CircleShape).background(C.SurfaceAlt).pressable { if (value > min) onChange(value - 1) }, contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Remove, "کمتر", tint = C.Text) }
        Txt(format(value), 15.sp, FontWeight.Bold, modifier = Modifier.width(78.dp), align = androidx.compose.ui.text.style.TextAlign.Center)
        Box(Modifier.size(38.dp).clip(CircleShape).background(C.SurfaceAlt).pressable { if (value < max) onChange(value + 1) }, contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Add, "بیشتر", tint = C.Text) }
    }
}

/** A row that cycles through [options] (value, label) in a small dialog-free way: tap to open a choice list. */
@Composable
private fun ChoiceRow(title: String, current: String, options: List<Pair<String, String>>, onPick: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    SettingRow(title, { open = true }, value = options.firstOrNull { it.first == current }?.second ?: current)
    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            containerColor = C.Surface,
            title = { Txt(title, 18.sp, FontWeight.Bold) },
            text = {
                Column {
                    options.forEach { (v, l) ->
                        Row(Modifier.fillMaxWidth().pressable { onPick(v); open = false }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Txt(l, 16.sp, if (v == current) FontWeight.Bold else FontWeight.Normal, if (v == current) C.Pink else C.Text, Modifier.weight(1f))
                            if (v == current) Icon(Icons.Rounded.Check, null, tint = C.Pink)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { open = false }) { Txt("بستن", 15.sp, color = C.Muted) } },
        )
    }
}

// ============================================================================ personal info
@Composable
fun PersonalInfoScreen(vm: AppVM) {
    val s = vm.snap ?: return
    val me = s.me
    val body = s.body
    var name by remember { mutableStateOf(me.name.orEmpty()) }
    var phone by remember { mutableStateOf(body?.phone.orEmpty()) }
    var gender by remember { mutableStateOf(body?.gender ?: "female") }
    var birth by remember { mutableStateOf(body?.birthDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }) }
    var height by remember { mutableStateOf(body?.heightCm?.let { fmt1(it) }.orEmpty()) }
    var weight by remember { mutableStateOf(body?.weightKg?.let { fmt1(it) }.orEmpty()) }
    var pickDate by remember { mutableStateOf(false) }
    var tmpDate by remember { mutableStateOf(birth ?: LocalDate.now().minusYears(25)) }
    val isHer = me.role == "her"

    if (pickDate) {
        AlertDialog(
            onDismissRequest = { pickDate = false },
            containerColor = C.Surface,
            title = { Txt("تاریخ تولد", 18.sp, FontWeight.Bold) },
            text = {
                val nowY = Fa.jalaliParts(LocalDate.now()).first
                JalaliDatePicker(tmpDate, nowY - 80, nowY - 5, { tmpDate = it })
            },
            confirmButton = { TextButton(onClick = { birth = tmpDate; pickDate = false }) { Txt("تأیید", 16.sp, FontWeight.Bold, C.Pink) } },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Txt("لغو", 16.sp, color = C.Muted) } },
        )
    }

    SettingsPage(vm, "اطلاعات شخصی") {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(Modifier.size(96.dp).clip(CircleShape).background(C.PinkSoft), contentAlignment = Alignment.Center) {
                Txt(name.take(1).ifBlank { "؟" }, 40.sp, FontWeight.Bold, C.Pink)
            }
        }
        Field(name, { name = it.take(30) }, "نام")
        Column {
            Txt("ایمیل", 13.sp, color = C.Muted, modifier = Modifier.padding(horizontal = 4.dp))
            Spacer(Modifier.height(6.dp))
            Box(Modifier.fillMaxWidth().height(58.dp).clip(RoundedCornerShape(20.dp)).background(C.SurfaceAlt).padding(horizontal = 18.dp), contentAlignment = Alignment.CenterStart) {
                Txt((vm.store.email ?: me.email).orEmpty(), 16.sp, color = C.Muted, maxLines = 1)
            }
        }
        if (isHer) {
            Field(phone, { phone = it.filter { c -> c.isDigit() || c == '+' }.take(16) }, "شماره تلفن (اختیاری)", keyboardType = KeyboardType.Phone, ltr = true)
            Column {
                Txt("جنسیت", 13.sp, color = C.Muted, modifier = Modifier.padding(horizontal = 4.dp))
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("female" to "زن", "male" to "مرد", "other" to "ترجیح می‌دم نگم").forEach { (id, l) -> FilterChip(l, gender == id) { gender = id } }
                }
            }
            Column {
                Txt("تاریخ تولد", 13.sp, color = C.Muted, modifier = Modifier.padding(horizontal = 4.dp))
                Spacer(Modifier.height(6.dp))
                Box(Modifier.fillMaxWidth().height(58.dp).clip(RoundedCornerShape(20.dp)).background(C.Surface).pressable { pickDate = true }.padding(horizontal = 18.dp), contentAlignment = Alignment.CenterStart) {
                    Txt(birth?.let { Fa.date(it, "d MMMM yyyy") } ?: "انتخاب کن", 16.sp, color = if (birth == null) C.Muted else C.Text)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Field(height, { height = it.filter { c -> c.isDigit() || c == '.' }.take(5) }, "قد (سانتی‌متر)", Modifier.weight(1f), keyboardType = KeyboardType.Decimal, ltr = true)
                Field(weight, { weight = it.filter { c -> c.isDigit() || c == '.' }.take(5) }, "وزن (کیلوگرم)", Modifier.weight(1f), keyboardType = KeyboardType.Decimal, ltr = true)
            }
            Txt("این اطلاعات خصوصیه و فقط برای خودت دیده می‌شه.", 12.sp, color = C.Muted)
        }
        BigButton("ذخیره", {
            if (name.isBlank()) { vm.toast("اسمت رو بنویس"); return@BigButton }
            if (name.trim() != me.name) vm.saveName(name)
            if (isHer) {
                val h = height.toDoubleOrNull()?.takeIf { it in 80.0..250.0 }
                val w = weight.toDoubleOrNull()?.takeIf { it in 20.0..400.0 }
                vm.saveBody { it.copy(phone = phone.ifBlank { null }, gender = gender, birthDate = birth?.toString(), heightCm = h, weightKg = w) }
            }
            vm.toast("ذخیره شد")
            vm.pop()
        })
    }
}

// ============================================================================ preferences
@Composable
fun PreferencesScreen(vm: AppVM) {
    val ctx = LocalContext.current
    val s = vm.snap
    val goal = s?.body?.waterGoalMl ?: 2400
    var goalDlg by remember { mutableStateOf(false) }
    var cacheMb by remember { mutableStateOf(cacheSizeMb(ctx)) }
    if (goalDlg) SettingsDialog("هدف آب روزانه (میلی‌لیتر)", goal, 500, 8000, { n -> vm.saveBody { it.copy(waterGoalMl = n) }; goalDlg = false }, { goalDlg = false })

    SettingsPage(vm, "تنظیمات") {
        Panel(padding = 0.dp) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                ChoiceRow("یکای وزن", vm.pref("wunit", "kg"), listOf("kg" to "کیلوگرم", "lb" to "پوند")) { vm.setPref("wunit", it) }
                RowDivider()
                ChoiceRow("یکای قد", vm.pref("hunit", "cm"), listOf("cm" to "سانتی‌متر", "ft" to "اینچ")) { vm.setPref("hunit", it) }
                RowDivider()
                ToggleRow("شاخص توده بدنی (BMI)", vm.prefBool("bmi", true), { vm.setPref("bmi", it.toString()) })
            }
        }
        Panel(padding = 0.dp) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                ChoiceRow("یکای دما", vm.pref("tunit", "c"), listOf("c" to "سانتی‌گراد (°C)", "f" to "فارنهایت (°F)")) { vm.setPref("tunit", it) }
            }
        }
        Panel(padding = 0.dp) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                SettingRow("هدف آب روزانه", { goalDlg = true }, value = "${Fa.n(goal)} میلی‌لیتر")
                RowDivider()
                ChoiceRow("یکای حجم", vm.pref("vunit", "ml"), listOf("ml" to "میلی‌لیتر", "oz" to "اونس")) { vm.setPref("vunit", it) }
            }
        }
        Panel(padding = 0.dp) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                ChoiceRow("اولین روز هفته", vm.pref("wk", "6"), listOf("6" to "شنبه", "7" to "یکشنبه", "1" to "دوشنبه")) { vm.setPref("wk", it); WeekCfg.start = it.toInt() }
                RowDivider()
                ChoiceRow("قالب ساعت", vm.pref("t24", "true"), listOf("true" to "۲۴ ساعته", "false" to "۱۲ ساعته")) { vm.setPref("t24", it) }
            }
        }
        Panel(padding = 0.dp) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                SettingRow("پاک کردن حافظه‌ی موقت", {
                    ctx.cacheDir.deleteRecursively()
                    vm.store.articlesJson = null
                    vm.loadArticles()
                    cacheMb = cacheSizeMb(ctx)
                    vm.toast("حافظه‌ی موقت پاک شد")
                }, value = "${Fa.digits(String.format(java.util.Locale.US, "%.1f", cacheMb))} مگابایت")
            }
        }
    }
}

private fun cacheSizeMb(ctx: android.content.Context): Double =
    ctx.cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() } / 1048576.0

// ============================================================================ cycle settings
@Composable
fun CycleSettingsScreen(vm: AppVM) {
    val me = vm.snap?.me ?: return
    SettingsPage(vm, "تنظیمات چرخه") {
        Panel {
            StepRow("طول چرخه", me.cycleLength, 20, 45, { "${Fa.n(it)} روز" }) { v -> vm.patch { put("cycle_length", v) } }
            RowDivider()
            StepRow("طول پریود", me.periodLength, 2, 10, { "${Fa.n(it)} روز" }) { v -> vm.patch { put("period_length", v) } }
            RowDivider()
            StepRow("روزهای PMS", me.pmsDays, 1, 10, { "${Fa.n(it)} روز" }) { v -> vm.patch { put("pms_days", v) } }
        }
        Panel {
            Txt("نظم چرخه", 16.sp, FontWeight.Medium)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("منظم", "کمی بالا و پایین", "نامنظم").forEachIndexed { i, t -> FilterChip(t, me.regularity == i) { vm.patch { put("regularity", i) } } }
            }
            Spacer(Modifier.height(6.dp))
            RowDivider()
            ToggleRow("روش هورمونی", me.hormonal, { v -> vm.patch { put("hormonal", v) } }, "قرص، آی‌یودی، آمپول… (باروری و تخمک‌گذاری نمایش داده نمی‌شه)")
        }
    }
}

// ============================================================================ reminder
@Composable
fun ReminderScreen(vm: AppVM) {
    val ctx = LocalContext.current
    val on = vm.prefBool("rem_on", false)
    val minutes = vm.pref("rem_min", "540").toIntOrNull() ?: 540
    val vib = vm.prefBool("rem_vib", true)
    val toneUri = vm.pref("rem_tone", "")
    val is24 = vm.prefBool("t24", true)
    val toneName = remember(toneUri) {
        if (toneUri.isBlank()) "پیش‌فرض" else runCatching { RingtoneManager.getRingtone(ctx, Uri.parse(toneUri))?.getTitle(ctx) }.getOrNull() ?: "سفارشی"
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        @Suppress("DEPRECATION")
        val uri = r.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        if (r.resultCode == android.app.Activity.RESULT_OK) vm.setPref("rem_tone", uri?.toString().orEmpty())
    }
    fun timeText(m: Int): String {
        val h = m / 60
        val mm = m % 60
        return if (is24) Fa.digits(String.format(java.util.Locale.US, "%02d:%02d", h, mm))
        else Fa.digits(String.format(java.util.Locale.US, "%d:%02d", if (h % 12 == 0) 12 else h % 12, mm)) + if (h < 12) " ق.ظ" else " ب.ظ"
    }
    SettingsPage(vm, "یادآور") {
        Panel(padding = 0.dp) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                ToggleRow("یادآور ثبت روزانه", on, { vm.setPref("rem_on", it.toString()) }, "اگه امروز چیزی ثبت نکرده باشی یادت می‌ندازه")
                AnimatedVisibility(on) {
                    Column {
                        RowDivider()
                        SettingRow("ساعت یادآور", {
                            TimePickerDialog(ctx, { _, h, m -> vm.setPref("rem_min", (h * 60 + m).toString()) }, minutes / 60, minutes % 60, is24).show()
                        }, value = timeText(minutes))
                        RowDivider()
                        SettingRow("آهنگ زنگ", {
                            val i = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, if (toneUri.isBlank()) null else Uri.parse(toneUri))
                            }
                            picker.launch(i)
                        }, value = toneName)
                        RowDivider()
                        ToggleRow("لرزش", vib, { vm.setPref("rem_vib", it.toString()) })
                    }
                }
            }
        }
        Txt("صدای اعلان از تنظیمات صدای خود گوشی کنترل می‌شه.", 12.sp, color = C.Muted)
    }
}

// ============================================================================ notifications (cycle events)
@Composable
fun NotificationsScreen(vm: AppVM) {
    val s = vm.snap ?: return
    val me = s.me
    val isHer = me.role == "her"
    SettingsPage(vm, "اعلان‌ها") {
        Panel(padding = 0.dp) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                ToggleRow("فعال بودن اعلان‌ها", me.notifyEnabled, { v -> vm.patch { put("notify_enabled", v) } }, "کلید اصلی همه‌ی نوتیف‌ها")
                AnimatedVisibility(me.notifyEnabled) {
                    Column {
                        RowDivider()
                        StepRow("ساعت یادآوری", me.notifyHour, 5, 23, { Fa.hour(it) }) { v -> vm.patch { put("notify_hour", v) } }
                        RowDivider()
                        ToggleRow("PMS", me.notifyPms, { v -> vm.patch { put("notify_pms", v) } }, "یک روز قبل و روز شروع")
                        RowDivider()
                        ToggleRow("پریود", me.notifyPeriod, { v -> vm.patch { put("notify_period", v) } }, "دو روز قبل و روز پریود")
                        if (s.owner?.hormonal != true) {
                            RowDivider()
                            ToggleRow("تخمک‌گذاری", me.notifyOvulation, { v -> vm.patch { put("notify_ovulation", v) } }, "روز تخمین‌زده‌شده")
                        }
                        RowDivider()
                        ToggleRow("تأخیر پریود", me.notifyLate, { v -> vm.patch { put("notify_late", v) } }, "اگه از موعد دیرتر شد")
                    }
                }
            }
        }
        if (!isHer) Txt("حال‌وهوایی که ثبت می‌شه هم با همین کلید اصلی برات اعلان می‌شه.", 12.sp, color = C.Muted)
    }
}

// ============================================================================ security
@Composable
fun SecurityScreen(vm: AppVM) {
    val ctx = LocalContext.current
    var confirmAll by remember { mutableStateOf(false) }
    if (confirmAll) {
        ConfirmDialog(
            title = "خروج از همه‌ی دستگاه‌ها؟",
            text = "حساب‌ت روی همه‌ی گوشی‌ها (از جمله همین گوشی) خارج می‌شه.",
            confirm = "خروج",
            onConfirm = { confirmAll = false; vm.logoutEverywhere() },
            onDismiss = { confirmAll = false },
        )
    }
    SettingsPage(vm, "حساب و امنیت") {
        Panel(padding = 0.dp) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                ToggleRow("قفل با اثر انگشت یا چهره", vm.lockEnabled, { v ->
                    val act = ctx.findFragmentActivity()
                    if (act == null || !Biometrics.available(ctx)) vm.toast("اول روی گوشی قفل صفحه یا اثر انگشت تنظیم کن")
                    else Biometrics.prompt(act, if (v) "فعال‌سازی قفل" else "غیرفعال‌سازی قفل", { vm.setLock(v) })
                }, "با قفل فعال، محتوا از اسکرین‌شات و Recents پنهان می‌شه")
                RowDivider()
                SettingRow("ورود دومرحله‌ای پیامکی", null, value = "به‌زودی", sub = "نیاز به سرویس پیامک دارد")
                RowDivider()
                SettingRow("Google Authenticator", null, value = "به‌زودی")
                RowDivider()
                SettingRow("تغییر رمز عبور", { vm.push(Screen.ChangePassword) })
                RowDivider()
                SettingRow("مدیریت دستگاه‌ها", { confirmAll = true }, sub = "خروج از حساب روی همه‌ی دستگاه‌ها")
            }
        }
    }
}

@Composable
fun ChangePasswordScreen(vm: AppVM) {
    var p1 by remember { mutableStateOf("") }
    var p2 by remember { mutableStateOf("") }
    SettingsPage(vm, "تغییر رمز عبور") {
        Field(p1, { p1 = it }, "رمز جدید (حداقل ۸ کاراکتر)", password = true)
        Field(p2, { p2 = it }, "تکرار رمز جدید", password = true, imeAction = androidx.compose.ui.text.input.ImeAction.Done, onDone = { vm.changePassword(p1, p2) })
        BigButton("ذخیره", { vm.changePassword(p1, p2) }, busy = vm.busy)
    }
}

// ============================================================================ linked accounts
@Composable
fun LinkedScreen(vm: AppVM) {
    SettingsPage(vm, "حساب‌های متصل") {
        Panel(padding = 0.dp) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                SettingRow("Google", null, value = "به‌زودی")
                RowDivider()
                SettingRow("Apple", null, value = "به‌زودی")
                RowDivider()
                SettingRow("Facebook", null, value = "به‌زودی")
            }
        }
        Txt("ورود با حساب‌های دیگه نیاز به راه‌اندازی در Supabase داره و فعلاً فعال نیست.", 12.sp, color = C.Muted)
    }
}

// ============================================================================ data & analytics
@Composable
fun DataScreen(vm: AppVM) {
    val ctx = LocalContext.current
    var info by remember { mutableStateOf<String?>(null) }
    info?.let { txt ->
        AlertDialog(
            onDismissRequest = { info = null },
            containerColor = C.Surface,
            text = { Txt(txt, 15.sp, lineHeight = 24.sp) },
            confirmButton = { TextButton(onClick = { info = null }) { Txt("باشه", 16.sp, FontWeight.Bold, C.Pink) } },
        )
    }
    SettingsPage(vm, "داده و تحلیل") {
        Panel(padding = 0.dp) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                SettingRow("نحوه‌ی استفاده از داده", { info = "فقط چیزهایی ذخیره می‌شه که خودت ثبت می‌کنی. هیچ ابزار تحلیل یا ردیابی تبلیغاتی داخل لونا نیست." }, sub = "چه چیزی ذخیره می‌شه و چرا")
                RowDivider()
                SettingRow("تبلیغات", { info = "لونا هیچ تبلیغی نداره و اطلاعات تو رو برای تبلیغات استفاده یا فروش نمی‌کنه." }, sub = "لونا تبلیغ ندارد")
                RowDivider()
                SettingRow("دانلود داده‌هام", {
                    val json = vm.exportJson()
                    val i = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "داده‌های لونا")
                        putExtra(Intent.EXTRA_TEXT, json)
                    }
                    ctx.startActivity(Intent.createChooser(i, "ذخیره یا ارسال داده‌ها"))
                }, sub = "یک نسخه‌ی کامل از همه‌ی داده‌هات (JSON)")
            }
        }
    }
}

// ============================================================================ appearance
@Composable
fun AppearanceScreen(vm: AppVM) {
    SettingsPage(vm, "ظاهر برنامه") {
        Panel(padding = 0.dp) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                SettingRow("تم", { vm.push(Screen.ThemeScreen) }, value = listOf("مثل سیستم", "روشن", "تیره")[vm.themeMode.coerceIn(0, 2)])
                RowDivider()
                SettingRow("زبان", { vm.push(Screen.Language) }, value = "فارسی")
            }
        }
    }
}

@Composable
fun ThemeScreen(vm: AppVM) {
    SettingsPage(vm, "تم") {
        Panel(padding = 0.dp) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                listOf("مثل سیستم", "روشن", "تیره").forEachIndexed { i, t ->
                    if (i > 0) RowDivider()
                    Row(Modifier.fillMaxWidth().pressable { vm.setTheme(i) }.padding(vertical = 15.dp), verticalAlignment = Alignment.CenterVertically) {
                        Txt(t, 16.sp, FontWeight.Medium, modifier = Modifier.weight(1f))
                        Box(Modifier.size(24.dp).clip(CircleShape).border(2.dp, if (vm.themeMode == i) C.Pink else C.Track, CircleShape), contentAlignment = Alignment.Center) {
                            if (vm.themeMode == i) Box(Modifier.size(12.dp).clip(CircleShape).background(C.Pink))
                        }
                    }
                }
            }
        }
        Panel {
            Txt("رنگ برنامه", 16.sp, FontWeight.Medium)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Accents.forEachIndexed { i, a ->
                    Box(
                        Modifier.size(42.dp).clip(CircleShape).background(a.primary).then(if (vm.accent == i) Modifier.border(3.dp, C.Text, CircleShape) else Modifier).pressable { vm.chooseAccent(i) },
                        contentAlignment = Alignment.Center,
                    ) { if (vm.accent == i) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(20.dp)) }
                }
            }
            Txt(Accents[vm.accent.coerceIn(0, Accents.lastIndex)].name, 12.sp, color = C.Muted, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
fun LanguageScreen(vm: AppVM) {
    SettingsPage(vm, "زبان") {
        Panel(padding = 0.dp) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Row(Modifier.fillMaxWidth().padding(vertical = 15.dp), verticalAlignment = Alignment.CenterVertically) {
                    Txt("فارسی", 16.sp, FontWeight.Bold, C.Pink, Modifier.weight(1f))
                    Icon(Icons.Rounded.Check, null, tint = C.Pink)
                }
                listOf("English", "العربية", "Türkçe").forEach { l ->
                    RowDivider()
                    SettingRow(l, null, value = "به‌زودی")
                }
            }
        }
    }
}

// ============================================================================ help & support
@Composable
fun HelpScreen(vm: AppVM) {
    SettingsPage(vm, "راهنما و پشتیبانی") {
        Panel(padding = 0.dp) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                SettingRow("سؤالات متداول", { vm.push(Screen.Faq) })
                RowDivider()
                SettingRow("تماس با پشتیبانی", { vm.push(Screen.Contact) })
                RowDivider()
                SettingRow("سیاست حریم خصوصی", { vm.push(Screen.Privacy) })
                RowDivider()
                SettingRow("شرایط استفاده", { vm.push(Screen.Terms) })
            }
        }
    }
}

@Composable
fun FaqScreen(vm: AppVM) {
    var open by remember { mutableStateOf(-1) }
    SettingsPage(vm, "سؤالات متداول") {
        Content.faqs.forEachIndexed { i, f ->
            Panel(Modifier.pressable { open = if (open == i) -1 else i }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Txt(f.q, 16.sp, FontWeight.Bold, modifier = Modifier.weight(1f))
                    Icon(if (open == i) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown, null, tint = C.Pink)
                }
                AnimatedVisibility(open == i) { Txt(f.a, 15.sp, color = C.Muted, lineHeight = 24.sp, modifier = Modifier.padding(top = 8.dp)) }
            }
        }
    }
}

@Composable
fun ContactScreen(vm: AppVM) {
    var subject by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf("") }
    SettingsPage(vm, "تماس با پشتیبانی") {
        Txt("مشکل یا پیشنهادت رو بنویس؛ پیامت مستقیم برای ما ارسال می‌شه.", 14.sp, color = C.Muted)
        Field(subject, { subject = it.take(120) }, "موضوع")
        Field(msg, { msg = it.take(2000) }, "پیام", singleLine = false)
        BigButton("ارسال", { vm.sendTicket(subject, msg) { vm.pop() } }, busy = vm.busy)
    }
}

@Composable
fun TextPageScreen(vm: AppVM, title: String, text: String) {
    SettingsPage(vm, title) {
        Panel { text.split("\n\n").forEach { Txt(it, 15.sp, lineHeight = 26.sp, modifier = Modifier.padding(bottom = 10.dp)) } }
    }
}
