package com.rvetiveau.rvscorecount.game.papayoo

import com.rvetiveau.rvscorecount.domain.engine.RoundValidation
import org.junit.Assert.assertEquals
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
}
