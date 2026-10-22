package com.rvetiveau.rvscorecount.domain.usecase

import com.rvetiveau.rvscorecount.domain.engine.BeloteRules
import com.rvetiveau.rvscorecount.domain.engine.GameEngine
import com.rvetiveau.rvscorecount.domain.engine.GameIds
import com.rvetiveau.rvscorecount.domain.model.BelotePlay
import com.rvetiveau.rvscorecount.domain.model.PapayooPlay
import com.rvetiveau.rvscorecount.domain.model.Party
import com.rvetiveau.rvscorecount.domain.model.PartyState
import com.rvetiveau.rvscorecount.domain.model.PlayerRecords
import com.rvetiveau.rvscorecount.domain.model.beloteRecords
import com.rvetiveau.rvscorecount.domain.model.computeStandings
import com.rvetiveau.rvscorecount.domain.model.papayooRecords
import com.rvetiveau.rvscorecount.domain.repository.BeloteRepository
import com.rvetiveau.rvscorecount.domain.repository.PartyRepository
import com.rvetiveau.rvscorecount.domain.repository.PlayerRepository
import com.rvetiveau.rvscorecount.domain.repository.PlayerSeat
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * A Joueur's profile (Story 10.3), read once: every finished Partie they sat at, per Jeu. A Partie
 * whose stored rows do not fit together is left out, so that one broken Partie cannot hide the rest.
 */
class GetPlayerRecordsUseCase @Inject constructor(
    private val playerRepository: PlayerRepository,
    private val partyRepository: PartyRepository,
    private val beloteRepository: BeloteRepository,
    private val beloteRules: BeloteRules,
    private val engines: Map<String, @JvmSuppressWildcards GameEngine>
) {
    /** Null when no Joueur has [playerId]. */
    suspend operator fun invoke(playerId: UUID): PlayerRecords? {
        val players = playerRepository.observePlayers().first()
        val player = players.firstOrNull { it.id == playerId } ?: return null
        val names = players.associate { it.id to it.name }

        val papayooPlays = mutableListOf<PapayooPlay>()
        val belotePlays = mutableListOf<BelotePlay>()
        playerRepository.getSeats(playerId).groupBy { it.partyId }.forEach { (partyId, seats) ->
            val party = partyRepository.getPartyById(partyId)?.takeIf { it.status == PartyState.FINISHED } ?: return@forEach
            when (party.gameId) {
                GameIds.PAPAYOO -> papayooPlay(party, seats)?.let(papayooPlays::add)
                GameIds.BELOTE -> belotePlay(party, seats)?.let(belotePlays::add)
            }
        }

        val fullRoundPoints = engines[GameIds.PAPAYOO]?.totalPointsPerRound
        return PlayerRecords(
            player = player,
            papayoo = fullRoundPoints?.let { papayooRecords(papayooPlays, it) },
            belote = beloteRecords(belotePlays) { id -> names[id].orEmpty() }
        )
    }

    private suspend fun papayooPlay(party: Party, seats: List<PlayerSeat>): PapayooPlay? {
        val participants = partyRepository.getParticipantsByPartyId(party.id)
        val rounds = partyRepository.getRoundsByPartyId(party.id)
        val scores = rounds.flatMap { partyRepository.getScoresByRoundId(it.id) }
        val consistent = runCatching { computeStandings(participants, scores) }.isSuccess &&
            scores.all { score -> rounds.any { it.id == score.roundId } }
        if (!consistent) return null
        return PapayooPlay(party, participants, rounds, scores, seats.mapTo(mutableSetOf()) { it.participantId })
    }

    private suspend fun belotePlay(party: Party, seats: List<PlayerSeat>): BelotePlay? {
        val state = try {
            loadBeloteParty(party.id, partyRepository, beloteRepository, beloteRules)
        } catch (_: IllegalStateException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        } ?: return null
        val seat = seats.map { it.participantId }.firstOrNull { seat -> state.players.any { it.id == seat } } ?: return null
        return BelotePlay(state, seat, playerRepository.getPlayerIdsBySeat(party.id))
    }
}
