package com.rvetiveau.rvscorecount.domain.model

import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerStatsTest {

    private val partyId = UUID.randomUUID()
    private val alice = Participant(UUID.randomUUID(), partyId, "Alice", 0)
    private val bob = Participant(UUID.randomUUID(), partyId, "Bob", 1)
    private val zoe = Participant(UUID.randomUUID(), partyId, "Zoé", 2)

    private fun round(number: Int, sevenHolder: Participant? = null) =
        Round(UUID.randomUUID(), partyId, number, CardSuit.HEART, sevenHolderId = sevenHolder?.id)

    private fun scores(round: Round, vararg points: Pair<Participant, Int>) =
        round to points.map { (participant, value) -> RoundScore(round.id, participant.id, value) }

    @Test
    fun `computes average, zero rounds, sevens and the best and worst rounds`() {
        val r1 = round(1, sevenHolder = bob)
        val r2 = round(2, sevenHolder = alice)
        val r3 = round(3, sevenHolder = bob)
        val stats = playerStats(
            listOf(alice, bob),
            mapOf(
                scores(r3, alice to 0, bob to 250),
                scores(r1, alice to 100, bob to 150),
                scores(r2, alice to 175, bob to 75)
            )
        )

        val aliceStats = stats.getValue(alice.id)
        assertEquals(3, aliceStats.roundsPlayed)
        assertEquals(91.666, aliceStats.averagePoints, 0.001)
        assertEquals(1, aliceStats.zeroRounds)
        assertEquals(1, aliceStats.sevensTaken)
        assertEquals(RoundPoints(0, 3), aliceStats.best)
        assertEquals(RoundPoints(175, 2), aliceStats.worst)
        assertEquals(2, stats.getValue(bob.id).sevensTaken)
    }

    @Test
    fun `a tie keeps the earliest round`() {
        val r1 = round(1)
        val r2 = round(2)
        val stats = playerStats(listOf(alice), mapOf(scores(r2, alice to 50), scores(r1, alice to 50)))

        assertEquals(RoundPoints(50, 1), stats.getValue(alice.id).best)
        assertEquals(RoundPoints(50, 1), stats.getValue(alice.id).worst)
    }

    @Test
    fun `sevens are unknown when no round records the holder`() {
        val r1 = round(1)
        val stats = playerStats(listOf(alice, bob), mapOf(scores(r1, alice to 100, bob to 150)))

        assertNull(stats.getValue(alice.id).sevensTaken)
    }

    @Test
    fun `a participant who never played has no statistics`() {
        val r1 = round(1)
        val stats = playerStats(listOf(alice, zoe), mapOf(scores(r1, alice to 250)))

        assertTrue(zoe.id !in stats)
        assertEquals(1, stats.getValue(alice.id).roundsPlayed)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects a score from outside the party`() {
        val r1 = round(1)
        playerStats(listOf(alice), mapOf(r1 to listOf(RoundScore(r1.id, UUID.randomUUID(), 250))))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects a seven holder from outside the party`() {
        val r1 = round(1, sevenHolder = zoe)
        playerStats(listOf(alice), mapOf(scores(r1, alice to 250)))
    }
}
