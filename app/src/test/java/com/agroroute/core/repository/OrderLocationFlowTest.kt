package com.agroroute.core.repository

import com.agroroute.core.database.*
import com.agroroute.core.location.SelectedLocation
import com.agroroute.core.location.navigationCoordinate
import com.agroroute.core.model.*
import com.agroroute.core.notification.NotificationService
import com.agroroute.core.remote.FirestoreService
import com.agroroute.core.remote.OrderLifecycle
import com.agroroute.core.util.Result
import com.google.firebase.firestore.*
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

class OrderLocationFlowTest {
    private val pickup = "North Farm\n[7.000000, 80.000000]"
    private val delivery = "Warehouse A\n[6.900000, 79.900000]"

    @Test fun snapshotsSurviveListingAndProfileChangesAndDifferBetweenOrders() {
        val listing = HarvestListing(location = pickup)
        val supplier = UserProfile(location = delivery)
        val first = Order().withLocationSnapshots(listing.location, supplier.location)
        val laterListing = listing.copy(location = "Other farm\n[8.000000, 81.000000]")
        val laterSupplier = supplier.copy(location = "Warehouse B\n[6.800000, 79.800000]")
        val second = Order().withLocationSnapshots(laterListing.location, laterSupplier.location)
        assertEquals(pickup, first.pickupLocation)
        assertEquals(delivery, first.deliveryLocation)
        assertEquals(7.0, first.pickupSnapshot()!!.latitude, 0.0)
        assertNotEquals(first.deliverySnapshot(), second.deliverySnapshot())
    }

    @Test fun oldAddressOnlyAndMalformedOrdersAreReadableWithoutInventedPins() {
        val old = Order(pickupLocation = "Old Farm", deliveryLocation = "Warehouse\n[999, 80]")
        assertNull(old.pickupSnapshot())
        assertNull(old.deliverySnapshot())
        assertEquals("Old Farm", old.pickupLocation)
    }

    @Test fun newOrderCannotOmitEitherMapDestination() {
        for ((pickupValue, deliveryValue) in listOf("Old Farm" to delivery, pickup to "", pickup to "Warehouse")) {
            assertTrue(runCatching { Order().withLocationSnapshots(pickupValue, deliveryValue) }.isFailure)
        }
    }

    @Test fun navigationCoordinatesUseSavedPinAndLocaleIndependentDecimalPoints() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            assertEquals("7.000000,80.000000", navigationCoordinate(SelectedLocation.fromStoredText(pickup)!!))
        } finally { Locale.setDefault(previous) }
    }

    @Test fun bidAcceptanceCopiesAuthoritativeLocationsRatherThanCachedOrderOrProfiles() = runTest {
        val fixture = Fixture()
        val listing = HarvestListing(listingId = "listing-1", farmerId = "farmer-1", quantity = 10.0, location = pickup)
        every { fixture.listingSnapshot.toObject(HarvestListing::class.java) } returns listing
        every { fixture.proposalSnapshot.toObject(Bid::class.java) } returns Bid(
            bidId = "proposal-1", listingId = "listing-1", supplierId = "supplier-1", deliveryLocation = delivery)
        val stale = Order(orderId = "proposal-1", bidId = "proposal-1", listingId = "listing-1",
            farmerId = "farmer-1", supplierId = "supplier-1", quantity = 10.0,
            pickupLocation = "Wrong cached pickup", deliveryLocation = "Wrong cached delivery")
        assertTrue(fixture.repository.acceptBidAndCreateOrder(stale) is Result.Success)
        val stored = fixture.writtenOrder.captured
        assertEquals(pickup, stored.pickupLocation)
        assertEquals(delivery, stored.deliveryLocation)
        coVerify { fixture.orderDao.upsertOrder(stored) }
        coVerify { fixture.users.upsertUserProfile(match { it.userId == "farmer-1" && it.points == 1 }) }
    }

    @Test fun requestAcceptanceCopiesDeliveryChosenBeforeFarmerAcceptance() = runTest {
        val fixture = Fixture()
        every { fixture.listingSnapshot.toObject(HarvestListing::class.java) } returns HarvestListing(
            listingId = "listing-1", farmerId = "farmer-1", quantity = 10.0, biddingEnabled = false, location = pickup)
        every { fixture.proposalSnapshot.toObject(OrderRequest::class.java) } returns OrderRequest(
            requestId = "proposal-1", listingId = "listing-1", farmerId = "farmer-1", supplierId = "supplier-1",
            requestedQuantity = 5.0, deliveryLocation = delivery)
        assertTrue(fixture.repository.acceptOrderRequestAndCreateOrder("proposal-1") is Result.Success)
        assertEquals(pickup, fixture.writtenOrder.captured.pickupLocation)
        assertEquals(delivery, fixture.writtenOrder.captured.deliveryLocation)
        coVerify { fixture.users.upsertUserProfile(match { it.userId == "farmer-1" && it.points == 1 }) }
    }

    @Test fun legacyRequestWithoutDeliveryDoesNotWriteOrderOrConsumeStock() = runTest {
        val fixture = Fixture()
        every { fixture.listingSnapshot.toObject(HarvestListing::class.java) } returns HarvestListing(
            listingId = "listing-1", farmerId = "farmer-1", quantity = 10.0, biddingEnabled = false, location = pickup)
        every { fixture.proposalSnapshot.toObject(OrderRequest::class.java) } returns OrderRequest(
            requestId = "proposal-1", listingId = "listing-1", farmerId = "farmer-1", supplierId = "supplier-1",
            requestedQuantity = 5.0)
        assertTrue(fixture.repository.acceptOrderRequestAndCreateOrder("proposal-1") is Result.Error)
        verify(exactly = 0) { fixture.transaction.set(any<DocumentReference>(), any<Any>()) }
        coVerify(exactly = 0) { fixture.orderDao.upsertOrder(any()) }
    }

    private class Fixture {
        val firebase = mockk<FirebaseFirestore>(relaxed = true)
        val service = mockk<FirestoreService>(relaxed = true)
        val transaction = mockk<Transaction>(relaxed = true)
        val orderDao = mockk<OrderDao>(relaxed = true)
        val users = mockk<UserProfileDao>(relaxed = true)
        val listingSnapshot = mockk<DocumentSnapshot>()
        val proposalSnapshot = mockk<DocumentSnapshot>()
        val listingRef = mockk<DocumentReference>()
        val proposalRef = mockk<DocumentReference>()
        val orderRef = mockk<DocumentReference>()
        val writtenOrder = slot<Order>()
        val auth = mockk<AuthRepository>()
        val repository: OrderRepositoryImpl
        init {
            every { service.firestore } returns firebase
            every { firebase.collection("listings").document("listing-1") } returns listingRef
            every { firebase.collection("bids").document("proposal-1") } returns proposalRef
            every { firebase.collection("order_requests").document("proposal-1") } returns proposalRef
            every { firebase.collection("orders").document("proposal-1") } returns orderRef
            every { transaction.get(listingRef) } returns listingSnapshot
            every { transaction.get(proposalRef) } returns proposalSnapshot
            every { transaction.set(orderRef, capture(writtenOrder)) } returns transaction
            val farmerRef = mockk<DocumentReference>()
            val farmerSnapshot = mockk<DocumentSnapshot>()
            every { firebase.collection("users").document("farmer-1") } returns farmerRef
            every { transaction.get(farmerRef) } returns farmerSnapshot
            every { farmerSnapshot.id } returns "farmer-1"
            every { farmerSnapshot.reference } returns farmerRef
            every { farmerSnapshot.getLong("points") } returns 0L
            every { farmerSnapshot.getString("role") } returns "FARMER"
            every { farmerSnapshot.toObject(UserProfile::class.java) } returns UserProfile(userId = "farmer-1", role = UserRole.FARMER)
            every { auth.currentUserId } returns "farmer-1"
            every { auth.getCurrentUserProfile() } returns flowOf(Result.Success(UserProfile(
                userId = "farmer-1", role = UserRole.FARMER, location = "Changed profile location")))
            coEvery { service.runTransaction<Any>(any()) } coAnswers {
                firstArg<(Transaction) -> Any>().invoke(transaction)
            }
            repository = OrderRepositoryImpl(orderDao, service, mockk(relaxed = true), mockk(relaxed = true),
                mockk(relaxed = true), auth, mockk(relaxed = true), users,
                mockk<NotificationService>(relaxed = true), mockk<OrderLifecycle>(relaxed = true))
        }
    }
}
