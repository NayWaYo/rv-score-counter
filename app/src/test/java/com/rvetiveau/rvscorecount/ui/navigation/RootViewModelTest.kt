package com.rvetiveau.rvscorecount.ui.navigation

import com.rvetiveau.rvscorecount.domain.model.Participant
import com.rvetiveau.rvscorecount.domain.model.Party
import com.rvetiveau.rvscorecount.domain.model.PartyState
import com.rvetiveau.rvscorecount.domain.model.Round
import com.rvetiveau.rvscorecount.domain.model.RoundScore
import com.rvetiveau.rvscorecount.domain.repository.PartyRepository
import com.rvetiveau.rvscorecount.domain.usecase.ResumePartyUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@OptIn(ExperimentalCoroutinesApi::class)
class RootViewModelTest {

    private open class FakePartyRepository(
        var partyInProgress: Party? = null,
        var failure: Throwable? = null
    ) : PartyRepository {
        override suspend fun createParty(party: Party) = error("not used in this test")
        override suspend fun updateParty(party: Party) = error("not used in this test")
        override suspend fun createPartyWithParticipants(party: Party, participants: List<Participant>) = error("not used in this test")
        override suspend fun getPartyById(id: UUID): Party? = error("not used in this test")
        override fun observeParty(id: UUID): Flow<Party?> = flowOf(null)
        open override suspend fun getPartyInProgress(): Party? {
            failure?.let { throw it }
            return partyInProgress
        }
        override fun observeFinishedParties(): Flow<List<Party>> = error("not used in this test")
        override suspend fun deleteParty(id: UUID) = error("not used in this test")
        override suspend fun addParticipant(participant: Participant, updatedParty: Party) = error("not used in this test")
        override suspend fun getParticipantsByPartyId(partyId: UUID): List<Participant> = error("not used in this test")
        override fun observeParticipants(partyId: UUID): Flow<List<Participant>> = flowOf(emptyList())
        override suspend fun saveRound(round: Round, scores: Map<UUID, Int>, updatedParty: Party) = error("not used in this test")
        override suspend fun correctRound(round: Round, scores: Map<UUID, Int>) = error("not used in this test")
        override suspend fun getRoundsByPartyId(partyId: UUID): List<Round> = error("not used in this test")
        override suspend fun getScoresByRoundId(roundId: UUID): List<RoundScore> = error("not used in this test")
    }

    private class ControlledPartyRepository : FakePartyRepository() {
        val pendingReads = mutableListOf<Continuation<Party?>>()

        override suspend fun getPartyInProgress(): Party? = suspendCoroutine { continuation ->
            pendingReads += continuation
        }
    }

    private fun aParty() = Party(
        id = UUID.randomUUID(),
        gameId = "papayoo",
        status = PartyState.IN_PROGRESS,
        currentDealerIndex = 0,
        createdAt = 0L
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `emits Ready with Home when no party is in progress`() = runTest {
        val repository = FakePartyRepository(partyInProgress = null)
        val viewModel = RootViewModel(ResumePartyUseCase(repository))

        assertEquals(RootUiState.Ready(Routes.Home), viewModel.uiState.value)
    }

    @Test
    fun `emits Ready with Party route when a party is in progress`() = runTest {
        val party = aParty()
        val repository = FakePartyRepository(partyInProgress = party)
        val viewModel = RootViewModel(ResumePartyUseCase(repository))

        assertEquals(RootUiState.Ready(Routes.Party(party.id.toString())), viewModel.uiState.value)
    }

    @Test
    fun `emits Ready with the Belote party route when a Belote party is in progress`() = runTest {
        val party = aParty().copy(gameId = "belote")
        val viewModel = RootViewModel(ResumePartyUseCase(FakePartyRepository(partyInProgress = party)))

        assertEquals(RootUiState.Ready(Routes.BeloteParty(party.id.toString())), viewModel.uiState.value)
    }

    @Test
    fun `emits Error when the repository fails, and retry recovers once fixed`() = runTest {
        val repository = FakePartyRepository(failure = IllegalStateException("Room read failed"))
        val viewModel = RootViewModel(ResumePartyUseCase(repository))

        assertTrue(viewModel.uiState.value is RootUiState.Error)

        repository.failure = null
        repository.partyInProgress = null
        viewModel.retry()

        assertEquals(RootUiState.Ready(Routes.Home), viewModel.uiState.value)
    }

    @Test
    fun `ignores an obsolete resume result after retry completes`() = runTest {
        val repository = ControlledPartyRepository()
        val viewModel = RootViewModel(ResumePartyUseCase(repository))
        val obsoleteParty = aParty()

        viewModel.retry()
        repository.pendingReads[1].resume(null)
        assertEquals(RootUiState.Ready(Routes.Home), viewModel.uiState.value)

        repository.pendingReads[0].resume(obsoleteParty)
        assertEquals(RootUiState.Ready(Routes.Home), viewModel.uiState.value)
    }
}
