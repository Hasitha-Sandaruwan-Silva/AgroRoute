package com.agroroute.common.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agroroute.common.model.UserRole

/**
 * A small pill-shaped badge that visually identifies a user's [UserRole] (Farmer / Supplier).
 */
@Composable
fun RoleBadge(
    role: UserRole,
    modifier: Modifier = Modifier
) {
    val (label, containerColor, contentColor) = when (role) {
        UserRole.FARMER   -> Triple(
            "Farmer",
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
        UserRole.SUPPLIER -> Triple(
            "Supplier",
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer
        )
    }

    Text(
        text  = label,
        color = contentColor,
        fontSize = 12.sp,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(containerColor)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}
