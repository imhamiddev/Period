package dev.imhamid.period

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import dev.imhamid.period.ui.AppVM
import dev.imhamid.period.ui.AuthScreen
import dev.imhamid.period.ui.BootScreen
import dev.imhamid.period.ui.C
import dev.imhamid.period.ui.HomeScreen
import dev.imhamid.period.ui.IntroScreen
import dev.imhamid.period.ui.NameScreen
import dev.imhamid.period.ui.PeriodTheme
import dev.imhamid.period.ui.Route
import dev.imhamid.period.ui.RoleScreen
import dev.imhamid.period.ui.SetupScreen
import dev.imhamid.period.ui.Txt
import dev.imhamid.period.ui.UpdateBanner
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private val vm: AppVM by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PeriodTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Root(vm)
                }
            }
        }
    }
}

@Composable
private fun Root(vm: AppVM) {
    val notifPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val route = vm.route
    LaunchedEffect(route) {
        if (route == Route.Home && Build.VERSION.SDK_INT >= 33) notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.checkUpdate() }
    LaunchedEffect(vm.error) {
        if (vm.error != null) { delay(3600); vm.error = null }
    }

    val bannerVisible = vm.update !is dev.imhamid.period.update.UpdateState.None

    Box(Modifier.fillMaxSize().background(C.Paper)) {
        Column(Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = bannerVisible,
                enter = expandVertically(tween(450)) + fadeIn(tween(450)),
                exit = shrinkVertically(tween(350)) + fadeOut(tween(250)),
            ) { UpdateBanner(vm) }

            Box(Modifier.weight(1f).then(if (bannerVisible) Modifier.consumeWindowInsets(WindowInsets.statusBars) else Modifier)) {
                AnimatedContent(
                    targetState = route,
                    transitionSpec = {
                        val d = if (targetState.ordinal >= initialState.ordinal) -1 else 1
                        val ease = androidx.compose.animation.core.FastOutSlowInEasing
                        (fadeIn(tween(420, delayMillis = 90)) + scaleIn(tween(520, easing = ease), initialScale = 0.96f) +
                            slideInHorizontally(tween(520, easing = ease)) { d * it / 5 }) togetherWith
                            (fadeOut(tween(220)) + slideOutHorizontally(tween(420, easing = ease)) { -d * it / 8 })
                    },
                    label = "route",
                ) { r ->
                    when (r) {
                        Route.Boot -> BootScreen()
                        Route.Intro -> IntroScreen(vm)
                        Route.Auth -> AuthScreen(vm)
                        Route.Name -> NameScreen(vm)
                        Route.Role -> RoleScreen(vm)
                        Route.Setup -> SetupScreen(vm)
                        Route.Home -> HomeScreen(vm)
                    }
                }
            }
        }

        // error toast
        AnimatedVisibility(
            visible = vm.error != null,
            modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = if (bannerVisible) 44.dp else 8.dp),
            enter = slideInVertically(tween(350)) { -it } + fadeIn(tween(350)),
            exit = slideOutVertically(tween(300)) { -it } + fadeOut(tween(250)),
        ) {
            Box(
                Modifier.padding(horizontal = 20.dp).clip(RoundedCornerShape(20.dp)).background(C.Ink).padding(horizontal = 18.dp, vertical = 12.dp),
            ) { Txt(vm.error ?: "", 14.sp, color = androidx.compose.ui.graphics.Color.White) }
        }
    }
}
