package com.agroroute.domain.repository

import com.agroroute.data.model.UserProfile
import com.agroroute.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Domain contract for authentication and user profile management — implemented in the data layer.
 */
interface AuthRepository {

    /** Returns true if a user is currently signed in. */
    val isLoggedIn: Boolean

    /** The UID of the currently signed-in user, or null. */
    val currentUserId: String?

    /** Emits the current user's profile from the local cache. */
    fun getCurrentUserProfile(): Flow<Result<UserProfile?>>

    /**
     * Signs in with [email] and [password].
     * On success, fetches the user profile from Firestore and caches locally.
     */
    suspend fun login(email: String, password: String): Result<UserProfile>

    /**
     * Registers a new account and creates a Firestore user document.
     * Returns the newly created [UserProfile].
     */
    suspend fun register(
        email: String,
        password: String,
        displayName: String,
        role: com.agroroute.data.model.UserRole
    ): Result<UserProfile>

    /** Signs out and clears local user cache. */
    suspend fun logout(): Result<Unit>

    /** Updates the user's profile in Firestore and Room. */
    suspend fun updateProfile(profile: UserProfile): Result<Unit>
}
