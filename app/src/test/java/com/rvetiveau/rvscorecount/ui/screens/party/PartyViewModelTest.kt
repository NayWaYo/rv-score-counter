package com.rvetiveau.rvscorecount.ui.screens.party

import com.rvetiveau.rvscorecount.domain.model.Participant
import com.rvetiveau.rvscorecount.domain.model.Party
import com.rvetiveau.rvscorecount.domain.model.PartyState
import com.rvetiveau.rvscorecount.domain.model.Round
import com.rvetiveau.rvscorecount.domain.model.RoundScore
import com.rvetiveau.rvscorecount.domain.repository.PartyRepository
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
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
class PartyViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakePartyRepository(
        private val party: Party? = null,
        private val participants: List<Participant> = emptyList(),
        var failure: Throwable? = null
    ) : PartyRepository {
        override suspend fun createParty(party: Party) = error("not used in this test")
        override suspend fun updateParty(party: Party) = error("not used in this test")
        override suspend fun createPartyWithParticipants(party: Party, participants: List<Participant>) = error("not used in this test")
        override suspend fun getPartyById(id: UUID): Party? {
            failure?.let { throw it }
            return party
        }
        override fun observeParty(id: UUID): Flow<Party?> = flowOf(party)
        override suspend fun getPartyInProgress(): Party? = error("not used in this test")
        override suspend fun addParticipant(participant: Participant) = error("not used in this test")
        override suspend fun getParticipantsByPartyId(partyId: UUID): List<Participant> = participants
        override fun observeParticipants(partyId: UUID): Flow<List<Participant>> = flowOf(participants)
        override suspend fun saveRound(round: Round, scores: Map<UUID, Int>) = error("not used in this test")
        override suspend fun getRoundsByPartyId(partyId: UUID): List<Round> = error("not used in this test")
        override suspend fun getScoresByRoundId(roundId: UUID): List<RoundScore> = error("not used in this test")
    }

    private fun aParty(id: UUID, dealerIndex: Int = 0) = Party(
        id = id,
        gameId = "papayoo",
        status = PartyState.IN_PROGRESS,
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
        val vm = PartyViewModel(FakePartyRepository(party = aParty(partyId), participants = participants))

        vm.initialize(partyId)

        val state = vm.uiState.value
        check(state is PartyUiState.Loaded)
        assertEquals("Alice", state.dealerName)
    }

    @Test
    fun `derives the dealer from currentDealerIndex, not just the first participant`() = runTest {
        val partyId = UUID.randomUUID()
        val participants = listOf(aParticipant(partyId, "Alice", 0), aParticipant(partyId, "Bob", 1))
        val vm = PartyViewModel(FakePartyRepository(party = aParty(partyId, dealerIndex = 1), participants = participants))

        vm.initialize(partyId)

        val state = vm.uiState.value
        check(state is PartyUiState.Loaded)
        assertEquals("Bob", state.dealerName)
    }

    @Test
    fun `is PartyNotFound when the party does not exist`() = runTest {
        val vm = PartyViewModel(FakePartyRepository(party = null))

        vm.initialize(UUID.randomUUID())

        assertTrue(vm.uiState.value is PartyUiState.PartyNotFound)
    }

    @Test
    fun `is InconsistentParticipants when no participant matches the dealer index`() = runTest {
        val partyId = UUID.randomUUID()
        val participants = listOf(aParticipant(partyId, "Alice", 0))
        val vm = PartyViewModel(FakePartyRepository(party = aParty(partyId, dealerIndex = 5), participants = participants))

        vm.initialize(partyId)

        assertTrue(vm.uiState.value is PartyUiState.InconsistentParticipants)
    }

    @Test
    fun `is InconsistentParticipants when the party has no participants`() = runTest {
        val partyId = UUID.randomUUID()
        val vm = PartyViewModel(FakePartyRepository(party = aParty(partyId), participants = emptyList()))

        vm.initialize(partyId)

        assertTrue(vm.uiState.value is PartyUiState.InconsistentParticipants)
    }

    @Test
    fun `is InconsistentParticipants when list positions are duplicated`() = runTest {
        val partyId = UUID.randomUUID()
        val participants = listOf(aParticipant(partyId, "Alice", 0), aParticipant(partyId, "Bob", 0))
        val vm = PartyViewModel(FakePartyRepository(party = aParty(partyId), participants = participants))

        vm.initialize(partyId)

        assertTrue(vm.uiState.value is PartyUiState.InconsistentParticipants)
    }

    @Test
    fun `is InconsistentParticipants when list positions are not continuous`() = runTest {
        val partyId = UUID.randomUUID()
        val participants = listOf(aParticipant(partyId, "Alice", 0), aParticipant(partyId, "Bob", 2))
        val vm = PartyViewModel(FakePartyRepository(party = aParty(partyId), participants = participants))

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
        val vm = PartyViewModel(repository)

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
        val vm = PartyViewModel(FakePartyRepository(party = aParty(partyId), participants = participants))

        vm.initialize(partyId)
        vm.initialize(partyId)

        val state = vm.uiState.value
        check(state is PartyUiState.Loaded)
        assertEquals("Alice", state.dealerName)
    }
}
