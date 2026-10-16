package com.agroroute

import android.app.NotificationManager
import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.material3.Text
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.*
import androidx.test.platform.app.InstrumentationRegistry
import com.agroroute.core.model.UserRole
import com.agroroute.core.notification.*
import com.agroroute.core.theme.AgroRouteTheme
import com.agroroute.features.participants.*
import com.agroroute.features.settings.NotificationRow
import com.agroroute.features.settings.notificationDestination
import kotlinx.coroutines.flow.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class ChatNotificationPresentationTest(private val dark: Boolean, private val senderRole: UserRole) {
    @get:Rule val compose = createComposeRule()
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "dark={0},sender={1}")
        fun themes() = listOf(arrayOf(false, UserRole.FARMER), arrayOf(true, UserRole.FARMER),
            arrayOf(false, UserRole.SUPPLIER), arrayOf(true, UserRole.SUPPLIER))
    }
    private val senderId get() = if (senderRole == UserRole.FARMER) "farmer-1" else "supplier-1"
    private val receiverId get() = if (senderRole == UserRole.FARMER) "supplier-1" else "farmer-1"
    private val senderName get() = if (senderRole == UserRole.FARMER) "Nimal" else "Kasun"
    private val pairId get() = conversationIdentity("farmer-1", "supplier-1")

    @Test fun existingInboxRowOpensSameChatPreservesOrderContextAndReturnsToHistory() {
        var read by mutableStateOf(false)
        var sentOrder = ""
        val orderId = "order-1024"
        val message = "Hi, is this order ready?"
        val item = ChatNotification.item(ChatNotification.eventId(pairId, "message-1"), senderId, senderName,
            message, System.currentTimeMillis(), null, orderId)
        val repo = object : ChatRepository {
            override val currentUserId = receiverId
            override fun conversations() = flowOf(emptyList<Conversation>())
            override suspend fun pair(otherId: String): Pair<String, ParticipantIdentity> {
                assertEquals(senderId, otherId)
                return pairId to ParticipantIdentity(senderId, senderName, senderRole)
            }
            override fun messages(conversationId: String): Flow<List<ChatMessage>> {
                assertEquals(pairId, conversationId)
                return flowOf(listOf(ChatMessage("message-1", senderId, message, 1)))
            }
            override suspend fun send(otherId: String, input: String) = Unit
            override suspend fun sendWithContext(otherId: String, input: String, orderId: String) { sentOrder = orderId }
        }
        val vm = ChatViewModel(repo)
        compose.setContent { AgroRouteTheme(darkTheme = dark) {
            val nav = rememberNavController()
            NavHost(nav, startDestination = "inbox") {
                composable("inbox") {
                    NotificationRow(item.copy(readAt = if (read) 1 else null), onClick = {
                        read = true
                        val role = if (senderRole == UserRole.FARMER) UserRole.SUPPLIER else UserRole.FARMER
                        nav.navigate(requireNotNull(notificationDestination(item, role)))
                    })
                }
                composable(ParticipantRoutes.CHAT, arguments = listOf(
                    navArgument("participantId") { type = NavType.StringType },
                    navArgument("orderId") { type = NavType.StringType; defaultValue = "" })) { entry ->
                    ChatScreen(entry.arguments!!.getString("participantId")!!, nav, vm,
                        orderId = entry.arguments!!.getString("orderId").orEmpty())
                }
            }
        } }
        compose.onNodeWithText("New message from $senderName").assertIsDisplayed()
        compose.onNodeWithText("Order #order-1024 \u2022 $message").assertIsDisplayed().performClick()
        compose.onNodeWithText("Regarding Order #order-1024").assertIsDisplayed()
        compose.onNodeWithText(message).assertIsDisplayed()
        compose.onNodeWithText("Reconnect").assertDoesNotExist()
        compose.onNode(hasSetTextAction()).performTextInput("Thanks")
        compose.onNodeWithContentDescription("Send").performClick()
        compose.runOnIdle { assertEquals(orderId, sentOrder) }
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("New message from $senderName").assertIsDisplayed()
        compose.onNodeWithContentDescription("Unread").assertDoesNotExist()
    }
    @Test fun localAndroidNotificationUsesCommittedEventIdentityWithoutDuplicates() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        Assume.assumeTrue("Existing notification permission must be granted", NotificationHelper.areNotificationsEnabled(context))
        val service = NotificationService(context)
        val id = ChatNotification.eventId(pairId, "pixel-fixture-${senderRole.name}-$dark")
        val item = ChatNotification.item(id, senderId, senderName, "Hi, is this order ready?", 1, null)
        try {
            repeat(2) { service.post(NotificationEvent.CHAT_MESSAGE, NotificationEvent.Payload(),
                item.route, item.title, item.body, item.id) }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val posted = manager.activeNotifications.filter { it.id == id.hashCode() }
            assertEquals(1, posted.size)
            assertEquals(item.title, posted.single().notification.extras.getString("android.title"))
            assertEquals(item.body, posted.single().notification.extras.getString("android.text"))
            assertNotNull(posted.single().notification.contentIntent)
        } finally { service.cancelLocalNotification(id) }
    }
}
