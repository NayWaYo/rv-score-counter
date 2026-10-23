package com.rvetiveau.rvscorecount.domain.usecase

import com.rvetiveau.rvscorecount.game.tarot.TarotEngine
import com.rvetiveau.rvscorecount.domain.model.EndRule
import com.rvetiveau.rvscorecount.domain.model.FreeGameRecord
import com.rvetiveau.rvscorecount.domain.model.FreeRound
import com.rvetiveau.rvscorecount.domain.model.Participant
import com.rvetiveau.rvscorecount.domain.model.Party
import com.rvetiveau.rvscorecount.domain.model.PartyState
import com.rvetiveau.rvscorecount.domain.model.PlayerSummary
import com.rvetiveau.rvscorecount.domain.model.WinDirection
import com.rvetiveau.rvscorecount.domain.model.WinRecord
import com.rvetiveau.rvscorecount.domain.repository.PlayerSeat
import com.rvetiveau.rvscorecount.game.belote.BeloteEngine
import com.rvetiveau.rvscorecount.game.free.FreeGameEngine
import com.rvetiveau.rvscorecount.game.papayoo.PapayooEngine
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The Jeu libre in a Joueur's profile, the Historique and « Rejouer » (Story 11.4). */
class FreeRecordsAndReplayTest {

    private val rules = FreeGameEngine()

    /** Several finished Parties of Jeu libre, Alice sitting at each, linked to her Joueur. */
    private class Store : PartyRepositoryAdapter() {
        val parties = mutableListOf<Party>()
        val participants = mutableListOf<Participant>()
        val created = mutableListOf<Pair<Party, List<Participant>>>()
        val free = FakeFreeGameRepository()

        override suspend fun getPartyById(id: UUID): Party? = parties.firstOrNull { it.id == id }
        override suspend fun getParticipantsByPartyId(partyId: UUID): List<Participant> = participants.filter { it.partyId == partyId }
        override fun observeFinishedParties(): Flow<List<Party>> = MutableStateFlow(parties.filter { it.status == PartyState.FINISHED })
        override suspend fun createPartyWithParticipants(party: Party, participants: List<Participant>) {
            created += party to participants
        }
    }

    private val store = Store()
    private val aliceId = UUID.randomUUID()
    private val players = FakePlayerRepository(players = listOf(PlayerSummary(aliceId, "Alice", finishedParties = 1, lastPlayedAt = 0)))

    /** A finished Partie of [gameName] where Alice scores [alice] and Bob [bob] in one Manche (none when null). */
    private fun seed(
        gameName: String,
        alice: Int?,
        bob: Int = 10,
        createdAt: Long = 0,
        direction: WinDirection = WinDirection.LOWEST_WINS,
        status: PartyState = PartyState.FINISHED
    ): Party {
        val party = Party(UUID.randomUUID(), "free", status, 0, createdAt, EndRule.RoundCount(1), gameName, direction)
        val aliceSeat = Participant(UUID.randomUUID(), party.id, "Alice", 0)
        val bobSeat = Participant(UUID.randomUUID(), party.id, "Bob", 1)
        store.parties += party
        store.participants += listOf(bobSeat, aliceSeat)
        if (alice != null) {
            store.free.rounds.value += FreeRound(UUID.randomUUID(), party.id, 1, mapOf(aliceSeat.id to alice, bobSeat.id to bob))
        }
        players.seats[aliceId] = players.seats[aliceId].orEmpty() + PlayerSeat(party.id, aliceSeat.id)
        return party
    }

    private suspend fun records() = checkNotNull(
        GetPlayerRecordsUseCase(
            players, store, FakeBeloteStore(), BeloteEngine(), mapOf(PapayooEngine.GAME_ID to PapayooEngine()), store.free, rules
        )(aliceId)
    ).free

    @Test
    fun `the profile counts every game together, then each game from the most played`() = runTest {
        seed("Skyjo", alice = 5, createdAt = 1)
        seed("skyjo ", alice = 30, createdAt = 2)
        seed(" Skyjo", alice = 10, createdAt = 3)
        seed("Yams", alice = 50, createdAt = 4, direction = WinDirection.HIGHEST_WINS)
        seed("Uno", alice = 10, createdAt = 5)

        val free = checkNotNull(records())

        assertEquals(WinRecord(wins = 4, parties = 5), free.wins)
        assertEquals(
            listOf(
                FreeGameRecord("Skyjo", WinRecord(2, 3)),
                FreeGameRecord("Uno", WinRecord(1, 1)),
                FreeGameRecord("Yams", WinRecord(1, 1))
            ),
            free.byGame
        )
    }

    @Test
    fun `a partie ended before its first manche counts without a win, one in progress does not count`() = runTest {
        seed("Skyjo", alice = null)
        seed("Skyjo", alice = 1, status = PartyState.IN_PROGRESS)

        assertEquals(WinRecord(wins = 0, parties = 1), records()?.wins)
    }

    @Test
    fun `without any partie of jeu libre the section is absent`() = runTest {
        assertNull(records())
    }

    @Test
    fun `the history names the game played`() = runTest {
        seed("Skyjo", alice = 5)

        val history = ObservePartyHistoryUseCase(store, FakeBeloteStore(), BeloteEngine(), store.free, rules, FakeTarotRepository(), TarotEngine())
        val summary = history().first().single()

        assertEquals("Skyjo", summary.gameName)
        assertEquals(listOf("Alice"), summary.winners)
    }

    @Test
    fun `replay creates the same game with the same rules and players in the same order`() = runTest {
        val reference = seed("Yams", alice = 5, direction = WinDirection.HIGHEST_WINS)

        val newId = CreateFreePartyUseCase(store, rules).fromParty(reference.id).getOrThrow()

        val (party, seats) = store.created.single()
        assertEquals(newId, party.id)
        assertEquals(PartyState.IN_PROGRESS, party.status)
        assertEquals("Yams", party.gameName)
        assertEquals(WinDirection.HIGHEST_WINS, party.winDirection)
        assertEquals(EndRule.RoundCount(1), party.endRule)
        assertEquals(0, party.currentDealerIndex)
        assertEquals(listOf("Alice" to 0, "Bob" to 1), seats.map { it.name to it.listPosition })
    }

    @Test
    fun `replay refuses a partie in progress`() = runTest {
        val running = seed("Skyjo", alice = 1, status = PartyState.IN_PROGRESS)

        val result = CreateFreePartyUseCase(store, rules).fromParty(running.id)

        assertTrue(result.exceptionOrNull() is InvalidPartyConfigurationException)
        assertTrue(store.created.isEmpty())
    }
}
