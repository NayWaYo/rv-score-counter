package com.rvetiveau.rvscorecount.ui.screens.free

import androidx.annotation.StringRes
import com.rvetiveau.rvscorecount.domain.model.EndRule
import com.rvetiveau.rvscorecount.domain.model.WinDirection
import com.rvetiveau.rvscorecount.domain.model.playerNameKey
import com.rvetiveau.rvscorecount.domain.model.suggestPlayerNames
import com.rvetiveau.rvscorecount.ui.screens.setup.ParticipantDraft

enum class FreeEndMode { MANUAL, TARGET_SCORE, ROUND_COUNT }

/**
 * Draft of a Partie of Jeu libre (Story 11.2): its game name, its Participants in table order and
 * its rules. Each end mode keeps its own value while another one is chosen.
 */
data class FreeSetupUiState(
    val minPlayers: Int = 2,
    val maxPlayers: Int = 10,
    val gameNameInput: String = "",
    /** Game names already played, the most recent first. */
    val knownGameNames: List<String> = emptyList(),
    val nameInput: String = "",
    val participants: List<ParticipantDraft> = emptyList(),
    /** Names of earlier Parties, the most recently played first. */
    val knownPlayerNames: List<String> = emptyList(),
    @StringRes val messageRes: Int? = null,
    val winDirection: WinDirection = WinDirection.LOWEST_WINS,
    val endMode: FreeEndMode = FreeEndMode.MANUAL,
    val targetScoreInput: String = DEFAULT_TARGET_SCORE.toString(),
    val roundCountInput: String = DEFAULT_ROUND_COUNT.toString(),
    val isCreatingParty: Boolean = false,
    val creationCompleted: Boolean = false,
    @StringRes val creationErrorRes: Int? = null
) {
    val canAdd: Boolean get() = participants.size < maxPlayers
    val isMaxReached: Boolean get() = !canAdd

    /** Known games starting with the name typed; none once the name is exactly a known one. */
    val gameNameSuggestions: List<String>
        get() = if (knownGameNames.any { playerNameKey(it) == playerNameKey(gameNameInput) }) {
            emptyList()
        } else {
            suggestPlayerNames(knownGameNames, gameNameInput, taken = emptyList())
        }

    val nameSuggestions: List<String>
        get() = if (canAdd) suggestPlayerNames(knownPlayerNames, nameInput, participants.map { it.name }) else emptyList()

    val endValueInput: String
        get() = when (endMode) {
            FreeEndMode.MANUAL -> ""
            FreeEndMode.TARGET_SCORE -> targetScoreInput
            FreeEndMode.ROUND_COUNT -> roundCountInput
        }

    /** The end value when the mode needs one: null while it is not a number from 1 to [MAX_END_VALUE]. */
    private val endValue: Int? get() = endValueInput.toIntOrNull()?.takeIf { it in 1..MAX_END_VALUE }

    val isEndValueValid: Boolean get() = endMode == FreeEndMode.MANUAL || endValue != null

    /** The end rule to create the Partie with; null for an end by hand, or while the value is invalid. */
    val endRule: EndRule?
        get() = when (endMode) {
            FreeEndMode.MANUAL -> null
            FreeEndMode.TARGET_SCORE -> endValue?.let(EndRule::TargetScore)
            FreeEndMode.ROUND_COUNT -> endValue?.let(EndRule::RoundCount)
        }

    val canStart: Boolean
        get() = gameNameInput.isNotBlank() &&
            participants.size >= minPlayers &&
            isEndValueValid &&
            !isCreatingParty &&
            !creationCompleted

    companion object {
        const val MAX_GAME_NAME_LENGTH = 30
        const val MAX_END_VALUE = 9_999
        const val DEFAULT_TARGET_SCORE = 100
        const val DEFAULT_ROUND_COUNT = 10
    }
}
