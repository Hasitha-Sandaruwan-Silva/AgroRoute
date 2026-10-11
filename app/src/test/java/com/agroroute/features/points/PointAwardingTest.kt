package com.agroroute.features.points

import com.agroroute.core.database.OrderDao
import com.agroroute.core.database.UserProfileDao
import com.agroroute.core.model.*
import com.agroroute.core.remote.FirestoreService
import com.agroroute.core.remote.OrderLifecycle
import com.agroroute.core.repository.AuthRepository
import com.agroroute.core.repository.OrderRepositoryImpl
import com.agroroute.core.util.Result
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentSnapshot
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class PointAwardingTest {
    private val dao = mockk<OrderDao>(relaxed = true)
    private val users = mockk<UserProfileDao>(relaxed = true)
    private val remote = mockk<FirestoreService>(relaxed = true)
    private val completion = mockk<OrderLifecycle>()
    private fun repository(role: UserRole = UserRole.SUPPLIER): OrderRepositoryImpl {
        val auth = mockk<AuthRepository>()
        every { auth.currentUserId } returns "supplier-1"
        every { auth.getCurrentUserProfile() } returns flowOf(Result.Success(UserProfile(userId = "supplier-1", role = role)))
        return OrderRepositoryImpl(dao, remote, mockk(relaxed = true), mockk(relaxed = true),
            mockk(relaxed = true), auth, mockk(relaxed = true), users, mockk(relaxed = true), completion)
    }
    @Test fun `completion delegates to trusted server and caches trusted points without client transaction`() = runTest {
        val order = Order(orderId = "order-1", farmerId = "farmer-1", supplierId = "supplier-1", status = OrderStatus.DELIVERED)
        coEvery { completion.complete("order-1") } returns Unit
        val firestore = mockk<FirebaseFirestore>(relaxed = true)
        every { remote.firestore } returns firestore
        val orderSnapshot = mockk<DocumentSnapshot>()
        every { firestore.collection("orders").document("order-1").get() } returns Tasks.forResult(orderSnapshot)
        every { orderSnapshot.toObject(Order::class.java) } returns order
        for ((id, points) in listOf("farmer-1" to 6, "supplier-1" to 3)) {
            val snapshot = mockk<DocumentSnapshot>()
            every { firestore.collection("users").document(id).get() } returns Tasks.forResult(snapshot)
            every { snapshot.toObject(UserProfile::class.java) } returns UserProfile(userId = id, points = points)
        }
        assertTrue(repository().updateOrderStatus("order-1", OrderStatus.DELIVERED) is Result.Success)
        coVerify(exactly = 1) { completion.complete("order-1") }
        coVerify { dao.upsertOrder(order); users.upsertUserProfile(match { it.userId == "farmer-1" && it.points == 6 }) }
        coVerify(exactly = 0) { remote.runTransaction<Any>(any()) }
    }
    @Test fun `failed server completion never invents delivered order or points in cache`() = runTest {
        coEvery { completion.complete(any()) } throws IllegalStateException("Order must be in transit")
        assertTrue(repository().updateOrderStatus("order-1", OrderStatus.DELIVERED) is Result.Error)
        coVerify(exactly = 0) { dao.upsertOrder(any()); users.upsertUserProfile(any()) }
    }
    @Test fun `farmer cannot invoke supplier completion`() = runTest {
        assertTrue(repository(UserRole.FARMER).updateOrderStatus("order-1", OrderStatus.DELIVERED) is Result.Error)
        coVerify(exactly = 0) { completion.complete(any()) }
    }
}
