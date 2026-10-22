package com.rvetiveau.rvscorecount.ui.screens.belote

import androidx.annotation.StringRes
import com.rvetiveau.rvscorecount.domain.model.BeloteEndRule
import com.rvetiveau.rvscorecount.domain.model.suggestPlayerNames

enum class BeloteEndMode { TARGET_SCORE, ROUND_COUNT }

/**
 * Draft of a Partie of Belote before its creation: two Équipes of two players ([playerNames] by
 * Équipe, then by seat) and the end rule, each mode keeping its own value while the other is chosen.
 */
data class BeloteSetupUiState(
    val isInitialized: Boolean = false,
    val teamNames: List<String> = listOf("", ""),
    val playerNames: List<List<String>> = listOf(listOf("", ""), listOf("", "")),
    /** Names of earlier Parties, the most recently played first. */
    val knownPlayerNames: List<String> = emptyList(),
    val endMode: BeloteEndMode = BeloteEndMode.TARGET_SCORE,
    val targetScoreInput: String = BeloteEndRule.DEFAULT_TARGET_SCORE.toString(),
    val roundCountInput: String = BeloteEndRule.DEFAULT_ROUND_COUNT.toString(),
    val isCreatingParty: Boolean = false,
    val creationCompleted: Boolean = false,
    @StringRes val creationErrorRes: Int? = null
) {
    val endValueInput: String
        get() = when (endMode) {
            BeloteEndMode.TARGET_SCORE -> targetScoreInput
            BeloteEndMode.ROUND_COUNT -> roundCountInput
        }

    /** Null while the value is not a number between 1 and [MAX_END_VALUE]. */
    val endRule: BeloteEndRule?
        get() = endValueInput.toIntOrNull()?.takeIf { it in 1..MAX_END_VALUE }?.let { value ->
            when (endMode) {
                BeloteEndMode.TARGET_SCORE -> BeloteEndRule.TargetScore(value)
                BeloteEndMode.ROUND_COUNT -> BeloteEndRule.RoundCount(value)
            }
        }

    /** Players around the table, the Équipes alternating: A1, B1, A2, B2. The first one deals. */
    val tableOrder: List<String>
        get() = listOf(playerNames[0][0], playerNames[1][0], playerNames[0][1], playerNames[1][1])

    /** Known names not yet seated, offered while a player field is still empty. */
    val playerSuggestions: List<String>
        get() = if (playerNames.flatten().any(String::isBlank)) {
            suggestPlayerNames(knownPlayerNames, typed = "", taken = playerNames.flatten())
        } else {
            emptyList()
        }

    val canStart: Boolean
        get() = isInitialized &&
            teamNames.all(String::isNotBlank) &&
            playerNames.flatten().all(String::isNotBlank) &&
            endRule != null &&
            !isCreatingParty &&
            !creationCompleted

    companion object {
        const val MAX_END_VALUE = 9999
    }
}
