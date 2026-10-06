package com.agroroute.core.widgets

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    height: Dp = 16.dp,
    cornerRadius: Dp = 8.dp
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translate by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )
    val base = MaterialTheme.colorScheme.surfaceVariant
    val highlight = MaterialTheme.colorScheme.surface
    val brush = Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = Offset(translate - 200f, 0f),
        end = Offset(translate, 0f)
    )
    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(cornerRadius))
            .background(brush)
    )
}

@Composable
fun ListingCardSkeleton(modifier: Modifier = Modifier) {
    AppCard(modifier = modifier) {
        ShimmerBox(modifier = Modifier.fillMaxWidth(), height = 150.dp, cornerRadius = 20.dp)
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .padding(top = 14.dp),
            height = 20.dp,
            cornerRadius = 8.dp
        )
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth(0.4f)
                .padding(top = 10.dp),
            height = 14.dp,
            cornerRadius = 8.dp
        )
    }
}

@Composable
fun ListSkeleton(count: Int = 4) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(count) {
            ListingCardSkeleton()
        }
    }
}

@Composable
fun StatCardsSkeleton() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(2) {
            AppCard(modifier = Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ShimmerBox(
                        modifier = Modifier.size(44.dp),
                        height = 44.dp,
                        cornerRadius = 15.dp
                    )
                    Column {
                        ShimmerBox(modifier = Modifier.fillMaxWidth(), height = 24.dp, cornerRadius = 8.dp)
                        ShimmerBox(
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .padding(top = 6.dp),
                            height = 12.dp,
                            cornerRadius = 8.dp
                        )
                    }
                }
            }
        }
    }
}
