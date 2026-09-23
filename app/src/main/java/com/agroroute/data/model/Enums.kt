package com.agroroute.data.model

/**
 * Enumerates all domain-specific enum types shared across data and domain layers.
 */

/** The role a user holds in the system — determines which screens they can access. */
enum class UserRole {
    FARMER,
    SUPPLIER
}

/** Lifecycle status of a [HarvestListing]. */
enum class ListingStatus {
    /** Accepting bids from suppliers. */
    OPEN,
    /** No longer accepting new bids. */
    CLOSED,
    /** Listing has been converted into an order. */
    FULFILLED
}

/** Lifecycle status of a [Bid] placed by a supplier. */
enum class BidStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    WITHDRAWN
}

/** Current stage of an [Order] through the fulfilment pipeline. */
enum class OrderStatus {
    AWAITING_PICKUP,
    IN_TRANSIT,
    DELIVERED,
    CANCELLED
}

/** Unit of weight/volume used when specifying harvest quantities. */
enum class QuantityUnit {
    KG,
    TONNE,
    BUSHEL,
    LITRE
}
