package com.agroroute.core.remote

import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import com.agroroute.core.model.OrderPickupVerification

/** Authorization, status transition and points are committed together by the trusted backend. */
@Singleton
class OrderCompletionService @Inject constructor() {
    suspend fun cancel(orderId: String) {
        FirebaseFunctions.getInstance().getHttpsCallable("cancelOrder")
            .call(mapOf("orderId" to orderId)).await()
    }
    suspend fun pickup(orderId: String, action: String, code: String? = null): OrderPickupVerification {
        val arguments = mutableMapOf<String, Any>("orderId" to orderId, "action" to action)
        if (code != null) arguments["code"] = code
        val data = FirebaseFunctions.getInstance().getHttpsCallable("orderPickup")
            .call(arguments).await().data as? Map<*, *> ?: error("Invalid pickup response")
        return OrderPickupVerification(
            farmerConfirmed = data["farmerConfirmed"] == true,
            supplierConfirmed = data["supplierConfirmed"] == true,
            legacy = data["legacy"] == true,
            code = data["code"] as? String,
            expiresAt = (data["expiresAt"] as? Number)?.toLong()
        )
    }
    suspend fun complete(orderId: String) {
        FirebaseFunctions.getInstance().getHttpsCallable("completeOrder")
            .call(mapOf("orderId" to orderId)).await()
    }
}
