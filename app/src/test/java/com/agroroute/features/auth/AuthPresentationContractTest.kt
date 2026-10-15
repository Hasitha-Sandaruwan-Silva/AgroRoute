package com.agroroute.features.auth

import com.agroroute.core.model.UserProfile
import com.agroroute.core.model.UserRole
import com.agroroute.core.remote.ProfilePhotoUploader
import com.agroroute.core.repository.AuthRepository
import com.agroroute.core.repository.GoogleSignInOutcome
import com.agroroute.core.util.Result
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/** Guards the existing auth contract while its screens change. No production services. */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthPresentationContractTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = mockk<AuthRepository>()
    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        every { repository.currentUserId } returns null
        every { repository.getCurrentUserProfile() } returns flowOf(Result.Success(null))
    }
    @After fun cleanup() { Dispatchers.resetMain() }
    private fun model() = AuthViewModel(LoginUseCase(repository), RegisterUseCase(repository),
        SendPasswordResetEmailUseCase(repository), repository, mockk<ProfilePhotoUploader>())

    @Test fun sessionRestoreAndLogoutPreserveStateAndCallback() = runTest(dispatcher) {
        val profile = UserProfile(userId = "fixture", role = UserRole.SUPPLIER)
        coEvery { repository.restoreSession() } returns Result.Success(profile)
        coEvery { repository.logout() } returns Result.Success(Unit)
        val vm = model()
        advanceUntilIdle()
        vm.restoreSession(); advanceUntilIdle()
        assertEquals(profile, vm.uiState.value.user)
        assertTrue(vm.uiState.value.sessionResolved)
        var completed = false
        vm.logout { completed = true }; advanceUntilIdle()
        assertTrue(completed); assertNull(vm.uiState.value.user); assertFalse(vm.uiState.value.isLoading)
        coVerify(exactly = 1) { repository.logout() }
    }

    @Test fun googleKeepsRoleRequiredAndExistingProfileOutcomes() = runTest(dispatcher) {
        coEvery { repository.signInWithGoogle("fixture-token") } returns Result.Success(GoogleSignInOutcome.RoleRequired)
        val profile = UserProfile(userId = "fixture", role = UserRole.FARMER)
        coEvery { repository.completeGoogleSignIn(UserRole.FARMER) } returns Result.Success(profile)
        val vm = model(); advanceUntilIdle()
        vm.signInWithGoogle("fixture-token"); advanceUntilIdle()
        assertTrue(vm.uiState.value.requiresGoogleRole); assertNull(vm.uiState.value.user)
        vm.completeGoogleSignIn(UserRole.FARMER); advanceUntilIdle()
        assertEquals(profile, vm.uiState.value.user)
        assertTrue(vm.uiState.value.sessionResolved)
        // The existing flow clears the role-required event when navigation consumes the user.
        assertTrue(vm.uiState.value.requiresGoogleRole)
        vm.consumeUser()
        assertFalse(vm.uiState.value.requiresGoogleRole)
        coEvery { repository.signInWithGoogle("fixture-token") } returns Result.Success(GoogleSignInOutcome.ProfileReady(profile))
        vm.signInWithGoogle("fixture-token"); advanceUntilIdle()
        assertEquals(profile, vm.uiState.value.user); assertFalse(vm.uiState.value.isLoading)
    }

    @Test fun loginValidationAndErrorMappingRemainUnchanged() = runTest(dispatcher) {
        val login = LoginUseCase(repository)
        assertEquals("Email cannot be empty", (login("", "secret") as Result.Error).message)
        assertEquals("Password cannot be empty", (login("test@example.com", "") as Result.Error).message)
        assertEquals("Password must be at least 6 characters", (login("test@example.com", "12345") as Result.Error).message)
        coEvery { repository.login("test@example.com", "123456") } returns Result.Error("Email or password is incorrect.")
        val vm = model(); advanceUntilIdle()
        vm.login(" test@example.com ", "123456"); advanceUntilIdle()
        assertEquals("Email or password is incorrect.", vm.uiState.value.error)
        assertFalse(vm.uiState.value.isLoading)
        coVerify(exactly = 1) { repository.login("test@example.com", "123456") }
    }

    @Test fun registerPreservesBothRolesTrimmingThresholdAndDuplicateError() = runTest(dispatcher) {
        val register = RegisterUseCase(repository)
        assertEquals("Email cannot be empty", (register("", "123456", "Name", UserRole.FARMER) as Result.Error).message)
        assertEquals("Password must be at least 6 characters", (register("a@b.com", "12345", "Name", UserRole.FARMER) as Result.Error).message)
        assertEquals("Name cannot be empty", (register("a@b.com", "123456", " ", UserRole.FARMER) as Result.Error).message)
        for (role in UserRole.entries) {
            coEvery { repository.register("test@example.com", "123456", "Test", role) } returns Result.Error("An account already exists for this email.")
            val vm = model(); advanceUntilIdle()
            vm.register(" test@example.com ", "123456", " Test ", role); advanceUntilIdle()
            assertEquals("An account already exists for this email.", vm.uiState.value.error)
            assertFalse(vm.uiState.value.isLoading)
            coVerify(exactly = 1) { repository.register("test@example.com", "123456", "Test", role) }
        }
    }
}
