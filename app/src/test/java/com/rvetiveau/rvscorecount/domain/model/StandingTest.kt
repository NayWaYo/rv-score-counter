package com.rvetiveau.rvscorecount.domain.model

import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class StandingTest {

    private val partyId = UUID.randomUUID()
    private val alice = Participant(UUID.randomUUID(), partyId, "Alice", 0)
    private val bob = Participant(UUID.randomUUID(), partyId, "Bob", 1)
    private val charlie = Participant(UUID.randomUUID(), partyId, "Charlie", 2)

    @Test
    fun `sums every round per participant and sorts by ascending cumulative score`() {
        val round1 = UUID.randomUUID()
        val round2 = UUID.randomUUID()
        val scores = listOf(
            RoundScore(round1, alice.id, 100), RoundScore(round1, bob.id, 50), RoundScore(round1, charlie.id, 100),
            RoundScore(round2, alice.id, 0), RoundScore(round2, bob.id, 200), RoundScore(round2, charlie.id, 50)
        )

        val standings = computeStandings(listOf(alice, bob, charlie), scores)

        assertEquals(listOf(alice, charlie, bob), standings.map { it.participant })
        assertEquals(listOf(100, 150, 250), standings.map { it.cumulativeScore })
    }

    @Test
    fun `breaks ties by list position and starts participants without rounds at zero`() {
        val round = UUID.randomUUID()
        val scores = listOf(RoundScore(round, alice.id, 0), RoundScore(round, charlie.id, 0))

        val standings = computeStandings(listOf(charlie, bob, alice), scores)

        assertEquals(listOf(alice, bob, charlie), standings.map { it.participant })
        assertEquals(listOf(0, 0, 0), standings.map { it.cumulativeScore })
    }

    @Test
    fun `fails on a score from a participant outside the party`() {
        val stray = RoundScore(UUID.randomUUID(), UUID.randomUUID(), 10)

        assertThrows(IllegalArgumentException::class.java) {
            computeStandings(listOf(alice, bob), listOf(stray))
        }
    }
}
