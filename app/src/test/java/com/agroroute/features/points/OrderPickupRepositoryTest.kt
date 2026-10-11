package com.agroroute.features.points

import com.agroroute.core.database.OrderDao
import com.agroroute.core.database.UserProfileDao
import com.agroroute.core.model.*
import com.agroroute.core.remote.FirestoreService
import com.agroroute.core.remote.OrderCompletionService
import com.agroroute.core.repository.AuthRepository
import com.agroroute.core.repository.OrderRepositoryImpl
import com.agroroute.core.util.Result
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentSnapshot
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class OrderPickupRepositoryTest {
    private class Fixture(role: UserRole) {
        val dao = mockk<OrderDao>(relaxed = true)
        val users = mockk<UserProfileDao>(relaxed = true)
        val remote = mockk<FirestoreService>(relaxed = true)
        val completion = mockk<OrderCompletionService>()
        val firestore = mockk<FirebaseFirestore>(relaxed = true)
        val order = Order(orderId = "order-1", farmerId = "farmer-1", supplierId = "supplier-1",
            status = OrderStatus.IN_TRANSIT, pickupLocation = "Farm\n[7, 80]", deliveryLocation = "Destination\n[6, 79]")
        val repository: OrderRepositoryImpl
        init {
            val auth = mockk<AuthRepository>()
            val uid = if (role == UserRole.FARMER) "farmer-1" else "supplier-1"
            every { auth.currentUserId } returns uid
            every { auth.getCurrentUserProfile() } returns flowOf(Result.Success(UserProfile(userId = uid, role = role)))
            every { remote.firestore } returns firestore
            val orderSnapshot = mockk<DocumentSnapshot>()
            every { firestore.collection("orders").document("order-1").get() } returns Tasks.forResult(orderSnapshot)
            every { orderSnapshot.toObject(Order::class.java) } returns order
            for (id in listOf("farmer-1", "supplier-1")) {
                val snapshot = mockk<DocumentSnapshot>()
                every { firestore.collection("users").document(id).get() } returns Tasks.forResult(snapshot)
                every { snapshot.toObject(UserProfile::class.java) } returns UserProfile(userId = id, points = 7)
            }
            repository = OrderRepositoryImpl(dao, remote, mockk(relaxed = true), mockk(relaxed = true),
                mockk(relaxed = true), auth, mockk(relaxed = true), users, mockk(relaxed = true), completion)
        }
    }
    @Test fun farmerVerificationUsesTrustedBackendAndCachesExistingSnapshotsAndPoints() = runTest {
        val f = Fixture(UserRole.FARMER)
        val verified = OrderPickupVerification(farmerConfirmed = true)
        coEvery { f.completion.pickup("order-1", "verify", "123456") } returns verified
        assertEquals(verified, f.repository.verifyPickupCode("order-1", "123456").getOrNull())
        coVerify { f.dao.upsertOrder(f.order); f.users.upsertUserProfile(match { it.points == 7 }) }
        coVerify(exactly = 0) { f.remote.runTransaction<Any>(any()) }
    }
    @Test fun rejectedCodeNeverChangesLocalOrderOrPoints() = runTest {
        val f = Fixture(UserRole.FARMER)
        coEvery { f.completion.pickup(any(), "verify", any()) } throws IllegalStateException("Incorrect pickup code")
        assertTrue(f.repository.verifyPickupCode("order-1", "000000") is Result.Error)
        coVerify(exactly = 0) { f.dao.upsertOrder(any()); f.users.upsertUserProfile(any()) }
    }
    @Test fun supplierTransitActionUsesCollectionCallableRatherThanClientStatusWrite() = runTest {
        val f = Fixture(UserRole.SUPPLIER)
        coEvery { f.completion.pickup("order-1", "collect", null) } returns OrderPickupVerification(supplierConfirmed = true)
        assertTrue(f.repository.updateOrderStatus("order-1", OrderStatus.IN_TRANSIT) is Result.Success)
        coVerify(exactly = 1) { f.completion.pickup("order-1", "collect", null) }
        coVerify(exactly = 0) { f.remote.runTransaction<Any>(any()) }
    }
    @Test fun farmerCannotUseSupplierTransportAction() = runTest {
        val f = Fixture(UserRole.FARMER)
        assertTrue(f.repository.updateOrderStatus("order-1", OrderStatus.IN_TRANSIT) is Result.Error)
        coVerify(exactly = 0) { f.completion.pickup(any(), any(), any()) }
    }
    @Test fun codeViewingNeverWritesOrderOrCachedPoints() = runTest {
        val f = Fixture(UserRole.SUPPLIER)
        coEvery { f.completion.pickup("order-1", "view", null) } returns OrderPickupVerification(code = "123456")
        assertEquals("123456", f.repository.getPickupVerification("order-1").getOrNull()?.code)
        coVerify(exactly = 0) { f.dao.upsertOrder(any()); f.users.upsertUserProfile(any()) }
    }
}
