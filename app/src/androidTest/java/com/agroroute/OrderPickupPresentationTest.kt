package com.agroroute

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.agroroute.core.components.OrderPickupSection
import com.agroroute.core.model.OrderPickupVerification
import com.agroroute.core.model.OrderStatus
import com.agroroute.core.theme.AgroRouteTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class OrderPickupPresentationTest(private val dark: Boolean) {
    @get:Rule val compose = createComposeRule()
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "dark={0}")
        fun configurations() = listOf(arrayOf(false), arrayOf(true))
    }
    @Test fun farmerCodeValidationUsesExistingActionAndDisplaysError() {
        var submitted: String? = null
        compose.setContent {
            AgroRouteTheme(darkTheme = dark) {
                OrderPickupSection("fixture", OrderStatus.AWAITING_PICKUP, false,
                    OrderPickupVerification(), false, "Incorrect pickup code", {}, onVerify = { submitted = it })
            }
        }
        compose.onNodeWithText("Confirm Handover").assertIsNotEnabled()
        compose.onNodeWithText("Enter pickup code").performTextInput("123456")
        compose.onNodeWithText("Confirm Handover").assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals("123456", submitted) }
        compose.onNodeWithText("Incorrect pickup code").assertIsDisplayed()
    }
    @Test fun supplierSeesCodeAndConfirmsOwnPickupWithoutFarmerConfirmation() {
        var collected = false
        compose.setContent {
            AgroRouteTheme(darkTheme = dark) {
                OrderPickupSection("fixture", OrderStatus.AWAITING_PICKUP, true,
                    OrderPickupVerification(code = "123456"), false, null, {}, onCollect = { collected = true })
            }
        }
        compose.onNodeWithText("123456").assertIsDisplayed()
        compose.onNodeWithText("Confirm Pickup").assertIsEnabled().performClick()
        compose.runOnIdle { assertTrue(collected) }
    }
    @Test fun confirmedFarmerCannotSubmitAnotherCode() {
        compose.setContent {
            AgroRouteTheme(darkTheme = dark) {
                OrderPickupSection("fixture", OrderStatus.IN_TRANSIT, false,
                    OrderPickupVerification(farmerConfirmed = true), false, null, {})
            }
        }
        compose.onNodeWithText("Handover confirmed ✓ · Points awarded").assertIsDisplayed()
        compose.onNodeWithText("Enter pickup code").assertDoesNotExist()
    }
    @Test fun historicalOrderOffersNoPickupRewardAction() {
        compose.setContent {
            AgroRouteTheme(darkTheme = dark) {
                OrderPickupSection("fixture", OrderStatus.DELIVERED, true,
                    OrderPickupVerification(legacy = true), false, null, {})
            }
        }
        compose.onNodeWithText("Confirm Pickup").assertDoesNotExist()
        compose.onNodeWithText("This older order has no pickup verification. No pickup reward will be added.").assertIsDisplayed()
    }
}
