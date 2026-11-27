package dev.imhamid.period.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Subscription screens follow the design, but nothing is charged: there is no payment gateway behind them.
 * The chosen plan and the masked card (brand + last 4 digits only) are kept on this phone.
 */
private const val DEMO_NOTE = "نسخه‌ی نمایشی: پرداخت واقعی انجام نمی‌شه و هزینه‌ای کسر نمی‌شه."

private fun savedCards(vm: AppVM): List<String> = vm.pref("cards", "").split("|").filter { it.isNotBlank() }

@Composable
fun UpgradeScreen(vm: AppVM) {
    var plan by remember { mutableStateOf(vm.pref("plan_sel", "monthly")) }
    Page {
        TopBar("ارتقای پلن", onBack = { vm.pop() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(90.dp).clip(CircleShape).background(C.PinkSoft), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.WorkspacePremium, null, tint = C.Orange, modifier = Modifier.size(52.dp))
            }
            Txt("لونا ویژه", 24.sp, FontWeight.Bold)
            Panel { Content.premiumPerks.forEach { p ->
                Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(22.dp).clip(CircleShape).background(C.Pink), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(14.dp)) }
                    Txt(p, 15.sp, modifier = Modifier.padding(start = 12.dp))
                }
            } }
            Content.plans.forEach { p ->
                val sel = plan == p.id
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(C.Surface).border(2.dp, if (sel) C.Pink else Color.Transparent, RoundedCornerShape(16.dp))
                        .pressable { plan = p.id }.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Txt(p.title, 17.sp, FontWeight.Bold)
                            if (p.badge != null) Box(Modifier.padding(start = 8.dp).clip(RoundedCornerShape(6.dp)).background(C.Pink).padding(horizontal = 8.dp, vertical = 1.dp)) { Txt(p.badge, 11.sp, FontWeight.Bold, Color.White) }
                        }
                        Txt("${p.price} ${p.per}", 14.sp, color = C.Muted)
                    }
                    Box(Modifier.size(24.dp).clip(CircleShape).border(2.dp, if (sel) C.Pink else C.Track, CircleShape), contentAlignment = Alignment.Center) {
                        if (sel) Box(Modifier.size(12.dp).clip(CircleShape).background(C.Pink))
                    }
                }
            }
            Txt(DEMO_NOTE, 12.sp, color = C.Muted, align = TextAlign.Center)
        }
        Box(Modifier.fillMaxWidth().background(C.Surface).navigationBarsPadding().padding(16.dp)) {
            BigButton("ادامه", { vm.setPref("plan_sel", plan); vm.push(Screen.ChoosePay) })
        }
    }
}

@Composable
fun ChoosePayScreen(vm: AppVM) {
    val cards = savedCards(vm)
    var sel by remember { mutableStateOf(vm.pref("pay_sel", cards.firstOrNull() ?: "")) }
    Page {
        TopBar("انتخاب روش پرداخت", onBack = { vm.pop() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (cards.isEmpty()) Txt("هنوز کارتی اضافه نکردی.", 15.sp, color = C.Muted)
            cards.forEach { c ->
                CardRow(c, sel == c) { sel = c }
            }
            OutlinePill("افزودن کارت جدید", { vm.push(Screen.AddPayment) }, Modifier.fillMaxWidth())
        }
        Box(Modifier.fillMaxWidth().background(C.Surface).navigationBarsPadding().padding(16.dp)) {
            BigButton("ادامه", { vm.setPref("pay_sel", sel); vm.push(Screen.Review) }, enabled = sel.isNotBlank())
        }
    }
}

@Composable
private fun CardRow(c: String, selected: Boolean?, onClick: (() -> Unit)?) {
    val (brand, last4) = c.split(":").let { it.getOrElse(0) { "کارت" } to it.getOrElse(1) { "0000" } }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(C.Surface)
            .then(if (selected == true) Modifier.border(2.dp, C.Pink, RoundedCornerShape(14.dp)) else Modifier)
            .then(if (onClick != null) Modifier.pressable { onClick() } else Modifier).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.CreditCard, null, tint = C.Pink, modifier = Modifier.size(30.dp))
        Column(Modifier.padding(start = 14.dp).weight(1f)) {
            Txt(brand, 16.sp, FontWeight.Bold)
            Txt("•••• •••• •••• ${Fa.digits(last4)}", 14.sp, color = C.Muted)
        }
        if (selected != null) Box(Modifier.size(24.dp).clip(CircleShape).border(2.dp, if (selected) C.Pink else C.Track, CircleShape), contentAlignment = Alignment.Center) {
            if (selected) Box(Modifier.size(12.dp).clip(CircleShape).background(C.Pink))
        }
    }
}

@Composable
fun ReviewScreen(vm: AppVM) {
    val plan = Content.plans.firstOrNull { it.id == vm.pref("plan_sel", "monthly") } ?: Content.plans.first()
    val card = vm.pref("pay_sel", "").split(":").getOrNull(1).orEmpty()
    Page {
        TopBar("مرور سفارش", onBack = { vm.pop() })
        Column(Modifier.weight(1f).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Panel {
                ReviewLine("پلن", "لونا ویژه · ${plan.title}")
                RowDivider()
                ReviewLine("مبلغ", "${plan.price} ${plan.per}")
                RowDivider()
                ReviewLine("روش پرداخت", if (card.isBlank()) "—" else "•••• ${Fa.digits(card)}")
            }
            Txt(DEMO_NOTE, 12.sp, color = C.Muted, align = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
        Box(Modifier.fillMaxWidth().background(C.Surface).navigationBarsPadding().padding(16.dp)) {
            BigButton("تأیید و پرداخت", { vm.push(Screen.Processing) })
        }
    }
}

@Composable
private fun ReviewLine(k: String, v: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Txt(k, 15.sp, color = C.Muted, modifier = Modifier.weight(1f))
        Txt(v, 15.sp, FontWeight.Bold)
    }
}

@Composable
fun ProcessingScreen(vm: AppVM) {
    val rot by rememberInfiniteTransition(label = "spin").animateFloat(0f, 360f, infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart), label = "r")
    LaunchedEffect(Unit) {
        delay(2200)
        vm.setPref("premium", "true")
        vm.setPref("plan", vm.pref("plan_sel", "monthly"))
        vm.setPref("plan_date", java.time.LocalDate.now().toString())
        vm.stack.removeAll { it is Screen.Review || it is Screen.ChoosePay || it is Screen.Upgrade || it is Screen.Processing }
        vm.push(Screen.PaySuccess)
    }
    Page {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Box(Modifier.size(90.dp).rotate(rot).border(8.dp, C.Pink, CircleShape).padding(8.dp))
            Txt("در حال پردازش پرداخت…", 18.sp, FontWeight.Bold, modifier = Modifier.padding(top = 24.dp))
            Txt("چند لحظه صبر کن", 14.sp, color = C.Muted)
        }
    }
}

@Composable
fun PaySuccessScreen(vm: AppVM) {
    Page {
        Column(Modifier.weight(1f).fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Box(Modifier.size(110.dp).clip(CircleShape).background(C.PinkSoft), contentAlignment = Alignment.Center) {
                Box(Modifier.size(70.dp).clip(CircleShape).background(C.Pink), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(42.dp)) }
            }
            Txt("اشتراک فعال شد!", 24.sp, FontWeight.Bold, modifier = Modifier.padding(top = 24.dp))
            Txt("از لونا ویژه لذت ببر. (نسخه‌ی نمایشی)", 15.sp, color = C.Muted, align = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
        }
        Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp)) { BigButton("بازگشت به حساب", { vm.popToRoot() }) }
    }
}

@Composable
fun BillingScreen(vm: AppVM) {
    val premium = vm.prefBool("premium", false)
    val plan = Content.plans.firstOrNull { it.id == vm.pref("plan", "") }
    var confirm by remember { mutableStateOf(false) }
    if (confirm) ConfirmDialog("لغو اشتراک؟", "پلن ویژه غیرفعال می‌شه.", "لغو اشتراک", { confirm = false; vm.setPref("premium", "false") }, { confirm = false })
    Page {
        TopBar("صورت‌حساب و اشتراک", onBack = { vm.pop() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Panel {
                Txt(if (premium) "پلن فعلی: لونا ویژه (${plan?.title ?: ""})" else "پلن فعلی: رایگان", 17.sp, FontWeight.Bold)
                if (premium) Txt("از ${vm.pref("plan_date", "").takeIf { it.isNotBlank() }?.let { Fa.date(java.time.LocalDate.parse(it), "d MMMM yyyy") } ?: "—"}", 13.sp, color = C.Muted)
                Spacer(Modifier.height(12.dp))
                if (premium) OutlinePill("لغو اشتراک", { confirm = true }, color = C.Red) else BigButton("ارتقا به پلن ویژه", { vm.push(Screen.Upgrade) })
            }
            Panel {
                Txt("تاریخچه‌ی پرداخت", 16.sp, FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Txt(if (premium) "یک پرداخت نمایشی ثبت شده." else "هنوز پرداختی ثبت نشده.", 14.sp, color = C.Muted)
            }
            Txt(DEMO_NOTE, 12.sp, color = C.Muted)
        }
    }
}

@Composable
fun PaymentMethodsScreen(vm: AppVM) {
    val cards = savedCards(vm)
    Page {
        TopBar("روش‌های پرداخت", onBack = { vm.pop() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (cards.isEmpty()) Txt("هنوز کارتی اضافه نکردی.", 15.sp, color = C.Muted)
            cards.forEach { c -> CardRow(c, null, null) }
            Txt("فقط نوع و ۴ رقم آخر کارت روی همین گوشی ذخیره می‌شه.", 12.sp, color = C.Muted)
        }
        Box(Modifier.fillMaxWidth().background(C.Surface).navigationBarsPadding().padding(16.dp)) { BigButton("افزودن کارت جدید", { vm.push(Screen.AddPayment) }) }
    }
}

@Composable
fun AddPaymentScreen(vm: AppVM) {
    var holder by remember { mutableStateOf("") }
    var number by remember { mutableStateOf("") }
    var exp by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }
    Page {
        TopBar("افزودن کارت", onBack = { vm.pop() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Field(holder, { holder = it.take(40) }, "نام صاحب کارت")
            Field(number, { number = it.filter { c -> c.isDigit() }.take(16) }, "شماره کارت", keyboardType = KeyboardType.Number, ltr = true)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Field(exp, { exp = it.filter { c -> c.isDigit() || c == '/' }.take(5) }, "تاریخ انقضا (MM/YY)", Modifier.weight(1f), keyboardType = KeyboardType.Number, ltr = true)
                Field(cvv, { cvv = it.filter { c -> c.isDigit() }.take(4) }, "CVV", Modifier.weight(1f), keyboardType = KeyboardType.Number, ltr = true)
            }
            Txt("شماره کامل و CVV ذخیره نمی‌شن؛ فقط ۴ رقم آخر نگه داشته می‌شه. $DEMO_NOTE", 12.sp, color = C.Muted)
        }
        Box(Modifier.fillMaxWidth().background(C.Surface).navigationBarsPadding().padding(16.dp)) {
            BigButton("ذخیره‌ی کارت", {
                if (holder.isBlank() || number.length < 16 || exp.length < 4 || cvv.length < 3) { vm.toast("اطلاعات کارت رو کامل وارد کن"); return@BigButton }
                val brand = when (number.first()) { '4' -> "Visa"; '5' -> "Mastercard"; '6' -> "کارت بانکی"; else -> "کارت" }
                val entry = "$brand:${number.takeLast(4)}"
                val cur = savedCards(vm)
                if (entry !in cur) vm.setPref("cards", (cur + entry).joinToString("|"))
                vm.toast("کارت اضافه شد")
                vm.pop()
            })
        }
    }
}
