package com.rvetiveau.rvscorecount.ui.components

import com.rvetiveau.rvscorecount.domain.model.CardSuit
import com.rvetiveau.rvscorecount.domain.model.Participant
import com.rvetiveau.rvscorecount.domain.model.Round
import com.rvetiveau.rvscorecount.domain.model.RoundScore
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RoundTableTest {

    private val partyId = UUID.randomUUID()
    private val alice = Participant(UUID.randomUUID(), partyId, "Alice", 0)
    private val bob = Participant(UUID.randomUUID(), partyId, "Bob", 1)
    private val chloe = Participant(UUID.randomUUID(), partyId, "Chloé", 2)

    private fun round(number: Int, sevenHolderId: UUID? = null) =
        Round(UUID.randomUUID(), partyId, number, CardSuit.HEART, sevenHolderId = sevenHolderId)

    @Test
    fun `orders players by table position and rounds chronologically`() {
        val r1 = round(1, sevenHolderId = bob.id)
        val r2 = round(2)
        val table = roundTable(
            listOf(chloe, alice, bob),
            mapOf(
                r2 to listOf(RoundScore(r2.id, alice.id, 10), RoundScore(r2.id, bob.id, 240)),
                r1 to listOf(RoundScore(r1.id, bob.id, 50), RoundScore(r1.id, alice.id, 200))
            )
        )

        assertEquals(listOf("Alice", "Bob", "Chloé"), table.players.map { it.name })
        assertEquals(listOf(1, 2), table.rows.map { it.roundNumber })
        assertEquals(bob.id, table.rows.first().sevenHolderId)
        assertEquals(210, table.totalOf(alice.id))
        assertEquals(0, table.totalOf(chloe.id))
    }

    @Test
    fun `marks the fewest points as best and the most as worst`() {
        val row = RoundTableRow(1, CardSuit.SPADE, mapOf(alice.id to 30, bob.id to 120, chloe.id to 100))

        assertEquals(30, row.bestPoints)
        assertEquals(120, row.worstPoints)
        assertEquals(250, row.total)
    }

    @Test
    fun `marks the most points as best when the highest total wins, without a suit`() {
        val row = RoundTableRow(1, suit = null, points = mapOf(alice.id to -5, bob.id to 12), highestIsBest = true)

        assertEquals(12, row.bestPoints)
        assertEquals(-5, row.worstPoints)
    }

    @Test
    fun `has no best or worst when everyone scored the same`() {
        val row = RoundTableRow(1, CardSuit.SPADE, mapOf(alice.id to 125, bob.id to 125))

        assertNull(row.bestPoints)
        assertNull(row.worstPoints)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects a score from outside the party`() {
        val r1 = round(1)
        roundTable(listOf(alice), mapOf(r1 to listOf(RoundScore(r1.id, UUID.randomUUID(), 250))))
    }
}
