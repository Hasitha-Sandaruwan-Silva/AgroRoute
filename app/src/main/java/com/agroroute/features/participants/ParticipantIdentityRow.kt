package com.agroroute.features.participants

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.agroroute.core.components.UserProfileAvatar
import com.agroroute.core.runtime.LocalMutationAllowed
import com.github.thelacspace.icons.Lucide

/** One identity layout across trade screens; the name owns the available width. */
@Composable
fun ParticipantIdentityRow(displayName: String, roleLabel: String, onProfile: () -> Unit,
    modifier: Modifier = Modifier, imageUrl: String? = null, points: Long? = null,
    nameMaxLines: Int = 1) {
    val enabled = LocalMutationAllowed.current
    Row(modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onProfile).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        UserProfileAvatar(displayName.ifBlank { roleLabel }, imageUrl, size = 48.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(displayName.ifBlank { "Participant unavailable" }, style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold, maxLines = nameMaxLines, overflow = TextOverflow.Ellipsis)
            Text(if (points == null) roleLabel else "$roleLabel  |  $points pts",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (enabled) Text("View Profile", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
        if (enabled) Icon(Lucide.ChevronRight, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
