package com.agroroute.core.model

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

/** Category selected by a farmer for a harvest listing. */
enum class ListingCategory(val displayName: String) {
    ORGANIC("Organic"),
    INORGANIC("Inorganic")
}

/**
 * Tracks whether a listing's harvest is already available (READY) or only
 * expected in the future (EXPECTED). FAILED marks a harvest that was lost.
 * READY is the default so existing listings keep behaving as before.
 */
enum class HarvestStatus {
    EXPECTED,
    READY,
    FAILED
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

/** Lifecycle status of an [OrderRequest] for fixed-price listings. */
enum class OrderRequestStatus {
    /** Request is awaiting farmer review. */
    PENDING,
    /** Farmer accepted - order created, stock updated. */
    ACCEPTED,
    /** Farmer rejected - no stock change. */
    REJECTED,
    /** Supplier cancelled before farmer decision. */
    CANCELLED
}

/** Unit of weight/volume used when specifying harvest quantities. */
enum class QuantityUnit {
    KG,
    TONNE,
    BUSHEL,
    LITRE
}
