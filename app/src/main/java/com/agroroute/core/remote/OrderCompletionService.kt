package com.agroroute.core.remote

import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import com.agroroute.core.model.OrderPickupVerification

/** Future trusted backend adapter. Not bound in Spark mode; enabling it also requires a coordinated Rules/receipt migration. */
@Singleton
class OrderCompletionService @Inject constructor() : OrderLifecycle {
    override suspend fun cancel(orderId: String) {
        FirebaseFunctions.getInstance().getHttpsCallable("cancelOrder")
            .call(mapOf("orderId" to orderId)).await()
    }
    override suspend fun pickup(orderId: String, action: String, code: String?): OrderPickupVerification {
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
    override suspend fun complete(orderId: String) {
        FirebaseFunctions.getInstance().getHttpsCallable("completeOrder")
            .call(mapOf("orderId" to orderId)).await()
    }
}
