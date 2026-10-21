package com.rvetiveau.rvscorecount.domain.usecase

import com.rvetiveau.rvscorecount.domain.model.CardSuit
import com.rvetiveau.rvscorecount.domain.model.Participant
import com.rvetiveau.rvscorecount.domain.model.Party
import com.rvetiveau.rvscorecount.domain.model.PartyState
import com.rvetiveau.rvscorecount.domain.model.PartySummary
import com.rvetiveau.rvscorecount.domain.model.Round
import com.rvetiveau.rvscorecount.domain.model.RoundScore
import com.rvetiveau.rvscorecount.domain.repository.PartyRepository
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import com.rvetiveau.rvscorecount.game.belote.BeloteEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** [ObservePartyHistoryUseCase] and [DeletePartyUseCase] (Story 6.1). */
class PartyHistoryUseCasesTest {

    private class FakePartyRepository(
        parties: List<Party>,
        private val participants: List<Participant> = emptyList(),
        private val rounds: List<Round> = emptyList(),
        private val roundScores: List<RoundScore> = emptyList(),
        private val deleteFailure: Throwable? = null
    ) : PartyRepository {
        val finished = MutableStateFlow(parties)
        private val all = parties.associateBy { it.id }.toMutableMap()
        var deletedId: UUID? = null
            private set

        fun addInProgress(party: Party) {
            all[party.id] = party
        }

        override suspend fun createParty(party: Party) = error("not used in this test")
        override suspend fun updateParty(party: Party) = error("not used in this test")
        override suspend fun createPartyWithParticipants(party: Party, participants: List<Participant>) = error("not used in this test")
        override suspend fun getPartyById(id: UUID): Party? = all[id]
        override fun observeParty(id: UUID): Flow<Party?> = error("not used in this test")
        override suspend fun getPartyInProgress(): Party? = error("not used in this test")
        override fun observeFinishedParties(): Flow<List<Party>> = finished
        override suspend fun deleteParty(id: UUID) {
            deleteFailure?.let { throw it }
            deletedId = id
            all.remove(id)
            finished.value = finished.value.filterNot { it.id == id }
        }
        override suspend fun addParticipant(participant: Participant, updatedParty: Party) = error("not used in this test")
        override suspend fun getParticipantsByPartyId(partyId: UUID): List<Participant> = participants.filter { it.partyId == partyId }
        override fun observeParticipants(partyId: UUID): Flow<List<Participant>> = error("not used in this test")
        override suspend fun saveRound(round: Round, scores: Map<UUID, Int>, updatedParty: Party) = error("not used in this test")
        override suspend fun correctRound(round: Round, scores: Map<UUID, Int>) = error("not used in this test")
        override suspend fun getRoundsByPartyId(partyId: UUID): List<Round> = rounds.filter { it.partyId == partyId }
        override suspend fun getScoresByRoundId(roundId: UUID): List<RoundScore> = roundScores.filter { it.roundId == roundId }
    }

    private fun aParty(createdAt: Long, status: PartyState = PartyState.FINISHED) =
        Party(id = UUID.randomUUID(), gameId = "papayoo", status = status, currentDealerIndex = 0, createdAt = createdAt)

    @Test
    fun `summarizes each finished party with its round count and every tied winner`() = runTest {
        val party = aParty(createdAt = 10L)
        val alice = Participant(UUID.randomUUID(), party.id, "Alice", 0)
        val bob = Participant(UUID.randomUUID(), party.id, "Bob", 1)
        val chloe = Participant(UUID.randomUUID(), party.id, "Chloé", 2)
        val round1 = Round(UUID.randomUUID(), party.id, 1, CardSuit.HEART)
        val round2 = Round(UUID.randomUUID(), party.id, 2, CardSuit.CLUB)
        val repository = FakePartyRepository(
            listOf(party),
            listOf(alice, bob, chloe),
            listOf(round1, round2),
            listOf(
                RoundScore(round1.id, alice.id, 50), RoundScore(round1.id, bob.id, 100), RoundScore(round1.id, chloe.id, 100),
                RoundScore(round2.id, alice.id, 100), RoundScore(round2.id, bob.id, 50), RoundScore(round2.id, chloe.id, 100)
            )
        )

        val summary = ObservePartyHistoryUseCase(repository, FakeBeloteStore(), BeloteEngine())().first().single()

        assertEquals(PartySummary(party.id, "papayoo", 10L, roundCount = 2, winners = listOf("Alice", "Bob"), winningScore = 150), summary)
    }

    @Test
    fun `a party without rounds has no winner and an inconsistent one is flagged`() = runTest {
        val empty = aParty(createdAt = 20L)
        val broken = aParty(createdAt = 10L)
        val round = Round(UUID.randomUUID(), broken.id, 1, CardSuit.SPADE)
        val repository = FakePartyRepository(
            listOf(empty, broken),
            listOf(Participant(UUID.randomUUID(), broken.id, "Alice", 0)),
            listOf(round),
            listOf(RoundScore(round.id, UUID.randomUUID(), 250))
        )

        val (emptySummary, brokenSummary) = ObservePartyHistoryUseCase(repository, FakeBeloteStore(), BeloteEngine())().first()

        assertEquals(0, emptySummary.roundCount)
        assertTrue(emptySummary.winners.isEmpty())
        assertTrue(emptySummary.isConsistent)
        assertTrue(!brokenSummary.isConsistent)
        assertNull(brokenSummary.winningScore)
    }

    @Test
    fun `deletes a finished party and the history follows`() = runTest {
        val party = aParty(createdAt = 10L)
        val repository = FakePartyRepository(listOf(party))

        val result = DeletePartyUseCase(repository)(party.id)

        assertTrue(result.isSuccess)
        assertEquals(party.id, repository.deletedId)
        assertTrue(ObservePartyHistoryUseCase(repository, FakeBeloteStore(), BeloteEngine())().first().isEmpty())
    }

    @Test
    fun `refuses to delete a missing or in-progress party`() = runTest {
        val inProgress = aParty(createdAt = 10L, status = PartyState.IN_PROGRESS)
        val repository = FakePartyRepository(emptyList()).also { it.addInProgress(inProgress) }

        listOf(inProgress.id, UUID.randomUUID()).forEach { id ->
            val result = DeletePartyUseCase(repository)(id)
            assertTrue(result.exceptionOrNull() is InvalidPartyDeletionException)
        }
        assertNull(repository.deletedId)
    }

    @Test
    fun `wraps a deletion failure`() = runTest {
        val party = aParty(createdAt = 10L)
        val repository = FakePartyRepository(listOf(party), deleteFailure = IllegalStateException("Room write failed"))

        val result = DeletePartyUseCase(repository)(party.id)

        assertTrue(result.exceptionOrNull() is PartyDeletionPersistenceException)
    }
}
