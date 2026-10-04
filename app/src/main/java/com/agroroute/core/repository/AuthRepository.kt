package com.agroroute.core.repository

import com.agroroute.core.model.UserProfile
import com.agroroute.core.model.UserRole
import com.agroroute.core.util.Result
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val isLoggedIn: Boolean
    val currentUserId: String?

    fun getCurrentUserProfile(): Flow<Result<UserProfile?>>

    suspend fun restoreSession(): Result<UserProfile?>

    suspend fun login(email: String, password: String): Result<UserProfile>

    suspend fun signInWithGoogle(idToken: String): Result<GoogleSignInOutcome>

    suspend fun completeGoogleSignIn(role: UserRole): Result<UserProfile>

    suspend fun register(
        email: String,
        password: String,
        displayName: String,
        role: UserRole
    ): Result<UserProfile>

    suspend fun logout(): Result<Unit>

    suspend fun updateProfile(profile: UserProfile): Result<Unit>

    suspend fun sendPasswordResetEmail(email: String): Result<Unit>
}

sealed interface GoogleSignInOutcome {
    data class ProfileReady(val profile: UserProfile) : GoogleSignInOutcome
    data object RoleRequired : GoogleSignInOutcome
}

internal fun googleSignInOutcome(profile: UserProfile?): GoogleSignInOutcome =
    profile?.let(GoogleSignInOutcome::ProfileReady) ?: GoogleSignInOutcome.RoleRequired
