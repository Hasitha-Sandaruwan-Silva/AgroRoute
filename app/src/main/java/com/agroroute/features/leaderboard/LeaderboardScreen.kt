package com.agroroute.features.leaderboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agroroute.core.theme.ForestDeep
import com.agroroute.core.theme.ForestMid
import com.agroroute.core.theme.ForestSoft
import com.agroroute.core.theme.LeafAccent
import com.agroroute.core.components.EmptyState
import com.agroroute.core.components.ListSkeleton
import com.agroroute.core.components.UserProfileAvatar
import com.github.thelacspace.icons.Lucide
import java.util.Locale

/**
 * Leaderboard screen showing farmers or suppliers ranked by points.
 */
@Composable
fun LeaderboardScreen(
    role: String, // "FARMER" or "SUPPLIER"
    viewModel: LeaderboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(role) {
        viewModel.loadLeaderboard(role)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when {
            uiState.isLoading && uiState.items.isEmpty() -> ListSkeleton(count = 5)
            uiState.error != null && uiState.items.isEmpty() -> Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                EmptyState(
                    icon = Lucide.Award,
                    title = "Leaderboard unavailable",
                    subtitle = uiState.error ?: "Check your connection and try again"
                )
                TextButton(onClick = { viewModel.clearError(); viewModel.loadLeaderboard(role) }) {
                    Text("Try again")
                }
            }
            uiState.items.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    icon = Lucide.Award,
                    title = "No rankings yet",
                    subtitle = "Complete orders to earn points and appear on the leaderboard"
                )
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    LeaderboardHeader(role = role)
                    uiState.error?.let { error ->
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "Showing saved rankings. Refresh failed: $error",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    LeaderboardList(items = uiState.items)
                }
            }
        }
    }
}

@Composable
private fun LeaderboardHeader(role: String) {
    val title = if (role == "FARMER") "Farmer Leaderboard" else "Supplier Leaderboard"
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(ForestDeep, ForestMid, ForestSoft)
                )
            )
            .padding(22.dp)
    ) {
        Icon(
            imageVector = Lucide.Trophy,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.08f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(88.dp)
        )
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Ranked by total points earned from completed orders",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun LeaderboardList(items: List<LeaderboardUserItem>) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(items, key = { _, item -> item.user.userId }) { index, item ->
            LeaderboardItem(item = item, rank = index + 1)
        }
    }
}

@Composable
private fun LeaderboardItem(item: LeaderboardUserItem, rank: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (rank) {
                1 -> MaterialTheme.colorScheme.primaryContainer
                2 -> MaterialTheme.colorScheme.secondaryContainer
                3 -> MaterialTheme.colorScheme.tertiaryContainer
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank badge
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        when (rank) {
                            1 -> LeafAccent
                            2 -> Color(0xFFC0C0C0) // Silver
                            3 -> Color(0xFFCD7F32) // Bronze
                            else -> MaterialTheme.colorScheme.outline
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = rank.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = when (rank) {
                        1 -> Color.White
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
            }

            Spacer(Modifier.width(16.dp))

            UserProfileAvatar(
                displayName = item.user.displayName,
                imageUrl = item.user.profileImageUrl,
                size = 42.dp
            )

            Spacer(Modifier.width(12.dp))

            // User info and average rating
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.user.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.user.location.ifBlank { "Location not specified" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Lucide.Star,
                        contentDescription = null,
                        tint = LeafAccent,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (item.reviewCount > 0) String.format(Locale.getDefault(), "%.1f (%d)", item.averageRating, item.reviewCount) else "No reviews yet",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Total Points
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${item.user.points} pts",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = LeafAccent
                )
            }
        }
    }
}
