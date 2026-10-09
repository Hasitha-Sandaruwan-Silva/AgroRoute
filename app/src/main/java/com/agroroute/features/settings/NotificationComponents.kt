package com.agroroute.features.settings

import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.agroroute.core.navigation.Screen
import com.agroroute.core.notification.NotificationInboxItem
import com.agroroute.features.auth.AuthViewModel
import com.github.thelacspace.icons.Lucide
import com.agroroute.core.theme.AgroGradients
import com.agroroute.core.theme.CreamInk
import java.text.DateFormat
import java.util.Date

/** Shared presentation scope; the existing VM/service still own reads and writes.
 * The account-keyed activity scope keeps popup/history read states in sync and
 * prevents another account's cached items from being rendered after a switch.
 */
@Composable
internal fun AccountNotificationInbox(
    navController: NavController,
    content: @Composable (
        state: NotificationInboxUiState,
        refresh: () -> Unit,
        open: (NotificationInboxItem) -> Unit,
        markRead: (NotificationInboxItem) -> Unit
    ) -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) {
        generateSequence(context) { (it as? ContextWrapper)?.baseContext }
            .filterIsInstance<ComponentActivity>().first()
    }
    val auth: AuthViewModel = hiltViewModel(viewModelStoreOwner = activity)
    val authState by auth.uiState.collectAsStateWithLifecycle()
    val user = authState.user.takeIf { authState.sessionResolved }
    if (user == null) {
        content(NotificationInboxUiState(isLoading = !authState.sessionResolved), {}, {}, {})
        return
    }
    key(user.userId) {
        val inbox: NotificationInboxViewModel = hiltViewModel(
            viewModelStoreOwner = activity, key = "notification-inbox:${user.userId}"
        )
        val state by inbox.uiState.collectAsStateWithLifecycle()
        var reading by remember { mutableStateOf<NotificationInboxItem?>(null) }
        LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { inbox.loadInbox() }
        content(
            state,
            inbox::loadInbox,
            { item ->
                if (notificationDestination(item, user.role) != null) {
                    inbox.openNotification(item) { route ->
                        if (auth.uiState.value.user?.userId == user.userId) {
                            navController.navigate(route) { launchSingleTop = true }
                        }
                    }
                } else {
                    // Invalid/missing destination: read the message without navigating.
                    reading = item
                    if (!item.isRead) inbox.markRead(item.id)
                }
            },
            { item -> if (!item.isRead) inbox.markRead(item.id) }
        )
        reading?.let { item ->
            AlertDialog(
                onDismissRequest = { reading = null },
                icon = { Icon(notificationIcon(item.eventType), null) },
                title = { Text(item.title) },
                text = { Text(item.body) },
                confirmButton = { TextButton(onClick = { reading = null }) { Text("Done") } }
            )
        }
    }
}

/** One bell/popover for both role TopBars and existing dashboard bell actions. */
@Composable
fun NotificationBell(
    navController: NavController,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    AccountNotificationInbox(navController) { state, refresh, open, _ ->
        var expanded by remember { mutableStateOf(false) }
        val unreadCount = state.items.count { !it.isRead }
        val badge = notificationBadge(unreadCount)
        val configuration = LocalConfiguration.current
        val popupWidth = (configuration.screenWidthDp - 32).coerceIn(1, 360).dp
        val popupHeight = (configuration.screenHeightDp * 0.65f).dp
        Box(modifier) {
            IconButton(
                onClick = { expanded = true; refresh() },
                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    .semantics {
                        contentDescription = if (unreadCount == 0) "Notifications" else "Notifications, $unreadCount unread"
                    }
            ) {
                BadgedBox(badge = {
                    if (badge != null) Badge { Text(badge) }
                }) {
                    Icon(Lucide.Bell, null, tint = tint, modifier = Modifier.size(22.dp))
                }
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.width(popupWidth).heightIn(max = popupHeight),
                shape = MaterialTheme.shapes.large,
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                shadowElevation = 3.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    Modifier.fillMaxWidth().background(AgroGradients.forest).padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Notifications", color = CreamInk, style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    IconButton(onClick = refresh, enabled = !state.isLoading) {
                        Icon(Lucide.RefreshCw, "Refresh notifications", Modifier.size(20.dp), tint = CreamInk)
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                if (state.isLoading) NotificationLoading()
                if (state.error != null) NotificationRetry(refresh)
                if (!state.isLoading && state.error == null && state.items.isEmpty()) {
                    NotificationEmpty()
                }
                state.items.sortedByDescending { it.createdAt }.take(5).forEach { item ->
                    NotificationRow(item = item, compact = true, onClick = {
                        expanded = false
                        open(item)
                    })
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                }
                TextButton(
                    onClick = {
                        expanded = false
                        navController.navigate(Screen.NotificationInbox.route) { launchSingleTop = true }
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                ) {
                    Text("View all notifications", modifier = Modifier.weight(1f))
                    Icon(Lucide.ArrowRight, null, Modifier.size(18.dp))
                }
            }
        }
    }
}

internal fun notificationIcon(event: String): ImageVector = when (event) {
    "BID_CREATED", "BID_ACCEPTED", "BID_REJECTED", "BID_WITHDRAWN" -> Lucide.Gavel
    "ORDER_REQUEST_CREATED", "ORDER_REQUEST_ACCEPTED", "ORDER_REQUEST_REJECTED", "ORDER_REQUEST_CANCELLED" -> Lucide.Inbox
    "ORDER_IN_TRANSIT" -> Lucide.Truck
    "ORDER_COMPLETED" -> Lucide.CheckCircle
    "ORDER_CANCELLED" -> Lucide.Receipt
    "HARVEST_REMINDER" -> Lucide.Calendar
    "HARVEST_READY", "HARVEST_FAILED", "HARVEST_UPDATE" -> Lucide.Sprout
    else -> Lucide.Bell
}

@Composable
internal fun NotificationRow(
    item: NotificationInboxItem,
    onClick: () -> Unit,
    compact: Boolean = false,
    markRead: (() -> Unit)? = null
) {
    val dateFormat = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT) }
    Column(
        Modifier.fillMaxWidth()
            .background(if (item.isRead) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.primaryContainer) {
                Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                    Icon(notificationIcon(item.eventType), null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(item.title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (item.isRead) FontWeight.Medium else FontWeight.Bold,
                        maxLines = if (compact) 2 else Int.MAX_VALUE, overflow = TextOverflow.Ellipsis)
                    if (!item.isRead) Box(Modifier.size(7.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary)
                        .semantics { contentDescription = "Unread" })
                }
                Spacer(Modifier.height(4.dp))
                Text(item.body, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (compact) 2 else Int.MAX_VALUE, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(8.dp))
                Text(dateFormat.format(Date(item.createdAt)), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (!item.isRead && markRead != null) {
                    TextButton(onClick = markRead, contentPadding = PaddingValues(horizontal = 0.dp)) { Text("Mark read") }
                }
            }
        }
    }
}

@Composable
internal fun NotificationLoading() {
    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        Spacer(Modifier.width(12.dp))
        Text("Loading notifications", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
internal fun NotificationEmpty(unreadOnly: Boolean = false) {
    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Lucide.Bell, null, Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
        Text(if (unreadOnly) "No unread notifications" else "No notifications yet", style = MaterialTheme.typography.titleSmall)
        Text("Updates for your account will appear here.", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
internal fun NotificationRetry(refresh: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text("Couldn't update notifications. Check your connection and try again.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick = refresh) { Text("Retry") }
    }
}
