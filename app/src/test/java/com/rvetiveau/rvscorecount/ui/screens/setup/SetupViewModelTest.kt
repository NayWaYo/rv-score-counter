package com.rvetiveau.rvscorecount.ui.screens.setup

import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.domain.engine.GameEngine
import com.rvetiveau.rvscorecount.domain.engine.RoundValidation
import com.rvetiveau.rvscorecount.domain.model.ParticipantId
import com.rvetiveau.rvscorecount.domain.usecase.GetAvailableGamesUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SetupViewModelTest {

    private class FakeGameEngine(
        override val gameId: String,
        override val minPlayers: Int = 3,
        override val maxPlayers: Int = 8
    ) : GameEngine {
        override val totalPointsPerRound: Int = 250
        override fun precomputeLastScore(entered: Map<ParticipantId, Int>): Int = 0
        override fun validateRound(scores: Map<ParticipantId, Int>): RoundValidation = RoundValidation.Success
    }

    private fun viewModel(engine: GameEngine = FakeGameEngine("papayoo")): SetupViewModel =
        SetupViewModel(GetAvailableGamesUseCase(mapOf(engine.gameId to engine))).also { it.initialize(engine.gameId) }

    private fun SetupViewModel.addName(name: String) {
        onNameChanged(name)
        onAddParticipant()
    }

    @Test
    fun `default state before initialize disables start and add`() {
        val vm = SetupViewModel(GetAvailableGamesUseCase(mapOf("papayoo" to FakeGameEngine("papayoo"))))

        val state = vm.uiState.value
        assertFalse(state.isInitialized)
        assertFalse(state.canStart)
        assertFalse(state.canAdd)
    }

    @Test
    fun `initializes min and max players from the matching engine`() {
        val vm = viewModel(FakeGameEngine("papayoo", minPlayers = 3, maxPlayers = 8))

        val state = vm.uiState.value
        assertTrue(state.isInitialized)
        assertEquals(3, state.minPlayers)
        assertEquals(8, state.maxPlayers)
    }

    @Test
    fun `initialize is idempotent across repeated calls`() {
        val vm = SetupViewModel(GetAvailableGamesUseCase(mapOf("papayoo" to FakeGameEngine("papayoo"))))
        vm.initialize("papayoo")
        vm.addName("Alice")

        vm.initialize("papayoo")

        assertEquals(listOf("Alice"), vm.uiState.value.participants.map { it.name })
    }

    @Test
    fun `fails fast when no engine is registered for the requested gameId`() {
        val vm = SetupViewModel(GetAvailableGamesUseCase(mapOf("belote" to FakeGameEngine("belote"))))

        assertThrows(IllegalStateException::class.java) { vm.initialize("papayoo") }
    }

    @Test
    fun `adds a trimmed name and clears the input and message`() {
        val vm = viewModel()

        vm.addName("  Alice  ")

        val state = vm.uiState.value
        assertEquals(listOf("Alice"), state.participants.map { it.name })
        assertEquals("", state.nameInput)
        assertNull(state.messageRes)
    }

    @Test
    fun `rejects a blank name with an inline error and does not add`() {
        val vm = viewModel()

        vm.addName("   ")

        val state = vm.uiState.value
        assertTrue(state.participants.isEmpty())
        assertEquals(R.string.setup_error_empty_name, state.messageRes)
    }

    @Test
    fun `clears a stale message as soon as the name changes`() {
        val vm = viewModel()
        vm.addName("   ")

        vm.onNameChanged("Alice")

        assertNull(vm.uiState.value.messageRes)
    }

    @Test
    fun `accepts duplicate names`() {
        val vm = viewModel()

        vm.addName("Alice")
        vm.addName("Alice")

        assertEquals(listOf("Alice", "Alice"), vm.uiState.value.participants.map { it.name })
    }

    @Test
    fun `canStart toggles exactly at the engine minimum`() {
        val vm = viewModel(FakeGameEngine("papayoo", minPlayers = 2, maxPlayers = 8))

        vm.addName("Alice")
        assertFalse(vm.uiState.value.canStart)

        vm.addName("Bob")
        assertTrue(vm.uiState.value.canStart)
    }

    @Test
    fun `canAdd toggles exactly at the engine maximum and rejects the next add`() {
        val vm = viewModel(FakeGameEngine("papayoo", minPlayers = 1, maxPlayers = 2))

        vm.addName("Alice")
        vm.addName("Bob")
        assertFalse(vm.uiState.value.canAdd)
        assertTrue(vm.uiState.value.isMaxReached)

        vm.addName("Charlie")

        val state = vm.uiState.value
        assertEquals(listOf("Alice", "Bob"), state.participants.map { it.name })
        assertEquals(R.string.setup_max_players_reached, state.messageRes)
    }

    @Test
    fun `removes a participant and keeps the relative order of the rest`() {
        val vm = viewModel()
        listOf("Alice", "Bob", "Charlie").forEach { vm.addName(it) }
        val bobId = vm.uiState.value.participants[1].id

        vm.onRemoveParticipant(bobId)

        assertEquals(listOf("Alice", "Charlie"), vm.uiState.value.participants.map { it.name })
    }

    @Test
    fun `removing a participant clears a stale maximum message`() {
        val vm = viewModel(FakeGameEngine("papayoo", minPlayers = 1, maxPlayers = 2))
        vm.addName("Alice")
        vm.addName("Bob")
        vm.addName("Charlie")
        val bobId = vm.uiState.value.participants[1].id

        vm.onRemoveParticipant(bobId)

        assertTrue(vm.uiState.value.canAdd)
        assertNull(vm.uiState.value.messageRes)
    }

    @Test
    fun `removing the first participant moves the future dealer to the next one`() {
        val vm = viewModel()
        listOf("Alice", "Bob", "Charlie").forEach { vm.addName(it) }
        val aliceId = vm.uiState.value.participants[0].id

        vm.onRemoveParticipant(aliceId)

        assertEquals("Bob", vm.uiState.value.participants.first().name)
    }

    @Test
    fun `moves a participant and the future dealer follows index 0`() {
        val vm = viewModel()
        listOf("Alice", "Bob", "Charlie").forEach { vm.addName(it) }

        vm.onMoveParticipant(2, 0)

        val state = vm.uiState.value
        assertEquals(listOf("Charlie", "Alice", "Bob"), state.participants.map { it.name })
        assertEquals("Charlie", state.participants.first().name)
    }

    @Test
    fun `moves a participant downward and preserves the others' relative order`() {
        val vm = viewModel()
        listOf("Alice", "Bob", "Charlie", "Dora").forEach { vm.addName(it) }

        vm.onMoveParticipant(0, 2)

        assertEquals(listOf("Bob", "Charlie", "Alice", "Dora"), vm.uiState.value.participants.map { it.name })
    }

    @Test
    fun `ignores an out-of-range move`() {
        val vm = viewModel()
        listOf("Alice", "Bob", "Charlie").forEach { vm.addName(it) }

        vm.onMoveParticipant(0, 5)

        assertEquals(listOf("Alice", "Bob", "Charlie"), vm.uiState.value.participants.map { it.name })
    }

    @Test
    fun `ignores a no-op move to the same index`() {
        val vm = viewModel()
        listOf("Alice", "Bob").forEach { vm.addName(it) }

        vm.onMoveParticipant(1, 1)

        assertEquals(listOf("Alice", "Bob"), vm.uiState.value.participants.map { it.name })
    }
}
