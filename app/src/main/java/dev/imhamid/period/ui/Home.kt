package dev.imhamid.period.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.material3.Icon
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
import dev.imhamid.period.engine.Overview
import dev.imhamid.period.engine.Phase
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.temporal.ChronoUnit

// ============================================================================ shell
@Composable
fun HomeScreen(vm: AppVM) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val pending = vm.snap?.let { s -> s.pairs.any { it.status == "pending" } } ?: false
    // light polling while visible (pairing requests, partner updates)
    LaunchedEffect(Unit) { while (true) { delay(if (pending) 6000 else 30000); vm.refresh() } }

    Box(Modifier.fillMaxSize().background(C.Paper)) {
        AnimatedContent(
            tab,
            transitionSpec = {
                val dir = if (targetState > initialState) -1 else 1
                (fadeIn(tween(350)) + slideInHorizontally(tween(400)) { dir * it / 10 }) togetherWith
                    (fadeOut(tween(200)) + slideOutHorizontally(tween(300)) { -dir * it / 10 })
            },
            label = "tabs",
        ) { t ->
            when (t) {
                0 -> HomeTab(vm)
                1 -> CalendarTab(vm)
                else -> SettingsTab(vm)
            }
        }
        BottomNav(tab, { tab = it }, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun BottomNav(selected: Int, onSelect: (Int) -> Unit, modifier: Modifier) {
    val items = listOf(Icons.Rounded.Home to "خانه", Icons.Rounded.CalendarMonth to "تقویم", Icons.Rounded.Settings to "تنظیمات")
    Row(
        modifier.navigationBarsPadding().padding(bottom = 14.dp)
            .clip(RoundedCornerShape(34.dp)).background(C.Ink.copy(alpha = 0.94f)).padding(6.dp)
            .animateContentSize(spring(0.8f)),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items.forEachIndexed { i, (icon, label) ->
            val sel = i == selected
            val bg by animateColorAsState(if (sel) C.Lime else Color.Transparent, tween(300), label = "nb")
            val fg by animateColorAsState(if (sel) C.Deep else Color.White.copy(alpha = 0.75f), tween(300), label = "nf")
            Row(
                Modifier.clip(RoundedCornerShape(28.dp)).background(bg).pressable { onSelect(i) }
                    .padding(horizontal = if (sel) 18.dp else 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(icon, null, tint = fg, modifier = Modifier.size(22.dp))
                if (sel) {
                    Spacer(Modifier.size(8.dp))
                    Txt(label, 14.sp, FontWeight.Medium, fg)
                }
            }
        }
    }
}

// ============================================================================ ring
@Composable
fun CycleRing(engine: CycleEngine?, ov: Overview?, modifier: Modifier = Modifier, size: Dp = 252.dp, content: @Composable () -> Unit) {
    val sweep by animateFloatAsState(1f, tween(1400, easing = androidx.compose.animation.core.FastOutSlowInEasing), label = "sweep")
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        0.85f, 1.15f, infiniteRepeatable(tween(1800), RepeatMode.Reverse), label = "p",
    )
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    val prog by animateFloatAsState(if (started) 1f else 0f, tween(1400, easing = androidx.compose.animation.core.FastOutSlowInEasing), label = "prog")

    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val stroke = 16.dp.toPx()
            val pad = stroke / 2 + 4.dp.toPx()
            val d = this.size.minDimension - pad * 2
            val tl = Offset(pad, pad)
            val sz = Size(d, d)
            drawArc(Color.White.copy(alpha = 0.16f), -90f, 360f, false, tl, sz, style = Stroke(stroke, cap = StrokeCap.Round))
            val seg = engine?.segmentFor(engine.today)
            if (engine != null && seg != null && ov != null) {
                val len = seg.length.toFloat()
                fun arc(fromIdx: Int, toIdx: Int, color: Color) {
                    val a0 = -90f + (fromIdx - 1) / len * 360f
                    val sw = (toIdx - fromIdx + 1) / len * 360f * prog
                    if (sw > 1f) drawArc(color, a0 + 1f, sw - 2f, false, tl, sz, style = Stroke(stroke, cap = StrokeCap.Round))
                }
                fun idx(d: LocalDate) = ChronoUnit.DAYS.between(seg.start, d).toInt() + 1
                arc(1, idx(seg.periodEnd), C.Coral)
                if (idx(seg.fertileEnd) > idx(seg.periodEnd)) arc(idx(seg.fertileStart), idx(seg.fertileEnd), C.Lime)
                arc(seg.length - engine.pmsDays + 1, seg.length, C.Amber)
                // marker for today
                val day = (ov.cycleDay ?: 1).coerceIn(1, seg.length)
                val ang = Math.toRadians((-90f + (day - 0.5f) / len * 360f * prog).toDouble())
                val r = d / 2
                val c = Offset(pad + r + (r * Math.cos(ang)).toFloat(), pad + r + (r * Math.sin(ang)).toFloat())
                drawCircle(Color.White.copy(alpha = 0.25f), 13.dp.toPx() * pulse, c)
                drawCircle(Color.White, 8.dp.toPx(), c)
            }
        }
        content()
    }
}

// ============================================================================ home tab
@Composable
private fun HomeTab(vm: AppVM) {
    val s = vm.snap ?: return
    val me = s.me
    val isHer = me.role == "her"
    val owner = s.owner
    val today = remember { LocalDate.now() }
    val engine = remember(s.logs, owner) {
        owner?.let { CycleEngine(today, s.logs, it.cycleLength, it.periodLength, it.lutealLength, it.pmsDays) }
    }
    val ov = remember(engine) { engine?.overview() }
    val ownerName = owner?.name?.takeIf { it.isNotBlank() } ?: "پارتنرت"
    val paused = !isHer && owner != null && !owner.shareEnabled

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // ---------------- hero
        Aurora(Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = 44.dp, bottomEnd = 44.dp))) {
            Column(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 24.dp, vertical = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Reveal(0, Modifier.fillMaxWidth()) {
                    Column {
                        Txt(if (isHer) "سلام ${me.name} 🌿" else "وضعیت $ownerName", 15.sp, color = Color.White.copy(alpha = 0.8f))
                        Txt(
                            when {
                                !isHer && owner == null -> "هنوز وصل نیستی"
                                paused -> "اشتراک موقتاً متوقفه"
                                ov == null || !ov.hasData -> "پریود رو ثبت کن"
                                else -> Fa.phaseName(ov.phase)
                            },
                            30.sp, FontWeight.Light, Color.White,
                        )
                    }
                }
                Spacer(Modifier.height(18.dp))
                Reveal(1) {
                    CycleRing(engine, ov) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val big = when {
                                ov == null || !ov.hasData -> "—"
                                ov.periodDay != null -> "روز ${Fa.n(ov.periodDay)}"
                                ov.daysToPeriod != null && ov.daysToPeriod > 0 && ov.lateDays == 0 -> Fa.n(ov.daysToPeriod)
                                ov.lateDays > 0 -> Fa.n(ov.lateDays)
                                else -> "امروز"
                            }
                            val small = when {
                                ov == null || !ov.hasData -> "اطلاعات کافی نیست"
                                ov.periodDay != null -> "از پریود"
                                ov.lateDays > 0 -> "روز تأخیر"
                                ov.daysToPeriod != null && ov.daysToPeriod > 0 -> "روز تا پریود"
                                else -> "پریود"
                            }
                            AnimatedContent(big, transitionSpec = { fadeIn(tween(350)) togetherWith fadeOut(tween(200)) }, label = "big") {
                                Txt(it, if (it.length > 4) 40.sp else 64.sp, FontWeight.Light, Color.White, lineHeight = 74.sp)
                            }
                            Txt(small, 14.sp, color = Color.White.copy(alpha = 0.8f))
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Reveal(2) {
                    Box(Modifier.clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = 0.18f)).padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Txt(
                            if (ov?.hasData == true) Fa.phaseHint(ov.phase) else "بعد از ثبت اولین پریود، پیش‌بینی‌ها شروع می‌شه",
                            13.sp, color = Color.White,
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }

        // ---------------- cards
        Column(Modifier.padding(horizontal = 20.dp).padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (!isHer && owner == null) {
                Reveal(0) { ConnectCard(vm, s.pairs) }
            } else if (paused) {
                Reveal(0) { Card { Txt("$ownerName اشتراک‌گذاری رو موقتاً متوقف کرده؛ وقتی فعالش کنه دوباره اطلاعات می‌رسه.", 15.sp, color = C.Muted) } }
            } else {
                if (isHer) Reveal(0) { TodayCard(vm, engine, today) }
                if (!isHer) Reveal(0) {
                    Card {
                        Column {
                            Txt("امروز چه کار کنم؟", 14.sp, color = C.Muted)
                            Spacer(Modifier.height(6.dp))
                            Txt(Fa.partnerTip(ov?.phase, ownerName), 16.sp)
                        }
                    }
                } else if (ov?.hasData == true) Reveal(1) {
                    Card { Column { Txt("برای تو", 14.sp, color = C.Muted); Spacer(Modifier.height(6.dp)); Txt(Fa.herTip(ov.phase), 16.sp) } }
                }
                if (ov?.hasData == true) Reveal(2) { UpcomingCard(ov) }
                if (ov?.hasData == true) Reveal(3) { StatsCard(ov, engine!!) }
            }
            if (isHer) Reveal(4) { ShareCard(vm) }
            Spacer(Modifier.height(110.dp))
        }
    }
}

@Composable
private fun TodayCard(vm: AppVM, engine: CycleEngine?, today: LocalDate) {
    val logged = engine?.isLogged(today) == true
    val label = when {
        logged -> "امروز ثبت شده ✓  (برای حذف بزن)"
        engine?.overview()?.periodDay != null -> "امروز هم خونریزی دارم"
        else -> "پریودم امروز شروع شد"
    }
    Card {
        Column {
            Txt("ثبت سریع", 14.sp, color = C.Muted)
            Spacer(Modifier.height(12.dp))
            PrimaryButton(
                label, { vm.toggleDay(today) },
                kind = if (logged) BtnKind.Lime else BtnKind.Dark,
                icon = { Icon(if (logged) Icons.Rounded.Check else Icons.Rounded.WaterDrop, null, tint = if (logged) C.Deep else C.Lime, modifier = Modifier.size(20.dp)) },
            )
        }
    }
}

@Composable
private fun UpcomingCard(ov: Overview) {
    Card {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Txt("پیش رو", 14.sp, color = C.Muted)
            ov.pmsStart?.let { d ->
                val n = ov.daysToPms ?: 0
                EventRow(C.Amber, "شروع PMS", if (ov.inPms) "همین الان" else Fa.daysText(n), Fa.date(d))
            }
            ov.nextPeriod?.let { d ->
                EventRow(C.Coral, "پریود بعدی", Fa.daysText(ov.daysToPeriod ?: 0), Fa.date(d))
            }
            ov.ovulation?.let { d ->
                EventRow(C.Lime, "تخمک‌گذاری", Fa.daysText(ov.daysToOvulation ?: 0), Fa.date(d))
            }
        }
    }
}

@Composable
private fun EventRow(color: Color, title: String, whenText: String, date: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(12.dp).clip(CircleShape).background(color))
        Spacer(Modifier.size(14.dp))
        Column(Modifier.weight(1f)) {
            Txt(title, 16.sp, FontWeight.Medium)
            Txt(date, 13.sp, color = C.Muted)
        }
        Txt(whenText, 14.sp, FontWeight.Medium, C.Green)
    }
}

@Composable
private fun StatsCard(ov: Overview, engine: CycleEngine) {
    Card {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Stat("طول چرخه", "${Fa.n(ov.cycleLength)} روز")
            Stat("طول پریود", "${Fa.n(engine.predictedPeriod)} روز")
            Stat("دقت", "±${Fa.n(ov.variability)} روز")
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Txt(value, 18.sp, FontWeight.Medium)
        Txt(label, 12.sp, color = C.Muted)
    }
}

// ---------------------------------------------------------------- share / pairing
@Composable
private fun ShareCard(vm: AppVM) {
    val s = vm.snap ?: return
    val ctx = LocalContext.current
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) { if (copied) { delay(1600); copied = false } }
    Card {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Txt("اشتراک با پارتنر", 14.sp, color = C.Muted)
            Txt("کد زیر رو بده به پارتنرت تا وارد برنامه‌ش کنه. بعدش خودت تایید می‌کنی.", 14.sp, color = C.Muted)
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(C.LimeSoft)
                    .pressable {
                        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("code", s.me.pairCode.orEmpty()))
                        copied = true
                    }.padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Txt(s.me.pairCode.orEmpty(), 30.sp, FontWeight.Bold, C.Deep, modifier = Modifier.weight(1f))
                AnimatedContent(copied, label = "cp") { c ->
                    if (c) Icon(Icons.Rounded.Check, null, tint = C.Green) else Icon(Icons.Rounded.ContentCopy, null, tint = C.Deep)
                }
            }
            s.pairs.forEach { p ->
                val partner = s.others[p.partnerId]
                PairRowItem(vm, p, partner?.name ?: "پارتنر")
            }
        }
    }
}

@Composable
private fun PairRowItem(vm: AppVM, p: PairRow, name: String) {
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
            RoundIcon(Icons.Rounded.Check, C.Green, Color.White) { vm.approve(p.id) }
            Spacer(Modifier.size(8.dp))
            RoundIcon(Icons.Rounded.Close, C.CoralSoft, C.Ink) { vm.removePair(p.id) }
        } else {
            RoundIcon(Icons.Rounded.Close, C.Paper, C.Muted) { vm.removePair(p.id) }
        }
    }
}

@Composable
private fun RoundIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, bg: Color, fg: Color, onClick: () -> Unit) {
    Box(Modifier.size(40.dp).clip(CircleShape).background(bg).pressable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = fg, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun ConnectCard(vm: AppVM, pairs: List<PairRow>) {
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
@Composable
private fun CalendarTab(vm: AppVM) {
    val s = vm.snap ?: return
    val isHer = s.me.role == "her"
    val owner = s.owner
    val today = remember { LocalDate.now() }
    val engine = remember(s.logs, owner) {
        owner?.let { CycleEngine(today, s.logs, it.cycleLength, it.periodLength, it.lutealLength, it.pmsDays) }
    }
    var month by rememberSaveable { mutableStateOf(Fa.jalaliMonth(today).first.toString()) }
    val first = LocalDate.parse(month)
    val len = Fa.jalaliMonth(first).second

    Column(Modifier.fillMaxSize().background(C.Paper).statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            RoundIcon(Icons.Rounded.ChevronRight, Color.White, C.Ink) { month = Fa.jalaliMonth(first.minusDays(1)).first.toString() }
            AnimatedContent(
                month,
                modifier = Modifier.weight(1f),
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                label = "mt",
            ) { m -> Txt(Fa.date(LocalDate.parse(m), "MMMM y"), 22.sp, FontWeight.Light, align = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
            RoundIcon(Icons.Rounded.ChevronLeft, Color.White, C.Ink) { month = first.plusDays(len.toLong()).toString() }
        }
        Spacer(Modifier.height(18.dp))
        Card(Modifier, ) {
            Column {
                Row(Modifier.fillMaxWidth()) {
                    listOf("ش", "ی", "د", "س", "چ", "پ", "ج").forEach {
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { Txt(it, 13.sp, color = C.Muted) }
                    }
                }
                Spacer(Modifier.height(8.dp))
                AnimatedContent(
                    month,
                    transitionSpec = {
                        val fwd = LocalDate.parse(targetState) > LocalDate.parse(initialState)
                        (fadeIn(tween(350)) + slideInHorizontally(tween(400)) { if (fwd) -it / 6 else it / 6 }) togetherWith
                            (fadeOut(tween(200)) + slideOutHorizontally(tween(300)) { if (fwd) it / 6 else -it / 6 })
                    },
                    label = "grid",
                ) { m ->
                    val f = LocalDate.parse(m)
                    val l = Fa.jalaliMonth(f).second
                    val lead = (f.dayOfWeek.value + 1) % 7 // Saturday = 0
                    val cells = lead + l
                    val rows = (cells + 6) / 7
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (r in 0 until rows) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                for (c in 0 until 7) {
                                    val i = r * 7 + c - lead
                                    Box(Modifier.weight(1f).aspectRatio(1f)) {
                                        if (i in 0 until l) DayCell(f.plusDays(i.toLong()), today, engine, isHer) { vm.toggleDay(it) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Card {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Legend(C.Coral, "پریود (ثبت‌شده)")
                Legend(C.CoralSoft, "پریود (پیش‌بینی)")
                Legend(C.AmberSoft, "PMS")
                Legend(C.LimeSoft, "پنجره‌ی باروری")
                Legend(C.Lime, "تخمک‌گذاری")
                if (isHer) Txt("برای ثبت یا حذف پریود، روی روز بزن.", 12.sp, color = C.Muted)
            }
        }
        Spacer(Modifier.height(120.dp))
    }
}

@Composable
private fun Legend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(16.dp).clip(CircleShape).background(color))
        Spacer(Modifier.size(10.dp))
        Txt(label, 13.sp)
    }
}

@Composable
private fun DayCell(d: LocalDate, today: LocalDate, engine: CycleEngine?, isHer: Boolean, onTap: (LocalDate) -> Unit) {
    val logged = engine?.isLogged(d) == true
    val phase = engine?.phaseOn(d)
    val predictedPeriod = engine?.isPredictedPeriod(d) == true
    val bg by animateColorAsState(
        when {
            logged -> C.Coral
            predictedPeriod -> C.CoralSoft
            phase == Phase.OVULATION -> C.Lime
            phase == Phase.FERTILE -> C.LimeSoft
            phase == Phase.PMS -> C.AmberSoft
            else -> Color.Transparent
        },
        tween(300), label = "cell",
    )
    val sc by animateFloatAsState(if (logged) 1f else 0.96f, spring(0.4f, 600f), label = "cs")
    val isToday = d == today
    val future = d.isAfter(today)
    Box(
        Modifier.fillMaxSize().scale(sc).clip(CircleShape).background(bg)
            .then(if (isToday) Modifier.border(2.dp, C.Ink, CircleShape) else Modifier)
            .then(if (isHer && !future) Modifier.pressable { onTap(d) } else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Txt(Fa.jalaliDay(d), 14.sp, if (logged || isToday) FontWeight.Bold else FontWeight.Normal, if (future && bg == Color.Transparent) C.Muted else C.Ink)
    }
}

// ============================================================================ settings tab
@Composable
private fun SettingsTab(vm: AppVM) {
    val s = vm.snap ?: return
    val me = s.me
    val isHer = me.role == "her"
    var editName by remember { mutableStateOf(false) }
    var nameText by remember(me.name) { mutableStateOf(me.name.orEmpty()) }

    Column(Modifier.fillMaxSize().background(C.Paper).statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(20.dp))
        Reveal(0) { Txt("تنظیمات", 32.sp, FontWeight.Light) }
        Spacer(Modifier.height(18.dp))
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Reveal(1) {
                Card {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(52.dp).clip(CircleShape).background(C.Lime), contentAlignment = Alignment.Center) {
                                Txt(me.name.orEmpty().take(1), 22.sp, FontWeight.Bold, C.Deep)
                            }
                            Spacer(Modifier.size(14.dp))
                            Column(Modifier.weight(1f)) {
                                Txt(me.name.orEmpty(), 18.sp, FontWeight.Medium)
                                Txt(me.email.orEmpty(), 12.sp, color = C.Muted)
                                Txt(if (isHer) "ثبت‌کننده‌ی چرخه" else "پارتنر", 12.sp, color = C.Green)
                            }
                            Txt(if (editName) "بستن" else "ویرایش", 13.sp, FontWeight.Medium, C.Green, Modifier.pressable { editName = !editName })
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

            if (isHer) Reveal(2) {
                Card {
                    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        Txt("چرخه", 14.sp, color = C.Muted)
                        SettingStepper("طول چرخه", me.cycleLength, 20, 45) { v -> vm.patch { put("cycle_length", v) } }
                        SettingStepper("طول پریود", me.periodLength, 2, 10) { v -> vm.patch { put("period_length", v) } }
                        SettingStepper("روزهای PMS", me.pmsDays, 1, 10) { v -> vm.patch { put("pms_days", v) } }
                    }
                }
                Spacer(Modifier.height(0.dp))
            }
            if (isHer) Reveal(3) {
                Card {
                    SettingSwitch("اشتراک با پارتنر", "اگه خاموش باشه پارتنرت چیزی نمی‌بینه", me.shareEnabled) { v -> vm.patch { put("share_enabled", v) } }
                }
            }
            Reveal(4) {
                Card {
                    SettingSwitch("نوتیفیکیشن", if (isHer) "یادآوری PMS و پریود" else "خبر PMS و پریود پارتنرت", me.notifyEnabled) { v -> vm.patch { put("notify_enabled", v) } }
                }
            }
            if (!isHer && s.pairs.isNotEmpty()) Reveal(5) {
                Card {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Txt("اتصال", 14.sp, color = C.Muted)
                        s.pairs.forEach { p ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Txt(s.others[p.herId]?.name ?: "درخواست ارسال‌شده", 16.sp, FontWeight.Medium)
                                    Txt(if (p.status == "pending") "منتظر تایید" else "وصل است", 12.sp, color = if (p.status == "pending") C.Amber else C.Green)
                                }
                                RoundIcon(Icons.Rounded.Close, C.Paper, C.Muted) { vm.removePair(p.id) }
                            }
                        }
                    }
                }
            }
            Reveal(6) {
                Card {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Txt("نسخه‌ی ${Fa.digits(BuildConfig.VERSION_NAME)}", 15.sp, FontWeight.Medium)
                        PrimaryButton("بررسی آپدیت", { vm.checkUpdate() }, kind = BtnKind.Lime)
                        PrimaryButton(
                            "خروج از حساب", { vm.logout() }, kind = BtnKind.Light,
                            modifier = Modifier.border(1.dp, C.Line, RoundedCornerShape(29.dp)),
                            icon = { Icon(Icons.AutoMirrored.Rounded.Logout, null, tint = C.Ink, modifier = Modifier.size(20.dp)) },
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(130.dp))
    }
}

@Composable
private fun SettingStepper(label: String, value: Int, min: Int, max: Int, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Txt(label, 16.sp, modifier = Modifier.weight(1f))
        RoundIcon(Icons.Rounded.ChevronRight, C.Paper, C.Ink) { if (value < max) onChange(value + 1) }
        Box(Modifier.size(52.dp, 40.dp), contentAlignment = Alignment.Center) {
            AnimatedContent(value, transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) }, label = "sv") { v ->
                Txt(Fa.n(v), 18.sp, FontWeight.Medium)
            }
        }
        RoundIcon(Icons.Rounded.ChevronLeft, C.Paper, C.Ink) { if (value > min) onChange(value - 1) }
    }
}

@Composable
private fun SettingSwitch(title: String, sub: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Txt(title, 16.sp, FontWeight.Medium)
            Txt(sub, 12.sp, color = C.Muted)
        }
        Switch(
            checked, onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = C.Green, checkedThumbColor = Color.White, uncheckedTrackColor = Color(0xFFD9DED9), uncheckedThumbColor = Color.White, uncheckedBorderColor = Color.Transparent),
        )
    }
}
