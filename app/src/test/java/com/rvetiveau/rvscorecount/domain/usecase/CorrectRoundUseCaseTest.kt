package com.rvetiveau.rvscorecount.domain.usecase

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
import org.junit.Assert.assertTrue
import org.junit.Test

class CorrectRoundUseCaseTest {

    private class FakePartyRepository(
        var party: Party,
        private val participants: List<Participant>,
        val rounds: MutableList<Round>,
        val roundScores: MutableList<RoundScore>
    ) : PartyRepository {
        var saveFailure: Throwable? = null
        var correctInvocationCount = 0
            private set

        override suspend fun createParty(party: Party) = error("not used in this test")
        override suspend fun updateParty(party: Party) = error("not used in this test")
        override suspend fun createPartyWithParticipants(party: Party, participants: List<Participant>) = error("not used in this test")
        override suspend fun getPartyById(id: UUID): Party? = party.takeIf { it.id == id }
        override fun observeParty(id: UUID): Flow<Party?> = error("not used in this test")
        override suspend fun getPartyInProgress(): Party? = error("not used in this test")
        override fun observeFinishedParties(): Flow<List<Party>> = error("not used in this test")
        override suspend fun deleteParty(id: UUID) = error("not used in this test")
        override suspend fun addParticipant(participant: Participant, updatedParty: Party) = error("not used in this test")
        override suspend fun getParticipantsByPartyId(partyId: UUID): List<Participant> = participants
        override fun observeParticipants(partyId: UUID): Flow<List<Participant>> = error("not used in this test")
        override suspend fun saveRound(round: Round, scores: Map<UUID, Int>, updatedParty: Party) = error("not used in this test")
        override suspend fun correctRound(round: Round, scores: Map<UUID, Int>) {
            correctInvocationCount++
            saveFailure?.let { throw it }
            rounds.replaceAll { if (it.id == round.id) round else it }
            roundScores.replaceAll { score ->
                if (score.roundId == round.id) score.copy(points = scores.getValue(score.participantId)) else score
            }
        }
        override suspend fun getRoundsByPartyId(partyId: UUID): List<Round> = rounds.filter { it.partyId == partyId }
        override suspend fun getScoresByRoundId(roundId: UUID): List<RoundScore> = roundScores.filter { it.roundId == roundId }
    }

    private val partyId = UUID.randomUUID()
    private val alice = Participant(UUID.randomUUID(), partyId, "Alice", 0)
    private val bob = Participant(UUID.randomUUID(), partyId, "Bob", 1)
    /** Joined after round 1. */
    private val charlie = Participant(UUID.randomUUID(), partyId, "Charlie", 2)
    private val round = Round(UUID.randomUUID(), partyId, 1, CardSuit.DIAMOND, sevenHolderId = alice.id)

    private fun repository(status: PartyState = PartyState.IN_PROGRESS) = FakePartyRepository(
        party = Party(partyId, PapayooEngine.GAME_ID, status, currentDealerIndex = 2, createdAt = 0L),
        participants = listOf(alice, bob, charlie),
        rounds = mutableListOf(round),
        roundScores = mutableListOf(RoundScore(round.id, alice.id, 150), RoundScore(round.id, bob.id, 100))
    )

    private fun useCase(repository: PartyRepository) =
        CorrectRoundUseCase(repository, mapOf(PapayooEngine.GAME_ID to PapayooEngine()))

    @Test
    fun `rewrites the scores and 7 holder, keeping number, suit and dealer`() = runTest {
        val repository = repository()

        val outcome = useCase(repository)(partyId, 1, mapOf(alice.id to 60, bob.id to 190), bob.id).getOrThrow()

        assertEquals(CorrectRoundOutcome.Corrected(round.copy(sevenHolderId = bob.id)), outcome)
        assertEquals(round.copy(sevenHolderId = bob.id), repository.rounds.single())
        assertEquals(
            mapOf(alice.id to 60, bob.id to 190),
            repository.roundScores.associate { it.participantId to it.points }
        )
        assertEquals(2, repository.party.currentDealerIndex)
    }

    @Test
    fun `a sum mismatch is a business outcome and writes nothing`() = runTest {
        val repository = repository()

        val outcome = useCase(repository)(partyId, 1, mapOf(alice.id to 150, bob.id to 90), alice.id).getOrThrow()

        assertEquals(CorrectRoundOutcome.SumMismatch(-10), outcome)
        assertEquals(0, repository.correctInvocationCount)
    }

    @Test
    fun `scores must cover exactly the participants who played the round`() = runTest {
        val repository = repository()

        val withLateComer = useCase(repository)(
            partyId, 1, mapOf(alice.id to 150, bob.id to 50, charlie.id to 50), alice.id
        )
        val missing = useCase(repository)(partyId, 1, mapOf(alice.id to 250), alice.id)

        assertTrue(withLateComer.exceptionOrNull() is InvalidRoundException)
        assertTrue(missing.exceptionOrNull() is InvalidRoundException)
        assertEquals(0, repository.correctInvocationCount)
    }

    @Test
    fun `the 7 holder must have played the round and scored at least 40`() = runTest {
        val repository = repository()
        val scores = mapOf(alice.id to 220, bob.id to 30)

        val tooLow = useCase(repository)(partyId, 1, scores, bob.id)
        val absent = useCase(repository)(partyId, 1, scores, charlie.id)
        val missing = useCase(repository)(partyId, 1, scores, null)

        listOf(tooLow, absent, missing).forEach { assertTrue(it.exceptionOrNull() is InvalidRoundException) }
        assertEquals(0, repository.correctInvocationCount)
    }

    @Test
    fun `an unknown round or a finished party is rejected`() = runTest {
        val scores = mapOf(alice.id to 150, bob.id to 100)

        val unknown = useCase(repository())(partyId, 2, scores, alice.id)
        val finished = useCase(repository(PartyState.FINISHED))(partyId, 1, scores, alice.id)

        assertTrue(unknown.exceptionOrNull() is InvalidRoundException)
        assertTrue(finished.exceptionOrNull() is InvalidRoundException)
    }

    @Test
    fun `a storage failure is reported as a persistence error`() = runTest {
        val repository = repository().apply { saveFailure = IllegalStateException("disk full") }

        val result = useCase(repository)(partyId, 1, mapOf(alice.id to 150, bob.id to 100), alice.id)

        assertTrue(result.exceptionOrNull() is RoundPersistenceException)
    }
}
