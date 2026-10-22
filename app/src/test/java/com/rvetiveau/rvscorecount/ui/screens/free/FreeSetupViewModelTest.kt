package com.rvetiveau.rvscorecount.ui.screens.free

import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.domain.model.EndRule
import com.rvetiveau.rvscorecount.domain.model.Participant
import com.rvetiveau.rvscorecount.domain.model.Party
import com.rvetiveau.rvscorecount.domain.model.PlayedGameName
import com.rvetiveau.rvscorecount.domain.model.WinDirection
import com.rvetiveau.rvscorecount.domain.usecase.CreateFreePartyUseCase
import com.rvetiveau.rvscorecount.domain.usecase.FakeFreeGameRepository
import com.rvetiveau.rvscorecount.domain.usecase.FakePlayerRepository
import com.rvetiveau.rvscorecount.domain.usecase.ObserveFreeGameNamesUseCase
import com.rvetiveau.rvscorecount.domain.usecase.ObserveKnownPlayerNamesUseCase
import com.rvetiveau.rvscorecount.domain.usecase.PartyRepositoryAdapter
import com.rvetiveau.rvscorecount.game.free.FreeGameEngine
import com.rvetiveau.rvscorecount.ui.navigation.NavigationEvent
import com.rvetiveau.rvscorecount.ui.navigation.Routes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FreeSetupViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class Store : PartyRepositoryAdapter() {
        var party: Party? = null
        var participants: List<Participant> = emptyList()

        override suspend fun createPartyWithParticipants(party: Party, participants: List<Participant>) {
            this.party = party
            this.participants = participants
        }
    }

    private val store = Store()
    private val gameNames = FakeFreeGameRepository(listOf(PlayedGameName("Skyjo", 20), PlayedGameName("Uno", 10)))

    private fun viewModel() = FreeSetupViewModel(
        CreateFreePartyUseCase(store, FreeGameEngine()),
        FreeGameEngine(),
        ObserveKnownPlayerNamesUseCase(FakePlayerRepository("Alice", "Bob")),
        ObserveFreeGameNamesUseCase(gameNames)
    )

    private fun FreeSetupViewModel.addPlayers(vararg names: String) = names.forEach { onSuggestionSelected(it) }

    @Test
    fun `starts with an end by hand and the lowest total winning, and cannot start yet`() {
        val state = viewModel().uiState.value

        assertEquals(FreeEndMode.MANUAL, state.endMode)
        assertEquals(WinDirection.LOWEST_WINS, state.winDirection)
        assertEquals(2, state.minPlayers)
        assertEquals(10, state.maxPlayers)
        assertFalse(state.canStart)
    }

    @Test
    fun `start needs a game name and two players`() {
        val vm = viewModel()

        vm.addPlayers("Alice", "Bob")
        assertFalse(vm.uiState.value.canStart)
        vm.onGameNameChanged("Skyjo")

        assertTrue(vm.uiState.value.canStart)
    }

    @Test
    fun `a game name longer than 30 characters is not taken`() {
        val vm = viewModel()

        vm.onGameNameChanged("x".repeat(31))

        assertEquals("", vm.uiState.value.gameNameInput)
    }

    @Test
    fun `known game names are filtered by the typed text and hidden once one is chosen`() {
        val vm = viewModel()
        assertEquals(listOf("Skyjo", "Uno"), vm.uiState.value.gameNameSuggestions)

        vm.onGameNameChanged("sk")
        assertEquals(listOf("Skyjo"), vm.uiState.value.gameNameSuggestions)
        vm.onGameNameSuggestionSelected("Skyjo")

        assertEquals("Skyjo", vm.uiState.value.gameNameInput)
        assertEquals(emptyList<String>(), vm.uiState.value.gameNameSuggestions)
    }

    @Test
    fun `players follow the papayoo rules, duplicates refused`() {
        val vm = viewModel()

        vm.addPlayers("Alice")
        vm.onNameChanged(" alice")
        vm.onAddParticipant()

        assertEquals(listOf("Alice"), vm.uiState.value.participants.map { it.name })
        assertEquals(R.string.setup_error_duplicate_name, vm.uiState.value.messageRes)
        vm.onNameChanged("")
        assertEquals(listOf("Bob"), vm.uiState.value.nameSuggestions)
    }

    @Test
    fun `an end value must be a number from 1 to 9999, each mode keeping its own`() {
        val vm = viewModel().apply {
            addPlayers("Alice", "Bob")
            onGameNameChanged("Skyjo")
        }

        vm.onEndModeChanged(FreeEndMode.TARGET_SCORE)
        assertEquals("100", vm.uiState.value.endValueInput)
        vm.onEndValueChanged("0")
        assertFalse(vm.uiState.value.canStart)
        vm.onEndValueChanged("12a")
        assertEquals("0", vm.uiState.value.endValueInput)
        vm.onEndValueChanged("150")
        assertEquals(EndRule.TargetScore(150), vm.uiState.value.endRule)
        vm.onEndModeChanged(FreeEndMode.ROUND_COUNT)
        assertEquals(EndRule.RoundCount(10), vm.uiState.value.endRule)
        vm.onEndModeChanged(FreeEndMode.MANUAL)

        assertNull(vm.uiState.value.endRule)
        assertTrue(vm.uiState.value.canStart)
    }

    @Test
    fun `starting creates the party with its rules and replaces the stack with it`() = runTest {
        val vm = viewModel().apply {
            addPlayers("Bob", "Alice")
            onGameNameChanged(" Yams ")
            onWinDirectionChanged(WinDirection.HIGHEST_WINS)
            onEndModeChanged(FreeEndMode.ROUND_COUNT)
            onEndValueChanged("13")
        }

        vm.onStartParty()

        val party = checkNotNull(store.party)
        assertEquals("Yams", party.gameName)
        assertEquals(WinDirection.HIGHEST_WINS, party.winDirection)
        assertEquals(EndRule.RoundCount(13), party.endRule)
        assertEquals(listOf("Bob", "Alice"), store.participants.map { it.name })
        assertEquals(NavigationEvent.ReplaceAll(Routes.FreeParty(party.id.toString())), vm.navigationEvents.first())
        assertFalse(vm.uiState.value.canStart)
    }

    @Test
    fun `the history button opens the history of the jeu libre`() = runTest {
        val vm = viewModel()

        vm.onHistorySelected()

        assertEquals(NavigationEvent.NavigateTo(Routes.GameHistory("free")), vm.navigationEvents.first())
    }
}
