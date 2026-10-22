package com.rvetiveau.rvscorecount.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class KnownPlayerTest {

    @Test
    fun `names are listed most recent first, once per player, with their latest spelling`() {
        val known = listOf(
            KnownPlayer("Bob", lastPlayedAt = 10),
            KnownPlayer("alice", lastPlayedAt = 5),
            KnownPlayer("Alice ", lastPlayedAt = 20),
            KnownPlayer("   ", lastPlayedAt = 30),
            KnownPlayer("Chloé", lastPlayedAt = 1)
        )

        assertEquals(listOf("Alice", "Bob", "Chloé"), known.distinctPlayerNames())
    }

    @Test
    fun `players without any partie left come last, by name`() {
        val known = listOf(
            KnownPlayer("zoé", lastPlayedAt = null),
            KnownPlayer("Bob", lastPlayedAt = 10),
            KnownPlayer("Denis", lastPlayedAt = null),
            KnownPlayer("Alice", lastPlayedAt = 20)
        )

        assertEquals(listOf("Alice", "Bob", "Denis", "zoé"), known.distinctPlayerNames())
    }

    @Test
    fun `suggestions start with the typed text, ignoring case and accents`() {
        val known = listOf("Hélène", "Henri", "Théo", "Éric")

        assertEquals(listOf("Hélène", "Henri"), suggestPlayerNames(known, typed = " HE", taken = emptyList()))
        assertEquals(listOf("Éric"), suggestPlayerNames(known, typed = "e", taken = emptyList()))
        assertEquals(known, suggestPlayerNames(known, typed = "", taken = emptyList()))
    }

    @Test
    fun `names already taken are not suggested, whatever their case`() {
        val known = listOf("Alice", "Bob", "Chloé")

        assertEquals(listOf("Chloé"), suggestPlayerNames(known, typed = "", taken = listOf("alice", " BOB ")))
    }

    @Test
    fun `a player is known by their name without case or surrounding spaces, accents kept`() {
        assertEquals(playerNameKey(" Alice "), playerNameKey("ALICE"))
        assertEquals("hélène", playerNameKey("Hélène"))
        assertEquals(null, duplicatePlayerName(listOf("Alice", "Bob", "Helene", "Hélène")))
        assertEquals(" alice", duplicatePlayerName(listOf("Alice", "Bob", " alice", "BOB")))
    }

    @Test
    fun `nothing is suggested when no known name matches`() {
        assertEquals(emptyList<String>(), suggestPlayerNames(listOf("Alice"), typed = "Z", taken = emptyList()))
        assertEquals(emptyList<String>(), suggestPlayerNames(emptyList(), typed = "", taken = emptyList()))
    }
}
