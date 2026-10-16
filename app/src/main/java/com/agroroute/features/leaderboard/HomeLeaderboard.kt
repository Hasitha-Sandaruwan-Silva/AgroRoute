package com.agroroute.features.leaderboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import com.agroroute.core.model.UserProfile
import com.agroroute.core.runtime.LocalMutationAllowed
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.agroroute.core.runtime.appViewModel as hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agroroute.core.components.*
import com.agroroute.core.theme.*
import com.github.thelacspace.icons.Lucide

@Composable
fun CommunityLeaderboards(onViewAll: (String) -> Unit, onParticipant: (String) -> Unit,
    viewModel: LeaderboardViewModel = hiltViewModel(key = "community-leaders")) {
    val state by viewModel.communityState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.retryCommunity() }
    CommunityLeadersContent(state, onViewAll, onParticipant, viewModel::retryCommunity)
}

@Composable
internal fun CommunityLeadersContent(state: CommunityLeadersState, onViewAll: (String) -> Unit,
    onParticipant: (String) -> Unit, onRetry: () -> Unit = {}) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Community Leaders", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        LeaderboardPreview("FARMER", state.farmers, state.loading, state.error, { onViewAll("FARMER") }, onParticipant, onRetry)
        LeaderboardPreview("SUPPLIER", state.suppliers, state.loading, state.error, { onViewAll("SUPPLIER") }, onParticipant, onRetry)
    }
}

@Composable
private fun LeaderboardPreview(role: String, users: List<UserProfile>, loading: Boolean, error: String?,
    onViewAll: () -> Unit, onParticipant: (String) -> Unit, onRetry: () -> Unit) {
    val navigationAllowed = LocalMutationAllowed.current
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (role == "FARMER") "Top Farmers" else "Top Suppliers", Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            TextButton(onClick = onViewAll) { Text(if (role == "FARMER") "View All Farmers" else "View All Suppliers", style = MaterialTheme.typography.labelMedium) }
        }
        when {
            loading && users.isEmpty() -> ShimmerBox(Modifier.fillMaxWidth().height(64.dp))
            error != null && users.isEmpty() -> {
                Text("Rankings are unavailable right now", style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = onRetry) { Text("Try again") }
            }
            users.isEmpty() -> Text("No rankings yet", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            else -> users.take(3).forEachIndexed { index, user ->
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .clickable(enabled = navigationAllowed && user.userId.isNotBlank()) { onParticipant(user.userId) }
                    .padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${index + 1}", Modifier.width(16.dp), style = MaterialTheme.typography.labelMedium,
                        color = if (index == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    UserProfileAvatar(user.displayName, user.profileImageUrl, size = 36.dp)
                    Text(user.displayName.ifBlank { "Community member" }, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${user.points} pts", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (error != null && users.isNotEmpty()) Text("Showing saved rankings", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun HomeLeaderboard(role: String, onViewAll: () -> Unit, viewModel: LeaderboardViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.loadLeaderboard(role) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(if (role == "FARMER") "Top growers" else "Top suppliers",
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Community leaderboard", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Lucide.Trophy, null, tint = LocalAgroExtendedColors.current.goldText, modifier = Modifier.size(22.dp))
        }
        when {
            state.isLoading && state.users.isEmpty() -> ShimmerBox(Modifier.fillMaxWidth().height(130.dp))
            state.error != null && state.users.isEmpty() -> AppCard {
                Text("Rankings are unavailable right now", style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = { viewModel.loadLeaderboard(role) }) { Text("Try again") }
            }
            !state.isLoading && state.users.isEmpty() -> AppCard {
                Text("No rankings yet", style = MaterialTheme.typography.titleSmall)
                Text(if (role == "FARMER") "Acceptance +1 · Handover +3 · Delivery +6."
                    else "Pickup +4 · Delivery +6. Requests earn no points.", style = MaterialTheme.typography.bodySmall)
            }
            else -> AnimatedVisibility(visible = state.users.isNotEmpty(), enter = fadeIn()) {
                AppCard {
                    state.users.take(3).forEachIndexed { index, user ->
                        val champion = index == 0
                        Row(
                            Modifier.fillMaxWidth().then(if (champion) Modifier.background(
                                LocalAgroExtendedColors.current.goldSoft, MaterialTheme.shapes.medium) else Modifier)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("${index + 1}", style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold, color = if (champion) LocalAgroExtendedColors.current.goldText else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(14.dp))
                            UserProfileAvatar(user.displayName, user.profileImageUrl,
                                size = 36.dp, selected = champion)
                            Text(user.displayName.ifBlank { "Community member" }, Modifier.weight(1f),
                                style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                fontWeight = if (champion) FontWeight.Bold else FontWeight.Medium)
                            Text("${user.points} pts", style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (state.error != null) Text("Showing saved rankings", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        TextButton(onClick = onViewAll, modifier = Modifier.fillMaxWidth()) {
            Text("View Leaderboard")
            Spacer(Modifier.width(8.dp))
            Icon(Lucide.ArrowRight, null, Modifier.size(18.dp))
        }
    }
}
