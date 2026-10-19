package com.rvetiveau.rvscorecount.ui.screens.party

import com.rvetiveau.rvscorecount.domain.model.CardSuit
import com.rvetiveau.rvscorecount.domain.model.Participant
import com.rvetiveau.rvscorecount.domain.model.Party
import com.rvetiveau.rvscorecount.domain.model.PartyState
import com.rvetiveau.rvscorecount.domain.model.Round
import com.rvetiveau.rvscorecount.domain.model.RoundScore
import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.domain.engine.GameEngine
import com.rvetiveau.rvscorecount.domain.repository.PartyRepository
import com.rvetiveau.rvscorecount.domain.usecase.AddParticipantUseCase
import com.rvetiveau.rvscorecount.domain.usecase.ClosePartyUseCase
import com.rvetiveau.rvscorecount.domain.usecase.GetAvailableGamesUseCase
import com.rvetiveau.rvscorecount.game.papayoo.PapayooEngine
import com.rvetiveau.rvscorecount.ui.components.RoundTable
import com.rvetiveau.rvscorecount.ui.components.RoundTablePlayer
import com.rvetiveau.rvscorecount.ui.components.RoundTableRow
import com.rvetiveau.rvscorecount.ui.components.StandingRow
import com.rvetiveau.rvscorecount.ui.navigation.NavigationEvent
import com.rvetiveau.rvscorecount.ui.navigation.Routes
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PartyViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** Mimics Room: [observeParty] re-emits whenever [saveRound] rewrites the party row. */
    private class FakePartyRepository(
        party: Party? = null,
        participants: List<Participant> = emptyList(),
        var failure: Throwable? = null,
        val rounds: MutableList<Round> = mutableListOf(),
        val roundScores: MutableList<RoundScore> = mutableListOf()
    ) : PartyRepository {
        // SharedFlow, not StateFlow: Room re-emits on every write, even when the row is unchanged.
        private val partyFlow = MutableSharedFlow<Party?>(replay = 1).apply { tryEmit(party) }
        val participantsFlow = MutableStateFlow(participants)

        override suspend fun createParty(party: Party) = error("not used in this test")
        var updateFailure: Throwable? = null

        override suspend fun updateParty(party: Party) {
            updateFailure?.let { throw it }
            partyFlow.emit(party)
        }
        override suspend fun createPartyWithParticipants(party: Party, participants: List<Participant>) = error("not used in this test")
        var addFailure: Throwable? = null

        override suspend fun getPartyById(id: UUID): Party? = partyFlow.replayCache.last()
        override fun observeParty(id: UUID): Flow<Party?> = flow {
            failure?.let { throw it }
            emitAll(partyFlow)
        }
        override suspend fun getPartyInProgress(): Party? = error("not used in this test")
        override fun observeFinishedParties(): Flow<List<Party>> = error("not used in this test")
        override suspend fun deleteParty(id: UUID) = error("not used in this test")
        override suspend fun addParticipant(participant: Participant, updatedParty: Party) {
            addFailure?.let { throw it }
            participantsFlow.value = participantsFlow.value.map {
                if (it.listPosition >= participant.listPosition) it.copy(listPosition = it.listPosition + 1) else it
            } + participant
            partyFlow.emit(updatedParty)
        }
        override suspend fun getParticipantsByPartyId(partyId: UUID): List<Participant> = participantsFlow.value
        override fun observeParticipants(partyId: UUID): Flow<List<Participant>> = participantsFlow
        override suspend fun saveRound(round: Round, scores: Map<UUID, Int>, updatedParty: Party) {
            rounds += round
            roundScores += scores.map { (id, points) -> RoundScore(round.id, id, points) }
            partyFlow.emit(updatedParty)
        }
        override suspend fun getRoundsByPartyId(partyId: UUID): List<Round> = rounds.toList()
        override suspend fun getScoresByRoundId(roundId: UUID): List<RoundScore> =
            roundScores.filter { it.roundId == roundId }
    }

    private fun viewModel(repository: PartyRepository): PartyViewModel {
        val engines = mapOf<String, GameEngine>(PapayooEngine.GAME_ID to PapayooEngine())
        return PartyViewModel(
            repository,
            GetAvailableGamesUseCase(engines),
            AddParticipantUseCase(repository, engines),
            ClosePartyUseCase(repository)
        )
    }

    private fun aParty(
        id: UUID,
        dealerIndex: Int = 0,
        status: PartyState = PartyState.IN_PROGRESS
    ) = Party(
        id = id,
        gameId = "papayoo",
        status = status,
        currentDealerIndex = dealerIndex,
        createdAt = 0L
    )

    private fun aParticipant(partyId: UUID, name: String, position: Int) = Participant(
        id = UUID.randomUUID(),
        partyId = partyId,
        name = name,
        listPosition = position
    )

    @Test
    fun `loads the dealer at listPosition 0 for a freshly created party`() = runTest {
        val partyId = UUID.randomUUID()
        val participants = listOf(aParticipant(partyId, "Alice", 0), aParticipant(partyId, "Bob", 1))
        val vm = viewModel(FakePartyRepository(party = aParty(partyId), participants = participants))

        vm.initialize(partyId)

        val state = vm.uiState.value
        check(state is PartyUiState.Loaded)
        assertEquals("Alice", state.dealerName)
    }

    @Test
    fun `derives the dealer from currentDealerIndex, not just the first participant`() = runTest {
        val partyId = UUID.randomUUID()
        val participants = listOf(aParticipant(partyId, "Alice", 0), aParticipant(partyId, "Bob", 1))
        val vm = viewModel(FakePartyRepository(party = aParty(partyId, dealerIndex = 1), participants = participants))

        vm.initialize(partyId)

        val state = vm.uiState.value
        check(state is PartyUiState.Loaded)
        assertEquals("Bob", state.dealerName)
    }

    // --- Story 4.1 classement temps réel ---

    @Test
    fun `lists every participant at zero before the first round with the dealer flagged`() = runTest {
        val partyId = UUID.randomUUID()
        val alice = aParticipant(partyId, "Alice", 0)
        val bob = aParticipant(partyId, "Bob", 1)
        val vm = viewModel(FakePartyRepository(party = aParty(partyId), participants = listOf(alice, bob)))

        vm.initialize(partyId)

        val state = vm.uiState.value as PartyUiState.Loaded
        assertEquals(1, state.roundNumber)
        assertEquals(
            listOf(
                StandingRow(alice.id, rank = 1, name = "Alice", cumulativeScore = 0, isDealer = true),
                StandingRow(bob.id, rank = 1, name = "Bob", cumulativeScore = 0, isDealer = false)
            ),
            state.standings
        )
    }

    @Test
    fun `updates the classement, dealer and round number as soon as Room emits a validated round`() = runTest {
        val partyId = UUID.randomUUID()
        val alice = aParticipant(partyId, "Alice", 0)
        val bob = aParticipant(partyId, "Bob", 1)
        val charlie = aParticipant(partyId, "Charlie", 2)
        val repository = FakePartyRepository(party = aParty(partyId), participants = listOf(alice, bob, charlie))
        val vm = viewModel(repository)
        vm.initialize(partyId)

        repository.saveRound(
            Round(UUID.randomUUID(), partyId, 1, CardSuit.HEART),
            mapOf(alice.id to 150, bob.id to 20, charlie.id to 80),
            aParty(partyId, dealerIndex = 1)
        )

        val state = vm.uiState.value as PartyUiState.Loaded
        assertEquals("Bob", state.dealerName)
        assertEquals(2, state.roundNumber)
        assertEquals(listOf("Bob", "Charlie", "Alice"), state.standings.map { it.name })
        assertEquals(listOf(20, 80, 150), state.standings.map { it.cumulativeScore })
        assertEquals(listOf(1, 2, 3), state.standings.map { it.rank })
        assertEquals(listOf(true, false, false), state.standings.map { it.isDealer })
    }

    @Test
    fun `tied participants share a rank and the next one skips it`() = runTest {
        val partyId = UUID.randomUUID()
        val participants = List(3) { aParticipant(partyId, "P$it", it) }
        val repository = FakePartyRepository(party = aParty(partyId), participants = participants)
        val vm = viewModel(repository)
        vm.initialize(partyId)

        repository.saveRound(
            Round(UUID.randomUUID(), partyId, 1, CardSuit.CLUB),
            mapOf(participants[0].id to 50, participants[1].id to 50, participants[2].id to 150),
            aParty(partyId, dealerIndex = 1)
        )

        val state = vm.uiState.value as PartyUiState.Loaded
        assertEquals(listOf(1, 1, 3), state.standings.map { it.rank })
    }

    @Test
    fun `a participant added in Room appears in the classement at zero`() = runTest {
        val partyId = UUID.randomUUID()
        val alice = aParticipant(partyId, "Alice", 0)
        val repository = FakePartyRepository(party = aParty(partyId), participants = listOf(alice))
        val vm = viewModel(repository)
        vm.initialize(partyId)

        repository.participantsFlow.value = listOf(alice, aParticipant(partyId, "Bob", 1))

        val state = vm.uiState.value as PartyUiState.Loaded
        assertEquals(listOf("Alice", "Bob"), state.standings.map { it.name })
    }

    @Test
    fun `is InconsistentParticipants when a score belongs to an unknown participant`() = runTest {
        val partyId = UUID.randomUUID()
        val alice = aParticipant(partyId, "Alice", 0)
        val round = Round(UUID.randomUUID(), partyId, 1, CardSuit.SPADE)
        val repository = FakePartyRepository(
            party = aParty(partyId),
            participants = listOf(alice),
            rounds = mutableListOf(round),
            roundScores = mutableListOf(RoundScore(round.id, UUID.randomUUID(), 250))
        )
        val vm = viewModel(repository)

        vm.initialize(partyId)

        assertTrue(vm.uiState.value is PartyUiState.InconsistentParticipants)
    }

    @Test
    fun `is PartyNotFound when the party does not exist`() = runTest {
        val vm = viewModel(FakePartyRepository(party = null))

        vm.initialize(UUID.randomUUID())

        assertTrue(vm.uiState.value is PartyUiState.PartyNotFound)
    }

    @Test
    fun `is InconsistentParticipants when no participant matches the dealer index`() = runTest {
        val partyId = UUID.randomUUID()
        val participants = listOf(aParticipant(partyId, "Alice", 0))
        val vm = viewModel(FakePartyRepository(party = aParty(partyId, dealerIndex = 5), participants = participants))

        vm.initialize(partyId)

        assertTrue(vm.uiState.value is PartyUiState.InconsistentParticipants)
    }

    @Test
    fun `is InconsistentParticipants when the party has no participants`() = runTest {
        val partyId = UUID.randomUUID()
        val vm = viewModel(FakePartyRepository(party = aParty(partyId), participants = emptyList()))

        vm.initialize(partyId)

        assertTrue(vm.uiState.value is PartyUiState.InconsistentParticipants)
    }

    @Test
    fun `is InconsistentParticipants when list positions are duplicated`() = runTest {
        val partyId = UUID.randomUUID()
        val participants = listOf(aParticipant(partyId, "Alice", 0), aParticipant(partyId, "Bob", 0))
        val vm = viewModel(FakePartyRepository(party = aParty(partyId), participants = participants))

        vm.initialize(partyId)

        assertTrue(vm.uiState.value is PartyUiState.InconsistentParticipants)
    }

    @Test
    fun `is InconsistentParticipants when list positions are not continuous`() = runTest {
        val partyId = UUID.randomUUID()
        val participants = listOf(aParticipant(partyId, "Alice", 0), aParticipant(partyId, "Bob", 2))
        val vm = viewModel(FakePartyRepository(party = aParty(partyId), participants = participants))

        vm.initialize(partyId)

        assertTrue(vm.uiState.value is PartyUiState.InconsistentParticipants)
    }

    @Test
    fun `shows a load error and retries after a transient repository failure`() = runTest {
        val partyId = UUID.randomUUID()
        val repository = FakePartyRepository(
            party = aParty(partyId),
            participants = listOf(aParticipant(partyId, "Alice", 0)),
            failure = IllegalStateException("Room read failed")
        )
        val vm = viewModel(repository)

        vm.initialize(partyId)

        assertTrue(vm.uiState.value is PartyUiState.LoadError)
        repository.failure = null
        vm.retry(partyId)
        assertTrue(vm.uiState.value is PartyUiState.Loaded)
    }

    @Test
    fun `initialize is idempotent and keeps the first loaded state on a repeated call`() = runTest {
        val partyId = UUID.randomUUID()
        val participants = listOf(aParticipant(partyId, "Alice", 0))
        val vm = viewModel(FakePartyRepository(party = aParty(partyId), participants = participants))

        vm.initialize(partyId)
        vm.initialize(partyId)

        val state = vm.uiState.value
        check(state is PartyUiState.Loaded)
        assertEquals("Alice", state.dealerName)
    }

    @Test
    fun `onStartRound emits the RoundEntry route with the exact initialized partyId`() = runTest {
        val partyId = UUID.randomUUID()
        val participants = listOf(aParticipant(partyId, "Alice", 0))
        val vm = viewModel(FakePartyRepository(party = aParty(partyId), participants = participants))
        vm.initialize(partyId)

        vm.onStartRound()

        val event = vm.navigationEvents.first()
        assertEquals(NavigationEvent.NavigateTo(Routes.RoundEntry(partyId.toString())), event)
    }

    @Test
    fun `onStartRound does not navigate before the party has finished loading`() = runTest {
        val partyId = UUID.randomUUID()
        val vm = viewModel(FakePartyRepository(party = null))
        vm.initialize(partyId)

        vm.onStartRound()

        val event = withTimeoutOrNull(100) { vm.navigationEvents.first() }
        assertTrue(event == null)
    }

    @Test
    fun `onStartRound emits only once until the screen resumes`() = runTest {
        val partyId = UUID.randomUUID()
        val participants = listOf(aParticipant(partyId, "Alice", 0))
        val vm = viewModel(FakePartyRepository(party = aParty(partyId), participants = participants))
        vm.initialize(partyId)

        vm.onStartRound()
        vm.onStartRound()

        vm.navigationEvents.first()
        assertTrue(withTimeoutOrNull(100) { vm.navigationEvents.first() } == null)

        vm.onScreenResumed()
        vm.onStartRound()
        assertEquals(
            NavigationEvent.NavigateTo(Routes.RoundEntry(partyId.toString())),
            vm.navigationEvents.first()
        )
    }

    @Test
    fun `onStartRound does not navigate for a finished party`() = runTest {
        val partyId = UUID.randomUUID()
        val participants = listOf(aParticipant(partyId, "Alice", 0))
        val vm = viewModel(
            FakePartyRepository(
                party = aParty(partyId, status = PartyState.FINISHED),
                participants = participants
            )
        )
        vm.initialize(partyId)

        vm.onStartRound()

        val state = vm.uiState.value as PartyUiState.Loaded
        assertTrue(!state.canStartRound)
        assertTrue(withTimeoutOrNull(100) { vm.navigationEvents.first() } == null)
    }

    // --- Story 4.3 historique des Manches ---

    @Test
    fun `builds the score sheet in chronological order with players in table order`() = runTest {
        val partyId = UUID.randomUUID()
        val alice = aParticipant(partyId, "Alice", 0)
        val bob = aParticipant(partyId, "Bob", 1)
        val repository = FakePartyRepository(party = aParty(partyId), participants = listOf(alice, bob))
        val vm = viewModel(repository)
        vm.initialize(partyId)

        repository.saveRound(Round(UUID.randomUUID(), partyId, 1, CardSuit.HEART), mapOf(bob.id to 50, alice.id to 200), aParty(partyId, 1))
        repository.saveRound(Round(UUID.randomUUID(), partyId, 2, CardSuit.CLUB), mapOf(alice.id to 0, bob.id to 250), aParty(partyId, 0))

        assertEquals(
            RoundTable(
                players = listOf(RoundTablePlayer(alice.id, "Alice"), RoundTablePlayer(bob.id, "Bob")),
                rows = listOf(
                    RoundTableRow(1, CardSuit.HEART, mapOf(alice.id to 200, bob.id to 50)),
                    RoundTableRow(2, CardSuit.CLUB, mapOf(alice.id to 0, bob.id to 250))
                )
            ),
            loadedState(vm).roundTable
        )
    }

    @Test
    fun `a participant added later is absent from rounds played before`() = runTest {
        val partyId = UUID.randomUUID()
        val alice = aParticipant(partyId, "Alice", 0)
        val bob = aParticipant(partyId, "Bob", 1)
        val repository = FakePartyRepository(party = aParty(partyId), participants = listOf(alice, bob))
        val vm = viewModel(repository)
        vm.initialize(partyId)
        repository.saveRound(Round(UUID.randomUUID(), partyId, 1, CardSuit.SPADE), mapOf(alice.id to 100, bob.id to 150), aParty(partyId, 1))

        vm.onAddParticipantClick()
        vm.onAdditionNameChanged("Zoé")
        vm.onAdditionNameConfirmed()
        vm.onInsertionPositionSelected(1)

        val table = loadedState(vm).roundTable
        assertEquals(listOf("Alice", "Zoé", "Bob"), table.players.map { it.name })
        assertEquals(setOf(alice.id, bob.id), table.rows.single().points.keys)
        assertEquals(3, loadedState(vm).standings.size)
    }

    @Test
    fun `the round history opens and closes, and never opens without rounds`() = runTest {
        val partyId = UUID.randomUUID()
        val alice = aParticipant(partyId, "Alice", 0)
        val repository = FakePartyRepository(party = aParty(partyId), participants = listOf(alice))
        val vm = viewModel(repository)
        vm.initialize(partyId)

        vm.onShowRoundHistory()
        assertTrue(!loadedState(vm).isRoundHistoryOpen)

        repository.saveRound(Round(UUID.randomUUID(), partyId, 1, CardSuit.SPADE), mapOf(alice.id to 250), aParty(partyId))
        assertTrue(!loadedState(vm).isRoundHistoryOpen)
        vm.onShowRoundHistory()
        assertTrue(loadedState(vm).isRoundHistoryOpen)
        vm.onHideRoundHistory()
        assertTrue(!loadedState(vm).isRoundHistoryOpen)
    }

    // --- Story 4.2 ajout de Participant en cours de Partie ---

    private fun loadedState(vm: PartyViewModel) = vm.uiState.value as PartyUiState.Loaded

    @Test
    fun `offers one seat after each participant and flags the one making the newcomer dealer`() = runTest {
        val partyId = UUID.randomUUID()
        val participants = listOf(aParticipant(partyId, "Alice", 0), aParticipant(partyId, "Bob", 1), aParticipant(partyId, "Chloé", 2))
        val vm = viewModel(FakePartyRepository(party = aParty(partyId, dealerIndex = 1), participants = participants))
        vm.initialize(partyId)

        vm.onAddParticipantClick()
        vm.onAdditionNameChanged("Zoé")
        vm.onAdditionNameConfirmed()

        assertEquals(
            listOf(
                InsertionOption(position = 1, afterName = "Alice", becomesDealer = true),
                InsertionOption(position = 2, afterName = "Bob", becomesDealer = false),
                InsertionOption(position = 3, afterName = "Chloé", becomesDealer = false)
            ),
            loadedState(vm).addition?.insertionOptions
        )
    }

    @Test
    fun `selecting a seat inserts the newcomer at zero, updates the dealer and closes the draft`() = runTest {
        val partyId = UUID.randomUUID()
        val participants = listOf(aParticipant(partyId, "Alice", 0), aParticipant(partyId, "Bob", 1), aParticipant(partyId, "Chloé", 2))
        val vm = viewModel(FakePartyRepository(party = aParty(partyId, dealerIndex = 1), participants = participants))
        vm.initialize(partyId)
        vm.onAddParticipantClick()
        vm.onAdditionNameChanged("  Zoé ")
        vm.onAdditionNameConfirmed()

        // Between Alice (last dealer) and Bob (next dealer): Zoé deals next, Bob right after.
        vm.onInsertionPositionSelected(1)

        val state = loadedState(vm)
        assertNull(state.addition)
        assertEquals("Zoé", state.dealerName)
        val zoe = state.standings.single { it.name == "Zoé" }
        assertEquals(0, zoe.cumulativeScore)
        assertTrue(zoe.isDealer)
        assertEquals(4, state.standings.size)
    }

    @Test
    fun `a blank name shows an error without opening the seat picker`() = runTest {
        val partyId = UUID.randomUUID()
        val vm = viewModel(FakePartyRepository(party = aParty(partyId), participants = listOf(aParticipant(partyId, "Alice", 0))))
        vm.initialize(partyId)
        vm.onAddParticipantClick()
        vm.onAdditionNameChanged("   ")

        vm.onAdditionNameConfirmed()

        val addition = loadedState(vm).addition!!
        assertEquals(R.string.party_add_error_empty_name, addition.errorRes)
        assertTrue(!addition.isChoosingPosition)
    }

    @Test
    fun `a failed insertion keeps the seat picker open with an error`() = runTest {
        val partyId = UUID.randomUUID()
        val repository = FakePartyRepository(party = aParty(partyId), participants = listOf(aParticipant(partyId, "Alice", 0)))
        repository.addFailure = IllegalStateException("Room write failed")
        val vm = viewModel(repository)
        vm.initialize(partyId)
        vm.onAddParticipantClick()
        vm.onAdditionNameChanged("Zoé")
        vm.onAdditionNameConfirmed()

        vm.onInsertionPositionSelected(1)

        val addition = loadedState(vm).addition!!
        assertEquals(R.string.party_add_error_failed, addition.errorRes)
        assertTrue(addition.isChoosingPosition)
        assertTrue(!addition.isSaving)
        assertEquals(1, loadedState(vm).standings.size)
    }

    @Test
    fun `cancelling drops the draft without writing`() = runTest {
        val partyId = UUID.randomUUID()
        val vm = viewModel(FakePartyRepository(party = aParty(partyId), participants = listOf(aParticipant(partyId, "Alice", 0))))
        vm.initialize(partyId)
        vm.onAddParticipantClick()
        vm.onAdditionNameChanged("Zoé")
        vm.onAdditionNameConfirmed()

        vm.onAdditionCancelled()

        assertNull(loadedState(vm).addition)
        assertEquals(1, loadedState(vm).standings.size)
    }

    @Test
    fun `adding is impossible at the engine maximum`() = runTest {
        val partyId = UUID.randomUUID()
        val participants = List(8) { aParticipant(partyId, "P$it", it) }
        val vm = viewModel(FakePartyRepository(party = aParty(partyId), participants = participants))
        vm.initialize(partyId)

        vm.onAddParticipantClick()

        val state = loadedState(vm)
        assertTrue(!state.canAddParticipant)
        assertTrue(state.isMaxPlayersReached)
        assertNull(state.addition)
    }

    // --- Story 5.1 clôture ---

    @Test
    fun `confirming the end of party finishes it and replaces the stack with the results`() = runTest {
        val partyId = UUID.randomUUID()
        val vm = viewModel(FakePartyRepository(party = aParty(partyId), participants = listOf(aParticipant(partyId, "Alice", 0))))
        vm.initialize(partyId)

        vm.onEndPartyClick()
        assertTrue(loadedState(vm).closure.isConfirmationOpen)
        vm.onEndPartyConfirmed()

        assertEquals(NavigationEvent.ReplaceAll(Routes.Results(partyId.toString())), vm.navigationEvents.first())
        val state = loadedState(vm)
        assertTrue(!state.canStartRound)
        assertTrue(!state.canAddParticipant)
    }

    @Test
    fun `cancelling the confirmation keeps the party in progress`() = runTest {
        val partyId = UUID.randomUUID()
        val vm = viewModel(FakePartyRepository(party = aParty(partyId), participants = listOf(aParticipant(partyId, "Alice", 0))))
        vm.initialize(partyId)

        vm.onEndPartyClick()
        vm.onEndPartyDismissed()
        vm.onEndPartyConfirmed()

        assertEquals(PartyClosureUiState(), loadedState(vm).closure)
        assertTrue(loadedState(vm).canStartRound)
        assertNull(withTimeoutOrNull(100) { vm.navigationEvents.first() })
    }

    @Test
    fun `a failed closure shows an error and stays on the party`() = runTest {
        val partyId = UUID.randomUUID()
        val repository = FakePartyRepository(party = aParty(partyId), participants = listOf(aParticipant(partyId, "Alice", 0)))
        repository.updateFailure = IllegalStateException("Room write failed")
        val vm = viewModel(repository)
        vm.initialize(partyId)

        vm.onEndPartyClick()
        vm.onEndPartyConfirmed()

        val closure = loadedState(vm).closure
        assertEquals(R.string.party_end_error, closure.errorRes)
        assertTrue(!closure.isConfirmationOpen)
        assertTrue(loadedState(vm).canStartRound)
        assertNull(withTimeoutOrNull(100) { vm.navigationEvents.first() })
    }
}
