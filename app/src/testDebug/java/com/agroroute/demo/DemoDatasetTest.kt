package com.agroroute.demo

import com.agroroute.core.location.SelectedLocation
import com.agroroute.core.location.usableDeviceCoordinates
import com.agroroute.core.model.*
import org.junit.Assert.*
import org.junit.Test

class DemoDatasetTest {
    private val now = 1_800_000_000_000L
    private val data = DemoDataset.create(now)
    @Test fun deterministicIdsAndRecords() { assertEquals(data, DemoDataset.create(now)) }
    @Test fun lightweightCompleteDataset() {
        assertEquals(6,data.profiles.size); assertEquals(10,data.listings.size)
        assertEquals(9,data.bids.size); assertEquals(6,data.requests.size)
        assertEquals(7,data.orders.size); assertEquals(4,data.reviews.size)
    }
    @Test fun everyPrimaryKeyUsesLiteralDemoNamespaceAndIsUnique() {
        val ids = data.profiles.map { it.userId } + data.listings.map { it.listingId } +
            data.bids.map { it.bidId } + data.requests.map { it.requestId } + data.reviews.map { it.reviewId }
        assertTrue(ids.all { it.startsWith("demo_") }); assertEquals(ids.size,ids.toSet().size)
        assertEquals(data.orders.size,data.orders.map { it.orderId }.toSet().size)
    }
    @Test fun listingsHaveFarmerOwnersAndRequiredStates() {
        data.listings.forEach { l -> assertEquals(UserRole.FARMER,data.profiles.single { it.userId == l.farmerId }.role) }
        assertTrue(data.listings.any { it.isExpired(now) })
        assertTrue(data.listings.any { it.status == ListingStatus.FULFILLED })
        assertTrue(data.listings.any { it.harvestStatus == HarvestStatus.EXPECTED && it.expectedHarvestAt!! > now })
        assertTrue(data.listings.any { it.biddingEnabled }); assertTrue(data.listings.any { !it.biddingEnabled })
    }
    @Test fun proposalsHaveCorrectOwnersModesAndQuantities() {
        data.bids.forEach { b ->
            val l = data.listings.single { it.listingId == b.listingId }
            assertEquals(UserRole.SUPPLIER,data.profiles.single { it.userId == b.supplierId }.role)
            assertTrue(b.requestedQuantity!! > 0 && b.requestedQuantity <= l.quantity)
        }
        data.requests.forEach { r ->
            val l = data.listings.single { it.listingId == r.listingId }
            assertEquals(l.farmerId,r.farmerId); assertEquals(l.reservePrice,r.listedPrice,0.0)
            assertFalse(l.biddingEnabled); assertTrue(r.requestedQuantity <= l.quantity)
            assertEquals(UserRole.SUPPLIER,data.profiles.single { it.userId == r.supplierId }.role)
        }
    }
    @Test fun tomatoCompetingBidsRemainPendingAt100Kg() {
        val l = data.listings.first(); val bids = data.bids.filter { it.listingId == l.listingId && it.status == BidStatus.PENDING }
        assertEquals(listOf(40.0,30.0,70.0),bids.map { it.requestedQuantity })
        assertEquals(100.0,l.availableStock(),0.0)
    }
    @Test fun acceptedProposalsHaveExactDeterministicOrdersAndSnapshots() {
        data.orders.forEach { o ->
            val l = data.listings.single { it.listingId == o.listingId }
            assertEquals(l.farmerId,o.farmerId); assertEquals(l.cropName,o.cropName); assertEquals(l.location,o.pickupLocation)
            if (o.bidId.isNotEmpty()) {
                val b = data.bids.single { it.bidId == o.orderId }
                assertEquals(BidStatus.ACCEPTED,b.status); assertEquals(b.bidId,o.bidId)
                assertEquals(b.requestedQuantity!!,o.quantity,0.0); assertEquals(b.offerPrice,o.agreedPrice,0.0)
                assertEquals(b.supplierId,o.supplierId); assertEquals(b.deliveryLocation,o.deliveryLocation)
            } else {
                val r = data.requests.single { it.requestId == o.orderId }
                assertEquals(OrderRequestStatus.ACCEPTED,r.status)
                assertEquals(r.requestedQuantity,o.quantity,0.0); assertEquals(r.listedPrice,o.agreedPrice,0.0)
                assertEquals(r.supplierId,o.supplierId); assertEquals(r.deliveryLocation,o.deliveryLocation)
            }
        }
        assertEquals(data.orders.size,data.bids.count { it.status == BidStatus.ACCEPTED } + data.requests.count { it.status == OrderRequestStatus.ACCEPTED })
    }
    @Test fun stockLedgerExcludesCancelledReservations() {
        data.listings.filter { it.harvestStatus == HarvestStatus.READY }.forEach { l ->
            val reserved = data.orders.filter { it.listingId == l.listingId && it.status != OrderStatus.CANCELLED }.sumOf { it.quantity }
            assertEquals(reserved,l.soldQuantity,0.0); assertEquals(l.quantity-reserved,l.availableStock(),0.0)
            assertTrue(l.availableStock() >= 0)
        }
        assertEquals(OrderStatus.entries.toSet(),data.orders.map { it.status }.toSet())
    }
    @Test fun everySavedLocationHasBoundedNonDefaultCoordinates() {
        val locations = data.profiles.map { it.location } + data.listings.map { it.location } +
            data.bids.map { it.deliveryLocation } + data.requests.map { it.deliveryLocation } +
            data.orders.flatMap { listOf(it.pickupLocation,it.deliveryLocation) }
        locations.forEach { value ->
            val pin = SelectedLocation.fromStoredText(value)!!
            assertTrue(usableDeviceCoordinates(pin.latitude,pin.longitude))
            assertTrue(pin.latitude in 6.0..8.0 && pin.longitude in 79.0..81.0)
            assertEquals(value,pin.toStoredText())
        }
    }
    @Test fun reviewsOnlyDeliveredAndOnePerParticipant() {
        assertEquals(data.reviews.size,data.reviews.map { it.orderId to it.reviewerId }.toSet().size)
        data.reviews.forEach { r ->
            val o = data.orders.single { it.orderId == r.orderId }
            assertEquals(OrderStatus.DELIVERED,o.status); assertTrue(r.rating in 1..5)
            assertEquals(setOf(o.farmerId,o.supplierId),setOf(r.reviewerId,r.revieweeId))
            assertEquals("${o.orderId}_${r.reviewerId}",r.reviewId)
        }
    }
}
