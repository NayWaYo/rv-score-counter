package com.rvetiveau.rvscorecount.domain.usecase

import com.rvetiveau.rvscorecount.domain.engine.GameEngine
import com.rvetiveau.rvscorecount.domain.model.CardSuit
import com.rvetiveau.rvscorecount.domain.model.Participant
import com.rvetiveau.rvscorecount.domain.model.Party
import com.rvetiveau.rvscorecount.domain.model.PartyState
import com.rvetiveau.rvscorecount.domain.model.Round
import com.rvetiveau.rvscorecount.domain.model.RoundScore
import com.rvetiveau.rvscorecount.domain.repository.PartyRepository
import com.rvetiveau.rvscorecount.game.papayoo.PapayooEngine
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidateRoundUseCaseTest {

    private class FakePartyRepository(
        var party: Party?,
        private val participants: List<Participant>,
        val rounds: MutableList<Round> = mutableListOf(),
        val roundScores: MutableList<RoundScore> = mutableListOf()
    ) : PartyRepository {
        var saveFailure: Throwable? = null
        var saveRoundInvocationCount = 0
            private set

        override suspend fun createParty(party: Party) = error("not used in this test")
        override suspend fun updateParty(party: Party) = error("not used in this test")
        override suspend fun createPartyWithParticipants(party: Party, participants: List<Participant>) = error("not used in this test")
        override suspend fun getPartyById(id: UUID): Party? = party?.takeIf { it.id == id }
        override fun observeParty(id: UUID): Flow<Party?> = error("not used in this test")
        override suspend fun getPartyInProgress(): Party? = error("not used in this test")
        override suspend fun addParticipant(participant: Participant, updatedParty: Party) = error("not used in this test")
        override suspend fun getParticipantsByPartyId(partyId: UUID): List<Participant> =
            participants.filter { it.partyId == partyId }
        override fun observeParticipants(partyId: UUID): Flow<List<Participant>> = error("not used in this test")
        override suspend fun saveRound(round: Round, scores: Map<UUID, Int>, updatedParty: Party) {
            saveRoundInvocationCount++
            saveFailure?.let { throw it }
            rounds += round
            roundScores += scores.map { (id, points) -> RoundScore(round.id, id, points) }
            party = updatedParty
        }
        override suspend fun getRoundsByPartyId(partyId: UUID): List<Round> =
            rounds.filter { it.partyId == partyId }.sortedBy { it.roundNumber }
        override suspend fun getScoresByRoundId(roundId: UUID): List<RoundScore> =
            roundScores.filter { it.roundId == roundId }
    }

    private val partyId = UUID.randomUUID()
    private val alice = Participant(UUID.randomUUID(), partyId, "Alice", 0)
    private val bob = Participant(UUID.randomUUID(), partyId, "Bob", 1)
    private val charlie = Participant(UUID.randomUUID(), partyId, "Charlie", 2)
    private val participants = listOf(alice, bob, charlie)

    private fun aParty(dealerIndex: Int = 0, status: PartyState = PartyState.IN_PROGRESS, gameId: String = PapayooEngine.GAME_ID) =
        Party(id = partyId, gameId = gameId, status = status, currentDealerIndex = dealerIndex, createdAt = 0L)

    private fun useCase(
        repository: PartyRepository,
        engines: Map<String, GameEngine> = mapOf(PapayooEngine.GAME_ID to PapayooEngine())
    ) = ValidateRoundUseCase(repository, engines)

    private fun validScores() = mapOf(alice.id to 100, bob.id to 80, charlie.id to 70)

    @Test
    fun `persists the round with its suit, scores and next dealer`() = runTest {
        val repository = FakePartyRepository(aParty(dealerIndex = 0), participants)

        val outcome = useCase(repository)(partyId, CardSuit.HEART, validScores()).getOrThrow()

        check(outcome is ValidateRoundOutcome.Validated)
        assertEquals(1, outcome.round.roundNumber)
        assertEquals(CardSuit.HEART, repository.rounds.single().suit)
        assertEquals(validScores(), repository.roundScores.associate { it.participantId to it.points })
        assertEquals(1, outcome.nextDealerIndex)
        assertEquals(1, repository.party?.currentDealerIndex)
    }

    @Test
    fun `dealer rotation wraps around to the first participant`() = runTest {
        val repository = FakePartyRepository(aParty(dealerIndex = 2), participants)

        val outcome = useCase(repository)(partyId, CardSuit.SPADE, validScores()).getOrThrow()

        check(outcome is ValidateRoundOutcome.Validated)
        assertEquals(0, outcome.nextDealerIndex)
        assertEquals(0, repository.party?.currentDealerIndex)
    }

    @Test
    fun `numbers rounds after the highest existing one and ranks by cumulative score`() = runTest {
        val previous = Round(UUID.randomUUID(), partyId, 1, CardSuit.CLUB)
        val repository = FakePartyRepository(
            aParty(),
            participants,
            rounds = mutableListOf(previous),
            roundScores = mutableListOf(
                RoundScore(previous.id, alice.id, 200),
                RoundScore(previous.id, bob.id, 0),
                RoundScore(previous.id, charlie.id, 50)
            )
        )

        val outcome = useCase(repository)(partyId, CardSuit.DIAMOND, validScores()).getOrThrow()

        check(outcome is ValidateRoundOutcome.Validated)
        assertEquals(2, outcome.round.roundNumber)
        assertEquals(listOf("Bob", "Charlie", "Alice"), outcome.standings.map { it.participant.name })
        assertEquals(listOf(80, 120, 300), outcome.standings.map { it.cumulativeScore })
    }

    @Test
    fun `a sum mismatch is a business outcome and writes nothing`() = runTest {
        val repository = FakePartyRepository(aParty(), participants)

        val outcome = useCase(repository)(
            partyId, CardSuit.HEART, mapOf(alice.id to 100, bob.id to 80, charlie.id to 60)
        ).getOrThrow()

        assertEquals(ValidateRoundOutcome.SumMismatch(-10), outcome)
        assertEquals(0, repository.saveRoundInvocationCount)
        assertEquals(0, repository.party?.currentDealerIndex)
    }

    @Test
    fun `rejects scores that do not cover exactly the party participants`() = runTest {
        val repository = FakePartyRepository(aParty(), participants)

        val missing = useCase(repository)(partyId, CardSuit.HEART, mapOf(alice.id to 150, bob.id to 100))
        val extra = useCase(repository)(partyId, CardSuit.HEART, validScores() + (UUID.randomUUID() to 0))

        assertTrue(missing.exceptionOrNull() is InvalidRoundException)
        assertTrue(extra.exceptionOrNull() is InvalidRoundException)
        assertEquals(0, repository.saveRoundInvocationCount)
    }

    @Test
    fun `rejects a negative score even when the sum is exact`() = runTest {
        val repository = FakePartyRepository(aParty(), participants)

        val result = useCase(repository)(
            partyId, CardSuit.HEART, mapOf(alice.id to 200, bob.id to 100, charlie.id to -50)
        )

        assertTrue(result.exceptionOrNull() is InvalidRoundException)
        assertEquals(0, repository.saveRoundInvocationCount)
    }

    @Test
    fun `rejects a missing, finished or unknown-game party`() = runTest {
        val missing = FakePartyRepository(null, participants)
        val finished = FakePartyRepository(aParty(status = PartyState.FINISHED), participants)
        val unknownGame = FakePartyRepository(aParty(gameId = "unknown"), participants)

        listOf(missing, finished, unknownGame).forEach { repository ->
            val result = useCase(repository)(partyId, CardSuit.HEART, validScores())
            assertTrue(result.exceptionOrNull() is InvalidRoundException)
            assertEquals(0, repository.saveRoundInvocationCount)
        }
    }

    @Test
    fun `rejects an out-of-bounds dealer index instead of guessing one`() = runTest {
        val repository = FakePartyRepository(aParty(dealerIndex = 3), participants)

        val result = useCase(repository)(partyId, CardSuit.HEART, validScores())

        assertTrue(result.exceptionOrNull() is InvalidRoundException)
        assertEquals(0, repository.saveRoundInvocationCount)
    }

    @Test
    fun `wraps a persistence failure and leaves the party untouched`() = runTest {
        val repository = FakePartyRepository(aParty(), participants)
        repository.saveFailure = IllegalStateException("constraint violated")

        val result = useCase(repository)(partyId, CardSuit.HEART, validScores())

        assertTrue(result.exceptionOrNull() is RoundPersistenceException)
        assertEquals(0, repository.party?.currentDealerIndex)
        assertNull(repository.rounds.firstOrNull())
    }
}
