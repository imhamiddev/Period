package dev.imhamid.period.ui

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.core.view.WindowCompat
import dev.imhamid.period.R

/** App palette. Surface colors follow [dark]; brand colors (Lime, Green, Coral, Amber) stay the same. */
object C {
    /** Backed by Compose state, so every reader recomposes when the theme flips. */
    var dark by mutableStateOf(false)

    val Lime = Color(0xFFBDEF6A)
    val Green = Color(0xFF3FA544)
    val GreenMid = Color(0xFF1F7A36)
    val Deep = Color(0xFF0B3D20)
    val Coral = Color(0xFFFF8A7A)
    val Amber = Color(0xFFF2B35E)

    val LimeSoft get() = if (dark) Color(0xFF26371C) else Color(0xFFE4F7BF)
    val CoralSoft get() = if (dark) Color(0xFF4A2A26) else Color(0xFFFFE1DC)
    val AmberSoft get() = if (dark) Color(0xFF4A3A1E) else Color(0xFFFCEBCB)

    /** Text/icons on [LimeSoft]. */
    val OnSoft get() = if (dark) Lime else Deep

    /** Dark fills (buttons, nav bar, selected chips). */
    val Ink get() = if (dark) Color(0xFF2D3D34) else Color(0xFF0E1B14)

    /** Primary text and icons on [Paper] / [Surface]. */
    val Text get() = if (dark) Color(0xFFF0F3EE) else Color(0xFF0E1B14)

    val Paper get() = if (dark) Color(0xFF0E1410) else Color(0xFFF4F4F1)
    val Surface get() = if (dark) Color(0xFF18211C) else Color(0xFFFFFFFF)
    val Muted get() = if (dark) Color(0xFF9FADA4) else Color(0xFF6B7A70)
    val Line get() = if (dark) Color(0x24FFFFFF) else Color(0x120E1B14)
    val Track get() = if (dark) Color(0xFF3A4A41) else Color(0xFFD9DED9)
}

val Vazir = FontFamily(
    Font(R.font.vazirmatn_light, FontWeight.Light),
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_bold, FontWeight.Bold),
)

/** mode: 0 = follow system, 1 = light, 2 = dark. */
@Composable
fun PeriodTheme(mode: Int = 0, content: @Composable () -> Unit) {
    val dark = when (mode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    SideEffect { C.dark = dark }
    val view = LocalView.current
    SideEffect {
        val w = (view.context as? Activity)?.window
        if (w != null) {
            val ic = WindowCompat.getInsetsController(w, view)
            ic.isAppearanceLightStatusBars = !dark
            ic.isAppearanceLightNavigationBars = !dark
        }
    }
    val scheme = if (dark) {
        darkColorScheme(primary = C.Lime, onPrimary = C.Deep, background = C.Paper, surface = C.Surface, onSurface = C.Text)
    } else {
        lightColorScheme(primary = C.Ink, onPrimary = Color.White, background = C.Paper, surface = C.Paper, onSurface = C.Ink)
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
