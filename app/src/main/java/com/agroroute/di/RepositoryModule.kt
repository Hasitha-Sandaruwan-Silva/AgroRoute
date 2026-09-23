package com.agroroute.di

import com.agroroute.data.repository.BidRepositoryImpl
import com.agroroute.data.repository.HarvestListingRepositoryImpl
import com.agroroute.data.repository.OrderRepositoryImpl
import com.agroroute.domain.repository.BidRepository
import com.agroroute.domain.repository.HarvestListingRepository
import com.agroroute.domain.repository.OrderRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    // TODO: Restore for production when Firebase is ready
    // @Binds
    // @Singleton
    // abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

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