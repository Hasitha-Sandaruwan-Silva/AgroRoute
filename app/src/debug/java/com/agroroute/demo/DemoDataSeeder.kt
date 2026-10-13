package com.agroroute.demo

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import com.agroroute.BuildConfig
import com.agroroute.core.database.AppDatabase
import com.agroroute.core.database.DatabaseMigrations
import kotlinx.coroutines.flow.first

/** Reuses the production schema/DAOs, but never its file, Hilt binding, session or remote repositories. */
object DemoDatabase {
    const val NAME = "agroroute_local_demo_db"
    fun open(context: Context): AppDatabase {
        check(BuildConfig.DEBUG)
        return Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, NAME)
            .addMigrations(DatabaseMigrations.MIGRATION_1_2, DatabaseMigrations.MIGRATION_2_3,
                DatabaseMigrations.MIGRATION_3_4, DatabaseMigrations.MIGRATION_4_5,
                DatabaseMigrations.MIGRATION_5_6, DatabaseMigrations.MIGRATION_6_7, DatabaseMigrations.MIGRATION_7_8)
            .build()
    }
}

class DemoDataSeeder(private val db: AppDatabase, private val clock: () -> Long = System::currentTimeMillis) {
    suspend fun loadDemoData() = db.withTransaction {
        check(BuildConfig.DEBUG)
        // Keep the first seed's clock anchor: repeated loads do not extend expiry or change snapshots.
        val anchor = db.harvestListingDao().getListingById("demo_listing_001").first()?.createdAt
            ?.plus(7 * DemoDataset.DAY) ?: clock()
        val data = DemoDataset.create(anchor)
        data.profiles.forEach { db.userProfileDao().upsertUserProfile(it) }
        db.harvestListingDao().upsertListings(data.listings)
        db.bidDao().upsertBids(data.bids)
        db.orderRequestDao().upsertRequests(data.requests)
        db.orderDao().upsertOrders(data.orders)
        db.reviewDao().upsertReviews(data.reviews)
    }

    suspend fun clearDemoData() = db.withTransaction {
        check(BuildConfig.DEBUG)
        // SQL identifiers are fixed; substr avoids LIKE's underscore wildcard matching real IDs.
        listOf("reviews" to "reviewId", "order_completion_records" to "orderId", "orders" to "orderId",
            "bids" to "bidId", "order_requests" to "requestId", "harvest_listings" to "listingId",
            "user_profiles" to "userId").forEach { (table, key) ->
            db.openHelper.writableDatabase.execSQL("DELETE FROM $table WHERE substr($key, 1, 5) = ?", arrayOf("demo_"))
        }
    }
}
