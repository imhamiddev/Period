package dev.imhamid.period.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.imhamid.period.update.UpdateState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.absoluteValue

// ============================================================================ boot
@Composable
fun BootScreen() {
    val t = rememberInfiniteTransition(label = "boot")
    val s by t.animateFloat(0.94f, 1.06f, infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "s")
    Aurora(Modifier.fillMaxSize()) {
        Box(
            Modifier.align(Alignment.Center).scale(s).size(104.dp).clip(RoundedCornerShape(32.dp))
                .background(Color.White.copy(alpha = 0.2f))
                .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(32.dp)),
            contentAlignment = Alignment.Center,
        ) { DropMark(54.dp) }
    }
}

// ============================================================================ intro
@Composable
fun IntroScreen(vm: AppVM) {
    val pager = rememberPagerState { 3 }
    val scope = rememberCoroutineScope()
    Aurora(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp)) {
            HorizontalPager(pager, Modifier.weight(1f)) { page ->
                val offset = ((pager.currentPage - page) + pager.currentPageOffsetFraction).absoluteValue
                Box(
                    Modifier.fillMaxSize().graphicsLayer {
                        alpha = 1f - offset.coerceIn(0f, 1f) * 0.8f
                        translationX = 0f
                    },
                ) {
                    when (page) {
                        0 -> IntroLogo()
                        1 -> IntroWords()
                        else -> IntroPitch()
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(bottom = 18.dp), horizontalArrangement = Arrangement.Center) {
                repeat(3) { i ->
                    val sel = pager.currentPage == i
                    val w by animateDpAsState(if (sel) 22.dp else 7.dp, spring(0.7f), label = "dw")
                    val a by animateFloatAsState(if (sel) 1f else 0.45f, label = "da")
                    Box(Modifier.padding(horizontal = 3.dp).size(w, 7.dp).clip(CircleShape).background(Color.White.copy(alpha = a)))
                }
            }
            val last = pager.currentPage == 2
            PrimaryButton(
                text = if (last) "شروع کنیم" else "ادامه",
                kind = BtnKind.Light,
                onClick = {
                    if (last) vm.finishIntro() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                },
            )
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun IntroLogo() {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Reveal(0) {
            Box(
                Modifier.size(150.dp).clip(RoundedCornerShape(46.dp)).background(Color.White.copy(alpha = 0.2f))
                    .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(46.dp)),
                contentAlignment = Alignment.Center,
            ) { DropMark(78.dp) }
        }
        Spacer(Modifier.height(28.dp))
        Reveal(2) { Txt("Luna", 44.sp, FontWeight.Light, Color.White) }
        Reveal(3) { Txt("چرخه‌ی تو، در آرامش", 16.sp, color = Color.White.copy(alpha = 0.75f)) }
    }
}

@Composable
private fun IntroWords() {
    val words = listOf("پیش‌بینی", "آگاهی", "همراهی", "آرامش")
    var idx by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { while (true) { delay(1500); idx = (idx + 1) % words.size } }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        words.forEachIndexed { i, w ->
            val active = i == idx
            val a by animateFloatAsState(if (active) 1f else 0.32f, tween(500), label = "wa")
            val sc by animateFloatAsState(if (active) 1f else 0.9f, spring(0.7f), label = "ws")
            Row(
                Modifier.padding(vertical = 6.dp).graphicsLayer { alpha = a; scaleX = sc; scaleY = sc; transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 0.5f) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val iconW by animateDpAsState(if (active) 46.dp else 0.dp, tween(450), label = "iw")
                if (iconW > 1.dp) {
                    Box(Modifier.size(iconW).clip(RoundedCornerShape(14.dp)).background(C.Lime), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.WaterDrop, null, tint = C.Deep, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.size(14.dp))
                }
                Txt(w, 46.sp, FontWeight.Light, Color.White)
            }
        }
        Spacer(Modifier.height(26.dp))
        Txt("وقتی بدونی چی در راهه، مهربون‌تر می‌شی.", 16.sp, color = Color.White.copy(alpha = 0.8f))
    }
}

@Composable
private fun IntroPitch() {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Reveal(0) { Txt("چرخه‌ات رو با کسی که دوستش داری شریک شو.", 36.sp, FontWeight.Light, Color.White, lineHeight = 52.sp) }
        Spacer(Modifier.height(18.dp))
        Reveal(2) {
            Txt(
                "پریودت رو ثبت کن. اگه خواستی، پارتنرت قبل از PMS و پریود با یه نوتیف مهربون باخبر می‌شه — بدون اینکه چیزی رو بپرسه.",
                15.sp, color = Color.White.copy(alpha = 0.8f),
            )
        }
    }
}

// ============================================================================ auth
@Composable
fun AuthScreen(vm: AppVM) {
    var email by rememberSaveable { mutableStateOf("") }
    var pass by rememberSaveable { mutableStateOf("") }
    Box(Modifier.fillMaxSize().background(C.Deep)) {
        Aurora(Modifier.fillMaxWidth().fillMaxHeight(0.42f)) {
            Column(Modifier.align(Alignment.Center).statusBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
                Reveal(0) {
                    Box(
                        Modifier.size(84.dp).clip(RoundedCornerShape(26.dp)).background(Color.White.copy(alpha = 0.2f))
                            .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(26.dp)),
                        contentAlignment = Alignment.Center,
                    ) { DropMark(44.dp) }
                }
                Spacer(Modifier.height(12.dp))
                Reveal(1) { Txt("Luna", 28.sp, FontWeight.Light, Color.White) }
            }
        }
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(0.62f)
                .clip(RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp)).background(C.Paper)
                .navigationBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
        ) {
            Reveal(0) { Txt("خوش اومدی", 30.sp, FontWeight.Light) }
            Spacer(Modifier.height(6.dp))
            Reveal(1) { Txt("ایمیل و رمزت رو بزن؛ اگه حساب نداشته باشی خودم می‌سازمش.", 14.sp, color = C.Muted) }
            Spacer(Modifier.height(22.dp))
            Reveal(2) { Field(email, { email = it.trim() }, "ایمیل", keyboardType = KeyboardType.Email, ltr = true) }
            Spacer(Modifier.height(14.dp))
            Reveal(3) {
                Field(pass, { pass = it }, "رمز عبور", password = true, imeAction = ImeAction.Go, onDone = { vm.submitAuth(email, pass) })
            }
            Spacer(Modifier.height(24.dp))
            Reveal(4) {
                PrimaryButton("ادامه", { vm.submitAuth(email, pass) }, loading = vm.busy, enabled = email.isNotBlank() && pass.isNotBlank())
            }
            if (!vm.configured) {
                Spacer(Modifier.height(14.dp))
                Txt("⚠️ Supabase هنوز تنظیم نشده", 12.sp, color = C.Coral)
            }
        }
    }
}

// ============================================================================ name
@Composable
fun NameScreen(vm: AppVM) {
    var name by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxSize().background(C.Paper).statusBarsPadding().navigationBarsPadding().imePadding().padding(24.dp)) {
        Spacer(Modifier.height(48.dp))
        Reveal(0) { Txt("اسمت چیه؟", 40.sp, FontWeight.Light) }
        Spacer(Modifier.height(8.dp))
        Reveal(1) { Txt("اسمی که دوست داری صدات کنم.", 16.sp, color = C.Muted) }
        Spacer(Modifier.height(32.dp))
        Reveal(2) { Field(name, { name = it }, "نام", imeAction = ImeAction.Done, onDone = { vm.saveName(name) }) }
        Spacer(Modifier.weight(1f))
        PrimaryButton("ادامه", { vm.saveName(name) }, loading = vm.busy, enabled = name.isNotBlank())
    }
}

// ============================================================================ role
@Composable
fun RoleScreen(vm: AppVM) {
    var role by rememberSaveable { mutableStateOf<String?>(null) }
    val name = vm.snap?.me?.name.orEmpty()
    Column(Modifier.fillMaxSize().background(C.Paper).statusBarsPadding().navigationBarsPadding().padding(24.dp)) {
        Spacer(Modifier.height(40.dp))
        Reveal(0) { Txt("سلام $name 👋", 36.sp, FontWeight.Light) }
        Spacer(Modifier.height(8.dp))
        Reveal(1) { Txt("تو برنامه چه نقشی داری؟", 16.sp, color = C.Muted) }
        Spacer(Modifier.height(28.dp))
        Reveal(2) {
            RoleCard("her", role, "پریودم رو ثبت می‌کنم", "چرخه‌ی خودم رو دنبال می‌کنم و اگه بخوام به پارتنرم خبر می‌دم.", Icons.Rounded.WaterDrop) { role = "her" }
        }
        Spacer(Modifier.height(14.dp))
        Reveal(3) {
            RoleCard("partner", role, "پارتنرم و می‌خوام خبر داشته باشم", "با کد پارتنرم وصل می‌شم و قبل از PMS و پریود نوتیف می‌گیرم.", Icons.Rounded.Favorite) { role = "partner" }
        }
        Spacer(Modifier.weight(1f))
        PrimaryButton("ادامه", { role?.let { vm.saveRole(it) } }, loading = vm.busy, enabled = role != null)
    }
}

@Composable
private fun RoleCard(id: String, selected: String?, title: String, desc: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    val sel = selected == id
    val border by animateColorAsState(if (sel) C.Green else C.Line, tween(250), label = "rb")
    val bg by animateColorAsState(if (sel) C.LimeSoft else Color.White, tween(250), label = "rbg")
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(bg)
            .border(if (sel) 2.dp else 1.dp, border, RoundedCornerShape(28.dp))
            .pressable(onClick = onClick).padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(C.Lime), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = C.Deep)
        }
        Spacer(Modifier.size(16.dp))
        Column(Modifier.weight(1f)) {
            Txt(title, 17.sp, FontWeight.Medium)
            Spacer(Modifier.height(2.dp))
            Txt(desc, 13.sp, color = C.Muted)
        }
        val a by animateFloatAsState(if (sel) 1f else 0f, spring(0.5f), label = "ck")
        Box(Modifier.size(26.dp).scale(a).alpha(a).clip(CircleShape).background(C.Green), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
    }
}

// ============================================================================ setup (her)
@Composable
fun SetupScreen(vm: AppVM) {
    val total = 6
    var step by rememberSaveable { mutableIntStateOf(0) }
    val today = remember { LocalDate.now() }
    var lastStart by rememberSaveable { mutableStateOf(today.minusDays(10).toString()) }
    var earlier by rememberSaveable { mutableStateOf(listOf<String>()) }
    var cycle by rememberSaveable { mutableIntStateOf(28) }
    var period by rememberSaveable { mutableIntStateOf(5) }
    var pms by rememberSaveable { mutableIntStateOf(5) }
    var regularity by rememberSaveable { mutableIntStateOf(0) }
    var hormonal by rememberSaveable { mutableStateOf(false) }

    fun next() {
        if (step == 1) {
            val all = (earlier + lastStart).map { LocalDate.parse(it) }.sorted()
            if (all.size >= 2) {
                val gaps = all.zipWithNext { x, y -> java.time.temporal.ChronoUnit.DAYS.between(x, y).toInt() }
                cycle = Math.round(gaps.average()).toInt().coerceIn(20, 45)
            }
        }
        if (step < total - 1) step++ else vm.saveSetup(
            LocalDate.parse(lastStart),
            earlier.map { LocalDate.parse(it) }.filter { it < LocalDate.parse(lastStart).minusDays(14) },
            cycle, period, pms, regularity, hormonal,
        )
    }

    Box(Modifier.fillMaxSize().background(C.Paper)) {
        Aurora(Modifier.fillMaxWidth().height(250.dp)) {
            Column(Modifier.statusBarsPadding().padding(24.dp)) {
                Txt("چند تا سؤال کوتاه", 15.sp, color = Color.White.copy(alpha = 0.85f))
                Spacer(Modifier.height(10.dp))
                StepBars(total, step)
                Spacer(Modifier.height(10.dp))
                Txt("مرحله‌ی ${Fa.n(step + 1)} از ${Fa.n(total)}", 13.sp, color = Color.White.copy(alpha = 0.75f))
            }
        }
        Column(
            Modifier.fillMaxSize().padding(top = 190.dp)
                .clip(RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp)).background(C.Paper)
                .navigationBarsPadding().padding(24.dp),
        ) {
            AnimatedContent(
                step,
                modifier = Modifier.weight(1f),
                transitionSpec = {
                    val d = if (targetState > initialState) -1 else 1
                    (slideInHorizontally(tween(480, easing = androidx.compose.animation.core.FastOutSlowInEasing)) { d * it / 3 } + fadeIn(tween(420))) togetherWith
                        (slideOutHorizontally(tween(380, easing = androidx.compose.animation.core.FastOutSlowInEasing)) { -d * it / 3 } + fadeOut(tween(220)))
                },
                label = "setup",
            ) { s ->
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    when (s) {
                        0 -> {
                            Txt("آخرین پریودت کی شروع شد؟", 26.sp, FontWeight.Light)
                            Spacer(Modifier.height(6.dp))
                            Txt("حدودی هم باشه مشکلی نیست.", 14.sp, color = C.Muted)
                            Spacer(Modifier.height(22.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items((0..60).map { today.minusDays(it.toLong()) }) { d ->
                                    DayChip(d, d.toString() == lastStart) {
                                        lastStart = d.toString()
                                        earlier = earlier.filter { LocalDate.parse(it) < d.minusDays(14) }
                                    }
                                }
                            }
                            Spacer(Modifier.height(18.dp))
                            Txt("انتخاب‌شده: ${Fa.date(LocalDate.parse(lastStart), "EEEE d MMMM")}", 15.sp, FontWeight.Medium, C.Green)
                        }
                        1 -> {
                            val base = LocalDate.parse(lastStart)
                            Txt("پریودهای قبلی (اختیاری)", 26.sp, FontWeight.Light)
                            Spacer(Modifier.height(6.dp))
                            Txt("اگه یادت هست، تاریخ شروع ۲ یا ۳ پریود قبلی رو بزن تا دقت پیش‌بینی از همون اول بیشتر بشه.", 14.sp, color = C.Muted)
                            Spacer(Modifier.height(22.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items((15..230).map { base.minusDays(it.toLong()) }) { d ->
                                    DayChip(d, d.toString() in earlier) {
                                        val ds = d.toString()
                                        earlier = when {
                                            ds in earlier -> earlier - ds
                                            earlier.size >= 3 -> { vm.error = "حداکثر ۳ تا"; earlier }
                                            earlier.any { kotlin.math.abs(java.time.temporal.ChronoUnit.DAYS.between(LocalDate.parse(it), d)) < 15 } -> {
                                                vm.error = "این تاریخ به یکی از انتخاب‌هات خیلی نزدیکه"; earlier
                                            }
                                            else -> earlier + ds
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(18.dp))
                            Txt(
                                if (earlier.isEmpty()) "چیزی انتخاب نشده؛ می‌تونی رد کنی." else "${Fa.n(earlier.size)} پریود انتخاب شد",
                                15.sp, FontWeight.Medium, C.Green,
                            )
                        }
                        2 -> {
                            Txt("طول معمول چرخه‌ات؟", 26.sp, FontWeight.Light)
                            Spacer(Modifier.height(6.dp))
                            Txt("از اولین روز یه پریود تا اولین روز پریود بعدی.", 14.sp, color = C.Muted)
                            Spacer(Modifier.height(36.dp))
                            Stepper(cycle, 20, 45, "روز") { cycle = it }
                        }
                        3 -> {
                            Txt("پریودت معمولاً چند روزه؟", 26.sp, FontWeight.Light)
                            Spacer(Modifier.height(6.dp))
                            Txt("با گذر زمان خودم دقیق‌ترش می‌کنم.", 14.sp, color = C.Muted)
                            Spacer(Modifier.height(36.dp))
                            Stepper(period, 2, 10, "روز") { period = it }
                        }
                        4 -> {
                            Txt("PMS چند روز قبل از پریودت شروع می‌شه؟", 26.sp, FontWeight.Light, lineHeight = 38.sp)
                            Spacer(Modifier.height(6.dp))
                            Txt("معمولاً بین ۳ تا ۷ روزه؛ نوتیف‌های PMS به همین عدد وابسته‌ست.", 14.sp, color = C.Muted)
                            Spacer(Modifier.height(36.dp))
                            Stepper(pms, 1, 10, "روز") { pms = it }
                        }
                        else -> {
                            Txt("دو تا سؤال آخر", 26.sp, FontWeight.Light)
                            Spacer(Modifier.height(18.dp))
                            Txt("چرخه‌ات چقدر منظمه؟", 15.sp, FontWeight.Medium)
                            Spacer(Modifier.height(10.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OptionRow("منظم", "هر ماه تقریباً سر همون موعد (±۲ روز)", regularity == 0) { regularity = 0 }
                                OptionRow("کمی بالا و پایین", "چند روز جلو و عقب می‌شه (±۴ روز)", regularity == 1) { regularity = 1 }
                                OptionRow("نامنظم", "خیلی فرق می‌کنه (±۷ روز یا بیشتر)", regularity == 2) { regularity = 2 }
                            }
                            Spacer(Modifier.height(22.dp))
                            Txt("روش هورمونی استفاده می‌کنی؟", 15.sp, FontWeight.Medium)
                            Txt("قرص، آی‌یودی هورمونی، آمپول و…", 12.sp, color = C.Muted)
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(Modifier.weight(1f)) { OptionRow("نه", "", !hormonal) { hormonal = false } }
                                Box(Modifier.weight(1f)) { OptionRow("بله", "", hormonal) { hormonal = true } }
                            }
                            if (hormonal) {
                                Spacer(Modifier.height(8.dp))
                                Txt("پس تخمک‌گذاری و پنجره‌ی باروری رو نشون نمی‌دم، چون با روش هورمونی معنی نداره.", 12.sp, color = C.Muted)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (step > 0) {
                    Box(Modifier.width(110.dp)) { PrimaryButton("قبلی", { step-- }, kind = BtnKind.Light, modifier = Modifier.border(1.dp, C.Line, RoundedCornerShape(29.dp))) }
                }
                Box(Modifier.weight(1f)) {
                    PrimaryButton(
                        when {
                            step == total - 1 -> "تمام"
                            step == 1 && earlier.isEmpty() -> "رد کردن"
                            else -> "بعدی"
                        },
                        { next() },
                        loading = vm.busy,
                    )
                }
            }
        }
    }
}

@Composable
private fun OptionRow(title: String, desc: String, selected: Boolean, onClick: () -> Unit) {
    val border by animateColorAsState(if (selected) C.Green else C.Line, tween(250), label = "ob")
    val bg by animateColorAsState(if (selected) C.LimeSoft else Color.White, tween(250), label = "obg")
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(bg)
            .border(if (selected) 2.dp else 1.dp, border, RoundedCornerShape(20.dp))
            .pressable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Txt(title, 15.sp, FontWeight.Medium)
            if (desc.isNotEmpty()) Txt(desc, 12.sp, color = C.Muted)
        }
        val a by animateFloatAsState(if (selected) 1f else 0f, spring(0.5f), label = "ock")
        Box(Modifier.size(22.dp).scale(a).alpha(a).clip(CircleShape).background(C.Green), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
        }
    }
}

private fun Modifier.width(w: androidx.compose.ui.unit.Dp) = this.then(Modifier.size(w, 58.dp))

@Composable
private fun DayChip(d: LocalDate, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(if (selected) C.Ink else Color.White, tween(250), label = "dcb")
    val fg by animateColorAsState(if (selected) Color.White else C.Ink, tween(250), label = "dcf")
    Column(
        Modifier.clip(RoundedCornerShape(20.dp)).background(bg).border(1.dp, C.Line, RoundedCornerShape(20.dp))
            .pressable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Txt(Fa.date(d, "EEE"), 12.sp, color = fg.copy(alpha = 0.7f))
        Txt(Fa.jalaliDay(d), 20.sp, FontWeight.Medium, fg)
        Txt(Fa.date(d, "MMM"), 11.sp, color = fg.copy(alpha = 0.7f))
    }
}

@Composable
private fun Stepper(value: Int, min: Int, max: Int, unit: String, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        StepBtn(Icons.Rounded.Add) { if (value < max) onChange(value + 1) }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AnimatedContent(
                value,
                transitionSpec = {
                    val up = targetState > initialState
                    (slideInVertically(tween(300)) { if (up) it else -it } + fadeIn(tween(300))) togetherWith
                        (slideOutVertically(tween(300)) { if (up) -it else it } + fadeOut(tween(200)))
                },
                label = "num",
            ) { v -> Txt(Fa.n(v), 84.sp, FontWeight.Light, lineHeight = 100.sp) }
            Txt(unit, 16.sp, color = C.Muted)
        }
        StepBtn(Icons.Rounded.Remove) { if (value > min) onChange(value - 1) }
    }
}

@Composable
private fun StepBtn(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(
        Modifier.size(64.dp).clip(CircleShape).background(Color.White).border(1.dp, C.Line, CircleShape).pressable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = C.Ink) }
}

// ============================================================================ update banner
@Composable
fun UpdateBanner(vm: AppVM) {
    val u = vm.update
    Box(Modifier.fillMaxWidth().background(C.Ink).statusBarsPadding()) {
        // progress fill
        if (u is UpdateState.Downloading) {
            val p by animateFloatAsState(u.progress, tween(200), label = "pg")
            Box(Modifier.fillMaxWidth(p).height(40.dp).background(C.Green.copy(alpha = 0.55f)).align(Alignment.CenterStart))
        }
        Row(
            Modifier.fillMaxWidth().height(40.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val text = when (u) {
                is UpdateState.Available -> "نسخه‌ی ${Fa.digits(u.version)} آمده"
                is UpdateState.Downloading -> "در حال دانلود… ${Fa.n((u.progress * 100).toInt())}٪"
                is UpdateState.Ready -> "آماده‌ی نصب است"
                is UpdateState.Failed -> "دانلود ناموفق بود"
                UpdateState.None -> ""
            }
            Txt(text, 13.sp, FontWeight.Medium, Color.White)
            val action = when (u) {
                is UpdateState.Available -> "دانلود"
                is UpdateState.Ready -> "نصب"
                is UpdateState.Failed -> "تلاش دوباره"
                else -> null
            }
            if (action != null) {
                Row(
                    Modifier.clip(RoundedCornerShape(14.dp)).background(C.Lime)
                        .pressable { if (u is UpdateState.Ready) vm.install() else vm.startDownload() }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (u !is UpdateState.Ready) {
                        Icon(Icons.Rounded.Download, null, tint = C.Deep, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(4.dp))
                    }
                    Txt(action, 13.sp, FontWeight.Bold, C.Deep)
                }
            }
        }
    }
}
