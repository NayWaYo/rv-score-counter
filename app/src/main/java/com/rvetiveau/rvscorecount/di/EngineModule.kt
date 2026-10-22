package com.rvetiveau.rvscorecount.di

import com.rvetiveau.rvscorecount.domain.engine.BeloteRules
import com.rvetiveau.rvscorecount.domain.engine.FreeGameRules
import com.rvetiveau.rvscorecount.domain.engine.Game
import com.rvetiveau.rvscorecount.domain.engine.GameEngine
import com.rvetiveau.rvscorecount.game.belote.BeloteEngine
import com.rvetiveau.rvscorecount.game.free.FreeGameEngine
import com.rvetiveau.rvscorecount.game.papayoo.PapayooEngine
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey

/**
 * Engines (AD-2, AD-8): every Jeu enters the Accueil catalogue as a [Game]; the [GameEngine] map is
 * the Papayoo's per-player contract, the Belote has its own [BeloteRules].
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class EngineModule {
    @Binds
    @IntoMap
    @StringKey(PapayooEngine.GAME_ID)
    abstract fun bindPapayooEngine(engine: PapayooEngine): GameEngine

    @Binds
    @IntoMap
    @StringKey(PapayooEngine.GAME_ID)
    abstract fun bindPapayooGame(engine: PapayooEngine): Game

    @Binds
    abstract fun bindBeloteRules(engine: BeloteEngine): BeloteRules

    @Binds
    @IntoMap
    @StringKey(BeloteEngine.GAME_ID)
    abstract fun bindBeloteGame(engine: BeloteEngine): Game

    // Not in the Accueil catalogue until its creation screen exists (AD-9, Story 11.2).
    @Binds
    abstract fun bindFreeGameRules(engine: FreeGameEngine): FreeGameRules
}
