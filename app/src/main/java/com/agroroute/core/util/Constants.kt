package com.agroroute.core.util

/**
 * App-wide string constants used across data, domain, and UI layers.
 */
object Constants {

    // ── Firestore collection names ─────────────────────────────────────────────
    const val COLLECTION_USERS         = "users"
    const val COLLECTION_LISTINGS      = "listings"
    const val COLLECTION_BIDS          = "bids"
    const val COLLECTION_ORDERS        = "orders"
    const val COLLECTION_ORDER_REQUESTS = "order_requests"

    // ── Firebase Storage paths ─────────────────────────────────────────────────
    const val STORAGE_LISTING_IMAGES = "listing_images"
    const val STORAGE_PROFILE_IMAGES = "profile_images"

    // ── Room database ──────────────────────────────────────────────────────────
    const val DATABASE_NAME    = "agroroute_db"
    const val DATABASE_VERSION = 4

    // ── Navigation argument keys ───────────────────────────────────────────────
    const val ARG_LISTING_ID = "listingId"
    const val ARG_BID_ID     = "bidId"
    const val ARG_ORDER_ID   = "orderId"
    const val ARG_USER_ID    = "userId"

    // ── Role identifiers ───────────────────────────────────────────────────────
    const val ROLE_FARMER   = "FARMER"
    const val ROLE_SUPPLIER = "SUPPLIER"
}
