package com.rvetiveau.rvscorecount.ui.screens.round

import androidx.lifecycle.SavedStateHandle
import com.rvetiveau.rvscorecount.domain.model.CardSuit
import com.rvetiveau.rvscorecount.domain.model.Participant
import com.rvetiveau.rvscorecount.domain.model.Party
import com.rvetiveau.rvscorecount.domain.model.PartyState
import com.rvetiveau.rvscorecount.domain.model.Round
import com.rvetiveau.rvscorecount.domain.model.RoundScore
import com.rvetiveau.rvscorecount.domain.repository.PartyRepository
import com.rvetiveau.rvscorecount.domain.usecase.RollDieUseCase
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RoundEntryViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeRollDieUseCase(private vararg val results: CardSuit) : RollDieUseCase() {
        var invocationCount = 0
            private set

        override fun invoke(random: kotlin.random.Random): CardSuit =
            results[invocationCount.coerceAtMost(results.lastIndex)].also { invocationCount++ }
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
        override fun observeParty(id: UUID): Flow<Party?> = error("not used in this test")
        override suspend fun getPartyInProgress(): Party? = error("not used in this test")
        override suspend fun addParticipant(participant: Participant) = error("not used in this test")
        override suspend fun getParticipantsByPartyId(partyId: UUID): List<Participant> = participants
        override fun observeParticipants(partyId: UUID): Flow<List<Participant>> = error("not used in this test")
        override suspend fun saveRound(round: Round, scores: Map<UUID, Int>) = error("not used in this test")
        override suspend fun getRoundsByPartyId(partyId: UUID): List<Round> = error("not used in this test")
        override suspend fun getScoresByRoundId(roundId: UUID): List<RoundScore> = error("not used in this test")
    }

    private fun aParty(id: UUID, status: PartyState = PartyState.IN_PROGRESS) = Party(
        id = id,
        gameId = "papayoo",
        status = status,
        currentDealerIndex = 0,
        createdAt = 0L
    )

    private fun aParticipant(
        partyId: UUID,
        name: String,
        position: Int,
        id: UUID = UUID.randomUUID()
    ) = Participant(id = id, partyId = partyId, name = name, listPosition = position)

    private fun viewModel(
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
        rollDieUseCase: RollDieUseCase = FakeRollDieUseCase(CardSuit.SPADE),
        partyRepository: PartyRepository = FakePartyRepository()
    ) = RoundEntryViewModel(partyRepository, rollDieUseCase, savedStateHandle)

    private fun initializedViewModel(
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
        rollDieUseCase: RollDieUseCase = FakeRollDieUseCase(CardSuit.SPADE)
    ): RoundEntryViewModel {
        val partyId = UUID.randomUUID()
        return viewModel(
            savedStateHandle = savedStateHandle,
            rollDieUseCase = rollDieUseCase,
            partyRepository = FakePartyRepository(
                aParty(partyId),
                listOf(aParticipant(partyId, "Alice", 0))
            )
        ).also { it.initialize(partyId) }
    }

    // --- Story 3.1 die roll behavior, preserved ---

    @Test
    fun `initial state has no suit and does not roll implicitly`() = runTest {
        val vm = viewModel()

        assertNull(vm.uiState.value.suit)
    }

    @Test
    fun `onRoll invokes the use case exactly once and publishes its result`() = runTest {
        val rollDieUseCase = FakeRollDieUseCase(CardSuit.HEART)
        val vm = initializedViewModel(rollDieUseCase = rollDieUseCase)

        vm.onRoll()

        assertEquals(1, rollDieUseCase.invocationCount)
        assertEquals(CardSuit.HEART, vm.uiState.value.suit)
    }

    @Test
    fun `onRoll persists the suit name to SavedStateHandle`() = runTest {
        val savedStateHandle = SavedStateHandle()
        val vm = initializedViewModel(savedStateHandle)

        vm.onRoll()

        assertEquals(vm.uiState.value.suit!!.name, savedStateHandle.get<String>("suit"))
    }

    @Test
    fun `restores each of the four saved suit values`() = runTest {
        CardSuit.entries.forEach { suit ->
            val vm = viewModel(SavedStateHandle(mapOf("suit" to suit.name)))
            assertEquals(suit, vm.uiState.value.suit)
        }
    }

    @Test
    fun `an unknown saved value falls back to not rolled instead of a default suit`() = runTest {
        val vm = viewModel(SavedStateHandle(mapOf("suit" to "UNKNOWN_SUIT")))

        assertNull(vm.uiState.value.suit)
    }

    @Test
    fun `reroll invokes the use case once again and replaces the result`() = runTest {
        val rollDieUseCase = FakeRollDieUseCase(CardSuit.SPADE, CardSuit.DIAMOND)
        val vm = initializedViewModel(rollDieUseCase = rollDieUseCase)

        vm.onRoll()
        vm.onRoll()

        assertEquals(2, rollDieUseCase.invocationCount)
        assertEquals(CardSuit.DIAMOND, vm.uiState.value.suit)
    }

    @Test
    fun `onRoll is ignored until content is loaded`() = runTest {
        val rollDieUseCase = FakeRollDieUseCase(CardSuit.HEART)
        val vm = viewModel(rollDieUseCase = rollDieUseCase)

        vm.onRoll()

        assertEquals(0, rollDieUseCase.invocationCount)
        assertNull(vm.uiState.value.suit)
        assertTrue(!vm.uiState.value.rerollEnabled)
    }

    // --- Story 3.2 participant loading (AC 1, 10) ---

    @Test
    fun `initialize loads participants ordered by listPosition with empty drafts`() = runTest {
        val partyId = UUID.randomUUID()
        val bob = aParticipant(partyId, "Bob", 1)
        val alice = aParticipant(partyId, "Alice", 0)
        val vm = viewModel(partyRepository = FakePartyRepository(aParty(partyId), listOf(bob, alice)))

        vm.initialize(partyId)

        val content = vm.uiState.value.content
        checkNotNull(content)
        assertEquals(listOf(alice.id, bob.id), content.participants.map { it.id })
        assertEquals(listOf("Alice", "Bob"), content.participants.map { it.name })
        assertEquals("", content.scores[alice.id])
        assertEquals("", content.scores[bob.id])
        assertTrue(!vm.uiState.value.isLoading)
        assertTrue(!vm.uiState.value.loadError)
    }

    @Test
    fun `initialize is idempotent and keeps the first loaded content on a repeated call`() = runTest {
        val partyId = UUID.randomUUID()
        val alice = aParticipant(partyId, "Alice", 0)
        val repository = FakePartyRepository(aParty(partyId), listOf(alice))
        val vm = viewModel(partyRepository = repository)

        vm.initialize(partyId)
        vm.onScoreChanged(alice.id, "10")
        vm.initialize(partyId)

        assertEquals("10", vm.uiState.value.content!!.scores[alice.id])
    }

    @Test
    fun `is a load error when the party does not exist`() = runTest {
        val vm = viewModel(partyRepository = FakePartyRepository(party = null))

        vm.initialize(UUID.randomUUID())

        assertTrue(vm.uiState.value.loadError)
        assertNull(vm.uiState.value.content)
    }

    @Test
    fun `is a load error when the repository returns a different party`() = runTest {
        val requestedPartyId = UUID.randomUUID()
        val vm = viewModel(
            partyRepository = FakePartyRepository(
                aParty(UUID.randomUUID()),
                listOf(aParticipant(requestedPartyId, "Alice", 0))
            )
        )

        vm.initialize(requestedPartyId)

        assertTrue(vm.uiState.value.loadError)
        assertNull(vm.uiState.value.content)
    }

    @Test
    fun `is a load error when the party is not IN_PROGRESS`() = runTest {
        val partyId = UUID.randomUUID()
        val vm = viewModel(
            partyRepository = FakePartyRepository(
                aParty(partyId, status = PartyState.FINISHED),
                listOf(aParticipant(partyId, "Alice", 0))
            )
        )

        vm.initialize(partyId)

        assertTrue(vm.uiState.value.loadError)
    }

    @Test
    fun `is a load error when there are no participants`() = runTest {
        val partyId = UUID.randomUUID()
        val vm = viewModel(partyRepository = FakePartyRepository(aParty(partyId), emptyList()))

        vm.initialize(partyId)

        assertTrue(vm.uiState.value.loadError)
    }

    @Test
    fun `is a load error when list positions are duplicated`() = runTest {
        val partyId = UUID.randomUUID()
        val participants = listOf(aParticipant(partyId, "Alice", 0), aParticipant(partyId, "Bob", 0))
        val vm = viewModel(partyRepository = FakePartyRepository(aParty(partyId), participants))

        vm.initialize(partyId)

        assertTrue(vm.uiState.value.loadError)
    }

    @Test
    fun `is a load error when list positions are not continuous`() = runTest {
        val partyId = UUID.randomUUID()
        val participants = listOf(aParticipant(partyId, "Alice", 0), aParticipant(partyId, "Bob", 2))
        val vm = viewModel(partyRepository = FakePartyRepository(aParty(partyId), participants))

        vm.initialize(partyId)

        assertTrue(vm.uiState.value.loadError)
    }

    @Test
    fun `is a load error when participant ids are duplicated`() = runTest {
        val partyId = UUID.randomUUID()
        val duplicateId = UUID.randomUUID()
        val participants = listOf(
            aParticipant(partyId, "Alice", 0, duplicateId),
            aParticipant(partyId, "Bob", 1, duplicateId)
        )
        val vm = viewModel(partyRepository = FakePartyRepository(aParty(partyId), participants))

        vm.initialize(partyId)

        assertTrue(vm.uiState.value.loadError)
    }

    @Test
    fun `is a load error when a participant belongs to a different party`() = runTest {
        val partyId = UUID.randomUUID()
        val foreignParticipant = aParticipant(UUID.randomUUID(), "Mallory", 0)
        val vm = viewModel(partyRepository = FakePartyRepository(aParty(partyId), listOf(foreignParticipant)))

        vm.initialize(partyId)

        assertTrue(vm.uiState.value.loadError)
    }

    @Test
    fun `shows a load error and recovers after retry once the repository succeeds`() = runTest {
        val partyId = UUID.randomUUID()
        val repository = FakePartyRepository(
            party = aParty(partyId),
            participants = listOf(aParticipant(partyId, "Alice", 0)),
            failure = IllegalStateException("Room read failed")
        )
        val vm = viewModel(partyRepository = repository)

        vm.initialize(partyId)

        assertTrue(vm.uiState.value.loadError)
        repository.failure = null
        vm.retry(partyId)

        assertTrue(!vm.uiState.value.loadError)
        assertTrue(vm.uiState.value.content != null)
    }

    // --- Story 3.2 free-order score drafting (AC 3, 4, 5, 8) ---

    @Test
    fun `onScoreChanged updates only the targeted participant in any order`() = runTest {
        val partyId = UUID.randomUUID()
        val alice = aParticipant(partyId, "Alice", 0)
        val bob = aParticipant(partyId, "Bob", 1)
        val vm = viewModel(partyRepository = FakePartyRepository(aParty(partyId), listOf(alice, bob)))
        vm.initialize(partyId)

        vm.onScoreChanged(bob.id, "80")

        val content = vm.uiState.value.content!!
        assertEquals("80", content.scores[bob.id])
        assertEquals("", content.scores[alice.id])
    }

    @Test
    fun `an empty draft is distinct from a zero score`() = runTest {
        val partyId = UUID.randomUUID()
        val alice = aParticipant(partyId, "Alice", 0)
        val vm = viewModel(partyRepository = FakePartyRepository(aParty(partyId), listOf(alice)))
        vm.initialize(partyId)

        vm.onScoreChanged(alice.id, "0")
        assertEquals("0", vm.uiState.value.content!!.scores[alice.id])
        assertEquals(0L, vm.uiState.value.currentSum)
        assertTrue(vm.uiState.value.content!!.hasAnyScoreEntered)

        vm.onScoreChanged(alice.id, "")
        assertEquals("", vm.uiState.value.content!!.scores[alice.id])
        assertEquals(0L, vm.uiState.value.currentSum)
        assertTrue(!vm.uiState.value.content!!.hasAnyScoreEntered)
    }

    @Test
    fun `rejects a sign, letters, a decimal separator and an Int overflow without transforming them`() = runTest {
        val partyId = UUID.randomUUID()
        val alice = aParticipant(partyId, "Alice", 0)
        val vm = viewModel(partyRepository = FakePartyRepository(aParty(partyId), listOf(alice)))
        vm.initialize(partyId)

        listOf("-5", "abc", "1.5", "99999999999").forEach { invalid ->
            vm.onScoreChanged(alice.id, invalid)
            assertEquals("", vm.uiState.value.content!!.scores[alice.id])
        }
    }

    @Test
    fun `ignores an unknown participant id`() = runTest {
        val partyId = UUID.randomUUID()
        val alice = aParticipant(partyId, "Alice", 0)
        val vm = viewModel(partyRepository = FakePartyRepository(aParty(partyId), listOf(alice)))
        vm.initialize(partyId)

        vm.onScoreChanged(UUID.randomUUID(), "10")

        assertEquals("", vm.uiState.value.content!!.scores[alice.id])
    }

    @Test
    fun `onScoreChanged is a no-op before initialize has loaded content`() = runTest {
        val vm = viewModel()

        vm.onScoreChanged(UUID.randomUUID(), "10")

        assertNull(vm.uiState.value.content)
    }

    @Test
    fun `computes the current sum in real time without overflow`() = runTest {
        val partyId = UUID.randomUUID()
        val participants = List(3) { aParticipant(partyId, "P$it", it) }
        val vm = viewModel(partyRepository = FakePartyRepository(aParty(partyId), participants))
        vm.initialize(partyId)

        vm.onScoreChanged(participants[0].id, Int.MAX_VALUE.toString())
        vm.onScoreChanged(participants[1].id, Int.MAX_VALUE.toString())

        assertEquals(Int.MAX_VALUE.toLong() * 2, vm.uiState.value.currentSum)
    }

    @Test
    fun `rerollEnabled locks as soon as any score is entered, including zero, and unlocks when all are cleared`() = runTest {
        val partyId = UUID.randomUUID()
        val alice = aParticipant(partyId, "Alice", 0)
        val bob = aParticipant(partyId, "Bob", 1)
        val vm = viewModel(partyRepository = FakePartyRepository(aParty(partyId), listOf(alice, bob)))
        vm.initialize(partyId)

        assertTrue(vm.uiState.value.rerollEnabled)

        vm.onScoreChanged(alice.id, "0")
        assertTrue(!vm.uiState.value.rerollEnabled)

        vm.onScoreChanged(alice.id, "")
        assertTrue(vm.uiState.value.rerollEnabled)
    }

    @Test
    fun `onRoll preserves the first suit and drafts when a score locks reroll`() = runTest {
        val partyId = UUID.randomUUID()
        val alice = aParticipant(partyId, "Alice", 0)
        val rollDieUseCase = FakeRollDieUseCase(CardSuit.SPADE, CardSuit.DIAMOND)
        val vm = viewModel(
            rollDieUseCase = rollDieUseCase,
            partyRepository = FakePartyRepository(aParty(partyId), listOf(alice))
        )
        vm.initialize(partyId)
        vm.onRoll()
        vm.onScoreChanged(alice.id, "42")

        vm.onRoll()

        assertEquals("42", vm.uiState.value.content!!.scores[alice.id])
        assertEquals(CardSuit.SPADE, vm.uiState.value.suit)
        assertEquals(1, rollDieUseCase.invocationCount)
    }

    // --- Story 3.2 SavedStateHandle restoration (AC 9) ---

    @Test
    fun `restores a previously saved valid draft after participants load`() = runTest {
        val partyId = UUID.randomUUID()
        val alice = aParticipant(partyId, "Alice", 0)
        val savedStateHandle = SavedStateHandle(mapOf("score_${alice.id}" to "15"))
        val vm = viewModel(
            savedStateHandle = savedStateHandle,
            partyRepository = FakePartyRepository(aParty(partyId), listOf(alice))
        )

        vm.initialize(partyId)

        assertEquals("15", vm.uiState.value.content!!.scores[alice.id])
    }

    @Test
    fun `ignores an invalid saved draft instead of restoring it`() = runTest {
        val partyId = UUID.randomUUID()
        val alice = aParticipant(partyId, "Alice", 0)
        val savedStateHandle = SavedStateHandle(mapOf("score_${alice.id}" to "-5"))
        val vm = viewModel(
            savedStateHandle = savedStateHandle,
            partyRepository = FakePartyRepository(aParty(partyId), listOf(alice))
        )

        vm.initialize(partyId)

        assertEquals("", vm.uiState.value.content!!.scores[alice.id])
    }

    @Test
    fun `a saved draft for a participant absent from the loaded party is not applied to anyone`() = runTest {
        val partyId = UUID.randomUUID()
        val alice = aParticipant(partyId, "Alice", 0)
        val savedStateHandle = SavedStateHandle(mapOf("score_${UUID.randomUUID()}" to "15"))
        val vm = viewModel(
            savedStateHandle = savedStateHandle,
            partyRepository = FakePartyRepository(aParty(partyId), listOf(alice))
        )

        vm.initialize(partyId)

        assertEquals("", vm.uiState.value.content!!.scores[alice.id])
    }
}
