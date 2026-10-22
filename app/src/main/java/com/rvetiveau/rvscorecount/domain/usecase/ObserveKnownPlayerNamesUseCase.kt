package com.rvetiveau.rvscorecount.domain.usecase

import com.rvetiveau.rvscorecount.domain.model.distinctPlayerNames
import com.rvetiveau.rvscorecount.domain.repository.PlayerRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Names suggested at Création de Partie: one per player, the most recently played first. */
class ObserveKnownPlayerNamesUseCase @Inject constructor(
    private val playerRepository: PlayerRepository
) {
    operator fun invoke(): Flow<List<String>> =
        playerRepository.observeKnownPlayers().map { it.distinctPlayerNames() }
}
