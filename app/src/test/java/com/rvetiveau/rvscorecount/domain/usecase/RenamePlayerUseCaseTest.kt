package com.rvetiveau.rvscorecount.domain.usecase

import com.rvetiveau.rvscorecount.domain.model.PlayerSummary
import com.rvetiveau.rvscorecount.domain.model.inPlayOrder
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RenamePlayerUseCaseTest {

    private val alice = PlayerSummary(UUID.randomUUID(), "Alice", finishedParties = 3, lastPlayedAt = 20)
    private val bob = PlayerSummary(UUID.randomUUID(), "Bob", finishedParties = 1, lastPlayedAt = 10)
    private val repository = FakePlayerRepository(players = listOf(alice, bob))
    private val rename = RenamePlayerUseCase(repository)

    private suspend fun names() = repository.observePlayers().first().map { it.name }

    @Test
    fun `renames the player, trimmed`() = runTest {
        assertEquals(RenamePlayerOutcome.Renamed, rename(alice.id, "  Alicia "))
        assertEquals(listOf("Alicia", "Bob"), names())
    }

    @Test
    fun `a player may change only the case or spaces of their own name`() = runTest {
        assertEquals(RenamePlayerOutcome.Renamed, rename(alice.id, "ALICE"))
        assertEquals(listOf("ALICE", "Bob"), names())
    }

    @Test
    fun `a blank name or another player's name is refused and nothing changes`() = runTest {
        assertEquals(RenamePlayerOutcome.BlankName, rename(alice.id, "   "))
        assertEquals(RenamePlayerOutcome.NameTaken("Bob"), rename(alice.id, " bob"))
        assertEquals(listOf("Alice", "Bob"), names())
    }

    @Test
    fun `an unknown player is reported`() = runTest {
        assertEquals(RenamePlayerOutcome.UnknownPlayer, rename(UUID.randomUUID(), "Zoé"))
    }

    @Test
    fun `players are listed most recently played first, those without any partie last by name`() {
        val zoe = PlayerSummary(UUID.randomUUID(), "zoé", 0, lastPlayedAt = null)
        val denis = PlayerSummary(UUID.randomUUID(), "Denis", 2, lastPlayedAt = null)

        assertEquals(listOf(alice, bob, denis, zoe), listOf(zoe, bob, denis, alice).inPlayOrder())
    }
}
