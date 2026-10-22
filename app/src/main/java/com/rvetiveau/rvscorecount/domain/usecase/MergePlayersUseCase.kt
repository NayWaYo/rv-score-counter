package com.rvetiveau.rvscorecount.domain.usecase

import com.rvetiveau.rvscorecount.domain.repository.PartyRepository
import com.rvetiveau.rvscorecount.domain.repository.PlayerRepository
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.first

sealed interface MergePlayersOutcome {
    data object Merged : MergePlayersOutcome
    data object UnknownPlayer : MergePlayersOutcome
    data object SamePlayer : MergePlayersOutcome
    /** Both sat at the Partie of [gameId] created at [playedAt]: two different people. */
    data class PlayedTogether(val gameId: String, val playedAt: Long) : MergePlayersOutcome
}

/**
 * Merges two Joueurs who are the same person (Story 10.4): the absorbed one's seats join the kept
 * one, under the kept name. Two Joueurs who sat at one Partie together cannot be one person.
 */
class MergePlayersUseCase @Inject constructor(
    private val playerRepository: PlayerRepository,
    private val partyRepository: PartyRepository
) {
    suspend operator fun invoke(keptId: UUID, absorbedId: UUID): MergePlayersOutcome {
        if (keptId == absorbedId) return MergePlayersOutcome.SamePlayer
        val players = playerRepository.observePlayers().first()
        if (players.none { it.id == keptId } || players.none { it.id == absorbedId }) return MergePlayersOutcome.UnknownPlayer

        val keptParties = playerRepository.getSeats(keptId).mapTo(mutableSetOf()) { it.partyId }
        val shared = playerRepository.getSeats(absorbedId).map { it.partyId }.filter { it in keptParties }
            .mapNotNull { partyRepository.getPartyById(it) }
            .minByOrNull { it.createdAt }
        if (shared != null) return MergePlayersOutcome.PlayedTogether(shared.gameId, shared.createdAt)

        playerRepository.mergePlayers(keptId, absorbedId)
        return MergePlayersOutcome.Merged
    }
}
