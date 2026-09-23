package com.agroroute.di

import com.agroroute.data.repository.MockAuthRepository
import com.agroroute.domain.repository.AuthRepository
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