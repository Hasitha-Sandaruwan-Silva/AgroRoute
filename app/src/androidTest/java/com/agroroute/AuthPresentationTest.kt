package com.agroroute

import android.graphics.Bitmap
import android.net.Uri
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.navigation.compose.*
import androidx.test.platform.app.InstrumentationRegistry
import com.agroroute.core.model.*
import com.agroroute.core.navigation.Screen
import com.agroroute.core.remote.ProfilePhotoUploader
import com.agroroute.core.repository.*
import com.agroroute.core.theme.AgroRouteTheme
import com.agroroute.core.util.Result
import com.agroroute.features.auth.*
import com.agroroute.features.welcome.WelcomeScreen
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

/** Exercises real auth screens, validation, ViewModel and navigation with no Firebase writes. */
@RunWith(Parameterized::class)
class AuthPresentationTest(private val dark: Boolean) {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "dark={0}")
        fun themes() = listOf(arrayOf(false), arrayOf(true))
    }
    private val repository = AuthFixtureRepository()
    private lateinit var viewModel: AuthViewModel
    private var completedRole: UserRole? = null
    private val role get() = if (dark) UserRole.SUPPLIER else UserRole.FARMER

    private fun content(compact: Boolean = false, startWelcome: Boolean = false) {
        // Keep the fixture activity visible during physical-device runs; no device settings change.
        compose.runOnUiThread {
            compose.activity.setShowWhenLocked(true)
            compose.activity.setTurnScreenOn(true)
            compose.activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        compose.setContent {
            AgroRouteTheme(darkTheme = dark) {
                val vm = remember {
                    AuthViewModel(LoginUseCase(repository), RegisterUseCase(repository),
                        SendPasswordResetEmailUseCase(repository), repository,
                        object : ProfilePhotoUploader {
                            override suspend fun upload(uri: Uri, userId: String): String = error("Not used")
                        }).also { viewModel = it }
                }
                val nav = rememberNavController()
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, if (compact) 1.3f else density.fontScale)) {
                Box(if (compact) Modifier.size(320.dp, 480.dp) else Modifier.fillMaxSize()) {
                NavHost(nav, startDestination = if (compact && !startWelcome) Screen.Register.route else Screen.Welcome.route) {
                    composable(Screen.Welcome.route) { WelcomeScreen(nav) }
                    composable(Screen.Login.route) { LoginScreen(nav, onLoginSuccess = { completedRole = it }, viewModel = vm) }
                    composable(Screen.Register.route) { RegisterScreen(nav, onRegisterSuccess = { completedRole = it }, viewModel = vm) }
                    composable(Screen.FarmerHome.route) { Text("Farmer destination") }
                    composable(Screen.SupplierHome.route) { Text("Supplier destination") }
                }
                }
                }
            }
        }
    }
    private fun login() { content(); compose.onNodeWithText("I already have an account").performClick() }
    private fun register() { content(); compose.onNodeWithText("Get Started").performClick() }
    private fun field(label: String) = compose.onNode(hasText(label) and hasSetTextAction())
    private fun fill(label: String, value: String) { field(label).performScrollTo().performTextReplacement(value) }
    private fun roleNode(label: String) = compose.onNode(isSelectable() and
        (hasText(label) or hasAnyDescendant(hasText(label))))
    private fun await(text: String) {
        compose.waitUntil(10_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    }
    private fun capture(name: String) {
        val image = compose.onNode(isRoot() and hasAnyDescendant(hasText("AgroRoute")))
            .captureToImage().asAndroidBitmap()
        File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
            "auth-${if (dark) "dark" else "light"}-$name.png").outputStream().use {
            image.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test fun welcomeFinalStateKeepsBothRoutesAndBackActions() {
        content()
        compose.onNodeWithText("Manage your harvests with a smarter farming experience.").assertIsDisplayed()
        compose.onNodeWithContentDescription("AgroRoute logo").assertIsDisplayed()
        capture("welcome")
        compose.onNodeWithText("I already have an account").performScrollTo().performClick()
        compose.onNodeWithText("Welcome Back").assertExists()
        compose.onNodeWithText("Sign in to continue your AgroRoute journey.").assertExists()
        compose.onNodeWithContentDescription("Back").performScrollTo().performClick()
        compose.onNodeWithText("Get Started").performScrollTo().performClick()
        compose.onNodeWithText("Choose your role").assertExists()
        compose.onNodeWithText("Join AgroRoute").assertExists()
        compose.onNodeWithText("Create your account and connect to a smarter agricultural marketplace.").assertExists()
        roleNode("Farmer").assertIsSelected()
        compose.onNodeWithContentDescription("Back").performScrollTo().performClick()
        compose.onNodeWithText("Get Started").assertExists()
        compose.runOnIdle { assertEquals(0, repository.loginCalls); assertEquals(0, repository.registerCalls) }
    }

    @Test fun restoredWelcomeKeepsAllThreePhotographicPages() {
        content()
        compose.onNodeWithText("Grow with Confidence").assertIsDisplayed()
        compose.onNodeWithTag("welcomeScreen").performTouchInput { swipeLeft() }
        compose.onNodeWithText("Trade Made Simple").assertIsDisplayed()
        compose.onNodeWithText("Connect farmers and suppliers in one trusted marketplace.").assertIsDisplayed()
        compose.onNodeWithContentDescription("Page 2 of 3").assertExists()
        capture("welcome-trade")
        compose.onNodeWithTag("welcomeScreen").performTouchInput { swipeLeft() }
        compose.onNodeWithText("From Farm to Destination").assertIsDisplayed()
        compose.onNodeWithText("Track orders, deliveries, and every step in between.").assertIsDisplayed()
        capture("welcome-delivery")
        compose.onNodeWithTag("welcomeScreen").performTouchInput { swipeRight() }
        compose.onNodeWithText("Trade Made Simple").assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, repository.loginCalls); assertEquals(0, repository.registerCalls) }
    }

    @Test fun compactWelcomeKeepsBothActionsReachableWithLargeText() {
        content(compact = true, startWelcome = true)
        compose.onNodeWithText("Get Started").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("I already have an account").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithText("Welcome Back").assertExists()
    }

    @Test fun roleCardsKeepFinalSelectionAndActualRegistrationFields() {
        register()
        roleNode("Supplier").performScrollTo().performClick().assertIsSelected()
        roleNode("Farmer").assertIsNotSelected()
        capture("register-supplier")
        roleNode("Farmer").performScrollTo().performClick().assertIsSelected()
        roleNode("Supplier").assertIsNotSelected()
        listOf("Full Name", "Email", "Password", "Confirm Password").forEach { field(it).assertExists() }
        compose.runOnIdle { assertEquals(0, repository.registerCalls) }
    }

    @Test fun keyboardNextAndDoneKeepTheExistingFieldSequence() {
        login()
        fill("Email", "test@example.com")
        field("Email").performImeAction()
        field("Password").assertIsFocused()
        field("Password").performTextInput("secret123")
        field("Password").performImeAction()
        field("Password").assertIsNotFocused()
        compose.onNodeWithText("Create account").performScrollTo().performClick()
        fill("Full Name", "Test Grower")
        field("Full Name").performImeAction()
        field("Email").assertIsFocused().performImeAction()
        field("Password").assertIsFocused().performImeAction()
        field("Confirm Password").assertIsFocused().performImeAction()
        field("Confirm Password").assertIsNotFocused()
        compose.runOnIdle { assertEquals(0, repository.loginCalls); assertEquals(0, repository.registerCalls) }
    }

    @Test fun loginPreservesValidationPasswordToggleAndKeyboardReachability() {
        login()
        capture("login")
        compose.onNodeWithText("Sign In").performScrollTo().performClick()
        await("Email cannot be empty")
        compose.runOnIdle { assertEquals(0, repository.loginCalls) }
        fill("Email", "test@example.com")
        fill("Password", "secret123")
        compose.onNodeWithContentDescription("Show password").performClick()
        field("Password").assertTextContains("secret123")
        compose.onNodeWithContentDescription("Hide password").performClick()
        field("Password").performClick()
        compose.onNodeWithText("Sign In").performScrollTo().assertIsDisplayed()
        capture("login-keyboard")
    }

    @Test fun loginLoadingSubmitsOnceAndNavigatesToSavedRole() {
        login()
        repository.loginGate = CompletableDeferred()
        fill("Email", "  test@example.com  ")
        fill("Password", "secret123")
        compose.onNodeWithText("Sign In").performScrollTo().performClick()
        compose.onNodeWithText("Continue with Google").assertIsNotEnabled()
        compose.runOnIdle {
            assertTrue(viewModel.uiState.value.isLoading)
            assertEquals(1, repository.loginCalls)
            assertEquals("test@example.com", repository.email)
        }
        capture("login-loading")
        compose.runOnIdle { repository.loginGate!!.complete(Result.Success(UserProfile(userId = "fixture", role = role))) }
        await(if (role == UserRole.FARMER) "Farmer destination" else "Supplier destination")
        compose.runOnIdle { assertEquals(role, completedRole) }
    }

    @Test fun invalidCredentialsKeepExistingReadableError() {
        login()
        fill("Email", "test@example.com"); fill("Password", "secret123")
        compose.onNodeWithText("Sign In").performScrollTo().performClick()
        await("Email or password is incorrect.")
        compose.onNodeWithText("Sign In").assertIsEnabled()
    }

    @Test fun registerPreservesFieldsDefaultRoleAndMismatchValidation() {
        register()
        capture("register")
        field("Full Name").assertExists(); field("Email").assertExists()
        field("Password").assertExists(); field("Confirm Password").assertExists()
        roleNode("Farmer").assertIsSelected()
        fill("Password", "secret123"); fill("Confirm Password", "different")
        compose.onNodeWithContentDescription("Show confirm password").performScrollTo().performClick()
        field("Confirm Password").assertTextContains("different")
        compose.onNodeWithText("Create Account").performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(0, repository.registerCalls) }
        capture("register-keyboard")
        compose.onAllNodesWithText("Passwords do not match").onFirst().assertExists()
    }

    @Test fun registerLoadingKeepsExactValuesAndRoleThenNavigates() {
        register()
        repository.registerGate = CompletableDeferred()
        fill("Full Name", "  Test Grower  "); fill("Email", "test@example.com")
        fill("Password", "secret123"); fill("Confirm Password", "secret123")
        roleNode(if (role == UserRole.FARMER) "Farmer" else "Supplier").performScrollTo().performClick()
        compose.onNodeWithText("Create Account").performScrollTo().performClick()
        compose.runOnIdle {
            assertTrue(viewModel.uiState.value.isLoading)
            assertEquals(1, repository.registerCalls)
            assertEquals(role, repository.role)
            assertEquals("Test Grower", repository.name)
            repository.registerGate!!.complete(Result.Success(UserProfile(userId = "fixture", role = role)))
        }
        await(if (role == UserRole.FARMER) "Farmer destination" else "Supplier destination")
        compose.runOnIdle { assertEquals(role, completedRole) }
    }

    @Test fun duplicateAccountErrorAndExistingLoginRegisterLinksRemainUsable() {
        login()
        compose.onNodeWithText("Create account").performScrollTo().performClick()
        fill("Full Name", "Test Grower"); fill("Email", "test@example.com")
        fill("Password", "secret123"); fill("Confirm Password", "secret123")
        compose.onNodeWithText("Create Account").performScrollTo().performClick()
        await("An account already exists for this email.")
        val footer = compose.onNodeWithText("Sign In").performScrollTo().assertIsDisplayed()
        val error = compose.onNodeWithText("An account already exists for this email.")
        assertTrue("Error snackbar must leave the footer clear",
            footer.fetchSemanticsNode().boundsInRoot.bottom <= error.fetchSemanticsNode().boundsInRoot.top)
        footer.performClick()
        await("Welcome Back")
    }

    @Test fun passwordResetPreservesValidationAndSuccessMessage() {
        login()
        compose.onNodeWithText("Forgot password?").performScrollTo().performClick()
        compose.onNodeWithText("Send Reset Link").performClick()
        compose.runOnIdle { assertEquals(0, repository.resetCalls) }
        field("Email address").performTextReplacement("test@example.com")
        compose.onNodeWithText("Send Reset Link").performClick()
        await("If the email exists, a reset link was sent to your inbox")
        compose.runOnIdle { assertEquals(1, repository.resetCalls) }
        // Existing behavior dismisses the dialog after the success snackbar completes.
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("Reset Password").fetchSemanticsNodes().isEmpty()
        }
        compose.onNodeWithText("Reset Password").assertDoesNotExist()
    }

    @Test fun compactViewportWithLargeFontKeepsRegisterActionReachableWithKeyboard() {
        content(compact = true)
        fill("Confirm Password", "secret123")
        field("Confirm Password").performClick()
        compose.onNodeWithText("Create Account").performScrollTo().assertIsDisplayed()
        roleNode("Supplier").performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithText("Create Account").performScrollTo().assertIsDisplayed()
    }
}

private class AuthFixtureRepository : AuthRepository {
    override val currentUserId: String? = null
    override val isLoggedIn = false
    override fun getCurrentUserProfile() = flowOf(Result.Success<UserProfile?>(null))
    override suspend fun restoreSession() = Result.Success<UserProfile?>(null)
    var loginGate: CompletableDeferred<Result<UserProfile>>? = null
    var registerGate: CompletableDeferred<Result<UserProfile>>? = null
    var loginCalls = 0; var registerCalls = 0; var resetCalls = 0
    var email = ""; var name = ""; var role: UserRole? = null
    override suspend fun login(email: String, password: String): Result<UserProfile> {
        loginCalls++; this.email = email
        return loginGate?.await() ?: Result.Error("Email or password is incorrect.")
    }
    override suspend fun register(email: String, password: String, displayName: String, role: UserRole): Result<UserProfile> {
        registerCalls++; this.email = email; name = displayName; this.role = role
        return registerGate?.await() ?: Result.Error("An account already exists for this email.")
    }
    override suspend fun signInWithGoogle(idToken: String): Result<GoogleSignInOutcome> = Result.Success(GoogleSignInOutcome.RoleRequired)
    override suspend fun completeGoogleSignIn(role: UserRole) = Result.Success(UserProfile(userId = "fixture", role = role))
    override suspend fun logout() = Result.Success(Unit)
    override suspend fun updateProfile(profile: UserProfile) = Result.Success(Unit)
    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> { resetCalls++; return Result.Success(Unit) }
}
