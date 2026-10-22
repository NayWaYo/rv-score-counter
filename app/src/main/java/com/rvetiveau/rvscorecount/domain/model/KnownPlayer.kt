package com.rvetiveau.rvscorecount.domain.model

import java.text.Normalizer
import java.util.Locale

/**
 * A Joueur, with the creation time of their latest Partie of any Jeu; null once every Partie they
 * played has been deleted, since a Joueur outlives their Parties (Story 10.1).
 */
data class KnownPlayer(
    val name: String,
    val lastPlayedAt: Long?
)

/**
 * Known names, the most recently played first and those without any Partie left last, by name; one
 * per player: names that differ only by case or surrounding spaces are the same player, shown with
 * the spelling of their latest Partie.
 */
fun List<KnownPlayer>.distinctPlayerNames(): List<String> =
    filter { it.name.isNotBlank() }
        .sortedWith(compareBy<KnownPlayer, Long?>(nullsLast(reverseOrder())) { it.lastPlayedAt }.thenBy { playerNameKey(it.name) })
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

/**
 * What identifies a Joueur (Story 10.1): two names that differ only by case or surrounding spaces
 * are the same player. Accents count: « Helene » and « Hélène » are two players.
 */
fun playerNameKey(name: String): String = name.trim().lowercase(Locale.ROOT)

/** The first of [names] that repeats an earlier one, by [playerNameKey]; null when all differ. */
fun duplicatePlayerName(names: List<String>): String? {
    val seen = HashSet<String>()
    return names.firstOrNull { !seen.add(playerNameKey(it)) }
}

private fun searchKey(text: String): String =
    Normalizer.normalize(playerNameKey(text), Normalizer.Form.NFD).replace(COMBINING_MARKS, "")

private val COMBINING_MARKS = Regex("\\p{Mn}+")
