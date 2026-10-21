package com.rvetiveau.rvscorecount.domain.usecase

import com.rvetiveau.rvscorecount.domain.engine.BeloteRules
import com.rvetiveau.rvscorecount.domain.engine.GameIds
import com.rvetiveau.rvscorecount.domain.model.BelotePartyState
import com.rvetiveau.rvscorecount.domain.model.BeloteRound
import com.rvetiveau.rvscorecount.domain.model.Participant
import com.rvetiveau.rvscorecount.domain.model.Party
import com.rvetiveau.rvscorecount.domain.model.Team
import com.rvetiveau.rvscorecount.domain.model.TeamSide
import com.rvetiveau.rvscorecount.domain.repository.BeloteRepository
import com.rvetiveau.rvscorecount.domain.repository.PartyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.UUID
import javax.inject.Inject

/**
 * A Partie of Belote observed from Room, its Manches rescored by the engine on every change; null
 * once the Partie no longer exists. An incoherent Partie fails the flow instead of being shown.
 */
class ObserveBelotePartyUseCase @Inject constructor(
    private val partyRepository: PartyRepository,
    private val beloteRepository: BeloteRepository,
    private val rules: BeloteRules
) {
    operator fun invoke(partyId: UUID): Flow<BelotePartyState?> = combine(
        partyRepository.observeParty(partyId),
        beloteRepository.observeTeams(partyId),
        partyRepository.observeParticipants(partyId),
        beloteRepository.observeRounds(partyId)
    ) { party, teams, players, rounds ->
        party?.let { assembleBeloteParty(it, teams, players, rounds, rules) }
    }
}

/** Loads a Partie of Belote once, or null when it does not exist. */
internal suspend fun loadBeloteParty(
    partyId: UUID,
    partyRepository: PartyRepository,
    beloteRepository: BeloteRepository,
    rules: BeloteRules
): BelotePartyState? {
    val party = partyRepository.getPartyById(partyId) ?: return null
    return assembleBeloteParty(
        party = party,
        teams = beloteRepository.getTeams(partyId),
        players = partyRepository.getParticipantsByPartyId(partyId),
        rounds = beloteRepository.getRounds(partyId),
        rules = rules
    )
}

internal fun assembleBeloteParty(
    party: Party,
    teams: List<Team>,
    players: List<Participant>,
    rounds: List<BeloteRound>,
    rules: BeloteRules
): BelotePartyState {
    check(party.gameId == GameIds.BELOTE) { "Party ${party.id} is not a Belote party" }
    val endRule = checkNotNull(party.endRule) { "Belote party ${party.id} has no end rule" }
    check(teams.map { it.side }.sorted() == TeamSide.entries) { "Belote party ${party.id} needs one team per side" }
    val tablePlayers = players.sortedBy { it.listPosition }
    check(tablePlayers.size == PLAYER_COUNT && TeamSide.entries.all { side -> tablePlayers.count { it.teamSide == side } == 2 }) {
        "Belote party ${party.id} needs two players per team"
    }
    check(party.currentDealerIndex in tablePlayers.indices) { "Belote party ${party.id} has no dealer" }
    check(rounds.map { it.roundNumber } == (1..rounds.size).toList()) { "Belote party ${party.id} has a gap in its rounds" }

    val results = rules.scoreParty(rounds.map { it.input })
    return BelotePartyState(
        party = party,
        teams = teams.sortedBy { it.side },
        players = tablePlayers,
        rounds = rounds,
        results = results,
        status = rules.partyStatus(results, endRule)
    )
}

private const val PLAYER_COUNT = 4
