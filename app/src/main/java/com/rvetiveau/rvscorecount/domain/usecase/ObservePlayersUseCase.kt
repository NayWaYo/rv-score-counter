package com.rvetiveau.rvscorecount.domain.usecase

import com.rvetiveau.rvscorecount.domain.model.PlayerSummary
import com.rvetiveau.rvscorecount.domain.model.inPlayOrder
import com.rvetiveau.rvscorecount.domain.repository.PlayerRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** The « Joueurs » screen (Story 10.2), in the order of the suggestions. */
class ObservePlayersUseCase @Inject constructor(
    private val playerRepository: PlayerRepository
) {
    operator fun invoke(): Flow<List<PlayerSummary>> = playerRepository.observePlayers().map { it.inPlayOrder() }
}
