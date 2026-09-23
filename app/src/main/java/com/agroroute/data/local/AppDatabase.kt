package com.agroroute.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.agroroute.data.local.converters.Converters
import com.agroroute.data.local.dao.BidDao
import com.agroroute.data.local.dao.HarvestListingDao
import com.agroroute.data.local.dao.OrderDao
import com.agroroute.data.local.dao.UserProfileDao
import com.agroroute.data.model.Bid
import com.agroroute.data.model.HarvestListing
import com.agroroute.data.model.Order
import com.agroroute.data.model.UserProfile
import com.agroroute.util.Constants

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
