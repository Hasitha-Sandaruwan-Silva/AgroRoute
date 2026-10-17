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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.vector.ImageVector

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
        tonalElevation = 3.dp, shadowElevation = 6.dp, modifier = modifier.size(48.dp)) {
        Box(contentAlignment = Alignment.Center) {
            if (locating) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
            else Icon(Lucide.MyLocation, description, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

/** Informational chrome only: callers retain every loading/error/permission decision. */
@Composable
internal fun MapStatusMessage(title: String, detail: String, icon: ImageVector = Lucide.MapPin,
    modifier: Modifier = Modifier, error: Boolean = false) {
    Surface(modifier = modifier, shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
        color = if (error) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = if (error) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        shadowElevation = 2.dp) {
        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
            Icon(icon, null, Modifier.size(20.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Text(detail, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
