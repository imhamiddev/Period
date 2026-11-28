package dev.imhamid.period.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private class TabDef(val label: String, val icon: ImageVector)

private val tabs = listOf(
    TabDef("خانه", Icons.Rounded.Home),
    TabDef("تقویم", Icons.Rounded.CalendarMonth),
    TabDef("ترکر", Icons.Rounded.Insights),
    TabDef("حساب", Icons.Rounded.Person),
)

@Composable
fun HomeScreen(vm: AppVM) {
    BackHandler(enabled = vm.stack.isNotEmpty() || vm.tab != 0) {
        if (!vm.pop()) vm.tab = 0
    }
    Box(Modifier.fillMaxSize().background(C.Paper)) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = vm.tab,
                    transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(140)) },
                    label = "tab",
                ) { t ->
                    when (t) {
                        0 -> HomeTab(vm)
                        1 -> CalendarTab(vm)
                        2 -> TrackerTab(vm)
                        else -> AccountTab(vm)
                    }
                }
            }
            BottomBar(vm)
        }

        // pages on top of the tabs
        val top = vm.stack.lastOrNull()
        AnimatedContent(
            targetState = top,
            transitionSpec = {
                val ease = FastOutSlowInEasing
                if (targetState != null && initialState == null || (targetState != null && vm.stack.size >= 2)) {
                    (slideInHorizontally(tween(320, easing = ease)) { -it / 3 } + fadeIn(tween(260))) togetherWith
                        (fadeOut(tween(160)))
                } else {
                    fadeIn(tween(120)) togetherWith (slideOutHorizontally(tween(260, easing = ease)) { -it / 3 } + fadeOut(tween(220)))
                }
            },
            label = "page",
        ) { sc ->
            if (sc != null) ScreenHost(vm, sc) else Box(Modifier.size(0.dp))
        }
    }
}

@Composable
private fun BottomBar(vm: AppVM) {
    Row(
        Modifier.fillMaxWidth().background(C.Surface).navigationBarsPadding().height(66.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEachIndexed { i, t ->
            val sel = vm.tab == i
            Column(
                Modifier.weight(1f).fillMaxSize().pressable(role = androidx.compose.ui.semantics.Role.Tab) { vm.tab = i },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            ) {
                Icon(t.icon, t.label, tint = if (sel) C.Pink else C.Muted, modifier = Modifier.size(26.dp))
                Txt(t.label, 11.sp, if (sel) FontWeight.Bold else FontWeight.Medium, if (sel) C.Pink else C.Muted, modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}

@Composable
private fun ScreenHost(vm: AppVM, sc: Screen) {
    when (sc) {
        is Screen.PhaseInfo -> PhaseScreen(vm, sc.index)
        Screen.EditPeriod -> EditPeriodScreen(vm)
        Screen.AddLog -> AddLogScreen(vm)
        Screen.LogWater -> WaterScreen(vm)
        Screen.LogWeight -> WeightScreen(vm)
        Screen.LogTemp -> TempScreen(vm)
        Screen.MyCycles -> MyCyclesScreen(vm)
        Screen.Partner -> PartnerScreen(vm)
        Screen.PersonalInfo -> PersonalInfoScreen(vm)
        Screen.Preferences -> PreferencesScreen(vm)
        Screen.CycleSettings -> CycleSettingsScreen(vm)
        Screen.Reminder -> ReminderScreen(vm)
        Screen.Notifications -> NotificationsScreen(vm)
        Screen.Security -> SecurityScreen(vm)
        Screen.ChangePassword -> ChangePasswordScreen(vm)
        Screen.Help -> HelpScreen(vm)
        Screen.Faq -> FaqScreen(vm)
        Screen.Privacy -> TextPageScreen(vm, "سیاست حریم خصوصی", Content.privacy)
        Screen.Terms -> TextPageScreen(vm, "شرایط استفاده", Content.terms)
    }
}
