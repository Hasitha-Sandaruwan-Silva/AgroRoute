package com.agroroute.core.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Presentation only. The real app stays composed and owns startup work and routing. */
@Composable
fun BrandedStartup(
    sessionResolved: Boolean,
    nativeSplashFinished: Boolean,
    darkTheme: Boolean,
    initialDarkTheme: Boolean = darkTheme,
    content: @Composable () -> Unit
) {
    var completed by rememberSaveable { mutableStateOf(false) }
    val timeline = remember { Animatable(0f) }
    val exit = remember { Animatable(0f) }
    val animationFinished = timeline.value >= 1f
    LaunchedEffect(nativeSplashFinished) {
        if (nativeSplashFinished && !completed) timeline.animateTo(1f, tween(2400, easing = LinearEasing))
    }
    LaunchedEffect(animationFinished, sessionResolved) {
        if (animationFinished && sessionResolved && !completed) {
            // Allow existing session-driven navigation to draw before uncovering its result.
            withFrameNanos { }
            withFrameNanos { }
            exit.animateTo(1f, tween(200))
            completed = true
        }
    }
    Box(Modifier.fillMaxSize()) {
        // Never move or dispose this composition when the overlay finishes.
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = if (completed) 1f else exit.value }
            .then(if (!completed) Modifier.clearAndSetSemantics { } else Modifier)) { content() }
        if (!completed) {
            com.agroroute.core.theme.StartupStatusBar(if (timeline.value < .08f) initialDarkTheme else darkTheme)
            StartupArtwork(timeline.value * 2400f, exit.value, darkTheme,
                animationFinished && !sessionResolved, initialDarkTheme)
        }
    }
}

private fun phase(time: Float, start: Float, end: Float): Float =
    FastOutSlowInEasing.transform(((time - start) / (end - start)).coerceIn(0f, 1f))

/** Geometry shared with tools/branding/Generate-StartupBranding.ps1. */
private fun leafOutline() = Path().apply {
    moveTo(27f, 55f); cubicTo(27f, 39f, 40f, 29f, 57f, 29f)
    lineTo(75f, 30f); lineTo(77f, 48f)
    cubicTo(77f, 66f, 63f, 79f, 47f, 79f)
    cubicTo(35f, 79f, 27f, 69f, 27f, 55f); close()
}
private fun routeCutout() = Path().apply {
    moveTo(40f, 69f); cubicTo(40f, 56f, 60f, 57f, 60f, 46f)
    lineTo(60f, 41f); cubicTo(60f, 36f, 67f, 36f, 67f, 41f)
    lineTo(67f, 46f); cubicTo(67f, 62f, 47f, 62f, 47f, 69f)
    cubicTo(47f, 74f, 40f, 74f, 40f, 69f); close()
}

@Composable
internal fun StartupArtwork(time: Float, exit: Float, dark: Boolean, waiting: Boolean, initialDark: Boolean = dark) {
    val initialBackground = if (initialDark) Color(0xFF0E211B) else Color(0xFFF5F1E9)
    val targetBackground = if (dark) Color(0xFF0E211B) else Color(0xFFF5F1E9)
    val background = lerp(initialBackground, targetBackground, phase(time, 0f, 400f))
    val initialInk = if (initialDark) Color(0xFFB8DE85) else Color(0xFF123D32)
    val targetInk = if (dark) Color(0xFFB8DE85) else Color(0xFF123D32)
    val ink = lerp(initialInk, targetInk, phase(time, 0f, 400f))
    val textInk = if (dark) Color(0xFFF5F1E9) else ink
    val accent = if (dark) Color(0xFFB8DE85) else Color(0xFF246B55)
    val mark = remember { leafOutline().apply { fillType = PathFillType.EvenOdd; addPath(routeCutout()) } }
    val route = remember { Path().apply {
        moveTo(43.5f, 69f); cubicTo(43.5f, 58f, 63.5f, 60f, 63.5f, 46f); lineTo(63.5f, 41f)
    } }
    val measure = remember { PathMeasure().apply { setPath(route, false) } }
    val reveal = phase(time, 400f, 1000f)
    val wordmark = phase(time, 800f, 1400f)
    val tagline = phase(time, 1300f, 1900f)
    val journey = phase(time, 1800f, 2380f)
    BoxWithConstraints(Modifier.fillMaxSize().testTag("startup-intro")
        .graphicsLayer { alpha = 1f - exit }.background(background)
        .pointerInput(Unit) { awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false).consume()
            do {
                val event = awaitPointerEvent()
                event.changes.forEach { it.consume() }
            } while (event.changes.any { it.pressed })
        } }) {
        Canvas(Modifier.fillMaxSize()) {
            val alpha = phase(time, 0f, 500f) * .045f
            drawOval(accent.copy(alpha = alpha), Offset(-size.width * .55f, -size.height * .15f),
                Size(size.width, size.height * .65f))
            drawOval(accent.copy(alpha = alpha), Offset(size.width * .7f, size.height * .6f),
                Size(size.width * .7f, size.height * .55f))
        }
        val iconSize = minOf(288.dp, maxHeight * .48f)
        Canvas(Modifier.size(iconSize).align(Alignment.Center).graphicsLayer {
            val settle = phase(time, 300f, 950f)
            scaleX = 1f - .08f * settle + .02f * exit; scaleY = scaleX
            translationY = -32.dp.toPx() * settle
        }) {
            scale(size.width / 108f, pivot = Offset.Zero) {
                // Begin with the native silhouette, then reveal the road inside it.
                drawPath(mark, ink)
                drawPath(route, accent.copy(alpha = reveal * .18f), style = Stroke(1.7f, cap = StrokeCap.Round))
                if (journey > 0f) {
                    val segment = Path()
                    measure.getSegment(0f, measure.length * journey, segment)
                    drawPath(segment, accent.copy(alpha = .7f), style = Stroke(1.7f, cap = StrokeCap.Round))
                    drawCircle(accent, 2.6f, measure.getPosition(measure.length * journey))
                }
            }
        }
        Column(Modifier.align(Alignment.TopCenter).offset(y = maxHeight / 2 + iconSize * .14f)
            .fillMaxWidth().padding(horizontal = 24.dp)
            .graphicsLayer { scaleX = 1f + .015f * exit; scaleY = scaleX },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("AgroRoute", color = textInk, style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold, letterSpacing = (1.1f - wordmark * 1.4f).sp,
                textAlign = TextAlign.Center, modifier = Modifier.graphicsLayer {
                    alpha = wordmark; translationY = (1f - wordmark) * 10.dp.toPx()
                })
            Text("From Harvest to Destination", color = textInk.copy(alpha = .68f),
                style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer { alpha = tagline; translationY = (1f - tagline) * 8.dp.toPx() })
            if (waiting) {
                LinearProgressIndicator(Modifier.padding(top = 14.dp).width(72.dp).height(2.dp)
                    .testTag("startup-session-loading"), color = accent, trackColor = accent.copy(alpha = .12f))
                Text("Restoring your session", style = MaterialTheme.typography.labelSmall,
                    color = textInk.copy(alpha = .6f))
            }
        }
    }
}
