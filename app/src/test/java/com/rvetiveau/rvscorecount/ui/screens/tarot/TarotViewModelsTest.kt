package com.rvetiveau.rvscorecount.ui.screens.tarot

import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.domain.engine.GameIds
import com.rvetiveau.rvscorecount.domain.model.Chelem
import com.rvetiveau.rvscorecount.domain.model.EndRule
import com.rvetiveau.rvscorecount.domain.model.PartyState
import com.rvetiveau.rvscorecount.domain.model.Poignee
import com.rvetiveau.rvscorecount.domain.model.TarotContract
import com.rvetiveau.rvscorecount.domain.model.TarotDealing
import com.rvetiveau.rvscorecount.domain.model.TarotSide
import com.rvetiveau.rvscorecount.domain.usecase.ClosePartyUseCase
import com.rvetiveau.rvscorecount.domain.usecase.CorrectTarotRoundUseCase
import com.rvetiveau.rvscorecount.domain.usecase.CreateTarotPartyUseCase
import com.rvetiveau.rvscorecount.domain.usecase.FakePlayerRepository
import com.rvetiveau.rvscorecount.domain.usecase.FakeTarotStore
import com.rvetiveau.rvscorecount.domain.usecase.ObserveKnownPlayerNamesUseCase
import com.rvetiveau.rvscorecount.domain.usecase.ObserveTarotPartyUseCase
import com.rvetiveau.rvscorecount.domain.usecase.PlayTarotRoundUseCase
import com.rvetiveau.rvscorecount.game.tarot.TarotEngine
import com.rvetiveau.rvscorecount.ui.components.TakerUiModel
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

/** Creation, Partie and saisie of the Tarot (Story 13.3). */
@OptIn(ExperimentalCoroutinesApi::class)
class TarotViewModelsTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val rules = TarotEngine()

    private fun setupViewModel(store: FakeTarotStore = FakeTarotStore()) = TarotSetupViewModel(
        CreateTarotPartyUseCase(store, rules),
        rules,
        ObserveKnownPlayerNamesUseCase(FakePlayerRepository("Alice", "Bob"))
    )

    private fun partyViewModel(store: FakeTarotStore, isResults: Boolean = false) = TarotPartyViewModel(
        ObserveTarotPartyUseCase(store, store.tarot, rules),
        ClosePartyUseCase(store),
        CreateTarotPartyUseCase(store, rules)
    ).also { it.initialize(store.partyId, isResults) }

    private fun roundViewModel(store: FakeTarotStore, correctedRoundNumber: Int? = null) = TarotRoundViewModel(
        ObserveTarotPartyUseCase(store, store.tarot, rules),
        PlayTarotRoundUseCase(store, store.tarot, rules),
        CorrectTarotRoundUseCase(store, store.tarot, rules),
        rules
    ).also { it.initialize(store.partyId, correctedRoundNumber) }

    private fun TarotPartyViewModel.loaded() = uiState.value as TarotPartyUiState.Loaded
    private fun TarotRoundViewModel.loaded() = uiState.value as TarotRoundUiState.Loaded

    @Test
    fun `setup starts with 3 to 5 Joueurs and an end by hand, or a number of Manches stepped from 1 to 99`() = runTest {
        val store = FakeTarotStore()
        val vm = setupViewModel(store)
        assertFalse(vm.uiState.value.hasRoundLimit)
        listOf("Alice", "Bob").forEach(vm::onSuggestionSelected)
        assertFalse(vm.uiState.value.canStart)
        vm.onNameChanged("Chloé")
        vm.onAddParticipant()
        assertTrue(vm.uiState.value.canStart)

        vm.onRoundLimitChanged(true)
        assertEquals(EndRule.RoundCount(10), vm.uiState.value.endRule)
        repeat(20) { vm.onRoundCountStep(-1) }
        assertEquals(1, vm.uiState.value.roundCount)
        assertFalse(vm.uiState.value.canDecreaseRoundCount)
        repeat(200) { vm.onRoundCountStep(1) }
        assertEquals(99, vm.uiState.value.roundCount)
        assertFalse(vm.uiState.value.canIncreaseRoundCount)
        repeat(87) { vm.onRoundCountStep(-1) }
        assertTrue(vm.uiState.value.canStart)

        vm.onStartParty()
        val (party, seats) = store.created.single()
        assertEquals(GameIds.TAROT, party.gameId)
        assertEquals(EndRule.RoundCount(12), party.endRule)
        assertEquals(listOf("Alice", "Bob", "Chloé"), seats.map { it.name })
        assertEquals(NavigationEvent.ReplaceAll(Routes.TarotParty(party.id.toString())), vm.navigationEvents.first())
    }

    @Test
    fun `setup refuses a sixth Joueur and opens the Tarot history`() = runTest {
        val vm = setupViewModel()
        listOf("A", "B", "C", "D", "E").forEach { vm.onNameChanged(it); vm.onAddParticipant() }
        assertTrue(vm.uiState.value.isMaxReached)
        vm.onNameChanged("F")
        vm.onAddParticipant()
        assertEquals(5, vm.uiState.value.participants.size)
        assertEquals(R.string.setup_max_players_reached, vm.uiState.value.messageRes)

        vm.onHistorySelected()
        assertEquals(NavigationEvent.NavigateTo(Routes.GameHistory(GameIds.TAROT)), vm.navigationEvents.first())
    }

    @Test
    fun `the Partie shows its standings, dealer, dealing, progress and contracts`() = runTest {
        val store = FakeTarotStore(endRule = EndRule.RoundCount(3))
        store.addRound(store.input(points = 50))
        store.addRound(store.input(points = 30, taker = store.bob))
        store.party.value = store.party.value?.copy(currentDealerIndex = 2)

        val state = partyViewModel(store).loaded()

        assertFalse(state.isFinished)
        assertEquals(TarotProgress(3, 3), state.progress)
        assertEquals(TarotDealing(18, 6), state.dealing)
        assertEquals(4, state.playerCount)
        // Alice +102 then −(−36) = +138, Bob −34 − 108, Chloé and David −34 + 36.
        assertEquals(listOf("Alice" to 138, "Chloé" to 2, "David" to 2, "Bob" to -142), state.standings.map { it.name to it.cumulativeScore })
        assertEquals("Chloé", state.standings.single { it.isDealer }.name)
        assertEquals(
            listOf(
                TarotContractLine(1, "Alice", null, false, TarotContract.PRISE, made = true, margin = 9),
                TarotContractLine(2, "Bob", null, false, TarotContract.PRISE, made = false, margin = 11)
            ),
            state.contracts
        )
        assertEquals(listOf(1, 2), state.roundTable.rows.map { it.roundNumber })
        assertEquals(3, state.nextRoundNumber)
    }

    @Test
    fun `at 5 the contracts name the partner or a taker alone`() = runTest {
        val store = FakeTarotStore(endRule = null, playerCount = 5)
        store.addRound(store.input().copy(partnerId = store.chloe.id))
        store.addRound(store.input(taker = store.bob))

        val contracts = partyViewModel(store).loaded().contracts

        assertEquals("Chloé", contracts[0].partnerName)
        assertFalse(contracts[0].isAlone)
        assertNull(contracts[1].partnerName)
        assertTrue(contracts[1].isAlone)
    }

    @Test
    fun `rounds open from the Partie in progress, and ending it opens the Résultats`() = runTest {
        val store = FakeTarotStore()
        store.addRound()
        val vm = partyViewModel(store)

        vm.onPlayRound()
        assertEquals(NavigationEvent.NavigateTo(Routes.TarotRound(store.partyId.toString())), vm.navigationEvents.first())
        vm.onRoundSelected(1)
        assertEquals(NavigationEvent.NavigateTo(Routes.TarotRound(store.partyId.toString(), 1)), vm.navigationEvents.first())

        vm.onEndPartyClick()
        assertTrue(vm.loaded().closure.isConfirmationOpen)
        vm.onEndPartyConfirmed()
        assertEquals(NavigationEvent.ReplaceAll(Routes.TarotResults(store.partyId.toString())), vm.navigationEvents.first())
        assertEquals(PartyState.FINISHED, store.party.value?.status)
        assertEquals(listOf("Alice"), vm.loaded().winnerNames)
    }

    @Test
    fun `the Résultats replay the same Partie`() = runTest {
        val store = FakeTarotStore()
        store.addRound()
        store.party.value = store.party.value?.copy(status = PartyState.FINISHED)
        val vm = partyViewModel(store, isResults = true)

        vm.onReplay()

        val (party, _) = store.created.single()
        assertEquals(NavigationEvent.ReplaceAll(Routes.TarotParty(party.id.toString())), vm.navigationEvents.first())
    }

    @Test
    fun `the preview appears once the Manche is complete, from the engine`() = runTest {
        val store = FakeTarotStore()
        val vm = roundViewModel(store)
        assertEquals(1, vm.loaded().roundNumber)
        assertFalse(vm.loaded().callsPartner)

        vm.onTakerSelected(store.alice.id)
        vm.onContractSelected(TarotContract.GARDE)
        vm.onBoutsSelected(2)
        assertEquals(41, vm.loaded().requiredPoints)
        assertNull(vm.loaded().preview)
        vm.onPointsChanged("50")

        // Garde made by 9: (25 + 9) × 2 = 68 per defender, 204 for Alice.
        val preview = checkNotNull(vm.loaded().preview)
        assertTrue(preview.made)
        assertEquals(9, preview.margin)
        assertEquals(68, preview.perDefender)
        assertEquals(listOf("Alice" to 204, "Bob" to -68, "Chloé" to -68, "David" to -68), preview.points)
        assertTrue(vm.loaded().canValidate)

        vm.onPetitAuBoutSelected(TarotSide.ATTACK)
        vm.onAttackPoigneeSelected(Poignee.SIMPLE)
        assertEquals(108, vm.loaded().preview?.perDefender)
        vm.onPointsChanged("92")
        assertTrue(vm.loaded().isPointsOutOfRange)
        assertNull(vm.loaded().preview)
        vm.onPointsChanged("123")
        assertEquals("92", vm.loaded().pointsText)
    }

    @Test
    fun `at 5 the partner is required, and a taker who was the partner clears it`() = runTest {
        val store = FakeTarotStore(playerCount = 5)
        val vm = roundViewModel(store)
        assertTrue(vm.loaded().callsPartner)
        vm.onTakerSelected(store.alice.id)
        vm.onContractSelected(TarotContract.PRISE)
        vm.onBoutsSelected(2)
        vm.onPointsChanged("41")
        assertNull(vm.loaded().preview)

        vm.onPartnerSelected(TarotPartnerChoice.Player(store.bob.id))
        assertEquals(listOf(50, 25, -25, -25, -25), vm.loaded().preview?.points?.map { it.second })
        vm.onTakerSelected(store.bob.id)
        assertNull(vm.loaded().partner)
        vm.onPartnerSelected(TarotPartnerChoice.Alone)
        assertEquals(100, vm.loaded().preview?.points?.get(1)?.second)
    }

    @Test
    fun `validating saves the Manche and goes back, or to the Résultats after the last one`() = runTest {
        val store = FakeTarotStore(endRule = EndRule.RoundCount(2))
        val first = roundViewModel(store)
        first.fill(store)
        first.onValidate()
        assertEquals(NavigationEvent.NavigateBack, first.navigationEvents.first())
        assertEquals(1, store.tarot.rounds.value.size)

        val last = roundViewModel(store)
        assertEquals(2, last.loaded().roundNumber)
        last.fill(store)
        last.onValidate()
        assertEquals(NavigationEvent.ReplaceAll(Routes.TarotResults(store.partyId.toString())), last.navigationEvents.first())
    }

    @Test
    fun `a failed write keeps the saisie with its message`() = runTest {
        val store = FakeTarotStore()
        val vm = roundViewModel(store)
        vm.fill(store)
        store.failWrites = true
        vm.onValidate()
        assertEquals(R.string.belote_round_error_failed, vm.loaded().errorRes)
        assertFalse(vm.loaded().isSaving)
    }

    @Test
    fun `a correction is prefilled and saved in place`() = runTest {
        val store = FakeTarotStore(playerCount = 5)
        store.addRound(store.input().copy(contract = TarotContract.GARDE_SANS, chelem = Chelem.ANNOUNCED_FAILED))
        val vm = roundViewModel(store, correctedRoundNumber = 1)

        val state = vm.loaded()
        assertTrue(state.isCorrection)
        assertEquals(store.alice.id, state.takerId)
        assertEquals(TarotContract.GARDE_SANS, state.contract)
        assertEquals(TarotPartnerChoice.Alone, state.partner)
        assertEquals("41", state.pointsText)
        assertEquals(Chelem.ANNOUNCED_FAILED, state.chelem)
        assertTrue(state.canValidate)

        vm.onChelemSelected(null)
        vm.onValidate()
        assertNull(store.tarot.rounds.value.single().input.chelem)
        assertEquals(NavigationEvent.NavigateBack, vm.navigationEvents.first())
    }

    @Test
    fun `an unknown Manche or a finished Partie cannot be entered`() = runTest {
        assertEquals(TarotRoundUiState.Unavailable, roundViewModel(FakeTarotStore(), correctedRoundNumber = 4).uiState.value)
        val finished = FakeTarotStore().apply { party.value = party.value?.copy(status = PartyState.FINISHED) }
        assertEquals(TarotRoundUiState.Unavailable, roundViewModel(finished).uiState.value)
    }

    // --- Story 13.4 ---

    @Test
    fun `the Partie opens its Statistiques, which follow the Manches and rank the takers`() = runTest {
        val store = FakeTarotStore()
        store.addRound(store.input(points = 50))
        val party = partyViewModel(store)
        party.onShowStats()
        assertEquals(NavigationEvent.NavigateTo(Routes.TarotStats(store.partyId.toString())), party.navigationEvents.first())

        val stats = TarotStatsViewModel(ObserveTarotPartyUseCase(store, store.tarot, rules)).also { it.initialize(store.partyId) }
        var loaded = stats.uiState.value as TarotStatsUiState.Loaded
        assertEquals(1, loaded.table.rows.size)
        assertEquals(TakerUiModel("Alice", takes = 1, made = 1), loaded.takers.first())

        store.addRound(store.input(points = 30, taker = store.bob))
        loaded = stats.uiState.value as TarotStatsUiState.Loaded
        assertEquals(2, loaded.table.rows.size)
        assertEquals(listOf("Alice", "Bob", "Chloé", "David"), loaded.takers.map { it.name })
        assertEquals(TakerUiModel("Bob", takes = 1, made = 0), loaded.takers[1])
    }

    @Test
    fun `the Statistiques of a missing Partie are unavailable`() = runTest {
        val store = FakeTarotStore().apply { party.value = null }
        val stats = TarotStatsViewModel(ObserveTarotPartyUseCase(store, store.tarot, rules)).also { it.initialize(store.partyId) }
        assertEquals(TarotStatsUiState.Unavailable, stats.uiState.value)
    }

    private fun TarotRoundViewModel.fill(store: FakeTarotStore) {
        onTakerSelected(store.alice.id)
        onContractSelected(TarotContract.PRISE)
        onBoutsSelected(2)
        onPointsChanged("41")
    }
}
