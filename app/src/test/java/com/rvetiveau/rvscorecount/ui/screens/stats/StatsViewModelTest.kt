package com.rvetiveau.rvscorecount.ui.screens.stats

import com.rvetiveau.rvscorecount.domain.model.CardSuit
import com.rvetiveau.rvscorecount.domain.model.Participant
import com.rvetiveau.rvscorecount.domain.model.Party
import com.rvetiveau.rvscorecount.domain.model.PartyState
import com.rvetiveau.rvscorecount.domain.model.Round
import com.rvetiveau.rvscorecount.domain.model.RoundScore
import com.rvetiveau.rvscorecount.domain.repository.PartyRepository
import com.rvetiveau.rvscorecount.ui.components.RoundRecordEntryUi
import com.rvetiveau.rvscorecount.ui.components.RoundRecordUi
import com.rvetiveau.rvscorecount.ui.components.RoundTableRow
import com.rvetiveau.rvscorecount.ui.components.SevenChampionUi
import com.rvetiveau.rvscorecount.ui.navigation.NavigationEvent
import com.rvetiveau.rvscorecount.ui.navigation.Routes
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakePartyRepository(
        private val party: Party?,
        private val participants: List<Participant>,
        private val rounds: List<Round> = emptyList(),
        val roundScores: MutableList<RoundScore> = mutableListOf()
    ) : PartyRepository {
        var readFailure: Throwable? = null

        override suspend fun createParty(party: Party) = error("not used in this test")
        override suspend fun updateParty(party: Party) = error("not used in this test")
        override suspend fun createPartyWithParticipants(party: Party, participants: List<Participant>) = error("not used in this test")
        override suspend fun getPartyById(id: UUID): Party? {
            readFailure?.let { throw it }
            return party
        }
        override fun observeParty(id: UUID): Flow<Party?> = error("not used in this test")
        override suspend fun getPartyInProgress(): Party? = error("not used in this test")
        override fun observeFinishedParties(): Flow<List<Party>> = error("not used in this test")
        override suspend fun deleteParty(id: UUID) = error("not used in this test")
        override suspend fun addParticipant(participant: Participant, updatedParty: Party) = error("not used in this test")
        override suspend fun getParticipantsByPartyId(partyId: UUID): List<Participant> = participants
        override fun observeParticipants(partyId: UUID): Flow<List<Participant>> = error("not used in this test")
        override suspend fun saveRound(round: Round, scores: Map<UUID, Int>, updatedParty: Party) = error("not used in this test")
        override suspend fun correctRound(round: Round, scores: Map<UUID, Int>) {
            roundScores.replaceAll { score ->
                if (score.roundId == round.id) score.copy(points = scores.getValue(score.participantId)) else score
            }
        }
        override suspend fun getRoundsByPartyId(partyId: UUID): List<Round> = rounds
        override suspend fun getScoresByRoundId(roundId: UUID): List<RoundScore> = roundScores.filter { it.roundId == roundId }
    }

    private val partyId = UUID.randomUUID()
    private val alice = Participant(UUID.randomUUID(), partyId, "Alice", 0)
    private val bob = Participant(UUID.randomUUID(), partyId, "Bob", 1)
    private val round1 = Round(UUID.randomUUID(), partyId, 1, CardSuit.HEART, sevenHolderId = alice.id)
    private val round2 = Round(UUID.randomUUID(), partyId, 2, CardSuit.SPADE, sevenHolderId = bob.id)

    private fun aParty(status: PartyState = PartyState.FINISHED) =
        Party(id = partyId, gameId = "papayoo", status = status, currentDealerIndex = 0, createdAt = 42L)

    private fun playedRepository(status: PartyState = PartyState.FINISHED) = FakePartyRepository(
        aParty(status),
        listOf(alice, bob),
        // Stored newest first on purpose: the sheet must still be chronological.
        listOf(round2, round1),
        mutableListOf(
            RoundScore(round1.id, bob.id, 50), RoundScore(round1.id, alice.id, 200),
            RoundScore(round2.id, alice.id, 0), RoundScore(round2.id, bob.id, 250)
        )
    )

    private fun loaded(vm: StatsViewModel) = vm.uiState.value as StatsUiState.Loaded

    @Test
    fun `gathers the classement with player stats, the stat cards and the rounds in chronological order`() = runTest {
        val vm = StatsViewModel(playedRepository()).also { it.initialize(partyId) }

        val state = loaded(vm)
        assertEquals(listOf("Alice", "Bob"), state.standings.map { it.name })
        assertEquals(listOf(200, 300), state.standings.map { it.cumulativeScore })
        assertTrue(state.standings.all { it.stats != null })
        assertEquals(
            listOf(
                RoundTableRow(1, CardSuit.HEART, mapOf(alice.id to 200, bob.id to 50), alice.id),
                RoundTableRow(2, CardSuit.SPADE, mapOf(alice.id to 0, bob.id to 250), bob.id)
            ),
            state.roundTable.rows
        )
        assertEquals(RoundRecordUi(listOf(RoundRecordEntryUi("Bob", 2)), 250), state.roundRecord)
        assertEquals(SevenChampionUi(listOf("Alice", "Bob"), 1), state.sevenChampion)
        assertTrue(!state.canCorrectRounds)
    }

    @Test
    fun `a party without rounds has an empty sheet and no stat card`() = runTest {
        val vm = StatsViewModel(FakePartyRepository(aParty(), listOf(alice, bob))).also { it.initialize(partyId) }

        val state = loaded(vm)
        assertTrue(state.roundTable.rows.isEmpty())
        assertNull(state.roundRecord)
        assertNull(state.sevenChampion)
    }

    @Test
    fun `a missing or inconsistent party has no statistics`() = runTest {
        listOf(
            FakePartyRepository(null, listOf(alice)),
            FakePartyRepository(aParty(), listOf(alice), listOf(round1), mutableListOf(RoundScore(round1.id, UUID.randomUUID(), 250)))
        ).forEach { repository ->
            val vm = StatsViewModel(repository).also { it.initialize(partyId) }
            assertEquals(StatsUiState.Unavailable, vm.uiState.value)
        }
    }

    @Test
    fun `a read failure shows the error state and retry recovers`() = runTest {
        val repository = playedRepository().also { it.readFailure = IllegalStateException("Room read failed") }
        val vm = StatsViewModel(repository).also { it.initialize(partyId) }
        assertEquals(StatsUiState.LoadError, vm.uiState.value)

        repository.readFailure = null
        vm.retry(partyId)

        assertNotNull(vm.uiState.value as? StatsUiState.Loaded)
    }

    @Test
    fun `a round of a party in progress opens its correction`() = runTest {
        val vm = StatsViewModel(playedRepository(PartyState.IN_PROGRESS)).also { it.initialize(partyId) }
        assertTrue(loaded(vm).canCorrectRounds)

        vm.onRoundSelected(2)

        assertEquals(
            NavigationEvent.NavigateTo(Routes.RoundCorrection(partyId.toString(), 2)),
            vm.navigationEvents.first()
        )
    }

    @Test
    fun `a finished party or an unknown round opens no correction`() = runTest {
        val finished = StatsViewModel(playedRepository()).also { it.initialize(partyId) }
        val inProgress = StatsViewModel(playedRepository(PartyState.IN_PROGRESS)).also { it.initialize(partyId) }

        finished.onRoundSelected(1)
        inProgress.onRoundSelected(3)

        assertNull(withTimeoutOrNull(100) { finished.navigationEvents.first() })
        assertNull(withTimeoutOrNull(100) { inProgress.navigationEvents.first() })
    }

    @Test
    fun `the corrected scores show once the screen resumes`() = runTest {
        val repository = playedRepository(PartyState.IN_PROGRESS)
        val vm = StatsViewModel(repository).also { it.initialize(partyId) }

        vm.onRoundSelected(1)
        vm.navigationEvents.first()
        repository.correctRound(round1, mapOf(alice.id to 50, bob.id to 200))
        vm.onScreenResumed()

        assertEquals(mapOf(alice.id to 50, bob.id to 200), loaded(vm).roundTable.rows.first().points)
        assertEquals(listOf(50, 450), loaded(vm).standings.map { it.cumulativeScore })
    }
}
