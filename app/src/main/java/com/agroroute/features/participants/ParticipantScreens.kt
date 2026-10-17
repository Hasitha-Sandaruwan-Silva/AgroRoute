package com.agroroute.features.participants

import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.agroroute.core.components.AppCard
import com.agroroute.core.components.UserProfileAvatar
import com.agroroute.core.runtime.appViewModel
import com.agroroute.core.runtime.LocalMutationAllowed
import com.github.thelacspace.icons.Lucide
import java.text.DateFormat
import java.util.Date
import java.util.Locale

object ParticipantRoutes {
    const val PROFILE = "participant/{participantId}"
    const val CHAT = "chat/{participantId}?orderId={orderId}"
    const val MESSAGES = "messages"
    fun profile(id: String) = "participant/${Uri.encode(id)}"
    fun chat(id: String, orderId: String = "") = "chat/${Uri.encode(id)}" +
        if (orderId.isBlank()) "" else "?orderId=${Uri.encode(orderId)}"
}

@Composable
fun ParticipantProfileLink(id: String, navController: NavController, label: String = "View profile") {
    if (id.isNotBlank() && LocalMutationAllowed.current) TextButton(onClick = { navController.navigate(ParticipantRoutes.profile(id)) }) { Text(label) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ParticipantPage(title: String, navController: NavController, content: @Composable (PaddingValues) -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text(title) }, navigationIcon = {
        IconButton(onClick = { navController.popBackStack() }) { Icon(Lucide.ArrowLeft, "Back") }
    }) }, containerColor = MaterialTheme.colorScheme.background, content = content)
}

@Composable
fun ParticipantProfileScreen(id: String, navController: NavController, viewModel: ParticipantProfileViewModel = appViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(id) { viewModel.load(id) }
    ParticipantPage("Participant profile", navController) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (state.loading) item { CircularProgressIndicator() }
            state.error?.let { error -> item { RetryCard(error) { viewModel.load(id) } } }
            state.profile?.let { profile ->
                val person = profile.identity
                item {
                    AppCard {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            UserProfileAvatar(person.name, person.imageUrl)
                            Column {
                                Text(person.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text(roleLabel(person), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                person.memberSince?.let { Text("Member since ${java.text.SimpleDateFormat("yyyy", Locale.getDefault()).format(Date(it))}", style = MaterialTheme.typography.bodySmall) }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(profile.averageRating?.let { "\u2605 ${String.format(Locale.getDefault(), "%.1f", it)} | ${profile.reviews.size} reviews" } ?: "No reviews yet")
                        Button(onClick = { navController.navigate(ParticipantRoutes.chat(id)) }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Lucide.MessageCircle, null); Spacer(Modifier.width(8.dp)); Text("Message")
                        }
                    }
                }
                profile.phone?.let { phone -> item {
                    AppCard {
                        Text("Contact", style = MaterialTheme.typography.titleMedium)
                        Text("Mobile number", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(phone)
                        OutlinedButton(onClick = {
                            com.agroroute.core.components.openPhoneDialer(context, phone)
                        }) { Icon(Lucide.Phone, null); Spacer(Modifier.width(8.dp)); Text("Call") }
                    }
                } }
                item {
                    AppCard {
                        Text("At a glance", style = MaterialTheme.typography.titleMedium)
                        Text("Reviews: ${profile.reviews.size}")
                        person.points?.let { Text("AgroRoute Points: $it") }
                        profile.activeHarvests?.let { Text("Published active listings: ${it.size}") }
                    }
                }
                profile.activeHarvests?.takeIf { it.isNotEmpty() }?.let { harvests -> item {
                    AppCard { Text("Active harvests", style = MaterialTheme.typography.titleMedium); harvests.take(3).forEach { Text(it) } }
                } }
                if (profile.reviews.isNotEmpty()) item {
                    AppCard {
                        Text("Rating distribution", style = MaterialTheme.typography.titleMedium)
                        (5 downTo 1).forEach { stars ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("$stars \u2605")
                                LinearProgressIndicator(progress = { profile.ratingCount(stars).toFloat() / profile.reviews.size }, modifier = Modifier.weight(1f))
                                Text("${profile.ratingCount(stars)}")
                            }
                        }
                    }
                }
                item { Text("Reviews", style = MaterialTheme.typography.titleLarge) }
                if (profile.reviews.isEmpty()) item { Text("No reviews yet. Reviews appear after completed trades.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                items(profile.reviews, key = { it.id }) { review ->
                    AppCard {
                        Text(review.reviewerName, fontWeight = FontWeight.Bold)
                        Text("${review.rating} \u2605", color = MaterialTheme.colorScheme.primary)
                        Text(review.text)
                        review.date?.let { Text(dateLabel(it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
            }
        }
    }
}

@Composable
fun MessagesScreen(navController: NavController, viewModel: MessagesViewModel = appViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.load() }
    ParticipantPage("Messages", navController) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item(key = "messages-header") {
                com.agroroute.core.components.PremiumHero {
                    Icon(Lucide.MessageCircle, null, Modifier.size(28.dp))
                    Text("Connected through harvest", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Your conversations with farmers and suppliers.", style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (state.loading) item { CircularProgressIndicator() }
            state.error?.let { error -> item { RetryCard(error, viewModel::load) } }
            if (!state.loading && state.error == null && state.conversations.isEmpty()) item {
                AppCard { Text("No conversations yet", style = MaterialTheme.typography.titleMedium); Text("Open a participant profile and tap Message to start a conversation.") }
            }
            items(state.conversations, key = { it.id }) { conversation ->
                AppCard(onClick = { navController.navigate(ParticipantRoutes.chat(conversation.other.id)) }) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        UserProfileAvatar(conversation.other.name, conversation.other.imageUrl, size = 48.dp)
                        Column(Modifier.weight(1f)) {
                            Text(conversation.other.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(roleLabel(conversation.other), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(conversation.lastMessage, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            conversation.updatedAt?.let { Text(dateLabel(it), style = MaterialTheme.typography.labelSmall) }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(id: String, navController: NavController, viewModel: ChatViewModel = appViewModel(), orderId: String = "") {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    LaunchedEffect(id, orderId) { viewModel.load(id, orderId) }
    androidx.lifecycle.compose.LifecycleResumeEffect(id, state.other?.id, viewModel.currentUserId) {
        val visible = viewModel.currentUserId?.let { it to id }
        if (state.other != null) com.agroroute.core.notification.ChatNotificationVisibility.visible.value = visible
        onPauseOrDispose {
            if (com.agroroute.core.notification.ChatNotificationVisibility.visible.value == visible)
                com.agroroute.core.notification.ChatNotificationVisibility.visible.value = null
        }
    }
    LaunchedEffect(state.messages.lastOrNull()?.id) { if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.lastIndex) }
    Scaffold(modifier = Modifier.imePadding(), topBar = {
        TopAppBar(title = {
            state.other?.let { other ->
                Row(Modifier.clickable { navController.navigate(ParticipantRoutes.profile(id)) }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    UserProfileAvatar(other.name, other.imageUrl, size = 40.dp)
                    Column(Modifier.weight(1f)) { Text(other.name, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(roleLabel(other), style = MaterialTheme.typography.labelSmall) }
                }
            } ?: Text("Chat")
        }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Lucide.ArrowLeft, "Back") } })
    }, bottomBar = {
        Surface(tonalElevation = 2.dp) {
            Column(Modifier.navigationBarsPadding().padding(12.dp)) {
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error); if (state.connectionError) TextButton(onClick = { viewModel.load(id, orderId) }) { Text("Reconnect") } }
                if (state.sending) Text("Sending...", style = MaterialTheme.typography.labelSmall)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = state.draft, onValueChange = viewModel::draft, modifier = Modifier.weight(1f),
                        enabled = !state.loading && !state.connectionError && !state.sending && state.other != null, placeholder = { Text("Type a message...") }, maxLines = 5, shape = RoundedCornerShape(20.dp))
                    IconButton(onClick = viewModel::send, enabled = !state.loading && !state.connectionError && !state.sending && state.draft.isNotBlank() && state.other != null) { Icon(Lucide.Send, "Send") }
                }
            }
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (orderId.isNotBlank()) Text("Regarding Order #${orderId.takeLast(12)}", Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.labelMedium)
            if (state.loading) CircularProgressIndicator(Modifier.padding(16.dp))
            if (!state.loading && !state.connectionError && state.messages.isEmpty()) Text("Start the conversation", Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(state.messages, key = { it.id }) { message ->
                    val mine = message.senderId == viewModel.currentUserId
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
                        Surface(modifier = Modifier.widthIn(max = 300.dp), shape = RoundedCornerShape(18.dp),
                            color = if (mine) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (mine) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant) {
                            Column(Modifier.padding(12.dp)) {
                                Text(message.text)
                                message.sentAt?.let { Text(dateLabel(it), style = MaterialTheme.typography.labelSmall) }
                            }
                        }
                    }
                }
            }
        }
    }
}
@Composable private fun RetryCard(error: String, retry: () -> Unit) { AppCard { Text(error); TextButton(onClick = retry) { Text("Retry") } } }
private fun roleLabel(person: ParticipantIdentity) = person.role.name.lowercase().replaceFirstChar { it.titlecase() }
private fun dateLabel(time: Long) = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(time))
