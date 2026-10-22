package com.inouiw.plateline.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.KeyboardType
import com.inouiw.plateline.ui.theme.*
import kotlinx.coroutines.delay

/** Fade + slide up on first composition. */
@Composable
fun Modifier.rise(delayMs: Int = 0): Modifier {
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(delayMs.toLong())
        a.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
    }
    return this.graphicsLayer { alpha = a.value; translationY = (1f - a.value) * 48f }
}

/** Scale-down press feedback. */
@Composable
fun Modifier.bounceClick(enabled: Boolean = true, onClick: () -> Unit): Modifier {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) 0.96f else 1f, spring(stiffness = Spring.StiffnessMedium), label = "press")
    return this.graphicsLayer { scaleX = s; scaleY = s }
        .clickable(interactionSource = src, indication = null, enabled = enabled, onClick = onClick)
}

@Composable
fun ScreenBox(bg: Color = Paper, content: @Composable BoxScope.() -> Unit) {
    Box(Modifier.fillMaxSize().background(bg).statusBarsPadding().navigationBarsPadding(), content = content)
}

@Composable
fun LogoMark(width: Dp = 40.dp, animate: Boolean = true) {
    val t = rememberInfiniteTransition(label = "logo")
    val draw by t.animateFloat(0f, 1f, infiniteRepeatable(tween(4500, easing = LinearEasing)), label = "draw")
    val drive by t.animateFloat(0f, 1f, infiniteRepeatable(tween(4500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "drive")
    Canvas(Modifier.width(width).height(width * 52f / 72f)) {
        val s = size.width / 72f
        drawRoundRect(Yellow, Offset(2 * s, 2 * s), Size(68 * s, 40 * s), androidx.compose.ui.geometry.CornerRadius(10 * s))
        drawCircle(Ink.copy(alpha = .4f), 1.6f * s, Offset(9 * s, 22 * s))
        drawCircle(Ink.copy(alpha = .4f), 1.6f * s, Offset(63 * s, 22 * s))
        val p = Path().apply {
            moveTo(29 * s, 33 * s); lineTo(29 * s, 12 * s); lineTo(38 * s, 12 * s)
            arcTo(Rect(31 * s, 12 * s, 45 * s, 26 * s), -90f, 180f, false)
            lineTo(29 * s, 26 * s)
        }
        val pm = PathMeasure().apply { setPath(p, false) }
        val seg = Path()
        val frac = if (animate) (draw / 0.35f).coerceIn(0f, 1f) else 1f
        pm.getSegment(0f, pm.length * frac, seg, true)
        val fade = if (animate && draw > 0.92f) 1f - (draw - 0.92f) / 0.08f else 1f
        drawPath(seg, Color(0xFF0A0E1F).copy(alpha = fade), style = Stroke(6 * s, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawLine(Ink.copy(alpha = .25f), Offset(2 * s, 49 * s), Offset(70 * s, 49 * s), 3 * s, StrokeCap.Round)
        drawCircle(Ink, 3.4f * s, Offset((8 + 56 * (if (animate) drive else 0f)) * s, 49 * s))
    }
}

@Composable
fun Wordmark(size: Int = 21, onInk: Boolean = false) {
    Text(buildAnnotatedString {
        withStyle(SpanStyle(color = if (onInk) White else Ink)) { append("plate") }
        withStyle(SpanStyle(color = if (onInk) Yellow else Amber)) { append("line") }
    }, style = sora(size, 800))
}

@Composable
fun LogoLockup() = Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
    LogoMark(40.dp); Wordmark()
}

@Composable
fun Plate(text: String, modifier: Modifier = Modifier, height: Dp = 92.dp, fontSize: Int = 58, float: Boolean = true) {
    val t = rememberInfiniteTransition(label = "plate")
    val dy by t.animateFloat(0f, -10f, infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "float")
    val shine by t.animateFloat(-0.3f, 1.3f, infiniteRepeatable(tween(4800, easing = LinearEasing)), label = "shine")
    Row(
        modifier
            .graphicsLayer { translationY = if (float) dy else 0f }
            .shadow(14.dp, RoundedCornerShape(14.dp), ambientColor = Ink, spotColor = Ink)
            .height(height).clip(RoundedCornerShape(14.dp))
            .background(Brush.verticalGradient(listOf(YellowLight, Yellow, YellowDeep)))
            .drawWithShine(shine),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.width(height * 0.46f).fillMaxHeight().background(PlateBlue), Arrangement.Center, Alignment.CenterHorizontally) {
            Text("IND", style = dm((height.value * .12f).toInt().coerceAtLeast(10), 700, White))
        }
        Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
            Text(text, style = TextStyle(fontFamily = Barlow, fontWeight = FontWeight.Bold, fontSize = fontSize.sp, letterSpacing = 2.5.sp, color = Ink), maxLines = 1)
        }
    }
}

private fun Modifier.drawWithShine(p: Float) = this.drawWithContent {
    drawContent()
    val w = size.width * 0.16f
    val x = p * size.width
    drawRect(Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(alpha = .6f), Color.Transparent), startX = x, endX = x + w), Offset(x, 0f), Size(w, size.height))
}

@Composable
fun BackButton(onClick: () -> Unit) {
    Box(Modifier.size(48.dp).clip(CircleShape).background(White).bounceClick(onClick = onClick), Alignment.Center) {
        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = Ink, modifier = Modifier.size(22.dp))
    }
}

@Composable
fun TopBar(title: String, trailing: String? = null, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        BackButton(onBack)
        Text(title, Modifier.weight(1f), style = sora(17, 700), textAlign = TextAlign.Center)
        if (trailing != null) Text(trailing, style = dm(14, 700, Slate), modifier = Modifier.widthIn(min = 48.dp), textAlign = TextAlign.End)
        else Spacer(Modifier.width(48.dp))
    }
}

@Composable
fun PrimaryCta(label: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    val nudge by rememberInfiniteTransition(label = "n").animateFloat(0f, 4f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "nudge")
    Row(
        modifier.fillMaxWidth().height(64.dp)
            .shadow(10.dp, CircleShape, ambientColor = Ink, spotColor = Ink)
            .clip(CircleShape).background(if (enabled) Ink else Ink.copy(alpha = .35f))
            .bounceClick(enabled, onClick).padding(start = 26.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, Modifier.weight(1f), style = sora(16, 600, White))
        Box(Modifier.size(48.dp).clip(CircleShape).background(Yellow), Alignment.Center) {
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = Ink, modifier = Modifier.size(22.dp).graphicsLayer { translationX = nudge })
        }
    }
}

@Composable
fun SecondaryButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(modifier.fillMaxWidth().height(60.dp).clip(CircleShape).background(White).border(1.dp, Line, CircleShape).bounceClick(onClick = onClick), Alignment.Center) {
        Text(label, style = dm(16, 700))
    }
}

@Composable
fun Card(modifier: Modifier = Modifier, radius: Dp = 28.dp, bg: Color = White, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.clip(RoundedCornerShape(radius)).background(bg), content = content)
}

@Composable
fun LaneAvatar(size: Dp = 44.dp, ping: Boolean = false) {
    val t = rememberInfiniteTransition(label = "ping")
    val s by t.animateFloat(0.6f, 2.1f, infiniteRepeatable(tween(2000, easing = LinearEasing)), label = "s")
    Box(Modifier.size(size), Alignment.Center) {
        if (ping) Box(Modifier.size(size).graphicsLayer { scaleX = s * .6f; scaleY = s * .6f; alpha = (1f - (s - .6f) / 1.5f).coerceIn(0f, .9f) }.border(2.dp, Yellow, CircleShape))
        Box(Modifier.size(size).clip(CircleShape).background(Ink), Alignment.Center) {
            Icon(Icons.Rounded.AutoAwesome, null, tint = Yellow, modifier = Modifier.size(size * .45f))
        }
    }
}

@Composable
fun YellowDot(icon: ImageVector, size: Dp = 40.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(Yellow), Alignment.Center) {
        Icon(icon, null, tint = Ink, modifier = Modifier.size(size * .45f))
    }
}

@Composable
fun LaneTip(text: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(White).padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        LaneAvatar(32.dp)
        Text(buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("Lane: ") }
            append(text)
        }, style = dm(13))
    }
}

@Composable
fun Field(label: String, value: String, onChange: (String) -> Unit, placeholder: String = "", keyboard: KeyboardType = KeyboardType.Text, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = dm(13, 700, Color(0xFF3C414C)))
        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth().height(60.dp).clip(CircleShape).background(White).border(1.dp, Line, CircleShape).padding(horizontal = 20.dp), Alignment.CenterStart) {
            BasicTextField(value, onChange, singleLine = true, textStyle = dm(17, 500), keyboardOptions = KeyboardOptions(keyboardType = keyboard), modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner -> if (value.isEmpty()) Text(placeholder, style = dm(17, 500, Color(0xFF9AA0AE))); inner() })
        }
    }
}

enum class Tab { Home, Offers, Lane, Profile }

@Composable
fun BottomNav(selected: Tab, modifier: Modifier = Modifier, onSelect: (Tab) -> Unit) {
    val items = listOf(Tab.Home to Icons.Rounded.Home, Tab.Offers to Icons.Rounded.LocalOffer, Tab.Lane to Icons.Rounded.ChatBubble, Tab.Profile to Icons.Rounded.Person)
    Row(
        modifier.width(272.dp).height(72.dp).shadow(14.dp, CircleShape, ambientColor = Ink, spotColor = Ink).clip(CircleShape).background(White).padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { (tab, icon) ->
            val bg by animateColorAsState(if (tab == selected) Yellow else Color.Transparent, label = "tab")
            Box(Modifier.size(56.dp).clip(CircleShape).background(bg).bounceClick { onSelect(tab) }, Alignment.Center) {
                Icon(icon, tab.name, tint = Ink, modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Composable
fun StepBar(progress: Float) {
    val p by animateFloatAsState(progress, tween(900), label = "bar")
    Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(Line)) {
        Box(Modifier.fillMaxWidth(p).fillMaxHeight().clip(CircleShape).background(Ink))
    }
}

@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(Modifier.clip(CircleShape).background(if (selected) Ink else White).border(1.dp, if (selected) Ink else Line, CircleShape).bounceClick(onClick = onClick).padding(horizontal = 18.dp, vertical = 12.dp)) {
        Text(text, style = dm(14, 700, if (selected) White else Ink))
    }
}

@Composable
fun Tick(size: Dp = 28.dp, dark: Boolean = true) {
    Box(Modifier.size(size).clip(CircleShape).background(if (dark) Ink else Yellow), Alignment.Center) {
        Icon(Icons.Rounded.Check, null, tint = if (dark) Yellow else Ink, modifier = Modifier.size(size * .6f))
    }
}
