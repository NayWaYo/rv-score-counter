package com.rvetiveau.rvscorecount.ui.screens.home

import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.domain.engine.Game
import com.rvetiveau.rvscorecount.domain.usecase.GetGameCatalogUseCase
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

    private class FakeGame(override val gameId: String) : Game

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun papayooViewModel(): HomeViewModel =
        HomeViewModel(GetGameCatalogUseCase(mapOf("papayoo" to FakeGame("papayoo"))))

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
    fun `emits NavigateTo Players on players selection`() = runTest {
        val viewModel = papayooViewModel()

        viewModel.onPlayersSelected()

        assertEquals(NavigationEvent.NavigateTo(Routes.Players), viewModel.navigationEvents.first())
    }

    @Test
    fun `emits NavigateTo Backup on backup selection`() = runTest {
        val viewModel = papayooViewModel()

        viewModel.onBackupSelected()

        assertEquals(NavigationEvent.NavigateTo(Routes.Backup), viewModel.navigationEvents.first())
    }

    @Test
    fun `shows the Papayoo first, then the Belote`() {
        val viewModel = HomeViewModel(
            GetGameCatalogUseCase(mapOf("belote" to FakeGame("belote"), "papayoo" to FakeGame("papayoo")))
        )

        assertEquals(listOf("papayoo", "belote"), viewModel.uiState.value.tiles.map { it.gameId })
        assertEquals(R.drawable.ic_belote_logo, viewModel.uiState.value.tiles.last().logoRes)
        assertEquals(R.string.game_name_belote, viewModel.uiState.value.tiles.last().nameRes)
    }

    @Test
    fun `emits NavigateTo BeloteSetup on Belote selection`() = runTest {
        val viewModel = HomeViewModel(GetGameCatalogUseCase(mapOf("belote" to FakeGame("belote"))))

        viewModel.onGameSelected("belote")

        assertEquals(NavigationEvent.NavigateTo(Routes.BeloteSetup), viewModel.navigationEvents.first())
    }

    @Test
    fun `the Jeu libre comes last and opens its own creation`() = runTest {
        val viewModel = HomeViewModel(
            GetGameCatalogUseCase(mapOf("free" to FakeGame("free"), "papayoo" to FakeGame("papayoo"), "belote" to FakeGame("belote")))
        )

        assertEquals(R.string.game_name_free, viewModel.uiState.value.tiles.last().nameRes)
        viewModel.onGameSelected("free")
        assertEquals(NavigationEvent.NavigateTo(Routes.FreeSetup), viewModel.navigationEvents.first())
    }

    @Test
    fun `fails fast when a registered engine has no Home presentation`() {
        val useCase = GetGameCatalogUseCase(mapOf("unknown-game" to FakeGame("unknown-game")))

        assertThrows(IllegalStateException::class.java) { HomeViewModel(useCase) }
    }
}
