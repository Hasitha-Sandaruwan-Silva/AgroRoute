package com.agroroute.core.model

/** Callable response only: pickup secrets never enter Room or shared order documents. */
data class OrderPickupVerification(
    val farmerConfirmed: Boolean = false,
    val supplierConfirmed: Boolean = false,
    val legacy: Boolean = false,
    val code: String? = null,
    val expiresAt: Long? = null
)
