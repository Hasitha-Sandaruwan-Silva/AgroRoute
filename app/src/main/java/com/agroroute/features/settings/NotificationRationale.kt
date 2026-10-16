package com.agroroute.features.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agroroute.core.components.PremiumHero
import com.github.thelacspace.icons.Lucide

/** App-owned explanation; Android retains ownership of the actual permission dialog. */
@Composable
fun NotificationRationale(onEnable: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.large,
        title = { Text("Stay in the loop", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                PremiumHero {
                    Icon(Lucide.Bell, null, Modifier.size(28.dp))
                    Text("A little closer to every harvest", style = MaterialTheme.typography.titleMedium)
                }
                Text("Get alerts for new order requests, pickup and delivery updates, and new messages.")
                Text("You can manage alerts anytime in notification settings.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { TextButton(onClick = onEnable) { Text("Enable notifications") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Maybe later") } }
    )
}
