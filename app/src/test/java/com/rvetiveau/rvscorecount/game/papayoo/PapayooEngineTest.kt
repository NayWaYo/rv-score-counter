package com.rvetiveau.rvscorecount.game.papayoo

import com.rvetiveau.rvscorecount.domain.engine.DealingRules
import com.rvetiveau.rvscorecount.domain.engine.RoundValidation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class PapayooEngineTest {

    private val engine = PapayooEngine()

    private fun scoresOf(vararg values: Int): Map<UUID, Int> =
        values.associate { UUID.randomUUID() to it }

    @Test
    fun `metadata matches papayoo rules`() {
        assertEquals("papayoo", engine.gameId)
        assertEquals(3, engine.minPlayers)
        assertEquals(8, engine.maxPlayers)
        assertEquals(250, engine.totalPointsPerRound)
    }

    @Test
    fun `validateRound succeeds when sum is exactly 250`() {
        val result = engine.validateRound(scoresOf(100, 100, 50))
        assertEquals(RoundValidation.Success, result)
    }

    @Test
    fun `validateRound succeeds when a score is zero`() {
        val result = engine.validateRound(scoresOf(0, 150, 100))
        assertEquals(RoundValidation.Success, result)
    }

    @Test
    fun `validateRound returns negative difference when sum is below 250`() {
        val result = engine.validateRound(scoresOf(100, 100, 40))
        assertTrue(result is RoundValidation.Error)
        assertEquals(-10, (result as RoundValidation.Error).difference)
    }

    @Test
    fun `validateRound returns positive difference when sum is above 250`() {
        val result = engine.validateRound(scoresOf(100, 100, 60))
        assertTrue(result is RoundValidation.Error)
        assertEquals(10, (result as RoundValidation.Error).difference)
    }

    @Test
    fun `precomputeLastScore returns positive complement`() {
        assertEquals(150, engine.precomputeLastScore(scoresOf(100)))
    }

    @Test
    fun `precomputeLastScore returns zero when entered sum equals total`() {
        assertEquals(0, engine.precomputeLastScore(scoresOf(250)))
    }

    @Test
    fun `precomputeLastScore returns negative complement when entered sum exceeds total`() {
        assertEquals(-50, engine.precomputeLastScore(scoresOf(300)))
    }

    @Test
    fun `dealing rules cover the whole deck for 3 to 8 players`() {
        (3..8).forEach { players ->
            val rules = checkNotNull(engine.dealingRules(players))
            val deck = if (rules.removedSuitRank == null) 60 else 56
            assertEquals(deck, rules.cardsPerPlayer * players)
        }
    }

    @Test
    fun `dealing rules match the official table`() {
        assertEquals(DealingRules(20, 5), engine.dealingRules(3))
        assertEquals(DealingRules(15, 5), engine.dealingRules(4))
        assertEquals(DealingRules(12, 4), engine.dealingRules(5))
        assertEquals(DealingRules(10, 3), engine.dealingRules(6))
        assertEquals(DealingRules(8, 3, removedSuitRank = 1), engine.dealingRules(7))
        assertEquals(DealingRules(7, 3, removedSuitRank = 1), engine.dealingRules(8))
    }

    @Test
    fun `no dealing rules outside 3 to 8 players`() {
        assertNull(engine.dealingRules(2))
        assertNull(engine.dealingRules(9))
    }

    @Test
    fun `a hand is dealt by threes then a last smaller packet`() {
        val twenty = checkNotNull(engine.dealingRules(3))
        assertEquals(6 to 2, twenty.fullPackets to twenty.lastPacket)
        val twelve = checkNotNull(engine.dealingRules(5))
        assertEquals(4 to 0, twelve.fullPackets to twelve.lastPacket)
    }

    @Test
    fun `the payoo are worth their number, plus 40 with the seven`() {
        val engine = PapayooEngine()

        assertEquals(1..20, engine.faceValueCards)
        assertEquals(19, engine.scoreOfCards(setOf(3, 6, 10), tookSuitSeven = false))
        assertEquals(59, engine.scoreOfCards(setOf(3, 6, 10), tookSuitSeven = true))
        assertEquals(40, engine.scoreOfCards(emptySet(), tookSuitSeven = true))
        assertEquals(250, engine.scoreOfCards((1..20).toSet(), tookSuitSeven = true))
        assertThrows(IllegalArgumentException::class.java) { engine.scoreOfCards(setOf(21), tookSuitSeven = false) }
    }
}
