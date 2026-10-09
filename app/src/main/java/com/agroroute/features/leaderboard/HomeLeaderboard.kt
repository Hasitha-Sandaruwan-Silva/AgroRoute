package com.agroroute.features.leaderboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agroroute.core.components.*
import com.agroroute.core.theme.*
import com.github.thelacspace.icons.Lucide

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
                Text("Completed orders earn community points.", style = MaterialTheme.typography.bodySmall)
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
