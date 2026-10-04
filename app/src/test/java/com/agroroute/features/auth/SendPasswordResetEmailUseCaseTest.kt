package com.agroroute.features.auth

import com.agroroute.core.repository.AuthRepository
import com.agroroute.core.util.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SendPasswordResetEmailUseCaseTest {

    @Test
    fun validEmailSendsResetEmail() = runBlocking {
        val authRepository = mockk<AuthRepository>()
        coEvery { authRepository.sendPasswordResetEmail("test@example.com") } returns Result.Success(Unit)

        val useCase = SendPasswordResetEmailUseCase(authRepository)
        val result = useCase("test@example.com")

        assertTrue(result is Result.Success)
        coVerify(exactly = 1) { authRepository.sendPasswordResetEmail("test@example.com") }
    }

    @Test
    fun blankEmailReturnsError() = runBlocking {
        val authRepository = mockk<AuthRepository>()
        val useCase = SendPasswordResetEmailUseCase(authRepository)
        val result = useCase("")

        assertTrue(result is Result.Error)
        assertEquals("Email cannot be empty", (result as Result.Error).message)
    }

    @Test
    fun invalidEmailFormatReturnsError() = runBlocking {
        val authRepository = mockk<AuthRepository>()
        val useCase = SendPasswordResetEmailUseCase(authRepository)
        val result = useCase("invalid-email")

        assertTrue(result is Result.Error)
        assertEquals("Please enter a valid email address", (result as Result.Error).message)
    }

    @Test
    fun emailIsTrimmedBeforeSending() = runBlocking {
        val authRepository = mockk<AuthRepository>()
        coEvery { authRepository.sendPasswordResetEmail("test@example.com") } returns Result.Success(Unit)

        val useCase = SendPasswordResetEmailUseCase(authRepository)
        val result = useCase("  test@example.com  ")

        assertTrue(result is Result.Success)
        coVerify(exactly = 1) { authRepository.sendPasswordResetEmail("test@example.com") }
    }

    @Test
    fun repositoryErrorPropagates() = runBlocking {
        val authRepository = mockk<AuthRepository>()
        coEvery { authRepository.sendPasswordResetEmail("test@example.com") } returns Result.Error("Network error")

        val useCase = SendPasswordResetEmailUseCase(authRepository)
        val result = useCase("test@example.com")

        assertTrue(result is Result.Error)
        assertEquals("Network error", (result as Result.Error).message)
    }
}
