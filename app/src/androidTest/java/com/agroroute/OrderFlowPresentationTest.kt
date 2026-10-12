package com.agroroute

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.agroroute.core.components.*
import com.agroroute.core.location.SelectedLocation
import com.agroroute.core.model.*
import com.agroroute.core.theme.AgroRouteTheme
import com.agroroute.features.farmer.orderrequests.FarmerOrderRequestCard
import com.agroroute.features.farmer.bids.SuperbBidCard
import com.agroroute.features.supplier.orders.SupplierOrderRequestCard
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class OrderFlowPresentationTest(private val dark: Boolean, private val compact: Boolean) {
    @get:Rule val compose = createComposeRule()
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "dark={0},compact={1}")
        fun configurations() = listOf(arrayOf(false, true), arrayOf(true, true), arrayOf(false, false), arrayOf(true, false))
    }
    private val pin = SelectedLocation("Saved test destination", 7.0, 80.0).toStoredText()
    private val order = Order(orderId = "fixture", cropName = "Rice", farmerId = "farmer", supplierId = "supplier", quantity = 10.0, agreedPrice = 100.0)
    private fun content(body: @Composable () -> Unit) = compose.setContent {
        AgroRouteTheme(darkTheme = dark) {
            Column(Modifier.width(if (compact) 320.dp else 384.dp).height(if (compact) 480.dp else 640.dp)
                .testTag("viewport").verticalScroll(rememberScrollState()).padding(12.dp)) { body() }
        }
    }
    @Test fun cancellationRequiresWarningAndPreservesExistingCallback() {
        var cancelled = false
        content { OrderLifecycleActions(order, false, false, { cancelled = true }) }
        compose.onNodeWithText("Cancel Order").performClick()
        compose.onNodeWithText("Cancelling this order will deduct 2 points from your score only. Your score cannot go below 0. The reserved stock will be released.").assertIsDisplayed()
        compose.runOnIdle { assertFalse(cancelled) }
        compose.onNodeWithText("Keep Order").performClick()
        compose.runOnIdle { assertFalse(cancelled) }
        compose.onNodeWithText("Cancel Order").performClick()
        compose.onAllNodesWithText("Cancel Order").onLast().performClick()
        compose.runOnIdle { assertTrue(cancelled) }
    }
    @Test fun supplierTransportHasDeliveryOnlyAndFarmerHasNoTransportActions() {
        var delivered = false
        content {
            OrderLifecycleActions(order.copy(status = OrderStatus.IN_TRANSIT), false, false, {})
            OrderLifecycleActions(order.copy(status = OrderStatus.IN_TRANSIT), true, false, {}, { delivered = true })
        }
        compose.onNodeWithText("Cancel Order").assertDoesNotExist()
        compose.onAllNodesWithText("Confirm Delivery").assertCountEquals(1)
        compose.onNodeWithText("Confirm Delivery").performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { assertTrue(delivered) }
    }
    @Test fun terminalStatusesHaveNoLifecycleActionsAndCorrectDeliveryPolicy() {
        content {
            OrderSummarySection(order.copy(status = OrderStatus.DELIVERED), false)
            OrderLifecycleActions(order.copy(status = OrderStatus.DELIVERED), true, false, {})
            OrderLifecycleActions(order.copy(status = OrderStatus.CANCELLED), false, false, {})
        }
        compose.onNodeWithText("Confirm Delivery").assertDoesNotExist()
        compose.onNodeWithText("Cancel Order").assertDoesNotExist()
        compose.onNodeWithText("Delivery reward: +6 each, once for eligible orders. Your profile shows the confirmed total.").performScrollTo().assertIsDisplayed()
    }
    @Test fun awaitingFarmerShowsAcceptanceAndHandoverPolicyAndLateVerificationIsAllowed() {
        content {
            OrderSummarySection(order, false)
            OrderPickupSection("fixture", OrderStatus.IN_TRANSIT, false, OrderPickupVerification(), false, null, {})
        }
        compose.onNodeWithText("Acceptance reward: +1 once, confirmed by the server.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Verify physical handover to earn +3 once. This does not confirm Supplier collection.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Enter pickup code").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Confirm Pickup").assertDoesNotExist()
    }
    @Test fun expiredSupplierCodeCannotCollectAndErrorRefreshIsReachable() {
        content { OrderPickupSection("fixture", OrderStatus.AWAITING_PICKUP, true,
            OrderPickupVerification(code = "123456", expiresAt = 1), false, "Please refresh the expired code", {}) }
        compose.onNodeWithText("Confirm Pickup").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Confirming collection awards +4 once and starts transport. The server confirms your points.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Pickup code expired. Refresh to get a valid code.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Generate new code").performScrollTo().assertIsDisplayed()
    }
    @Test fun reviewEligibilityAndReviewedStateCannotOfferDuplicateReview() {
        var reviews = 0
        content {
            ReviewEligibilitySection(false, true, false, null, "Farmer", { reviews++ }, {})
            ReviewEligibilitySection(false, false, true, null, "Supplier", { reviews++ }, {})
            ReviewEligibilitySection(true, false, false, null, "Supplier", { reviews++ }, {})
        }
        compose.onNodeWithText("Rate Farmer").performClick()
        compose.runOnIdle { assertEquals(1, reviews) }
        compose.onNodeWithText("Review submitted").assertIsDisplayed()
        compose.onNodeWithText("Rate Supplier").assertDoesNotExist()
        compose.onNodeWithText("Checking review eligibility…").assertIsDisplayed()
    }
    @Test fun pendingRequestExposesOnlyEligibleAcceptanceAndExistingRejectCallback() {
        var accepted = false; var rejected = false
        val listing = HarvestListing(quantity = 10.0, biddingEnabled = false, location = pin)
        val request = OrderRequest(requestId = "request", cropName = "Rice", requestedQuantity = 11.0, deliveryLocation = pin)
        content { FarmerOrderRequestCard(request, listing, false, { accepted = true }, { rejected = true }) }
        compose.onNodeWithText("Accept").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Insufficient stock — 10.0 KG available").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Reject").performScrollTo().performClick()
        compose.runOnIdle { assertFalse(accepted); assertTrue(rejected) }
    }
    @Test fun pendingBidUnavailableAcceptanceAndAcceptedOrderNavigation() {
        var opened = false
        content {
            SuperbBidCard(Bid(bidId = "pending"), false, {}, {}, false, "Mark the harvest ready first")
            SuperbBidCard(Bid(bidId = "accepted", status = BidStatus.ACCEPTED), false, {}, {}, false,
                null, onOpenOrder = { opened = true })
        }
        compose.onNodeWithText("Accept This Bid").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("View order").performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { assertTrue(opened) }
    }
    @Test fun loadingAndReviewErrorKeepActionsDisabledOrRetryable() {
        var retries = 0
        content {
            OrderLifecycleActions(order, true, true, {})
            ReviewEligibilitySection(false, false, false, "Backend unavailable", "Farmer", {}, { retries++ })
        }
        compose.onNodeWithText("Cancel Order").assertIsNotEnabled()
        compose.onNodeWithText("Rate Farmer").assertDoesNotExist()
        compose.onNodeWithText("Retry review check").performClick()
        compose.runOnIdle { assertEquals(1, retries) }
    }
    @Test fun supplierPickupMapAndExistingNavigationAreAvailableBeforeCollection() {
        content { OrderLocationSection(order.copy(pickupLocation = pin, deliveryLocation = pin), true) }
        compose.onNodeWithTag("location-map").assertIsDisplayed()
        compose.onNodeWithText("Navigate to Farmer").performScrollTo().assertIsEnabled()
        compose.onNodeWithText("Navigate to Delivery").assertDoesNotExist()
    }
    @Test fun supplierTransportUsesSavedDeliveryMapAndMissingLegacyCoordinatesDisableNavigation() {
        content { OrderLocationSection(order.copy(status = OrderStatus.IN_TRANSIT,
            pickupLocation = pin, deliveryLocation = "Older delivery address"), true) }
        compose.onNodeWithText("This older order has no saved delivery coordinates. Address information is shown below.").assertIsDisplayed()
        compose.onNodeWithText("Navigate to Delivery").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Navigate to Farmer").assertDoesNotExist()
    }
    @Test fun supplierPendingRequestCanCancelAndHasNoOrderPenaltyOrAcceptanceAction() {
        var cancelled = false
        var request by mutableStateOf(OrderRequest(requestId = "pending", cropName = "Rice", isPreHarvest = true))
        content { SupplierOrderRequestCard(request, HarvestListing(quantity = 10.0, harvestStatus = HarvestStatus.READY),
            { cancelled = true }) }
        compose.onNodeWithText("Cancel request").performScrollTo().assertIsEnabled().performClick()
        compose.runOnIdle { assertTrue(cancelled) }
        compose.onNodeWithText("Waiting for harvest confirmation", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Accept").assertDoesNotExist()
        compose.onNodeWithText("Cancel Order").assertDoesNotExist()
        compose.runOnIdle { request = request.copy(status = OrderRequestStatus.CANCELLED, isPreHarvest = true) }
        compose.onNodeWithText("Cancel request").assertDoesNotExist()
        compose.onNodeWithText("This request was cancelled. Request cancellation does not deduct order points.")
            .performScrollTo().assertIsDisplayed()
    }
    @Test fun farmerOrderMapHasSavedDeliveryAndNoNavigationControls() {
        content { OrderLocationSection(order.copy(pickupLocation = pin, deliveryLocation = pin), false) }
        compose.onNodeWithTag("location-map").assertIsDisplayed()
        compose.onNodeWithText("Navigate to Farmer").assertDoesNotExist()
        compose.onNodeWithText("Navigate to Delivery").assertDoesNotExist()
        compose.onNodeWithText("Recenter current location and destination").assertDoesNotExist()
    }
    @Test fun expiryBlocksPendingRequestAndBidWithoutRejection() {
        val listing = HarvestListing(quantity = 100.0, expiresAt = 1, location = pin)
        val bid = Bid(requestedQuantity = 40.0, deliveryLocation = pin)
        content {
            SuperbBidCard(bid, false, {}, {}, false, listingAcceptanceBlockedReason(listing, true, bid.requestedQuantity, pin))
            FarmerOrderRequestCard(OrderRequest(requestedQuantity = 40.0, deliveryLocation = pin),
                listing.copy(biddingEnabled = false), false, {}, {})
        }
        compose.onNodeWithText("Accept This Bid").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Accept").performScrollTo().assertIsNotEnabled()
        compose.onAllNodesWithText("Listing expired").assertCountEquals(2)
    }
    @Test fun stockRestorationAutomaticallyEnablesPendingBid() {
        var listing by mutableStateOf(HarvestListing(quantity = 100.0, availableQuantity = 60.0, soldQuantity = 40.0, location = pin))
        val bid = Bid(requestedQuantity = 70.0, deliveryLocation = pin)
        content { SuperbBidCard(bid, false, {}, {}, false,
            listingAcceptanceBlockedReason(listing, true, bid.requestedQuantity, pin)) }
        compose.onNodeWithText("Accept This Bid").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Insufficient stock — 60.0 KG available").performScrollTo().assertIsDisplayed()
        compose.runOnIdle { listing = listing.releaseSale(40.0, true) }
        compose.onNodeWithText("Accept This Bid").performScrollTo().assertIsEnabled()
        compose.onNodeWithText("Insufficient stock — 60.0 KG available").assertDoesNotExist()
    }
    @Test fun supplierTransitShowsDeliveryMarkerAndFloatingRecenter() {
        content { OrderLocationSection(order.copy(status = OrderStatus.IN_TRANSIT, pickupLocation = pin,
            deliveryLocation = SelectedLocation("Saved delivery depot", 6.91, 79.86).toStoredText()), true) }
        compose.onNodeWithText("Go to delivery destination").assertIsDisplayed()
        compose.onNodeWithTag("order-map-recenter").assertIsDisplayed()
        compose.onNodeWithText("Saved delivery depot").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Navigate to Delivery").performScrollTo().assertIsEnabled()
        compose.onNodeWithText("Navigate to Farmer").assertDoesNotExist()
    }
    @Test fun deliveredAndCancelledMapsHaveNoActiveNavigation() {
        var status by mutableStateOf(OrderStatus.DELIVERED)
        content { OrderLocationSection(order.copy(status = status, pickupLocation = pin, deliveryLocation = pin), true) }
        compose.onNodeWithText("Delivery location summary").assertIsDisplayed()
        compose.onNodeWithTag("location-map").assertIsDisplayed()
        compose.onNodeWithTag("order-map-recenter").assertDoesNotExist()
        compose.onNodeWithText("Navigate to Delivery").assertDoesNotExist()
        compose.runOnIdle { status = OrderStatus.CANCELLED }
        compose.onNodeWithText("Cancelled order location").assertIsDisplayed()
        compose.onNodeWithTag("order-map-recenter").assertDoesNotExist()
        compose.onNodeWithText("Navigate to Farmer").assertDoesNotExist()
    }
    @Test fun changingStageUpdatesEmbeddedDestinationWithoutChangingSnapshots() {
        var status by mutableStateOf(OrderStatus.AWAITING_PICKUP)
        val pickup = SelectedLocation("Immutable Farmer farm", 7.29, 80.63).toStoredText()
        val delivery = SelectedLocation("Immutable Supplier depot", 6.91, 79.86).toStoredText()
        content { OrderLocationSection(order.copy(status = status, pickupLocation = pickup, deliveryLocation = delivery), true) }
        compose.onNodeWithText("Immutable Farmer farm").performScrollTo().assertIsDisplayed()
        compose.runOnIdle { status = OrderStatus.IN_TRANSIT }
        compose.onNodeWithText("Immutable Supplier depot").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Immutable Farmer farm").assertDoesNotExist()
        compose.onNodeWithText("Navigate to Delivery").performScrollTo().assertIsEnabled()
    }

}
