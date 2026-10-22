package com.rvetiveau.rvscorecount.domain.scan

import org.junit.Assert.assertEquals
import org.junit.Test

class TrumpReadingTest {

    private val trumps = 1..20

    /** A word of [text] whose characters are [spacing] apart along x, starting at ([x], [y]). */
    private fun word(
        text: String,
        x: Float = 0f,
        y: Float = 0f,
        angle: Float = 0f,
        confidence: Float = 0.8f,
        spacing: Float = 50f,
        height: Float = 100f
    ) = ScannedWord(
        symbols = text.mapIndexed { index, char ->
            ScannedSymbol(char.toString(), x + index * spacing, y, height, confidence)
        },
        angle = angle
    )

    @Test
    fun `reads plain numbers within the range, each once`() {
        val words = listOf(word("3"), word("14", x = 500f), word("14", x = 500f), word("20", x = 1000f))

        assertEquals(setOf(3, 14, 20), TrumpReading.numbers(words, trumps))
    }

    @Test
    fun `numbers out of range, with a leading zero or empty are ignored`() {
        val words = listOf(word("21"), word("0", x = 500f), word("01", x = 1000f), word("", x = 1500f))

        assertEquals(emptySet<Int>(), TrumpReading.numbers(words, trumps))
    }

    @Test
    fun `merged neighbouring numbers are split on the gap between them`() {
        val merged = ScannedWord(
            symbols = listOf(
                ScannedSymbol("1", 0f, 0f, 100f, 0.8f),
                ScannedSymbol("6", 50f, 0f, 100f, 0.8f),
                ScannedSymbol("1", 300f, 0f, 100f, 0.8f),
                ScannedSymbol("4", 350f, 0f, 100f, 0.8f)
            ),
            angle = 0f
        )

        assertEquals(setOf(14, 16), TrumpReading.numbers(listOf(merged), trumps))
    }

    @Test
    fun `punctuation separates and digits touching a letter are dropped`() {
        assertEquals(setOf(14), TrumpReading.numbers(listOf(word("14)")), trumps))
        assertEquals(setOf(3), TrumpReading.numbers(listOf(word("|3")), trumps))
        assertEquals(emptySet<Int>(), TrumpReading.numbers(listOf(word("L8")), trumps))
        assertEquals(emptySet<Int>(), TrumpReading.numbers(listOf(word("8I")), trumps))
    }

    @Test
    fun `a number read upside down against the fan is dropped`() {
        val words = listOf(
            word("19", x = 0f, angle = 60f),
            word("16", x = 500f, angle = 80f),
            word("6", x = 1000f, angle = 82f),
            word("9", x = 1000f, angle = -98f, confidence = 0.9f)
        )

        assertEquals(setOf(6, 16, 19), TrumpReading.numbers(words, trumps))
    }

    @Test
    fun `the value read most confidently at a place wins there`() {
        val words = listOf(
            word("18", angle = 0f, confidence = 0.8f),
            word("18", angle = 10f, confidence = 0.7f),
            word("8", x = 25f, angle = 5f, confidence = 0.7f)
        )

        assertEquals(setOf(18), TrumpReading.numbers(words, trumps))
    }

    @Test
    fun `a single faint reading is kept above the minimum score only`() {
        assertEquals(setOf(7), TrumpReading.numbers(listOf(word("7", confidence = 0.3f)), trumps))
        assertEquals(emptySet<Int>(), TrumpReading.numbers(listOf(word("7", confidence = 0.2f)), trumps))
    }

    @Test
    fun `nothing read gives no number`() {
        assertEquals(emptySet<Int>(), TrumpReading.numbers(emptyList(), trumps))
    }
}
