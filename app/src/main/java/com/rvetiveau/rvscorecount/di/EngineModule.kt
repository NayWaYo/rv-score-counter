package com.rvetiveau.rvscorecount.di

import com.rvetiveau.rvscorecount.domain.engine.GameEngine
import com.rvetiveau.rvscorecount.game.papayoo.PapayooEngine
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey

@Module
@InstallIn(SingletonComponent::class)
abstract class EngineModule {
    @Binds
    @IntoMap
    @StringKey(PapayooEngine.GAME_ID)
    abstract fun bindPapayooEngine(engine: PapayooEngine): GameEngine
}
