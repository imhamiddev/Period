package dev.imhamid.period.ui

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.QuestionAnswer
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.imhamid.period.engine.Overview
import dev.imhamid.period.engine.Phase
import dev.imhamid.period.engine.toEngine
import java.time.DayOfWeek
import java.time.LocalDate

/** 0 = first day of the week chosen in Preferences (Saturday by default). */
fun weekIndex(d: LocalDate): Int = (d.dayOfWeek.value - WeekCfg.start + 7) % 7

@Composable
private fun LogoMark() {
    Box(Modifier.size(40.dp).clip(CircleShape).background(C.PinkSoft), contentAlignment = Alignment.Center) {
        Icon(Icons.Rounded.WaterDrop, null, tint = C.Pink, modifier = Modifier.size(22.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTab(vm: AppVM) {
    val s = vm.snap ?: return
    val isHer = s.me.role == "her"
    val today = rememberToday()
    val owner = s.owner
    val engine = remember(owner, s.logs, today) { owner?.toEngine(today, s.logs) }
    val ov = remember(engine) { engine?.overview() }
    val name = owner?.name?.takeIf { it.isNotBlank() } ?: "پارتنرت"
    val unread = s.inbox.filter { it.toId == s.me.id && it.readAt == null }
    var menu by remember { mutableStateOf(false) }

    PullToRefreshBox(isRefreshing = vm.refreshing, onRefresh = { vm.pullRefresh() }, modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().background(C.Paper).statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(Modifier.fillMaxWidth().height(60.dp)) {
                Box(Modifier.align(Alignment.CenterStart)) { LogoMark() }
                Txt("لونا", 22.sp, FontWeight.Bold, modifier = Modifier.align(Alignment.Center))
                Box(Modifier.align(Alignment.CenterEnd)) {
                    Box(Modifier.size(44.dp).pressable(role = null) { menu = true }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.MoreVert, "منو", tint = C.Text)
                    }
                    DropdownMenu(menu, { menu = false }) {
                        DropdownMenuItem(text = { Txt("پارتنر و پیام‌ها", 15.sp) }, onClick = { menu = false; vm.push(Screen.Partner) })
                        DropdownMenuItem(text = { Txt("همگام‌سازی", 15.sp) }, onClick = { menu = false; vm.refresh() })
                    }
                }
            }

            if (!isHer && s.activePair == null) ConnectCard(vm, s.pairs)

            // ---- dial
            Panel(padding = 20.dp) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (!isHer && owner != null) Txt("چرخه‌ی $name", 14.sp, color = C.Muted, modifier = Modifier.padding(bottom = 8.dp))
                    LunarRing(engine, ov, size = 292.dp) { RingCenter(ov, isHer) }
                    Spacer(Modifier.height(18.dp))
                    if (isHer) BigButton("ویرایش پریود", { vm.push(Screen.EditPeriod) }, Modifier.width(190.dp).height(46.dp))
                    else BigButton("ارسال پیام", { vm.push(Screen.Partner) }, Modifier.width(190.dp).height(46.dp))
                }
            }

            // ---- late period
            if (ov != null && ov.lateDays > 0) {
                Panel { LateCard(vm, ov.lateDays, isHer, name, today) }
            }

            // ---- unread messages
            if (unread.isNotEmpty()) {
                Panel(Modifier.pressable { vm.push(Screen.Partner) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).clip(CircleShape).background(C.Pink))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Txt("${Fa.n(unread.size)} پیام جدید", 16.sp, FontWeight.Bold)
                            Txt(unread.first().body, 14.sp, color = C.Muted, maxLines = 1)
                        }
                    }
                }
            }

            // ---- soft health hint (only for her)
            if (isHer) {
                val dismissed = vm.pref("insight_off", "").split("|")
                val flag = remember(s.logs, today) { Insights.flags(s, today) }.firstOrNull { it.id !in dismissed }
                if (flag != null) {
                    Panel {
                        Row(verticalAlignment = Alignment.Top) {
                            Box(Modifier.size(40.dp).clip(CircleShape).background(C.AmberSoft), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Info, null, tint = C.Orange) }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Txt(flag.title, 16.sp, FontWeight.Bold)
                                Txt(flag.text, 14.sp, color = C.Muted, lineHeight = 22.sp, modifier = Modifier.padding(top = 4.dp))
                                Txt("باشه، دیدم", 14.sp, FontWeight.Bold, C.Pink, Modifier.padding(top = 10.dp).pressable { vm.setPref("insight_off", (dismissed + flag.id).filter { it.isNotBlank() }.takeLast(30).joinToString("|")) })
                            }
                        }
                    }
                }
                val bbt = remember(s.health, today) { Insights.bbtOvulation(s, today) }
                if (bbt != null) {
                    Panel {
                        Row(verticalAlignment = Alignment.Top) {
                            Box(Modifier.size(40.dp).clip(CircleShape).background(C.PinkSoft), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Thermostat, null, tint = C.Pink) }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Txt("تأیید تخمک‌گذاری با دما", 16.sp, FontWeight.Bold)
                                Txt("طبق دمای صبحگاهی‌ت، تخمک‌گذاری حدود ${Fa.date(bbt, "d MMMM")} اتفاق افتاده.", 14.sp, color = C.Muted, lineHeight = 22.sp, modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                    }
                }
            }

            // ---- partner: one-tap reactions
            if (!isHer && s.activePair != null && owner != null) {
                val todayLog = s.days[today]
                val moodText = todayLog?.moods.orEmpty().mapNotNull { Catalog.mood(it)?.label }.joinToString("، ")
                    .ifBlank { todayLog?.mood?.let { Fa.moodNames[it - 1] }.orEmpty() }
                var sentLabel by remember { mutableStateOf<String?>(null) }
                androidx.compose.runtime.LaunchedEffect(sentLabel) { if (sentLabel != null) { kotlinx.coroutines.delay(2500); sentLabel = null } }
                Panel {
                    Txt("واکنش سریع", 14.sp, color = C.Muted)
                    Txt(if (moodText.isNotBlank()) "حال امروز $name: $moodText" else "با یه لمس به $name بگو کنارشی", 15.sp, FontWeight.Medium, modifier = Modifier.padding(top = 4.dp, bottom = 14.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        listOf(
                            Triple(Icons.Rounded.Favorite, "دوستت دارم", "دوستت دارم"),
                            Triple(Icons.Rounded.Check, "فهمیدم", "فهمیدم، کنارتم"),
                            Triple(Icons.Rounded.DirectionsRun, "دارم میام", "دارم میام پیشت"),
                            Triple(Icons.Rounded.QuestionAnswer, "چیزی لازمه؟", "چیزی لازم داری؟"),
                        ).forEach { (ic, label, msg) ->
                            Column(Modifier.width(76.dp).pressable { vm.sendMessage(msg); sentLabel = label }, horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(Modifier.size(54.dp).clip(CircleShape).background(if (sentLabel == label) C.Pink else C.PinkSoft), contentAlignment = Alignment.Center) {
                                    Icon(if (sentLabel == label) Icons.Rounded.Check else ic, null, tint = if (sentLabel == label) Color.White else C.Pink)
                                }
                                Txt(label, 12.sp, color = C.Muted, align = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
                            }
                        }
                    }
                }
            }

            // ---- phases
            Panel {
                SectionHeader("فازهای چرخه قاعدگی", onMore = { vm.push(Screen.PhaseInfo(Content.phaseIndex(ov?.phase))) })
                Spacer(Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    itemsIndexed(Content.phases) { i, p ->
                        Column(
                            Modifier.width(150.dp).clip(RoundedCornerShape(12.dp)).background(C.SurfaceAlt)
                                .pressable { vm.push(Screen.PhaseInfo(i)) }.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(24.dp).clip(RoundedCornerShape(5.dp)).background(p.color), contentAlignment = Alignment.Center) {
                                    Txt(Fa.n(p.index), 14.sp, FontWeight.Bold, Color.White, lineHeight = 16.sp)
                                }
                                Spacer(Modifier.width(8.dp))
                                Txt(p.short, 15.sp, FontWeight.Bold, maxLines = 1)
                            }
                            Spacer(Modifier.height(10.dp))
                            Uterus(96.dp, p.soft)
                        }
                    }
                }
            }

            // ---- today's tip
            val tip = if (isHer) Fa.herTip(ov?.phase) else Fa.partnerTip(ov?.phase, name, Fa.isPartnerLabel(s.activePair?.label))
            Panel {
                Txt("نکته‌ی امروز", 14.sp, color = C.Muted)
                Spacer(Modifier.height(6.dp))
                Txt(tip, 15.sp, lineHeight = 24.sp)
            }

            // ---- quick log
            if (isHer) {
                val logged = engine?.isLogged(today) == true
                Panel {
                    Txt("ثبت سریع", 14.sp, color = C.Muted)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        BigButton(
                            if (logged) "امروز پریودم" else if (ov?.periodDay != null) "امروز هم خونریزی دارم" else "پریودم امروز شروع شد",
                            { vm.toggleDay(today) }, Modifier.weight(1.3f).height(50.dp),
                            color = if (logged) C.Green else C.Pink,
                        )
                        BigButton("افزودن لاگ", { vm.openLog(today) }, Modifier.weight(1f).height(50.dp), color = C.Follicular)
                    }
                }
            }

            // ---- upcoming
            if (ov != null && ov.hasData) UpcomingPanel(ov)

            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun RingCenter(ov: Overview?, isHer: Boolean) {
    if (ov == null || !ov.hasData) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Rounded.WaterDrop, null, tint = C.Pink, modifier = Modifier.size(30.dp))
            Txt("هنوز داده‌ای نیست", 18.sp, FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
            Txt(if (isHer) "اولین پریودت رو ثبت کن" else "منتظر اطلاعات چرخه", 13.sp, color = C.Muted)
        }
        return
    }
    val inPeriod = ov.periodDay != null
    val day = ov.periodDay ?: ov.cycleDay ?: 1
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Rounded.WaterDrop, null, tint = phaseColor(ov.phase), modifier = Modifier.size(26.dp))
        Txt(Fa.phaseName(ov.phase), 16.sp, color = C.Muted)
        Row(verticalAlignment = Alignment.Bottom) {
            Txt(Fa.n(day), 76.sp, FontWeight.Bold, lineHeight = 84.sp)
            Txt(if (inPeriod) "روز پریود" else "روز چرخه", 14.sp, color = C.Muted, modifier = Modifier.padding(start = 6.dp, bottom = 16.dp))
        }
        val line = when {
            ov.lateDays > 0 -> "${Fa.n(ov.lateDays)} روز تأخیر"
            (ov.daysToOvulation ?: -1) >= 0 && ov.phase != Phase.OVULATION -> "تخمک‌گذاری · ${Fa.daysText(ov.daysToOvulation ?: 0)}"
            ov.daysToPeriod != null -> "پریود بعدی · ${Fa.daysText(ov.daysToPeriod)}"
            else -> Fa.phaseHint(ov.phase)
        }
        Txt(line, 14.sp, color = C.Muted, align = TextAlign.Center)
    }
}

@Composable
private fun UpcomingPanel(ov: Overview) {
    Panel {
        Txt("پیش رو", 14.sp, color = C.Muted)
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ov.pmsStart?.let { d -> EventRow(C.Luteal, "شروع PMS", if (ov.inPms) "همین الان" else Fa.daysText(ov.daysToPms ?: 0), Fa.date(d)) }
            ov.nextPeriod?.let { d -> EventRow(C.Period, "پریود بعدی", Fa.daysText(ov.daysToPeriod ?: 0), Fa.range(d, ov.variability)) }
            ov.ovulation?.let { d -> EventRow(C.Ovulation, "تخمک‌گذاری", Fa.daysText(ov.daysToOvulation ?: 0), Fa.date(d)) }
        }
    }
}

@Composable
private fun EventRow(color: Color, title: String, whenText: String, date: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(12.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Txt(title, 16.sp, FontWeight.Medium)
            Txt(date, 13.sp, color = C.Muted)
        }
        Txt(whenText, 14.sp, FontWeight.Medium, C.Pink)
    }
}

// ============================================================================ phase detail
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhaseScreen(vm: AppVM, start: Int) {
    val s = vm.snap
    val today = rememberToday()
    val current = remember(s) { s?.owner?.toEngine(today, s.logs)?.overview()?.phase?.let { Content.phaseIndex(it) } }
    val pager = rememberPagerState(initialPage = start.coerceIn(0, 3)) { 4 }
    Page {
        TopBar("", onBack = { vm.pop() }, close = true)
        HorizontalPager(pager, Modifier.weight(1f)) { i ->
            val p = Content.phases[i]
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Uterus(190.dp, p.soft)
                Txt(p.title, 22.sp, FontWeight.Bold, modifier = Modifier.padding(top = 14.dp))
                Txt(if (current == i) "فاز فعلی" else "فاز ${Fa.n(p.index)} از ۴", 13.sp, color = C.Muted)
                Spacer(Modifier.height(18.dp))
                InfoBlock("چه اتفاقی می‌افته؟", p.what)
                Spacer(Modifier.height(10.dp))
                InfoBlock("علائم", p.symptoms)
                Spacer(Modifier.height(10.dp))
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(C.Surface).padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Txt("احتمال بارداری:", 16.sp, FontWeight.Bold)
                        Spacer(Modifier.width(8.dp))
                        ChanceChip(p.chance)
                    }
                    Txt(p.chanceText, 15.sp, modifier = Modifier.padding(top = 6.dp), lineHeight = 24.sp)
                }
                Spacer(Modifier.height(10.dp))
                InfoBlock("پیشنهاد", p.tip)
                Spacer(Modifier.height(24.dp))
            }
        }
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(vertical = 14.dp), horizontalArrangement = Arrangement.Center) {
            repeat(4) { i ->
                Box(Modifier.padding(horizontal = 4.dp).size(if (pager.currentPage == i) 22.dp else 8.dp, 8.dp).clip(RoundedCornerShape(4.dp)).background(if (pager.currentPage == i) Content.phases[i].color else C.Track))
            }
        }
    }
}

@Composable
private fun InfoBlock(title: String, body: String) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(C.Surface).padding(16.dp)) {
        Txt(title, 16.sp, FontWeight.Bold)
        Txt(body, 15.sp, modifier = Modifier.padding(top = 6.dp), lineHeight = 24.sp)
    }
}

@Composable
fun ChanceChip(text: String) {
    val c = when (text) { "زیاد" -> C.Red; "متوسط" -> C.Orange; else -> Color(0xFF607D8B) }
    Box(Modifier.clip(RoundedCornerShape(6.dp)).background(c).padding(horizontal = 10.dp, vertical = 2.dp)) {
        Txt(text, 13.sp, FontWeight.Medium, Color.White)
    }
}

// ============================================================================ edit period
@Composable
fun EditPeriodScreen(vm: AppVM) {
    val s = vm.snap ?: return
    val today = rememberToday()
    var selected by remember(s.logs.size) { mutableStateOf(s.logs) }
    val months = remember(today) {
        val first = Fa.jalaliMonth(today).first
        val list = mutableListOf<Pair<LocalDate, Int>>()
        var f = first
        repeat(4) { f = Fa.jalaliMonth(f.minusDays(1)).first }
        repeat(7) { val m = Fa.jalaliMonth(f); list += m; f = m.first.plusDays(m.second.toLong()) }
        list
    }
    Page {
        TopBar("ویرایش پریود", onBack = { vm.pop() }, close = true)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            months.forEach { (first, len) ->
                Panel {
                    Txt(Fa.date(first, "MMMM yyyy"), 17.sp, FontWeight.Bold, modifier = Modifier.fillMaxWidth(), align = TextAlign.Center)
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth()) { (0..6).forEach { Txt(weekdayLetter(it), 13.sp, color = C.Muted, modifier = Modifier.weight(1f), align = TextAlign.Center) } }
                    Spacer(Modifier.height(6.dp))
                    val lead = weekIndex(first)
                    val cells = lead + len
                    val rows = (cells + 6) / 7
                    repeat(rows) { r ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            repeat(7) { c ->
                                val n = r * 7 + c - lead + 1
                                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                    if (n in 1..len) {
                                        val d = first.plusDays(n - 1L)
                                        val future = d.isAfter(today)
                                        val on = d in selected
                                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.pressable(enabled = !future) {
                                            selected = if (on) selected - d else selected + d
                                        }) {
                                            Txt(Fa.jalaliDay(d), 15.sp, FontWeight.Medium, if (on) C.Pink else if (future) C.Muted.copy(alpha = 0.5f) else C.Text)
                                            Box(
                                                Modifier.padding(top = 2.dp).size(26.dp).clip(CircleShape)
                                                    .background(if (on) C.Pink else Color.Transparent)
                                                    .border(1.5.dp, if (on) C.Pink else C.Track, CircleShape),
                                                contentAlignment = Alignment.Center,
                                            ) { if (on) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(16.dp)) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        Box(Modifier.fillMaxWidth().background(C.Surface).navigationBarsPadding().padding(16.dp)) {
            BigButton("ذخیره", { vm.applyPeriodDays(selected); vm.pop() })
        }
    }
}
