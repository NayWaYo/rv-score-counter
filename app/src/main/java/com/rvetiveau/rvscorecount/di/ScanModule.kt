package com.rvetiveau.rvscorecount.di

import com.rvetiveau.rvscorecount.data.scan.MlKitTrumpReader
import com.rvetiveau.rvscorecount.domain.scan.TrumpReader
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class ScanModule {
    @Binds
    abstract fun bindTrumpReader(impl: MlKitTrumpReader): TrumpReader
}
