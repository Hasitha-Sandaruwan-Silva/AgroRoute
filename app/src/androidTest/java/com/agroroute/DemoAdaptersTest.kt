package com.agroroute
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.agroroute.core.database.AppDatabase
import com.agroroute.core.model.*
import com.agroroute.core.repository.*
import com.agroroute.core.util.Result
import com.agroroute.demo.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*

class DemoAdaptersTest {
    private lateinit var db: AppDatabase
    private lateinit var manager: DemoSessionManager
    private lateinit var adapters: DemoRepositories
    @Before fun open() = runBlocking {
        db=Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext,AppDatabase::class.java).build()
        DemoDataSeeder(db).loadDemoData(); manager=DemoSessionManager(); manager.select(UserRole.FARMER); adapters=DemoRepositories(db,manager)
    }
    @After fun close() { db.close() }
    @Test fun sessionReadsOnlySelectedRoomProfileWithoutChangingFirebaseAuth() = runBlocking {
        val firebase = com.google.firebase.auth.FirebaseAuth.getInstance(); val uid = firebase.currentUser?.uid
        val auth=adapters.repository(AuthRepository::class.java)
        assertEquals("demo_farmer_001",auth.currentUserId); assertTrue(auth.isLoggedIn)
        assertEquals("Nimal Perera",auth.restoreSession().getOrNull()?.displayName)
        manager.select(UserRole.SUPPLIER)
        assertEquals("demo_supplier_001",auth.currentUserId)
        assertEquals("FreshMart Traders",auth.getCurrentUserProfile().first().getOrNull()?.displayName)
        assertTrue(auth.logout() is Result.Error); assertTrue(auth.login("fake","fake") is Result.Error)
        assertEquals(uid,firebase.currentUser?.uid)
    }
    @Test fun allReadAdaptersUseExistingDemoRoomRows() = runBlocking {
        val listings=adapters.repository(HarvestListingRepository::class.java)
        assertEquals(10,listings.getAllListings().first().getOrNull()!!.size)
        val bids=adapters.repository(BidRepository::class.java).getBidsForListing("demo_listing_001").first().getOrNull()!!
        assertEquals(listOf(40.0,30.0,70.0),bids.filter { it.status==BidStatus.PENDING }.map { it.requestedQuantity })
        assertTrue(adapters.repository(OrderRequestRepository::class.java).getRequestsForFarmer("demo_farmer_001").first().getOrNull()!!.isNotEmpty())
        assertTrue(adapters.repository(OrderRepository::class.java).getOrdersForSupplier("demo_supplier_001").first().getOrNull()!!.isNotEmpty())
        assertEquals(2,adapters.repository(ReviewRepository::class.java).getReviewsForOrder("demo_request_002").first().getOrNull()!!.size)
        assertEquals(3,adapters.repository(LeaderboardRepository::class.java).getFarmerLeaderboard().first().getOrNull()!!.size)
    }
    @Test fun everyMutatingRepositoryMethodFailsClosedAndLeavesRowsUnchanged() = runBlocking {
        val before=db.harvestListingDao().getAllListings().first()
        val orders=db.orderDao().getAllOrders().first()
        val listing=adapters.repository(HarvestListingRepository::class.java)
        listOf(listing.saveListing(before.first()),listing.updateListing(before.first()),listing.saveDraft(before.first()),
            listing.deleteListing(before.first().listingId),listing.toggleBiddingMode(before.first().listingId,false),
            listing.markHarvestReady(before.first().listingId,200.0),listing.markHarvestFailed(before.first().listingId,"demo")).forEach { assertBlocked(it) }
        val bid=adapters.repository(BidRepository::class.java)
        assertBlocked(bid.placeBid(Bid())); assertBlocked(bid.withdrawBid("demo_bid_001")); assertBlocked(bid.updateBidStatus("demo_bid_001",BidStatus.ACCEPTED))
        val requests=adapters.repository(OrderRequestRepository::class.java)
        assertBlocked(requests.placeOrderRequest(OrderRequest())); assertBlocked(requests.cancelRequest("demo_request_005")); assertBlocked(requests.updateRequestStatus("demo_request_005",OrderRequestStatus.ACCEPTED))
        val order=adapters.repository(OrderRepository::class.java)
        assertBlocked(order.createOrder(orders.first())); assertBlocked(order.acceptBidAndCreateOrder(orders.first()))
        assertBlocked(order.acceptOrderRequestAndCreateOrder("demo_request_005")); assertBlocked(order.updateOrderStatus(orders.first().orderId,OrderStatus.DELIVERED))
        assertBlocked(order.updateDeliveryLocation(orders.first().orderId,"new")); assertBlocked(order.getPickupVerification(orders.first().orderId))
        assertBlocked(order.refreshPickupCode(orders.first().orderId)); assertBlocked(order.verifyPickupCode(orders.first().orderId,"123456"))
        assertBlocked(adapters.repository(ReviewRepository::class.java).submitReview(Review()))
        assertBlocked(adapters.repository(AuthRepository::class.java).updateProfile(UserProfile()))
        assertEquals(before,db.harvestListingDao().getAllListings().first()); assertEquals(orders,db.orderDao().getAllOrders().first())
    }
    @Test fun refreshesAreLocalReadsAndSnapshotDestinationsStayFixed() = runBlocking {
        val r=adapters.repository(OrderRepository::class.java)
        assertTrue(r.syncOrders("demo_supplier_001") is Result.Success)
        val o=r.getOrderById("demo_request_001").first().getOrNull()!!
        db.userProfileDao().upsertUserProfile(UserProfile(userId=o.farmerId,location="changed"))
        assertEquals(o,r.getOrderById(o.orderId).first().getOrNull())
    }
    @Test fun separateProductionCacheIsUntouchedByDemoRefreshAndClear() = runBlocking {
        val production=Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext,AppDatabase::class.java).build()
        try {
            production.harvestListingDao().upsertListing(HarvestListing(listingId="real-listing"))
            adapters.repository(HarvestListingRepository::class.java).syncListings()
            DemoDataSeeder(db).clearDemoData()
            assertEquals(listOf("real-listing"),production.harvestListingDao().getAllListings().first().map { it.listingId })
            assertNotEquals(com.agroroute.core.util.Constants.DATABASE_NAME,DemoDatabase.NAME)
        } finally { production.close() }
    }
    @Test fun localFactoryBuildsRealViewModelsAndRejectsRemoteDependencies() {
        val f=DemoViewModelFactory(InstrumentationRegistry.getInstrumentation().targetContext,adapters)
        val owner = object : androidx.lifecycle.ViewModelStoreOwner { override val viewModelStore = androidx.lifecycle.ViewModelStore() }
        val provider = androidx.lifecycle.ViewModelProvider(owner,f)
        try {
        listOf(com.agroroute.features.auth.AuthViewModel::class.java,
            com.agroroute.features.farmer.listing.ListingViewModel::class.java,
            com.agroroute.features.farmer.bids.BidsViewModel::class.java,
            com.agroroute.features.farmer.orderrequests.FarmerOrderRequestsViewModel::class.java,
            com.agroroute.features.farmer.orders.FarmerOrderViewModel::class.java,
            com.agroroute.features.supplier.marketplace.MarketplaceViewModel::class.java,
            com.agroroute.features.supplier.bids.SupplierBidsViewModel::class.java,
            com.agroroute.features.supplier.orders.SupplierOrderViewModel::class.java,
            com.agroroute.features.supplier.bidding.PlaceBidViewModel::class.java,
            com.agroroute.features.supplier.orderrequest.PlaceOrderRequestViewModel::class.java,
            com.agroroute.features.leaderboard.LeaderboardViewModel::class.java,
            com.agroroute.features.reviews.ReviewViewModel::class.java).forEach { assertNotNull(provider[it]) }
        try { f.create(com.agroroute.features.settings.NotificationInboxViewModel::class.java); fail("Remote service must not be constructed") }
        catch (_: IllegalStateException) { }
        } finally { owner.viewModelStore.clear() }
    }
    private fun assertBlocked(value: Result<*>) {
        assertTrue(value is Result.Error); assertEquals(DemoRepositories.UNAVAILABLE,(value as Result.Error).message)
    }
}
