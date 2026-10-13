package com.agroroute

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.agroroute.core.database.AppDatabase
import com.agroroute.core.database.DatabaseMigrations
import com.agroroute.core.model.*
import com.agroroute.demo.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class DemoDatabaseTest {
    private lateinit var db: AppDatabase
    @Before fun open() {
        db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext,AppDatabase::class.java).build()
    }
    @After fun close() { db.close() }
    @Test fun firstSeedPersistsEveryEntityThroughExistingDaos() = runBlocking {
        DemoDataSeeder(db).loadDemoData()
        assertEquals(10,db.harvestListingDao().getAllListings().first().size)
        assertEquals(7,db.orderDao().getAllOrders().first().size)
        assertEquals(3,db.userProfileDao().getUsersByRoleSortedByPoints("FARMER").first().size)
        assertEquals(3,db.userProfileDao().getUsersByRoleSortedByPoints("SUPPLIER").first().size)
        DemoDataset.create(System.currentTimeMillis()).bids.forEach { assertNotNull(db.bidDao().getBidById(it.bidId).first()) }
        DemoDataset.create(System.currentTimeMillis()).requests.forEach { assertNotNull(db.orderRequestDao().getRequestById(it.requestId).first()) }
        assertEquals(4,db.orderDao().getAllOrders().first().sumOf { db.reviewDao().getReviewsByOrderId(it.orderId).first().size })
    }
    @Test fun repeatedSeedPreservesRowsDeadlinesAndSnapshots() = runBlocking {
        var clock = 1_800_000_000_000L
        val seeder = DemoDataSeeder(db) { clock }
        seeder.loadDemoData()
        val listings = db.harvestListingDao().getAllListings().first()
        val orders = db.orderDao().getAllOrders().first()
        clock += DemoDataset.DAY
        seeder.loadDemoData()
        assertEquals(listings,db.harvestListingDao().getAllListings().first())
        assertEquals(orders,db.orderDao().getAllOrders().first())
    }
    @Test fun clearOnlyLiteralNamespacePreservesNonDemoRowsInEveryTable() = runBlocking {
        val id = "demoXreal" // would be damaged by an unescaped SQL LIKE 'demo_%'.
        db.userProfileDao().upsertUserProfile(UserProfile(userId=id))
        db.harvestListingDao().upsertListing(HarvestListing(listingId=id))
        db.bidDao().upsertBid(Bid(bidId=id))
        db.orderRequestDao().upsertRequest(OrderRequest(requestId=id))
        db.orderDao().upsertOrder(Order(orderId=id))
        db.reviewDao().upsertReview(Review(reviewId=id))
        db.orderCompletionRecordDao().upsertRecord(OrderCompletionRecord(orderId=id))
        val seeder = DemoDataSeeder(db); seeder.loadDemoData(); seeder.clearDemoData(); seeder.clearDemoData()
        assertEquals(listOf(id),db.harvestListingDao().getAllListings().first().map { it.listingId })
        assertEquals(listOf(id),db.orderDao().getAllOrders().first().map { it.orderId })
        assertNotNull(db.userProfileDao().getUserProfile(id).first()); assertNotNull(db.bidDao().getBidById(id).first())
        assertNotNull(db.orderRequestDao().getRequestById(id).first()); assertNotNull(db.reviewDao().getReviewById(id).first())
        assertNotNull(db.orderCompletionRecordDao().getRecordByOrderIdSync(id))
        val tables = listOf("user_profiles","harvest_listings","bids","order_requests","orders","reviews","order_completion_records")
        tables.forEach { table -> db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM $table").use { it.moveToFirst(); assertEquals(1,it.getInt(0)) } }
    }
    @Test fun emptyDatabaseDoesNotAutomaticallySeedAndUsesVersionEight() = runBlocking {
        assertTrue(db.harvestListingDao().getAllListings().first().isEmpty())
        assertEquals(8,db.openHelper.readableDatabase.version)
        assertNotEquals(com.agroroute.core.util.Constants.DATABASE_NAME,DemoDatabase.NAME)
    }
    @Test fun roomReopensVersionSevenCacheWithPreservedLegacyBid() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "local-demo-migration-test"
        context.deleteDatabase(name) // Dedicated test file, never either application database.
        try {
            val before = Room.databaseBuilder(context,AppDatabase::class.java,name).build()
            before.bidDao().upsertBid(Bid(bidId="legacy",supplierName="Preserved",requestedQuantity=null))
            val sqlite = before.openHelper.writableDatabase
            sqlite.execSQL("ALTER TABLE bids DROP COLUMN requestedQuantity")
            sqlite.execSQL("DELETE FROM room_master_table")
            sqlite.version = 7
            before.close()
            val after = Room.databaseBuilder(context,AppDatabase::class.java,name)
                .addMigrations(DatabaseMigrations.MIGRATION_7_8).build()
            try {
                val preserved = after.bidDao().getBidById("legacy").first()!!
                assertEquals("Preserved",preserved.supplierName)
                assertNull(preserved.requestedQuantity)
                assertEquals(8,after.openHelper.readableDatabase.version)
                DemoDataSeeder(after).loadDemoData()
                DemoDataSeeder(after).clearDemoData()
                assertNotNull(after.bidDao().getBidById("legacy").first())
            } finally { after.close() }
        } finally { context.deleteDatabase(name) }
    }
    @Test fun migrationSevenToEightPreservesLegacyBidAndAddsNullableQuantity() {
        val sqlite = db.openHelper.writableDatabase
        sqlite.execSQL("CREATE TABLE legacy_bids AS SELECT bidId, listingId, supplierId, supplierName, offerPrice, message, status, createdAt, updatedAt, deliveryLocation FROM bids")
        sqlite.execSQL("DROP TABLE bids")
        sqlite.execSQL("ALTER TABLE legacy_bids RENAME TO bids")
        sqlite.execSQL("INSERT INTO bids VALUES ('legacy','listing','supplier','Name',320,'','PENDING',1,1,'address')")
        DatabaseMigrations.MIGRATION_7_8.migrate(sqlite)
        sqlite.query("SELECT bidId, requestedQuantity FROM bids").use { assertTrue(it.moveToFirst()); assertEquals("legacy",it.getString(0)); assertTrue(it.isNull(1)) }
    }
}
