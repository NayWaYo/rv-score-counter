package com.rvetiveau.rvscorecount.domain.usecase

import com.rvetiveau.rvscorecount.domain.model.PartyState
import com.rvetiveau.rvscorecount.domain.model.PlayerSummary
import com.rvetiveau.rvscorecount.domain.repository.PlayerSeat
import java.util.UUID
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MergeAndDeletePlayerUseCasesTest {

    private val store = FakeBeloteStore()
    private val val_ = PlayerSummary(UUID.randomUUID(), "Val", finishedParties = 1, lastPlayedAt = 10)
    private val valentin = PlayerSummary(UUID.randomUUID(), "Valentin", finishedParties = 1, lastPlayedAt = 20)
    private val typo = PlayerSummary(UUID.randomUUID(), "Alcie", finishedParties = 0, lastPlayedAt = null)
    private val players = FakePlayerRepository(players = listOf(val_, valentin, typo))
    private val merge = MergePlayersUseCase(players, store)
    private val delete = DeletePlayerUseCase(players)

    private fun seat(playerId: UUID, partyId: UUID) {
        players.seats[playerId] = players.seats[playerId].orEmpty() + PlayerSeat(partyId, UUID.randomUUID())
    }

    @Test
    fun `two players who never sat together are merged into the kept one`() = runTest {
        seat(val_.id, store.seedParty(status = PartyState.FINISHED).id)
        seat(valentin.id, store.seedParty(status = PartyState.FINISHED).id)

        assertEquals(MergePlayersOutcome.Merged, merge(val_.id, valentin.id))
        assertEquals(listOf(val_.id to valentin.id), players.merges)
    }

    @Test
    fun `players who sat at the same party are refused, naming its oldest one`() = runTest {
        val shared = store.seedParty(status = PartyState.FINISHED)
        seat(val_.id, shared.id)
        seat(valentin.id, shared.id)

        assertEquals(MergePlayersOutcome.PlayedTogether("belote", shared.createdAt), merge(val_.id, valentin.id))
        assertTrue(players.merges.isEmpty())
    }

    @Test
    fun `a player cannot be merged with themselves or an unknown one`() = runTest {
        assertEquals(MergePlayersOutcome.SamePlayer, merge(val_.id, val_.id))
        assertEquals(MergePlayersOutcome.UnknownPlayer, merge(val_.id, UUID.randomUUID()))
        assertTrue(players.merges.isEmpty())
    }

    @Test
    fun `only a player without any seat can be deleted`() = runTest {
        seat(val_.id, store.seedParty().id)

        assertEquals(DeletePlayerOutcome.HasParties, delete(val_.id))
        assertEquals(DeletePlayerOutcome.Deleted, delete(typo.id))
        assertEquals(DeletePlayerOutcome.UnknownPlayer, delete(UUID.randomUUID()))
        assertEquals(listOf("Val", "Valentin"), players.players.value.map { it.name })
    }
}
