package com.rvetiveau.rvscorecount.data.repository

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.rvetiveau.rvscorecount.data.db.AppDatabase
import com.rvetiveau.rvscorecount.domain.model.Chelem
import com.rvetiveau.rvscorecount.domain.model.EndRule
import com.rvetiveau.rvscorecount.domain.model.Participant
import com.rvetiveau.rvscorecount.domain.model.Party
import com.rvetiveau.rvscorecount.domain.model.PartyState
import com.rvetiveau.rvscorecount.domain.model.Poignee
import com.rvetiveau.rvscorecount.domain.model.TarotContract
import com.rvetiveau.rvscorecount.domain.model.TarotRound
import com.rvetiveau.rvscorecount.domain.model.TarotRoundInput
import com.rvetiveau.rvscorecount.domain.model.TarotSide
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** The Manches of the Tarot (Story 13.2) are written with their Partie in one transaction, or not at all. */
class TarotRepositoryImplTest {

    private lateinit var database: AppDatabase
    private lateinit var parties: PartyRepositoryImpl
    private lateinit var repository: TarotRepositoryImpl

    private val party = Party(UUID.randomUUID(), "tarot", PartyState.IN_PROGRESS, 0, 1_000, endRule = EndRule.RoundCount(2))
    private val players = listOf("Alice", "Bob", "Chloé", "David", "Eve").mapIndexed { index, name ->
        Participant(UUID.randomUUID(), party.id, name, index)
    }
    private val alice = players[0]
    private val chloe = players[2]

    private fun round(number: Int, input: TarotRoundInput) = TarotRound(UUID.randomUUID(), party.id, number, input)

    private val first = round(
        1,
        TarotRoundInput(
            alice.id, TarotContract.GARDE, attackBouts = 2, attackPoints = 50, partnerId = chloe.id,
            petitAuBout = TarotSide.ATTACK, attackPoignee = Poignee.SIMPLE, chelem = Chelem.UNANNOUNCED_MADE
        )
    )

    @Before
    fun setUp() = runTest {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        parties = PartyRepositoryImpl(
            appDatabase = database,
            partyDao = database.partyDao(),
            participantDao = database.participantDao(),
            roundDao = database.roundDao(),
            roundScoreDao = database.roundScoreDao(),
            playerDao = database.playerDao()
        )
        repository = TarotRepositoryImpl(database, database.partyDao(), database.participantDao(), database.tarotRoundDao())
        parties.createPartyWithParticipants(party, players)
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun storedParty() = checkNotNull(parties.getPartyById(party.id))

    @Test
    fun savesAMancheWithTheNextDealerAndReadsItBackInOrder() = runTest {
        val second = round(2, TarotRoundInput(players[1].id, TarotContract.PRISE, attackBouts = 0, attackPoints = 40))
        repository.saveRound(first, party.copy(currentDealerIndex = 1))
        repository.saveRound(second, party.copy(currentDealerIndex = 2))

        assertEquals(listOf(first, second), repository.getRounds(party.id))
        assertEquals(listOf(first, second), repository.observeRounds(party.id).first())
        assertEquals(2, storedParty().currentDealerIndex)
    }

    @Test
    fun correctsWhatWasEnteredAndFinishesThePartyTogether() = runTest {
        repository.saveRound(first, party.copy(currentDealerIndex = 1))
        val corrected = first.copy(input = first.input.copy(contract = TarotContract.GARDE_SANS, partnerId = null, chelem = null))

        repository.correctRound(corrected, party.copy(currentDealerIndex = 1, status = PartyState.FINISHED))

        assertEquals(listOf(corrected), repository.getRounds(party.id))
        assertEquals(PartyState.FINISHED, storedParty().status)
    }

    @Test
    fun aRefusedWriteChangesNothing() = runTest {
        val stranger = UUID.randomUUID()
        val refused = listOf(
            round(1, first.input.copy(takerId = stranger)),
            round(1, first.input.copy(partnerId = stranger))
        )
        refused.forEach { round ->
            assertTrue(runCatching { repository.saveRound(round, party.copy(currentDealerIndex = 1)) }.isFailure)
        }
        assertEquals(emptyList<TarotRound>(), repository.getRounds(party.id))
        assertEquals(0, storedParty().currentDealerIndex)

        // The same number twice is refused by the unique index: the Partie is not updated either.
        repository.saveRound(first, party.copy(currentDealerIndex = 1))
        assertTrue(runCatching { repository.saveRound(first.copy(id = UUID.randomUUID()), party.copy(currentDealerIndex = 2)) }.isFailure)
        assertEquals(1, storedParty().currentDealerIndex)
    }

    @Test
    fun onlyATarotPartyTakesTarotManches() = runTest {
        val papayoo = Party(UUID.randomUUID(), "papayoo", PartyState.IN_PROGRESS, 0, 2_000)
        val seat = Participant(UUID.randomUUID(), papayoo.id, "Alice", 0)
        parties.createPartyWithParticipants(papayoo, listOf(seat))

        val misplaced = TarotRound(UUID.randomUUID(), papayoo.id, 1, first.input.copy(takerId = seat.id, partnerId = null))
        assertTrue(runCatching { repository.saveRound(misplaced, papayoo.copy(currentDealerIndex = 1)) }.isFailure)
        assertEquals(emptyList<TarotRound>(), repository.getRounds(papayoo.id))
    }

    @Test
    fun aFinishedPartyOrAnotherNumberCannotBeCorrected() = runTest {
        repository.saveRound(first, party.copy(currentDealerIndex = 1, status = PartyState.FINISHED))

        assertTrue(runCatching { repository.correctRound(first, party.copy(currentDealerIndex = 1)) }.isFailure)
        parties.updateParty(party.copy(currentDealerIndex = 1))
        assertTrue(runCatching { repository.correctRound(first.copy(roundNumber = 2), party.copy(currentDealerIndex = 1)) }.isFailure)
        assertEquals(listOf(first), repository.getRounds(party.id))
    }

    @Test
    fun deletingThePartyDeletesItsManches() = runTest {
        repository.saveRound(first, party.copy(currentDealerIndex = 1))
        parties.deleteParty(party.id)
        assertEquals(emptyList<TarotRound>(), repository.getRounds(party.id))
    }
}
