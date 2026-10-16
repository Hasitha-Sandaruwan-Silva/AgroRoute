package com.agroroute.features.auth

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.shape.RoundedCornerShape
import com.agroroute.R
import com.agroroute.core.components.LoadingButton
import com.github.thelacspace.icons.Lucide
import com.agroroute.core.theme.CreamInk
import com.agroroute.core.theme.ForestDeep

/** Quiet surface shading shared by the photographic onboarding family. */
@Composable
internal fun AgroAuthBackground(modifier: Modifier = Modifier) {
    val leaf = MaterialTheme.colorScheme.primary
    Canvas(modifier) {
        drawRect(Brush.verticalGradient(listOf(leaf.copy(alpha = .035f), leaf.copy(alpha = 0f))))
    }
}

/** Keep the existing snackbar queue/dismissal contract and friendly message mapping. */
@Composable
internal fun AuthMessageHost(state: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(state, modifier) { data ->
        Snackbar(
            modifier = Modifier.padding(16.dp).semantics { liveRegion = LiveRegionMode.Polite },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.inverseSurface,
            contentColor = MaterialTheme.colorScheme.inverseOnSurface
        ) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Lucide.Info, null, Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.inversePrimary)
                Text(com.agroroute.core.components.presentationError(data.visuals.message),
                    style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/** One entry transition, always composed and interactive; Compose honors duration scale. */
@Composable
internal fun AuthReveal(delayMillis: Int = 0, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val progress by animateFloatAsState(if (entered) 1f else 0f,
        tween(420, delayMillis), label = "authEntry")
    Box(modifier.graphicsLayer {
        alpha = progress
        translationY = (1f - progress) * 12.dp.toPx()
        scaleX = 0.985f + 0.015f * progress
        scaleY = scaleX
    }) { content() }
}

@Composable
internal fun AgroAuthBrand(onPhoto: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
            Image(painterResource(R.drawable.agroroute), "AgroRoute logo", Modifier.padding(6.dp).size(52.dp))
        }
        Column(Modifier.weight(1f)) {
            Text("AgroRoute", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold,
                color = if (onPhoto) CreamInk else MaterialTheme.colorScheme.primary)
            Text("From farm to market", style = MaterialTheme.typography.labelMedium,
                color = if (onPhoto) CreamInk.copy(alpha = .9f) else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** The same original farm photograph and logo treatment as Welcome. */
@Composable
internal fun AgroAuthPhotoHeader() {
    Box(Modifier.fillMaxWidth().height(128.dp).clip(RoundedCornerShape(28.dp))) {
        Image(painterResource(R.drawable.onboarding_farm), null,
            Modifier.matchParentSize(), contentScale = ContentScale.Crop)
        Box(Modifier.matchParentSize().background(Brush.horizontalGradient(
            listOf(ForestDeep.copy(alpha = .84f), ForestDeep.copy(alpha = .22f)))))
        Box(Modifier.align(Alignment.CenterStart).padding(20.dp)) {
            AgroAuthBrand(onPhoto = true)
        }
    }
}

@Composable
internal fun AuthPrimaryButton(text: String, isLoading: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) .985f else 1f, tween(120), label = "authPress")
    AuthReveal(delayMillis = 160) {
        LoadingButton(text, isLoading, onClick,
            modifier.graphicsLayer { scaleX = scale; scaleY = scale }, interactionSource = interaction)
    }
}
