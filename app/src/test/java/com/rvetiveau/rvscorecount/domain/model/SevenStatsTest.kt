package com.rvetiveau.rvscorecount.domain.model

import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class SevenStatsTest {

    private val partyId = UUID.randomUUID()
    private val alice = Participant(UUID.randomUUID(), partyId, "Alice", 0)
    private val bob = Participant(UUID.randomUUID(), partyId, "Bob", 1)
    private val chloe = Participant(UUID.randomUUID(), partyId, "Chloé", 2)
    private val participants = listOf(chloe, bob, alice)

    private fun round(number: Int, holder: Participant?) =
        Round(UUID.randomUUID(), partyId, number, CardSuit.HEART, sevenHolderId = holder?.id)

    @Test
    fun `the champion is whoever took the seven most often`() {
        val champion = sevenChampion(participants, listOf(round(1, bob), round(2, alice), round(3, bob)))

        assertEquals(SevenChampion(listOf(bob), 2), champion)
    }

    @Test
    fun `ties are kept in table order and rounds without holder are ignored`() {
        val champion = sevenChampion(participants, listOf(round(1, chloe), round(2, null), round(3, alice)))

        assertEquals(SevenChampion(listOf(alice, chloe), 1), champion)
    }

    @Test
    fun `no champion when no round recorded a holder`() {
        assertNull(sevenChampion(participants, listOf(round(1, null))))
        assertNull(sevenChampion(participants, emptyList()))
    }

    @Test
    fun `a holder outside the party is an inconsistency`() {
        val stranger = Participant(UUID.randomUUID(), partyId, "Zoé", 3)

        assertThrows(IllegalArgumentException::class.java) {
            sevenChampion(participants, listOf(round(1, stranger)))
        }
    }
}
