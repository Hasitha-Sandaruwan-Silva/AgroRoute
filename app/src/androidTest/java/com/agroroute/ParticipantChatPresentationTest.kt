package com.agroroute

import androidx.compose.ui.test.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.*
import com.agroroute.core.model.UserRole
import com.agroroute.core.theme.AgroRouteTheme
import com.agroroute.features.participants.*
import kotlinx.coroutines.flow.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class ParticipantChatPresentationTest(private val dark: Boolean, private val targetRole: UserRole) {
    @get:Rule val compose = createComposeRule()
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "dark={0},target={1}")
        fun themes() = listOf(arrayOf(false, UserRole.FARMER), arrayOf(true, UserRole.FARMER),
            arrayOf(false, UserRole.SUPPLIER), arrayOf(true, UserRole.SUPPLIER))
    }
    @Composable private fun ParticipantTestTheme(content: @Composable () -> Unit) {
        AgroRouteTheme(darkTheme = dark) {
            if (targetRole == UserRole.SUPPLIER) com.agroroute.features.farmer.components.FarmerPresentation(content)
            else CompositionLocalProvider(com.agroroute.features.supplier.components.LocalSupplierPresentation provides true, content = content)
        }
    }
    private fun capture(screen: String) {
        val receiver = if (targetRole == UserRole.SUPPLIER) "farmer" else "supplier"
        File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
            "$receiver-${if (dark) "dark" else "light"}-$screen.png").outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
    }
    private val person get() = ParticipantIdentity("other", "Nimal Perera", targetRole, memberSince = 1704067200000, points = 24)
    private val people get() = object : ParticipantRepository {
        override suspend fun identity(id: String) = person
        override fun profile(id: String) = flowOf(ParticipantProfile(person,
            listOf(ParticipantReview("r1", 5, "Fresh produce and clear communication", 1704067200000, "Kasun")),
            if (targetRole == UserRole.FARMER) listOf("Tomatoes") else null))
    }
    private inner class FakeChat : ChatRepository {
        override val currentUserId = "me"
        val rows = MutableStateFlow(listOf(ChatMessage("incoming", "other", "Yes, it is available.", 1)))
        override fun conversations() = flowOf(listOf(Conversation("one", person, "Yes, it is available.", 1)))
        override suspend fun pair(otherId: String) = "one" to person
        override fun messages(conversationId: String) = rows
        override suspend fun send(otherId: String, input: String) {
            rows.value += ChatMessage("sent", "me", messageText(input), 2)
        }
    }
    @Test fun participantProfileOpensChatAndDisplaysBothDirections() {
        val profile = ParticipantProfileViewModel(people)
        val chat = ChatViewModel(FakeChat())
        compose.setContent {
            ParticipantTestTheme {
                val nav = rememberNavController()
                NavHost(nav, startDestination = "profile") {
                    composable("profile") { ParticipantProfileScreen("other", nav, profile) }
                    composable(ParticipantRoutes.CHAT) { ChatScreen("other", nav, chat) }
                }
            }
        }
        compose.onNodeWithText("Nimal Perera").assertIsDisplayed()
        compose.onNodeWithText(if (targetRole == UserRole.FARMER) "Farmer" else "Supplier").assertIsDisplayed()
        compose.onNodeWithText("Edit profile").assertDoesNotExist()
        compose.onNodeWithText("Message").performClick()
        compose.onNodeWithText("Yes, it is available.").assertIsDisplayed()
        compose.onNodeWithContentDescription("Send").assertIsNotEnabled()
        compose.onNode(hasSetTextAction()).performTextInput("Hi, is this harvest available?")
        compose.onNodeWithContentDescription("Send").performClick()
        compose.onNodeWithText("Hi, is this harvest available?").assertIsDisplayed()
        capture("chat")
        compose.onNodeWithContentDescription("Send").assertIsNotEnabled()
    }
    @Test fun realReviewMetricsAndRoleSpecificActivityAreReadOnly() {
        val profile = ParticipantProfileViewModel(people)
        compose.setContent {
            ParticipantTestTheme { ParticipantProfileScreen("other", rememberNavController(), profile) }
        }
        compose.onNodeWithText("Reviews: 1").assertExists()
        compose.onNodeWithText("AgroRoute Points: 24").assertExists()
        if (targetRole == UserRole.FARMER) compose.onNodeWithText("Published active listings: 1").assertExists()
        else compose.onNodeWithText("Published active listings:", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Fresh produce and clear communication").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Kasun").assertIsDisplayed()
        compose.onNodeWithText("Email", substring = true).assertDoesNotExist()
    }
    @Test fun authorizedContactUsesDialerActionWithoutCalling() {
        val repo = object : ParticipantRepository {
            override suspend fun identity(id: String) = person
            override fun profile(id: String) = flowOf(ParticipantProfile(person, emptyList(), null, "071 234 5678"))
        }
        val profile = ParticipantProfileViewModel(repo)
        compose.setContent {
            ParticipantTestTheme { ParticipantProfileScreen("other", rememberNavController(), profile) }
        }
        compose.onNodeWithText("071 234 5678").assertExists()
        compose.onNodeWithText("Call").assertExists()
        // Deliberately do not launch a dialer or call from automated QA.
    }
    @Test fun messagesEntryReopensExistingConversation() {
        val repository = FakeChat()
        val messages = MessagesViewModel(repository)
        val chat = ChatViewModel(repository)
        compose.setContent {
            ParticipantTestTheme {
                val nav = rememberNavController()
                NavHost(nav, startDestination = "messages") {
                    composable("messages") { MessagesScreen(nav, messages) }
                    composable(ParticipantRoutes.CHAT) { ChatScreen("other", nav, chat) }
                }
            }
        }
        capture("messages")
        compose.onNodeWithText("Nimal Perera").performClick()
        compose.onNodeWithText("Yes, it is available.").assertIsDisplayed()
    }
    @Test fun emptyChatHasNormalComposerAndNoReconnect() {
        val repository = FakeChat().apply { rows.value = emptyList() }
        val chat = ChatViewModel(repository)
        compose.setContent { ParticipantTestTheme { ChatScreen("other", rememberNavController(), chat) } }
        compose.onNodeWithText("Start the conversation").assertIsDisplayed()
        compose.onNodeWithText("Reconnect").assertDoesNotExist()
        compose.onNode(hasSetTextAction()).performTextInput("First message")
        compose.onNodeWithContentDescription("Send").performClick()
        compose.onNodeWithText("First message").assertIsDisplayed()
        compose.onNodeWithText("Reconnect").assertDoesNotExist()
    }

}
