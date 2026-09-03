package com.rvetiveau.rvscorecount.domain.usecase

import com.rvetiveau.rvscorecount.domain.model.CardSuit
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Test

class RollDieUseCaseTest {

    /** Records the requested bitCount so we can prove nextInt(4) asks for exactly 2 bits. */
    private class FakeRandom(private val values: List<Int>) : Random() {
        var lastBitCount: Int? = null
        private var index = 0
        override fun nextBits(bitCount: Int): Int {
            lastBitCount = bitCount
            return values[index++]
        }
    }

    @Test
    fun `maps index 0 to SPADE`() {
        val useCase = RollDieUseCase()
        assertEquals(CardSuit.SPADE, useCase(FakeRandom(listOf(0))))
    }

    @Test
    fun `maps index 1 to HEART`() {
        val useCase = RollDieUseCase()
        assertEquals(CardSuit.HEART, useCase(FakeRandom(listOf(1))))
    }

    @Test
    fun `maps index 2 to CLUB`() {
        val useCase = RollDieUseCase()
        assertEquals(CardSuit.CLUB, useCase(FakeRandom(listOf(2))))
    }

    @Test
    fun `maps index 3 to DIAMOND`() {
        val useCase = RollDieUseCase()
        assertEquals(CardSuit.DIAMOND, useCase(FakeRandom(listOf(3))))
    }

    @Test
    fun `requests exactly 2 bits for a 4-way uniform draw`() {
        val useCase = RollDieUseCase()
        val random = FakeRandom(listOf(0))

        useCase(random)

        assertEquals(2, random.lastBitCount)
    }
}
