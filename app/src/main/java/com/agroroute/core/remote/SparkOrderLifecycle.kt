package com.agroroute.core.remote

import com.agroroute.core.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Participant-attested Spark protocol. PIN correctness cannot be proven by Security Rules. */
@Singleton
class SparkOrderLifecycle @Inject constructor(
    private val remote: FirestoreService,
    private val auth: FirebaseAuth
) : OrderLifecycle {
    private data class LocalCode(val revision: Long, val code: String)
    private val codes = java.util.concurrent.ConcurrentHashMap<String, LocalCode>()
    private val db get() = remote.firestore
    private fun uid() = auth.currentUser?.uid ?: error("Sign in to update this order")
    private fun key(uid: String, orderId: String) = "$uid/$orderId"
    private fun orderRef(id: String) = db.collection("orders").document(id.also {
        require(it.isNotBlank() && it.length <= 128 && !it.contains('/')) { "Choose a valid order" }
    })
    private fun sessionRef(id: String) = db.collection("spark_pickup_sessions").document(id)
    private fun participant(order: Order, uid: String) {
        check(uid == order.farmerId || uid == order.supplierId) { "This order belongs to another account" }
    }
    override suspend fun pickup(orderId: String, action: String, code: String?): OrderPickupVerification = withContext(Dispatchers.IO) {
        val actor = uid()
        val ref = orderRef(orderId)
        val session = sessionRef(orderId)
        var issued: LocalCode? = null
        val failure = remote.runTransaction<String?> { tx ->
            val orderDoc = tx.get(ref)
            val order = orderDoc.toObject(Order::class.java) ?: error("Order is unavailable")
            participant(order, actor)
            val state = tx.get(session)
            val farmerConfirmed = state.getBoolean("farmerConfirmed") == true
            val supplierConfirmed = state.getBoolean("supplierConfirmed") == true
            val issuedAt = state.getTimestamp("issuedAt")?.toDate()?.time
            // Local time only provides friendly feedback; Rules enforce server time.
            val expired = issuedAt == null || issuedAt + DAY <= System.currentTimeMillis()
            val revision = state.getLong("revision") ?: 0L
            if (action == "view" || action == "refresh") {
                if (actor == order.supplierId && !farmerConfirmed && order.status != OrderStatus.CANCELLED &&
                    (order.status == OrderStatus.AWAITING_PICKUP || supplierConfirmed) &&
                    (action == "refresh" || expired || codes[key(actor, orderId)]?.revision != revision)) {
                    check(state.exists() || order.status == OrderStatus.AWAITING_PICKUP) { "This older order has no pickup verification" }
                    var pin = PickupPin.generate()
                    // Avoid repeating the old six digits, including on a second device.
                    while (state.exists() && PickupPin.matches(pin.code, state.getString("salt").orEmpty(), state.getString("verifier").orEmpty())) {
                        pin = PickupPin.generate()
                    }
                    val fields = mutableMapOf<String, Any>("salt" to pin.salt, "verifier" to pin.verifier,
                        "iterations" to PickupPin.ITERATIONS, "revision" to revision + 1,
                        "issuedAt" to FieldValue.serverTimestamp())
                    if (!state.exists()) fields.putAll(mapOf("orderId" to orderId, "farmerId" to order.farmerId,
                        "supplierId" to order.supplierId, "createdAt" to FieldValue.serverTimestamp(),
                        "attemptCount" to 0, "farmerConfirmed" to false, "supplierConfirmed" to false))
                    if (state.exists()) tx.update(session, fields) else tx.set(session, fields)
                    issued = LocalCode(revision + 1, pin.code)
                }
                null
            } else {
                check(action in listOf("verify", "collect")) { "Unknown pickup action" }
                check(order.status != OrderStatus.CANCELLED) { "This order was cancelled" }
                check(state.exists()) { "Ask the Supplier to generate a pickup code first" }
                val expected = if (action == "verify") order.farmerId else order.supplierId
                check(actor == expected) { "This action belongs to the other participant" }
                if ((action == "verify" && farmerConfirmed) || (action == "collect" && supplierConfirmed)) return@runTransaction null
                val profile = tx.get(db.collection("users").document(actor))
                if (action == "verify") {
                    check(!expired) { "Pickup code expired. Ask the Supplier to generate a new code." }
                    val window = state.getTimestamp("attemptWindowAt")?.toDate()?.time
                    val inWindow = window != null && System.currentTimeMillis() < window + WINDOW
                    val count = if (inWindow) state.getLong("attemptCount") ?: 0 else 0L
                    check(count < 5) { "Too many incorrect attempts. Try again after 30 minutes." }
                    val matches = PickupPin.matches(code.orEmpty(), state.getString("salt").orEmpty(), state.getString("verifier").orEmpty())
                    if (!matches) {
                        val fields = mutableMapOf<String, Any>("attemptCount" to count + 1, "lastAttemptAt" to FieldValue.serverTimestamp())
                        if (!inWindow) fields["attemptWindowAt"] = FieldValue.serverTimestamp()
                        tx.update(session, fields)
                        "Incorrect pickup code. Check the six digits with the Supplier."
                    } else {
                        tx.update(session, mapOf("farmerConfirmed" to true, "farmerConfirmedAt" to FieldValue.serverTimestamp()))
                        SparkScore.award(tx, db, order, profile, "farmer_handover", 3)
                        null
                    }
                } else {
                    check(order.status == OrderStatus.AWAITING_PICKUP) { "Pickup has already been confirmed or this order is no longer eligible" }
                    check(!expired || farmerConfirmed) { "Pickup code expired. Generate a new code before collection." }
                    tx.update(session, mapOf("supplierConfirmed" to true, "supplierConfirmedAt" to FieldValue.serverTimestamp()))
                    tx.update(ref, mapOf("status" to "IN_TRANSIT", "updatedAt" to System.currentTimeMillis(),
                        "pickedUpAt" to FieldValue.serverTimestamp()))
                    SparkScore.award(tx, db, order, profile, "supplier_pickup", 4)
                    null
                }
            }
        }
        issued?.let { latest -> codes.compute(key(actor, orderId)) { _, existing ->
            if (existing != null && existing.revision > latest.revision) existing else latest
        } }
        failure?.let { error(it) } // wrong-attempt transaction must commit before returning an error
        val committed = session.get(Source.SERVER).await()
        val latestOrder = ref.get(Source.SERVER).await().toObject(Order::class.java) ?: error("Order is unavailable")
        val farmerConfirmed = committed.getBoolean("farmerConfirmed") == true
        val expiry = committed.getTimestamp("issuedAt")?.toDate()?.time?.plus(DAY)
        val local = codes[key(actor, orderId)]?.takeIf { it.revision == committed.getLong("revision") }
        OrderPickupVerification(farmerConfirmed, committed.getBoolean("supplierConfirmed") == true,
            !committed.exists() && latestOrder.status != OrderStatus.AWAITING_PICKUP,
            if (actor == latestOrder.supplierId && !farmerConfirmed) local?.code else null,
            if (actor == latestOrder.supplierId) expiry else null)
    }

    override suspend fun complete(orderId: String) {
        val actor = uid()
        val ref = orderRef(orderId)
        remote.runTransaction { tx ->
            val snapshot = tx.get(ref)
            val order = snapshot.toObject(Order::class.java) ?: error("Order is unavailable")
            check(actor == order.supplierId) { "Only the Supplier can confirm delivery" }
            if (order.status == OrderStatus.DELIVERED) return@runTransaction
            check(order.status == OrderStatus.IN_TRANSIT) { "Confirm pickup before delivery" }
            val state = tx.get(sessionRef(orderId))
            val eligible = snapshot.getLong("sparkVersion") == 1L || state.getBoolean("supplierConfirmed") == true
            val farmer = if (eligible) tx.get(db.collection("users").document(order.farmerId)) else null
            val supplier = if (eligible) tx.get(db.collection("users").document(order.supplierId)) else null
            tx.update(ref, mapOf("status" to "DELIVERED", "updatedAt" to System.currentTimeMillis(),
                "deliveredAt" to FieldValue.serverTimestamp()))
            if (eligible) {
                SparkScore.award(tx, db, order, requireNotNull(farmer), "farmer_delivery", 6)
                SparkScore.award(tx, db, order, requireNotNull(supplier), "supplier_delivery", 6)
            }
        }
    }

    override suspend fun cancel(orderId: String) {
        val actor = uid()
        val ref = orderRef(orderId)
        remote.runTransaction { tx ->
            val order = tx.get(ref).toObject(Order::class.java) ?: error("Order is unavailable")
            participant(order, actor)
            if (order.status == OrderStatus.CANCELLED) return@runTransaction
            check(order.status == OrderStatus.AWAITING_PICKUP) { "Orders cannot be cancelled after pickup" }
            val listingRef = db.collection("listings").document(order.listingId)
            val listing = tx.get(listingRef).toObject(HarvestListing::class.java) ?: error("The listing for this order is unavailable. Please try cancelling again later.")
            val profile = tx.get(db.collection("users").document(actor))
            check(listing.soldQuantity >= order.quantity) { "Reserved stock needs attention before cancellation. Please try again later." }
            val restored = listing.releaseSale(order.quantity, restoredBiddingMode = order.bidId.isNotBlank())
            tx.update(listingRef, mapOf("availableQuantity" to restored.availableQuantity,
                "soldQuantity" to restored.soldQuantity, "status" to restored.status.name,
                "biddingEnabled" to restored.biddingEnabled, "stockOrderId" to orderId))
            tx.update(ref, mapOf("status" to "CANCELLED", "updatedAt" to System.currentTimeMillis(),
                "cancelledAt" to FieldValue.serverTimestamp(), "cancelledBy" to actor))
            SparkScore.award(tx, db, order, profile, "cancellation", -2)
        }
    }
    companion object { const val DAY = 86_400_000L; const val WINDOW = 1_800_000L }
}
