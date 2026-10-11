package com.agroroute.core.remote

import com.agroroute.core.model.OrderPickupVerification

/** Spark is the active implementation; FunctionsOrderLifecycle is retained for future billing-backed use. */
interface OrderLifecycle {
    suspend fun pickup(orderId: String, action: String, code: String? = null): OrderPickupVerification
    suspend fun complete(orderId: String)
    suspend fun cancel(orderId: String)
}
