package com.agroroute

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.agroroute.core.components.AgroSnackbarHost
import com.agroroute.core.components.OrderPickupSection
import com.agroroute.core.model.OrderPickupVerification
import com.agroroute.core.model.OrderStatus
import com.agroroute.core.theme.AgroRouteTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class UserMessagePresentationTest(private val dark: Boolean) {
    @get:Rule val compose = createComposeRule()
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "dark={0}")
        fun themes() = listOf(arrayOf(false), arrayOf(true))
    }
    @Test fun directSnackbarCannotDisplayProviderDiagnostics() {
        compose.setContent {
            AgroRouteTheme(darkTheme = dark) {
                val host = remember { SnackbarHostState() }
                LaunchedEffect(Unit) { host.showSnackbar("Cloudinary upload failed (HTTP 401)") }
                AgroSnackbarHost(host)
            }
        }
        compose.onNodeWithText("Couldn't complete this action. Please try again.").assertIsDisplayed()
        compose.onNodeWithText("Cloudinary upload failed (HTTP 401)").assertDoesNotExist()
    }
    @Test fun inlinePickupErrorsAreFriendlyAndControlsRemainAvailable() {
        compose.setContent {
            AgroRouteTheme(darkTheme = dark) {
                OrderPickupSection("fixture", OrderStatus.AWAITING_PICKUP, false,
                    OrderPickupVerification(), false, "Firestore transaction failed", {}, onVerify = {})
            }
        }
        compose.onNodeWithText("Couldn't complete this action. Please try again.").assertIsDisplayed()
        compose.onNodeWithText("Firestore transaction failed").assertDoesNotExist()
        compose.onNodeWithText("Enter pickup code").assertExists()
        compose.onNodeWithText("Confirm Handover").assertIsNotEnabled()
    }
}
