package com.agroroute.core.model

import com.agroroute.core.location.SelectedLocation

/** Read only the order's saved strings; never resolve an order through a current profile/listing. */
fun Order.pickupSnapshot(): SelectedLocation? = SelectedLocation.fromStoredText(pickupLocation)
fun Order.deliverySnapshot(): SelectedLocation? = SelectedLocation.fromStoredText(deliveryLocation)

fun requirePickupLocation(value: String) {
    require(value.length <= 2000 && SelectedLocation.fromStoredText(value) != null) {
        "Farmer pickup location is missing. Ask the farmer to select the listing's pickup pin on the map."
    }
}

fun requireDeliveryLocation(value: String) {
    require(value.length <= 2000 && SelectedLocation.fromStoredText(value) != null) {
        "Select a delivery location on the map before confirming. Older bids/requests without a delivery pin must be placed again."
    }
}

/** Preserve exact encoded source values, copied inside the acceptance transaction. */
fun Order.withLocationSnapshots(pickup: String, delivery: String): Order {
    requirePickupLocation(pickup)
    requireDeliveryLocation(delivery)
    return copy(pickupLocation = pickup, deliveryLocation = delivery)
}
