package com.rvetiveau.rvscorecount.domain.scan

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/** One character read on a photo, its center and height in the photo's own frame. */
data class ScannedSymbol(
    val text: String,
    val x: Float,
    val y: Float,
    val height: Float,
    val confidence: Float
)

/**
 * A word read on a photo: its characters, and its angle in degrees clockwise in the photo's frame.
 * The same printed number is usually read several times, once per rotation of the photo.
 */
data class ScannedWord(
    val symbols: List<ScannedSymbol>,
    val angle: Float
)

/**
 * Turns the words read on a photo of cards held in a fan into the card numbers it shows, each once.
 * Tuned on photos of Tarot trumps (Story 9.1): the reading is confirmed by the user, never trusted.
 */
object TrumpReading {

    /** A number confirmed by readings worth less than this summed confidence is dropped. */
    private const val MIN_SCORE = 0.25

    /** Two digits farther apart than this many digit heights belong to different numbers. */
    private const val MAX_DIGIT_SPACING = 1.0f

    /** Readings closer than this many text heights are the same printed number. */
    private const val SAME_PLACE = 0.7f

    private val NUMBER = Regex("[1-9][0-9]?")

    private data class Reading(
        val value: Int,
        val confidence: Float,
        val x: Float,
        val y: Float,
        val angle: Float,
        val height: Float
    )

    fun numbers(words: List<ScannedWord>, range: IntRange): Set<Int> =
        aggregate(words.flatMap { word -> readings(word, range) })

    private fun readings(word: ScannedWord, range: IntRange): List<Reading> =
        digitGroups(word.symbols).mapNotNull { group ->
            val text = group.joinToString("") { it.text }
            // "01" is a 10 read upside down, not a 1.
            val value = text.takeIf(NUMBER::matches)?.toInt()?.takeIf { it in range } ?: return@mapNotNull null
            Reading(
                value = value,
                confidence = group.minOf { it.confidence },
                x = group.map { it.x }.average().toFloat(),
                y = group.map { it.y }.average().toFloat(),
                angle = normalized(word.angle),
                height = group.maxOf { it.height }
            )
        }

    /**
     * Splits a word into numbers: neighbouring indices get merged ("1614" for 16 and 14) and the
     * frame glued to them ("14)", "|3"). Punctuation separates numbers, and so does a gap wider than
     * a digit between two digits. A letter is a misread digit ("L8" is an 18): the digits touching it
     * are dropped rather than read as another number.
     */
    internal fun digitGroups(symbols: List<ScannedSymbol>): List<List<ScannedSymbol>> {
        val groups = mutableListOf<List<ScannedSymbol>>()
        var current = mutableListOf<ScannedSymbol>()
        var touchesLetter = false
        fun close() {
            if (current.isNotEmpty() && !touchesLetter) groups += current
            current = mutableListOf()
            touchesLetter = false
        }
        symbols.forEach { symbol ->
            if (symbol.text.any(Char::isLetter)) {
                touchesLetter = true
                close()
                touchesLetter = true
                return@forEach
            }
            val isDigit = symbol.text.length == 1 && symbol.text[0].isDigit()
            val previous = current.lastOrNull()
            val farFromPrevious = previous != null &&
                hypot(previous.x - symbol.x, previous.y - symbol.y) >
                MAX_DIGIT_SPACING * maxOf(previous.height, symbol.height)
            if (!isDigit || farFromPrevious) close()
            if (isDigit) current += symbol
        }
        close()
        return groups
    }

    /**
     * 1. The cards of a fan point roughly the same way: a reading more than 90° away from the
     *    confidence-weighted mean orientation is upside down (a 6 read as a 9) and dropped.
     * 2. Readings at the same place are one printed number: the value with the highest summed
     *    confidence wins there.
     * 3. A card exists once: the result is the set of winning values.
     */
    private fun aggregate(readings: List<Reading>): Set<Int> {
        if (readings.isEmpty()) return emptySet()
        val reference = Math.toDegrees(
            atan2(
                readings.sumOf { it.confidence * sin(Math.toRadians(it.angle.toDouble())) },
                readings.sumOf { it.confidence * cos(Math.toRadians(it.angle.toDouble())) }
            )
        ).toFloat()
        val upright = readings.filter { abs(normalized(it.angle - reference)) <= 90f }

        val places = mutableListOf<MutableList<Reading>>()
        upright.sortedByDescending { it.confidence }.forEach { reading ->
            val place = places.firstOrNull { place ->
                val head = place.first()
                hypot(head.x - reading.x, head.y - reading.y) < SAME_PLACE * maxOf(head.height, reading.height)
            }
            if (place == null) places += mutableListOf(reading) else place += reading
        }
        return places.mapNotNull { place ->
            place.groupBy { it.value }
                .mapValues { (_, same) -> same.sumOf { it.confidence.toDouble() } }
                .maxByOrNull { it.value }
                ?.takeIf { it.value >= MIN_SCORE }
                ?.key
        }.toSortedSet()
    }

    /** [angle] in (-180°, 180°]. */
    private fun normalized(angle: Float): Float = ((angle % 360f) + 540f) % 360f - 180f
}
