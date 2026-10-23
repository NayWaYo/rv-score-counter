package com.rvetiveau.rvscorecount.domain.usecase

import com.rvetiveau.rvscorecount.domain.engine.GameIds
import com.rvetiveau.rvscorecount.domain.engine.TarotRules
import com.rvetiveau.rvscorecount.domain.model.Participant
import com.rvetiveau.rvscorecount.domain.model.Party
import com.rvetiveau.rvscorecount.domain.model.TarotPartyState
import com.rvetiveau.rvscorecount.domain.model.TarotRound
import com.rvetiveau.rvscorecount.domain.repository.PartyRepository
import com.rvetiveau.rvscorecount.domain.repository.TarotRepository
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * A Partie of Tarot observed from Room (Story 13.3), scored by the engine on every change; null
 * once the Partie no longer exists. An incoherent Partie fails the flow instead of being shown.
 */
class ObserveTarotPartyUseCase @Inject constructor(
    private val partyRepository: PartyRepository,
    private val tarotRepository: TarotRepository,
    private val rules: TarotRules
) {
    operator fun invoke(partyId: UUID): Flow<TarotPartyState?> = combine(
        partyRepository.observeParty(partyId),
        partyRepository.observeParticipants(partyId),
        tarotRepository.observeRounds(partyId)
    ) { party, participants, rounds ->
        party?.let { assembleTarotParty(it, participants, rounds, rules) }
    }
}

/** Loads a Partie of Tarot once, or null when it does not exist. */
internal suspend fun loadTarotParty(
    partyId: UUID,
    partyRepository: PartyRepository,
    tarotRepository: TarotRepository,
    rules: TarotRules
): TarotPartyState? {
    val party = partyRepository.getPartyById(partyId) ?: return null
    return assembleTarotParty(
        party = party,
        participants = partyRepository.getParticipantsByPartyId(partyId),
        rounds = tarotRepository.getRounds(partyId),
        rules = rules
    )
}

internal fun assembleTarotParty(
    party: Party,
    participants: List<Participant>,
    rounds: List<TarotRound>,
    rules: TarotRules
): TarotPartyState {
    check(party.gameId == GameIds.TAROT) { "Party ${party.id} is not a Tarot party" }
    val seats = participants.sortedBy { it.listPosition }
    check(seats.size in rules.minPlayers..rules.maxPlayers) { "Tarot party ${party.id} has ${seats.size} players" }
    check(party.currentDealerIndex in seats.indices) { "Tarot party ${party.id} has no dealer" }
    val ordered = rounds.sortedBy { it.roundNumber }
    check(ordered.map { it.roundNumber } == (1..ordered.size).toList()) { "Tarot party ${party.id} has a gap in its rounds" }
    ordered.forEach { round ->
        rules.inputError(round.input, seats)?.let { error -> error("Round ${round.roundNumber} of party ${party.id}: $error") }
    }

    return TarotPartyState(
        party = party,
        participants = seats,
        rounds = ordered,
        results = ordered.map { rules.scoreRound(it.input, seats) },
        standings = rules.standings(seats, ordered),
        status = rules.partyStatus(seats, ordered, party.endRule),
        dealing = rules.dealing(seats.size),
        callsPartner = seats.size == rules.partnerPlayerCount
    )
}
