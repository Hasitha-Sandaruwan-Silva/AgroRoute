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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
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
    val animationFinished by remember { derivedStateOf { timeline.value >= 1f } }
    val nativePalette by remember { derivedStateOf { timeline.value < .08f } }
    LaunchedEffect(nativeSplashFinished) {
        if (nativeSplashFinished && !completed) timeline.animateTo(1f, tween(2600, easing = LinearEasing))
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
            com.agroroute.core.theme.StartupStatusBar(if (nativePalette) initialDarkTheme else darkTheme)
            AnimatedStartupArtwork({ timeline.value * 2600f }, { exit.value }, darkTheme,
                animationFinished && !sessionResolved, initialDarkTheme)
        }
    }
}

/** Confine frame-by-frame reads to the overlay, outside the real app's composition scope. */
@Composable
private fun AnimatedStartupArtwork(time: () -> Float, exit: () -> Float, dark: Boolean, waiting: Boolean, initialDark: Boolean) {
    StartupArtwork(time(), exit(), dark, waiting, initialDark)
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
    val road = phase(time, 1200f, 1750f)
    val arrow = phase(time, 1250f, 1750f)
    val wordmark = phase(time, 1450f, 2050f)
    val tagline = phase(time, 1650f, 2200f)
    val journey = phase(time, 1950f, 2600f)
    val gold = Color(0xFFD5AC54)
    val routeGlow = if (dark) Color(0xFF58AC83) else Color(0xFF81A977)
    val reveal = phase(time, 650f, 1200f)
    val settle = phase(time, 1150f, 1450f)
    val drift = phase(time, 200f, 2600f)
    val destinationPulse = phase(time, 1500f, 1950f)
    // Reuse native paths instead of allocating geometry on every animation frame.
    val route = remember { Path() }
    val drawn = remember { Path() }
    val trail = remember { Path() }
    val measure = remember { PathMeasure() }
    val destinationBackground = MaterialTheme.colorScheme.background
    val exitBackground = lerp(background, destinationBackground, exit * .4f)
    BoxWithConstraints(Modifier.fillMaxSize().testTag("startup-intro")
        .graphicsLayer { alpha = 1f - exit }.background(exitBackground)
        .pointerInput(Unit) { awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false).consume()
            do {
                val event = awaitPointerEvent()
                event.changes.forEach { it.consume() }
            } while (event.changes.any { it.pressed })
        } }) {
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2 - 40.dp.toPx() * reveal)
            val atmosphere = phase(time, 200f, 600f)
            drawCircle(Brush.radialGradient(listOf(
                (if (dark) Color(0xFF287052) else Color(0xFFD9E7C8)).copy(alpha = .42f * atmosphere * (1f - exit)),
                Color.Transparent), center, size.minDimension * .75f),
                radius = size.minDimension * .75f, center = center)
            val alpha = phase(time, 0f, 650f) * .055f
            // Broad botanical silhouettes drift in opposite directions, behind the mark.
            rotate(-32f + drift * 2f, Offset(size.width * .12f, size.height * .28f)) {
                drawOval(accent.copy(alpha = alpha), Offset(-size.width * .32f + 12.dp.toPx() * drift, size.height * .07f),
                    Size(size.width * .7f, size.height * .39f))
            }
            rotate(35f - drift * 2f, Offset(size.width * .9f, size.height * .73f)) {
                drawOval(accent.copy(alpha = alpha * .7f), Offset(size.width * .73f - 10.dp.toPx() * drift, size.height * .51f),
                    Size(size.width * .5f, size.height * .4f))
            }
            // Deterministic positions and distinct drift speeds keep the atmosphere calm.
            repeat(18) { i ->
                val x = size.width * ((i * 37 + 11) % 97) / 97f +
                    (if (i % 2 == 0) 1 else -1) * 5.dp.toPx() * drift
                val y = size.height * ((i * 53 + 7) % 101) / 101f -
                    (9f + i % 4 * 3f).dp.toPx() * drift
                val particle = if (i % 4 == 0) gold else accent
                if (i % 3 == 0) drawCircle(particle.copy(alpha = .035f * atmosphere), 4.dp.toPx(), Offset(x, y))
                drawCircle(particle.copy(alpha = .23f * atmosphere),
                    (if (i % 3 == 0) 1.5f else .8f).dp.toPx(), Offset(x, y))
            }
            route.reset()
            route.apply {
                moveTo(-12.dp.toPx(), size.height * .68f)
                cubicTo(size.width * .25f, size.height * .68f, size.width * .16f,
                    center.y + 35.dp.toPx(), size.width * .34f, center.y + 35.dp.toPx())
                quadraticTo(size.width * .43f, center.y + 35.dp.toPx(), center.x, center.y)
            }
            measure.setPath(route, false)
            drawn.reset()
            trail.reset()
            val routeProgress = phase(time, 300f, 1150f)
            measure.getSegment(0f, measure.length * routeProgress, drawn, true)
            measure.getSegment(measure.length * (routeProgress - .18f).coerceAtLeast(0f),
                measure.length * routeProgress, trail, true)
            val routeAlpha = 1f - phase(time, 1350f, 1850f)
            drawPath(drawn, routeGlow.copy(alpha = .08f * routeAlpha), style = Stroke(12.dp.toPx(), cap = StrokeCap.Round))
            drawPath(drawn, routeGlow.copy(alpha = .16f * routeAlpha), style = Stroke(5.dp.toPx(), cap = StrokeCap.Round))
            drawPath(drawn, (if (dark) Color(0xFFC4DCC3) else ink).copy(alpha = .72f * routeAlpha),
                style = Stroke(1.7.dp.toPx(), cap = StrokeCap.Round))
            drawPath(trail, gold.copy(alpha = .65f * routeAlpha), style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
            if (routeProgress > 0f) {
                val point = measure.getPosition(measure.length * routeProgress)
                drawCircle(Brush.radialGradient(listOf(gold.copy(alpha = .3f * routeAlpha), Color.Transparent),
                    point, 15.dp.toPx()), 15.dp.toPx(), point)
                drawCircle(gold.copy(alpha = routeAlpha), 3.dp.toPx(), point)
                drawCircle(Color(0xFFFFF3D3).copy(alpha = routeAlpha), 1.25.dp.toPx(), point)
            }
            repeat(3) { i ->
                val start = 850f + i * 260f
                val ripple = phase(time, start, start + 1100f)
                val opacity = ( .23f - i * .035f) * (1f - ripple) * phase(time, start, start + 150f)
                val radius = (62f + i * 5f + (91f + i * 9f) * ripple).dp.toPx()
                drawCircle(routeGlow.copy(alpha = opacity * .22f), radius, center, style = Stroke(5.dp.toPx()))
                drawCircle((if (i == 1) gold else routeGlow).copy(alpha = opacity),
                    radius, center, style = Stroke((1.3f - i * .2f).dp.toPx()))
            }
        }
        // Same 288dp canvas and exact source-pixel registration as the native splash.
        // Its position is unchanged at handoff; only then does it settle above the name.
        val iconSize = minOf(288.dp, maxHeight * .48f)
        Box(Modifier.size(iconSize).align(Alignment.Center).testTag("startup-official-logo").graphicsLayer {
            // Keep the native mark registered at handoff, then softly reveal its emphasis.
            alpha = (1f - .42f * phase(time, 0f, 350f) * (1f - reveal)) * (1f - .22f * exit)
            scaleX = (1f - .055f * phase(time, 250f, 650f) + .073f * reveal - .018f * settle) * (1f - .035f * exit)
            scaleY = scaleX
            translationY = -43.dp.toPx() * reveal + 3.dp.toPx() * settle
        }) {
            Canvas(Modifier.fillMaxSize()) {
                val glowCenter = Offset(size.width * .53f + 2.dp.toPx() * drift, size.height * .5f)
                val radius = size.width * (.32f + .018f * drift)
                drawCircle(Brush.radialGradient(listOf(routeGlow.copy(alpha = .12f * reveal), Color.Transparent),
                    glowCenter, radius), radius, glowCenter)
            }
            Image(painterResource(R.drawable.official_startup_logo), null,
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
                Modifier.fillMaxSize().drawWithContent {
                    // Travel stays clipped to the original arrow pixels: the mark never detaches.
                    if (arrow > 0f && arrow < 1f) {
                        val leading = size.width * (.5f + .4f * arrow)
                        clipRect(left = (leading - size.width * .09f).coerceAtLeast(0f), right = leading) {
                            this@drawWithContent.drawContent()
                        }
                    }
                }.graphicsLayer { alpha = .42f },
                colorFilter = ColorFilter.tint(Color(0xFFFFE4A0)), contentScale = ContentScale.Fit)
            Image(painterResource(R.drawable.official_startup_arrow), null,
                Modifier.fillMaxSize().graphicsLayer {
                    alpha = .3f * kotlin.math.sin(destinationPulse * Math.PI).toFloat()
                }, colorFilter = ColorFilter.tint(Color(0xFFFFE4A0)), contentScale = ContentScale.Fit)
        }
        Column(Modifier.align(Alignment.TopCenter).offset(y = maxHeight / 2 + 64.dp)
            .fillMaxWidth().padding(horizontal = 24.dp)
            .graphicsLayer { scaleX = 1f - .025f * exit; scaleY = scaleX; alpha = 1f - .3f * exit },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("AgroRoute", color = textInk, style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold, letterSpacing = (1.1f - wordmark * 1.4f).sp,
                textAlign = TextAlign.Center, modifier = Modifier.testTag("startup-name").graphicsLayer {
                    alpha = wordmark; translationY = (1f - wordmark) * 16.dp.toPx()
                })
            Text("From Harvest to Destination", color = textInk.copy(alpha = .68f),
                style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
                modifier = Modifier.testTag("startup-tagline").graphicsLayer { alpha = tagline; translationY = (1f - tagline) * 8.dp.toPx() })
            // Deliberate branding motion, not a claim of data-loading progress.
            Canvas(Modifier.padding(top = 14.dp).width(136.dp).height(16.dp)
                .graphicsLayer { alpha = phase(time, 1950f, 2100f) * (1f - exit) }) {
                val start = Offset(8.dp.toPx(), size.height / 2)
                val end = Offset(size.width - 8.dp.toPx(), size.height / 2)
                val point = Offset(start.x + (end.x - start.x) * journey, start.y)
                drawLine(accent.copy(alpha = .16f), start, end, 2.dp.toPx(), StrokeCap.Round)
                if (journey > 0f) drawLine(Brush.linearGradient(listOf(accent, gold), start, end),
                    start, point, 2.dp.toPx(), StrokeCap.Round)
                drawCircle(gold.copy(alpha = .23f), 3.dp.toPx(), end, style = Stroke(1.dp.toPx()))
                drawCircle(Brush.radialGradient(listOf(gold.copy(alpha = .4f), Color.Transparent),
                    point, 8.dp.toPx()), 8.dp.toPx(), point)
                drawCircle(gold, 2.5.dp.toPx(), point)
                drawCircle(Color(0xFFFFF3D3), 1.dp.toPx(), point)
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
