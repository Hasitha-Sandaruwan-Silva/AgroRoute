package com.agroroute.core.remote

import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/** Authorization, status transition and points are committed together by the trusted backend. */
@Singleton
class OrderCompletionService @Inject constructor() {
    suspend fun complete(orderId: String) {
        FirebaseFunctions.getInstance().getHttpsCallable("completeOrder")
            .call(mapOf("orderId" to orderId)).await()
    }
}
