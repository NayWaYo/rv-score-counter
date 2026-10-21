package com.rvetiveau.rvscorecount.ui.screens.history

import androidx.lifecycle.SavedStateHandle
import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.domain.model.Participant
import com.rvetiveau.rvscorecount.domain.model.Party
import com.rvetiveau.rvscorecount.domain.model.PartyState
import com.rvetiveau.rvscorecount.domain.model.Round
import com.rvetiveau.rvscorecount.domain.model.RoundScore
import com.rvetiveau.rvscorecount.domain.repository.PartyRepository
import com.rvetiveau.rvscorecount.domain.usecase.DeletePartyUseCase
import com.rvetiveau.rvscorecount.domain.usecase.ObservePartyHistoryUseCase
import com.rvetiveau.rvscorecount.ui.navigation.NavigationEvent
import com.rvetiveau.rvscorecount.ui.navigation.Routes
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import com.rvetiveau.rvscorecount.domain.usecase.FakeBeloteStore
import com.rvetiveau.rvscorecount.game.belote.BeloteEngine
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakePartyRepository(parties: List<Party>) : PartyRepository {
        val finished = MutableStateFlow(parties)
        var readFailure: Throwable? = null
        var deleteFailure: Throwable? = null

        override suspend fun createParty(party: Party) = error("not used in this test")
        override suspend fun updateParty(party: Party) = error("not used in this test")
        override suspend fun createPartyWithParticipants(party: Party, participants: List<Participant>) = error("not used in this test")
        override suspend fun getPartyById(id: UUID): Party? = finished.value.firstOrNull { it.id == id }
        override fun observeParty(id: UUID): Flow<Party?> = error("not used in this test")
        override suspend fun getPartyInProgress(): Party? = error("not used in this test")
        override fun observeFinishedParties(): Flow<List<Party>> = flow {
            readFailure?.let { throw it }
            emitAll(finished)
        }
        override suspend fun deleteParty(id: UUID) {
            deleteFailure?.let { throw it }
            finished.value = finished.value.filterNot { it.id == id }
        }
        override suspend fun addParticipant(participant: Participant, updatedParty: Party) = error("not used in this test")
        override suspend fun getParticipantsByPartyId(partyId: UUID): List<Participant> = emptyList()
        override fun observeParticipants(partyId: UUID): Flow<List<Participant>> = error("not used in this test")
        override suspend fun saveRound(round: Round, scores: Map<UUID, Int>, updatedParty: Party) = error("not used in this test")
        override suspend fun correctRound(round: Round, scores: Map<UUID, Int>) = error("not used in this test")
        override suspend fun getRoundsByPartyId(partyId: UUID): List<Round> = emptyList()
        override suspend fun getScoresByRoundId(roundId: UUID): List<RoundScore> = emptyList()
    }

    private fun aParty(createdAt: Long) =
        Party(id = UUID.randomUUID(), gameId = "papayoo", status = PartyState.FINISHED, currentDealerIndex = 0, createdAt = createdAt)

    private fun viewModel(repository: PartyRepository, gameId: String? = null) = HistoryViewModel(
        ObservePartyHistoryUseCase(repository, FakeBeloteStore(), BeloteEngine()),
        DeletePartyUseCase(repository),
        SavedStateHandle(listOfNotNull(gameId?.let { "gameId" to it }).toMap())
    )

    @Test
    fun `lists the finished parties in the order given by Room`() = runTest {
        val recent = aParty(20L)
        val old = aParty(10L)
        val vm = viewModel(FakePartyRepository(listOf(recent, old)))

        val state = vm.uiState.value as HistoryUiState.Loaded
        assertEquals(listOf(recent.id, old.id), state.entries.map { it.partyId })
    }

    @Test
    fun `shows the empty state without finished party`() = runTest {
        val vm = viewModel(FakePartyRepository(emptyList()))

        assertEquals(HistoryUiState.Empty, vm.uiState.value)
    }

    @Test
    fun `the history of a game lists only its parties`() = runTest {
        val papayoo = aParty(20L)
        val repository = FakePartyRepository(listOf(papayoo))

        val papayooHistory = viewModel(repository, gameId = "papayoo")
        val beloteHistory = viewModel(repository, gameId = "belote")

        assertEquals("papayoo", papayooHistory.gameId)
        assertEquals(listOf(papayoo.id), (papayooHistory.uiState.value as HistoryUiState.Loaded).entries.map { it.partyId })
        assertEquals(HistoryUiState.Empty, beloteHistory.uiState.value)
        assertEquals(null, viewModel(repository).gameId)
    }

    @Test
    fun `selecting an entry opens its detail`() = runTest {
        val party = aParty(10L)
        val vm = viewModel(FakePartyRepository(listOf(party)))

        vm.onEntrySelected(party.id)

        assertEquals(NavigationEvent.NavigateTo(Routes.PartyDetail(party.id.toString())), vm.navigationEvents.first())
    }

    @Test
    fun `deletion waits for confirmation and the list follows Room`() = runTest {
        val kept = aParty(20L)
        val deleted = aParty(10L)
        val repository = FakePartyRepository(listOf(kept, deleted))
        val vm = viewModel(repository)

        vm.onDeleteRequested(deleted.id)
        assertEquals(deleted.id, (vm.uiState.value as HistoryUiState.Loaded).pendingDeletionId)
        assertEquals(2, repository.finished.value.size)

        vm.onDeleteConfirmed()

        val state = vm.uiState.value as HistoryUiState.Loaded
        assertEquals(listOf(kept.id), state.entries.map { it.partyId })
        assertNull(state.pendingDeletionId)
        assertTrue(!state.isDeleting)
    }

    @Test
    fun `dismissing the confirmation deletes nothing`() = runTest {
        val party = aParty(10L)
        val repository = FakePartyRepository(listOf(party))
        val vm = viewModel(repository)

        vm.onDeleteRequested(party.id)
        vm.onDeleteDismissed()
        vm.onDeleteConfirmed()

        assertNull((vm.uiState.value as HistoryUiState.Loaded).pendingDeletionId)
        assertEquals(1, repository.finished.value.size)
    }

    @Test
    fun `deleting the last party leads to the empty state`() = runTest {
        val party = aParty(10L)
        val vm = viewModel(FakePartyRepository(listOf(party)))

        vm.onDeleteRequested(party.id)
        vm.onDeleteConfirmed()

        assertEquals(HistoryUiState.Empty, vm.uiState.value)
    }

    @Test
    fun `a failed deletion keeps the entry and shows an error`() = runTest {
        val party = aParty(10L)
        val repository = FakePartyRepository(listOf(party)).also { it.deleteFailure = IllegalStateException("Room write failed") }
        val vm = viewModel(repository)

        vm.onDeleteRequested(party.id)
        vm.onDeleteConfirmed()

        val state = vm.uiState.value as HistoryUiState.Loaded
        assertEquals(R.string.history_delete_error, state.deleteErrorRes)
        assertEquals(listOf(party.id), state.entries.map { it.partyId })
    }

    @Test
    fun `a read failure shows the error state and retry recovers`() = runTest {
        val repository = FakePartyRepository(listOf(aParty(10L))).also { it.readFailure = IllegalStateException("Room read failed") }
        val vm = viewModel(repository)
        assertEquals(HistoryUiState.LoadError, vm.uiState.value)

        repository.readFailure = null
        vm.retry()

        assertTrue(vm.uiState.value is HistoryUiState.Loaded)
    }
}
