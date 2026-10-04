package com.agroroute.features.auth

import com.agroroute.core.util.Result
import com.agroroute.core.repository.AuthRepository
import javax.inject.Inject

/**
 * Sends a password reset email to the specified email address.
 * Returns success regardless of whether the email exists to prevent account enumeration.
 */
class SendPasswordResetEmailUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String): Result<Unit> {
        if (email.isBlank()) return Result.Error("Email cannot be empty")
        // Basic email format validation
        val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        if (!emailRegex.matches(email.trim())) {
            return Result.Error("Please enter a valid email address")
        }
        return authRepository.sendPasswordResetEmail(email.trim())
    }
}
