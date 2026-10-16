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
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Community Leaders", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        LeaderboardPreview("FARMER", state.farmers, state.loading, state.error, { onViewAll("FARMER") }, onParticipant, onRetry)
        LeaderboardPreview("SUPPLIER", state.suppliers, state.loading, state.error, { onViewAll("SUPPLIER") }, onParticipant, onRetry)
    }
}

@Composable
private fun LeaderboardPreview(role: String, users: List<UserProfile>, loading: Boolean, error: String?,
    onViewAll: () -> Unit, onParticipant: (String) -> Unit, onRetry: () -> Unit) {
    val navigationAllowed = LocalMutationAllowed.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(if (role == "FARMER") "Top Farmers" else "Top Suppliers",
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Community leaderboard", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Lucide.Trophy, null, tint = LocalAgroExtendedColors.current.goldText, modifier = Modifier.size(22.dp))
        }
        when {
            loading && users.isEmpty() -> ShimmerBox(Modifier.fillMaxWidth().height(130.dp))
            error != null && users.isEmpty() -> AppCard {
                Text("Rankings are unavailable right now", style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = { onRetry() }) { Text("Try again") }
            }
            !loading && users.isEmpty() -> AppCard {
                Text("No rankings yet", style = MaterialTheme.typography.titleSmall)
                Text(if (role == "FARMER") "Acceptance +1 · Handover +3 · Delivery +6."
                    else "Pickup +4 · Delivery +6. Requests earn no points.", style = MaterialTheme.typography.bodySmall)
            }
            else -> AnimatedVisibility(visible = users.isNotEmpty(), enter = fadeIn()) {
                AppCard {
                    users.take(3).forEachIndexed { index, user ->
                        val champion = index == 0
                        Row(
                            Modifier.fillMaxWidth().clickable(enabled = navigationAllowed && user.userId.isNotBlank()) { onParticipant(user.userId) }.then(if (champion) Modifier.background(
                                LocalAgroExtendedColors.current.goldSoft, MaterialTheme.shapes.medium) else Modifier)
                                .padding(horizontal = 10.dp, vertical = if (champion) 14.dp else 10.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("${index + 1}", style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold, color = if (champion) LocalAgroExtendedColors.current.goldText else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(14.dp))
                            UserProfileAvatar(user.displayName, user.profileImageUrl,
                                size = if (champion) 50.dp else if (index < 3) 40.dp else 34.dp, selected = champion)
                            Text(user.displayName.ifBlank { "Community member" }, Modifier.weight(1f),
                                style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis,
                                fontWeight = if (champion) FontWeight.Bold else FontWeight.Medium)
                            Text("${user.points} pts", style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (error != null) Text("Showing saved rankings", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        TextButton(onClick = onViewAll, modifier = Modifier.fillMaxWidth()) {
            Text(if (role == "FARMER") "View All Farmers" else "View All Suppliers")
            Spacer(Modifier.width(8.dp))
            Icon(Lucide.ArrowRight, null, Modifier.size(18.dp))
        }
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
                    state.users.take(5).forEachIndexed { index, user ->
                        val champion = index == 0
                        Row(
                            Modifier.fillMaxWidth().then(if (champion) Modifier.background(
                                LocalAgroExtendedColors.current.goldSoft, MaterialTheme.shapes.medium) else Modifier)
                                .padding(horizontal = 10.dp, vertical = if (champion) 14.dp else 10.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("${index + 1}", style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold, color = if (champion) LocalAgroExtendedColors.current.goldText else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(14.dp))
                            UserProfileAvatar(user.displayName, user.profileImageUrl,
                                size = if (champion) 50.dp else if (index < 3) 40.dp else 34.dp, selected = champion)
                            Text(user.displayName.ifBlank { "Community member" }, Modifier.weight(1f),
                                style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis,
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
