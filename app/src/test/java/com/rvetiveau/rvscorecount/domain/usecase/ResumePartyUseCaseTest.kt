package com.rvetiveau.rvscorecount.domain.usecase

import com.rvetiveau.rvscorecount.domain.model.Participant
import com.rvetiveau.rvscorecount.domain.model.Party
import com.rvetiveau.rvscorecount.domain.model.PartyState
import com.rvetiveau.rvscorecount.domain.model.Round
import com.rvetiveau.rvscorecount.domain.model.RoundScore
import com.rvetiveau.rvscorecount.domain.repository.PartyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import java.util.UUID

class ResumePartyUseCaseTest {

    private class FakePartyRepository(
        private val partyInProgress: Party? = null,
        private val failure: Throwable? = null
    ) : PartyRepository {
        override suspend fun createParty(party: Party) = error("not used in this test")
        override suspend fun updateParty(party: Party) = error("not used in this test")
        override suspend fun getPartyById(id: UUID): Party? = error("not used in this test")
        override fun observeParty(id: UUID): Flow<Party?> = flowOf(null)
        override suspend fun getPartyInProgress(): Party? {
            failure?.let { throw it }
            return partyInProgress
        }
        override suspend fun addParticipant(participant: Participant) = error("not used in this test")
        override suspend fun getParticipantsByPartyId(partyId: UUID): List<Participant> = error("not used in this test")
        override fun observeParticipants(partyId: UUID): Flow<List<Participant>> = flowOf(emptyList())
        override suspend fun saveRound(round: Round, scores: Map<UUID, Int>) = error("not used in this test")
        override suspend fun getRoundsByPartyId(partyId: UUID): List<Round> = error("not used in this test")
        override suspend fun getScoresByRoundId(roundId: UUID): List<RoundScore> = error("not used in this test")
    }

    private fun aParty() = Party(
        id = UUID.randomUUID(),
        gameId = "papayoo",
        status = PartyState.IN_PROGRESS,
        currentDealerIndex = 0,
        createdAt = 0L
    )

    @Test
    fun `returns the party in progress when one exists`() = runTest {
        val party = aParty()
        val useCase = ResumePartyUseCase(FakePartyRepository(partyInProgress = party))

        assertEquals(party, useCase())
    }

    @Test
    fun `returns null when no party is in progress`() = runTest {
        val useCase = ResumePartyUseCase(FakePartyRepository(partyInProgress = null))

        assertNull(useCase())
    }

    @Test
    fun `propagates repository failures instead of treating them as no party`() = runTest {
        val failure = IllegalStateException("Room read failed")
        val useCase = ResumePartyUseCase(FakePartyRepository(failure = failure))

        val thrown = try {
            useCase()
            null
        } catch (t: Throwable) {
            t
        }

        assertSame(failure, thrown)
    }
}
