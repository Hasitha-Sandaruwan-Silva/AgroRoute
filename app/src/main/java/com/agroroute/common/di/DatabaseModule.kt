package com.agroroute.common.di

import android.content.Context
import androidx.room.Room
import com.agroroute.auth.UserProfileDao
import com.agroroute.common.database.AppDatabase
import com.agroroute.common.util.Constants
import com.agroroute.farmer.HarvestListingDao
import com.agroroute.common.order.OrderDao
import com.agroroute.supplier.BidDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that provides the Room [AppDatabase] singleton and all DAO instances.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            Constants.DATABASE_NAME
        )
            .fallbackToDestructiveMigration() // Acceptable for coursework; use migrations in prod
            .build()

    @Provides
    @Singleton
    fun provideUserProfileDao(db: AppDatabase): UserProfileDao = db.userProfileDao()

    @Provides
    @Singleton
    fun provideHarvestListingDao(db: AppDatabase): HarvestListingDao = db.harvestListingDao()

    @Provides
    @Singleton
    fun provideBidDao(db: AppDatabase): BidDao = db.bidDao()

    @Provides
    @Singleton
    fun provideOrderDao(db: AppDatabase): OrderDao = db.orderDao()
}
