package com.agroroute.features.participants

import com.agroroute.core.model.UserRole
import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class ParticipantChatTest {
    @Before fun main() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun reset() { Dispatchers.resetMain() }
    private val farmer = ParticipantIdentity("farmer", "Farmer Name", UserRole.FARMER)
    private val supplier = ParticipantIdentity("supplier", "Supplier Name", UserRole.SUPPLIER)
    @Test fun conversationIdentityIsStableAndCollisionSafe() {
        assertEquals(conversationIdentity("farmer", "supplier"), conversationIdentity("farmer", "supplier"))
        assertNotEquals(conversationIdentity("a_b", "c"), conversationIdentity("a", "b_c"))
        assertThrows(IllegalArgumentException::class.java) { conversationIdentity("same", "same") }
        assertThrows(IllegalArgumentException::class.java) { conversationIdentity("a/b", "c") }
    }
    @Test fun trimAndRejectBlankAndOversizeMessages() {
        assertEquals("Hello", messageText(" \nHello  "))
        assertThrows(IllegalArgumentException::class.java) { messageText(" \n\t ") }
        assertThrows(IllegalArgumentException::class.java) { messageText("x".repeat(4001)) }
    }
    @Test fun ratingAndDistributionUseRealReviewsAndEmptyRatingIsAbsent() {
        val reviews = listOf(ParticipantReview("1", 5, "Great", null, "A"), ParticipantReview("2", 3, "Fine", null, "B"))
        val profile = ParticipantProfile(farmer, reviews, listOf("Rice"))
        assertEquals(4.0, profile.averageRating!!, .001)
        assertEquals(1, profile.ratingCount(5)); assertEquals(0, profile.ratingCount(4))
        assertEquals(2, profile.reviews.size)
        assertNull(ParticipantProfile(supplier, emptyList(), null).averageRating)
        assertNull(supplier.memberSince); assertNull(supplier.points)
    }
    @Test fun bothRolesProfilesKeepCorrectIdentityAndNoInventedActivity() = runTest {
        for (person in listOf(farmer, supplier)) {
            val repo = object : ParticipantRepository {
                override suspend fun identity(id: String) = person
                override fun profile(id: String) = flowOf(ParticipantProfile(person, emptyList(), null))
            }
            val vm = ParticipantProfileViewModel(repo)
            vm.load(person.id)
            assertEquals(person, vm.uiState.value.profile?.identity)
            assertNull(vm.uiState.value.profile?.activeHarvests)
        }
    }
    @Test fun rapidSendDoesNotDuplicateAndFailureRetainsDraft() = runTest {
        val completion = CompletableDeferred<Unit>()
        var sends = 0
        val repo = object : ChatRepository {
            override val currentUserId = "supplier"
            override fun conversations() = flowOf(emptyList<Conversation>())
            override suspend fun pair(otherId: String) = "conversation" to farmer
            override fun messages(conversationId: String) = flowOf(emptyList<ChatMessage>())
            override suspend fun send(otherId: String, input: String) { sends++; completion.await(); error("network") }
        }
        val vm = ChatViewModel(repo)
        val store = ViewModelStore().apply { put("chat", vm) }
        try {
            vm.load("farmer"); vm.draft("Hello"); vm.send(); vm.send()
            assertEquals(1, sends); assertTrue(vm.uiState.value.sending)
            completion.complete(Unit); runCurrent()
            assertEquals("Hello", vm.uiState.value.draft)
            assertFalse(vm.uiState.value.sending)
            assertEquals("Couldn't send message. Try again.", vm.uiState.value.error)
        } finally { store.clear() }
    }
    @Test fun successfulSendClearsDraftAndRealtimeMessagesAppear() = runTest {
        val messages = MutableStateFlow(emptyList<ChatMessage>())
        val repo = object : ChatRepository {
            override val currentUserId = "supplier"
            override fun conversations() = flowOf(emptyList<Conversation>())
            override suspend fun pair(otherId: String) = "conversation" to farmer
            override fun messages(conversationId: String) = messages
            override suspend fun send(otherId: String, input: String) { messages.value = listOf(ChatMessage("1", "supplier", messageText(input), 1)) }
        }
        val vm = ChatViewModel(repo); val store = ViewModelStore().apply { put("chat", vm) }
        try {
            vm.load("farmer"); vm.draft("  Hello  "); vm.send()
            assertEquals("", vm.uiState.value.draft)
            assertEquals("Hello", vm.uiState.value.messages.single().text)
            messages.value = messages.value + ChatMessage("2", "farmer", "Reply", 2)
            assertEquals("Reply", vm.uiState.value.messages.last().text)
        } finally { store.clear() }
    }
    @Test fun restoredAuthWaitsThenLoadsEmptyChatWithoutReconnect() = runTest {
        val auth = MutableStateFlow<String?>(null)
        var pairs = 0
        val repo = object : ChatRepository {
            override val currentUserId get() = auth.value
            override val authenticatedUserIds = auth
            override fun conversations() = flowOf(emptyList<Conversation>())
            override suspend fun pair(otherId: String): Pair<String, ParticipantIdentity> {
                check(currentUserId != null); pairs++; return "conversation" to farmer
            }
            override fun messages(conversationId: String) = flowOf(emptyList<ChatMessage>())
            override suspend fun send(otherId: String, input: String) = Unit
        }
        val vm = ChatViewModel(repo); val store = ViewModelStore().apply { put("chat", vm) }
        try {
            vm.load("farmer"); runCurrent()
            assertTrue(vm.uiState.value.loading); assertNull(vm.uiState.value.error); assertEquals(0, pairs)
            auth.value = "supplier"; runCurrent()
            assertFalse(vm.uiState.value.loading); assertFalse(vm.uiState.value.connectionError)
            assertTrue(vm.uiState.value.messages.isEmpty()); assertNull(vm.uiState.value.error)
            assertEquals(1, pairs)
            auth.value = null; runCurrent()
            assertTrue(vm.uiState.value.loading); assertNull(vm.uiState.value.other)
        } finally { store.clear() }
    }
    @Test fun reconnectRemovesFailedListenerAndKeepsCanonicalConversation() = runTest {
        var fail = true
        var active = 0
        var peak = 0
        val ids = mutableListOf<String>()
        val repo = object : ChatRepository {
            override val currentUserId = "supplier"
            override fun conversations() = flowOf(emptyList<Conversation>())
            override suspend fun pair(otherId: String) = "canonical" to farmer
            override fun messages(conversationId: String) = flow<List<ChatMessage>> {
                ids += conversationId; active++; peak = maxOf(peak, active)
                try {
                    if (fail) error("permission denied")
                    emit(emptyList()); awaitCancellation()
                } finally { active-- }
            }
            override suspend fun send(otherId: String, input: String) = Unit
        }
        val vm = ChatViewModel(repo); val store = ViewModelStore().apply { put("chat", vm) }
        try {
            vm.load("farmer"); runCurrent()
            assertTrue(vm.uiState.value.connectionError)
            assertEquals("Couldn't connect to chat. Try again.", vm.uiState.value.error)
            fail = false; vm.load("farmer"); runCurrent()
            assertFalse(vm.uiState.value.connectionError); assertNull(vm.uiState.value.error)
            vm.load("farmer"); runCurrent()
            assertEquals(listOf("canonical", "canonical", "canonical"), ids)
            assertEquals(1, peak); assertEquals(1, active)
        } finally { store.clear() }
    }

}
