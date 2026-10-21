package com.rvetiveau.rvscorecount.domain.usecase

import com.rvetiveau.rvscorecount.domain.engine.Game
import com.rvetiveau.rvscorecount.domain.engine.GameIds
import javax.inject.Inject

/**
 * The gameIds of the Accueil catalogue (AD-8), in [GameIds.CATALOG_ORDER]. A key/gameId mismatch
 * or a registered Jeu missing from that order is a registry programming error.
 */
class GetGameCatalogUseCase @Inject constructor(
    private val games: Map<String, @JvmSuppressWildcards Game>
) {
    operator fun invoke(): List<String> {
        games.forEach { (key, game) ->
            check(game.gameId == key) { "Game registry mismatch: key '$key' does not match gameId '${game.gameId}'" }
            check(key in GameIds.CATALOG_ORDER) { "Game '$key' is missing from GameIds.CATALOG_ORDER" }
        }
        return GameIds.CATALOG_ORDER.filter { it in games }
    }
}
