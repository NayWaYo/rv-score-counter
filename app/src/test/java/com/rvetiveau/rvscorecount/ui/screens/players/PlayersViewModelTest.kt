package com.rvetiveau.rvscorecount.ui.screens.players

import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.domain.model.PlayerSummary
import com.rvetiveau.rvscorecount.domain.usecase.FakePlayerRepository
import com.rvetiveau.rvscorecount.domain.usecase.ObservePlayersUseCase
import com.rvetiveau.rvscorecount.domain.usecase.RenamePlayerUseCase
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlayersViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val alice = PlayerSummary(UUID.randomUUID(), "Alice", finishedParties = 3, lastPlayedAt = 20)
    private val bob = PlayerSummary(UUID.randomUUID(), "Bob", finishedParties = 0, lastPlayedAt = null)

    private fun viewModel(repository: FakePlayerRepository) =
        PlayersViewModel(ObservePlayersUseCase(repository), RenamePlayerUseCase(repository))

    private fun PlayersViewModel.loaded() = uiState.value as PlayersUiState.Loaded

    @Test
    fun `lists the players in play order, or says there is none`() = runTest {
        assertEquals(listOf(alice, bob), viewModel(FakePlayerRepository(players = listOf(bob, alice))).loaded().players)
        assertEquals(PlayersUiState.Empty, viewModel(FakePlayerRepository()).uiState.value)
    }

    @Test
    fun `renaming opens a dialog with the current name, and the list follows the new name`() = runTest {
        val vm = viewModel(FakePlayerRepository(players = listOf(alice, bob)))

        vm.onRenameRequested(alice.id)
        assertEquals("Alice", vm.loaded().renaming?.input)
        vm.onRenameInputChanged("Alicia")
        vm.onRenameConfirmed()

        assertNull(vm.loaded().renaming)
        assertEquals(listOf("Alicia", "Bob"), vm.loaded().players.map { it.name })
    }

    @Test
    fun `a refused name keeps the dialog open with its message until the name changes`() = runTest {
        val vm = viewModel(FakePlayerRepository(players = listOf(alice, bob)))
        vm.onRenameRequested(alice.id)

        vm.onRenameInputChanged(" BOB ")
        vm.onRenameConfirmed()
        assertEquals(R.string.players_rename_taken, vm.loaded().renaming?.errorRes)
        assertEquals("Bob", vm.loaded().renaming?.takenBy)

        vm.onRenameInputChanged("")
        assertNull(vm.loaded().renaming?.errorRes)
        vm.onRenameConfirmed()
        assertEquals(R.string.setup_error_empty_name, vm.loaded().renaming?.errorRes)
        assertEquals(listOf("Alice", "Bob"), vm.loaded().players.map { it.name })
    }

    @Test
    fun `a storage failure keeps the dialog open with an error`() = runTest {
        val repository = FakePlayerRepository(players = listOf(alice)).apply { renameFailure = IOException("disk") }
        val vm = viewModel(repository)
        vm.onRenameRequested(alice.id)
        vm.onRenameInputChanged("Alicia")

        vm.onRenameConfirmed()

        assertEquals(R.string.players_rename_failed, vm.loaded().renaming?.errorRes)
        assertEquals(false, vm.loaded().renaming?.isSaving)
    }

    @Test
    fun `cancelling closes the dialog without renaming`() = runTest {
        val vm = viewModel(FakePlayerRepository(players = listOf(alice)))
        vm.onRenameRequested(alice.id)
        vm.onRenameInputChanged("Alicia")

        vm.onRenameDismissed()

        assertNull(vm.loaded().renaming)
        assertEquals(listOf("Alice"), vm.loaded().players.map { it.name })
    }
}
