package com.agroroute.core.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import com.github.thelacspace.icons.Lucide
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.agroroute.core.model.UserRole

private data class AccentQuad(
    val accent: androidx.compose.ui.graphics.Color,
    val container: androidx.compose.ui.graphics.Color,
    val onContainer: androidx.compose.ui.graphics.Color,
    val onAccent: androidx.compose.ui.graphics.Color
)

@Composable
fun RoleSelectCard(
    role: UserRole,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (title, subtitle, icon) = when (role) {
        UserRole.FARMER -> Triple(
            "Farmer",
            "Sell your harvest",
            Lucide.Sprout
        )
        UserRole.SUPPLIER -> Triple(
            "Supplier",
            "Buy from farms",
            Lucide.Store
        )
    }
    val (accent, accentContainer, onAccentContainer, onAccent) = when (role) {
        UserRole.FARMER -> AccentQuad(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            MaterialTheme.colorScheme.onPrimary
        )
        UserRole.SUPPLIER -> AccentQuad(
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
            MaterialTheme.colorScheme.onSecondary
        )
    }
    val borderColor by animateColorAsState(
        targetValue = if (selected) accent
        else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
        label = "roleBorder"
    )
    val containerColor by animateColorAsState(
        targetValue = if (selected) accentContainer.copy(alpha = 0.35f)
        else MaterialTheme.colorScheme.surface,
        label = "roleBg"
    )
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "rolePress"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(MaterialTheme.shapes.large)
            .background(containerColor)
            .border(2.dp, borderColor, MaterialTheme.shapes.large)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(MaterialTheme.shapes.large)
                .background(if (selected) accent else accentContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) onAccent else onAccentContainer,
                modifier = Modifier.size(30.dp)
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 14.dp)
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
