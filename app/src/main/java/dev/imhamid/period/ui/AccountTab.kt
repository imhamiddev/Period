package dev.imhamid.period.ui

import kotlinx.serialization.json.put
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.ConfirmationNumber
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WorkspacePremium
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.imhamid.period.BuildConfig
import dev.imhamid.period.data.PairRow

@Composable
fun AccountTab(vm: AppVM) {
    val s = vm.snap ?: return
    val me = s.me
    val isHer = me.role == "her"
    val ctx = LocalContext.current
    var confirmLogout by remember { mutableStateOf(false) }

    if (confirmLogout) {
        ConfirmDialog(
            title = "خروج از حساب؟",
            text = "اگه تغییری هنوز به سرور نرسیده باشه (مثلاً آفلاین ثبت کردی)، با خروج از دست می‌ره.",
            confirm = "خروج",
            onConfirm = { confirmLogout = false; vm.logout() },
            onDismiss = { confirmLogout = false },
        )
    }

    Column(Modifier.fillMaxSize().background(C.Paper).statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.fillMaxWidth().height(60.dp)) { Txt("حساب", 22.sp, FontWeight.Bold, modifier = Modifier.align(Alignment.Center)) }

        // ---- profile
        Panel(Modifier.pressable { vm.push(Screen.PersonalInfo) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(60.dp).clip(CircleShape).background(C.PinkSoft), contentAlignment = Alignment.Center) {
                    Txt(me.name.orEmpty().take(1).ifBlank { "؟" }, 26.sp, FontWeight.Bold, C.Pink)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Txt(me.name.orEmpty(), 18.sp, FontWeight.Bold)
                    Txt((vm.store.email ?: me.email).orEmpty(), 13.sp, color = C.Muted, maxLines = 1)
                    Box(Modifier.padding(top = 4.dp).clip(RoundedCornerShape(8.dp)).background(C.PinkSoft).padding(horizontal = 8.dp, vertical = 2.dp)) {
                        Txt(if (isHer) "ثبت‌کننده‌ی چرخه" else "پارتنر", 11.sp, FontWeight.Medium, C.Pink)
                    }
                }
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = C.Text)
            }
        }

        // ---- menu
        Panel(padding = 0.dp) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                SettingRow("تنظیمات و یکاها", { vm.push(Screen.Preferences) }, icon = Icons.Rounded.Settings)
                if (isHer) { RowDivider(); SettingRow("تنظیمات چرخه", { vm.push(Screen.CycleSettings) }, icon = Icons.Rounded.WaterDrop) }
                if (isHer) { RowDivider(); SettingRow("گزارش برای پزشک (PDF)", { vm.push(Screen.MyCycles) }, icon = Icons.Rounded.Description) }
                RowDivider()
                SettingRow("یادآور روزانه", { vm.push(Screen.Reminder) }, icon = Icons.Rounded.Alarm)
                RowDivider()
                SettingRow("اعلان‌ها", { vm.push(Screen.Notifications) }, icon = Icons.Rounded.Notifications)
                RowDivider()
                SettingRow("پارتنر و پیام‌ها", { vm.push(Screen.Partner) }, icon = Icons.Rounded.Favorite, value = s.inbox.count { it.toId == me.id && it.readAt == null }.takeIf { it > 0 }?.let { Fa.n(it) })
                RowDivider()
                SettingRow("حساب و امنیت", { vm.push(Screen.Security) }, icon = Icons.Rounded.Shield)
                RowDivider()
                SettingRow("راهنما و پشتیبانی", { vm.push(Screen.Help) }, icon = Icons.Rounded.Description)
                RowDivider()
                SettingRow("خروج از حساب", { confirmLogout = true }, icon = Icons.AutoMirrored.Rounded.Logout, danger = true, chevron = false)
            }
        }

        // ---- sync / update
        Panel {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Txt("نسخه‌ی برنامه", 14.sp, color = C.Muted, modifier = Modifier.weight(1f))
                Txt(Fa.digits(BuildConfig.VERSION_NAME), 14.sp, FontWeight.Bold, C.Pink)
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Txt("همگام‌سازی", 14.sp, color = C.Muted, modifier = Modifier.weight(1f))
                Txt(if (vm.offline) "آفلاین" else if (vm.pendingChanges > 0) "در حال ارسال" else "به‌روز", 14.sp, FontWeight.Bold, if (vm.offline) C.Amber else C.Green)
            }
            if (vm.pendingChanges > 0) Txt("${Fa.n(vm.pendingChanges)} تغییر روی گوشی منتظر ارسال به سرور", 12.sp, color = C.Muted)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinePill("همگام‌سازی", { vm.refresh() }, Modifier.weight(1f))
                BigButton("بررسی آپدیت", { vm.checkUpdate() }, Modifier.weight(1f).height(44.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

// ============================================================================ partner
@Composable
fun PartnerScreen(vm: AppVM) {
    val s = vm.snap ?: return
    val isHer = s.me.role == "her"
    Page {
        TopBar("پارتنر و پیام‌ها", onBack = { vm.pop() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (isHer) {
                Panel { ToggleRow("اشتراک با پارتنر", s.me.shareEnabled, { v -> vm.patch { put("share_enabled", v) } }, "اگه خاموش باشه کسی چرخه‌ات رو نمی‌بینه") }
                ShareCard(vm)
                s.pairs.filter { it.status == "active" }.forEach { RoleEditor(vm, it, s.others[it.partnerId]?.name ?: "پارتنر") }
            } else {
                if (s.activePair == null) ConnectCard(vm, s.pairs)
                else Panel {
                    s.pairs.forEach { p -> PairRowItem(vm, p, s.others[p.herId]?.name ?: "درخواست ارسال‌شده") }
                }
            }
            MessagesCard(vm)
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun RoleEditor(vm: AppVM, p: PairRow, name: String) {
    val label = vm.labelOf(p)
    var text by remember(p.id, label) { mutableStateOf(label.orEmpty()) }
    Panel {
        Txt("نقش $name تو زندگی‌ت", 15.sp, FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(Fa.roles) { r -> FilterChip(r, label == r) { vm.setPairLabel(p.id, r) } }
        }
        Spacer(Modifier.height(10.dp))
        Field(text, { text = it.take(20) }, "یا خودت بنویس (مثلاً عمه، همکار…)", imeAction = androidx.compose.ui.text.input.ImeAction.Done, onDone = { vm.setPairLabel(p.id, text) })
        Spacer(Modifier.height(10.dp))
        BigButton("ذخیره‌ی نقش", { vm.setPairLabel(p.id, text) }, Modifier.height(46.dp))
    }
}
