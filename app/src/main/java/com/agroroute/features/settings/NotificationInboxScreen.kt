package com.agroroute.features.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.agroroute.core.navigation.Screen
import com.github.thelacspace.icons.Lucide

/** Shared Farmer/Supplier history, backed by the same account-scoped inbox as the bell. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationInboxScreen(navController: NavController) {
    AccountNotificationInbox(navController) { state, refresh, open, markRead ->
        var unreadOnly by remember { mutableStateOf(false) }
        val unreadCount = state.items.count { !it.isRead }
        val visible = state.items.filter { !unreadOnly || !it.isRead }.sortedByDescending { it.createdAt }
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Notifications", maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Lucide.ArrowLeft, "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = refresh, enabled = !state.isLoading) {
                            Icon(Lucide.RefreshCw, "Refresh notifications")
                        }
                        IconButton(onClick = { navController.navigate(Screen.NotificationSettings.route) }) {
                            Icon(Lucide.Settings, "Notification settings")
                        }
                    }
                )
            }
        ) { padding ->
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "filters") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        com.agroroute.core.components.PremiumHero {
                            Icon(Lucide.Bell, null, Modifier.size(28.dp))
                            Text("Your field updates", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Text("Stay updated on orders, requests and messages.", style = MaterialTheme.typography.bodyMedium)
                        }
                        Text(when {
                            state.isLoading && state.items.isEmpty() -> "Checking for updates"
                            state.error != null && state.items.isEmpty() -> "Updates unavailable"
                            unreadCount == 0 -> "All caught up"
                            else -> "$unreadCount unread"
                        },
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = !unreadOnly, onClick = { unreadOnly = false },
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
                                label = { Text("All (${state.items.size})") })
                            FilterChip(selected = unreadOnly, onClick = { unreadOnly = true },
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
                                label = { Text("Unread ($unreadCount)") })
                        }
                    }
                }
                if (state.isLoading) item(key = "loading") { NotificationLoading() }
                if (state.error != null) item(key = "retry") { NotificationRetry(refresh) }
                if (!state.isLoading && state.error == null && visible.isEmpty()) {
                    item(key = "empty") { NotificationEmpty(unreadOnly) }
                }
                visible.groupBy { notificationDay(it.createdAt) }.forEach { (day, notifications) ->
                    item(key = "day:${day.name}") {
                        Text(day.label, style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                    }
                    items(notifications, key = { "notification:${it.id}" }) { item ->
                        Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 1.dp,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))) {
                            NotificationRow(item, onClick = { open(item) }, markRead = { markRead(item) })
                        }
                    }
                }
            }
        }
    }
}
