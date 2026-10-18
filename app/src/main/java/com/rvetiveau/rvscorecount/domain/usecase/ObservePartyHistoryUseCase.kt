package com.rvetiveau.rvscorecount.domain.usecase

import com.rvetiveau.rvscorecount.domain.model.Party
import com.rvetiveau.rvscorecount.domain.model.PartySummary
import com.rvetiveau.rvscorecount.domain.model.computeStandings
import com.rvetiveau.rvscorecount.domain.repository.PartyRepository
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest

/**
 * Historique (Story 6.1) : résumé de chaque Partie FINISHED, la plus récente d'abord, recalculé à
 * chaque changement de la liste (fin de Partie, suppression).
 */
class ObservePartyHistoryUseCase @Inject constructor(
    private val partyRepository: PartyRepository
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<List<PartySummary>> =
        partyRepository.observeFinishedParties().mapLatest { parties -> parties.map { summarize(it) } }

    private suspend fun summarize(party: Party): PartySummary {
        val participants = partyRepository.getParticipantsByPartyId(party.id)
        val rounds = partyRepository.getRoundsByPartyId(party.id)
        val scores = rounds.flatMap { partyRepository.getScoresByRoundId(it.id) }
        val base = PartySummary(
            partyId = party.id,
            gameId = party.gameId,
            createdAt = party.createdAt,
            roundCount = rounds.size,
            winners = emptyList(),
            winningScore = null
        )
        if (rounds.isEmpty()) return base

        val standings = try {
            computeStandings(participants, scores)
        } catch (_: IllegalArgumentException) {
            return base.copy(isConsistent = false)
        }
        val best = standings.firstOrNull()?.cumulativeScore ?: return base.copy(isConsistent = false)
        return base.copy(
            winners = standings.takeWhile { it.cumulativeScore == best }.map { it.participant.name },
            winningScore = best
        )
    }
}
