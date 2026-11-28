package dev.imhamid.period.ui

import androidx.compose.runtime.setValue
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.distinctUntilChanged

/** Full-screen page with the status-bar inset and the app background. */
@Composable
fun Page(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxSize().background(C.Paper).statusBarsPadding(), content = content)
}

@Composable
fun TopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    close: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
) {
    Box(Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 16.dp)) {
        if (onBack != null) {
            Box(Modifier.align(Alignment.CenterStart).size(44.dp).pressable(role = null) { onBack() }, contentAlignment = Alignment.Center) {
                Icon(if (close) Icons.Rounded.Close else Icons.AutoMirrored.Rounded.ArrowBack, "بازگشت", tint = C.Text, modifier = Modifier.size(24.dp))
            }
        }
        Txt(title, 19.sp, FontWeight.Bold, modifier = Modifier.align(Alignment.Center), align = TextAlign.Center, maxLines = 1)
        if (trailing != null) Box(Modifier.align(Alignment.CenterEnd)) { trailing() }
    }
}

/** White rounded card. */
@Composable
fun Panel(modifier: Modifier = Modifier, padding: Dp = 16.dp, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(C.Surface).padding(padding),
        content = content,
    )
}

@Composable
fun SectionHeader(title: String, onMore: (() -> Unit)? = null, moreText: String? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Txt(title, 18.sp, FontWeight.Bold, modifier = Modifier.weight(1f))
        if (onMore != null) {
            Row(Modifier.pressable(role = null) { onMore() }, verticalAlignment = Alignment.CenterVertically) {
                if (moreText != null) Txt(moreText, 14.sp, FontWeight.Medium, C.Pink)
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = C.Pink, modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
fun BigButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = C.Pink,
    enabled: Boolean = true,
    busy: Boolean = false,
) {
    val bg by animateColorAsState(if (enabled) color else color.copy(alpha = 0.35f), tween(200), label = "bb")
    Box(
        modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(28.dp)).background(bg)
            .pressable(enabled = enabled && !busy) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        if (busy) androidx.compose.material3.CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.5.dp)
        else Txt(text, 17.sp, FontWeight.Bold, Color.White)
    }
}

@Composable
fun OutlinePill(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, color: Color = C.Text) {
    Row(
        modifier.clip(RoundedCornerShape(24.dp)).border(1.dp, C.Line, RoundedCornerShape(24.dp))
            .pressable { onClick() }.padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (icon != null) Icon(icon, null, tint = C.Pink, modifier = Modifier.size(18.dp))
        Txt(text, 15.sp, FontWeight.Medium, color)
    }
}

@Composable
fun LSwitch(checked: Boolean, onChange: (Boolean) -> Unit, color: Color = C.Pink) {
    val bg by animateColorAsState(if (checked) color else C.Track, tween(200), label = "sw")
    val x by animateDpAsState(if (checked) 22.dp else 2.dp, tween(200), label = "swx")
    Box(
        Modifier.width(48.dp).height(28.dp).clip(RoundedCornerShape(14.dp)).background(bg)
            .pressable(role = androidx.compose.ui.semantics.Role.Switch) { onChange(!checked) },
    ) {
        Box(Modifier.padding(start = x, top = 2.dp).size(24.dp).clip(CircleShape).background(Color.White))
    }
}

@Composable
fun ToggleRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit, sub: String? = null, color: Color = C.Pink) {
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Txt(title, 16.sp, FontWeight.Medium)
            if (sub != null) Txt(sub, 12.sp, color = C.Muted)
        }
        Spacer(Modifier.width(12.dp))
        LSwitch(checked, onChange, color)
    }
}

@Composable
fun SettingRow(
    title: String,
    onClick: (() -> Unit)?,
    value: String? = null,
    sub: String? = null,
    icon: ImageVector? = null,
    danger: Boolean = false,
    chevron: Boolean = true,
) {
    val tint = if (danger) C.Red else C.Text
    Row(
        Modifier.fillMaxWidth().then(if (onClick != null) Modifier.pressable(role = androidx.compose.ui.semantics.Role.Button) { onClick() } else Modifier)
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = if (danger) C.Red else C.Text, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(14.dp))
        }
        Column(Modifier.weight(1f)) {
            Txt(title, 16.sp, FontWeight.Medium, tint)
            if (sub != null) Txt(sub, 12.sp, color = C.Muted)
        }
        if (value != null) {
            Txt(value, 14.sp, FontWeight.Medium, C.Text)
            Spacer(Modifier.width(6.dp))
        }
        if (chevron && onClick != null) Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = if (danger) C.Red else C.Text, modifier = Modifier.size(22.dp))
    }
}

@Composable
fun RowDivider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(C.Line))
}

/** Round badge with an icon, optionally with a selected state and a count below. */
@Composable
fun CircleOpt(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    count: Int? = null,
    size: Dp = 62.dp,
    bg: Color = C.PinkSoft,
    selectedColor: Color = C.Pink,
) {
    val fill by animateColorAsState(if (selected) selectedColor else bg, tween(180), label = "fill")
    Column(
        modifier.then(if (onClick != null) Modifier.pressable(role = androidx.compose.ui.semantics.Role.Checkbox) { onClick() } else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(size).clip(CircleShape).background(fill), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = if (selected) Color.White else selectedColor, modifier = Modifier.size(size * 0.48f))
        }
        if (count != null) Txt("${Fa.n(count)}×", 16.sp, FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
        Txt(label, 11.sp, color = if (selected) selectedColor else C.Muted, align = TextAlign.Center, modifier = Modifier.padding(top = 4.dp), maxLines = 2, lineHeight = 15.sp)
    }
}

/** 1..4 blood drops. */
@Composable
fun Drops(n: Int, size: Dp = 34.dp, color: Color = C.Period) {
    Canvas(Modifier.size(width = size * 1.15f, height = size)) {
        fun drop(cx: Float, cy: Float, r: Float) {
            val p = androidx.compose.ui.graphics.Path().apply {
                moveTo(cx, cy - r * 1.35f)
                cubicTo(cx + r * 0.2f, cy - r * 0.8f, cx + r, cy - r * 0.1f, cx + r, cy + r * 0.35f)
                cubicTo(cx + r, cy + r * 1.0f, cx + r * 0.55f, cy + r * 1.3f, cx, cy + r * 1.3f)
                cubicTo(cx - r * 0.55f, cy + r * 1.3f, cx - r, cy + r * 1.0f, cx - r, cy + r * 0.35f)
                cubicTo(cx - r, cy - r * 0.1f, cx - r * 0.2f, cy - r * 0.8f, cx, cy - r * 1.35f)
                close()
            }
            drawPath(p, color)
        }
        val w = this.size.width
        val h = this.size.height
        when (n.coerceIn(1, 4)) {
            1 -> drop(w / 2, h / 2, h * 0.28f)
            2 -> { drop(w * 0.36f, h * 0.55f, h * 0.24f); drop(w * 0.68f, h * 0.4f, h * 0.2f) }
            3 -> { drop(w * 0.3f, h * 0.6f, h * 0.2f); drop(w * 0.62f, h * 0.62f, h * 0.2f); drop(w * 0.46f, h * 0.28f, h * 0.17f) }
            else -> { drop(w * 0.25f, h * 0.62f, h * 0.17f); drop(w * 0.52f, h * 0.68f, h * 0.17f); drop(w * 0.76f, h * 0.55f, h * 0.15f); drop(w * 0.4f, h * 0.3f, h * 0.15f); drop(w * 0.66f, h * 0.28f, h * 0.13f) }
        }
    }
}

@Composable
fun FlowOpt(id: String, selected: Boolean, count: Int? = null, onClick: (() -> Unit)?, modifier: Modifier = Modifier) {
    val label = Catalog.flows.firstOrNull { it.id == id }?.label ?: "—"
    val ring by animateColorAsState(if (selected) C.Period else Color.Transparent, tween(180), label = "fring")
    Column(
        modifier.then(if (onClick != null) Modifier.pressable { onClick() } else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(62.dp).clip(CircleShape).background(if (selected) C.Period.copy(alpha = 0.14f) else C.SurfaceAlt).border(2.dp, ring, CircleShape),
            contentAlignment = Alignment.Center,
        ) { Drops(Catalog.drops(id), 34.dp) }
        if (count != null) Txt("${Fa.n(count)}×", 16.sp, FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
        Txt(label, 11.sp, color = if (selected) C.Period else C.Muted, modifier = Modifier.padding(top = 4.dp))
    }
}

/** Pink segmented control like "Log Count | Health". */
@Composable
fun SegTabs(options: List<String>, selected: Int, onSelect: (Int) -> Unit, color: Color = C.Pink) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(C.Surface).padding(4.dp)) {
        options.forEachIndexed { i, t ->
            val bg by animateColorAsState(if (i == selected) color else Color.Transparent, tween(200), label = "seg")
            Box(
                Modifier.weight(1f).height(42.dp).clip(RoundedCornerShape(11.dp)).background(bg).pressable(role = androidx.compose.ui.semantics.Role.Tab) { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) { Txt(t, 15.sp, FontWeight.Medium, if (i == selected) Color.White else C.Text) }
        }
    }
}

@Composable
fun FilterChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(if (selected) C.Pink else Color.Transparent, tween(180), label = "fc")
    Box(
        Modifier.clip(RoundedCornerShape(22.dp)).background(bg).border(1.dp, if (selected) C.Pink else C.Line, RoundedCornerShape(22.dp))
            .pressable { onClick() }.padding(horizontal = 18.dp, vertical = 9.dp),
    ) { Txt(text, 14.sp, FontWeight.Medium, if (selected) Color.White else C.Text) }
}

/** Horizontal ruler: scroll to choose a number. [scale] = how many units per tick (e.g. 10 → one decimal). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RulerPicker(
    value: Double,
    min: Int,
    max: Int,
    onChange: (Double) -> Unit,
    modifier: Modifier = Modifier,
    scale: Int = 1,
    color: Color = C.Pink,
) {
    val tickW = 12.dp
    val count = (max - min) * scale + 1
    val startIndex = ((value - min) * scale).toInt().coerceIn(0, count - 1)
    val state = rememberLazyListState(initialFirstVisibleItemIndex = startIndex)
    val screenW = LocalConfiguration.current.screenWidthDp.dp
    val pad = (screenW - tickW) / 2
    val cb = rememberUpdatedState(onChange)
    LaunchedEffect(state) {
        snapshotFlow { state.firstVisibleItemIndex + if (state.firstVisibleItemScrollOffset > 0 && state.isScrollInProgress.not()) 0 else 0 }
            .distinctUntilChanged()
            .collect { idx -> cb.value(min + idx.coerceIn(0, count - 1).toDouble() / scale) }
    }
    Box(modifier.fillMaxWidth().height(110.dp), contentAlignment = Alignment.TopCenter) {
        LazyRow(
            state = state,
            flingBehavior = rememberSnapFlingBehavior(state),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = pad),
            modifier = Modifier.fillMaxSize(),
        ) {
            itemsIndexed(List(count) { it }) { i, _ ->
                val big = i % (5 * scale.coerceAtLeast(1)) == 0
                Box(Modifier.width(tickW).fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                    Box(Modifier.padding(top = 36.dp).width(2.dp).height(if (big) 38.dp else 22.dp).clip(RoundedCornerShape(1.dp)).background(if (big) C.Text.copy(alpha = 0.55f) else C.Track))
                }
            }
        }
        Box(Modifier.padding(top = 20.dp).width(4.dp).height(64.dp).clip(RoundedCornerShape(2.dp)).background(color))
    }
}

/** One wheel column (for dates). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WheelColumn(items: List<String>, index: Int, onIndex: (Int) -> Unit, modifier: Modifier = Modifier) {
    val itemH = 46.dp
    val state = rememberLazyListState(initialFirstVisibleItemIndex = index.coerceIn(0, (items.size - 1).coerceAtLeast(0)))
    val cb = rememberUpdatedState(onIndex)
    val centered by remember { derivedStateOf { state.firstVisibleItemIndex } }
    LaunchedEffect(state) {
        snapshotFlow { state.firstVisibleItemIndex }.distinctUntilChanged().collect { cb.value(it.coerceIn(0, (items.size - 1).coerceAtLeast(0))) }
    }
    Box(modifier.height(itemH * 3), contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxWidth().height(itemH).clip(RoundedCornerShape(12.dp)).background(C.PinkSoft))
        LazyColumn(
            state = state,
            flingBehavior = rememberSnapFlingBehavior(state),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = itemH),
            modifier = Modifier.fillMaxSize(),
        ) {
            itemsIndexed(items) { i, t ->
                Box(Modifier.fillMaxWidth().height(itemH), contentAlignment = Alignment.Center) {
                    Txt(t, if (i == centered) 20.sp else 16.sp, if (i == centered) FontWeight.Bold else FontWeight.Normal, if (i == centered) C.Pink else C.Muted)
                }
            }
        }
    }
}

/** Jalali date wheels (day / month / year). Calls [onChange] with the Gregorian date. */
@Composable
fun JalaliDatePicker(initial: java.time.LocalDate, minYear: Int, maxYear: Int, onChange: (java.time.LocalDate) -> Unit, modifier: Modifier = Modifier) {
    val (iy, im, id) = remember { Fa.jalaliParts(initial) }
    var y by remember { androidx.compose.runtime.mutableIntStateOf(iy.coerceIn(minYear, maxYear)) }
    var m by remember { androidx.compose.runtime.mutableIntStateOf(im) }
    var d by remember { androidx.compose.runtime.mutableIntStateOf(id) }
    val years = remember { (minYear..maxYear).map { Fa.n(it) } }
    val len = Fa.jalaliMonthLen(y, m)
    val days = remember(len) { (1..len).map { Fa.n(it) } }
    LaunchedEffect(y, m, d) { onChange(Fa.fromJalali(y, m, d.coerceAtMost(len))) }
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        WheelColumn(days, (d - 1).coerceIn(0, len - 1), { d = it + 1 }, Modifier.weight(1f))
        WheelColumn(Fa.jalaliMonths, m - 1, { m = it + 1 }, Modifier.weight(1.4f))
        WheelColumn(years, y - minYear, { y = minYear + it }, Modifier.weight(1.2f))
    }
}

/** Small colored dot + label for legends. */
@Composable
fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Txt(label, 12.sp, color = C.Muted)
    }
}

@Composable
fun Hairline(modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxWidth().height(1.dp)) { drawLine(Color.Gray.copy(alpha = 0.25f), Offset(0f, 0f), Offset(size.width, 0f), 1f, cap = StrokeCap.Round) }
}
