package dev.imhamid.period.ui

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.core.view.WindowCompat
import dev.imhamid.period.R

class Accent(val name: String, val primary: Color)

val Accents = listOf(Accent("سرخابی", Color(0xFFE8457C)))

/** App palette (Lunari). Surface colors follow [dark]; phase colors stay the same in both themes. */
object C {
    /** Backed by Compose state, so every reader recomposes when the theme flips. */
    var dark by mutableStateOf(false)

    /** Index into [Accents]. */
    var accent by mutableIntStateOf(0)
    private val a: Accent get() = Accents[accent.coerceIn(0, Accents.lastIndex)]

    val Pink get() = a.primary
    val PinkSoft get() = if (dark) lerp(Color(0xFF1E1E1E), a.primary, 0.20f) else lerp(Color.White, a.primary, 0.16f)
    val PinkDeep get() = lerp(a.primary, Color.Black, 0.18f)

    // Cycle phases
    val Period get() = Pink
    val Follicular = Color(0xFF2FA8F0)
    val Ovulation = Color(0xFFFFC02D)
    val Luteal = Color(0xFF9B7BFF)
    val Fertile = Color(0xFFFFB020)
    val Orange = Color(0xFFFF9F1C)
    val Blue = Color(0xFF2196F3)
    val Green = Color(0xFF4CAF50)
    val Red = Color(0xFFE5484D)

    val Coral = Color(0xFFFF8A7A)
    val Amber = Color(0xFFF2B35E)
    val CoralSoft get() = if (dark) Color(0xFF4A2A26) else Color(0xFFFFE1DC)
    val AmberSoft get() = if (dark) Color(0xFF4A3A1E) else Color(0xFFFCEBCB)

    /** Primary text and icons on [Paper] / [Surface]. */
    val Text get() = if (dark) Color(0xFFF5F5F5) else Color(0xFF212121)
    val Paper get() = if (dark) Color(0xFF121212) else Color(0xFFF5F5F5)
    val Surface get() = if (dark) Color(0xFF1E1E1E) else Color(0xFFFFFFFF)
    val SurfaceAlt get() = if (dark) Color(0xFF2A2A2A) else Color(0xFFF3F3F3)
    val Muted get() = if (dark) Color(0xFFA3A3A3) else Color(0xFF8A8A8A)
    val Line get() = if (dark) Color(0x24FFFFFF) else Color(0x14000000)
    val Track get() = if (dark) Color(0xFF3A3A3A) else Color(0xFFE6E6E6)

    // ---- compatibility names (older components): buttons and highlights all follow the accent
    val Ink get() = Pink
    val Lime get() = Pink
    val LimeSoft get() = PinkSoft
    val OnSoft get() = Pink
    val Green2 get() = Pink
    val GreenMid get() = PinkDeep
    val Deep get() = PinkDeep
    val AuroraTop get() = Pink
    val AuroraMid get() = PinkDeep
}

val Vazir = FontFamily(
    Font(R.font.vazirmatn_light, FontWeight.Light),
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_bold, FontWeight.Bold),
)

/** mode: 0 = follow system, 1 = light, 2 = dark. */
@Composable
fun PeriodTheme(content: @Composable () -> Unit) {
    val dark = false
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
        darkColorScheme(primary = C.Pink, onPrimary = Color.White, background = C.Paper, surface = C.Surface, onSurface = C.Text)
    } else {
        lightColorScheme(primary = C.Pink, onPrimary = Color.White, background = C.Paper, surface = C.Paper, onSurface = C.Text)
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
