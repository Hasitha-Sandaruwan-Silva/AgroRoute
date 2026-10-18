package com.agroroute.core.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import com.agroroute.R
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
    val road = phase(time, 400f, 1100f)
    val arrow = phase(time, 900f, 1500f)
    val wordmark = phase(time, 1200f, 1650f)
    val tagline = phase(time, 1450f, 1900f)
    val journey = phase(time, 1800f, 2400f)
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
        // Same 288dp canvas and exact source-pixel registration as the native splash.
        // Its position is unchanged at handoff; only then does it settle above the name.
        val iconSize = minOf(288.dp, maxHeight * .48f)
        Box(Modifier.size(iconSize).align(Alignment.Center).testTag("startup-official-logo").graphicsLayer {
            val reveal = phase(time, 0f, 500f)
            val settle = phase(time, 500f, 1100f)
            alpha = .9f + .1f * reveal
            scaleX = .985f + .015f * reveal + .015f * exit; scaleY = scaleX
            translationY = -40.dp.toPx() * settle
        }) {
            Image(painterResource(R.drawable.official_startup_base), null,
                Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            Image(painterResource(R.drawable.official_startup_road), null,
                Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            Image(painterResource(R.drawable.official_startup_road), null,
                Modifier.fillMaxSize().graphicsLayer { alpha = .18f }.drawWithContent {
                    // A light sweep follows the existing road pixels, bottom to top.
                    // No path or replacement silhouette is drawn; the final mark is unchanged.
                    if (road > 0f && road < 1f) {
                        val top = size.height * (1f - road)
                        clipRect(top = top, bottom = top + size.height * .16f) { this@drawWithContent.drawContent() }
                    }
                }, colorFilter = ColorFilter.tint(Color.White), contentScale = ContentScale.Fit)
            Image(painterResource(R.drawable.official_startup_arrow), null,
                Modifier.fillMaxSize().graphicsLayer {
                    val approach = phase(time, 500f, 900f) * (1f - arrow)
                    translationX = -2.dp.toPx() * approach
                    translationY = 1.dp.toPx() * approach
                }, contentScale = ContentScale.Fit)
        }
        Column(Modifier.align(Alignment.TopCenter).offset(y = maxHeight / 2 + 64.dp)
            .fillMaxWidth().padding(horizontal = 24.dp)
            .graphicsLayer { scaleX = 1f + .015f * exit; scaleY = scaleX },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("AgroRoute", color = textInk, style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold, letterSpacing = (1.1f - wordmark * 1.4f).sp,
                textAlign = TextAlign.Center, modifier = Modifier.testTag("startup-name").graphicsLayer {
                    alpha = wordmark; translationY = (1f - wordmark) * 10.dp.toPx()
                })
            Text("From Harvest to Destination", color = textInk.copy(alpha = .68f),
                style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
                modifier = Modifier.testTag("startup-tagline").graphicsLayer { alpha = tagline; translationY = (1f - tagline) * 8.dp.toPx() })
            // Deliberate branding motion, not a claim of data-loading progress.
            Canvas(Modifier.padding(top = 8.dp).width(72.dp).height(2.dp)
                .graphicsLayer { alpha = journey * (1f - exit) }) {
                drawRoundRect(accent.copy(alpha = .14f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height))
                val width = size.width * .22f
                val x = (size.width - width) * journey
                drawRoundRect(accent.copy(alpha = .65f), topLeft = Offset(x, 0f),
                    size = Size(width, size.height), cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height))
            }
            if (waiting) {
                LinearProgressIndicator(Modifier.padding(top = 14.dp).width(72.dp).height(2.dp)
                    .testTag("startup-session-loading"), color = accent, trackColor = accent.copy(alpha = .12f))
                Text("Restoring your session", style = MaterialTheme.typography.labelSmall,
                    color = textInk.copy(alpha = .6f))
            }
        }
    }
}
