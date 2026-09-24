package com.agroroute.common.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.agroroute.auth.UserProfileDao
import com.agroroute.common.model.Bid
import com.agroroute.common.model.HarvestListing
import com.agroroute.common.model.Order
import com.agroroute.common.model.UserProfile
import com.agroroute.common.util.Constants
import com.agroroute.farmer.HarvestListingDao
import com.agroroute.common.order.OrderDao
import com.agroroute.supplier.BidDao

/**
 * Room database definition that aggregates all entities, DAOs, and type converters for local persistence.
 */
@Database(
    entities = [
        UserProfile::class,
        HarvestListing::class,
        Bid::class,
        Order::class
    ],
    version = Constants.DATABASE_VERSION,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userProfileDao(): UserProfileDao
    abstract fun harvestListingDao(): HarvestListingDao
    abstract fun bidDao(): BidDao
    abstract fun orderDao(): OrderDao
}
