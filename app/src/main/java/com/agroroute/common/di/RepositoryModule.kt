package com.agroroute.common.di

import com.agroroute.farmer.HarvestListingRepository
import com.agroroute.farmer.HarvestListingRepositoryImpl
import com.agroroute.common.order.OrderRepository
import com.agroroute.common.order.OrderRepositoryImpl
import com.agroroute.supplier.BidRepository
import com.agroroute.supplier.BidRepositoryImpl
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