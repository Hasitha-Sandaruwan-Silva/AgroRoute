package com.agroroute.features.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import com.agroroute.core.model.UserProfile
import com.agroroute.core.model.UserRole
import com.agroroute.core.remote.ProfilePhotoUploader
import com.agroroute.core.repository.AuthRepository
import com.agroroute.core.repository.GoogleSignInOutcome
import com.agroroute.core.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val user: UserProfile? = null,
    val requiresGoogleRole: Boolean = false,
    val sessionResolved: Boolean = false,
    val error: String? = null,
    val photoUploading: Boolean = false,
    val photoPreviewUri: Uri? = null,
    val photoUploadSucceeded: Boolean = false,
    val passwordResetSent: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase,
    private val sendPasswordResetEmailUseCase: SendPasswordResetEmailUseCase,
    private val authRepository: AuthRepository,
    private val storageService: ProfilePhotoUploader
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        authRepository.getCurrentUserProfile()
            .onEach { result ->
                when (result) {
                    is Result.Success -> _uiState.update {
                        it.copy(
                            isLoading = false,
                            sessionResolved = authRepository.currentUserId == null || result.data != null,
                            user = result.data
                        )
                    }
                    is Result.Error -> _uiState.update { it.copy(error = result.message) }
                    Result.Loading -> Unit
                }
            }
            .launchIn(viewModelScope)
    }

    fun restoreSession() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = authRepository.restoreSession()) {
                is Result.Success -> _uiState.update { it.copy(isLoading = false, sessionResolved = true, user = result.data) }
                is Result.Error -> _uiState.update {
                    it.copy(isLoading = false, sessionResolved = true, user = null, error = result.message)
                }
                Result.Loading -> Unit
            }
        }
    }

    fun logout(onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = authRepository.logout()) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false, sessionResolved = true, user = null) }
                    onSuccess()
                }
                is Result.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                Result.Loading -> Unit
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, sessionResolved = false, error = null) }
            when (val result = loginUseCase(email, password)) {
                is Result.Success -> _uiState.update { it.copy(isLoading = false, user = result.data) }
                is Result.Error   -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                is Result.Loading -> Unit
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, sessionResolved = false, error = null) }
            when (val result = authRepository.signInWithGoogle(idToken)) {
                is Result.Success -> when (val outcome = result.data) {
                    is GoogleSignInOutcome.ProfileReady -> _uiState.update {
                        it.copy(isLoading = false, sessionResolved = true, user = outcome.profile)
                    }
                    GoogleSignInOutcome.RoleRequired -> _uiState.update {
                        it.copy(isLoading = false, requiresGoogleRole = true)
                    }
                }
                is Result.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                Result.Loading -> Unit
            }
        }
    }

    fun completeGoogleSignIn(role: UserRole) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = authRepository.completeGoogleSignIn(role)) {
                is Result.Success -> _uiState.update {
                    it.copy(isLoading = false, sessionResolved = true, user = result.data)
                }
                is Result.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                Result.Loading -> Unit
            }
        }
    }

    fun showError(message: String) = _uiState.update { it.copy(error = message) }

    fun register(
        email: String,
        password: String,
        displayName: String,
        role: UserRole
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, sessionResolved = false, error = null) }
            when (val result = registerUseCase(email, password, displayName, role)) {
                is Result.Success -> _uiState.update { it.copy(isLoading = false, user = result.data) }
                is Result.Error   -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                is Result.Loading -> Unit
            }
        }
    }

    fun sendPasswordResetEmail(email: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, passwordResetSent = false) }
            when (val result = sendPasswordResetEmailUseCase(email)) {
                is Result.Success -> _uiState.update {
                    it.copy(isLoading = false, passwordResetSent = true)
                }
                is Result.Error -> _uiState.update {
                    it.copy(isLoading = false, error = result.message, passwordResetSent = false)
                }
                is Result.Loading -> Unit
            }
        }
    }

    fun updateProfile(profile: UserProfile, onSuccess: () -> Unit = {}) {
        if (_uiState.value.isLoading || _uiState.value.photoUploading) return
        val current = _uiState.value.user ?: return showError("Sign in to update your profile")
        val safeProfile = profile.copy(
            userId = current.userId,
            email = current.email,
            role = current.role,
            points = current.points,
            createdAt = current.createdAt
        )
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = authRepository.updateProfile(safeProfile)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false, user = safeProfile) }
                    onSuccess()
                }
                is Result.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                Result.Loading -> Unit
            }
        }
    }

    fun uploadProfilePhoto(uri: Uri) {
        if (_uiState.value.photoUploading || _uiState.value.isLoading) return
        val current = _uiState.value.user ?: return showError("Sign in to update your profile photo")
        viewModelScope.launch {
            _uiState.update { it.copy(photoUploading = true, photoPreviewUri = uri, photoUploadSucceeded = false, error = null) }
            try {
                val url = storageService.upload(uri, current.userId)
                val updated = (_uiState.value.user ?: current).copy(profileImageUrl = url)
                when (val result = authRepository.updateProfile(updated)) {
                    is Result.Success -> _uiState.update { it.copy(user = updated, photoPreviewUri = null, photoUploadSucceeded = true) }
                    is Result.Error -> {
                        android.util.Log.e("ProfilePhoto", "Cloudinary upload succeeded; Firestore/Room profile save failed", result.cause)
                        _uiState.update { it.copy(error = "Photo uploaded, but your profile could not be saved. Try again.") }
                    }
                    Result.Loading -> Unit
                }
            } catch (error: kotlinx.coroutines.CancellationException) {
                throw error
            } catch (error: Exception) {
                android.util.Log.e("ProfilePhoto", "Profile photo upload failed", error)
                _uiState.update { it.copy(error = com.agroroute.core.util.userFacingThrowable(error, "Couldn't upload your profile photo. Check your connection and try again.")) }
            } finally {
                _uiState.update { it.copy(photoUploading = false) }
            }
        }
    }

    fun retryProfilePhoto() { _uiState.value.photoPreviewUri?.let(::uploadProfilePhoto) }

    fun clearError() = _uiState.update { it.copy(error = null) }

    fun consumeUser() = _uiState.update { it.copy(user = null, requiresGoogleRole = false) }

    fun consumePasswordResetSent() = _uiState.update { it.copy(passwordResetSent = false) }
}
