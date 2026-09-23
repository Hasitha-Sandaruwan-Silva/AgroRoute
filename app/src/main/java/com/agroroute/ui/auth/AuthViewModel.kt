package com.agroroute.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agroroute.data.model.UserProfile
import com.agroroute.data.model.UserRole
import com.agroroute.domain.usecase.LoginUseCase
import com.agroroute.domain.usecase.RegisterUseCase
import com.agroroute.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Represents the UI state for auth screens. */
data class AuthUiState(
    val isLoading: Boolean = false,
    val user: UserProfile? = null,
    val error: String? = null
)

/**
 * ViewModel shared by [LoginScreen] and [RegisterScreen] — handles auth logic
 * through use cases only, with no direct dependency on Firebase.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = loginUseCase(email, password)) {
                is Result.Success -> _uiState.update { it.copy(isLoading = false, user = result.data) }
                is Result.Error   -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                is Result.Loading -> Unit // handled above
            }
        }
    }

    fun register(
        email: String,
        password: String,
        displayName: String,
        role: UserRole
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = registerUseCase(email, password, displayName, role)) {
                is Result.Success -> _uiState.update { it.copy(isLoading = false, user = result.data) }
                is Result.Error   -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                is Result.Loading -> Unit
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}
