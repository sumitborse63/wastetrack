package com.sktech.wastetrack.di

import com.sktech.wastetrack.data.repository.AuthRepositoryImpl
import com.sktech.wastetrack.data.repository.BidRepositoryImpl
import com.sktech.wastetrack.domain.repository.IAuthRepository
import com.sktech.wastetrack.domain.repository.IBidRepository
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
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): IAuthRepository

    @Binds
    @Singleton
    abstract fun bindBidRepository(
        bidRepositoryImpl: BidRepositoryImpl
    ): IBidRepository
}
