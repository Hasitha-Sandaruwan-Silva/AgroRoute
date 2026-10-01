package com.agroroute.features.welcome

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agroroute.R

private val PrimaryGreen = Color(0xFF2E7D32)
private val DarkText = Color(0xFF14532D)

/**
 * Animated Logo Component that scales up with a spring bounce effect when loaded.
 */
@Composable
fun AnimatedLogoHeader(
    modifier: Modifier = Modifier
) {
    var animateIn by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        animateIn = true
    }

    // Spring Bounce Scale Animation
    val scale by animateFloatAsState(
        targetValue = if (animateIn) 1f else 0.4f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "logoScale"
    )

    // Smooth Fade-In Animation
    val alpha by animateFloatAsState(
        targetValue = if (animateIn) 1f else 0f,
        animationSpec = tween(durationMillis = 700),
        label = "logoAlpha"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        // Logo Image with Animation
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "AgroRoute Logo",
            modifier = Modifier
                .scale(scale)
                .alpha(alpha)
                .size(80.dp)
        )

        Text(
            text = "AgroRoute",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            ),
            color = DarkText,
            modifier = Modifier
                .alpha(alpha)
                .padding(top = 6.dp)
        )

        Text(
            text = "From farm to market, fairly priced",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = PrimaryGreen,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .alpha(alpha)
                .padding(top = 2.dp)
        )
    }
}