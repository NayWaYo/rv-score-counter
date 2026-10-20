package com.rvetiveau.rvscorecount.ui.screens.partydetail

import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.domain.model.CardSuit
import com.rvetiveau.rvscorecount.domain.model.Participant
import com.rvetiveau.rvscorecount.domain.model.Party
import com.rvetiveau.rvscorecount.domain.model.PartyState
import com.rvetiveau.rvscorecount.domain.model.Round
import com.rvetiveau.rvscorecount.domain.model.RoundScore
import com.rvetiveau.rvscorecount.domain.repository.PartyRepository
import com.rvetiveau.rvscorecount.domain.usecase.CreatePartyUseCase
import com.rvetiveau.rvscorecount.game.papayoo.PapayooEngine
import com.rvetiveau.rvscorecount.ui.components.RoundRecordEntryUi
import com.rvetiveau.rvscorecount.ui.components.RoundRecordUi
import com.rvetiveau.rvscorecount.ui.components.RoundTableRow
import com.rvetiveau.rvscorecount.ui.components.SevenChampionUi
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PartyDetailViewModelTest {

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
        private val roundScores: List<RoundScore> = emptyList()
    ) : PartyRepository {
        var readFailure: Throwable? = null
        var createFailure: Throwable? = null
        var createdParty: Party? = null
            private set

        override suspend fun createParty(party: Party) = error("not used in this test")
        override suspend fun updateParty(party: Party) = error("not used in this test")
        override suspend fun createPartyWithParticipants(party: Party, participants: List<Participant>) {
            createFailure?.let { throw it }
            createdParty = party
        }
        override suspend fun getPartyById(id: UUID): Party? {
            readFailure?.let { throw it }
            return party
        }
        override fun observeParty(id: UUID): Flow<Party?> = error("not used in this test")
        override suspend fun getPartyInProgress(): Party? = error("not used in this test")
        override fun observeFinishedParties(): Flow<List<Party>> = error("not used in this test")
        override suspend fun deleteParty(id: UUID) = error("not used in this test")
        override suspend fun addParticipant(participant: Participant, updatedParty: Party) = error("not used in this test")
        override suspend fun getParticipantsByPartyId(partyId: UUID): List<Participant> = participants
        override fun observeParticipants(partyId: UUID): Flow<List<Participant>> = error("not used in this test")
        override suspend fun saveRound(round: Round, scores: Map<UUID, Int>, updatedParty: Party) = error("not used in this test")
        override suspend fun correctRound(round: Round, scores: Map<UUID, Int>) = error("not used in this test")
        override suspend fun getRoundsByPartyId(partyId: UUID): List<Round> = rounds
        override suspend fun getScoresByRoundId(roundId: UUID): List<RoundScore> = roundScores.filter { it.roundId == roundId }
    }

    private val partyId = UUID.randomUUID()
    private val alice = Participant(UUID.randomUUID(), partyId, "Alice", 0)
    private val bob = Participant(UUID.randomUUID(), partyId, "Bob", 1)
    private val round1 = Round(UUID.randomUUID(), partyId, 1, CardSuit.HEART)
    private val round2 = Round(UUID.randomUUID(), partyId, 2, CardSuit.SPADE)

    private fun aParty(status: PartyState = PartyState.FINISHED) =
        Party(id = partyId, gameId = "papayoo", status = status, currentDealerIndex = 0, createdAt = 42L)

    private fun playedRepository() = FakePartyRepository(
        aParty(),
        listOf(alice, bob),
        // Stored newest first on purpose: the detail must still be chronological.
        listOf(round2, round1),
        listOf(
            RoundScore(round1.id, bob.id, 50), RoundScore(round1.id, alice.id, 200),
            RoundScore(round2.id, alice.id, 0), RoundScore(round2.id, bob.id, 250)
        )
    )

    private fun viewModel(repository: PartyRepository) = PartyDetailViewModel(
        repository,
        CreatePartyUseCase(repository, mapOf(PapayooEngine.GAME_ID to PapayooEngine()))
    )

    @Test
    fun `shows the final classement and the rounds in chronological order`() = runTest {
        val vm = viewModel(playedRepository()).also { it.initialize(partyId) }

        val state = vm.uiState.value as PartyDetailUiState.Loaded
        assertEquals(42L, state.createdAt)
        assertEquals(listOf("Alice", "Bob"), state.standings.map { it.name })
        assertEquals(listOf(200, 300), state.standings.map { it.cumulativeScore })
        assertEquals(listOf("Alice"), state.winners.map { it.name })
        assertEquals(
            listOf(
                RoundTableRow(1, CardSuit.HEART, mapOf(alice.id to 200, bob.id to 50)),
                RoundTableRow(2, CardSuit.SPADE, mapOf(alice.id to 0, bob.id to 250))
            ),
            state.roundTable.rows
        )
        assertEquals(listOf("Alice", "Bob"), state.roundTable.players.map { it.name })
        assertEquals(RoundRecordUi(listOf(RoundRecordEntryUi("Bob", 2)), 250), state.roundRecord)
    }

    @Test
    fun `a party without rounds has no winner`() = runTest {
        val vm = viewModel(FakePartyRepository(aParty(), listOf(alice, bob))).also { it.initialize(partyId) }

        val state = vm.uiState.value as PartyDetailUiState.Loaded
        assertTrue(state.roundTable.rows.isEmpty())
        assertNull(state.roundRecord)
        assertTrue(state.winners.isEmpty())
    }

    @Test
    fun `an in-progress, missing or inconsistent party has no detail`() = runTest {
        listOf(
            FakePartyRepository(aParty(PartyState.IN_PROGRESS), listOf(alice)),
            FakePartyRepository(null, listOf(alice)),
            FakePartyRepository(aParty(), listOf(alice), listOf(round1), listOf(RoundScore(round1.id, UUID.randomUUID(), 250)))
        ).forEach { repository ->
            val vm = viewModel(repository).also { it.initialize(partyId) }
            assertEquals(PartyDetailUiState.Unavailable, vm.uiState.value)
        }
    }

    @Test
    fun `a read failure shows the error state and retry recovers`() = runTest {
        val repository = playedRepository().also { it.readFailure = IllegalStateException("Room read failed") }
        val vm = viewModel(repository).also { it.initialize(partyId) }
        assertEquals(PartyDetailUiState.LoadError, vm.uiState.value)

        repository.readFailure = null
        vm.retry(partyId)

        assertTrue(vm.uiState.value is PartyDetailUiState.Loaded)
    }

    @Test
    fun `replay creates a new party and replaces the stack with it`() = runTest {
        // Papayoo needs at least 3 players, so the relaunch needs a full table.
        val chloe = Participant(UUID.randomUUID(), partyId, "Chloé", 2)
        val repository = FakePartyRepository(aParty(), listOf(alice, bob, chloe))
        val vm = viewModel(repository).also { it.initialize(partyId) }

        vm.onReplay()

        assertEquals(NavigationEvent.ReplaceAll(Routes.Party(repository.createdParty!!.id.toString())), vm.navigationEvents.first())
    }

    @Test
    fun `a failed replay shows an error`() = runTest {
        val repository = playedRepository().also { it.createFailure = IllegalStateException("Room write failed") }
        val vm = viewModel(repository).also { it.initialize(partyId) }

        vm.onReplay()

        val state = vm.uiState.value as PartyDetailUiState.Loaded
        assertEquals(R.string.results_replay_error, state.replayErrorRes)
        assertTrue(!state.isReplaying)
    }

    @Test
    fun `shows who took the 40 in each round and the 40 champion`() = runTest {
        val r1 = round1.copy(sevenHolderId = alice.id)
        val r2 = round2.copy(sevenHolderId = bob.id)
        val r3 = Round(UUID.randomUUID(), partyId, 3, CardSuit.CLUB, sevenHolderId = bob.id)
        val repository = FakePartyRepository(
            aParty(),
            listOf(alice, bob),
            listOf(r1, r2, r3),
            listOf(
                RoundScore(r1.id, alice.id, 200), RoundScore(r1.id, bob.id, 50),
                RoundScore(r2.id, alice.id, 0), RoundScore(r2.id, bob.id, 250),
                RoundScore(r3.id, alice.id, 100), RoundScore(r3.id, bob.id, 150)
            )
        )
        val vm = viewModel(repository).also { it.initialize(partyId) }

        val state = vm.uiState.value as PartyDetailUiState.Loaded
        assertEquals(listOf(alice.id, bob.id, bob.id), state.roundTable.rows.map { it.sevenHolderId })
        assertEquals(SevenChampionUi(listOf("Bob"), 2), state.sevenChampion)
    }
}
