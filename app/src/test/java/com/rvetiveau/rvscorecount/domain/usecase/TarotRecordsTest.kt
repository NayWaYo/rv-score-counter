package com.rvetiveau.rvscorecount.domain.usecase

import com.rvetiveau.rvscorecount.domain.engine.GameIds
import com.rvetiveau.rvscorecount.domain.model.EndRule
import com.rvetiveau.rvscorecount.domain.model.Participant
import com.rvetiveau.rvscorecount.domain.model.Party
import com.rvetiveau.rvscorecount.domain.model.PartyState
import com.rvetiveau.rvscorecount.domain.model.PlayerSummary
import com.rvetiveau.rvscorecount.domain.model.TakerStat
import com.rvetiveau.rvscorecount.domain.model.TarotContract
import com.rvetiveau.rvscorecount.domain.model.TarotPlay
import com.rvetiveau.rvscorecount.domain.model.TarotRecords
import com.rvetiveau.rvscorecount.domain.model.TarotRound
import com.rvetiveau.rvscorecount.domain.model.TarotRoundInput
import com.rvetiveau.rvscorecount.domain.model.WinRecord
import com.rvetiveau.rvscorecount.domain.model.tarotRecords
import com.rvetiveau.rvscorecount.domain.repository.PlayerSeat
import com.rvetiveau.rvscorecount.game.belote.BeloteEngine
import com.rvetiveau.rvscorecount.game.free.FreeGameEngine
import com.rvetiveau.rvscorecount.game.papayoo.PapayooEngine
import com.rvetiveau.rvscorecount.game.tarot.TarotEngine
import java.util.UUID
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The Tarot's taker ranking and a Joueur's Tarot record (Story 13.4). */
class TarotRecordsTest {

    private val rules = TarotEngine()
    private val partyId = UUID.randomUUID()
    private val alice = Participant(UUID.randomUUID(), partyId, "Alice", 0)
    private val bob = Participant(UUID.randomUUID(), partyId, "Bob", 1)
    private val chloe = Participant(UUID.randomUUID(), partyId, "Chloé", 2)
    private val seats = listOf(alice, bob, chloe)

    private fun take(taker: Participant, contract: TarotContract, points: Int) =
        TarotRoundInput(taker.id, contract, attackBouts = 2, attackPoints = points)

    /** A finished Partie of Tarot at 3 with [inputs] as its Manches, in order. */
    private fun state(vararg inputs: TarotRoundInput) = assembleTarotParty(
        party = Party(partyId, GameIds.TAROT, PartyState.FINISHED, 0, 1, endRule = null),
        participants = seats,
        rounds = inputs.mapIndexed { index, input -> TarotRound(UUID.randomUUID(), partyId, index + 1, input) },
        rules = rules
    )

    @Test
    fun `takers are ranked by takes, then contracts made, then table order`() {
        val ranking = state(
            take(bob, TarotContract.PRISE, 30),
            take(chloe, TarotContract.GARDE, 50),
            take(bob, TarotContract.PRISE, 50),
            take(chloe, TarotContract.PRISE, 50)
        ).takerRanking()

        assertEquals(
            listOf(TakerStat(chloe, 2, 2), TakerStat(bob, 2, 1), TakerStat(alice, 0, 0)),
            ranking
        )
        assertEquals(emptyList<TakerStat>(), state().takerRanking())
    }

    @Test
    fun `the record counts wins, takes, contracts made and the contract taken most`() {
        // Alice wins the first Partie, Bob the second; Alice takes three times and falls once.
        val first = TarotPlay(state(take(alice, TarotContract.GARDE, 50), take(alice, TarotContract.PRISE, 30)), alice.id)
        val second = TarotPlay(state(take(bob, TarotContract.GARDE_SANS, 60), take(alice, TarotContract.PRISE, 45)), alice.id)

        assertEquals(
            TarotRecords(WinRecord(1, 2), takes = 3, made = 2, favoriteContract = TarotContract.PRISE),
            tarotRecords(listOf(first, second))
        )
    }

    @Test
    fun `contracts taken equally often go to the highest one`() {
        val play = TarotPlay(state(take(alice, TarotContract.PRISE, 50), take(alice, TarotContract.GARDE_CONTRE, 50)), alice.id)
        assertEquals(TarotContract.GARDE_CONTRE, tarotRecords(listOf(play))?.favoriteContract)
    }

    @Test
    fun `a Partie without a Manche counts without a win, and no take leaves no favourite`() {
        val records = checkNotNull(tarotRecords(listOf(TarotPlay(state(), alice.id))))
        assertEquals(WinRecord(0, 1), records.wins)
        assertEquals(0, records.takes)
        assertNull(records.favoriteContract)
        assertNull(records.contractRate)
        assertNull(tarotRecords(emptyList()))
    }

    @Test
    fun `the profile reads finished Parties of Tarot and leaves an incoherent one out`() = runTest {
        val store = FakeTarotStore(endRule = EndRule.RoundCount(1))
        store.addRound()
        store.party.value = store.party.value?.copy(status = PartyState.FINISHED)
        val aliceId = UUID.randomUUID()
        val players = FakePlayerRepository(players = listOf(PlayerSummary(aliceId, "Alice", finishedParties = 1, lastPlayedAt = 0)))
        players.seats[aliceId] = listOf(PlayerSeat(store.partyId, store.alice.id))
        val records = GetPlayerRecordsUseCase(
            players, store, FakeBeloteStore(), BeloteEngine(), mapOf(PapayooEngine.GAME_ID to PapayooEngine()),
            FakeFreeGameRepository(), FreeGameEngine(), store.tarot, rules
        )

        assertEquals(
            TarotRecords(WinRecord(1, 1), takes = 1, made = 1, favoriteContract = TarotContract.PRISE),
            records(aliceId)?.tarot
        )

        store.addRound(store.input().copy(takerId = UUID.randomUUID()))
        assertNull(records(aliceId)?.tarot)
    }
}
