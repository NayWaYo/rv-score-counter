package com.rvetiveau.rvscorecount.domain.usecase

import com.rvetiveau.rvscorecount.domain.engine.GameIds
import com.rvetiveau.rvscorecount.domain.model.EndRule
import com.rvetiveau.rvscorecount.domain.model.Participant
import com.rvetiveau.rvscorecount.domain.model.Party
import com.rvetiveau.rvscorecount.domain.model.PartyState
import com.rvetiveau.rvscorecount.domain.model.TarotContract
import com.rvetiveau.rvscorecount.domain.model.TarotRound
import com.rvetiveau.rvscorecount.domain.model.TarotRoundInput
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * One Partie of Tarot in memory (Story 13.3), its first [playerCount] seats among Alice, Bob, Chloé,
 * David and Eve, Alice dealing first: its Partie and Participants through this
 * [PartyRepositoryAdapter], its Manches through [tarot], whose writes update the Partie as Room
 * would, or fail when [failWrites].
 */
class FakeTarotStore(
    endRule: EndRule? = EndRule.RoundCount(3),
    playerCount: Int = 4
) : PartyRepositoryAdapter() {
    val partyId: UUID = UUID.randomUUID()
    val seats = listOf("Alice", "Bob", "Chloé", "David", "Eve").take(playerCount).mapIndexed { index, name ->
        Participant(UUID.randomUUID(), partyId, name, index)
    }
    val alice = seats[0]
    val bob = seats[1]
    val chloe = seats[2]

    val party = MutableStateFlow<Party?>(Party(partyId, GameIds.TAROT, PartyState.IN_PROGRESS, 0, 10, endRule))
    val participants = MutableStateFlow(seats.reversed())
    var failWrites = false

    val tarot = object : FakeTarotRepository() {
        override suspend fun saveRound(round: TarotRound, updatedParty: Party) {
            if (failWrites) throw IOException("disk full")
            super.saveRound(round, updatedParty)
            party.value = updatedParty
        }

        override suspend fun correctRound(round: TarotRound, updatedParty: Party) {
            if (failWrites) throw IOException("disk full")
            super.correctRound(round, updatedParty)
            party.value = updatedParty
        }
    }

    /** Alice takes a Prise with 2 bouts and [points] points (41 makes it by 0). */
    fun input(points: Int = 41, taker: Participant = alice) =
        TarotRoundInput(taker.id, TarotContract.PRISE, attackBouts = 2, attackPoints = points)

    /** Stores a validated Manche directly, the Partie unchanged. */
    fun addRound(input: TarotRoundInput = input()) {
        tarot.rounds.value += TarotRound(UUID.randomUUID(), partyId, tarot.rounds.value.size + 1, input)
    }

    /** Parties created by « Rejouer », with their seats. */
    val created = mutableListOf<Pair<Party, List<Participant>>>()

    override suspend fun getPartyById(id: UUID): Party? = party.value?.takeIf { it.id == id }
    override fun observeParty(id: UUID): Flow<Party?> = party
    override suspend fun updateParty(party: Party) {
        if (failWrites) throw IOException("disk full")
        this.party.value = party
    }
    override fun observeFinishedParties(): Flow<List<Party>> =
        party.map { listOfNotNull(it?.takeIf { party -> party.status == PartyState.FINISHED }) }
    override suspend fun createPartyWithParticipants(party: Party, participants: List<Participant>) {
        if (failWrites) throw IOException("disk full")
        created += party to participants
    }
    override suspend fun getParticipantsByPartyId(partyId: UUID): List<Participant> = participants.value
    override fun observeParticipants(partyId: UUID): Flow<List<Participant>> = participants
}
