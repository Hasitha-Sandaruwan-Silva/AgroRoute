package com.agroroute.features.welcome

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agroroute.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val PrimaryGreen = Color(0xFF2E7D32)
private val DarkGreenText = Color(0xFF14532D)

/**
 * Super Dramatic & Unmissable Animated Logo Header
 */
@Composable
fun AnimatedLogoHeader(
    modifier: Modifier = Modifier
) {
    // 🌟 1. Dramatic Entrance Animation States
    val logoScale = remember { Animatable(0.1f) }
    val logoRotate = remember { Animatable(-360f) } // Full 360-degree Spin!
    val logoAlpha = remember { Animatable(0f) }

    val titleScale = remember { Animatable(0.3f) }
    val titleAlpha = remember { Animatable(0f) }
    val titleOffsetY = remember { Animatable(40f) }

    val taglineAlpha = remember { Animatable(0f) }
    val taglineOffsetY = remember { Animatable(25f) }

    // 🌟 2. Continuous Breathing/Pulse Effect (After entrance)
    val infiniteTransition = rememberInfiniteTransition(label = "pulseTransition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    LaunchedEffect(Unit) {
        // Step A: Logo 360 Spin + High Bouncy Scale
        launch {
            logoAlpha.animateTo(1f, animationSpec = tween(350))
        }
        launch {
            logoRotate.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioHighBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
        launch {
            logoScale.animateTo(
                targetValue = 1.18f, // Bounce Overshoot
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioHighBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
            logoScale.animateTo(1.0f, animationSpec = tween(250))
        }

        // Step B: App Title Zoom-Pop & Slide
        delay(300)
        launch {
            titleAlpha.animateTo(1f, animationSpec = tween(350))
        }
        launch {
            titleScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
            )
        }
        launch {
            titleOffsetY.animateTo(0f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }

        // Step C: Tagline Slide Up
        delay(180)
        launch {
            taglineAlpha.animateTo(1f, animationSpec = tween(350))
        }
        launch {
            taglineOffsetY.animateTo(0f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
    ) {
        // 🌟 360-Degree Spinning & Bouncing Logo with Continuous Pulse
        Image(
            painter = painterResource(id = R.drawable.agroroute1),
            contentDescription = "AgroRoute Logo",
            modifier = Modifier
                .scale(logoScale.value * pulseScale) // Bouncy Entrance + Breathing Pulse
                .rotate(logoRotate.value)            // Full -360° Spin
                .alpha(logoAlpha.value)
                .size(105.dp)
        )

        Spacer(Modifier.height(10.dp))

        // 🌟 Zoom-Pop Animated App Title
        Text(
            text = "AgroRoute",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.8.sp
            ),
            color = DarkGreenText,
            modifier = Modifier
                .scale(titleScale.value)
                .offset(y = titleOffsetY.value.dp)
                .alpha(titleAlpha.value)
        )

        // 🌟 Animated Tagline
        Text(
            text = "From farm to market, fairly priced",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.2.sp
            ),
            color = PrimaryGreen,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 2.dp)
                .offset(y = taglineOffsetY.value.dp)
                .alpha(taglineAlpha.value)
        )
    }
}