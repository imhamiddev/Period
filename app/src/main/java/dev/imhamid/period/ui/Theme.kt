package dev.imhamid.period.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import dev.imhamid.period.R

object C {
    val Lime = Color(0xFFBDEF6A)
    val LimeSoft = Color(0xFFE4F7BF)
    val Green = Color(0xFF3FA544)
    val GreenMid = Color(0xFF1F7A36)
    val Deep = Color(0xFF0B3D20)
    val Ink = Color(0xFF0E1B14)
    val Paper = Color(0xFFF4F4F1)
    val Muted = Color(0xFF6B7A70)
    val Line = Color(0x120E1B14)
    val Coral = Color(0xFFFF8A7A)
    val CoralSoft = Color(0xFFFFE1DC)
    val Amber = Color(0xFFF2B35E)
    val AmberSoft = Color(0xFFFCEBCB)
}

val Vazir = FontFamily(
    Font(R.font.vazirmatn_light, FontWeight.Light),
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_bold, FontWeight.Bold),
)

@Composable
fun PeriodTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = C.Ink,
            onPrimary = Color.White,
            background = C.Paper,
            surface = C.Paper,
            onSurface = C.Ink,
        ),
        content = content,
    )
}
