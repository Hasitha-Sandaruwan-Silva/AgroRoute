package com.agroroute.core.di

import android.content.Context
import androidx.room.Room
import com.agroroute.core.database.DatabaseMigrations
import com.agroroute.core.database.UserProfileDao
import com.agroroute.core.database.AppDatabase
import com.agroroute.core.util.Constants
import com.agroroute.core.database.HarvestListingDao
import com.agroroute.core.database.OrderDao
import com.agroroute.core.database.BidDao
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
            .addMigrations(DatabaseMigrations.MIGRATION_1_2)
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
