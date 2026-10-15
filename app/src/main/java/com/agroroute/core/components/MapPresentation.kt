package com.agroroute.core.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.thelacspace.icons.Lucide

/** Shared map chrome; location acquisition and permission decisions belong to callers. */
@Composable
internal fun MapLocationControl(
    locating: Boolean,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(onClick = onClick, enabled = !locating, shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        shadowElevation = 3.dp, modifier = modifier.size(48.dp)) {
        Box(contentAlignment = Alignment.Center) {
            if (locating) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
            else Icon(Lucide.MyLocation, description, tint = MaterialTheme.colorScheme.primary)
        }
    }
}
