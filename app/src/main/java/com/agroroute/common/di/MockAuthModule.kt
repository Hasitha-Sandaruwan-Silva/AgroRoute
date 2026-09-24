package com.agroroute.common.di

import com.agroroute.auth.AuthRepository
import com.agroroute.auth.MockAuthRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class MockAuthModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: MockAuthRepository
    ): AuthRepository
}