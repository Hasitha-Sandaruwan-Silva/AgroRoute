package com.agroroute.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.agroroute.core.model.Bid
import com.agroroute.core.model.HarvestListing
import com.agroroute.core.model.Order
import com.agroroute.core.model.UserProfile
import com.agroroute.core.util.Constants

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
