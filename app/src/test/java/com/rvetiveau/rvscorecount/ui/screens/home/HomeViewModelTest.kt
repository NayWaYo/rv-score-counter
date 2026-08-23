package com.rvetiveau.rvscorecount.ui.screens.home

import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.domain.engine.GameEngine
import com.rvetiveau.rvscorecount.domain.engine.RoundValidation
import com.rvetiveau.rvscorecount.domain.model.ParticipantId
import com.rvetiveau.rvscorecount.domain.usecase.GetAvailableGamesUseCase
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
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private class FakeGameEngine(override val gameId: String) : GameEngine {
        override val minPlayers: Int = 3
        override val maxPlayers: Int = 8
        override val totalPointsPerRound: Int = 250
        override fun precomputeLastScore(entered: Map<ParticipantId, Int>): Int = 0
        override fun validateRound(scores: Map<ParticipantId, Int>): RoundValidation = RoundValidation.Success
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun papayooViewModel(): HomeViewModel =
        HomeViewModel(GetAvailableGamesUseCase(mapOf("papayoo" to FakeGameEngine("papayoo"))))

    @Test
    fun `exposes a Papayoo tile from the registered engine`() {
        val viewModel = papayooViewModel()

        val expected = GameTileUiModel(
            gameId = "papayoo",
            nameRes = R.string.game_name_papayoo,
            logoRes = R.drawable.ic_papayoo_logo
        )
        assertEquals(HomeUiState(tiles = listOf(expected)), viewModel.uiState.value)
    }

    @Test
    fun `emits NavigateTo Setup with the exact gameId on game selection`() = runTest {
        val viewModel = papayooViewModel()

        viewModel.onGameSelected("papayoo")

        assertEquals(NavigationEvent.NavigateTo(Routes.Setup("papayoo")), viewModel.navigationEvents.first())
    }

    @Test
    fun `emits NavigateTo History on history selection`() = runTest {
        val viewModel = papayooViewModel()

        viewModel.onHistorySelected()

        assertEquals(NavigationEvent.NavigateTo(Routes.History), viewModel.navigationEvents.first())
    }

    @Test
    fun `fails fast when a registered engine has no Home presentation`() {
        val useCase = GetAvailableGamesUseCase(mapOf("unknown-game" to FakeGameEngine("unknown-game")))

        assertThrows(IllegalStateException::class.java) { HomeViewModel(useCase) }
    }
}
