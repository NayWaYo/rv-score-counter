package com.rvetiveau.rvscorecount.domain.usecase

import com.rvetiveau.rvscorecount.domain.engine.BeloteRules
import com.rvetiveau.rvscorecount.domain.engine.GameIds
import com.rvetiveau.rvscorecount.domain.model.BelotePartyStatus
import com.rvetiveau.rvscorecount.domain.model.Party
import com.rvetiveau.rvscorecount.domain.model.PartySummary
import com.rvetiveau.rvscorecount.domain.model.TeamScore
import com.rvetiveau.rvscorecount.domain.model.TeamSide
import com.rvetiveau.rvscorecount.domain.model.computeStandings
import com.rvetiveau.rvscorecount.domain.repository.BeloteRepository
import com.rvetiveau.rvscorecount.domain.repository.PartyRepository
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest

/**
 * Historique (Story 6.1) : résumé de chaque Partie FINISHED, la plus récente d'abord, recalculé à
 * chaque changement de la liste (fin de Partie, suppression). Une Partie de Belote (Story 7.4) est
 * résumée par Équipe, ses Manches rescorées par le moteur.
 */
class ObservePartyHistoryUseCase @Inject constructor(
    private val partyRepository: PartyRepository,
    private val beloteRepository: BeloteRepository,
    private val beloteRules: BeloteRules
) {
    /** [gameId] restricts the history to one Jeu (Création de Partie); null keeps every Jeu. */
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(gameId: String? = null): Flow<List<PartySummary>> =
        partyRepository.observeFinishedParties().mapLatest { parties ->
            parties.filter { gameId == null || it.gameId == gameId }.map { party ->
                if (party.gameId == GameIds.BELOTE) summarizeBelote(party) else summarize(party)
            }
        }

    private suspend fun summarize(party: Party): PartySummary {
        val participants = partyRepository.getParticipantsByPartyId(party.id)
        val rounds = partyRepository.getRoundsByPartyId(party.id)
        val scores = rounds.flatMap { partyRepository.getScoresByRoundId(it.id) }
        val base = baseSummary(party, rounds.size)
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

    /** A finished Partie of Belote has a winning Équipe; without one, its stored data are incoherent. */
    private suspend fun summarizeBelote(party: Party): PartySummary {
        val rounds = beloteRepository.getRounds(party.id)
        val base = baseSummary(party, rounds.size)
        val state = try {
            assembleBeloteParty(
                party = party,
                teams = beloteRepository.getTeams(party.id),
                players = partyRepository.getParticipantsByPartyId(party.id),
                rounds = rounds,
                rules = beloteRules
            )
        } catch (_: IllegalStateException) {
            return base.copy(isConsistent = false)
        } catch (_: IllegalArgumentException) {
            return base.copy(isConsistent = false)
        }
        val totals = state.totals
        val teamScores = TeamSide.entries.map { TeamScore(state.team(it).name, totals[it]) }
        val winner = (state.status as? BelotePartyStatus.Won)?.winner
            ?: return base.copy(isConsistent = false, teamScores = teamScores)
        return base.copy(
            winners = listOf(state.team(winner).name),
            winningScore = totals[winner],
            teamScores = teamScores
        )
    }

    private fun baseSummary(party: Party, roundCount: Int) = PartySummary(
        partyId = party.id,
        gameId = party.gameId,
        createdAt = party.createdAt,
        roundCount = roundCount,
        winners = emptyList(),
        winningScore = null
    )
}
