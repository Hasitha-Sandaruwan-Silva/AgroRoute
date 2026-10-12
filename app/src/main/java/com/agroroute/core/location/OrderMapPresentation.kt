package com.agroroute.core.location

import com.agroroute.core.model.Order
import com.agroroute.core.model.OrderStatus
import com.agroroute.core.model.deliverySnapshot
import com.agroroute.core.model.pickupSnapshot

enum class OrderDestinationKind { PICKUP, DELIVERY }

/** Read-only presentation. Destinations always come from the accepted order. */
data class OrderMapPresentation(
    val destination: SelectedLocation?,
    val address: String,
    val kind: OrderDestinationKind,
    val heading: String,
    val destinationLabel: String,
    val navigationLabel: String?,
    val sceneKey: String
) {
    val navigationEnabled: Boolean get() = navigationLabel != null
}

fun orderMapPresentation(order: Order, forSupplier: Boolean): OrderMapPresentation {
    val pickup = forSupplier && (order.status == OrderStatus.AWAITING_PICKUP || order.status == OrderStatus.CANCELLED)
    val kind = if (pickup) OrderDestinationKind.PICKUP else OrderDestinationKind.DELIVERY
    val destination = if (pickup) order.pickupSnapshot() else order.deliverySnapshot()
    val active = forSupplier && order.status in listOf(OrderStatus.AWAITING_PICKUP, OrderStatus.IN_TRANSIT)
    val label = if (pickup) "Farmer pickup" else "Delivery destination"
    return OrderMapPresentation(
        destination = destination,
        address = destination?.address?.ifBlank { destination.coordinates() }
            ?: (if (pickup) order.pickupLocation else order.deliveryLocation).ifBlank { "Not provided" },
        kind = kind,
        heading = when {
            order.status == OrderStatus.CANCELLED -> "Cancelled order location"
            order.status == OrderStatus.DELIVERED -> "Delivery location summary"
            active && pickup -> "Go to Farmer pickup"
            active -> "Go to delivery destination"
            else -> label
        },
        destinationLabel = label,
        navigationLabel = if (!active) null else if (pickup) "Navigate to Farmer" else "Navigate to Delivery",
        sceneKey = "${order.orderId}:${order.status}:$forSupplier:$kind"
    )
}

/** Opt-in order rendering; the editable picker keeps its existing camera behavior. */
data class OrderMapViewport(
    val sceneKey: String,
    val kind: OrderDestinationKind,
    val initialCameraPending: Boolean = false,
    val recenterRequest: Int = 0,
    val route: RoadRoute? = null
)

sealed interface OrderCameraTarget {
    data class Destination(val location: SelectedLocation, val zoom: Double = 15.5) : OrderCameraTarget
    data class Bounds(val points: List<SelectedLocation>) : OrderCameraTarget
}

/** One initial camera update per destination/stage, then only explicit recentering. */
class OrderMapCameraController {
    private var positioned = false
    private var lastRecenterRequest = 0

    fun onUserGesture() { positioned = true }

    fun target(
        destination: SelectedLocation?,
        current: SelectedLocation?,
        route: RoadRoute?,
        pending: Boolean,
        recenterRequest: Int
    ): OrderCameraTarget? {
        if (destination == null || pending) return null
        val explicit = recenterRequest != lastRecenterRequest
        if (positioned && !explicit) return null
        lastRecenterRequest = recenterRequest
        positioned = true
        val points = buildList {
            add(destination)
            current?.takeIf { usableDeviceCoordinates(it.latitude, it.longitude) }?.let(::add)
            route?.geometry?.let(::addAll)
        }.distinctBy { it.latitude to it.longitude }
        return if (points.size < 2) OrderCameraTarget.Destination(destination) else OrderCameraTarget.Bounds(points)
    }
}

/** GeoJSON uses longitude first; stored order values stay latitude first. */
fun SelectedLocation.geoJsonCoordinate(): Pair<Double, Double> = longitude to latitude
