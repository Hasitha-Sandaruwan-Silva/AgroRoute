package com.agroroute.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agroroute.core.model.UserRole
import com.agroroute.core.theme.PillShape

@Composable
fun RoleBadge(
    role: UserRole,
    modifier: Modifier = Modifier
) {
    val (label, containerColor, contentColor) = when (role) {
        UserRole.FARMER -> Triple(
            "🌾 Farmer",
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
        UserRole.SUPPLIER -> Triple(
            "🏪 Supplier",
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer
        )
    }

    Text(
        text = label,
        color = contentColor,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .clip(PillShape)
            .background(containerColor)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    )
}
