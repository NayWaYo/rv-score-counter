package com.rvetiveau.rvscorecount.domain.usecase

import com.rvetiveau.rvscorecount.domain.model.EndRule
import com.rvetiveau.rvscorecount.domain.model.BeloteRound
import com.rvetiveau.rvscorecount.domain.model.BeloteRoundInput
import com.rvetiveau.rvscorecount.domain.model.PartyState
import com.rvetiveau.rvscorecount.domain.model.PlayerSummary
import com.rvetiveau.rvscorecount.domain.model.TeamSide
import com.rvetiveau.rvscorecount.domain.model.WinRecord
import com.rvetiveau.rvscorecount.domain.repository.PlayerSeat
import com.rvetiveau.rvscorecount.game.belote.BeloteEngine
import com.rvetiveau.rvscorecount.game.free.FreeGameEngine
import com.rvetiveau.rvscorecount.game.tarot.TarotEngine
import com.rvetiveau.rvscorecount.game.papayoo.PapayooEngine
import java.util.UUID
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Which Parties a profile reads (Story 10.3); the figures themselves are in PlayerRecordsTest. */
class GetPlayerRecordsUseCaseTest {

    private val store = FakeBeloteStore()
    private val playerIds = listOf("Alice", "Bob", "Chloé", "David").associateWith { UUID.randomUUID() }
    private val players = FakePlayerRepository(
        players = playerIds.map { (name, id) -> PlayerSummary(id, name, finishedParties = 1, lastPlayedAt = 0) }
    )
    private val useCase = GetPlayerRecordsUseCase(
        players, store, store, BeloteEngine(), mapOf(PapayooEngine.GAME_ID to PapayooEngine()), FakeFreeGameRepository(), FreeGameEngine(),
        FakeTarotRepository(), TarotEngine()
    )

    /** A one-Manche Partie of Belote that Alice's Équipe wins as takers, linked to the Joueurs. */
    private fun seedBelote(status: PartyState): UUID {
        val party = store.seedParty(EndRule.RoundCount(1), status)
        val alice = store.takerOf(party.id, TeamSide.FIRST)
        store.rounds.value += BeloteRound(UUID.randomUUID(), party.id, 1, BeloteRoundInput(TeamSide.FIRST, 100), alice)
        val seats = store.participants.value.filter { it.partyId == party.id }
        players.playerIdsBySeat[party.id] = seats.associate { it.id to playerIds.getValue(it.name) }
        val aliceId = playerIds.getValue("Alice")
        players.seats[aliceId] = players.seats[aliceId].orEmpty() + PlayerSeat(party.id, alice)
        return party.id
    }

    @Test
    fun `only finished and consistent parties count`() = runTest {
        seedBelote(PartyState.FINISHED)
        seedBelote(PartyState.IN_PROGRESS)
        val broken = seedBelote(PartyState.FINISHED)
        store.teams.value = store.teams.value.filterNot { it.partyId == broken && it.side == TeamSide.SECOND }

        val records = checkNotNull(useCase(playerIds.getValue("Alice")))

        assertEquals("Alice", records.player.name)
        assertNull(records.papayoo)
        assertEquals(WinRecord(1, 1), records.belote?.wins)
        assertEquals(1, records.belote?.made)
        assertEquals("Chloé", records.belote?.bestTeammate?.name)
    }

    @Test
    fun `a player without any finished party has no section, an unknown one no profile`() = runTest {
        seedBelote(PartyState.IN_PROGRESS)

        val records = checkNotNull(useCase(playerIds.getValue("Alice")))

        assertNull(records.papayoo)
        assertNull(records.belote)
        assertNull(useCase(UUID.randomUUID()))
    }
}
