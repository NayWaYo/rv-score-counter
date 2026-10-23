package com.rvetiveau.rvscorecount.di

import com.rvetiveau.rvscorecount.domain.engine.BeloteRules
import com.rvetiveau.rvscorecount.domain.engine.FreeGameRules
import com.rvetiveau.rvscorecount.domain.engine.Game
import com.rvetiveau.rvscorecount.domain.engine.GameEngine
import com.rvetiveau.rvscorecount.domain.engine.TarotRules
import com.rvetiveau.rvscorecount.game.belote.BeloteEngine
import com.rvetiveau.rvscorecount.game.free.FreeGameEngine
import com.rvetiveau.rvscorecount.game.papayoo.PapayooEngine
import com.rvetiveau.rvscorecount.game.tarot.TarotEngine
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey

/**
 * Engines (AD-2, AD-8): every Jeu enters the Accueil catalogue as a [Game]; the [GameEngine] map is
 * the Papayoo's per-player contract, the Belote, the Tarot and the Jeu libre have their own rules contracts.
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

    // In the Accueil catalogue with its creation screen (AD-9, Story 11.2).
    @Binds
    abstract fun bindFreeGameRules(engine: FreeGameEngine): FreeGameRules

    @Binds
    @IntoMap
    @StringKey(FreeGameEngine.GAME_ID)
    abstract fun bindFreeGame(engine: FreeGameEngine): Game

    // Enters the Accueil catalogue with its creation screen (AD-10, Story 13.3).
    @Binds
    abstract fun bindTarotRules(engine: TarotEngine): TarotRules
}
