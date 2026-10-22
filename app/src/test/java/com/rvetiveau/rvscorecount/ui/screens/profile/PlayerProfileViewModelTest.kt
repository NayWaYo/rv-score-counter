package com.rvetiveau.rvscorecount.ui.screens.profile

import com.rvetiveau.rvscorecount.domain.model.PartyState
import com.rvetiveau.rvscorecount.domain.model.PlayerSummary
import com.rvetiveau.rvscorecount.domain.repository.PlayerRepository
import com.rvetiveau.rvscorecount.domain.repository.PlayerSeat
import com.rvetiveau.rvscorecount.domain.usecase.FakeBeloteStore
import com.rvetiveau.rvscorecount.domain.usecase.FakeFreeGameRepository
import com.rvetiveau.rvscorecount.domain.usecase.FakePlayerRepository
import com.rvetiveau.rvscorecount.domain.usecase.DeletePlayerUseCase
import com.rvetiveau.rvscorecount.domain.usecase.GetPlayerRecordsUseCase
import com.rvetiveau.rvscorecount.domain.usecase.MergePlayersUseCase
import com.rvetiveau.rvscorecount.domain.usecase.ObservePlayersUseCase
import com.rvetiveau.rvscorecount.game.belote.BeloteEngine
import com.rvetiveau.rvscorecount.game.free.FreeGameEngine
import com.rvetiveau.rvscorecount.game.papayoo.PapayooEngine
import com.rvetiveau.rvscorecount.ui.navigation.NavigationEvent
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class PlayerProfileViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val alice = PlayerSummary(UUID.randomUUID(), "Alice", finishedParties = 0, lastPlayedAt = null)
    private val alicia = PlayerSummary(UUID.randomUUID(), "Alicia", finishedParties = 0, lastPlayedAt = 5)
    private val store = FakeBeloteStore()
    private val players = FakePlayerRepository(players = listOf(alice, alicia))

    private fun viewModel(repository: PlayerRepository = players) = PlayerProfileViewModel(
        GetPlayerRecordsUseCase(
            repository, store, store, BeloteEngine(), mapOf(PapayooEngine.GAME_ID to PapayooEngine()), FakeFreeGameRepository(), FreeGameEngine()
        ),
        ObservePlayersUseCase(repository),
        MergePlayersUseCase(repository, store),
        DeletePlayerUseCase(repository)
    )

    private fun PlayerProfileViewModel.loaded() = uiState.value as PlayerProfileUiState.Loaded

    // --- Story 10.4 ---

    @Test
    fun `merging asks for the other player, confirms, then reloads the profile`() = runTest {
        val vm = viewModel().apply { initialize(alice.id) }
        assertEquals(listOf(alicia), vm.loaded().otherPlayers)

        vm.onMergeRequested()
        assertEquals(ProfileDialog.ChooseMergeTarget, vm.loaded().dialog)
        vm.onMergeTargetSelected(alicia.id)
        assertEquals(ProfileDialog.ConfirmMerge(alicia), vm.loaded().dialog)
        vm.onMergeConfirmed()

        assertEquals(listOf(alice.id to alicia.id), players.merges)
        assertNull(vm.loaded().dialog)
        assertEquals(emptyList<PlayerSummary>(), vm.loaded().otherPlayers)
    }

    @Test
    fun `players who sat together are refused with the party, and nothing is merged`() = runTest {
        val shared = store.seedParty(status = PartyState.FINISHED)
        players.seats[alice.id] = listOf(PlayerSeat(shared.id, UUID.randomUUID()))
        players.seats[alicia.id] = listOf(PlayerSeat(shared.id, UUID.randomUUID()))
        val vm = viewModel().apply { initialize(alice.id) }

        vm.onMergeRequested()
        vm.onMergeTargetSelected(alicia.id)
        vm.onMergeConfirmed()

        assertEquals(ProfileDialog.MergeRefused(alicia, "belote", shared.createdAt), vm.loaded().dialog)
        assertTrue(players.merges.isEmpty())
    }

    @Test
    fun `a failed merge keeps the confirmation open with an error`() = runTest {
        players.writeFailure = IOException("disk")
        val vm = viewModel().apply { initialize(alice.id) }

        vm.onMergeRequested()
        vm.onMergeTargetSelected(alicia.id)
        vm.onMergeConfirmed()

        assertEquals(ProfileDialog.ConfirmMerge(alicia, isSaving = false, failed = true), vm.loaded().dialog)
    }

    @Test
    fun `a player without any party can be deleted, which goes back`() = runTest {
        val vm = viewModel().apply { initialize(alice.id) }

        vm.onDeleteRequested()
        assertEquals(ProfileDialog.ConfirmDelete(), vm.loaded().dialog)
        vm.onDeleteConfirmed()

        assertEquals(NavigationEvent.NavigateBack, vm.navigationEvents.first())
        assertEquals(listOf("Alicia"), players.players.value.map { it.name })
    }

    @Test
    fun `a player who still has a party is not offered the deletion`() = runTest {
        val vm = viewModel().apply { initialize(alicia.id) }

        vm.onDeleteRequested()

        assertNull(vm.loaded().dialog)
    }

    @Test
    fun `cancelling a dialog closes it`() = runTest {
        val vm = viewModel().apply { initialize(alice.id) }
        vm.onMergeRequested()

        vm.onDialogDismissed()

        assertNull(vm.loaded().dialog)
    }

    @Test
    fun `a known player's records are loaded once`() = runTest {
        val vm = viewModel()

        vm.initialize(alice.id)

        val state = vm.uiState.value as PlayerProfileUiState.Loaded
        assertEquals("Alice", state.records.player.name)
    }

    @Test
    fun `an unknown player is not found`() = runTest {
        val vm = viewModel()

        vm.initialize(UUID.randomUUID())

        assertEquals(PlayerProfileUiState.NotFound, vm.uiState.value)
    }

    @Test
    fun `a storage failure is a load error`() = runTest {
        val failing = object : PlayerRepository by players {
            override suspend fun getSeats(playerId: UUID): List<PlayerSeat> = throw IOException("disk")
        }
        val vm = viewModel(failing)

        vm.initialize(alice.id)

        assertTrue(vm.uiState.value is PlayerProfileUiState.LoadError)
    }
}
