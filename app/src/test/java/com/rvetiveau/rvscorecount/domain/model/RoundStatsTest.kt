package com.rvetiveau.rvscorecount.domain.model

import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class RoundStatsTest {

    private val partyId = UUID.randomUUID()
    private val alice = Participant(UUID.randomUUID(), partyId, "Alice", 0)
    private val bob = Participant(UUID.randomUUID(), partyId, "Bob", 1)
    private val chloe = Participant(UUID.randomUUID(), partyId, "Chloé", 2)
    private val participants = listOf(chloe, bob, alice)

    private fun round(number: Int, vararg points: Pair<Participant, Int>): Pair<Round, List<RoundScore>> {
        val round = Round(UUID.randomUUID(), partyId, number, CardSuit.HEART)
        return round to points.map { (participant, value) -> RoundScore(round.id, participant.id, value) }
    }

    @Test
    fun `the record is the highest score taken in a single round`() {
        val record = roundRecord(
            participants,
            mapOf(
                round(1, alice to 100, bob to 90, chloe to 60),
                round(2, alice to 20, bob to 210, chloe to 20)
            )
        )

        assertEquals(RoundRecord(listOf(RoundRecordEntry(bob, 2)), 210), record)
    }

    @Test
    fun `ties are kept by round then table order`() {
        val record = roundRecord(
            participants,
            mapOf(
                round(2, alice to 10, bob to 120, chloe to 120),
                round(1, alice to 120, bob to 70, chloe to 60)
            )
        )

        assertEquals(
            RoundRecord(
                listOf(RoundRecordEntry(alice, 1), RoundRecordEntry(bob, 2), RoundRecordEntry(chloe, 2)),
                120
            ),
            record
        )
    }

    @Test
    fun `no record before any round`() {
        assertNull(roundRecord(participants, emptyMap()))
    }

    @Test
    fun `a score from an unknown participant is an inconsistency`() {
        val stranger = Participant(UUID.randomUUID(), partyId, "Inconnu", 3)

        assertThrows(IllegalArgumentException::class.java) {
            roundRecord(participants, mapOf(round(1, stranger to 250)))
        }
    }
}
