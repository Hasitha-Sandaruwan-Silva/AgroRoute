package com.agroroute.core.widgets

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.agroroute.core.theme.AgroElevation
import com.agroroute.core.theme.AgroRadius
import com.agroroute.core.theme.AgroSize
import com.agroroute.core.theme.AgroSpacing
import com.agroroute.core.theme.LocalAgroExtendedColors

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    elevation: Dp = 1.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val extended = LocalAgroExtendedColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && onClick != null) 0.98f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "cardPress"
    )

    val cardModifier = modifier
        .scale(scale)
        .then(
            if (onClick != null) {
                Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
            } else Modifier
        )

    Card(
        modifier = cardModifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = elevation,
            pressedElevation = if (onClick != null) AgroElevation.raised else elevation
        ),
        border = BorderStroke(width = 1.dp, color = extended.cardBorder)
    ) {
        Column(modifier = Modifier.padding(AgroSpacing.cardPadding), content = content)
    }
}

/**
 * The one tinted rounded container every icon in the app sits in, so icon
 * framing reads identically on cards, settings rows, empty states and heroes.
 */
@Composable
internal fun AgroIconBox(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    boxSize: Dp = AgroSize.boxMd,
    iconSize: Dp = AgroSize.iconLg,
    corner: Dp = AgroRadius.iconMd,
    containerColor: Color = LocalAgroExtendedColors.current.iconContainer,
    contentColor: Color = LocalAgroExtendedColors.current.iconOnContainer,
    contentDescription: String? = null
) {
    Box(
        modifier = modifier
            .size(boxSize)
            .clip(corner.let { androidx.compose.foundation.shape.RoundedCornerShape(it) })
            .background(containerColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = contentColor,
            modifier = Modifier.size(iconSize)
        )
    }
}

/** Label-plus-value pair used across detail cards for a consistent rhythm. */
@Composable
internal fun AgroDetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = modifier.padding(vertical = AgroSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AgroSpacing.md)
    ) {
        if (icon != null) {
            AgroIconBox(
                icon = icon,
                boxSize = AgroSize.boxSm,
                iconSize = AgroSize.iconSm,
                corner = AgroRadius.iconSm
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = valueColor
            )
        }
    }
}

/** Horizontally scrollable chip strip so filter rows never wrap awkwardly. */
@Composable
internal fun AgroChipRow(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(AgroSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}
