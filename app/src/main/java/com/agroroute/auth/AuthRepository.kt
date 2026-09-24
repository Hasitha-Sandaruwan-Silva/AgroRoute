package com.agroroute.auth

import com.agroroute.common.model.UserProfile
import com.agroroute.common.model.UserRole
import com.agroroute.common.util.Result
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val isLoggedIn: Boolean
    val currentUserId: String?

    fun getCurrentUserProfile(): Flow<Result<UserProfile?>>

    suspend fun login(email: String, password: String): Result<UserProfile>

    suspend fun register(
        email: String,
        password: String,
        displayName: String,
        role: UserRole
    ): Result<UserProfile>

    suspend fun logout(): Result<Unit>

    suspend fun updateProfile(profile: UserProfile): Result<Unit>
}
