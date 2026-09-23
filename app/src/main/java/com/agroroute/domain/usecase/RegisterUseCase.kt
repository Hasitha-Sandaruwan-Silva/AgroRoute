package com.agroroute.domain.usecase

import com.agroroute.data.model.UserProfile
import com.agroroute.data.model.UserRole
import com.agroroute.domain.repository.AuthRepository
import com.agroroute.util.Result
import javax.inject.Inject

/**
 * Registers a new user account and creates their profile document in Firestore.
 */
class RegisterUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        email: String,
        password: String,
        displayName: String,
        role: UserRole
    ): Result<UserProfile> {
        if (email.isBlank())       return Result.Error("Email cannot be empty")
        if (password.length < 6)   return Result.Error("Password must be at least 6 characters")
        if (displayName.isBlank()) return Result.Error("Name cannot be empty")
        return authRepository.register(email.trim(), password, displayName.trim(), role)
    }
}
