package com.agroroute.domain.usecase

import com.agroroute.data.model.UserProfile
import com.agroroute.domain.repository.AuthRepository
import com.agroroute.util.Result
import javax.inject.Inject

/**
 * Authenticates a user with email and password, returning their [UserProfile] on success.
 */
class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String): Result<UserProfile> {
        if (email.isBlank()) return Result.Error("Email cannot be empty")
        if (password.isBlank()) return Result.Error("Password cannot be empty")
        if (password.length < 6) return Result.Error("Password must be at least 6 characters")
        return authRepository.login(email.trim(), password)
    }
}
