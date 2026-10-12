package com.agroroute.core.location

import com.agroroute.core.model.Order
import com.agroroute.core.model.OrderStatus
import com.agroroute.core.model.UserProfile
import org.junit.Assert.*
import org.junit.Test

class OrderMapPresentationTest {
    private val pickup = SelectedLocation("Farm", 7.29, 80.63)
    private val delivery = SelectedLocation("Depot", 6.91, 79.86)
    private val current = SelectedLocation("", 7.31, 80.61)
    private val order = Order(orderId = "order", pickupLocation = pickup.toStoredText(), deliveryLocation = delivery.toStoredText())

    @Test fun awaitingPickupUsesFarmerSnapshotAndGreenPickupKind() {
        val state = orderMapPresentation(order, true)
        assertEquals(pickup, state.destination)
        assertEquals(OrderDestinationKind.PICKUP, state.kind)
        assertEquals("Go to Farmer pickup", state.heading)
        assertEquals("Navigate to Farmer", state.navigationLabel)
    }
    @Test fun transitUsesSupplierDeliverySnapshotAndUpdatesScene() {
        val waiting = orderMapPresentation(order, true)
        val transit = orderMapPresentation(order.copy(status = OrderStatus.IN_TRANSIT), true)
        assertEquals(delivery, transit.destination)
        assertEquals(OrderDestinationKind.DELIVERY, transit.kind)
        assertEquals("Navigate to Delivery", transit.navigationLabel)
        assertNotEquals(waiting.sceneKey, transit.sceneKey)
    }
    @Test fun changingProfilesCannotChangeEitherSavedOrderDestination() {
        val profile = UserProfile(location = "New farm\n[8, 81]")
        assertNotEquals(profile.location, order.pickupLocation)
        assertEquals(pickup, orderMapPresentation(order, true).destination)
        assertEquals(delivery, orderMapPresentation(order.copy(status = OrderStatus.IN_TRANSIT), true).destination)
    }
    @Test fun deliveredAndCancelledHaveCompactSummaryWithoutActiveNavigation() {
        for (status in listOf(OrderStatus.DELIVERED, OrderStatus.CANCELLED)) {
            val state = orderMapPresentation(order.copy(status = status), true)
            assertFalse(state.navigationEnabled)
            assertNull(state.navigationLabel)
            assertNotNull(state.destination)
        }
        assertEquals(delivery, orderMapPresentation(order.copy(status = OrderStatus.DELIVERED), true).destination)
        assertEquals(pickup, orderMapPresentation(order.copy(status = OrderStatus.CANCELLED), true).destination)
    }
    @Test fun farmerAlwaysHasDeliverySummaryAndNoSupplierControls() {
        for (status in OrderStatus.entries) {
            val state = orderMapPresentation(order.copy(status = status), false)
            assertEquals(delivery, state.destination)
            assertFalse(state.navigationEnabled)
        }
    }
    @Test fun malformedSnapshotKeepsAddressWithoutInventingCoordinate() {
        val state = orderMapPresentation(order.copy(pickupLocation = "Old farm address"), true)
        assertNull(state.destination)
        assertEquals("Old farm address", state.address)
    }
    @Test fun initialCameraFitsBothPinsWithoutCountryFallback() {
        val target = OrderMapCameraController().target(pickup, current, null, false, 0) as OrderCameraTarget.Bounds
        assertEquals(setOf(pickup, current), target.points.toSet())
        assertEquals(2, target.points.size)
    }
    @Test fun missingCurrentPositionCentersSavedDestinationAtStreetZoom() {
        val target = OrderMapCameraController().target(pickup, null, null, false, 0) as OrderCameraTarget.Destination
        assertEquals(pickup, target.location)
        assertEquals(15.5, target.zoom, 0.0)
    }
    @Test fun missingDestinationNeverInventsDefaultSriLankaPoint() {
        assertNull(OrderMapCameraController().target(null, current, null, false, 0))
    }
    @Test fun gpsAndRouteLoadingDoNotConsumeInitialCameraFit() {
        val controller = OrderMapCameraController()
        assertNull(controller.target(pickup, null, null, true, 0))
        assertTrue(controller.target(pickup, current, null, false, 0) is OrderCameraTarget.Bounds)
    }
    @Test fun recompositionAndPositionUpdatesDoNotResetUserCamera() {
        val controller = OrderMapCameraController()
        assertNotNull(controller.target(pickup, current, null, false, 0))
        controller.onUserGesture()
        repeat(5) { assertNull(controller.target(pickup, current.copy(latitude = 7.32 + it * .001), null, false, 0)) }
    }
    @Test fun gestureBeforeGpsArrivesIsPreservedUntilExplicitRecenter() {
        val controller = OrderMapCameraController()
        controller.onUserGesture()
        assertNull(controller.target(pickup, current, null, false, 0))
        assertTrue(controller.target(pickup, current, null, false, 1) is OrderCameraTarget.Bounds)
        assertNull(controller.target(pickup, current, null, false, 1))
    }
    @Test fun recenterWithoutGpsStillCentersDestination() {
        val controller = OrderMapCameraController()
        controller.onUserGesture()
        assertTrue(controller.target(delivery, null, null, false, 1) is OrderCameraTarget.Destination)
    }
    @Test fun reopeningOrNewStageGetsOneFreshCameraController() {
        val waiting = OrderMapCameraController()
        assertNotNull(waiting.target(pickup, current, null, false, 0))
        assertNull(waiting.target(pickup, current, null, false, 0))
        assertNotNull(OrderMapCameraController().target(delivery, current, null, false, 0))
    }
    @Test fun identicalCurrentAndDestinationAvoidDegenerateBoundsAndOriginFixIsIgnored() {
        assertTrue(OrderMapCameraController().target(pickup, pickup, null, false, 0) is OrderCameraTarget.Destination)
        assertTrue(OrderMapCameraController().target(pickup, SelectedLocation("", 0.0, 0.0), null, false, 0) is OrderCameraTarget.Destination)
    }
    @Test fun validRoadGeometryIsIncludedInCameraBounds() {
        val detour = SelectedLocation("", 7.4, 80.7)
        val route = RoadRoute(listOf(current, detour, pickup), 12400.0, 1440)
        val target = OrderMapCameraController().target(pickup, current, route, false, 0) as OrderCameraTarget.Bounds
        assertTrue(target.points.contains(detour))
    }
    @Test fun geoJsonAndExternalNavigationUseCorrectCoordinateOrder() {
        assertEquals(80.63 to 7.29, pickup.geoJsonCoordinate())
        assertEquals("7.290000,80.630000", navigationCoordinate(pickup))
    }
}
