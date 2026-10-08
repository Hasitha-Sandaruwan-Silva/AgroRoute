package com.agroroute.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agroroute.core.model.UserRole
import com.agroroute.core.notification.NotificationInboxItem
import com.agroroute.core.notification.NotificationRoute
import com.agroroute.core.notification.NotificationService
import com.agroroute.core.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificationInboxUiState(
    val items: List<NotificationInboxItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class NotificationInboxViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val notificationService: NotificationService
) : ViewModel() {
    private val _uiState = MutableStateFlow(NotificationInboxUiState())
    val uiState: StateFlow<NotificationInboxUiState> = _uiState.asStateFlow()

    fun loadInbox() {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching { notificationService.getInboxItems() }
                .onSuccess { items -> _uiState.update { it.copy(items = items, isLoading = false) } }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, error = error.message ?: "Could not load notifications.")
                    }
                }
        }
    }

    fun markRead(notificationId: String) {
        viewModelScope.launch {
            runCatching { notificationService.markInboxItemRead(notificationId) }
                .onSuccess {
                    val now = System.currentTimeMillis()
                    _uiState.update { state ->
                        state.copy(items = state.items.map { item ->
                            if (item.id == notificationId && !item.isRead) item.copy(readAt = now) else item
                        })
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.message ?: "Could not mark notification as read.") }
                }
        }
    }

    fun openNotification(item: NotificationInboxItem, onOpen: (String) -> Unit) {
        viewModelScope.launch {
            val userId = authRepository.currentUserId ?: return@launch
            val profile = authRepository.getCurrentUserProfile().first().getOrNull()
            val role = profile?.takeIf { it.userId == userId }?.role ?: return@launch
            if (!NotificationRoute.isAllowed(item.route, role)) {
                _uiState.update { it.copy(error = "This notification does not belong to an available screen for your account.") }
                return@launch
            }
            if (!item.isRead) {
                runCatching { notificationService.markInboxItemRead(item.id) }
                    .onSuccess {
                        _uiState.update { state ->
                            state.copy(items = state.items.map { current ->
                                if (current.id == item.id) current.copy(readAt = System.currentTimeMillis()) else current
                            })
                        }
                    }
                    .onFailure { error ->
                        _uiState.update { it.copy(error = error.message ?: "Could not mark notification as read.") }
                    }
            }
            onOpen(item.route)
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}
