package com.agroroute.features.participants

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileState(val profile: ParticipantProfile? = null, val loading: Boolean = true, val error: String? = null)
@HiltViewModel
class ParticipantProfileViewModel @Inject constructor(private val people: ParticipantRepository) : ViewModel() {
    private val state = MutableStateFlow(ProfileState())
    val uiState = state.asStateFlow()
    private var job: Job? = null
    fun load(id: String) {
        job?.cancel(); state.value = ProfileState()
        job = viewModelScope.launch {
            people.profile(id).catch { state.value = ProfileState(loading = false, error = "Couldn't load this profile. Try again.") }
                .collect { state.value = ProfileState(it, false) }
        }
    }
}

data class MessagesState(val conversations: List<Conversation> = emptyList(), val loading: Boolean = true, val error: String? = null)
@HiltViewModel
class MessagesViewModel @Inject constructor(private val chat: ChatRepository) : ViewModel() {
    private val state = MutableStateFlow(MessagesState())
    val uiState = state.asStateFlow()
    private var job: Job? = null
    fun load() {
        job?.cancel(); state.value = MessagesState()
        job = viewModelScope.launch {
            try {
                chat.conversations().collect { state.value = MessagesState(it, false) }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { state.value = MessagesState(loading = false, error = "Couldn't load messages. Try again.") }
        }
    }
}

data class ChatState(val other: ParticipantIdentity? = null, val messages: List<ChatMessage> = emptyList(),
    val loading: Boolean = true, val sending: Boolean = false, val draft: String = "", val error: String? = null)
@HiltViewModel
class ChatViewModel @Inject constructor(private val chat: ChatRepository) : ViewModel() {
    private val state = MutableStateFlow(ChatState())
    val uiState = state.asStateFlow()
    val currentUserId get() = chat.currentUserId
    private var job: Job? = null
    private var otherId: String? = null
    fun load(id: String) {
        if (otherId != id) state.value = ChatState()
        otherId = id; job?.cancel()
        job = viewModelScope.launch {
            try {
                val (conversationId, other) = chat.pair(id)
                state.update { it.copy(other = other, loading = false, error = null) }
                chat.messages(conversationId).collect { messages -> state.update { it.copy(messages = messages, loading = false) } }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { state.update { it.copy(loading = false, error = "Couldn't open this chat. Try again.") } }
        }
    }
    fun draft(value: String) { if (!state.value.sending) state.update { it.copy(draft = value.take(4000)) } }
    fun send() {
        val id = otherId ?: return
        if (state.value.sending || state.value.draft.isBlank() || state.value.other == null) return
        val text = state.value.draft
        state.update { it.copy(sending = true, error = null) }
        viewModelScope.launch {
            try { chat.send(id, text); state.update { it.copy(sending = false, draft = "") } }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { state.update { it.copy(sending = false, error = "Couldn't send message. Try again.") } }
        }
    }
}
