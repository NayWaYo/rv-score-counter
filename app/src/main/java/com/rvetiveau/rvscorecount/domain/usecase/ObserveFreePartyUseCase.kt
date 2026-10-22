package com.rvetiveau.rvscorecount.domain.usecase

import com.rvetiveau.rvscorecount.domain.engine.FreeGameRules
import com.rvetiveau.rvscorecount.domain.engine.GameIds
import com.rvetiveau.rvscorecount.domain.model.FreePartyState
import com.rvetiveau.rvscorecount.domain.model.FreeRound
import com.rvetiveau.rvscorecount.domain.model.Participant
import com.rvetiveau.rvscorecount.domain.model.Party
import com.rvetiveau.rvscorecount.domain.repository.FreeGameRepository
import com.rvetiveau.rvscorecount.domain.repository.PartyRepository
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * A Partie of Jeu libre observed from Room (Story 11.3), ranked by the engine on every change; null
 * once the Partie no longer exists. An incoherent Partie fails the flow instead of being shown.
 */
class ObserveFreePartyUseCase @Inject constructor(
    private val partyRepository: PartyRepository,
    private val freeGameRepository: FreeGameRepository,
    private val rules: FreeGameRules
) {
    operator fun invoke(partyId: UUID): Flow<FreePartyState?> = combine(
        partyRepository.observeParty(partyId),
        partyRepository.observeParticipants(partyId),
        freeGameRepository.observeRounds(partyId)
    ) { party, participants, rounds ->
        party?.let { assembleFreeParty(it, participants, rounds, rules) }
    }
}

/** Loads a Partie of Jeu libre once, or null when it does not exist. */
internal suspend fun loadFreeParty(
    partyId: UUID,
    partyRepository: PartyRepository,
    freeGameRepository: FreeGameRepository,
    rules: FreeGameRules
): FreePartyState? {
    val party = partyRepository.getPartyById(partyId) ?: return null
    return assembleFreeParty(
        party = party,
        participants = partyRepository.getParticipantsByPartyId(partyId),
        rounds = freeGameRepository.getRounds(partyId),
        rules = rules
    )
}

internal fun assembleFreeParty(
    party: Party,
    participants: List<Participant>,
    rounds: List<FreeRound>,
    rules: FreeGameRules
): FreePartyState {
    check(party.gameId == GameIds.FREE) { "Party ${party.id} is not a Jeu libre party" }
    val direction = checkNotNull(party.winDirection) { "Jeu libre party ${party.id} has no win direction" }
    checkNotNull(party.gameName) { "Jeu libre party ${party.id} has no game name" }
    val seats = participants.sortedBy { it.listPosition }
    check(party.currentDealerIndex in seats.indices) { "Jeu libre party ${party.id} has no dealer" }
    val ordered = rounds.sortedBy { it.roundNumber }
    check(ordered.map { it.roundNumber } == (1..ordered.size).toList()) { "Jeu libre party ${party.id} has a gap in its rounds" }
    check(ordered.all { rules.isValidRound(it.scores, seats) }) { "Jeu libre party ${party.id} has a round without every score" }

    return FreePartyState(
        party = party,
        participants = seats,
        rounds = ordered,
        standings = rules.standings(seats, ordered, direction),
        status = rules.partyStatus(seats, ordered, direction, party.endRule)
    )
}
