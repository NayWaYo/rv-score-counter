package com.rvetiveau.rvscorecount.domain.usecase

import com.rvetiveau.rvscorecount.domain.engine.GameIds
import com.rvetiveau.rvscorecount.domain.model.EndRule
import com.rvetiveau.rvscorecount.domain.model.FreeRound
import com.rvetiveau.rvscorecount.domain.model.Participant
import com.rvetiveau.rvscorecount.domain.model.Party
import com.rvetiveau.rvscorecount.domain.model.PartyState
import com.rvetiveau.rvscorecount.domain.model.WinDirection
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * One Partie of Jeu libre in memory (Story 11.3), Alice dealing first then Bob and Chloé: its
 * Partie and Participants through this [PartyRepositoryAdapter], its Manches through [free], whose
 * writes update the Partie as Room would, or fail when [failWrites].
 */
class FakeFreeStore(
    endRule: EndRule? = EndRule.TargetScore(100),
    winDirection: WinDirection = WinDirection.LOWEST_WINS
) : PartyRepositoryAdapter() {
    val partyId: UUID = UUID.randomUUID()
    val alice = Participant(UUID.randomUUID(), partyId, "Alice", 0)
    val bob = Participant(UUID.randomUUID(), partyId, "Bob", 1)
    val chloe = Participant(UUID.randomUUID(), partyId, "Chloé", 2)

    val party = MutableStateFlow<Party?>(
        Party(partyId, GameIds.FREE, PartyState.IN_PROGRESS, 0, 10, endRule, "Skyjo", winDirection)
    )
    val participants = MutableStateFlow(listOf(chloe, alice, bob))
    var failWrites = false

    val free = object : FakeFreeGameRepository() {
        override suspend fun saveRound(round: FreeRound, updatedParty: Party) {
            if (failWrites) throw IOException("disk full")
            super.saveRound(round, updatedParty)
            party.value = updatedParty
        }

        override suspend fun correctRound(round: FreeRound, updatedParty: Party) {
            if (failWrites) throw IOException("disk full")
            super.correctRound(round, updatedParty)
            party.value = updatedParty
        }
    }

    /** Stores a validated Manche directly, the Partie unchanged. */
    fun addRound(alicePoints: Int, bobPoints: Int, chloePoints: Int) {
        val number = free.rounds.value.size + 1
        free.rounds.value += FreeRound(
            UUID.randomUUID(), partyId, number, mapOf(alice.id to alicePoints, bob.id to bobPoints, chloe.id to chloePoints)
        )
    }

    fun scores(alicePoints: Int, bobPoints: Int, chloePoints: Int) =
        mapOf(alice.id to alicePoints, bob.id to bobPoints, chloe.id to chloePoints)

    override suspend fun getPartyById(id: UUID): Party? = party.value?.takeIf { it.id == id }
    override fun observeParty(id: UUID): Flow<Party?> = party
    override suspend fun updateParty(party: Party) {
        if (failWrites) throw IOException("disk full")
        this.party.value = party
    }
    override fun observeFinishedParties(): Flow<List<Party>> =
        party.map { listOfNotNull(it?.takeIf { party -> party.status == PartyState.FINISHED }) }
    override suspend fun getParticipantsByPartyId(partyId: UUID): List<Participant> = participants.value
    override fun observeParticipants(partyId: UUID): Flow<List<Participant>> = participants
}
