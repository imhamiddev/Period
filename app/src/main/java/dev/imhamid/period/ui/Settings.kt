package dev.imhamid.period.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.imhamid.period.BuildConfig
import dev.imhamid.period.data.PairRow
import kotlinx.serialization.json.put

// ============================================================================ settings tab
@Composable
internal fun SettingsTab(vm: AppVM) {
    val s = vm.snap ?: return
    val me = s.me
    val isHer = me.role == "her"
    val ctx = LocalContext.current
    var editName by remember { mutableStateOf(false) }
    var nameText by remember(me.name) { mutableStateOf(me.name.orEmpty()) }

    Column(Modifier.fillMaxSize().background(C.Paper).statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(20.dp))
        Reveal(0) { Txt("تنظیمات", 32.sp, FontWeight.Light) }
        Spacer(Modifier.height(18.dp))

        Column(verticalArrangement = Arrangement.spacedBy(22.dp)) {
            // ---------------------------------------------------------------- profile
            Reveal(1) {
                Card {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(56.dp).clip(CircleShape).background(C.Lime), contentAlignment = Alignment.Center) {
                                Txt(me.name.orEmpty().take(1), 24.sp, FontWeight.Bold, C.Deep)
                            }
                            Spacer(Modifier.size(14.dp))
                            Column(Modifier.weight(1f)) {
                                Txt(me.name.orEmpty(), 18.sp, FontWeight.Medium)
                                Txt(me.email.orEmpty(), 12.sp, color = C.Muted, maxLines = 1)
                                Spacer(Modifier.height(4.dp))
                                RoleBadge(if (isHer) "ثبت‌کننده‌ی چرخه" else "پارتنر")
                            }
                            Txt(
                                if (editName) "بستن" else "ویرایش", 13.sp, FontWeight.Medium, C.Green,
                                Modifier.pressable { editName = !editName }.padding(8.dp),
                            )
                        }
                        androidx.compose.animation.AnimatedVisibility(editName) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Field(nameText, { nameText = it }, "نام")
                                PrimaryButton("ذخیره", { vm.saveName(nameText); editName = false }, loading = vm.busy)
                            }
                        }
                    }
                }
            }

            // ---------------------------------------------------------------- cycle (her)
            if (isHer) Reveal(2) {
                SettingsSection(Icons.Rounded.WaterDrop, "چرخه", "پایه‌ی همه‌ی پیش‌بینی‌ها") {
                    SettingStepper("طول چرخه", me.cycleLength, 20, 45, { "${Fa.n(it)} روز" }) { v -> vm.patch { put("cycle_length", v) } }
                    SettingsDivider()
                    SettingStepper("طول پریود", me.periodLength, 2, 10, { "${Fa.n(it)} روز" }) { v -> vm.patch { put("period_length", v) } }
                    SettingsDivider()
                    SettingStepper("روزهای PMS", me.pmsDays, 1, 10, { "${Fa.n(it)} روز" }) { v -> vm.patch { put("pms_days", v) } }
                    SettingsDivider()
                    Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Txt("نظم چرخه", 16.sp, FontWeight.Medium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("منظم", "کمی بالا و پایین", "نامنظم").forEachIndexed { i, t ->
                                Chip(t, me.regularity == i, { vm.patch { put("regularity", i) } })
                            }
                        }
                    }
                    SettingsDivider()
                    SettingSwitch("روش هورمونی", "قرص، آی‌یودی، آمپول… (باروری و تخمک‌گذاری نمایش داده نمی‌شه)", me.hormonal) { v -> vm.patch { put("hormonal", v) } }
                }
            }

            // ---------------------------------------------------------------- partner
            if (isHer) Reveal(3) {
                SettingsSection(Icons.Rounded.Favorite, "پارتنر", "اشتراک‌گذاری و اتصال") {
                    SettingSwitch("اشتراک با پارتنر", "اگه خاموش باشه پارتنرت چیزی نمی‌بینه", me.shareEnabled) { v -> vm.patch { put("share_enabled", v) } }
                    if (s.pairs.isNotEmpty()) {
                        SettingsDivider()
                        s.pairs.forEachIndexed { i, p ->
                            if (i > 0) SettingsDivider()
                            PairLine(vm, p, s.others[p.partnerId]?.name ?: "پارتنر", editable = true)
                        }
                    } else {
                        SettingsDivider()
                        Txt("هنوز پارتنری وصل نشده؛ کد اتصالت تو صفحه‌ی خانه‌ست.", 12.sp, color = C.Muted, modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp))
                    }
                }
            } else if (s.pairs.isNotEmpty()) Reveal(3) {
                SettingsSection(Icons.Rounded.Favorite, "اتصال", "ارتباط با ثبت‌کننده‌ی چرخه") {
                    s.pairs.forEachIndexed { i, p ->
                        if (i > 0) SettingsDivider()
                        PairLine(vm, p, s.others[p.herId]?.name ?: "درخواست ارسال‌شده", editable = false)
                    }
                }
            }

            // ---------------------------------------------------------------- notifications
            Reveal(4) {
                SettingsSection(Icons.Rounded.Notifications, "نوتیفیکیشن", if (isHer) "یادآوری PMS و پریود" else "خبر PMS و پریود پارتنرت") {
                    SettingSwitch("فعال بودن اعلان‌ها", "کلید اصلی همه‌ی نوتیف‌ها", me.notifyEnabled) { v -> vm.patch { put("notify_enabled", v) } }
                    SettingsDivider()
                    SettingSwitch("اعلان فوری پیام‌ها", "پیام‌ها تا حدود ۲۰ ثانیه بعد می‌رسن؛ یه اعلان کوچیک ثابت نشون می‌ده (فقط روی همین گوشی)", vm.liveNotify) { v -> vm.chooseLiveNotify(v) }
                    androidx.compose.animation.AnimatedVisibility(me.notifyEnabled) {
                        Column {
                            SettingsDivider()
                            SettingStepper("ساعت یادآوری", me.notifyHour, 5, 23, { Fa.hour(it) }) { v -> vm.patch { put("notify_hour", v) } }
                            SettingsDivider()
                            SettingSwitch("PMS", "یک روز قبل و روز شروع", me.notifyPms) { v -> vm.patch { put("notify_pms", v) } }
                            SettingsDivider()
                            SettingSwitch("پریود", "دو روز قبل و روز پریود", me.notifyPeriod) { v -> vm.patch { put("notify_period", v) } }
                            if (s.owner?.hormonal != true) {
                                SettingsDivider()
                                SettingSwitch("تخمک‌گذاری", "روز تخمین‌زده‌شده", me.notifyOvulation) { v -> vm.patch { put("notify_ovulation", v) } }
                            }
                            SettingsDivider()
                            SettingSwitch("تأخیر پریود", "اگه از موعد دیرتر شد", me.notifyLate) { v -> vm.patch { put("notify_late", v) } }
                        }
                    }
                }
            }

            // ---------------------------------------------------------------- appearance
            Reveal(5) {
                SettingsSection(Icons.Rounded.Palette, "ظاهر") {
                    Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Txt("حالت نمایش", 16.sp, FontWeight.Medium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("مثل سیستم", "روشن", "تیره").forEachIndexed { i, t ->
                                Chip(t, vm.themeMode == i, { vm.setTheme(i) })
                            }
                        }
                    }
                    SettingsDivider()
                    Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Txt("رنگ برنامه", 16.sp, FontWeight.Medium)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Accents.forEachIndexed { i, a ->
                                Box(
                                    Modifier.size(40.dp).clip(CircleShape).background(a.green)
                                        .then(if (vm.accent == i) Modifier.border(3.dp, C.Text, CircleShape) else Modifier)
                                        .pressable { vm.chooseAccent(i) },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (vm.accent == i) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                        Txt(Accents[vm.accent.coerceIn(0, Accents.lastIndex)].name, 12.sp, color = C.Muted)
                    }
                }
            }

            // ---------------------------------------------------------------- security
            Reveal(5) {
                SettingsSection(Icons.Rounded.Lock, "امنیت") {
                    SettingSwitch("قفل برنامه", "اثر انگشت، Face یا قفل گوشی", vm.lockEnabled) { v ->
                        val act = ctx.findFragmentActivity()
                        if (act == null || !Biometrics.available(ctx)) {
                            vm.error = "اول روی گوشی قفل صفحه یا اثر انگشت تنظیم کن"
                        } else {
                            Biometrics.prompt(act, if (v) "فعال‌سازی قفل" else "غیرفعال‌سازی قفل", { vm.setLock(v) })
                        }
                    }
                }
            }

            // ---------------------------------------------------------------- about / account
            Reveal(6) {
                SettingsSection(Icons.Rounded.Info, "درباره و حساب") {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Txt("نسخه‌ی برنامه", 16.sp, modifier = Modifier.weight(1f))
                            Txt(Fa.digits(BuildConfig.VERSION_NAME), 15.sp, FontWeight.Medium, C.Green)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Txt("همگام‌سازی", 16.sp, modifier = Modifier.weight(1f))
                            Txt(
                                if (vm.offline) "آفلاین" else if (vm.pendingChanges > 0) "در حال ارسال" else "به‌روز",
                                15.sp, FontWeight.Medium, if (vm.offline) C.Amber else C.Green,
                            )
                        }
                        if (vm.pendingChanges > 0) Txt("${Fa.n(vm.pendingChanges)} تغییر روی گوشی منتظر ارسال به سرور", 12.sp, color = C.Muted)
                        PrimaryButton("همگام‌سازی حالا", { vm.refresh() }, kind = BtnKind.Light, modifier = Modifier.border(1.dp, C.Line, RoundedCornerShape(29.dp)))
                        PrimaryButton("بررسی آپدیت", { vm.checkUpdate() }, kind = BtnKind.Lime)
                        PrimaryButton(
                            "خروج از حساب", { vm.logout() }, kind = BtnKind.Light,
                            modifier = Modifier.border(1.dp, C.Line, RoundedCornerShape(29.dp)),
                            icon = { Icon(Icons.AutoMirrored.Rounded.Logout, null, tint = C.Text, modifier = Modifier.size(20.dp)) },
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(130.dp))
    }
}

// ---------------------------------------------------------------- building blocks
@Composable
private fun SettingsSection(icon: ImageVector, title: String, subtitle: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(30.dp).clip(RoundedCornerShape(10.dp)).background(C.LimeSoft), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = C.OnSoft, modifier = Modifier.size(17.dp))
            }
            Spacer(Modifier.size(10.dp))
            Txt(title, 15.sp, FontWeight.Medium)
            if (subtitle != null) Txt("  ·  $subtitle", 12.sp, color = C.Muted, maxLines = 1)
        }
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(C.Surface)
                .border(1.dp, C.Line, RoundedCornerShape(26.dp)),
            content = content,
        )
    }
}

@Composable
private fun SettingsDivider() {
    Box(Modifier.fillMaxWidth().padding(horizontal = 18.dp).height(1.dp).background(C.Line))
}

@Composable
private fun RoleBadge(text: String) {
    Box(Modifier.clip(RoundedCornerShape(10.dp)).background(C.LimeSoft).padding(horizontal = 10.dp, vertical = 3.dp)) {
        Txt(text, 11.sp, FontWeight.Medium, C.OnSoft)
    }
}

@Composable
private fun PairLine(vm: AppVM, p: PairRow, name: String, editable: Boolean) {
    val pending = p.status == "pending"
    val label = vm.labelOf(p)
    var open by remember(p.id) { mutableStateOf(false) }
    var text by remember(p.id, label) { mutableStateOf(label.orEmpty()) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(38.dp).clip(CircleShape).background(C.Lime), contentAlignment = Alignment.Center) {
                Txt(name.take(1), 15.sp, FontWeight.Bold, C.Deep)
            }
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Txt(name, 16.sp, FontWeight.Medium)
                    if (label != null) {
                        Spacer(Modifier.size(8.dp))
                        RoleBadge(label)
                    }
                }
                Txt(if (pending) "منتظر تایید" else "وصل است", 12.sp, color = if (pending) C.Amber else C.Green)
            }
            if (editable) {
                Txt(
                    if (open) "بستن" else "نقش", 13.sp, FontWeight.Medium, C.Green,
                    Modifier.pressable { open = !open }.padding(8.dp),
                )
            }
            RoundIcon(Icons.Rounded.Close, C.Paper, C.Muted) { vm.removePair(p.id) }
        }
        if (!editable && label != null) Txt("نقش تو برای ${vm.snap?.others?.get(p.herId)?.name ?: "اون"}: $label", 12.sp, color = C.Muted)
        if (editable) {
            androidx.compose.animation.AnimatedVisibility(open) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Txt("این آدم چه نقشی تو زندگی‌ت داره؟", 13.sp, color = C.Muted)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(Fa.roles) { r ->
                            Chip(r, label == r, { vm.setPairLabel(p.id, r); open = false })
                        }
                    }
                    Field(text, { text = it.take(20) }, "یا خودت بنویس (مثلاً عمه، همکار…)", imeAction = androidx.compose.ui.text.input.ImeAction.Done, onDone = { vm.setPairLabel(p.id, text); open = false })
                    PrimaryButton("ذخیره‌ی نقش", { vm.setPairLabel(p.id, text); open = false }, kind = BtnKind.Lime)
                }
            }
        }
    }
}

@Composable
private fun SettingStepper(label: String, value: Int, min: Int, max: Int, format: (Int) -> String = { Fa.n(it) }, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Txt(label, 16.sp, FontWeight.Medium, modifier = Modifier.weight(1f))
        RoundIcon(Icons.Rounded.ChevronRight, C.Paper, C.Text) { if (value < max) onChange(value + 1) }
        Box(Modifier.size(76.dp, 40.dp), contentAlignment = Alignment.Center) {
            AnimatedContent(value, transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) }, label = "sv") { v ->
                Txt(format(v), 15.sp, FontWeight.Medium)
            }
        }
        RoundIcon(Icons.Rounded.ChevronLeft, C.Paper, C.Text) { if (value > min) onChange(value - 1) }
    }
}

@Composable
private fun SettingSwitch(title: String, sub: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Txt(title, 16.sp, FontWeight.Medium)
            Txt(sub, 12.sp, color = C.Muted)
        }
        Switch(
            checked, onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = C.Green, checkedThumbColor = Color.White, uncheckedTrackColor = C.Track, uncheckedThumbColor = Color.White, uncheckedBorderColor = Color.Transparent),
        )
    }
}
