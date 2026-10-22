package com.rvetiveau.rvscorecount.ui.screens.belote

import com.rvetiveau.rvscorecount.domain.usecase.FakeFreeGameRepository
import com.rvetiveau.rvscorecount.game.free.FreeGameEngine
import androidx.lifecycle.SavedStateHandle
import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.domain.model.EndRule
import com.rvetiveau.rvscorecount.domain.model.BeloteOutcome
import com.rvetiveau.rvscorecount.domain.model.BeloteRound
import com.rvetiveau.rvscorecount.domain.model.BeloteRoundInput
import com.rvetiveau.rvscorecount.domain.model.PartyState
import com.rvetiveau.rvscorecount.domain.model.TeamPoints
import com.rvetiveau.rvscorecount.domain.model.TeamSide.FIRST
import com.rvetiveau.rvscorecount.domain.model.TeamSide.SECOND
import com.rvetiveau.rvscorecount.domain.usecase.CorrectBeloteRoundUseCase
import com.rvetiveau.rvscorecount.domain.usecase.CreateBelotePartyUseCase
import com.rvetiveau.rvscorecount.domain.usecase.DeletePartyUseCase
import com.rvetiveau.rvscorecount.domain.usecase.FakeBeloteStore
import com.rvetiveau.rvscorecount.domain.usecase.ObserveBelotePartyUseCase
import com.rvetiveau.rvscorecount.domain.usecase.FakePlayerRepository
import com.rvetiveau.rvscorecount.domain.usecase.ObserveKnownPlayerNamesUseCase
import com.rvetiveau.rvscorecount.domain.usecase.ObservePartyHistoryUseCase
import com.rvetiveau.rvscorecount.domain.usecase.PlayBeloteRoundUseCase
import com.rvetiveau.rvscorecount.game.belote.BeloteEngine
import com.rvetiveau.rvscorecount.ui.navigation.NavigationEvent
import com.rvetiveau.rvscorecount.ui.navigation.Routes
import com.rvetiveau.rvscorecount.ui.screens.history.HistoryViewModel
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
import java.io.IOException
import java.util.UUID

/** ViewModels of the Belote screens (Story 7.3) over the real engine and an in-memory store. */
@OptIn(ExperimentalCoroutinesApi::class)
class BeloteViewModelsTest {

    private val store = FakeBeloteStore()
    private val rules = BeloteEngine()
    private val observe = ObserveBelotePartyUseCase(store, store, rules)
    private val create = CreateBelotePartyUseCase(store, store)
    private val playRound = PlayBeloteRoundUseCase(store, store, rules)

    /** Plays with the first player of the taking Équipe as taker (Story 8.3). */
    private suspend fun play(partyId: UUID, input: BeloteRoundInput) =
        playRound(partyId, input, store.takerOf(partyId, input.taker))

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // Setup

    /** [knownNames] are listed from the most to the least recently played. */
    private fun setupViewModel(knownNames: List<String> = emptyList()): BeloteSetupViewModel {
        val players = FakePlayerRepository(*knownNames.toTypedArray())
        return BeloteSetupViewModel(create, ObserveKnownPlayerNamesUseCase(players))
            .apply { initialize(listOf("Nous", "Eux")) }
    }

    private fun BeloteSetupViewModel.fillPlayers() {
        listOf("Alice", "Chloé", "Bob", "David").forEachIndexed { index, name ->
            onPlayerNameChanged(team = index / 2, seat = index % 2, name = name)
        }
    }

    @Test
    fun `setup starts with the default teams and end rule, and the table alternates the teams`() {
        val viewModel = setupViewModel()
        viewModel.fillPlayers()

        val state = viewModel.uiState.value
        assertEquals(listOf("Nous", "Eux"), state.teamNames)
        assertEquals(EndRule.TargetScore(1000), state.endRule)
        assertEquals(listOf("Alice", "Bob", "Chloé", "David"), state.tableOrder)
        assertTrue(state.canStart)
    }

    @Test
    fun `a selected suggestion fills the first empty player field until all four are seated`() {
        val viewModel = setupViewModel(knownNames = listOf("Alice", "Bob", "Chloé", "David", "Émile"))
        viewModel.onPlayerNameChanged(team = 0, seat = 0, name = "alice")
        assertEquals(listOf("Bob", "Chloé", "David", "Émile"), viewModel.uiState.value.playerSuggestions)

        listOf("Chloé", "Bob", "David").forEach(viewModel::onSuggestionSelected)

        val state = viewModel.uiState.value
        assertEquals(listOf(listOf("alice", "Chloé"), listOf("Bob", "David")), state.playerNames)
        assertEquals(emptyList<String>(), state.playerSuggestions)
        viewModel.onSuggestionSelected("Émile")
        assertEquals(state.playerNames, viewModel.uiState.value.playerNames)
    }

    @Test
    fun `setup cannot start with the same player twice`() {
        val viewModel = setupViewModel()
        viewModel.fillPlayers()
        assertFalse(viewModel.uiState.value.hasDuplicatePlayer)

        viewModel.onPlayerNameChanged(team = 1, seat = 1, name = "alice ")

        assertTrue(viewModel.uiState.value.hasDuplicatePlayer)
        assertFalse(viewModel.uiState.value.canStart)
    }

    @Test
    fun `setup cannot start with a missing name or an invalid end value`() {
        val viewModel = setupViewModel()
        assertFalse(viewModel.uiState.value.canStart)

        viewModel.fillPlayers()
        viewModel.onTeamNameChanged(1, " ")
        assertFalse(viewModel.uiState.value.canStart)

        viewModel.onTeamNameChanged(1, "Eux")
        viewModel.onEndValueChanged("0")
        assertNull(viewModel.uiState.value.endRule)
        assertFalse(viewModel.uiState.value.canStart)
    }

    @Test
    fun `each end mode keeps its value and only digits are accepted`() {
        val viewModel = setupViewModel()

        viewModel.onEndModeChanged(BeloteEndMode.ROUND_COUNT)
        viewModel.onEndValueChanged("12")
        viewModel.onEndValueChanged("1a")
        viewModel.onEndValueChanged("12345")
        assertEquals(EndRule.RoundCount(12), viewModel.uiState.value.endRule)

        viewModel.onEndModeChanged(BeloteEndMode.TARGET_SCORE)
        assertEquals(EndRule.TargetScore(1000), viewModel.uiState.value.endRule)
    }

    @Test
    fun `starting creates the party and replaces the stack with it`() = runTest {
        val viewModel = setupViewModel()
        viewModel.fillPlayers()

        viewModel.onStartParty()

        val party = store.parties.value.values.single()
        assertEquals(NavigationEvent.ReplaceAll(Routes.BeloteParty(party.id.toString())), viewModel.navigationEvents.first())
        assertEquals(EndRule.TargetScore(1000), party.endRule)
    }

    @Test
    fun `the setup history button opens the Belote history and keeps the draft`() = runTest {
        val viewModel = setupViewModel()
        viewModel.fillPlayers()

        viewModel.onHistorySelected()

        assertEquals(NavigationEvent.NavigateTo(Routes.GameHistory("belote")), viewModel.navigationEvents.first())
        assertTrue(viewModel.uiState.value.canStart)
    }

    @Test
    fun `a creation failure is shown and the draft is kept`() {
        store.writeFailure = IOException("disk")
        val viewModel = setupViewModel()
        viewModel.fillPlayers()

        viewModel.onStartParty()

        assertEquals(R.string.setup_error_creation_failed, viewModel.uiState.value.creationErrorRes)
        assertTrue(viewModel.uiState.value.canStart)
    }

    // Party in progress

    @Test
    fun `the party screen shows the leader, the progress, the dealer, the reserve and the rounds`() = runTest {
        val party = store.seedParty()
        play(party.id, BeloteRoundInput(FIRST, 100))
        play(party.id, BeloteRoundInput(SECOND, 81))
        val viewModel = BelotePartyViewModel(observe).apply { initialize(party.id) }

        val state = viewModel.uiState.value as BelotePartyUiState.Loaded
        assertEquals(listOf(181, 62), state.teams.map { it.total })
        assertEquals(listOf(true, false), state.teams.map { it.isLeader })
        assertEquals(listOf("Alice", "Chloé"), state.teams[0].playerNames)
        assertEquals(BeloteProgress.Target(1000, 819, listOf("Nous")), state.progress)
        assertEquals("Chloé", state.dealerName)
        assertEquals(81, state.pendingReserve)
        assertEquals(listOf(BeloteOutcome.MADE, BeloteOutcome.DISPUTED), state.rounds.map { it.outcome })
        assertEquals("Bob", state.rounds[1].takerName)
        assertEquals(3, state.nextRoundNumber)
    }

    @Test
    fun `a party by round count shows the current round, and equal totals at the end an extra one`() = runTest {
        val party = store.seedParty(EndRule.RoundCount(2))
        val viewModel = BelotePartyViewModel(observe).apply { initialize(party.id) }
        assertEquals(BeloteProgress.Rounds(1, 2), (viewModel.uiState.value as BelotePartyUiState.Loaded).progress)

        play(party.id, BeloteRoundInput(FIRST, 100))
        play(party.id, BeloteRoundInput(SECOND, 100))

        assertEquals(BeloteProgress.ExtraRound, (viewModel.uiState.value as BelotePartyUiState.Loaded).progress)
    }

    @Test
    fun `the party screen opens the round entry and the correction`() = runTest {
        val party = store.seedParty()
        val viewModel = BelotePartyViewModel(observe).apply { initialize(party.id) }

        viewModel.onPlayRound()
        assertEquals(NavigationEvent.NavigateTo(Routes.BeloteRound(party.id.toString())), viewModel.navigationEvents.first())
        viewModel.onRoundSelected(2)
        assertEquals(NavigationEvent.NavigateTo(Routes.BeloteRound(party.id.toString(), 2)), viewModel.navigationEvents.first())
    }

    @Test
    fun `a finished party sends to its results and an absent one is not found`() = runTest {
        val finished = store.seedParty(status = PartyState.FINISHED)
        val viewModel = BelotePartyViewModel(observe).apply { initialize(finished.id) }
        assertEquals(NavigationEvent.ReplaceAll(Routes.BeloteResults(finished.id.toString())), viewModel.navigationEvents.first())

        val absent = BelotePartyViewModel(observe).apply { initialize(UUID.randomUUID()) }
        assertEquals(BelotePartyUiState.NotFound, absent.uiState.value)
    }

    // Round entry

    private fun roundViewModel(partyId: UUID, correctedRoundNumber: Int? = null) =
        BeloteRoundViewModel(observe, playRound, CorrectBeloteRoundUseCase(store, store, rules), rules)
            .apply { initialize(partyId, correctedRoundNumber) }

    private val BeloteRoundViewModel.loaded get() = uiState.value as BeloteRoundUiState.Loaded

    @Test
    fun `typing one team's points fills the other's and the preview follows`() {
        val party = store.seedParty()
        val viewModel = roundViewModel(party.id)

        viewModel.onTakerPlayerSelected(store.takerOf(party.id, FIRST))
        viewModel.onPointsChanged(SECOND, "62")

        assertEquals(listOf("100", "62"), viewModel.loaded.pointsInputs)
        assertEquals(TeamPoints(100, 62), viewModel.loaded.preview?.points)
        assertTrue(viewModel.loaded.canValidate)
    }

    @Test
    fun `points above 162 are flagged and block the validation`() {
        val party = store.seedParty()
        val viewModel = roundViewModel(party.id)
        viewModel.onTakerPlayerSelected(store.takerOf(party.id, FIRST))

        viewModel.onPointsChanged(FIRST, "170")

        assertTrue(viewModel.loaded.pointsError)
        assertEquals(listOf("170", ""), viewModel.loaded.pointsInputs)
        assertNull(viewModel.loaded.preview)
        assertFalse(viewModel.loaded.canValidate)
    }

    @Test
    fun `a capot sets 162 and 0, and typing points again removes it`() {
        val party = store.seedParty()
        val viewModel = roundViewModel(party.id)
        viewModel.onTakerPlayerSelected(store.takerOf(party.id, SECOND))

        viewModel.onCapotChanged(SECOND)
        assertEquals(listOf("0", "162"), viewModel.loaded.pointsInputs)
        assertEquals(BeloteOutcome.CAPOT, viewModel.loaded.preview?.outcome)

        viewModel.onPointsChanged(SECOND, "150")
        assertNull(viewModel.loaded.capotTeam)
        assertEquals(BeloteOutcome.MADE, viewModel.loaded.preview?.outcome)
    }

    @Test
    fun `the preview includes the pending reserve`() = runTest {
        val party = store.seedParty()
        play(party.id, BeloteRoundInput(FIRST, 81))
        val viewModel = roundViewModel(party.id)

        viewModel.onTakerPlayerSelected(store.takerOf(party.id, SECOND))
        viewModel.onPointsChanged(SECOND, "100")

        assertEquals(2, viewModel.loaded.roundNumber)
        assertEquals(81, viewModel.loaded.preview?.reserveWon)
        assertEquals(TeamPoints(62, 181), viewModel.loaded.preview?.points)
    }

    @Test
    fun `validating a round goes back to the party`() = runTest {
        val party = store.seedParty()
        val viewModel = roundViewModel(party.id)
        viewModel.onTakerPlayerSelected(store.takerOf(party.id, FIRST))
        viewModel.onPointsChanged(FIRST, "100")

        viewModel.onValidate()

        assertEquals(NavigationEvent.NavigateBack, viewModel.navigationEvents.first())
        assertEquals(BeloteRoundInput(FIRST, 100), store.getRounds(party.id).single().input)
    }

    @Test
    fun `the round ending the party opens the results`() = runTest {
        val party = store.seedParty(EndRule.RoundCount(1))
        val viewModel = roundViewModel(party.id)
        viewModel.onTakerPlayerSelected(store.takerOf(party.id, FIRST))
        viewModel.onPointsChanged(FIRST, "100")

        viewModel.onValidate()

        assertEquals(NavigationEvent.ReplaceAll(Routes.BeloteResults(party.id.toString())), viewModel.navigationEvents.first())
    }

    @Test
    fun `a correction starts from the stored input and the reserve left before it`() = runTest {
        val party = store.seedParty()
        play(party.id, BeloteRoundInput(FIRST, 81))
        play(party.id, BeloteRoundInput(SECOND, 100, beloteTeam = FIRST))
        val viewModel = roundViewModel(party.id, correctedRoundNumber = 2)

        val state = viewModel.loaded
        assertTrue(state.isCorrection)
        assertEquals(SECOND, state.taker)
        assertEquals(listOf("62", "100"), state.pointsInputs)
        assertEquals(FIRST, state.beloteTeam)
        assertEquals(81, state.preview?.reserveWon)

        viewModel.onPointsChanged(SECOND, "120")
        viewModel.onValidate()
        assertEquals(NavigationEvent.NavigateBack, viewModel.navigationEvents.first())
        assertEquals(120, store.getRounds(party.id)[1].input.takerTrickPoints)
    }

    @Test
    fun `choosing the player who took chooses the taking team`() {
        val party = store.seedParty()
        val viewModel = roundViewModel(party.id)
        val bob = store.participants.value.first { it.name == "Bob" }

        assertEquals(listOf("Alice", "Bob", "Chloé", "David"), viewModel.loaded.players.map { it.name })
        viewModel.onTakerPlayerSelected(bob.id)
        viewModel.onPointsChanged(SECOND, "100")

        assertEquals(bob.id, viewModel.loaded.takerPlayerId)
        assertEquals(SECOND, viewModel.loaded.taker)
        assertTrue(viewModel.loaded.canValidate)
    }

    @Test
    fun `correcting a round played before asks for the player who took`() = runTest {
        val party = store.seedParty()
        store.rounds.value = listOf(BeloteRound(UUID.randomUUID(), party.id, 1, BeloteRoundInput(SECOND, 100)))
        val viewModel = roundViewModel(party.id, correctedRoundNumber = 1)

        assertEquals(SECOND, viewModel.loaded.taker)
        assertNull(viewModel.loaded.takerPlayerId)
        assertFalse(viewModel.loaded.canValidate)

        viewModel.onTakerPlayerSelected(store.takerOf(party.id, SECOND))
        viewModel.onValidate()
        assertEquals(NavigationEvent.NavigateBack, viewModel.navigationEvents.first())
        assertEquals(store.takerOf(party.id, SECOND), store.getRounds(party.id).single().takerPlayerId)
    }

    @Test
    fun `the statistics rank the takers for the Chad card`() = runTest {
        val party = store.seedParty()
        play(party.id, BeloteRoundInput(SECOND, 100))
        val viewModel = BeloteStatsViewModel(observe).apply { initialize(party.id) }

        val takers = (viewModel.uiState.value as BeloteStatsUiState.Loaded).takers
        assertEquals(BeloteTakerUiModel("Bob", takes = 1, made = 1), takers.first())
        assertEquals(4, takers.size)
    }

    @Test
    fun `an unknown round or a finished party cannot be entered`() {
        val party = store.seedParty()
        assertEquals(BeloteRoundUiState.Unavailable, roundViewModel(party.id, correctedRoundNumber = 1).uiState.value)

        val finished = store.seedParty(status = PartyState.FINISHED)
        assertEquals(BeloteRoundUiState.Unavailable, roundViewModel(finished.id).uiState.value)
    }

    @Test
    fun `a saving failure is shown and the entry is kept`() {
        val party = store.seedParty()
        val viewModel = roundViewModel(party.id)
        viewModel.onTakerPlayerSelected(store.takerOf(party.id, FIRST))
        viewModel.onPointsChanged(FIRST, "100")
        store.writeFailure = IOException("disk")

        viewModel.onValidate()

        assertEquals(R.string.belote_round_error_failed, viewModel.loaded.errorRes)
        assertTrue(viewModel.loaded.canValidate)
    }

    // Results

    @Test
    fun `the results show the winner first and replay starts a new party`() = runTest {
        val party = store.seedParty(EndRule.RoundCount(1))
        play(party.id, BeloteRoundInput(SECOND, 100))
        val viewModel = BeloteResultsViewModel(observe, create).apply { initialize(party.id) }

        val state = viewModel.uiState.value as BeloteResultsUiState.Loaded
        assertEquals(listOf("Eux", "Nous"), state.teams.map { it.name })
        assertEquals(listOf(100, 62), state.teams.map { it.total })

        viewModel.onReplay()
        val newParty = store.parties.value.values.single { it.id != party.id }
        assertEquals(NavigationEvent.ReplaceAll(Routes.BeloteParty(newParty.id.toString())), viewModel.navigationEvents.first())
        assertEquals(EndRule.RoundCount(1), newParty.endRule)
    }

    @Test
    fun `the results open the statistics`() = runTest {
        val party = store.seedParty(EndRule.RoundCount(1))
        play(party.id, BeloteRoundInput(SECOND, 100))
        val viewModel = BeloteResultsViewModel(observe, create).apply { initialize(party.id) }

        assertEquals(1, (viewModel.uiState.value as BeloteResultsUiState.Loaded).roundCount)
        viewModel.onShowStats()
        assertEquals(NavigationEvent.NavigateTo(Routes.BeloteStats(party.id.toString())), viewModel.navigationEvents.first())
    }

    // Historique, détail et statistiques (Story 7.4)

    @Test
    fun `the history opens the Belote detail for a Belote party`() = runTest {
        val party = store.seedParty(EndRule.RoundCount(1))
        play(party.id, BeloteRoundInput(FIRST, 100))
        val viewModel = HistoryViewModel(ObservePartyHistoryUseCase(store, store, rules, FakeFreeGameRepository(), FreeGameEngine()), DeletePartyUseCase(store), SavedStateHandle())

        viewModel.onEntrySelected(party.id)

        assertEquals(NavigationEvent.NavigateTo(Routes.BelotePartyDetail(party.id.toString())), viewModel.navigationEvents.first())
    }

    @Test
    fun `the detail shows both teams and the rounds, and replays the party`() = runTest {
        val party = store.seedParty(EndRule.RoundCount(2))
        play(party.id, BeloteRoundInput(FIRST, 81))
        play(party.id, BeloteRoundInput(SECOND, 100))
        val viewModel = BelotePartyDetailViewModel(observe, create).apply { initialize(party.id) }

        val state = viewModel.uiState.value as BelotePartyDetailUiState.Loaded
        assertEquals(listOf("Nous", "Eux"), state.teams.map { it.name })
        assertEquals(listOf(62, 262), state.teams.map { it.total })
        assertEquals(listOf(false, true), state.teams.map { it.isLeader })
        assertEquals(listOf(BeloteOutcome.DISPUTED, BeloteOutcome.MADE), state.rounds.map { it.outcome })

        viewModel.onReplay()
        val newParty = store.parties.value.values.single { it.id != party.id }
        assertEquals(NavigationEvent.ReplaceAll(Routes.BeloteParty(newParty.id.toString())), viewModel.navigationEvents.first())
    }

    @Test
    fun `a party in progress has no detail`() {
        val party = store.seedParty()

        val viewModel = BelotePartyDetailViewModel(observe, create).apply { initialize(party.id) }

        assertEquals(BelotePartyDetailUiState.Unavailable, viewModel.uiState.value)
    }

    @Test
    fun `the statistics follow the cumulated score of each team, live`() = runTest {
        val party = store.seedParty()
        play(party.id, BeloteRoundInput(FIRST, 100))
        val viewModel = BeloteStatsViewModel(observe).apply { initialize(party.id) }

        play(party.id, BeloteRoundInput(SECOND, 81))

        val state = viewModel.uiState.value as BeloteStatsUiState.Loaded
        assertEquals(2, state.roundCount)
        assertEquals(listOf("Nous", "Eux"), state.series.map { it.player.name })
        assertEquals(listOf(listOf(0, 100, 181), listOf(0, 62, 62)), state.series.map { it.points })
    }

    @Test
    fun `the party screen opens the statistics`() = runTest {
        val party = store.seedParty()
        val viewModel = BelotePartyViewModel(observe).apply { initialize(party.id) }

        viewModel.onShowStats()

        assertEquals(NavigationEvent.NavigateTo(Routes.BeloteStats(party.id.toString())), viewModel.navigationEvents.first())
    }

    @Test
    fun `a party in progress has no results`() {
        val party = store.seedParty()

        val viewModel = BeloteResultsViewModel(observe, create).apply { initialize(party.id) }

        assertEquals(BeloteResultsUiState.Unavailable, viewModel.uiState.value)
    }
}
