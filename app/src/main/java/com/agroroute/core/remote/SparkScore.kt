package com.agroroute.core.remote

import com.agroroute.core.model.Order
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Transaction

/** Rules require an exact new event and its eligible before/after transition. */
object SparkScore {
    fun eventId(orderId: String, type: String, uid: String = "") =
        if (type == "cancellation") "${orderId}_${uid}_cancellation" else "${orderId}_$type"
    fun award(tx: Transaction, db: FirebaseFirestore, order: Order, profile: DocumentSnapshot,
              type: String, delta: Int): Int {
        val uid = profile.id
        val points = profile.getLong("points") ?: 0L
        check(points in 0..Int.MAX_VALUE.toLong()) { "Account points are unavailable" }
        val id = eventId(order.orderId, type, uid)
        tx.set(db.collection("scoreEvents").document(id), mapOf(
            "orderId" to order.orderId, "userId" to uid, "role" to profile.getString("role"),
            "eventType" to type, "pointsDelta" to delta, "createdAt" to FieldValue.serverTimestamp()))
        val total = (points + delta).coerceIn(0, Int.MAX_VALUE.toLong()).toInt()
        tx.update(profile.reference, mapOf("points" to total,
            "lastScoreEventId" to id))
        return total
    }
}
