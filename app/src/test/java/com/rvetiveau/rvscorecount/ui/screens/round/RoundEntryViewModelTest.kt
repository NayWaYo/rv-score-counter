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
import com.rvetiveau.rvscorecount.domain.scan.TrumpReader
import com.rvetiveau.rvscorecount.domain.usecase.CorrectRoundUseCase
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
        var failure: Throwable? = null,
        private val rounds: List<Round> = emptyList(),
        private val roundScores: List<RoundScore> = emptyList()
    ) : PartyRepository {
        var saveRoundInvocationCount = 0
            private set
        var savedRound: Round? = null
            private set
        var savedScores: Map<UUID, Int>? = null
            private set
        var saveFailure: Throwable? = null
        var correctedRound: Round? = null
            private set
        var correctedScores: Map<UUID, Int>? = null
            private set

        override suspend fun createParty(party: Party) = error("not used in this test")
        override suspend fun updateParty(party: Party) = error("not used in this test")
        override suspend fun createPartyWithParticipants(party: Party, participants: List<Participant>) = error("not used in this test")
        override suspend fun getPartyById(id: UUID): Party? {
            failure?.let { throw it }
            return party
        }
        override fun observeParty(id: UUID): Flow<Party?> = error("not used in this test")
        override suspend fun getPartyInProgress(): Party? = error("not used in this test")
        override fun observeFinishedParties(): Flow<List<Party>> = error("not used in this test")
        override suspend fun deleteParty(id: UUID) = error("not used in this test")
        override suspend fun addParticipant(participant: Participant, updatedParty: Party) = error("not used in this test")
        override suspend fun getParticipantsByPartyId(partyId: UUID): List<Participant> = participants
        override fun observeParticipants(partyId: UUID): Flow<List<Participant>> = error("not used in this test")
        override suspend fun saveRound(round: Round, scores: Map<UUID, Int>, updatedParty: Party) {
            saveRoundInvocationCount++
            saveFailure?.let { throw it }
            savedRound = round
            savedScores = scores
        }
        override suspend fun correctRound(round: Round, scores: Map<UUID, Int>) {
            correctedRound = round
            correctedScores = scores
        }
        override suspend fun getRoundsByPartyId(partyId: UUID): List<Round> = rounds
        override suspend fun getScoresByRoundId(roundId: UUID): List<RoundScore> =
            roundScores.filter { it.roundId == roundId }
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
        ),
        trumpReader: TrumpReader = FakeTrumpReader()
    ) = RoundEntryViewModel(
        partyRepository,
        rollDieUseCase,
        savedStateHandle,
        getAvailableGamesUseCase,
        validateRoundUseCase,
        CorrectRoundUseCase(partyRepository, mapOf(PapayooEngine.GAME_ID to PapayooEngine())),
        trumpReader
    )

    /** Reads [result] on any photo, or throws [failure]. */
    private class FakeTrumpReader(
        var result: Set<Int> = emptySet(),
        var failure: Throwable? = null
    ) : TrumpReader {
        val readPaths = mutableListOf<String>()

        override suspend fun read(photoPath: String, numbers: IntRange): Set<Int> {
            readPaths += photoPath
            failure?.let { throw it }
            return result
        }
    }

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
    fun `suggests the sole remaining score without entering it, regardless of entry order`() = runTest {
        val partyId = UUID.randomUUID()
        val participants = List(3) { aParticipant(partyId, "P$it", it) }
        val vm = viewModel(partyRepository = FakePartyRepository(aParty(partyId), participants))
        vm.initialize(partyId)

        vm.onScoreChanged(participants[2].id, "80")
        vm.onScoreChanged(participants[0].id, "100")

        val content = vm.uiState.value.content!!
        assertEquals("", content.scores[participants[1].id])
        assertEquals(ScoreSuggestion(participants[1].id, 70), content.suggestion)
    }

    @Test
    fun `the suggestion follows each digit typed in another field`() = runTest {
        val partyId = UUID.randomUUID()
        val participants = List(3) { aParticipant(partyId, "P$it", it) }
        val vm = viewModel(partyRepository = FakePartyRepository(aParty(partyId), participants))
        vm.initialize(partyId)
        vm.onScoreChanged(participants[0].id, "100")

        vm.onScoreChanged(participants[1].id, "5")
        assertEquals(ScoreSuggestion(participants[2].id, 145), vm.uiState.value.content!!.suggestion)

        vm.onScoreChanged(participants[1].id, "50")
        val content = vm.uiState.value.content!!
        assertEquals("", content.scores[participants[2].id])
        assertEquals(ScoreSuggestion(participants[2].id, 100), content.suggestion)
    }

    @Test
    fun `typing in the suggested field replaces the suggestion`() = runTest {
        val partyId = UUID.randomUUID()
        val participants = List(3) { aParticipant(partyId, "P$it", it) }
        val vm = viewModel(partyRepository = FakePartyRepository(aParty(partyId), participants))
        vm.initialize(partyId)
        vm.onScoreChanged(participants[0].id, "100")
        vm.onScoreChanged(participants[1].id, "80")

        vm.onScoreChanged(participants[2].id, "7")

        val content = vm.uiState.value.content!!
        assertEquals("7", content.scores[participants[2].id])
        assertNull(content.suggestion)
    }

    @Test
    fun `clearing another field moves the suggestion there`() = runTest {
        val partyId = UUID.randomUUID()
        val participants = List(3) { aParticipant(partyId, "P$it", it) }
        val vm = viewModel(partyRepository = FakePartyRepository(aParty(partyId), participants))
        vm.initialize(partyId)
        vm.onScoreChanged(participants[0].id, "100")
        vm.onScoreChanged(participants[1].id, "80")
        vm.onScoreChanged(participants[2].id, "70")

        vm.onScoreChanged(participants[0].id, "")

        assertEquals(ScoreSuggestion(participants[0].id, 100), vm.uiState.value.content!!.suggestion)
    }

    @Test
    fun `suggests nothing when the entered scores already exceed the total`() = runTest {
        val partyId = UUID.randomUUID()
        val participants = List(3) { aParticipant(partyId, "P$it", it) }
        val vm = viewModel(partyRepository = FakePartyRepository(aParty(partyId), participants))
        vm.initialize(partyId)

        vm.onScoreChanged(participants[0].id, "200")
        vm.onScoreChanged(participants[1].id, "100")

        val content = vm.uiState.value.content!!
        assertEquals("", content.scores[participants[2].id])
        assertNull(content.suggestion)
        assertEquals(SumIndicatorState.ERROR, content.sumIndicatorState)
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
        vm.onSevenHolderSelected(participants[0].id)
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
        assertEquals(participants[0].id, repository.savedRound?.sevenHolderId)
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
    fun `an exceeded total blocks validation`() = runTest {
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
        assertEquals("80", state.content!!.scores[participants[1].id])
        assertEquals(ScoreSuggestion(participants[2].id, 70), state.content.suggestion)
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
    fun `suggests the sole empty draft after restoring valid saved scores`() = runTest {
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
        assertEquals("", content.scores[participants[2].id])
        assertEquals(ScoreSuggestion(participants[2].id, 70), content.suggestion)
    }

    @Test
    fun `an invalid restored draft is left empty and only suggested`() = runTest {
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
        assertEquals(ScoreSuggestion(alice.id, 170), content.suggestion)
    }

    // --- Qui a eu le 40 ---

    @Test
    fun `validation waits for the seven holder and rejects one with fewer than 40 points`() = runTest {
        val partyId = UUID.randomUUID()
        val participants = List(3) { aParticipant(partyId, "P$it", it) }
        val vm = viewModel(partyRepository = FakePartyRepository(aParty(partyId), participants))
        vm.initialize(partyId)
        vm.onRoll()
        vm.onScoreChanged(participants[0].id, "200")
        vm.onScoreChanged(participants[1].id, "20")
        assertEquals(40, vm.uiState.value.content!!.sevenPoints)
        assertTrue(!vm.uiState.value.canValidate)

        vm.onSevenHolderSelected(participants[1].id)
        assertTrue(vm.uiState.value.content!!.isSevenHolderScoreTooLow)
        assertTrue(!vm.uiState.value.canValidate)

        vm.onSevenHolderSelected(participants[0].id)
        assertTrue(!vm.uiState.value.content!!.isSevenHolderScoreTooLow)
        assertTrue(vm.uiState.value.canValidate)
    }

    @Test
    fun `the seven holder survives a process restart and an unknown id is ignored`() = runTest {
        val partyId = UUID.randomUUID()
        val participants = List(3) { aParticipant(partyId, "P$it", it) }
        val savedStateHandle = SavedStateHandle()
        val first = viewModel(savedStateHandle = savedStateHandle, partyRepository = FakePartyRepository(aParty(partyId), participants))
        first.initialize(partyId)
        first.onSevenHolderSelected(UUID.randomUUID())
        assertNull(first.uiState.value.content!!.sevenHolderId)
        first.onSevenHolderSelected(participants[2].id)

        val restored = viewModel(savedStateHandle = savedStateHandle, partyRepository = FakePartyRepository(aParty(partyId), participants))
        restored.initialize(partyId)

        assertEquals(participants[2].id, restored.uiState.value.content!!.sevenHolderId)
    }

    // --- Classement sous le dé ---

    private fun aPlayedParty(): Triple<UUID, List<Participant>, FakePartyRepository> {
        val partyId = UUID.randomUUID()
        val alice = aParticipant(partyId, "Alice", 0)
        val bob = aParticipant(partyId, "Bob", 1)
        val round1 = Round(UUID.randomUUID(), partyId, 1, CardSuit.HEART, sevenHolderId = bob.id)
        val round2 = Round(UUID.randomUUID(), partyId, 2, CardSuit.CLUB, sevenHolderId = bob.id)
        val repository = FakePartyRepository(
            party = aParty(partyId).copy(currentDealerIndex = 1),
            participants = listOf(alice, bob),
            rounds = listOf(round1, round2),
            roundScores = listOf(
                RoundScore(round1.id, alice.id, 40),
                RoundScore(round1.id, bob.id, 210),
                RoundScore(round2.id, alice.id, 100),
                RoundScore(round2.id, bob.id, 150)
            )
        )
        return Triple(partyId, listOf(alice, bob), repository)
    }

    @Test
    fun `loads the standings of the party with the dealer and without statistics`() = runTest {
        val (partyId, participants, repository) = aPlayedParty()
        val (alice, bob) = participants
        val vm = viewModel(partyRepository = repository).also { it.initialize(partyId) }

        val summary = vm.uiState.value.content!!.summary
        assertEquals(listOf(alice.id, bob.id), summary.standings.map { it.participantId })
        assertEquals(listOf(140, 360), summary.standings.map { it.cumulativeScore })
        assertEquals(listOf(false, true), summary.standings.map { it.isDealer })
        assertTrue(summary.standings.all { it.stats == null })
    }

    @Test
    fun `the first round has everyone at zero`() = runTest {
        val vm = initializedViewModel()

        val summary = vm.uiState.value.content!!.summary
        assertEquals(listOf(0), summary.standings.map { it.cumulativeScore })
    }

    @Test
    fun `a round score from an unknown participant is a load error`() = runTest {
        val partyId = UUID.randomUUID()
        val round = Round(UUID.randomUUID(), partyId, 1, CardSuit.HEART)
        val repository = FakePartyRepository(
            party = aParty(partyId),
            participants = listOf(aParticipant(partyId, "Alice", 0)),
            rounds = listOf(round),
            roundScores = listOf(RoundScore(round.id, UUID.randomUUID(), 250))
        )
        val vm = viewModel(partyRepository = repository).also { it.initialize(partyId) }

        assertTrue(vm.uiState.value.loadError)
        assertNull(vm.uiState.value.content)
    }

    @Test
    fun `only the die is shown until a panel is chosen`() = runTest {
        val vm = initializedViewModel()

        assertEquals(RoundEntryPanel.NONE, vm.uiState.value.panel)
    }

    @Test
    fun `each panel button shows its panel, and hides it on a second tap`() = runTest {
        val savedStateHandle = SavedStateHandle()
        val vm = initializedViewModel(savedStateHandle)

        vm.onPanelSelected(RoundEntryPanel.STANDINGS)
        assertEquals(RoundEntryPanel.STANDINGS, vm.uiState.value.panel)

        vm.onPanelSelected(RoundEntryPanel.SCORES)
        assertEquals(RoundEntryPanel.SCORES, vm.uiState.value.panel)
        assertEquals("SCORES", savedStateHandle.get<String>("panel"))

        vm.onPanelSelected(RoundEntryPanel.SCORES)
        assertEquals(RoundEntryPanel.NONE, vm.uiState.value.panel)
    }

    @Test
    fun `panels stay closed while the party is loading`() = runTest {
        val vm = viewModel()

        vm.onPanelSelected(RoundEntryPanel.SCORES)

        assertEquals(RoundEntryPanel.NONE, vm.uiState.value.panel)
    }

    @Test
    fun `a restored panel is shown again`() = runTest {
        val vm = initializedViewModel(SavedStateHandle(mapOf("panel" to "STANDINGS")))

        assertEquals(RoundEntryPanel.STANDINGS, vm.uiState.value.panel)
    }

    @Test
    fun `a restored score draft reopens the score form`() = runTest {
        val partyId = UUID.randomUUID()
        val alice = aParticipant(partyId, "Alice", 0)
        val vm = viewModel(
            savedStateHandle = SavedStateHandle(mapOf("score_${alice.id}" to "120")),
            partyRepository = FakePartyRepository(aParty(partyId), listOf(alice))
        ).also { it.initialize(partyId) }

        assertEquals(RoundEntryPanel.SCORES, vm.uiState.value.panel)
    }

    // --- Correction d'une Manche validée ---

    private inner class CorrectionFixture {
        val partyId: UUID = UUID.randomUUID()
        val alice = aParticipant(partyId, "Alice", 0)
        val bob = aParticipant(partyId, "Bob", 1)
        /** Joined after round 1: never part of its correction. */
        val charlie = aParticipant(partyId, "Charlie", 2)
        val round = Round(UUID.randomUUID(), partyId, 1, CardSuit.HEART, sevenHolderId = alice.id)
        val repository = FakePartyRepository(
            aParty(partyId),
            listOf(alice, bob, charlie),
            rounds = listOf(round),
            roundScores = listOf(RoundScore(round.id, alice.id, 150), RoundScore(round.id, bob.id, 100))
        )

        fun viewModel(
            roundNumber: Int = 1,
            savedStateHandle: SavedStateHandle = SavedStateHandle()
        ) = viewModel(savedStateHandle = savedStateHandle, partyRepository = repository)
            .also { it.initialize(partyId, roundNumber) }
    }

    @Test
    fun `a correction reopens the round with its suit, scores and 7 holder, for its players only`() = runTest {
        val fixture = CorrectionFixture()

        val state = fixture.viewModel().uiState.value

        assertEquals(1, state.correctedRoundNumber)
        assertEquals(CardSuit.HEART, state.suit)
        assertEquals(RoundEntryPanel.SCORES, state.panel)
        assertEquals(listOf("Alice", "Bob"), state.content!!.participants.map { it.name })
        assertEquals(mapOf(fixture.alice.id to "150", fixture.bob.id to "100"), state.content.scores)
        assertEquals(fixture.alice.id, state.content.sevenHolderId)
        assertTrue(state.canValidate)
    }

    @Test
    fun `the die stays locked during a correction`() = runTest {
        val vm = CorrectionFixture().viewModel()

        vm.onRoll()

        assertEquals(CardSuit.HEART, vm.uiState.value.suit)
        assertEquals(false, vm.uiState.value.rollEnabled)
    }

    @Test
    fun `saving a correction rewrites the round and returns to the party`() = runTest {
        val fixture = CorrectionFixture()
        val vm = fixture.viewModel()

        vm.onScoreChanged(fixture.alice.id, "")
        vm.onScoreChanged(fixture.bob.id, "90")
        vm.onSevenHolderSelected(fixture.bob.id)
        vm.onValidate()

        assertEquals(NavigationEvent.NavigateBack, vm.navigationEvents.first())
        assertEquals(mapOf(fixture.alice.id to 160, fixture.bob.id to 90), fixture.repository.correctedScores)
        assertEquals(fixture.round.copy(sevenHolderId = fixture.bob.id), fixture.repository.correctedRound)
        assertEquals(0, fixture.repository.saveRoundInvocationCount)
    }

    @Test
    fun `a saved draft wins over the stored score after process death`() = runTest {
        val fixture = CorrectionFixture()

        val state = fixture.viewModel(
            savedStateHandle = SavedStateHandle(mapOf("score_${fixture.bob.id}" to "90", "suit" to "SPADE"))
        ).uiState.value

        assertEquals("90", state.content!!.scores[fixture.bob.id])
        assertEquals(CardSuit.HEART, state.suit)
    }

    @Test
    fun `an unknown round is a load error`() = runTest {
        val state = CorrectionFixture().viewModel(roundNumber = 2).uiState.value

        assertTrue(state.loadError)
        assertNull(state.content)
    }

    // --- Story 9.1 trump scan ---

    private inner class ScanFixture(
        val reader: FakeTrumpReader = FakeTrumpReader(),
        engine: GameEngine = PapayooEngine()
    ) {
        val partyId: UUID = UUID.randomUUID()
        val alice = aParticipant(partyId, "Alice", 0)
        val bob = aParticipant(partyId, "Bob", 1)
        val chloe = aParticipant(partyId, "Chloé", 2)
        val vm = viewModel(
            partyRepository = FakePartyRepository(aParty(partyId), listOf(alice, bob, chloe)),
            getAvailableGamesUseCase = GetAvailableGamesUseCase(mapOf(PapayooEngine.GAME_ID to engine)),
            trumpReader = reader
        ).also { it.initialize(partyId) }

        val scan: TrumpScan? get() = vm.uiState.value.scan
        val content: RoundEntryContent get() = vm.uiState.value.content!!

        /** Photographs [participant]'s trumps and lets the reader read [read]. */
        fun scan(participant: Participant, read: Set<Int>) {
            reader.result = read
            vm.onScanRequested(participant.id)
            vm.onPhotoTaken("photo-${participant.name}.jpg")
        }
    }

    @Test
    fun `a scan request asks for a photo, and the photo is read into a confirmation`() = runTest {
        val fixture = ScanFixture()

        fixture.vm.onScanRequested(fixture.alice.id)
        assertEquals(Unit, withTimeoutOrNull(1_000) { fixture.vm.photoRequests.first() })
        fixture.reader.result = setOf(3, 6, 10)
        fixture.vm.onPhotoTaken("photo.jpg")

        val scan = fixture.scan!!
        assertEquals(fixture.alice.id, scan.participantId)
        assertEquals(false, scan.isReading)
        assertEquals(setOf(3, 6, 10), scan.selected)
        assertEquals(19, scan.score)
        assertEquals(listOf("photo.jpg"), fixture.reader.readPaths)
    }

    @Test
    fun `coming back from the camera without a photo changes nothing`() = runTest {
        val fixture = ScanFixture()

        fixture.vm.onScanRequested(fixture.alice.id)
        fixture.vm.onPhotoTaken(null)

        assertNull(fixture.scan)
        assertTrue(fixture.reader.readPaths.isEmpty())
    }

    @Test
    fun `nothing recognized and an unreadable photo are told apart`() = runTest {
        val fixture = ScanFixture()

        fixture.scan(fixture.alice, read = emptySet())
        assertEquals(true, fixture.scan!!.nothingRecognized)
        assertEquals(false, fixture.scan!!.readFailed)

        fixture.reader.failure = IllegalStateException("not an image")
        fixture.vm.onScanRetake()
        fixture.vm.onPhotoTaken("photo.jpg")
        assertEquals(true, fixture.scan!!.readFailed)
        assertEquals(emptySet<Int>(), fixture.scan!!.selected)
    }

    @Test
    fun `confirming writes the trumps, plus 40 for the seven holder, into the field`() = runTest {
        val fixture = ScanFixture()
        fixture.vm.onSevenHolderSelected(fixture.bob.id)

        fixture.scan(fixture.bob, read = setOf(3, 6))
        fixture.vm.onScanTrumpToggled(10)
        assertEquals(59, fixture.scan!!.score)
        assertEquals(true, fixture.scan!!.includesSeven)
        fixture.vm.onScanConfirmed()

        assertNull(fixture.scan)
        assertEquals("59", fixture.content.scores[fixture.bob.id])
        assertEquals(setOf(3, 6, 10), fixture.content.scannedTrumps[fixture.bob.id])
    }

    @Test
    fun `a trump already counted for another player blocks the confirmation`() = runTest {
        val fixture = ScanFixture()
        fixture.scan(fixture.alice, read = setOf(3, 6))
        fixture.vm.onScanConfirmed()

        fixture.scan(fixture.bob, read = setOf(6, 20))
        assertEquals(mapOf(3 to "Alice", 6 to "Alice"), fixture.content.trumpsTakenByOthers(fixture.bob.id))
        fixture.vm.onScanConfirmed()
        assertEquals(setOf(6, 20), fixture.scan!!.selected)
        assertEquals("", fixture.content.scores[fixture.bob.id].orEmpty())

        fixture.vm.onScanTrumpToggled(6)
        fixture.vm.onScanConfirmed()
        assertEquals("20", fixture.content.scores[fixture.bob.id])
    }

    @Test
    fun `cancelling the confirmation changes nothing`() = runTest {
        val fixture = ScanFixture()
        fixture.scan(fixture.alice, read = setOf(3))

        fixture.vm.onScanDismissed()

        assertNull(fixture.scan)
        assertEquals("", fixture.content.scores[fixture.alice.id].orEmpty())
        assertTrue(fixture.content.scannedTrumps.isEmpty())
    }

    @Test
    fun `a score typed by hand forgets the scanned trumps`() = runTest {
        val fixture = ScanFixture()
        fixture.scan(fixture.alice, read = setOf(3))
        fixture.vm.onScanConfirmed()

        fixture.vm.onScoreChanged(fixture.alice.id, "4")

        assertEquals("4", fixture.content.scores[fixture.alice.id])
        assertNull(fixture.content.scannedTrumps[fixture.alice.id])
    }

    @Test
    fun `choosing another seven holder moves the 40 between scanned scores`() = runTest {
        val fixture = ScanFixture()
        fixture.vm.onSevenHolderSelected(fixture.alice.id)
        fixture.scan(fixture.alice, read = setOf(1, 2))
        fixture.vm.onScanConfirmed()
        fixture.scan(fixture.bob, read = setOf(3))
        fixture.vm.onScanConfirmed()
        assertEquals("43", fixture.content.scores[fixture.alice.id])

        fixture.vm.onSevenHolderSelected(fixture.bob.id)

        assertEquals("3", fixture.content.scores[fixture.alice.id])
        assertEquals("43", fixture.content.scores[fixture.bob.id])
        assertEquals(setOf(3), fixture.content.scannedTrumps[fixture.bob.id])
    }

    @Test
    fun `a game without face value cards offers no scan`() = runTest {
        val noCards = object : GameEngine by PapayooEngine() {
            override val faceValueCards: IntRange? = null
        }
        val fixture = ScanFixture(engine = noCards)

        fixture.vm.onScanRequested(fixture.alice.id)

        assertNull(fixture.content.faceValueCards)
        assertNull(withTimeoutOrNull(100) { fixture.vm.photoRequests.first() })
    }
}
