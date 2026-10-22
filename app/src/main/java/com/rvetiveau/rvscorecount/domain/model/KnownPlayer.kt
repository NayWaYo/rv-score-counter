package com.rvetiveau.rvscorecount.domain.model

import java.text.Normalizer
import java.util.Locale

/** A player name already used in a Partie of any Jeu, with the creation time of its latest Partie. */
data class KnownPlayer(
    val name: String,
    val lastPlayedAt: Long
)

/**
 * Known names, the most recently played first, one per player: names that differ only by case or
 * surrounding spaces are the same player, shown with the spelling of their latest Partie.
 */
fun List<KnownPlayer>.distinctPlayerNames(): List<String> =
    filter { it.name.isNotBlank() }
        .sortedByDescending { it.lastPlayedAt }
        .distinctBy { playerNameKey(it.name) }
        .map { it.name.trim() }

/**
 * Names to suggest at Création de Partie: known names not already [taken], starting with [typed].
 * Case and accents are ignored on both sides, so "hel" suggests "Hélène".
 */
fun suggestPlayerNames(knownNames: List<String>, typed: String, taken: Collection<String>): List<String> {
    val typedKey = searchKey(typed)
    val takenKeys = taken.mapTo(HashSet(), ::playerNameKey)
    return knownNames.filter { name ->
        playerNameKey(name) !in takenKeys && searchKey(name).startsWith(typedKey)
    }
}

private fun playerNameKey(name: String): String = name.trim().lowercase(Locale.ROOT)

private fun searchKey(text: String): String =
    Normalizer.normalize(playerNameKey(text), Normalizer.Form.NFD).replace(COMBINING_MARKS, "")

private val COMBINING_MARKS = Regex("\\p{Mn}+")
