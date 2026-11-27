package dev.imhamid.period.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.activity.compose.BackHandler
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.serialization.json.put
import dev.imhamid.period.BuildConfig
import dev.imhamid.period.data.PairRow
import dev.imhamid.period.engine.CycleEngine
import dev.imhamid.period.engine.toEngine
import dev.imhamid.period.engine.Overview
import dev.imhamid.period.engine.Phase
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.temporal.ChronoUnit

// ============================================================================ shell
@Composable
internal fun LateCard(vm: AppVM, days: Int, isHer: Boolean, name: String, today: LocalDate) {
    Card {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(12.dp).clip(CircleShape).background(C.Amber))
                Spacer(Modifier.size(10.dp))
                Txt(if (isHer) "پریودت ${Fa.n(days)} روز دیر کرده" else "پریود $name ${Fa.n(days)} روز دیر کرده", 16.sp, FontWeight.Medium)
            }
            Txt(
                if (isHer) "اگه شروع شده ثبتش کن. اگه نه، استرس و بی‌خوابی و سفر هم می‌تونن چرخه رو بهم بزنن؛ اگه خیلی طول کشید با دکتر حرف بزن."
                else "شاید فقط استرس باشه. هول نکن؛ آروم و مهربون کنارش باش.",
                14.sp, color = C.Muted,
            )
            if (isHer) PrimaryButton("پریودم امروز شروع شد", { vm.toggleDay(today) })
        }
    }
}


@Composable
internal fun MessagesCard(vm: AppVM) {
    val s = vm.snap ?: return
    val isHer = s.me.role == "her"
    val received = s.inbox.filter { it.toId == s.me.id }.take(3)
    LaunchedEffect(s.inbox) { if (received.any { it.readAt == null }) vm.markRead() }
    var sent by remember { mutableStateOf<String?>(null) }
    var custom by rememberSaveable { mutableStateOf("") }
    var editSaved by remember { mutableStateOf(false) }
    var target by remember { mutableStateOf<String?>(null) }
    val activePairs = s.pairs.filter { it.status == "active" }
    LaunchedEffect(sent) { if (sent != null) { delay(1700); sent = null } }
    Card {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Txt("پیام‌ها", 14.sp, color = C.Muted)
            if (received.isEmpty()) Txt("هنوز پیامی نیومده.", 13.sp, color = C.Muted)
            received.forEach { m ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(C.LimeSoft).padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        val from = s.pairs.firstOrNull { it.partnerId == m.fromId || it.herId == m.fromId }?.let { vm.personTitle(it) }
                        if (from != null) Txt(from, 11.sp, FontWeight.Medium, C.OnSoft)
                        Txt(m.body, 15.sp)
                        Txt(Fa.ago(m.createdAt), 11.sp, color = C.Muted)
                    }
                    if (m.readAt == null) Box(Modifier.size(8.dp).clip(CircleShape).background(C.Green))
                }
            }
            if (isHer && activePairs.size > 1) {
                Txt("به کی پیام بدم؟", 14.sp, color = C.Muted)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { Chip("همه", target == null, { target = null }) }
                    items(activePairs) { p -> Chip(vm.personTitle(p), target == p.id, { target = p.id }) }
                }
            }
            Txt(if (isHer) "به پارتنرت خبر بده" else "یه پیام مهربون بفرست", 14.sp, color = C.Muted)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(if (isHer) Fa.herQuick else Fa.partnerQuick) { txt ->
                    Chip(if (sent == txt) "ارسال شد ✓" else txt, sent == txt, {
                        if (sent == null) { sent = txt; vm.sendMessage(txt, target) }
                    })
                }
            }

            // ---- my own saved messages
            if (vm.savedMessages.isNotEmpty()) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Txt("پیام‌های خودم", 14.sp, color = C.Muted, modifier = Modifier.weight(1f))
                    Txt(
                        if (editSaved) "تمام" else "ویرایش", 13.sp, FontWeight.Medium, C.Green,
                        Modifier.pressable { editSaved = !editSaved }.padding(4.dp),
                    )
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(vm.savedMessages) { txt ->
                        Chip(if (editSaved) "✕  $txt" else if (sent == txt) "ارسال شد ✓" else txt, sent == txt, {
                            if (editSaved) vm.deleteSavedMessage(txt)
                            else if (sent == null) { sent = txt; vm.sendMessage(txt, target) }
                        })
                    }
                }
            }

            // ---- free text
            Field(custom, { custom = it.take(200) }, "پیام دلخواه", imeAction = androidx.compose.ui.text.input.ImeAction.Done, onDone = {
                if (custom.isNotBlank()) { vm.sendMessage(custom.trim(), target); custom = "" }
            })
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.weight(1f)) {
                    PrimaryButton("ارسال", { vm.sendMessage(custom.trim(), target); custom = "" }, enabled = custom.isNotBlank())
                }
                Box(Modifier.weight(1f)) {
                    PrimaryButton("ذخیره برای بعد", { vm.saveMessage(custom); custom = "" }, kind = BtnKind.Lime, enabled = custom.isNotBlank())
                }
            }
        }
    }
}


@Composable
internal fun ShareCard(vm: AppVM) {
    val s = vm.snap ?: return
    val ctx = LocalContext.current
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) { if (copied) { delay(1600); copied = false } }
    Card {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Txt("اشتراک چرخه", 14.sp, color = C.Muted)
            Txt("کد زیر رو بده به هر کسی که می‌خوای چرخه‌ت رو ببینه (پارتنر، مادر، خواهر…). بعدش خودت تایید می‌کنی و نقشش رو تو تنظیمات می‌نویسی.", 14.sp, color = C.Muted)
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(C.LimeSoft)
                    .pressable {
                        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("code", s.me.pairCode.orEmpty()))
                        copied = true
                    }.padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Txt(s.me.pairCode.orEmpty(), 30.sp, FontWeight.Bold, C.OnSoft, modifier = Modifier.weight(1f))
                AnimatedContent(copied, label = "cp") { c ->
                    if (c) Icon(Icons.Rounded.Check, null, tint = C.Green) else Icon(Icons.Rounded.ContentCopy, null, tint = C.OnSoft)
                }
            }
            s.pairs.forEach { p ->
                PairRowItem(vm, p, vm.personTitle(p))
            }
        }
    }
}


@Composable
internal fun PairRowItem(vm: AppVM, p: PairRow, name: String) {
    var confirmRemove by remember(p.id) { mutableStateOf(false) }
    if (confirmRemove) {
        val pending = p.status == "pending"
        ConfirmDialog(
            title = if (pending) "رد درخواست؟" else "قطع اتصال؟",
            text = if (pending) "درخواست اتصال $name رد می‌شه." else "$name دیگه چرخه‌ی اشتراکی رو نمی‌بینه. هر وقت خواستی می‌شه دوباره وصل شد.",
            confirm = if (pending) "رد کن" else "قطع کن",
            onConfirm = { confirmRemove = false; vm.removePair(p.id) },
            onDismiss = { confirmRemove = false },
        )
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(C.Lime), contentAlignment = Alignment.Center) {
            Txt(name.take(1), 16.sp, FontWeight.Bold, C.Deep)
        }
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Txt(name, 16.sp, FontWeight.Medium)
            Txt(if (p.status == "pending") "منتظر تایید تو" else "وصل است", 12.sp, color = if (p.status == "pending") C.Amber else C.Green)
        }
        if (p.status == "pending") {
            RoundIcon(Icons.Rounded.Check, C.Green, Color.White, "تایید") { vm.approve(p.id) }
            Spacer(Modifier.size(8.dp))
            RoundIcon(Icons.Rounded.Close, C.CoralSoft, C.Text, "رد درخواست") { confirmRemove = true }
        } else {
            RoundIcon(Icons.Rounded.Close, C.Paper, C.Muted, "قطع اتصال") { confirmRemove = true }
        }
    }
}

/** 40 dp visual circle inside a 48 dp touch target; [description] is read by screen readers. */

@Composable
internal fun RoundIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, bg: Color, fg: Color, description: String? = null, onClick: () -> Unit) {
    Box(
        Modifier.size(48.dp).pressable(onClick = onClick).semantics { if (description != null) contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(20.dp))
        }
    }
}


@Composable
internal fun ConnectCard(vm: AppVM, pairs: List<PairRow>) {
    var code by rememberSaveable { mutableStateOf("") }
    val pending = pairs.any { it.status == "pending" }
    Card {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (pending) {
                val t = rememberInfiniteTransition(label = "wait")
                val a by t.animateFloat(0.3f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "a")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(12.dp).alpha(a).clip(CircleShape).background(C.Amber))
                    Spacer(Modifier.size(12.dp))
                    Txt("درخواست فرستاده شد؛ منتظر تایید اونه…", 16.sp, FontWeight.Medium)
                }
                Txt("به محض تایید، اطلاعات و نوتیف‌ها فعال می‌شن.", 14.sp, color = C.Muted)
            } else {
                Txt("اتصال به پارتنر", 14.sp, color = C.Muted)
                Txt("کدی که پارتنرت تو برنامه‌ش می‌بینه رو وارد کن.", 14.sp, color = C.Muted)
                Field(code, { code = it.filter { c -> c.isLetterOrDigit() }.uppercase().take(8) }, "کد اتصال", ltr = true, imeAction = androidx.compose.ui.text.input.ImeAction.Done, onDone = { vm.connect(code) })
                PrimaryButton("ارسال درخواست", { vm.connect(code) }, loading = vm.busy, enabled = code.length >= 4)
            }
        }
    }
}

// ============================================================================ calendar tab
