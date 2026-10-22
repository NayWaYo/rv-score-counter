package com.rvetiveau.rvscorecount.game.free

import com.rvetiveau.rvscorecount.domain.model.EndRule
import com.rvetiveau.rvscorecount.domain.model.FreePartyStatus
import com.rvetiveau.rvscorecount.domain.model.FreeRound
import com.rvetiveau.rvscorecount.domain.model.Participant
import com.rvetiveau.rvscorecount.domain.model.WinDirection
import com.rvetiveau.rvscorecount.domain.model.WinDirection.HIGHEST_WINS
import com.rvetiveau.rvscorecount.domain.model.WinDirection.LOWEST_WINS
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class FreeGameEngineTest {

    private val engine = FreeGameEngine()
    private val partyId = UUID.randomUUID()
    private val alice = Participant(UUID.randomUUID(), partyId, "Alice", 0)
    private val bob = Participant(UUID.randomUUID(), partyId, "Bob", 1)
    private val chloe = Participant(UUID.randomUUID(), partyId, "Chloé", 2)
    private val players = listOf(alice, bob, chloe)

    private fun rounds(vararg scores: Triple<Int, Int, Int>) = scores.mapIndexed { index, (a, b, c) ->
        FreeRound(UUID.randomUUID(), partyId, index + 1, mapOf(alice.id to a, bob.id to b, chloe.id to c))
    }

    private fun ranking(rounds: List<FreeRound>, direction: WinDirection) =
        engine.standings(players, rounds, direction).map { Triple(it.participant.name, it.total, it.rank) }

    @Test
    fun `metadata matches the jeu libre`() {
        assertEquals("free", engine.gameId)
        assertEquals(2, engine.minPlayers)
        assertEquals(10, engine.maxPlayers)
        assertEquals(-99_999..99_999, engine.roundScores)
    }

    @Test
    fun `a manche needs exactly one score per participant, negatives included`() {
        assertTrue(engine.isValidRound(mapOf(alice.id to -12, bob.id to 0, chloe.id to 99_999), players))
        assertFalse(engine.isValidRound(mapOf(alice.id to 1, bob.id to 2), players))
        assertFalse(engine.isValidRound(mapOf(alice.id to 1, bob.id to 2, chloe.id to 3, UUID.randomUUID() to 4), players))
        assertFalse(engine.isValidRound(mapOf(alice.id to 1, bob.id to 2, chloe.id to 100_000), players))
        assertFalse(engine.isValidRound(mapOf(alice.id to 1, bob.id to 2, chloe.id to -100_000), players))
    }

    @Test
    fun `standings follow the win direction, ties share a rank in table order`() {
        val played = rounds(Triple(10, -5, 10), Triple(5, 20, 5))

        assertEquals(
            listOf(Triple("Alice", 15, 1), Triple("Bob", 15, 1), Triple("Chloé", 15, 1)),
            ranking(played, LOWEST_WINS)
        )
        val skyjo = rounds(Triple(12, -3, 20), Triple(8, 4, 1))
        assertEquals(
            listOf(Triple("Bob", 1, 1), Triple("Alice", 20, 2), Triple("Chloé", 21, 3)),
            ranking(skyjo, LOWEST_WINS)
        )
        assertEquals(
            listOf(Triple("Chloé", 21, 1), Triple("Alice", 20, 2), Triple("Bob", 1, 3)),
            ranking(skyjo, HIGHEST_WINS)
        )
    }

    @Test
    fun `before any manche everyone is first with 0`() {
        assertEquals(listOf(1, 1, 1), engine.standings(players, emptyList(), HIGHEST_WINS).map { it.rank })
        assertEquals(players, engine.winners(players, emptyList(), LOWEST_WINS))
    }

    @Test
    fun `a target score ends the partie once a total reaches it, whatever the direction`() {
        val below = rounds(Triple(40, 60, 99))
        val reached = rounds(Triple(40, 60, 99), Triple(10, 0, 1))

        assertEquals(FreePartyStatus.InProgress, engine.partyStatus(players, below, LOWEST_WINS, EndRule.TargetScore(100)))
        // Alice 50, Bob 60, Chloé 100: Chloé ends the Skyjo, Alice wins it.
        assertEquals(
            FreePartyStatus.Finished(listOf(alice)),
            engine.partyStatus(players, reached, LOWEST_WINS, EndRule.TargetScore(100))
        )
        assertEquals(
            FreePartyStatus.Finished(listOf(chloe)),
            engine.partyStatus(players, reached, HIGHEST_WINS, EndRule.TargetScore(100))
        )
    }

    @Test
    fun `a round count ends the partie after its last manche, and no rule never ends it`() {
        val two = rounds(Triple(1, 2, 3), Triple(1, 2, 3))

        assertEquals(FreePartyStatus.InProgress, engine.partyStatus(players, two.take(1), HIGHEST_WINS, EndRule.RoundCount(2)))
        assertEquals(FreePartyStatus.Finished(listOf(chloe)), engine.partyStatus(players, two, HIGHEST_WINS, EndRule.RoundCount(2)))
        assertEquals(FreePartyStatus.InProgress, engine.partyStatus(players, rounds(Triple(500, 0, 0)), HIGHEST_WINS, endRule = null))
    }

    @Test
    fun `a score of someone outside the partie is an inconsistency`() {
        val stranger = FreeRound(UUID.randomUUID(), partyId, 1, mapOf(alice.id to 1, UUID.randomUUID() to 2))

        assertThrows(IllegalArgumentException::class.java) { engine.standings(players, listOf(stranger), LOWEST_WINS) }
    }
}
