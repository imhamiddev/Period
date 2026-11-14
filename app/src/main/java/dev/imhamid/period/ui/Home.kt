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
import dev.imhamid.period.engine.toEngine
import dev.imhamid.period.engine.Overview
import dev.imhamid.period.engine.Phase
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.temporal.ChronoUnit

// ============================================================================ shell
@Composable
fun HomeScreen(vm: AppVM) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var sheetDay by remember { mutableStateOf<LocalDate?>(null) }
    var shownDay by remember { mutableStateOf(LocalDate.now()) }
    val openDay: (LocalDate) -> Unit = { shownDay = it; sheetDay = it }
    val pending = vm.snap?.let { s -> s.pairs.any { it.status == "pending" } } ?: false
    // light polling while visible (pairing requests, partner updates, messages)
    LaunchedEffect(Unit) { while (true) { delay(if (pending) 6000 else 15000); vm.refresh() } }
    BackHandler(enabled = sheetDay != null) { sheetDay = null }

    Box(Modifier.fillMaxSize().background(C.Paper)) {
        AnimatedContent(
            tab,
            transitionSpec = {
                val dir = if (targetState > initialState) -1 else 1
                (fadeIn(tween(380, delayMillis = 60)) + slideInHorizontally(tween(460, easing = androidx.compose.animation.core.FastOutSlowInEasing)) { dir * it / 6 } + androidx.compose.animation.scaleIn(tween(460), 0.97f)) togetherWith
                    (fadeOut(tween(180)) + slideOutHorizontally(tween(340)) { -dir * it / 8 })
            },
            label = "tabs",
        ) { t ->
            when (t) {
                0 -> HomeTab(vm, openDay)
                1 -> CalendarTab(vm, openDay)
                2 -> StatsTab(vm)
                else -> SettingsTab(vm)
            }
        }
        BottomNav(tab, { tab = it }, Modifier.align(Alignment.BottomCenter))

        // day sheet
        AnimatedVisibility(sheetDay != null, enter = fadeIn(tween(250)), exit = fadeOut(tween(260))) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { sheetDay = null },
            )
        }
        AnimatedVisibility(
            sheetDay != null,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(androidx.compose.animation.core.spring(0.85f, 380f)) { it } + fadeIn(tween(200)),
            exit = slideOutVertically(tween(300)) { it } + fadeOut(tween(220)),
        ) {
            DaySheet(vm, shownDay) { sheetDay = null }
        }
    }
}

@Composable
private fun BottomNav(selected: Int, onSelect: (Int) -> Unit, modifier: Modifier) {
    val items = listOf(Icons.Rounded.Home to "خانه", Icons.Rounded.CalendarMonth to "تقویم", Icons.Rounded.BarChart to "آمار", Icons.Rounded.Settings to "تنظیمات")
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(300); shown = true }
    val enter by animateFloatAsState(if (shown) 1f else 0f, spring(0.62f, 220f), label = "naventer")
    Row(
        modifier.navigationBarsPadding().padding(bottom = 14.dp)
            .graphicsLayer { translationY = (1f - enter) * 170.dp.toPx(); alpha = enter.coerceIn(0f, 1f) }
            .clip(RoundedCornerShape(34.dp)).background(C.Ink.copy(alpha = 0.94f)).padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items.forEachIndexed { i, (icon, label) ->
            val sel = i == selected
            val bg by animateColorAsState(if (sel) C.Lime else Color.Transparent, tween(320), label = "nb")
            val fg by animateColorAsState(if (sel) C.Deep else Color.White.copy(alpha = 0.75f), tween(320), label = "nf")
            val iconScale by animateFloatAsState(if (sel) 1.15f else 1f, spring(0.3f, 380f), label = "is")
            val rot = remember { Animatable(0f) }
            LaunchedEffect(sel) {
                if (sel && shown) { rot.snapTo(-22f); rot.animateTo(0f, spring(0.3f, 260f)) }
            }
            Row(
                Modifier.animateContentSize(spring(0.78f, 420f))
                    .clip(RoundedCornerShape(28.dp)).background(bg)
                    .pressable { onSelect(i) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    icon, null, tint = fg,
                    modifier = Modifier.size(22.dp).graphicsLayer { scaleX = iconScale; scaleY = iconScale; rotationZ = rot.value },
                )
                AnimatedVisibility(
                    sel,
                    enter = fadeIn(tween(260, delayMillis = 80)) + expandHorizontally(spring(0.8f, 420f)),
                    exit = fadeOut(tween(120)) + shrinkHorizontally(tween(220)),
                ) {
                    Row {
                        Spacer(Modifier.size(8.dp))
                        Txt(label, 14.sp, FontWeight.Medium, fg)
                    }
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
                if (!engine.hormonal && idx(seg.fertileEnd) > idx(seg.periodEnd)) arc(idx(seg.fertileStart), idx(seg.fertileEnd), C.Lime)
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
private fun HomeTab(vm: AppVM, onOpenDay: (LocalDate) -> Unit) {
    val s = vm.snap ?: return
    val me = s.me
    val isHer = me.role == "her"
    val owner = s.owner
    val today = remember { LocalDate.now() }
    val engine = remember(s.logs, owner) {
        owner?.toEngine(today, s.logs)
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
                if (ov?.hasData == true && engine != null) {
                    Spacer(Modifier.height(16.dp))
                    Reveal(3, Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Color.White.copy(alpha = 0.14f))
                                .padding(vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            HeroStat("روز چرخه", ov.cycleDay?.let { Fa.n(it) } ?: "—")
                            HeroStat("طول چرخه", "${Fa.n(ov.cycleLength)} روز")
                            HeroStat("طول پریود", "${Fa.n(engine.predictedPeriod)} روز")
                            HeroStat("دقت", "±${Fa.n(ov.variability)}")
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }

        // ---------------- cards
        Column(Modifier.padding(horizontal = 20.dp).padding(top = 22.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            if (!isHer && owner == null) {
                Reveal(0) { ConnectCard(vm, s.pairs) }
            } else if (paused) {
                Reveal(0) { Card { Txt("$ownerName اشتراک‌گذاری رو موقتاً متوقف کرده؛ وقتی فعالش کنه دوباره اطلاعات می‌رسه.", 15.sp, color = C.Muted) } }
            } else {
                // ---- today
                Reveal(0) {
                    HomeSection(if (isHer) "امروز" else "امروز، یه نگاه") {
                        if ((ov?.lateDays ?: 0) >= 1) LateCard(vm, ov!!.lateDays, isHer, ownerName, today)
                        if (isHer) TodayCard(vm, engine, today)
                        MoodCard(vm, today, isHer, ownerName, onOpenDay)
                        if (!isHer) {
                            Card {
                                Column {
                                    Txt("امروز چه کار کنم؟", 14.sp, color = C.Muted)
                                    Spacer(Modifier.height(6.dp))
                                    Txt(Fa.partnerTip(ov?.phase, ownerName, Fa.isPartnerLabel(s.activePair?.let { vm.labelOf(it) })), 16.sp)
                                }
                            }
                        } else if (ov?.hasData == true) {
                            Card { Column { Txt("برای تو", 14.sp, color = C.Muted); Spacer(Modifier.height(6.dp)); Txt(Fa.herTip(ov.phase), 16.sp) } }
                        }
                    }
                }
                // ---- upcoming
                if (ov?.hasData == true) Reveal(1) {
                    HomeSection("پیش رو") { UpcomingCard(ov) }
                }
                // ---- messages
                if (s.activePair != null) Reveal(2) {
                    HomeSection("پیام‌ها") { MessagesCard(vm) }
                }
            }
            // ---- partner sharing
            if (isHer) Reveal(3) {
                HomeSection("پارتنر") { ShareCard(vm) }
            }
            Spacer(Modifier.height(86.dp))
        }
    }
}

@Composable
private fun HomeSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Txt(title, 13.sp, FontWeight.Medium, C.Muted, Modifier.padding(horizontal = 6.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
private fun HeroStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Txt(value, 16.sp, FontWeight.Medium, Color.White)
        Txt(label, 11.sp, color = Color.White.copy(alpha = 0.75f))
    }
}

@Composable
private fun LateCard(vm: AppVM, days: Int, isHer: Boolean, name: String, today: LocalDate) {
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
private fun MoodCard(vm: AppVM, today: LocalDate, isHer: Boolean, name: String, onOpenDay: (LocalDate) -> Unit) {
    val s = vm.snap ?: return
    val dl = s.days[today]
    Card(Modifier.pressable { onOpenDay(today) }) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Txt(if (isHer) "حال امروز" else "حال امروز $name", 14.sp, color = C.Muted)
            if (dl?.mood != null || !dl?.symptoms.isNullOrEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (dl?.mood != null) {
                        Txt(Fa.moods[dl.mood - 1], 34.sp, lineHeight = 40.sp)
                        Spacer(Modifier.size(12.dp))
                        Column {
                            Txt(Fa.moodNames[dl.mood - 1], 16.sp, FontWeight.Medium)
                            if (dl.energy != null) Txt("انرژی ${Fa.energyNames[dl.energy - 1]}", 12.sp, color = C.Muted)
                        }
                    }
                }
                val shown = listOfNotNull(Fa.flowOf(dl!!.symptoms)?.let { "شدت: ${Fa.flowName(it)}" }) +
                    Fa.realSymptoms(dl.symptoms).map { Fa.symptomName(it) }
                if (shown.isNotEmpty()) Txt(shown.joinToString("، "), 13.sp, color = C.Muted)
            } else {
                Txt(if (isHer) "هنوز ثبت نکردی؛ بزن تا بگی امروز چه خبره 😄" else "هنوز چیزی ثبت نشده.", 15.sp, color = C.Muted)
            }
        }
    }
}

@Composable
private fun MessagesCard(vm: AppVM) {
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
                EventRow(C.Coral, "پریود بعدی", Fa.daysText(ov.daysToPeriod ?: 0), Fa.range(d, ov.variability))
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

// ---------------------------------------------------------------- share / pairing
@Composable
private fun ShareCard(vm: AppVM) {
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
            RoundIcon(Icons.Rounded.Close, C.CoralSoft, C.Text) { vm.removePair(p.id) }
        } else {
            RoundIcon(Icons.Rounded.Close, C.Paper, C.Muted) { vm.removePair(p.id) }
        }
    }
}

@Composable
internal fun RoundIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, bg: Color, fg: Color, onClick: () -> Unit) {
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
private fun CalendarTab(vm: AppVM, onOpenDay: (LocalDate) -> Unit) {
    val s = vm.snap ?: return
    val isHer = s.me.role == "her"
    val owner = s.owner
    val today = remember { LocalDate.now() }
    val engine = remember(s.logs, owner) {
        owner?.toEngine(today, s.logs)
    }
    var month by rememberSaveable { mutableStateOf(Fa.jalaliMonth(today).first.toString()) }
    val first = LocalDate.parse(month)
    val len = Fa.jalaliMonth(first).second

    Column(Modifier.fillMaxSize().background(C.Paper).statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            RoundIcon(Icons.Rounded.ChevronRight, C.Surface, C.Text) { month = Fa.jalaliMonth(first.minusDays(1)).first.toString() }
            AnimatedContent(
                month,
                modifier = Modifier.weight(1f),
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                label = "mt",
            ) { m -> Txt(Fa.date(LocalDate.parse(m), "MMMM y"), 22.sp, FontWeight.Light, align = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
            RoundIcon(Icons.Rounded.ChevronLeft, C.Surface, C.Text) { month = first.plusDays(len.toLong()).toString() }
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
                                        if (i in 0 until l) DayCell(f.plusDays(i.toLong()), today, engine, s.days) { onOpenDay(it) }
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
                if (engine?.hormonal != true) {
                    Legend(C.LimeSoft, "پنجره‌ی باروری")
                    Legend(C.Lime, "تخمک‌گذاری")
                }
                Legend(C.Text.copy(alpha = 0.55f), "حال یا علائم ثبت شده")
                Txt("روی هر روز بزن تا جزئیاتش رو ببینی.", 12.sp, color = C.Muted)
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
private fun DayCell(d: LocalDate, today: LocalDate, engine: CycleEngine?, days: Map<LocalDate, dev.imhamid.period.data.DayLog>, onTap: (LocalDate) -> Unit) {
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
            .then(if (isToday) Modifier.border(2.dp, C.Text, CircleShape) else Modifier)
            .then(if (!future) Modifier.pressable { onTap(d) } else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Txt(Fa.jalaliDay(d), 14.sp, if (logged || isToday) FontWeight.Bold else FontWeight.Normal, if (future && bg == Color.Transparent) C.Muted else C.Text)
        if (days[d]?.let { it.mood != null || it.symptoms.isNotEmpty() } == true) {
            Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 3.dp).size(4.dp).clip(CircleShape).background(C.Text.copy(alpha = 0.55f)))
        }
    }
}
