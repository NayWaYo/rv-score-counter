package com.rvetiveau.rvscorecount.di

import com.rvetiveau.rvscorecount.data.repository.AppearanceRepositoryImpl
import com.rvetiveau.rvscorecount.data.repository.PartyRepositoryImpl
import com.rvetiveau.rvscorecount.domain.repository.AppearanceRepository
import com.rvetiveau.rvscorecount.domain.repository.PartyRepository
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
    abstract fun bindPartyRepository(impl: PartyRepositoryImpl): PartyRepository

    @Binds
    @Singleton
    abstract fun bindAppearanceRepository(impl: AppearanceRepositoryImpl): AppearanceRepository
}
