package com.agroroute.core.di

import com.agroroute.core.repository.HarvestListingRepository
import com.agroroute.core.repository.HarvestListingRepositoryImpl
import com.agroroute.core.repository.OrderRepository
import com.agroroute.core.repository.OrderRepositoryImpl
import com.agroroute.core.repository.BidRepository
import com.agroroute.core.repository.BidRepositoryImpl
import com.agroroute.core.repository.AuthRepository
import com.agroroute.core.repository.AuthRepositoryImpl
import com.agroroute.core.repository.ListingPhotoStorage
import com.agroroute.core.remote.CloudinaryPhotoStorage
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    /**
     * Binds [CloudinaryPhotoStorage] as the [ListingPhotoStorage] implementation.
     * New listing photos are uploaded to Cloudinary using an unsigned preset —
     * no API secret is embedded in the app.
     *
     * Firebase Storage ([com.agroroute.core.remote.StorageService]) is kept as a
     * dependency so that existing Firebase Storage URLs in old listings continue to
     * load via Coil/URL. Only *new* uploads go through Cloudinary.
     */
    @Binds
    @Singleton
    abstract fun bindListingPhotoStorage(impl: CloudinaryPhotoStorage): ListingPhotoStorage

    @Binds
    @Singleton
    abstract fun bindHarvestListingRepository(
        impl: HarvestListingRepositoryImpl
    ): HarvestListingRepository

    @Binds
    @Singleton
    abstract fun bindBidRepository(
        impl: BidRepositoryImpl
    ): BidRepository

    @Binds
    @Singleton
    abstract fun bindOrderRepository(
        impl: OrderRepositoryImpl
    ): OrderRepository
}

