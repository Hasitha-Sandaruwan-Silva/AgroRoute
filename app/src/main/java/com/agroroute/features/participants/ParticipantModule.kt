package com.agroroute.features.participants

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class ParticipantModule {
    @Binds abstract fun people(impl: ParticipantRepositoryImpl): ParticipantRepository
    @Binds abstract fun chat(impl: ChatRepositoryImpl): ChatRepository
}
