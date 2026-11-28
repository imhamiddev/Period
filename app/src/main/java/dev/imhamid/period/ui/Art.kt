package dev.imhamid.period.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.imhamid.period.engine.CycleEngine
import dev.imhamid.period.engine.Overview
import dev.imhamid.period.engine.Phase
import java.time.LocalDate
import java.time.temporal.ChronoUnit

fun phaseColor(p: Phase?): Color = when (p) {
    Phase.MENSTRUAL -> C.Period
    Phase.FOLLICULAR -> C.Follicular
    Phase.FERTILE, Phase.OVULATION -> C.Ovulation
    Phase.LUTEAL, Phase.PMS -> C.Luteal
    null -> C.Track
}

/** Cycle dial: period arc (pink), fertile window (yellow), PMS (purple), a knob for today and fine ticks inside. */
@Composable
fun LunarRing(engine: CycleEngine?, ov: Overview?, size: Dp = 300.dp, content: @Composable () -> Unit) {
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    val prog by animateFloatAsState(if (started) 1f else 0f, tween(1300, easing = FastOutSlowInEasing), label = "ring")
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        0.9f, 1.12f, infiniteRepeatable(tween(1600), RepeatMode.Reverse), label = "p",
    )
    val track = C.Track
    val face = C.Surface
    val tick = C.Muted.copy(alpha = 0.45f)
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val stroke = 26.dp.toPx()
            val pad = stroke / 2 + 2.dp.toPx()
            val d = this.size.minDimension - pad * 2
            val tl = Offset(pad, pad)
            val sz = Size(d, d)
            drawArc(track.copy(alpha = 0.55f), -90f, 360f, false, tl, sz, style = Stroke(stroke))
            drawCircle(face, d / 2 - stroke / 2 - 2.dp.toPx(), Offset(this.size.width / 2, this.size.height / 2))
            // ticks
            val cx = this.size.width / 2
            val cy = this.size.height / 2
            val rIn = d / 2 - stroke / 2 - 14.dp.toPx()
            for (i in 0 until 40) {
                val a = Math.toRadians(i * 9.0)
                val len = if (i % 5 == 0) 7.dp.toPx() else 4.dp.toPx()
                drawLine(
                    tick,
                    Offset(cx + ((rIn - len) * Math.cos(a)).toFloat(), cy + ((rIn - len) * Math.sin(a)).toFloat()),
                    Offset(cx + (rIn * Math.cos(a)).toFloat(), cy + (rIn * Math.sin(a)).toFloat()),
                    1.2.dp.toPx(),
                )
            }
            val seg = engine?.segmentFor(engine.today)
            if (engine != null && seg != null && ov != null) {
                val len = seg.length.toFloat()
                fun idx(x: LocalDate) = ChronoUnit.DAYS.between(seg.start, x).toInt() + 1
                fun arc(fromIdx: Int, toIdx: Int, color: Color) {
                    val a0 = -90f + (fromIdx - 1) / len * 360f
                    val sw = (toIdx - fromIdx + 1) / len * 360f * prog
                    if (sw > 2f) drawArc(color, a0 + 1f, sw - 2f, false, tl, sz, style = Stroke(stroke, cap = StrokeCap.Round))
                }
                arc(1, idx(seg.periodEnd), C.Period)
                if (!engine.hormonal && idx(seg.fertileEnd) > idx(seg.periodEnd)) arc(idx(seg.fertileStart), idx(seg.fertileEnd), C.Ovulation)
                arc(seg.length - engine.pmsDays + 1, seg.length, C.Luteal.copy(alpha = 0.85f))
                // today's knob
                val day = (ov.cycleDay ?: 1).coerceIn(1, seg.length)
                val ang = Math.toRadians((-90f + (day - 0.5f) / len * 360f * prog).toDouble())
                val r = d / 2
                val c = Offset(pad + r + (r * Math.cos(ang)).toFloat(), pad + r + (r * Math.sin(ang)).toFloat())
                drawCircle(Color.Black.copy(alpha = 0.10f), 20.dp.toPx() * pulse, c)
                drawCircle(Color.White, 15.dp.toPx(), c)
                drawCircle(phaseColor(ov.phase), 10.dp.toPx(), c)
            }
        }
        content()
    }
}

/** Simplified uterus with fallopian tubes and ovaries, drawn on a path so it scales with [size]. */
@Composable
fun Uterus(size: Dp, circle: Color, modifier: Modifier = Modifier, glow: Color? = null) {
    Box(modifier.size(size).clip(CircleShape).background(circle), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size * 0.78f)) {
            val w = this.size.width
            val h = this.size.height
            val dark = Color(0xFFC8433F)
            val mid = Color(0xFFE5675F)
            val light = Color(0xFFF7B5AE)
            val stroke = Stroke(w * 0.06f, cap = StrokeCap.Round)
            // tubes
            val left = Path().apply {
                moveTo(w * 0.42f, h * 0.36f)
                cubicTo(w * 0.28f, h * 0.20f, w * 0.18f, h * 0.18f, w * 0.10f, h * 0.30f)
                cubicTo(w * 0.06f, h * 0.38f, w * 0.12f, h * 0.46f, w * 0.20f, h * 0.44f)
            }
            val right = Path().apply {
                moveTo(w * 0.58f, h * 0.36f)
                cubicTo(w * 0.72f, h * 0.20f, w * 0.82f, h * 0.18f, w * 0.90f, h * 0.30f)
                cubicTo(w * 0.94f, h * 0.38f, w * 0.88f, h * 0.46f, w * 0.80f, h * 0.44f)
            }
            drawPath(left, dark, style = stroke)
            drawPath(right, dark, style = stroke)
            // ovaries
            drawCircle(Color.White, w * 0.085f, Offset(w * 0.16f, h * 0.50f))
            drawCircle(Color.White, w * 0.085f, Offset(w * 0.84f, h * 0.50f))
            drawCircle(glow ?: light, w * 0.035f, Offset(w * 0.16f, h * 0.50f))
            drawCircle(glow ?: light, w * 0.035f, Offset(w * 0.84f, h * 0.50f))
            // body
            val body = Path().apply {
                moveTo(w * 0.36f, h * 0.30f)
                cubicTo(w * 0.40f, h * 0.24f, w * 0.60f, h * 0.24f, w * 0.64f, h * 0.30f)
                cubicTo(w * 0.72f, h * 0.46f, w * 0.66f, h * 0.66f, w * 0.56f, h * 0.76f)
                lineTo(w * 0.44f, h * 0.76f)
                cubicTo(w * 0.34f, h * 0.66f, w * 0.28f, h * 0.46f, w * 0.36f, h * 0.30f)
                close()
            }
            drawPath(body, mid)
            drawPath(body, dark, style = Stroke(w * 0.025f))
            // cavity
            val cavity = Path().apply {
                moveTo(w * 0.42f, h * 0.36f)
                lineTo(w * 0.58f, h * 0.36f)
                lineTo(w * 0.52f, h * 0.66f)
                lineTo(w * 0.48f, h * 0.66f)
                close()
            }
            drawPath(cavity, light)
            // cervix
            drawRoundRect(dark, Offset(w * 0.455f, h * 0.76f), Size(w * 0.09f, h * 0.14f), androidx.compose.ui.geometry.CornerRadius(w * 0.03f))
        }
    }
}

enum class FaceKind { Smile, Sad, Calm, Neutral }

/** Small round face drawn on a canvas (used by the calendar). */
@Composable
fun Face(color: Color, kind: FaceKind, size: Dp = 28.dp, modifier: Modifier = Modifier, ink: Color = Color(0xFF3B2A33)) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val c = Offset(w / 2, w / 2)
        drawCircle(color, w / 2, c)
        val eyeY = w * 0.40f
        val dx = w * 0.19f
        val st = Stroke(w * 0.07f, cap = StrokeCap.Round)
        when (kind) {
            FaceKind.Calm -> {
                // closed eyes (little arcs) and a tiny smile
                drawArc(ink, 200f, 140f, false, Offset(c.x - dx - w * 0.08f, eyeY - w * 0.06f), Size(w * 0.16f, w * 0.12f), style = st)
                drawArc(ink, 200f, 140f, false, Offset(c.x + dx - w * 0.08f, eyeY - w * 0.06f), Size(w * 0.16f, w * 0.12f), style = st)
                drawArc(ink, 20f, 140f, false, Offset(c.x - w * 0.11f, w * 0.52f), Size(w * 0.22f, w * 0.14f), style = st)
            }
            else -> {
                drawCircle(ink, w * 0.05f, Offset(c.x - dx, eyeY))
                drawCircle(ink, w * 0.05f, Offset(c.x + dx, eyeY))
                when (kind) {
                    FaceKind.Smile -> drawArc(ink, 20f, 140f, false, Offset(c.x - w * 0.16f, w * 0.48f), Size(w * 0.32f, w * 0.22f), style = st)
                    FaceKind.Sad -> drawArc(ink, 200f, 140f, false, Offset(c.x - w * 0.14f, w * 0.58f), Size(w * 0.28f, w * 0.20f), style = st)
                    else -> drawLine(ink, Offset(c.x - w * 0.12f, w * 0.66f), Offset(c.x + w * 0.12f, w * 0.66f), w * 0.07f, StrokeCap.Round)
                }
            }
        }
    }
}
