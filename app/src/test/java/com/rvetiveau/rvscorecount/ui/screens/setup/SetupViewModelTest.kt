package com.rvetiveau.rvscorecount.ui.screens.setup

import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.domain.engine.GameEngine
import com.rvetiveau.rvscorecount.domain.engine.RoundValidation
import com.rvetiveau.rvscorecount.domain.model.Participant
import com.rvetiveau.rvscorecount.domain.model.ParticipantId
import com.rvetiveau.rvscorecount.domain.model.Party
import com.rvetiveau.rvscorecount.domain.model.Round
import com.rvetiveau.rvscorecount.domain.model.RoundScore
import com.rvetiveau.rvscorecount.domain.repository.PartyRepository
import com.rvetiveau.rvscorecount.domain.usecase.CreatePartyUseCase
import com.rvetiveau.rvscorecount.domain.usecase.GetAvailableGamesUseCase
import com.rvetiveau.rvscorecount.ui.navigation.NavigationEvent
import com.rvetiveau.rvscorecount.ui.navigation.Routes
import java.util.UUID
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SetupViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeGameEngine(
        override val gameId: String,
        override val minPlayers: Int = 3,
        override val maxPlayers: Int = 8
    ) : GameEngine {
        override val totalPointsPerRound: Int = 250
        override fun precomputeLastScore(entered: Map<ParticipantId, Int>): Int = 0
        override fun validateRound(scores: Map<ParticipantId, Int>): RoundValidation = RoundValidation.Success
    }

    private open class FakePartyRepository(
        private val failure: Throwable? = null
    ) : PartyRepository {
        var createCallCount = 0
            private set

        override suspend fun createParty(party: Party) = error("not used in this test")
        override suspend fun updateParty(party: Party) = error("not used in this test")
        override suspend fun createPartyWithParticipants(party: Party, participants: List<Participant>) {
            createCallCount++
            failure?.let { throw it }
        }
        override suspend fun getPartyById(id: UUID): Party? = error("not used in this test")
        override fun observeParty(id: UUID): Flow<Party?> = flowOf(null)
        override suspend fun getPartyInProgress(): Party? = error("not used in this test")
        override suspend fun addParticipant(participant: Participant) = error("not used in this test")
        override suspend fun getParticipantsByPartyId(partyId: UUID): List<Participant> = error("not used in this test")
        override fun observeParticipants(partyId: UUID): Flow<List<Participant>> = flowOf(emptyList())
        override suspend fun saveRound(round: Round, scores: Map<UUID, Int>, updatedParty: Party) = error("not used in this test")
        override suspend fun getRoundsByPartyId(partyId: UUID): List<Round> = error("not used in this test")
        override suspend fun getScoresByRoundId(roundId: UUID): List<RoundScore> = error("not used in this test")
    }

    /** Suspends every createPartyWithParticipants call until the test resumes it, to observe the busy window. */
    private class ControlledPartyRepository : FakePartyRepository() {
        private val pendingCreates = mutableListOf<Continuation<Unit>>()

        override suspend fun createPartyWithParticipants(party: Party, participants: List<Participant>) {
            super.createPartyWithParticipants(party, participants)
            suspendCoroutine<Unit> { continuation -> pendingCreates += continuation }
        }

        fun completeNext() {
            pendingCreates.removeAt(0).resume(Unit)
        }
    }

    private fun createPartyUseCase(engine: GameEngine, repository: PartyRepository = FakePartyRepository()): CreatePartyUseCase =
        CreatePartyUseCase(repository, mapOf(engine.gameId to engine))

    private fun viewModel(
        engine: GameEngine = FakeGameEngine("papayoo"),
        partyRepository: PartyRepository = FakePartyRepository()
    ): SetupViewModel =
        SetupViewModel(
            GetAvailableGamesUseCase(mapOf(engine.gameId to engine)),
            createPartyUseCase(engine, partyRepository)
        ).also { it.initialize(engine.gameId) }

    private fun SetupViewModel.addName(name: String) {
        onNameChanged(name)
        onAddParticipant()
    }

    @Test
    fun `default state before initialize disables start and add`() {
        val engine = FakeGameEngine("papayoo")
        val vm = SetupViewModel(GetAvailableGamesUseCase(mapOf("papayoo" to engine)), createPartyUseCase(engine))

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
        val engine = FakeGameEngine("papayoo")
        val vm = SetupViewModel(GetAvailableGamesUseCase(mapOf("papayoo" to engine)), createPartyUseCase(engine))
        vm.initialize("papayoo")
        vm.addName("Alice")

        vm.initialize("papayoo")

        assertEquals(listOf("Alice"), vm.uiState.value.participants.map { it.name })
    }

    @Test
    fun `fails fast when no engine is registered for the requested gameId`() {
        val engine = FakeGameEngine("belote")
        val vm = SetupViewModel(GetAvailableGamesUseCase(mapOf("belote" to engine)), createPartyUseCase(engine))

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

    @Test
    fun `confirms successfully and navigates to the exact created Party route`() = runTest {
        val repository = FakePartyRepository()
        val engine = FakeGameEngine("papayoo", minPlayers = 1, maxPlayers = 8)
        val vm = viewModel(engine, repository)
        vm.addName("Alice")

        vm.onStartParty()

        val event = vm.navigationEvents.first()
        check(event is NavigationEvent.NavigateTo)
        val route = event.route
        check(route is Routes.Party)
        assertEquals(1, repository.createCallCount)
        // A well-formed UUID string proves the emitted route carries the created partyId, not a placeholder.
        UUID.fromString(route.partyId)
        assertFalse(vm.uiState.value.isCreatingParty)
        assertFalse(vm.uiState.value.canStart)

        vm.onStartParty()

        assertEquals(1, repository.createCallCount)
    }

    @Test
    fun `keeps the ordered draft and surfaces an inline error when creation fails`() = runTest {
        val failure = IllegalStateException("Room write failed")
        val repository = FakePartyRepository(failure = failure)
        val engine = FakeGameEngine("papayoo", minPlayers = 1, maxPlayers = 8)
        val vm = viewModel(engine, repository)
        vm.addName("Alice")
        vm.addName("Bob")

        vm.onStartParty()

        val state = vm.uiState.value
        assertEquals(listOf("Alice", "Bob"), state.participants.map { it.name })
        assertEquals(R.string.setup_error_creation_failed, state.creationErrorRes)
        assertFalse(state.isCreatingParty)
    }

    @Test
    fun `clears the creation error when the draft is corrected`() = runTest {
        val repository = FakePartyRepository(failure = IllegalStateException("Room write failed"))
        val engine = FakeGameEngine("papayoo", minPlayers = 1, maxPlayers = 8)
        val vm = viewModel(engine, repository)
        vm.addName("Alice")
        vm.onStartParty()
        assertEquals(R.string.setup_error_creation_failed, vm.uiState.value.creationErrorRes)

        vm.onNameChanged("Bob")

        assertNull(vm.uiState.value.creationErrorRes)
    }

    @Test
    fun `does nothing when called below the engine minimum`() = runTest {
        val repository = FakePartyRepository()
        val engine = FakeGameEngine("papayoo", minPlayers = 2, maxPlayers = 8)
        val vm = viewModel(engine, repository)
        vm.addName("Alice")

        vm.onStartParty()

        assertEquals(0, repository.createCallCount)
        assertFalse(vm.uiState.value.isCreatingParty)
    }

    @Test
    fun `disables start immediately after the tap while creation is in flight`() = runTest {
        val repository = ControlledPartyRepository()
        val engine = FakeGameEngine("papayoo", minPlayers = 1, maxPlayers = 8)
        val vm = viewModel(engine, repository)
        vm.addName("Alice")

        vm.onStartParty()

        assertTrue(vm.uiState.value.isCreatingParty)
        assertFalse(vm.uiState.value.canStart)

        repository.completeNext()
    }

    @Test
    fun `a rapid second tap creates only one Party and emits only one navigation event`() = runTest {
        val repository = ControlledPartyRepository()
        val engine = FakeGameEngine("papayoo", minPlayers = 1, maxPlayers = 8)
        val vm = viewModel(engine, repository)
        vm.addName("Alice")

        vm.onStartParty()
        vm.onStartParty()

        assertEquals(1, repository.createCallCount)
        repository.completeNext()

        val event = vm.navigationEvents.first()
        assertTrue(event is NavigationEvent.NavigateTo)
    }
}
