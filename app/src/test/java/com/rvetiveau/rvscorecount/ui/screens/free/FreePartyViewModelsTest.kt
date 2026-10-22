package com.rvetiveau.rvscorecount.ui.screens.free

import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.domain.model.EndRule
import com.rvetiveau.rvscorecount.domain.model.PartyState
import com.rvetiveau.rvscorecount.domain.model.WinDirection
import com.rvetiveau.rvscorecount.domain.usecase.ClosePartyUseCase
import com.rvetiveau.rvscorecount.domain.usecase.CorrectFreeRoundUseCase
import com.rvetiveau.rvscorecount.domain.usecase.FakeFreeStore
import com.rvetiveau.rvscorecount.domain.usecase.ObserveFreePartyUseCase
import com.rvetiveau.rvscorecount.domain.usecase.PlayFreeRoundUseCase
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

/** The Partie of Jeu libre and the saisie of its Manches (Story 11.3). */
@OptIn(ExperimentalCoroutinesApi::class)
class FreePartyViewModelsTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val rules = FreeGameEngine()

    private fun partyViewModel(store: FakeFreeStore) = FreePartyViewModel(
        ObserveFreePartyUseCase(store, store.free, rules),
        ClosePartyUseCase(store)
    ).also { it.initialize(store.partyId) }

    private fun roundViewModel(store: FakeFreeStore, correctedRoundNumber: Int? = null) = FreeRoundViewModel(
        ObserveFreePartyUseCase(store, store.free, rules),
        PlayFreeRoundUseCase(store, store.free, rules),
        CorrectFreeRoundUseCase(store, store.free, rules),
        rules
    ).also { it.initialize(store.partyId, correctedRoundNumber) }

    private fun FreePartyViewModel.loaded() = uiState.value as FreePartyUiState.Loaded
    private fun FreeRoundViewModel.loaded() = uiState.value as FreeRoundUiState.Loaded

    @Test
    fun `the partie shows its classement, the dealer, its manches and how far the target is`() {
        val store = FakeFreeStore(endRule = EndRule.TargetScore(100))
        store.addRound(alicePoints = 40, bobPoints = -5, chloePoints = 40)
        store.party.value = store.party.value?.copy(currentDealerIndex = 1)

        val state = partyViewModel(store).loaded()

        assertEquals("Skyjo", state.gameName)
        assertFalse(state.isFinished)
        assertEquals(listOf("Bob" to 1, "Alice" to 2, "Chloé" to 2), state.standings.map { it.name to it.rank })
        assertEquals(listOf("Bob"), state.standings.filter { it.isDealer }.map { it.name })
        assertEquals(FreeProgress.Target(100, 60, listOf("Alice", "Chloé")), state.progress)
        assertEquals(2, state.nextRoundNumber)
        assertEquals(store.scores(40, -5, 40), state.roundTable.rows.single().points)
        assertEquals(-5, state.roundTable.rows.single().bestPoints)
    }

    @Test
    fun `a round count shows the current manche and an end by hand shows nothing`() {
        val counted = FakeFreeStore(endRule = EndRule.RoundCount(10), winDirection = WinDirection.HIGHEST_WINS)
        counted.addRound(1, 2, 3)

        val countedState = partyViewModel(counted).loaded()

        assertEquals(FreeProgress.Rounds(2, 10), countedState.progress)
        assertEquals(3, countedState.roundTable.rows.single().bestPoints)
        assertNull(partyViewModel(FakeFreeStore(endRule = null)).loaded().progress)
    }

    @Test
    fun `play round and a manche touched open the saisie, while in progress only`() = runTest {
        val store = FakeFreeStore()
        store.addRound(1, 2, 3)
        val viewModel = partyViewModel(store)

        viewModel.onPlayRound()
        assertEquals(NavigationEvent.NavigateTo(Routes.FreeRound(store.partyId.toString())), viewModel.navigationEvents.first())
        viewModel.onRoundSelected(1)
        assertEquals(NavigationEvent.NavigateTo(Routes.FreeRound(store.partyId.toString(), 1)), viewModel.navigationEvents.first())

        store.party.value = store.party.value?.copy(status = PartyState.FINISHED)
        viewModel.onPlayRound()
        viewModel.onRoundSelected(1)
        viewModel.onBackHome()
        assertEquals(NavigationEvent.ReplaceAll(Routes.Home), viewModel.navigationEvents.first())
    }

    @Test
    fun `fin de partie asks first, then finishes the partie with its winners`() {
        val store = FakeFreeStore(endRule = null)
        store.addRound(alicePoints = 3, bobPoints = 8, chloePoints = 3)
        val viewModel = partyViewModel(store)

        viewModel.onEndPartyClick()
        assertTrue(viewModel.loaded().closure.isConfirmationOpen)
        viewModel.onEndPartyDismissed()
        assertFalse(viewModel.loaded().closure.isConfirmationOpen)
        assertEquals(PartyState.IN_PROGRESS, store.party.value?.status)

        viewModel.onEndPartyClick()
        viewModel.onEndPartyConfirmed()

        val state = viewModel.loaded()
        assertEquals(PartyState.FINISHED, store.party.value?.status)
        assertTrue(state.isFinished)
        assertFalse(state.closure.isConfirmationOpen)
        assertEquals(listOf("Alice", "Chloé"), state.winnerNames)
        assertTrue(state.standings.none { it.isDealer })
        assertNull(state.progress)
    }

    @Test
    fun `a failed closure keeps the partie in progress and says so`() {
        val store = FakeFreeStore()
        val viewModel = partyViewModel(store)
        store.failWrites = true

        viewModel.onEndPartyClick()
        viewModel.onEndPartyConfirmed()

        val state = viewModel.loaded()
        assertFalse(state.isFinished)
        assertFalse(state.closure.isConfirmationOpen)
        assertEquals(R.string.party_end_error, state.closure.errorRes)
    }

    @Test
    fun `a partie finished without a manche has no winner`() {
        val store = FakeFreeStore()
        store.party.value = store.party.value?.copy(status = PartyState.FINISHED)

        assertTrue(partyViewModel(store).loaded().winnerNames.isEmpty())
    }

    @Test
    fun `a missing partie is not found`() {
        val store = FakeFreeStore()
        store.party.value = null

        assertEquals(FreePartyUiState.NotFound, partyViewModel(store).uiState.value)
    }

    @Test
    fun `the saisie has one empty field per player in table order and validates once all are filled`() {
        val viewModel = roundViewModel(FakeFreeStore())
        val state = viewModel.loaded()

        assertEquals(1, state.roundNumber)
        assertFalse(state.isCorrection)
        assertEquals(listOf("Alice", "Bob", "Chloé"), state.fields.map { it.name })
        assertFalse(state.canValidate)

        state.fields.forEach { viewModel.onScoreChanged(it.participantId, "7") }
        assertTrue(viewModel.loaded().canValidate)
    }

    @Test
    fun `the sign button and a typed minus make a score negative, letters are refused`() {
        val store = FakeFreeStore()
        val viewModel = roundViewModel(store)

        viewModel.onToggleSign(store.alice.id)
        viewModel.onScoreChanged(store.alice.id, "-12")
        viewModel.onScoreChanged(store.alice.id, "-12a")
        viewModel.onToggleSign(store.bob.id)
        viewModel.onScoreChanged(store.bob.id, "-4")
        viewModel.onToggleSign(store.bob.id)
        viewModel.onScoreChanged(store.chloe.id, "-")

        val fields = viewModel.loaded().fields.associate { it.participantId to it.text }
        assertEquals("-12", fields[store.alice.id])
        assertEquals("4", fields[store.bob.id])
        assertEquals("-", fields[store.chloe.id])
        assertFalse(viewModel.loaded().canValidate)
    }

    @Test
    fun `a score beyond the bounds is flagged and blocks the validation`() {
        val store = FakeFreeStore()
        val viewModel = roundViewModel(store)

        viewModel.onScoreChanged(store.alice.id, "100000")
        viewModel.onScoreChanged(store.alice.id, "1000000")
        viewModel.onScoreChanged(store.bob.id, "0")
        viewModel.onScoreChanged(store.chloe.id, "0")

        val alice = viewModel.loaded().fields.first { it.participantId == store.alice.id }
        assertEquals("100000", alice.text)
        assertTrue(alice.isOutOfBounds)
        assertFalse(viewModel.loaded().canValidate)
    }

    @Test
    fun `validating saves the manche and goes back to the partie`() = runTest {
        val store = FakeFreeStore()
        val viewModel = roundViewModel(store)
        viewModel.onScoreChanged(store.alice.id, "-3")
        viewModel.onScoreChanged(store.bob.id, "0")
        viewModel.onScoreChanged(store.chloe.id, "12")

        viewModel.onValidate()

        assertEquals(NavigationEvent.NavigateBack, viewModel.navigationEvents.first())
        assertEquals(store.scores(-3, 0, 12), store.free.rounds.value.single().scores)
    }

    @Test
    fun `a failed save keeps the scores and says so`() {
        val store = FakeFreeStore()
        val viewModel = roundViewModel(store)
        viewModel.loaded().fields.forEach { viewModel.onScoreChanged(it.participantId, "1") }
        store.failWrites = true

        viewModel.onValidate()

        val state = viewModel.loaded()
        assertEquals(R.string.belote_round_error_failed, state.errorRes)
        assertFalse(state.isSaving)
        assertTrue(state.fields.all { it.text == "1" })
    }

    @Test
    fun `a correction is prefilled and saves over the manche`() = runTest {
        val store = FakeFreeStore()
        store.addRound(alicePoints = -2, bobPoints = 5, chloePoints = 0)
        val viewModel = roundViewModel(store, correctedRoundNumber = 1)

        val state = viewModel.loaded()
        assertTrue(state.isCorrection)
        assertEquals(listOf("-2", "5", "0"), state.fields.map { it.text })

        viewModel.onScoreChanged(store.bob.id, "6")
        viewModel.onValidate()

        assertEquals(NavigationEvent.NavigateBack, viewModel.navigationEvents.first())
        assertEquals(store.scores(-2, 6, 0), store.free.rounds.value.single().scores)
    }

    @Test
    fun `the saisie is unavailable for a finished partie or a manche not played`() {
        val missingRound = FakeFreeStore()
        assertEquals(FreeRoundUiState.Unavailable, roundViewModel(missingRound, correctedRoundNumber = 1).uiState.value)

        val finished = FakeFreeStore()
        finished.party.value = finished.party.value?.copy(status = PartyState.FINISHED)
        assertEquals(FreeRoundUiState.Unavailable, roundViewModel(finished).uiState.value)
    }
}
