package com.rvetiveau.rvscorecount.di

import com.rvetiveau.rvscorecount.data.repository.AppearanceRepositoryImpl
import com.rvetiveau.rvscorecount.data.repository.BeloteRepositoryImpl
import com.rvetiveau.rvscorecount.data.repository.FreeGameRepositoryImpl
import com.rvetiveau.rvscorecount.data.repository.PartyRepositoryImpl
import com.rvetiveau.rvscorecount.data.repository.PlayerRepositoryImpl
import com.rvetiveau.rvscorecount.domain.repository.AppearanceRepository
import com.rvetiveau.rvscorecount.domain.repository.BeloteRepository
import com.rvetiveau.rvscorecount.domain.repository.FreeGameRepository
import com.rvetiveau.rvscorecount.domain.repository.PartyRepository
import com.rvetiveau.rvscorecount.domain.repository.PlayerRepository
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

    @Binds
    @Singleton
    abstract fun bindBeloteRepository(impl: BeloteRepositoryImpl): BeloteRepository

    @Binds
    @Singleton
    abstract fun bindPlayerRepository(impl: PlayerRepositoryImpl): PlayerRepository

    @Binds
    @Singleton
    abstract fun bindFreeGameRepository(impl: FreeGameRepositoryImpl): FreeGameRepository
}
