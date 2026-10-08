package com.agroroute.features.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.agroroute.core.components.AgroSnackbarHost
import com.agroroute.core.components.AppCard
import com.agroroute.core.components.EmptyState
import com.agroroute.core.components.ErrorSnackbarEffect
import com.agroroute.core.components.ListSkeleton
import com.agroroute.core.navigation.Screen
import com.github.thelacspace.icons.Lucide
import java.text.DateFormat
import java.util.Date

@Composable
fun NotificationInboxScreen(
    navController: NavController,
    viewModel: NotificationInboxViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var unreadOnly by remember { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.loadInbox() }
    ErrorSnackbarEffect(uiState.error, snackbarHostState) { viewModel.clearError() }

    val unreadCount = uiState.items.count { !it.isRead }
    val visibleItems = uiState.items.filter { !unreadOnly || !it.isRead }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Notifications",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (unreadCount == 0) "All caught up" else "$unreadCount unread",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = viewModel::loadInbox, enabled = !uiState.isLoading) {
                    Icon(Lucide.RefreshCw, contentDescription = "Refresh notifications")
                }
                TextButton(onClick = { navController.navigate(Screen.NotificationSettings.route) }) {
                    Text("Settings")
                }
            }

            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = !unreadOnly,
                    onClick = { unreadOnly = false },
                    label = { Text("All (${uiState.items.size})") }
                )
                FilterChip(
                    selected = unreadOnly,
                    onClick = { unreadOnly = true },
                    label = { Text("Unread ($unreadCount)") }
                )
            }

            when {
                uiState.isLoading && uiState.items.isEmpty() -> ListSkeleton()
                uiState.error != null && uiState.items.isEmpty() -> EmptyState(
                    icon = Lucide.Bell,
                    title = "Notifications unavailable",
                    subtitle = "Check your connection and try again"
                )
                visibleItems.isEmpty() -> EmptyState(
                    icon = Lucide.Bell,
                    title = if (unreadOnly) "No unread notifications" else "No notifications yet",
                    subtitle = if (unreadOnly) "New order, bid and harvest updates will appear here." else "Relevant updates for your account will appear here."
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(visibleItems, key = { it.id }) { item ->
                        AppCard(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                viewModel.openNotification(item) { route ->
                                    navController.navigate(route)
                                }
                            }
                        ) {
                            Row(verticalAlignment = Alignment.Top) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = if (item.isRead) FontWeight.Medium else FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(Modifier.height(5.dp))
                                    Text(
                                        text = item.body,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (!item.isRead) {
                                    Box(
                                        modifier = Modifier
                                            .padding(start = 10.dp, top = 5.dp)
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                                        .format(Date(item.createdAt)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                if (!item.isRead) {
                                    TextButton(onClick = { viewModel.markRead(item.id) }) {
                                        Text("Mark read")
                                    }
                                }
                                TextButton(
                                    onClick = {
                                        viewModel.openNotification(item) { route ->
                                            navController.navigate(route)
                                        }
                                    }
                                ) {
                                    Text("Open")
                                }
                            }
                        }
                    }
                }
            }
        }
        AgroSnackbarHost(
            snackbarHostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
