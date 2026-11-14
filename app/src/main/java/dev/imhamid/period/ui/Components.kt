package dev.imhamid.period.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun Txt(
    text: String,
    size: TextUnit = 16.sp,
    weight: FontWeight = FontWeight.Normal,
    color: Color = C.Ink,
    modifier: Modifier = Modifier,
    align: TextAlign? = null,
    lineHeight: TextUnit = size * 1.55f,
    maxLines: Int = Int.MAX_VALUE,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        textAlign = align,
        maxLines = maxLines,
        style = TextStyle(fontFamily = Vazir, fontSize = size, fontWeight = weight, lineHeight = lineHeight),
    )
}

/** Soft spring-scale press + light haptic tick. */
fun Modifier.pressable(enabled: Boolean = true, onClick: () -> Unit): Modifier = composed {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.95f else 1f,
        spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
        label = "press",
    )
    val haptic = LocalHapticFeedback.current
    this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(interactionSource = src, indication = null, enabled = enabled) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        }
}

/** Slowly drifting green aurora, like the reference screens. */
@Composable
fun Aurora(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit = {}) {
    val t = rememberInfiniteTransition(label = "aurora")
    val a by t.animateFloat(0f, 1f, infiniteRepeatable(tween(13000, easing = LinearEasing), RepeatMode.Reverse), label = "a")
    val b by t.animateFloat(0f, 1f, infiniteRepeatable(tween(17000, easing = LinearEasing), RepeatMode.Reverse), label = "b")
    Box(
        modifier
            .background(Brush.verticalGradient(listOf(Color(0xFF8ED65A), C.Green, C.GreenMid, C.Deep)))
            .drawBehind {
                val w = size.width
                val h = size.height
                val r1 = w * 1.05f
                val c1 = Offset(w * (0.85f - 0.55f * a), h * (0.12f + 0.12f * b))
                drawRect(Brush.radialGradient(listOf(C.Lime.copy(alpha = 0.95f), Color.Transparent), c1, r1))
                val r2 = w * 0.9f
                val c2 = Offset(w * (0.1f + 0.5f * b), h * (0.72f - 0.14f * a))
                drawRect(Brush.radialGradient(listOf(C.Deep.copy(alpha = 0.85f), Color.Transparent), c2, r2))
                val r3 = w * 0.7f
                val c3 = Offset(w * (0.2f + 0.3f * a), h * (0.35f + 0.1f * b))
                drawRect(Brush.radialGradient(listOf(Color(0xFF45B24A).copy(alpha = 0.55f), Color.Transparent), c3, r3))
            },
        content = content,
    )
}

/** Fade + rise entrance with a stagger index. */
@Composable
fun Reveal(index: Int = 0, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(index * 70L); shown = true }
    val alpha by animateFloatAsState(if (shown) 1f else 0f, tween(520), label = "ra")
    val off by animateDpAsState(if (shown) 0.dp else 22.dp, tween(620, easing = FastOutSlowInEasing), label = "ro")
    Box(modifier.graphicsLayer { this.alpha = alpha; translationY = off.toPx() }) { content() }
}

enum class BtnKind { Dark, Light, Glass, Lime }

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: BtnKind = BtnKind.Dark,
    loading: Boolean = false,
    enabled: Boolean = true,
    icon: (@Composable () -> Unit)? = null,
) {
    val bg by animateColorAsState(
        when {
            !enabled -> if (kind == BtnKind.Dark) Color(0xFF9AA59E) else Color.White.copy(alpha = 0.35f)
            kind == BtnKind.Dark -> C.Ink
            kind == BtnKind.Light -> Color.White
            kind == BtnKind.Lime -> C.Lime
            else -> Color.White.copy(alpha = 0.18f)
        },
        label = "btnbg",
    )
    val fg = when (kind) {
        BtnKind.Dark -> Color.White
        BtnKind.Light, BtnKind.Lime -> C.Ink
        BtnKind.Glass -> Color.White
    }
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current
    val scale by animateFloatAsState(if (pressed) 0.95f else 1f, spring(dampingRatio = 0.45f, stiffness = 420f), label = "bs")
    val glow by animateFloatAsState(if (pressed) 0.16f else 0f, tween(140), label = "bg")
    val glowColor = if (kind == BtnKind.Light || kind == BtnKind.Lime) Color.Black else Color.White
    Box(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(29.dp))
            .background(bg)
            .then(if (kind == BtnKind.Glass) Modifier.border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(29.dp)) else Modifier)
            .clickable(interactionSource = src, indication = null, enabled = enabled && !loading) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .drawBehind { drawRect(glowColor.copy(alpha = glow)) },
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(loading, transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) }, label = "btn") { l ->
            if (l) CircularProgressIndicator(Modifier.size(22.dp), color = fg, strokeWidth = 2.5.dp)
            else Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                icon?.invoke()
                Txt(text, 16.sp, FontWeight.Medium, fg)
            }
        }
    }
}

@Composable
fun Card(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Color.White)
            .border(BorderStroke(1.dp, C.Line), RoundedCornerShape(28.dp))
            .padding(20.dp),
    ) { content() }
}

@Composable
fun Field(
    value: String,
    onValue: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    password: Boolean = false,
    ltr: Boolean = false,
    imeAction: ImeAction = ImeAction.Next,
    onDone: () -> Unit = {},
    singleLine: Boolean = true,
) {
    var focused by remember { mutableStateOf(false) }
    var show by remember { mutableStateOf(false) }
    val border by animateColorAsState(if (focused) C.Green else C.Line, tween(200), label = "fb")
    val width by animateDpAsState(if (focused) 2.dp else 1.dp, tween(200), label = "fw")
    Box(modifier) {
        androidx.compose.foundation.layout.Column {
            Txt(label, 13.sp, color = C.Muted, modifier = Modifier.padding(horizontal = 4.dp))
            androidx.compose.foundation.layout.Spacer(Modifier.height(6.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .then(if (singleLine) Modifier.height(58.dp) else Modifier.heightIn(min = 120.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .border(width, border, RoundedCornerShape(20.dp))
                    .padding(horizontal = 18.dp, vertical = if (singleLine) 0.dp else 14.dp),
                verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
            ) {
                if (password) {
                    AnimatedContent(
                        show,
                        modifier = Modifier.pressable { show = !show },
                        transitionSpec = { (fadeIn(tween(180)) + scaleIn(spring(0.5f), 0.5f)) togetherWith (fadeOut(tween(120)) + scaleOut(targetScale = 0.5f)) },
                        label = "eye",
                    ) { sh ->
                        Icon(if (sh) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, contentDescription = null, tint = C.Muted, modifier = Modifier.size(22.dp))
                    }
                }
                if (password) androidx.compose.foundation.layout.Spacer(Modifier.size(12.dp))
                BasicTextField(
                    value = value,
                    onValueChange = onValue,
                    modifier = Modifier.weight(1f).onFocusChanged { focused = it.isFocused },
                    singleLine = singleLine,
                    minLines = if (singleLine) 1 else 4,
                    textStyle = TextStyle(
                        fontFamily = Vazir, fontSize = 16.sp, color = C.Ink,
                        textDirection = if (ltr || password) TextDirection.Ltr else TextDirection.Content,
                    ),
                    cursorBrush = SolidColor(C.Green),
                    visualTransformation = if (password && !show) PasswordVisualTransformation() else VisualTransformation.None,
                    keyboardOptions = KeyboardOptions(keyboardType = if (password) KeyboardType.Password else keyboardType, imeAction = imeAction),
                    keyboardActions = KeyboardActions(onDone = { onDone() }, onGo = { onDone() }),
                )
            }
        }
    }
}

/** Animated progress bars, like the "Step 1 of 3" header in the reference. */
@Composable
fun StepBars(count: Int, current: Int, modifier: Modifier = Modifier, onDark: Boolean = true) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(count) { i ->
            val w by animateDpAsState(if (i == current) 44.dp else 26.dp, spring(0.7f), label = "sw")
            val c by animateColorAsState(
                if (i <= current) (if (onDark) Color.White else C.Ink) else (if (onDark) Color.White.copy(alpha = 0.3f) else C.Line),
                label = "sc",
            )
            Box(Modifier.size(w, 4.dp).clip(CircleShape).background(c))
        }
    }
}

/** Little drop mark used as logo. */
@Composable
fun DropMark(size: Dp, color: Color = Color.White, accent: Color = C.Lime) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val p = Path().apply {
            moveTo(w * 0.5f, h * 0.08f)
            cubicTo(w * 0.5f, h * 0.08f, w * 0.14f, h * 0.48f, w * 0.14f, h * 0.65f)
            cubicTo(w * 0.14f, h * 0.85f, w * 0.30f, h * 0.94f, w * 0.5f, h * 0.94f)
            cubicTo(w * 0.70f, h * 0.94f, w * 0.86f, h * 0.85f, w * 0.86f, h * 0.65f)
            cubicTo(w * 0.86f, h * 0.48f, w * 0.5f, h * 0.08f, w * 0.5f, h * 0.08f)
            close()
        }
        drawPath(p, color)
        drawCircle(accent, radius = w * 0.1f, center = Offset(w * 0.62f, h * 0.68f))
    }
}

@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val bg by animateColorAsState(if (selected) C.Ink else Color.White, tween(220), label = "chipbg")
    val fg by animateColorAsState(if (selected) Color.White else C.Ink, tween(220), label = "chipfg")
    val sc by animateFloatAsState(if (selected) 1.05f else 1f, spring(0.45f, 500f), label = "chipsc")
    Box(
        modifier.graphicsLayer { scaleX = sc; scaleY = sc }
            .clip(RoundedCornerShape(20.dp)).background(bg).border(1.dp, C.Line, RoundedCornerShape(20.dp))
            .pressable(onClick = onClick).padding(horizontal = 14.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) { Txt(text, 14.sp, color = fg) }
}
