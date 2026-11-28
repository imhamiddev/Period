package dev.imhamid.period.ui

import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.imhamid.period.update.UpdateState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate

// ============================================================================ splash
@Composable
fun BootScreen() {
    val t = rememberInfiniteTransition(label = "boot")
    val s by t.animateFloat(0.94f, 1.06f, infiniteRepeatable(tween(1300), RepeatMode.Reverse), label = "s")
    Box(Modifier.fillMaxSize().background(C.Pink), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.scale(s).size(104.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.WaterDrop, null, tint = C.Pink, modifier = Modifier.size(58.dp))
            }
            Txt("لونا", 34.sp, FontWeight.Bold, Color.White, Modifier.padding(top = 18.dp))
        }
    }
}

// ============================================================================ walkthrough + welcome
private class Slide(val title: String, val text: String, val color: Color)

private val slides = listOf(
    Slide("پریودت رو دقیق دنبال کن", "با چند لمس چرخه‌ات رو ثبت کن و پیش‌بینی پریود، PMS و تخمک‌گذاری رو ببین.", Color(0xFFFFC9D9)),
    Slide("علائم و حال‌وهوات رو ثبت کن", "جریان، علائم، حال‌وهوا، وزن، دما و آب؛ همه‌چیز یه‌جا و فقط برای خودت.", Color(0xFFC4E6FA)),
    Slide("پارتنرت هم در جریان باشه", "اگه بخوای، پارتنرت قبل از PMS و پریود خبردار می‌شه و می‌تونه مهربون‌تر کنارت باشه.", Color(0xFFDCD2FF)),
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IntroScreen(vm: AppVM) {
    val pager = rememberPagerState { slides.size + 1 }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().background(C.Surface).statusBarsPadding().navigationBarsPadding()) {
        HorizontalPager(pager, Modifier.weight(1f)) { i ->
            Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                if (i < slides.size) {
                    val sl = slides[i]
                    Uterus(230.dp, sl.color)
                    Spacer(Modifier.height(32.dp))
                    Txt(sl.title, 26.sp, FontWeight.Bold, align = TextAlign.Center)
                    Txt(sl.text, 16.sp, color = C.Muted, align = TextAlign.Center, modifier = Modifier.padding(top = 10.dp), lineHeight = 26.sp)
                } else {
                    Box(Modifier.size(120.dp).clip(CircleShape).background(C.PinkSoft), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.WaterDrop, null, tint = C.Pink, modifier = Modifier.size(70.dp))
                    }
                    Txt("به لونا خوش اومدی", 28.sp, FontWeight.Bold, modifier = Modifier.padding(top = 24.dp))
                    Txt("همراه هوشمند چرخه‌ی تو", 16.sp, color = C.Muted, modifier = Modifier.padding(top = 6.dp))
                    Spacer(Modifier.height(36.dp))
                    BigButton("ثبت‌نام", { vm.authMode = AuthMode.SignUp; vm.finishIntro() })
                    Spacer(Modifier.height(12.dp))
                    Box(
                        Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(28.dp)).background(C.PinkSoft).pressable { vm.authMode = AuthMode.SignIn; vm.finishIntro() },
                        contentAlignment = Alignment.Center,
                    ) { Txt("ورود", 17.sp, FontWeight.Bold, C.Pink) }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalArrangement = Arrangement.Center) {
            repeat(slides.size + 1) { i ->
                Box(Modifier.padding(horizontal = 4.dp).size(if (pager.currentPage == i) 24.dp else 8.dp, 8.dp).clip(RoundedCornerShape(4.dp)).background(if (pager.currentPage == i) C.Pink else C.Track))
            }
        }
        if (pager.currentPage < slides.size) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Txt("رد کردن", 16.sp, FontWeight.Medium, C.Muted, Modifier.pressable { scope.launch { pager.animateScrollToPage(slides.size) } }.padding(12.dp))
                BigButton("ادامه", { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } }, Modifier.weight(1f))
            }
        } else Spacer(Modifier.height(16.dp))
    }
}

// ============================================================================ auth
@Composable
fun AuthScreen(vm: AppVM) {
    AnimatedContent(
        targetState = vm.authMode,
        transitionSpec = { (fadeIn(tween(260)) + slideInHorizontally(tween(260)) { it / 6 }) togetherWith (fadeOut(tween(140)) + slideOutHorizontally(tween(200)) { -it / 8 }) },
        label = "auth",
    ) { mode ->
        Column(Modifier.fillMaxSize().background(C.Surface).statusBarsPadding().navigationBarsPadding().imePadding()) {
            when (mode) {
                AuthMode.SignIn -> SignInForm(vm)
                AuthMode.SignUp -> SignUpForm(vm)
                AuthMode.Forgot -> ForgotForm(vm)
                AuthMode.Otp -> OtpForm(vm)
                AuthMode.NewPass -> NewPassForm(vm)
                AuthMode.Updated -> UpdatedForm(vm)
            }
        }
    }
}

@Composable
private fun AuthHeader(title: String, sub: String) {
    Column(Modifier.padding(top = 36.dp, bottom = 24.dp)) {
        Box(Modifier.size(56.dp).clip(CircleShape).background(C.PinkSoft), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.WaterDrop, null, tint = C.Pink, modifier = Modifier.size(30.dp)) }
        Txt(title, 28.sp, FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))
        Txt(sub, 15.sp, color = C.Muted, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun SignInForm(vm: AppVM) {
    var email by rememberSaveable { mutableStateOf("") }
    var pass by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
        AuthHeader("خوش برگشتی!", "برای ادامه وارد حسابت شو")
        Field(email, { email = it.trim() }, "ایمیل", keyboardType = KeyboardType.Email, ltr = true)
        Spacer(Modifier.height(14.dp))
        Field(pass, { pass = it }, "رمز عبور", password = true, imeAction = ImeAction.Done, onDone = { vm.signIn(email, pass) })
        Txt("فراموشی رمز؟", 14.sp, FontWeight.Medium, C.Pink, Modifier.align(Alignment.End).pressable { vm.authMode = AuthMode.Forgot }.padding(vertical = 12.dp))
        Spacer(Modifier.height(8.dp))
        BigButton("ورود", { vm.signIn(email, pass) }, busy = vm.busy)
        Row(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalArrangement = Arrangement.Center) {
            Txt("حساب نداری؟ ", 15.sp, color = C.Muted)
            Txt("ثبت‌نام", 15.sp, FontWeight.Bold, C.Pink, Modifier.pressable { vm.authMode = AuthMode.SignUp })
        }
    }
}

@Composable
private fun SignUpForm(vm: AppVM) {
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var pass by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
        AuthHeader("ساخت حساب", "چند ثانیه وقت می‌بره")
        Field(name, { name = it.take(30) }, "نام")
        Spacer(Modifier.height(14.dp))
        Field(email, { email = it.trim() }, "ایمیل", keyboardType = KeyboardType.Email, ltr = true)
        Spacer(Modifier.height(14.dp))
        Field(pass, { pass = it }, "رمز عبور (حداقل ۸ کاراکتر)", password = true, imeAction = ImeAction.Done, onDone = { vm.signUp(name, email, pass) })
        Spacer(Modifier.height(24.dp))
        BigButton("ثبت‌نام", { vm.signUp(name, email, pass) }, busy = vm.busy)
        Row(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalArrangement = Arrangement.Center) {
            Txt("حساب داری؟ ", 15.sp, color = C.Muted)
            Txt("ورود", 15.sp, FontWeight.Bold, C.Pink, Modifier.pressable { vm.authMode = AuthMode.SignIn })
        }
    }
}

@Composable
private fun ForgotForm(vm: AppVM) {
    var email by rememberSaveable { mutableStateOf(vm.resetEmail) }
    TopBar("", onBack = { vm.authMode = AuthMode.SignIn })
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
        AuthHeader("فراموشی رمز", "ایمیلت رو بنویس تا یه کد ۶ رقمی برات بفرستیم")
        Field(email, { email = it.trim() }, "ایمیل", keyboardType = KeyboardType.Email, ltr = true, imeAction = ImeAction.Done, onDone = { vm.requestReset(email) })
        Spacer(Modifier.height(24.dp))
        BigButton("ارسال کد", { vm.requestReset(email) }, busy = vm.busy)
    }
}

@Composable
private fun OtpForm(vm: AppVM) {
    var code by rememberSaveable { mutableStateOf("") }
    TopBar("", onBack = { vm.authMode = AuthMode.Forgot })
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
        AuthHeader("کد تأیید", "کد ۶ رقمی‌ای که به ${vm.resetEmail} فرستادیم رو وارد کن")
        Box(Modifier.fillMaxWidth()) {
            BasicTextField(
                value = code,
                onValueChange = { v -> code = v.filter { it.isDigit() }.take(6); if (code.length == 6) vm.verifyReset(code) },
                modifier = Modifier.fillMaxWidth().height(64.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                textStyle = TextStyle(color = Color.Transparent, fontSize = 1.sp),
                cursorBrush = SolidColor(Color.Transparent),
                decorationBox = {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(6) { i ->
                            Box(
                                Modifier.weight(1f).height(60.dp).clip(RoundedCornerShape(14.dp)).background(C.SurfaceAlt)
                                    .border(2.dp, if (i == code.length) C.Pink else Color.Transparent, RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center,
                            ) { Txt(code.getOrNull(i)?.let { Fa.digits(it.toString()) } ?: "", 24.sp, FontWeight.Bold) }
                        }
                    }
                },
            )
        }
        Spacer(Modifier.height(24.dp))
        BigButton("تأیید", { vm.verifyReset(code) }, busy = vm.busy, enabled = code.length == 6)
        Txt("کد نرسید؟ ارسال دوباره", 14.sp, FontWeight.Medium, C.Pink, Modifier.align(Alignment.CenterHorizontally).pressable { vm.requestReset(vm.resetEmail) }.padding(20.dp))
    }
}

@Composable
private fun NewPassForm(vm: AppVM) {
    var p1 by rememberSaveable { mutableStateOf("") }
    var p2 by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
        AuthHeader("رمز جدید", "یه رمز قوی انتخاب کن")
        Field(p1, { p1 = it }, "رمز جدید (حداقل ۸ کاراکتر)", password = true)
        Spacer(Modifier.height(14.dp))
        Field(p2, { p2 = it }, "تکرار رمز", password = true, imeAction = ImeAction.Done, onDone = { vm.setNewPassword(p1, p2) })
        Spacer(Modifier.height(24.dp))
        BigButton("ذخیره", { vm.setNewPassword(p1, p2) }, busy = vm.busy)
    }
}

@Composable
private fun UpdatedForm(vm: AppVM) {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(110.dp).clip(CircleShape).background(C.PinkSoft), contentAlignment = Alignment.Center) {
            Box(Modifier.size(70.dp).clip(CircleShape).background(C.Pink), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(42.dp)) }
        }
        Txt("رمز عوض شد!", 26.sp, FontWeight.Bold, modifier = Modifier.padding(top = 22.dp))
        Txt("حالا می‌تونی وارد برنامه بشی.", 15.sp, color = C.Muted, modifier = Modifier.padding(top = 6.dp))
        Spacer(Modifier.height(32.dp))
        BigButton("ادامه", { vm.finishReset() }, busy = vm.busy)
    }
}

// ============================================================================ account setup
@Composable
private fun StepScaffold(
    step: Int, total: Int, title: String, onBack: (() -> Unit)?, nextText: String = "ادامه",
    nextEnabled: Boolean = true, onNext: () -> Unit, content: @Composable () -> Unit,
) {
    Column(Modifier.fillMaxSize().background(C.Surface).statusBarsPadding().navigationBarsPadding().imePadding()) {
        Row(Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).then(if (onBack != null) Modifier.pressable(role = null) { onBack() } else Modifier), contentAlignment = Alignment.Center) {
                if (onBack != null) Icon(androidx.compose.material.icons.Icons.AutoMirrored.Rounded.ArrowBack, "بازگشت", tint = C.Text)
            }
            val p by animateFloatAsState(step / total.toFloat(), tween(300), label = "p")
            Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(C.Track)) { Box(Modifier.fillMaxWidth(p).fillMaxSize().clip(RoundedCornerShape(4.dp)).background(C.Pink)) }
            Txt("${Fa.n(step)} / ${Fa.n(total)}", 14.sp, FontWeight.Medium, modifier = Modifier.padding(horizontal = 12.dp))
        }
        Txt(title, 26.sp, FontWeight.Bold, modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 18.dp), align = TextAlign.Center, lineHeight = 36.sp)
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { content() }
        Box(Modifier.fillMaxWidth().padding(16.dp)) { BigButton(nextText, onNext, enabled = nextEnabled) }
    }
}

@Composable
fun NameScreen(vm: AppVM) {
    var name by rememberSaveable { mutableStateOf("") }
    StepScaffold(1, 7, "اسمت چیه؟", null, nextEnabled = name.isNotBlank(), onNext = { vm.saveName(name) }) {
        Box(Modifier.padding(horizontal = 24.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(C.SurfaceAlt).padding(vertical = 22.dp), contentAlignment = Alignment.Center) {
            BasicTextField(
                value = name, onValueChange = { name = it.take(30) }, singleLine = true,
                textStyle = TextStyle(fontFamily = Vazir, fontSize = 30.sp, color = C.Text, textAlign = TextAlign.Center, textDirection = TextDirection.Content),
                cursorBrush = SolidColor(C.Pink),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { if (name.isNotBlank()) vm.saveName(name) }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
fun RoleScreen(vm: AppVM) {
    var role by rememberSaveable { mutableStateOf<String?>(null) }
    val name = vm.snap?.me?.name.orEmpty()
    Column(Modifier.fillMaxSize().background(C.Surface).statusBarsPadding().navigationBarsPadding().padding(24.dp)) {
        Spacer(Modifier.height(40.dp))
        Txt("سلام $name", 32.sp, FontWeight.Bold)
        Txt("تو برنامه چه نقشی داری؟", 16.sp, color = C.Muted, modifier = Modifier.padding(top = 6.dp))
        Spacer(Modifier.height(28.dp))
        RoleCard("her", role, "پریودم رو ثبت می‌کنم", "چرخه‌ی خودم رو دنبال می‌کنم و اگه بخوام به پارتنرم خبر می‌دم.", Icons.Rounded.WaterDrop) { role = "her" }
        Spacer(Modifier.height(14.dp))
        RoleCard("partner", role, "پارتنرم و می‌خوام خبر داشته باشم", "با کد پارتنرم وصل می‌شم و قبل از PMS و پریود نوتیف می‌گیرم.", Icons.Rounded.Favorite) { role = "partner" }
        Spacer(Modifier.weight(1f))
        BigButton("ادامه", { role?.let { vm.saveRole(it) } }, enabled = role != null, busy = vm.busy)
    }
}

@Composable
private fun RoleCard(id: String, selected: String?, title: String, desc: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    val sel = selected == id
    val border by animateColorAsState(if (sel) C.Pink else C.Line, tween(250), label = "rb")
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(if (sel) C.PinkSoft else C.SurfaceAlt)
            .border(if (sel) 2.dp else 1.dp, border, RoundedCornerShape(20.dp)).pressable(onClick = onClick).padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(C.Pink), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Color.White) }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Txt(title, 17.sp, FontWeight.Bold)
            Txt(desc, 13.sp, color = C.Muted, modifier = Modifier.padding(top = 2.dp))
        }
        if (sel) Box(Modifier.size(26.dp).clip(CircleShape).background(C.Pink), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(16.dp)) }
    }
}

/** Steps 2..7 of 7 (name was step 1), then a short "preparing" screen. */
@Composable
fun SetupScreen(vm: AppVM) {
    var step by rememberSaveable { mutableIntStateOf(2) }
    val today = rememberToday()
    var birth by remember { mutableStateOf(today.minusYears(25)) }
    var weight by remember { mutableStateOf(60.0) }
    var height by remember { mutableStateOf(165.0) }
    var periodLen by rememberSaveable { mutableIntStateOf(5) }
    var cycleLen by rememberSaveable { mutableIntStateOf(28) }
    var last by remember { mutableStateOf(today.minusDays(14)) }
    val back: (() -> Unit)? = if (step > 2) ({ step-- }) else null

    when (step) {
        2 -> StepScaffold(2, 7, "تاریخ تولدت کیه؟", back, onNext = { step++ }) {
            val nowY = Fa.jalaliParts(today).first
            Box(Modifier.padding(horizontal = 24.dp)) { JalaliDatePicker(birth, nowY - 80, nowY - 8, { birth = it }) }
        }
        3 -> StepScaffold(3, 7, "وزنت چقدره؟", back, onNext = { step++ }) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Txt(Fa.digits(fmt1(weight)), 64.sp, FontWeight.Bold, C.Pink, lineHeight = 72.sp)
                    Txt("کیلوگرم", 18.sp, color = C.Muted, modifier = Modifier.padding(start = 8.dp, bottom = 14.dp))
                }
                Spacer(Modifier.height(30.dp))
                RulerPicker(weight, 30, 200, { weight = it }, scale = 10)
            }
        }
        4 -> StepScaffold(4, 7, "قدت چقدره؟", back, onNext = { step++ }) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Txt(Fa.digits(fmt1(height)), 64.sp, FontWeight.Bold, C.Pink, lineHeight = 72.sp)
                    Txt("سانتی‌متر", 18.sp, color = C.Muted, modifier = Modifier.padding(start = 8.dp, bottom = 14.dp))
                }
                Spacer(Modifier.height(30.dp))
                RulerPicker(height, 120, 220, { height = Math.round(it).toDouble() }, scale = 1)
            }
        }
        5 -> StepScaffold(5, 7, "معمولاً پریودت چند روز طول می‌کشه؟", back, onNext = { step++ }) {
            Box(Modifier.padding(horizontal = 80.dp)) { WheelColumn((2..10).map { "${Fa.n(it)} روز" }, periodLen - 2, { periodLen = it + 2 }) }
        }
        6 -> StepScaffold(6, 7, "معمولاً چرخه‌ات چند روزه‌ست؟", back, onNext = { step++ }) {
            Box(Modifier.padding(horizontal = 80.dp)) { WheelColumn((20..45).map { "${Fa.n(it)} روز" }, cycleLen - 20, { cycleLen = it + 20 }) }
        }
        7 -> StepScaffold(7, 7, "آخرین پریودت کی شروع شد؟", back, nextText = "تمام", onNext = { step = 8 }) {
            val nowY = Fa.jalaliParts(today).first
            Box(Modifier.padding(horizontal = 24.dp)) { JalaliDatePicker(last, nowY - 1, nowY, { last = if (it.isAfter(today)) today else it }) }
        }
        else -> {
            LaunchedEffect(Unit) {
                delay(2400)
                vm.finishSetup(birth, weight, height, cycleLen, periodLen, last)
            }
            val t = rememberInfiniteTransition(label = "prep")
            val s by t.animateFloat(0.9f, 1.1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "s")
            Column(Modifier.fillMaxSize().background(C.Surface), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Box(Modifier.scale(s)) { Uterus(160.dp, C.PinkSoft) }
                Txt("در حال ساخت تقویم شخصی‌ت…", 20.sp, FontWeight.Bold, modifier = Modifier.padding(top = 28.dp))
                Txt("چند لحظه صبر کن", 14.sp, color = C.Muted, modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
}

// ============================================================================ update banner
@Composable
fun UpdateBanner(vm: AppVM) {
    val u = vm.update
    Box(Modifier.fillMaxWidth().background(C.PinkDeep).statusBarsPadding()) {
        if (u is UpdateState.Downloading) {
            val p by animateFloatAsState(u.progress, tween(200), label = "pg")
            Box(Modifier.fillMaxWidth(p).height(40.dp).background(Color.White.copy(alpha = 0.25f)).align(Alignment.CenterStart))
        }
        Row(Modifier.fillMaxWidth().height(40.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
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
                    Modifier.clip(RoundedCornerShape(14.dp)).background(Color.White).pressable { if (u is UpdateState.Ready) vm.install() else vm.startDownload() }.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (u !is UpdateState.Ready) { Icon(Icons.Rounded.Download, null, tint = C.PinkDeep, modifier = Modifier.size(16.dp)); Spacer(Modifier.size(4.dp)) }
                    Txt(action, 13.sp, FontWeight.Bold, C.PinkDeep)
                }
            }
        }
    }
}
