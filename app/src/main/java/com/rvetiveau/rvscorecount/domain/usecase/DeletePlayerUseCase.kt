package com.rvetiveau.rvscorecount.domain.usecase

import com.rvetiveau.rvscorecount.domain.repository.PlayerRepository
import java.util.UUID
import javax.inject.Inject

sealed interface DeletePlayerOutcome {
    data object Deleted : DeletePlayerOutcome
    data object HasParties : DeletePlayerOutcome
    data object UnknownPlayer : DeletePlayerOutcome
}

/** Deletes a Joueur left without any Partie (Story 10.4), such as a typo kept in the suggestions. */
class DeletePlayerUseCase @Inject constructor(
    private val playerRepository: PlayerRepository
) {
    suspend operator fun invoke(playerId: UUID): DeletePlayerOutcome {
        if (playerRepository.getSeats(playerId).isNotEmpty()) return DeletePlayerOutcome.HasParties
        return if (playerRepository.deletePlayer(playerId)) DeletePlayerOutcome.Deleted else DeletePlayerOutcome.UnknownPlayer
    }
}
