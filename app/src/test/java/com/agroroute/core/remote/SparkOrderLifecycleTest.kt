package com.agroroute.core.remote

import com.agroroute.core.model.*
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.*
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Test

/** Real Kotlin protocol against an acknowledged transaction double; Rules tested independently in emulator. */
class SparkOrderLifecycleTest {
    @After fun cleanup() { unmockkStatic(FirebaseAuth::class) }
    private class Fixture {
        var actor = "s"
        val rows = mutableMapOf<String, MutableMap<String, Any?>>()
        val remote = mockk<FirestoreService>()
        val auth = mockk<FirebaseAuth>()
        val db = mockk<FirebaseFirestore>()
        val tx = mockk<Transaction>(relaxed = true)
        val refs = mutableMapOf<String, DocumentReference>()
        var writing = false
        val writes = mutableListOf<() -> Unit>()
        val service: SparkOrderLifecycle
        init {
            val user = mockk<FirebaseUser>()
            mockkStatic(FirebaseAuth::class)
            every { FirebaseAuth.getInstance() } returns auth
            every { auth.currentUser } returns user
            every { user.uid } answers { actor }
            every { remote.firestore } returns db
            every { db.collection(any()) } answers {
                val collection = firstArg<String>(); val result = mockk<CollectionReference>()
                every { result.document(any()) } answers { ref("$collection/${firstArg<String>()}") }
                result
            }
            every { tx.get(any()) } answers {
                check(!writing) { "Transaction read after write" }; snapshot(firstArg<DocumentReference>())
            }
            every { tx.set(any(), any<Any>()) } answers {
                writing = true; val target = firstArg<DocumentReference>().path
                @Suppress("UNCHECKED_CAST") val data = secondArg<Any>() as Map<String, Any?>
                writes += { rows[target] = resolved(data) }; tx
            }
            every { tx.update(any<DocumentReference>(), any<Map<String, Any?>>()) } answers {
                writing = true; val target = firstArg<DocumentReference>().path; val data = secondArg<Map<String, Any?>>()
                writes += { rows.getValue(target).putAll(resolved(data)) }; tx
            }
            coEvery { remote.runTransaction<Any?>(any()) } coAnswers {
                writing = false; writes.clear()
                val result = firstArg<(Transaction) -> Any?>()(tx)
                writes.forEach { it() }; result
            }
            rows["orders/o"] = mutableMapOf("orderId" to "o", "listingId" to "l", "bidId" to "", "farmerId" to "f",
                "supplierId" to "s", "quantity" to 5.0, "status" to "AWAITING_PICKUP", "sparkVersion" to 1L)
            rows["users/f"] = mutableMapOf("userId" to "f", "role" to "FARMER", "points" to 1L)
            rows["users/s"] = mutableMapOf("userId" to "s", "role" to "SUPPLIER", "points" to 0L)
            rows["listings/l"] = mutableMapOf("listingId" to "l", "farmerId" to "f", "quantity" to 10.0,
                "soldQuantity" to 5.0, "availableQuantity" to 5.0, "status" to "OPEN", "biddingEnabled" to false)
            service = SparkOrderLifecycle(remote, auth)
        }
        private fun resolved(data: Map<String, Any?>) = data.mapValues { (_, v) ->
            if (v is FieldValue) Timestamp.now() else v }.toMutableMap()
        private fun ref(path: String): DocumentReference = refs.getOrPut(path) {
            val ref = mockk<DocumentReference>()
            every { ref.path } returns path; every { ref.id } returns path.substringAfter('/')
            every { ref.get(Source.SERVER) } answers { Tasks.forResult(snapshot(ref)) }; ref
        }
        private fun snapshot(ref: DocumentReference): DocumentSnapshot {
            val data = rows[ref.path]?.toMap(); val snapshot = mockk<DocumentSnapshot>()
            every { snapshot.exists() } returns (data != null)
            every { snapshot.id } returns ref.id; every { snapshot.reference } returns ref
            every { snapshot.getLong(any()) } answers { (data?.get(firstArg<String>()) as? Number)?.toLong() }
            every { snapshot.getString(any()) } answers { data?.get(firstArg<String>()) as? String }
            every { snapshot.getBoolean(any()) } answers { data?.get(firstArg<String>()) as? Boolean }
            every { snapshot.getTimestamp(any()) } answers { data?.get(firstArg<String>()) as? Timestamp }
            every { snapshot.toObject(Order::class.java) } answers { data?.let { Order(orderId = ref.id,
                listingId = it["listingId"] as String, farmerId = it["farmerId"] as String, supplierId = it["supplierId"] as String,
                quantity = (it["quantity"] as Number).toDouble(), status = OrderStatus.valueOf(it["status"] as String)) } }
            every { snapshot.toObject(HarvestListing::class.java) } answers { data?.let { HarvestListing(listingId = ref.id,
                farmerId = it["farmerId"] as String, quantity = (it["quantity"] as Number).toDouble(),
                soldQuantity = (it["soldQuantity"] as Number).toDouble(), availableQuantity = (it["availableQuantity"] as Number).toDouble(),
                status = ListingStatus.valueOf(it["status"] as String), biddingEnabled = it["biddingEnabled"] as Boolean) } }
            return snapshot
        }
        fun points(uid: String) = (rows.getValue("users/$uid").getValue("points") as Number).toInt()
        fun state() = rows.getValue("spark_pickup_sessions/o")
        suspend fun code() = service.pickup("o", "view").code!!
        suspend fun fails(block: suspend () -> Unit): String {
            return try { block(); fail("Expected rejection"); "" } catch (e: IllegalStateException) { e.message.orEmpty() }
        }
    }
    @Test fun supplierEnrollsWithoutFirestorePlaintextAndFarmerNeverReceivesCode() = runTest {
        val f = Fixture(); val pin = f.code()
        assertTrue(pin.matches(Regex("[0-9]{6}"))); assertFalse(f.state().values.contains(pin)); assertFalse(f.state().containsKey("code"))
        f.actor = "f"; assertNull(f.service.pickup("o","view").code); assertEquals(1,f.points("f"))
    }
    @Test fun wrongPinCommitsFailedAttemptButNeverAwardsPoints() = runTest {
        val f=Fixture(); val pin=f.code(); f.actor="f"
        val wrong=((pin.toInt()+1)%1000000).toString().padStart(6,'0')
        assertTrue(f.fails { f.service.pickup("o","verify",wrong) }.contains("Incorrect"))
        assertEquals(1L,(f.state()["attemptCount"] as Number).toLong()); assertEquals(1,f.points("f"))
    }
    @Test fun correctPinAwardsThreeOnceWithoutSupplierCollection() = runTest {
        val f=Fixture();val pin=f.code();f.actor="f"
        assertTrue(f.service.pickup("o","verify",pin).farmerConfirmed)
        f.service.pickup("o","verify",pin);assertEquals(4,f.points("f"));assertEquals("AWAITING_PICKUP",f.rows["orders/o"]!!["status"])
        assertEquals(1,f.rows.keys.count { it.startsWith("scoreEvents/") })
    }
    @Test fun independentSupplierPickupAndDeliveryAwardsExactEventsOnceAndAllowsLateFarmer() = runTest {
        val f=Fixture();val pin=f.code();f.service.pickup("o","collect"); f.service.pickup("o","collect")
        assertEquals(4,f.points("s"));assertEquals("IN_TRANSIT",f.rows["orders/o"]!!["status"])
        f.service.complete("o");f.service.complete("o");assertEquals(10,f.points("s"));assertEquals(7,f.points("f"))
        f.actor="f";f.service.pickup("o","verify",pin);assertEquals(10,f.points("f"))
    }
    @Test fun fiveWrongAttemptsSurviveRotationAndLockCorrectCode() = runTest {
        val f=Fixture();val pin=f.code();f.actor="f";val wrong=((pin.toInt()+1)%1000000).toString().padStart(6,'0')
        repeat(5) { f.fails { f.service.pickup("o","verify",wrong) } }
        f.actor="s";val next=f.service.pickup("o","refresh").code!!;f.actor="f"
        assertTrue(f.fails { f.service.pickup("o","verify",next) }.contains("30 minutes")); assertEquals(5L,(f.state()["attemptCount"] as Number).toLong())
    }
    @Test fun restartAndExplicitRotationInvalidateOldVerifierWithoutLosingAttempts() = runTest {
        val f=Fixture();f.code();val previous=f.state()["verifier"];val restarted=SparkOrderLifecycle(f.remote, f.auth)
        assertNotNull(restarted.pickup("o","view").code);assertNotEquals(previous,f.state()["verifier"])
        assertEquals(2L,(f.state()["revision"] as Number).toLong())
    }
    @Test fun expiredCodeCannotVerifyOrCollectUntilSupplierRefreshes() = runTest {
        val f=Fixture();val pin=f.code();f.state()["issuedAt"]=Timestamp(java.util.Date(System.currentTimeMillis()-SparkOrderLifecycle.DAY-1000))
        f.actor="f";assertTrue(f.fails { f.service.pickup("o","verify",pin) }.contains("expired"));f.actor="s"
        assertTrue(f.fails { f.service.pickup("o","collect") }.contains("expired"));f.service.pickup("o","refresh");f.service.pickup("o","collect")
    }
    @Test fun cancellationRestoresReservedStockOnceAndFloorsActorPoints() = runTest {
        val f=Fixture();f.actor="f";f.service.cancel("o");f.service.cancel("o")
        assertEquals(0,f.points("f"));assertEquals(0,f.points("s"));assertEquals(10.0,f.rows["listings/l"]!!["availableQuantity"])
        assertEquals(0.0,f.rows["listings/l"]!!["soldQuantity"])
    }
    @Test fun unrelatedUserCannotEnrollVerifyCollectDeliverOrCancel() = runTest {
        val f=Fixture();f.code();f.actor="other"
        for(action in listOf("view","refresh","verify","collect")) f.fails { f.service.pickup("o",action,"123456") }
        f.fails { f.service.complete("o") };f.fails { f.service.cancel("o") };assertEquals(0,f.points("s"))
    }
    @Test fun participantCannotPerformOtherRoleActionOrSkipDeliveryOrCancelAfterPickup() = runTest {
        val f=Fixture();f.code();f.fails { f.service.pickup("o","verify","123456") };f.fails { f.service.complete("o") }
        f.actor="f";f.fails { f.service.pickup("o","collect") };f.actor="s";f.service.pickup("o","collect");f.fails { f.service.cancel("o") }
    }
    @Test fun legacyTransitCompletionDoesNotRetroactivelyAwardPointsOrEnroll() = runTest {
        val f=Fixture();f.rows["orders/o"]!!.remove("sparkVersion");f.rows["orders/o"]!!["status"]="IN_TRANSIT"
        assertTrue(f.service.pickup("o","view").legacy); f.service.complete("o");assertEquals(1,f.points("f"));assertEquals(0,f.points("s"))
    }
    @Test fun offlineTransactionFailureCannotPretendSuccessOrAwardPoints() = runTest {
        val f=Fixture();f.code()
        coEvery { f.remote.runTransaction<Any?>(any()) } throws java.io.IOException("Connection unavailable")
        try { f.service.pickup("o","collect");fail("Offline collection must fail") }
        catch (_: java.io.IOException) { }
        assertEquals("AWAITING_PICKUP",f.rows["orders/o"]!!["status"]);assertEquals(0,f.points("s"))
    }
}
