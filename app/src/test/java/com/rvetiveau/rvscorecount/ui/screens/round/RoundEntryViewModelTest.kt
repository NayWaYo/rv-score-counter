package com.rvetiveau.rvscorecount.ui.screens.round

import androidx.lifecycle.SavedStateHandle
import com.rvetiveau.rvscorecount.domain.engine.GameEngine
import com.rvetiveau.rvscorecount.domain.engine.RoundValidation
import com.rvetiveau.rvscorecount.domain.model.CardSuit
import com.rvetiveau.rvscorecount.domain.model.Participant
import com.rvetiveau.rvscorecount.domain.model.Party
import com.rvetiveau.rvscorecount.domain.model.PartyState
import com.rvetiveau.rvscorecount.domain.model.Round
import com.rvetiveau.rvscorecount.domain.model.RoundScore
import com.rvetiveau.rvscorecount.domain.repository.PartyRepository
import com.rvetiveau.rvscorecount.domain.usecase.GetAvailableGamesUseCase
import com.rvetiveau.rvscorecount.domain.usecase.RollDieUseCase
import com.rvetiveau.rvscorecount.domain.usecase.ValidateRoundUseCase
import com.rvetiveau.rvscorecount.ui.navigation.NavigationEvent
import com.rvetiveau.rvscorecount.game.papayoo.PapayooEngine
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
        var saveRoundInvocationCount = 0
            private set
        var savedRound: Round? = null
            private set
        var savedScores: Map<UUID, Int>? = null
            private set
        var saveFailure: Throwable? = null

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
        override suspend fun saveRound(round: Round, scores: Map<UUID, Int>, updatedParty: Party) {
            saveRoundInvocationCount++
            saveFailure?.let { throw it }
            savedRound = round
            savedScores = scores
        }
        override suspend fun getRoundsByPartyId(partyId: UUID): List<Round> = emptyList()
        override suspend fun getScoresByRoundId(roundId: UUID): List<RoundScore> = emptyList()
    }

    private class RecordingGameEngine : GameEngine by PapayooEngine() {
        var validateRoundInvocationCount = 0
            private set

        override fun validateRound(scores: Map<UUID, Int>): RoundValidation {
            validateRoundInvocationCount++
            return PapayooEngine().validateRound(scores)
        }
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
        partyRepository: PartyRepository = FakePartyRepository(),
        getAvailableGamesUseCase: GetAvailableGamesUseCase = GetAvailableGamesUseCase(
            mapOf(PapayooEngine.GAME_ID to PapayooEngine())
        ),
        validateRoundUseCase: ValidateRoundUseCase = ValidateRoundUseCase(
            partyRepository,
            mapOf(PapayooEngine.GAME_ID to PapayooEngine())
        )
    ) = RoundEntryViewModel(
        partyRepository,
        rollDieUseCase,
        savedStateHandle,
        getAvailableGamesUseCase,
        validateRoundUseCase
    )

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

    // --- Story 3.3 derived score state (AC 6, 8) ---

    @Test
    fun `sum state is neutral while a score is missing below the round total`() {
        val aliceId = UUID.randomUUID()
        val bobId = UUID.randomUUID()
        val content = RoundEntryContent(
            participants = listOf(
                RoundEntryParticipant(aliceId, "Alice"),
                RoundEntryParticipant(bobId, "Bob")
            ),
            scores = mapOf(aliceId to "100", bobId to ""),
            totalPointsPerRound = 250
        )

        assertTrue(!content.allFilled)
        assertEquals(SumIndicatorState.NEUTRAL, content.sumIndicatorState)
        assertTrue(!content.canValidate)
    }

    @Test
    fun `sum state is success and validation enabled only when all scores equal the round total`() {
        val aliceId = UUID.randomUUID()
        val bobId = UUID.randomUUID()
        val content = RoundEntryContent(
            participants = listOf(
                RoundEntryParticipant(aliceId, "Alice"),
                RoundEntryParticipant(bobId, "Bob")
            ),
            scores = mapOf(aliceId to "100", bobId to "150"),
            totalPointsPerRound = 250
        )

        assertTrue(content.allFilled)
        assertEquals(SumIndicatorState.SUCCESS, content.sumIndicatorState)
        assertTrue(content.canValidate)
    }

    @Test
    fun `sum state is error as soon as entered scores exceed the round total`() {
        val aliceId = UUID.randomUUID()
        val bobId = UUID.randomUUID()
        val charlieId = UUID.randomUUID()
        val content = RoundEntryContent(
            participants = listOf(
                RoundEntryParticipant(aliceId, "Alice"),
                RoundEntryParticipant(bobId, "Bob"),
                RoundEntryParticipant(charlieId, "Charlie")
            ),
            scores = mapOf(aliceId to "200", bobId to "100", charlieId to ""),
            totalPointsPerRound = 250
        )

        assertTrue(!content.allFilled)
        assertEquals(SumIndicatorState.ERROR, content.sumIndicatorState)
        assertTrue(!content.canValidate)
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
    fun `is a load error when no engine is registered for the party game`() = runTest {
        val partyId = UUID.randomUUID()
        val vm = viewModel(
            partyRepository = FakePartyRepository(
                aParty(partyId),
                listOf(aParticipant(partyId, "Alice", 0))
            ),
            getAvailableGamesUseCase = GetAvailableGamesUseCase(emptyMap())
        )

        vm.initialize(partyId)

        assertTrue(vm.uiState.value.loadError)
        assertNull(vm.uiState.value.content)
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
        val charlie = aParticipant(partyId, "Charlie", 2)
        val vm = viewModel(partyRepository = FakePartyRepository(aParty(partyId), listOf(alice, bob, charlie)))
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
        val participants = listOf(
            alice,
            aParticipant(partyId, "Bob", 1),
            aParticipant(partyId, "Charlie", 2)
        )
        val vm = viewModel(partyRepository = FakePartyRepository(aParty(partyId), participants))
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
        val participants = List(4) { aParticipant(partyId, "P$it", it) }
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
        val charlie = aParticipant(partyId, "Charlie", 2)
        val vm = viewModel(partyRepository = FakePartyRepository(aParty(partyId), listOf(alice, bob, charlie)))
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

    // --- Story 3.3 last-score precomputation (AC 1, 3, 4, 5) ---

    @Test
    fun `fills and marks the sole remaining draft regardless of entry order`() = runTest {
        val partyId = UUID.randomUUID()
        val participants = List(3) { aParticipant(partyId, "P$it", it) }
        val vm = viewModel(partyRepository = FakePartyRepository(aParty(partyId), participants))
        vm.initialize(partyId)

        vm.onScoreChanged(participants[2].id, "80")
        vm.onScoreChanged(participants[0].id, "100")

        val content = vm.uiState.value.content!!
        assertEquals("70", content.scores[participants[1].id])
        assertEquals(participants[1].id, content.precomputedParticipantId)
    }

    @Test
    fun `editing the precomputed draft clears its marker without immediately recomputing it`() = runTest {
        val partyId = UUID.randomUUID()
        val participants = List(3) { aParticipant(partyId, "P$it", it) }
        val vm = viewModel(partyRepository = FakePartyRepository(aParty(partyId), participants))
        vm.initialize(partyId)
        vm.onScoreChanged(participants[0].id, "100")
        vm.onScoreChanged(participants[1].id, "80")

        vm.onScoreChanged(participants[2].id, "")

        val content = vm.uiState.value.content!!
        assertEquals("", content.scores[participants[2].id])
        assertNull(content.precomputedParticipantId)
    }

    @Test
    fun `changing another draft moves and recalculates the precomputed score`() = runTest {
        val partyId = UUID.randomUUID()
        val participants = List(3) { aParticipant(partyId, "P$it", it) }
        val vm = viewModel(partyRepository = FakePartyRepository(aParty(partyId), participants))
        vm.initialize(partyId)
        vm.onScoreChanged(participants[0].id, "100")
        vm.onScoreChanged(participants[1].id, "80")

        vm.onScoreChanged(participants[0].id, "")

        val content = vm.uiState.value.content!!
        assertEquals("100", content.scores[participants[0].id])
        assertEquals(participants[0].id, content.precomputedParticipantId)
    }

    @Test
    fun `keeps a negative precomputed complement unchanged`() = runTest {
        val partyId = UUID.randomUUID()
        val participants = List(3) { aParticipant(partyId, "P$it", it) }
        val vm = viewModel(partyRepository = FakePartyRepository(aParty(partyId), participants))
        vm.initialize(partyId)

        vm.onScoreChanged(participants[0].id, "200")
        vm.onScoreChanged(participants[1].id, "100")

        val content = vm.uiState.value.content!!
        assertEquals("-50", content.scores[participants[2].id])
        assertEquals(participants[2].id, content.precomputedParticipantId)
    }

    @Test
    fun `score entry never validates or persists a round`() = runTest {
        val partyId = UUID.randomUUID()
        val participants = List(3) { aParticipant(partyId, "P$it", it) }
        val repository = FakePartyRepository(aParty(partyId), participants)
        val engine = RecordingGameEngine()
        val vm = viewModel(
            partyRepository = repository,
            getAvailableGamesUseCase = GetAvailableGamesUseCase(mapOf(engine.gameId to engine))
        )
        vm.initialize(partyId)

        vm.onScoreChanged(participants[0].id, "100")
        vm.onScoreChanged(participants[1].id, "80")

        assertEquals(0, engine.validateRoundInvocationCount)
        assertEquals(0, repository.saveRoundInvocationCount)
    }

    // --- Story 3.4 validation (AC 6, 7, 8) ---

    private fun readyToValidate(
        repository: FakePartyRepository,
        participants: List<Participant>,
        partyId: UUID
    ): RoundEntryViewModel = viewModel(partyRepository = repository).also { vm ->
        vm.initialize(partyId)
        vm.onRoll()
        vm.onScoreChanged(participants[0].id, "100")
        vm.onScoreChanged(participants[1].id, "80")
    }

    @Test
    fun `onValidate persists the drafts with the rolled suit and navigates back`() = runTest {
        val partyId = UUID.randomUUID()
        val participants = List(3) { aParticipant(partyId, "P$it", it) }
        val repository = FakePartyRepository(aParty(partyId), participants)
        val vm = readyToValidate(repository, participants, partyId)
        assertTrue(vm.uiState.value.canValidate)

        vm.onValidate()

        assertEquals(CardSuit.SPADE, repository.savedRound?.suit)
        assertEquals(
            mapOf(participants[0].id to 100, participants[1].id to 80, participants[2].id to 70),
            repository.savedScores
        )
        assertEquals(NavigationEvent.NavigateBack, vm.navigationEvents.first())
        assertTrue(vm.uiState.value.isSaving)
        assertTrue(!vm.uiState.value.canValidate)
    }

    @Test
    fun `onValidate is ignored while the suit has not been rolled`() = runTest {
        val partyId = UUID.randomUUID()
        val participants = List(3) { aParticipant(partyId, "P$it", it) }
        val repository = FakePartyRepository(aParty(partyId), participants)
        val vm = viewModel(partyRepository = repository)
        vm.initialize(partyId)
        vm.onScoreChanged(participants[0].id, "100")
        vm.onScoreChanged(participants[1].id, "80")

        assertTrue(!vm.uiState.value.canValidate)
        vm.onValidate()

        assertEquals(0, repository.saveRoundInvocationCount)
    }

    @Test
    fun `the first roll stays available after scores were entered`() = runTest {
        val partyId = UUID.randomUUID()
        val participants = List(3) { aParticipant(partyId, "P$it", it) }
        val vm = viewModel(partyRepository = FakePartyRepository(aParty(partyId), participants))
        vm.initialize(partyId)
        vm.onScoreChanged(participants[0].id, "100")

        vm.onRoll()

        assertEquals(CardSuit.SPADE, vm.uiState.value.suit)
        assertTrue(!vm.uiState.value.rollEnabled)
    }

    @Test
    fun `a negative precomputed complement blocks validation even when the sum is exact`() = runTest {
        val partyId = UUID.randomUUID()
        val participants = List(3) { aParticipant(partyId, "P$it", it) }
        val repository = FakePartyRepository(aParty(partyId), participants)
        val vm = viewModel(partyRepository = repository)
        vm.initialize(partyId)
        vm.onRoll()
        vm.onScoreChanged(participants[0].id, "200")
        vm.onScoreChanged(participants[1].id, "100")

        assertTrue(!vm.uiState.value.canValidate)
        vm.onValidate()

        assertEquals(0, repository.saveRoundInvocationCount)
    }

    @Test
    fun `a persistence failure keeps the drafts and shows a save error until the next edit`() = runTest {
        val partyId = UUID.randomUUID()
        val participants = List(3) { aParticipant(partyId, "P$it", it) }
        val repository = FakePartyRepository(aParty(partyId), participants)
        repository.saveFailure = IllegalStateException("disk full")
        val vm = readyToValidate(repository, participants, partyId)

        vm.onValidate()

        val state = vm.uiState.value
        assertTrue(state.saveError)
        assertTrue(!state.isSaving)
        assertEquals("70", state.content!!.scores[participants[2].id])
        assertNull(withTimeoutOrNull(100) { vm.navigationEvents.first() })

        vm.onScoreChanged(participants[2].id, "7")
        assertTrue(!vm.uiState.value.saveError)
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

    @Test
    fun `precomputes the sole empty draft after restoring valid saved scores`() = runTest {
        val partyId = UUID.randomUUID()
        val participants = List(3) { aParticipant(partyId, "P$it", it) }
        val savedStateHandle = SavedStateHandle(
            mapOf(
                "score_${participants[0].id}" to "100",
                "score_${participants[1].id}" to "80"
            )
        )
        val vm = viewModel(
            savedStateHandle = savedStateHandle,
            partyRepository = FakePartyRepository(aParty(partyId), participants)
        )

        vm.initialize(partyId)

        val content = vm.uiState.value.content!!
        assertEquals("70", content.scores[participants[2].id])
        assertEquals(participants[2].id, content.precomputedParticipantId)
    }

    @Test
    fun `does not turn an invalid restored draft into a precomputed score`() = runTest {
        val partyId = UUID.randomUUID()
        val alice = aParticipant(partyId, "Alice", 0)
        val bob = aParticipant(partyId, "Bob", 1)
        val savedStateHandle = SavedStateHandle(
            mapOf("score_${alice.id}" to "invalid", "score_${bob.id}" to "80")
        )
        val vm = viewModel(
            savedStateHandle = savedStateHandle,
            partyRepository = FakePartyRepository(aParty(partyId), listOf(alice, bob))
        )

        vm.initialize(partyId)

        val content = vm.uiState.value.content!!
        assertEquals("", content.scores[alice.id])
        assertNull(content.precomputedParticipantId)
    }
}
