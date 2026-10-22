package com.rvetiveau.rvscorecount.ui.screens.profile

import com.rvetiveau.rvscorecount.domain.model.PlayerSummary
import com.rvetiveau.rvscorecount.domain.repository.PlayerRepository
import com.rvetiveau.rvscorecount.domain.repository.PlayerSeat
import com.rvetiveau.rvscorecount.domain.usecase.FakeBeloteStore
import com.rvetiveau.rvscorecount.domain.usecase.FakePlayerRepository
import com.rvetiveau.rvscorecount.domain.usecase.GetPlayerRecordsUseCase
import com.rvetiveau.rvscorecount.game.belote.BeloteEngine
import com.rvetiveau.rvscorecount.game.papayoo.PapayooEngine
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
    private val store = FakeBeloteStore()
    private val players = FakePlayerRepository(players = listOf(alice))

    private fun viewModel() = PlayerProfileViewModel(
        GetPlayerRecordsUseCase(players, store, store, BeloteEngine(), mapOf(PapayooEngine.GAME_ID to PapayooEngine()))
    )

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
        val vm = PlayerProfileViewModel(
            GetPlayerRecordsUseCase(failing, store, store, BeloteEngine(), mapOf(PapayooEngine.GAME_ID to PapayooEngine()))
        )

        vm.initialize(alice.id)

        assertTrue(vm.uiState.value is PlayerProfileUiState.LoadError)
    }
}
