package com.rvetiveau.rvscorecount.ui.components

import com.rvetiveau.rvscorecount.domain.model.CardSuit
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Test

class ScoreChartTest {

    private val alice = RoundTablePlayer(UUID.randomUUID(), "Alice")
    private val bob = RoundTablePlayer(UUID.randomUUID(), "Bob")
    private val zoe = RoundTablePlayer(UUID.randomUUID(), "Zoé")

    @Test
    fun `accumulates each player's score from zero, round after round`() {
        val table = RoundTable(
            players = listOf(alice, bob),
            rows = listOf(
                RoundTableRow(1, CardSuit.HEART, mapOf(alice.participantId to 200, bob.participantId to 50)),
                RoundTableRow(2, CardSuit.SPADE, mapOf(alice.participantId to 0, bob.participantId to 250))
            )
        )

        val series = table.cumulativeSeries()

        assertEquals(listOf("Alice", "Bob"), series.map { it.player.name })
        assertEquals(listOf(0, 200, 200), series[0].points)
        assertEquals(listOf(0, 50, 300), series[1].points)
        assertEquals(300, series[1].total)
    }

    @Test
    fun `a player who joined later starts from zero just before their first round`() {
        val table = RoundTable(
            players = listOf(alice, zoe, bob),
            rows = listOf(
                RoundTableRow(1, CardSuit.HEART, mapOf(alice.participantId to 100, bob.participantId to 150)),
                RoundTableRow(2, CardSuit.CLUB, mapOf(alice.participantId to 50, zoe.participantId to 120, bob.participantId to 80))
            )
        )

        val zoeSeries = table.cumulativeSeries().single { it.player == zoe }

        assertEquals(listOf(null, 0, 120), zoeSeries.points)
    }

    @Test
    fun `a player who never played has no series`() {
        val table = RoundTable(
            players = listOf(alice, zoe),
            rows = listOf(RoundTableRow(1, CardSuit.HEART, mapOf(alice.participantId to 250)))
        )

        assertEquals(listOf("Alice"), table.cumulativeSeries().map { it.player.name })
    }

    @Test
    fun `axis steps are clean numbers giving at most four intervals`() {
        assertEquals(1, niceStep(0))
        assertEquals(1, niceStep(3))
        assertEquals(25, niceStep(90))
        assertEquals(100, niceStep(300))
        assertEquals(250, niceStep(1000))
        assertEquals(500, niceStep(1500))
    }
}
