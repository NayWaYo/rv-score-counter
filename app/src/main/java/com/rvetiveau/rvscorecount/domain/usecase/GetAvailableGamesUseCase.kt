package com.rvetiveau.rvscorecount.domain.usecase

import com.rvetiveau.rvscorecount.domain.engine.GameEngine
import javax.inject.Inject

/**
 * Exposes the Games registered via the Hilt multibinding registry (AD-2), ordered
 * deterministically by gameId. A key/gameId mismatch is a registry programming error.
 */
class GetAvailableGamesUseCase @Inject constructor(
    private val engines: Map<String, @JvmSuppressWildcards GameEngine>
) {
    operator fun invoke(): List<GameEngine> =
        engines.entries
            .sortedBy { it.key }
            .map { (key, engine) ->
                check(engine.gameId == key) {
                    "Engine registry mismatch: key '$key' does not match engine.gameId '${engine.gameId}'"
                }
                engine
            }
}
