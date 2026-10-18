package com.rvetiveau.rvscorecount.ui.screens.results

import com.rvetiveau.rvscorecount.domain.model.CardSuit
import com.rvetiveau.rvscorecount.domain.model.Participant
import com.rvetiveau.rvscorecount.domain.model.Party
import com.rvetiveau.rvscorecount.domain.model.PartyState
import com.rvetiveau.rvscorecount.domain.model.Round
import com.rvetiveau.rvscorecount.domain.model.RoundScore
import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.domain.repository.PartyRepository
import com.rvetiveau.rvscorecount.domain.usecase.CreatePartyUseCase
import com.rvetiveau.rvscorecount.game.papayoo.PapayooEngine
import com.rvetiveau.rvscorecount.ui.navigation.NavigationEvent
import com.rvetiveau.rvscorecount.ui.navigation.Routes
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ResultsViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakePartyRepository(
        private val party: Party?,
        private val participants: List<Participant>,
        private val rounds: List<Round> = emptyList(),
        private val roundScores: List<RoundScore> = emptyList(),
        var failure: Throwable? = null
    ) : PartyRepository {
        override suspend fun createParty(party: Party) = error("not used in this test")
        override suspend fun updateParty(party: Party) = error("not used in this test")
        var createFailure: Throwable? = null
        var createdParty: Party? = null
            private set
        var createCount = 0
            private set

        override suspend fun createPartyWithParticipants(party: Party, participants: List<Participant>) {
            createCount++
            createFailure?.let { throw it }
            createdParty = party
        }
        override suspend fun getPartyById(id: UUID): Party? {
            failure?.let { throw it }
            return party
        }
        override fun observeParty(id: UUID): Flow<Party?> = error("not used in this test")
        override suspend fun getPartyInProgress(): Party? = error("not used in this test")
        override suspend fun addParticipant(participant: Participant, updatedParty: Party) = error("not used in this test")
        override suspend fun getParticipantsByPartyId(partyId: UUID): List<Participant> = participants
        override fun observeParticipants(partyId: UUID): Flow<List<Participant>> = error("not used in this test")
        override suspend fun saveRound(round: Round, scores: Map<UUID, Int>, updatedParty: Party) = error("not used in this test")
        override suspend fun getRoundsByPartyId(partyId: UUID): List<Round> = rounds
        override suspend fun getScoresByRoundId(roundId: UUID): List<RoundScore> = roundScores.filter { it.roundId == roundId }
    }

    private val partyId = UUID.randomUUID()
    private val alice = Participant(UUID.randomUUID(), partyId, "Alice", 0)
    private val bob = Participant(UUID.randomUUID(), partyId, "Bob", 1)
    private val chloe = Participant(UUID.randomUUID(), partyId, "Chloé", 2)
    private val round = Round(UUID.randomUUID(), partyId, 1, CardSuit.HEART)

    private fun aParty(status: PartyState = PartyState.FINISHED) =
        Party(id = partyId, gameId = "papayoo", status = status, currentDealerIndex = 0, createdAt = 0L)

    private fun scores(vararg points: Pair<Participant, Int>) = points.map { (p, value) -> RoundScore(round.id, p.id, value) }

    private fun viewModel(repository: PartyRepository) = ResultsViewModel(
        repository,
        CreatePartyUseCase(repository, mapOf(PapayooEngine.GAME_ID to PapayooEngine()))
    )

    @Test
    fun `shows the final classement with every tied winner`() = runTest {
        val repository = FakePartyRepository(
            aParty(), listOf(alice, bob, chloe), listOf(round), scores(alice to 100, bob to 50, chloe to 100)
        )
        val vm = viewModel(repository)

        vm.initialize(partyId)

        val state = vm.uiState.value as ResultsUiState.Loaded
        assertEquals(listOf("Bob", "Alice", "Chloé"), state.standings.map { it.name })
        assertEquals(listOf(1, 2, 2), state.standings.map { it.rank })
        assertEquals(listOf("Bob"), state.winners.map { it.name })

        val tied = viewModel(
            FakePartyRepository(aParty(), listOf(alice, bob, chloe), listOf(round), scores(alice to 50, bob to 50, chloe to 150))
        ).also { it.initialize(partyId) }
        assertEquals(listOf("Alice", "Bob"), (tied.uiState.value as ResultsUiState.Loaded).winners.map { it.name })
    }

    @Test
    fun `an in-progress, missing or inconsistent party has no results`() = runTest {
        val stray = RoundScore(round.id, UUID.randomUUID(), 250)
        listOf(
            FakePartyRepository(aParty(PartyState.IN_PROGRESS), listOf(alice)),
            FakePartyRepository(null, listOf(alice)),
            FakePartyRepository(aParty(), listOf(alice), listOf(round), listOf(stray))
        ).forEach { repository ->
            val vm = viewModel(repository).also { it.initialize(partyId) }
            assertEquals(ResultsUiState.Unavailable, vm.uiState.value)
        }
    }

    @Test
    fun `a read failure shows the error state and retry recovers`() = runTest {
        val repository = FakePartyRepository(aParty(), listOf(alice), failure = IllegalStateException("Room read failed"))
        val vm = viewModel(repository)

        vm.initialize(partyId)
        assertEquals(ResultsUiState.LoadError, vm.uiState.value)

        repository.failure = null
        vm.retry(partyId)
        assertEquals(listOf("Alice"), (vm.uiState.value as ResultsUiState.Loaded).winners.map { it.name })
    }

    // --- Story 5.2 ---

    @Test
    fun `replay creates one new party and replaces the stack with it`() = runTest {
        val repository = FakePartyRepository(aParty(), listOf(alice, bob, chloe))
        val vm = viewModel(repository).also { it.initialize(partyId) }

        vm.onReplay()
        vm.onReplay()

        val newParty = repository.createdParty!!
        assertEquals(NavigationEvent.ReplaceAll(Routes.Party(newParty.id.toString())), vm.navigationEvents.first())
        assertEquals(1, repository.createCount)
        assertTrue((vm.uiState.value as ResultsUiState.Loaded).isReplaying)
    }

    @Test
    fun `a failed replay shows an error and can be retried`() = runTest {
        val repository = FakePartyRepository(aParty(), listOf(alice, bob, chloe))
        repository.createFailure = IllegalStateException("Room write failed")
        val vm = viewModel(repository).also { it.initialize(partyId) }

        vm.onReplay()

        val state = vm.uiState.value as ResultsUiState.Loaded
        assertEquals(R.string.results_replay_error, state.replayErrorRes)
        assertTrue(!state.isReplaying)

        repository.createFailure = null
        vm.onReplay()
        assertEquals(NavigationEvent.ReplaceAll(Routes.Party(repository.createdParty!!.id.toString())), vm.navigationEvents.first())
    }

    @Test
    fun `back to home replaces the whole stack with the home screen`() = runTest {
        val vm = viewModel(FakePartyRepository(aParty(), listOf(alice)))

        vm.onBackToHome()

        assertEquals(NavigationEvent.ReplaceAll(Routes.Home), vm.navigationEvents.first())
    }
}
