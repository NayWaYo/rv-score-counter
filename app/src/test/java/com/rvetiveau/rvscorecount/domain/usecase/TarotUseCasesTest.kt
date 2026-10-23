package com.rvetiveau.rvscorecount.domain.usecase

import com.rvetiveau.rvscorecount.domain.engine.GameIds
import com.rvetiveau.rvscorecount.domain.model.EndRule
import com.rvetiveau.rvscorecount.domain.model.ParticipantCreationData
import com.rvetiveau.rvscorecount.domain.model.PartyState
import com.rvetiveau.rvscorecount.domain.model.TarotContract
import com.rvetiveau.rvscorecount.domain.model.TarotDealing
import com.rvetiveau.rvscorecount.domain.model.TarotPartyStatus
import com.rvetiveau.rvscorecount.game.belote.BeloteEngine
import com.rvetiveau.rvscorecount.game.free.FreeGameEngine
import com.rvetiveau.rvscorecount.game.tarot.TarotEngine
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Creation, Manches and history of a Partie of Tarot (Story 13.3). */
class TarotUseCasesTest {

    private val rules = TarotEngine()

    private fun seats(vararg names: String) = names.map { ParticipantCreationData(UUID.randomUUID(), it) }

    @Test
    fun `creates a Partie of 3 to 5 Joueurs in table order, the first dealing`() = runTest {
        val store = FakeTarotStore()
        val draft = TarotPartyDraft(EndRule.RoundCount(10), seats(" Alice ", "Bob", "Chloé", "David"))

        val partyId = CreateTarotPartyUseCase(store, rules)(draft).getOrThrow()

        val (party, participants) = store.created.single()
        assertEquals(partyId, party.id)
        assertEquals(GameIds.TAROT, party.gameId)
        assertEquals(PartyState.IN_PROGRESS, party.status)
        assertEquals(0, party.currentDealerIndex)
        assertEquals(EndRule.RoundCount(10), party.endRule)
        assertEquals(listOf("Alice", "Bob", "Chloé", "David"), participants.map { it.name })
        assertEquals(listOf(0, 1, 2, 3), participants.map { it.listPosition })
    }

    @Test
    fun `a draft of the wrong size, with a duplicate or too many Manches is refused`() = runTest {
        val create = CreateTarotPartyUseCase(FakeTarotStore(), rules)
        listOf(
            TarotPartyDraft(null, seats("Alice", "Bob")),
            TarotPartyDraft(null, seats("A", "B", "C", "D", "E", "F")),
            TarotPartyDraft(null, seats("Alice", "Bob", "alice ")),
            TarotPartyDraft(null, seats("Alice", "Bob", " ")),
            TarotPartyDraft(EndRule.RoundCount(100), seats("Alice", "Bob", "Chloé"))
        ).forEach { draft ->
            assertTrue(draft.toString(), create(draft).exceptionOrNull() is InvalidPartyConfigurationException)
        }
        assertTrue(create(TarotPartyDraft(null, seats("A", "B", "C", "D", "E"))).isSuccess)
    }

    @Test
    fun `a Manche is saved with the next dealer, and the last one planned finishes the Partie`() = runTest {
        val store = FakeTarotStore(endRule = EndRule.RoundCount(2))
        val play = PlayTarotRoundUseCase(store, store.tarot, rules)

        assertEquals(TarotPartyStatus.InProgress, play(store.partyId, store.input()).getOrThrow())
        assertEquals(1, store.party.value?.currentDealerIndex)
        assertEquals(1, store.tarot.rounds.value.single().roundNumber)

        // Bob takes and falls: Alice keeps the lead.
        val status = play(store.partyId, store.input(points = 30, taker = store.bob)).getOrThrow()
        assertEquals(TarotPartyStatus.Finished(listOf(store.alice)), status)
        assertEquals(PartyState.FINISHED, store.party.value?.status)
        assertEquals(2, store.party.value?.currentDealerIndex)
    }

    @Test
    fun `without a limit the Partie goes on`() = runTest {
        val store = FakeTarotStore(endRule = null)
        val play = PlayTarotRoundUseCase(store, store.tarot, rules)
        repeat(5) { assertEquals(TarotPartyStatus.InProgress, play(store.partyId, store.input()).getOrThrow()) }
        assertEquals(1, store.party.value?.currentDealerIndex)
    }

    @Test
    fun `an impossible Manche, a finished Partie or a failed write saves nothing`() = runTest {
        val store = FakeTarotStore()
        val play = PlayTarotRoundUseCase(store, store.tarot, rules)

        assertTrue(play(store.partyId, store.input(points = 92)).exceptionOrNull() is InvalidTarotRoundException)
        assertTrue(play(store.partyId, store.input().copy(partnerId = store.bob.id)).exceptionOrNull() is InvalidTarotRoundException)
        store.failWrites = true
        assertTrue(play(store.partyId, store.input()).exceptionOrNull() is TarotRoundPersistenceException)
        store.failWrites = false
        store.party.value = store.party.value?.copy(status = PartyState.FINISHED)
        assertTrue(play(store.partyId, store.input()).exceptionOrNull() is InvalidTarotRoundException)
        assertTrue(store.tarot.rounds.value.isEmpty())
    }

    @Test
    fun `a correction replaces what was entered, keeps the dealer and may finish the Partie`() = runTest {
        val store = FakeTarotStore(endRule = EndRule.RoundCount(1))
        store.addRound(store.input(points = 30))
        store.party.value = store.party.value?.copy(currentDealerIndex = 1)

        val corrected = store.input(points = 60).copy(contract = TarotContract.GARDE)
        val status = CorrectTarotRoundUseCase(store, store.tarot, rules)(store.partyId, 1, corrected).getOrThrow()

        assertEquals(corrected, store.tarot.rounds.value.single().input)
        assertEquals(TarotPartyStatus.Finished(listOf(store.alice)), status)
        assertEquals(PartyState.FINISHED, store.party.value?.status)
        assertEquals(1, store.party.value?.currentDealerIndex)
    }

    @Test
    fun `an unknown Manche cannot be corrected`() = runTest {
        val store = FakeTarotStore()
        val result = CorrectTarotRoundUseCase(store, store.tarot, rules)(store.partyId, 1, store.input())
        assertTrue(result.exceptionOrNull() is InvalidTarotRoundException)
    }

    @Test
    fun `the observed Partie is scored in table order with its dealing`() = runTest {
        val store = FakeTarotStore(playerCount = 5)
        store.addRound(store.input().copy(partnerId = store.chloe.id))

        val state = checkNotNull(ObserveTarotPartyUseCase(store, store.tarot, rules)(store.partyId).first())

        assertEquals(store.seats, state.participants)
        assertEquals(TarotDealing(cardsPerPlayer = 15, dogSize = 3), state.dealing)
        assertTrue(state.callsPartner)
        // Prise made by 0 at 5: Alice +50, Chloé +25, the others −25.
        assertEquals(listOf(50, -25, 25, -25, -25), store.seats.map { state.results.single().points.getValue(it.id) })
        assertEquals(store.alice, state.dealer)
        assertEquals(listOf(store.alice), state.leaders)
    }

    @Test
    fun `an incoherent Partie is refused`() = runTest {
        val gap = FakeTarotStore().apply {
            addRound()
            tarot.rounds.value = tarot.rounds.value.map { it.copy(roundNumber = 2) }
        }
        val stranger = FakeTarotStore().apply { addRound(input().copy(takerId = UUID.randomUUID())) }
        val twoPlayers = FakeTarotStore().apply { participants.value = seats.take(2) }
        listOf(gap, stranger, twoPlayers).forEach { store ->
            val failure = runCatching { ObserveTarotPartyUseCase(store, store.tarot, rules)(store.partyId).first() }.exceptionOrNull()
            assertTrue(failure is IllegalStateException)
        }
    }

    @Test
    fun `replay creates the same Joueurs in the same order with the same end`() = runTest {
        val store = FakeTarotStore(endRule = EndRule.RoundCount(5))
        store.party.value = store.party.value?.copy(status = PartyState.FINISHED)

        CreateTarotPartyUseCase(store, rules).fromParty(store.partyId).getOrThrow()

        val (party, participants) = store.created.single()
        assertEquals(EndRule.RoundCount(5), party.endRule)
        assertEquals(listOf("Alice", "Bob", "Chloé", "David"), participants.map { it.name })
        assertFalse(participants.map { it.id }.any { id -> store.seats.any { it.id == id } })
    }

    @Test
    fun `a Partie in progress cannot be replayed`() = runTest {
        val store = FakeTarotStore()
        assertTrue(CreateTarotPartyUseCase(store, rules).fromParty(store.partyId).isFailure)
    }

    @Test
    fun `the history gives the winners of a finished Partie, none before a Manche`() = runTest {
        val store = FakeTarotStore()
        store.addRound()
        store.party.value = store.party.value?.copy(status = PartyState.FINISHED)
        val history = historyOf(store)

        val summary = history().first().single()
        assertEquals(listOf("Alice"), summary.winners)
        assertEquals(75, summary.winningScore)
        assertEquals(1, summary.roundCount)
        assertTrue(summary.isConsistent)

        store.tarot.rounds.value = emptyList()
        val empty = history().first().single()
        assertEquals(emptyList<String>(), empty.winners)
        assertNull(empty.winningScore)
    }

    @Test
    fun `an incoherent finished Partie is listed without a winner`() = runTest {
        val store = FakeTarotStore()
        store.addRound(store.input().copy(takerId = UUID.randomUUID()))
        store.party.value = store.party.value?.copy(status = PartyState.FINISHED)
        val history = historyOf(store)

        assertFalse(history().first().single().isConsistent)
    }

    private fun historyOf(store: FakeTarotStore) =
        ObservePartyHistoryUseCase(store, FakeBeloteStore(), BeloteEngine(), FakeFreeGameRepository(), FreeGameEngine(), store.tarot, rules)
}
