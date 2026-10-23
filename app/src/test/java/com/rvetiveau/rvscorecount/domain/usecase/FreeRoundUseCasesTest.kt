package com.rvetiveau.rvscorecount.domain.usecase

import com.rvetiveau.rvscorecount.game.tarot.TarotEngine
import com.rvetiveau.rvscorecount.domain.model.EndRule
import com.rvetiveau.rvscorecount.domain.model.FreePartyStatus
import com.rvetiveau.rvscorecount.domain.model.PartyState
import com.rvetiveau.rvscorecount.domain.model.WinDirection
import com.rvetiveau.rvscorecount.game.belote.BeloteEngine
import com.rvetiveau.rvscorecount.game.free.FreeGameEngine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Observation, saisie and correction of the Manches of a Partie of Jeu libre (Story 11.3). */
class FreeRoundUseCasesTest {

    private val rules = FreeGameEngine()

    private fun play(store: FakeFreeStore) = PlayFreeRoundUseCase(store, store.free, rules)
    private fun correct(store: FakeFreeStore) = CorrectFreeRoundUseCase(store, store.free, rules)
    private fun observe(store: FakeFreeStore) = ObserveFreePartyUseCase(store, store.free, rules)

    @Test
    fun `the partie is observed in table order, ranked by its win direction`() = runTest {
        val store = FakeFreeStore(winDirection = WinDirection.HIGHEST_WINS)
        store.addRound(alicePoints = 10, bobPoints = -3, chloePoints = 10)

        val state = checkNotNull(observe(store)(store.partyId).first())

        assertEquals(listOf("Alice", "Bob", "Chloé"), state.participants.map { it.name })
        assertEquals(listOf("Alice" to 1, "Chloé" to 1, "Bob" to 3), state.standings.map { it.participant.name to it.rank })
        assertEquals(listOf("Alice", "Chloé"), state.leaders.map { it.name })
        assertEquals("Alice", state.dealer.name)
        assertEquals(FreePartyStatus.InProgress, state.status)
    }

    @Test
    fun `a deleted partie is observed as null and a gap in its manches fails the flow`() = runTest {
        val store = FakeFreeStore()
        store.addRound(1, 2, 3)
        store.free.rounds.value = store.free.rounds.value.map { it.copy(roundNumber = 2) }

        assertTrue(runCatching { observe(store)(store.partyId).first() }.isFailure)

        store.party.value = null
        assertNull(observe(store)(store.partyId).first())
    }

    @Test
    fun `a manche with negative scores is saved and the next player deals`() = runTest {
        val store = FakeFreeStore()

        val status = play(store)(store.partyId, store.scores(-5, 12, 0)).getOrThrow()

        assertEquals(FreePartyStatus.InProgress, status)
        assertEquals(store.scores(-5, 12, 0), store.free.rounds.value.single().scores)
        assertEquals(1, store.free.rounds.value.single().roundNumber)
        assertEquals(1, store.party.value?.currentDealerIndex)
        assertEquals(PartyState.IN_PROGRESS, store.party.value?.status)
    }

    @Test
    fun `the dealer goes back to the first player after the last one`() = runTest {
        val store = FakeFreeStore()
        store.party.value = store.party.value?.copy(currentDealerIndex = 2)

        play(store)(store.partyId, store.scores(1, 2, 3)).getOrThrow()

        assertEquals(0, store.party.value?.currentDealerIndex)
    }

    @Test
    fun `the manche reaching the target score finishes the partie in the same write`() = runTest {
        val store = FakeFreeStore(endRule = EndRule.TargetScore(100))
        store.addRound(alicePoints = 90, bobPoints = 10, chloePoints = 20)

        val status = play(store)(store.partyId, store.scores(15, 5, 5)).getOrThrow()

        assertEquals(FreePartyStatus.Finished(listOf(store.bob)), status)
        assertEquals(PartyState.FINISHED, store.party.value?.status)
        assertEquals(PartyState.FINISHED, store.free.updatedParties.single().status)
    }

    @Test
    fun `the last manche of the count finishes the partie`() = runTest {
        val store = FakeFreeStore(endRule = EndRule.RoundCount(2), winDirection = WinDirection.HIGHEST_WINS)
        store.addRound(1, 2, 3)

        val status = play(store)(store.partyId, store.scores(10, 0, 0)).getOrThrow()

        assertEquals(FreePartyStatus.Finished(listOf(store.alice)), status)
        assertEquals(PartyState.FINISHED, store.party.value?.status)
    }

    @Test
    fun `a partie ended by hand never finishes by itself`() = runTest {
        val store = FakeFreeStore(endRule = null)

        val status = play(store)(store.partyId, store.scores(99_999, 99_999, 99_999)).getOrThrow()

        assertEquals(FreePartyStatus.InProgress, status)
        assertEquals(PartyState.IN_PROGRESS, store.party.value?.status)
    }

    @Test
    fun `a manche missing a player or beyond the bounds is refused and nothing is written`() = runTest {
        val store = FakeFreeStore()

        val missing = play(store)(store.partyId, mapOf(store.alice.id to 1, store.bob.id to 2))
        val tooBig = play(store)(store.partyId, store.scores(100_000, 0, 0))

        assertTrue(missing.exceptionOrNull() is InvalidFreeRoundException)
        assertTrue(tooBig.exceptionOrNull() is InvalidFreeRoundException)
        assertTrue(store.free.rounds.value.isEmpty())
        assertEquals(0, store.party.value?.currentDealerIndex)
    }

    @Test
    fun `a finished partie takes no more manches`() = runTest {
        val store = FakeFreeStore()
        store.party.value = store.party.value?.copy(status = PartyState.FINISHED)

        val result = play(store)(store.partyId, store.scores(1, 2, 3))

        assertTrue(result.exceptionOrNull() is InvalidFreeRoundException)
        assertTrue(store.free.rounds.value.isEmpty())
    }

    @Test
    fun `a failed write is reported as a persistence failure`() = runTest {
        val store = FakeFreeStore()
        store.failWrites = true

        val result = play(store)(store.partyId, store.scores(1, 2, 3))

        assertTrue(result.exceptionOrNull() is FreeRoundPersistenceException)
    }

    @Test
    fun `a correction replaces the scores and leaves the dealer`() = runTest {
        val store = FakeFreeStore()
        play(store)(store.partyId, store.scores(1, 2, 3)).getOrThrow()

        val status = correct(store)(store.partyId, 1, store.scores(-1, 20, 3)).getOrThrow()

        assertEquals(FreePartyStatus.InProgress, status)
        assertEquals(store.scores(-1, 20, 3), store.free.rounds.value.single().scores)
        assertEquals(1, store.party.value?.currentDealerIndex)
    }

    @Test
    fun `a correction reaching the target score finishes the partie`() = runTest {
        val store = FakeFreeStore(endRule = EndRule.TargetScore(50))
        store.addRound(10, 10, 10)

        val status = correct(store)(store.partyId, 1, store.scores(10, 60, 10)).getOrThrow()

        assertEquals(FreePartyStatus.Finished(listOf(store.alice, store.chloe)), status)
        assertEquals(PartyState.FINISHED, store.party.value?.status)
    }

    @Test
    fun `a correction of a manche not played, or with a missing score, is refused`() = runTest {
        val store = FakeFreeStore()
        store.addRound(1, 2, 3)

        val unknown = correct(store)(store.partyId, 2, store.scores(1, 2, 3))
        val partial = correct(store)(store.partyId, 1, mapOf(store.alice.id to 5))

        assertTrue(unknown.exceptionOrNull() is InvalidFreeRoundException)
        assertTrue(partial.exceptionOrNull() is InvalidFreeRoundException)
        assertEquals(store.scores(1, 2, 3), store.free.rounds.value.single().scores)
    }

    private fun history(store: FakeFreeStore) =
        ObservePartyHistoryUseCase(store, FakeBeloteStore(), BeloteEngine(), store.free, rules, FakeTarotRepository(), TarotEngine())

    @Test
    fun `the history sums up a finished partie by its winners for its win direction`() = runTest {
        val store = FakeFreeStore(endRule = EndRule.RoundCount(1), winDirection = WinDirection.HIGHEST_WINS)
        play(store)(store.partyId, store.scores(-4, 30, 30)).getOrThrow()

        val summary = history(store)().first().single()

        assertEquals(listOf("Bob", "Chloé"), summary.winners)
        assertEquals(30, summary.winningScore)
        assertEquals(1, summary.roundCount)
        assertTrue(summary.isConsistent)
    }

    @Test
    fun `a partie ended by hand before any manche has no winner in the history`() = runTest {
        val store = FakeFreeStore(endRule = null)
        ClosePartyUseCase(store)(store.partyId).getOrThrow()

        val summary = history(store)().first().single()

        assertTrue(summary.winners.isEmpty())
        assertNull(summary.winningScore)
        assertEquals(0, summary.roundCount)
    }
}
