package com.rvetiveau.rvscorecount.ui.screens.round

import com.rvetiveau.rvscorecount.domain.model.CardSuit
import java.util.UUID

/**
 * [suit] is null before any roll (Story 3.1, AC 2, 7); no Round exists until validation (Story 3.4).
 * [content] is null while the Partie/Participants aggregate is loading or failed (AC 1, 10):
 * score entry only renders once it is non-null. [isSaving] covers the single in-flight
 * validation; [saveError] is set when it failed, leaving the drafts untouched.
 */
data class RoundEntryUiState(
    val suit: CardSuit? = null,
    val content: RoundEntryContent? = null,
    val isLoading: Boolean = true,
    val loadError: Boolean = false,
    val isSaving: Boolean = false,
    val saveError: Boolean = false
) {
    val currentSum: Long get() = content?.currentSum ?: 0L

    /** Locked out as soon as any field holds a value, including "0" (AC 8). */
    val rerollEnabled: Boolean get() = content != null && !content.hasAnyScoreEntered

    /** The first roll stays available after score entry (Story 3.4, AC 8); only « Relancer » locks. */
    val rollEnabled: Boolean get() = content != null && !isSaving && (suit == null || rerollEnabled)

    val canValidate: Boolean get() = suit != null && !isSaving && content?.canValidate == true
}

/** One row per Participant ordered by listPosition (AC 1). [scores] holds the raw text draft
 * keyed by participant id; an absent or empty entry means "not entered" — never coerced to 0. */
data class RoundEntryContent(
    val participants: List<RoundEntryParticipant>,
    val scores: Map<UUID, String> = emptyMap(),
    val totalPointsPerRound: Int,
    val precomputedParticipantId: UUID? = null,
    /** Points of the suit seven when the game records its holder (Papayoo : 40), else null. */
    val sevenPoints: Int? = null,
    val sevenHolderId: UUID? = null
) {
    /** The chosen holder's entered score is below the seven's own points: impossible. */
    val isSevenHolderScoreTooLow: Boolean
        get() {
            val points = sevenPoints ?: return false
            val holderScore = sevenHolderId?.let { scores[it]?.toIntOrNull() } ?: return false
            return holderScore < points
        }

    private val isSevenHolderValid: Boolean
        get() = sevenPoints == null ||
            (sevenHolderId != null && scores[sevenHolderId]?.toIntOrNull() != null && !isSevenHolderScoreTooLow)

    val currentSum: Long get() = scores.values.sumOf { it.toIntOrNull()?.toLong() ?: 0L }

    val hasAnyScoreEntered: Boolean get() = scores.values.any { it.isNotEmpty() }

    val allFilled: Boolean
        get() = participants.all { participant -> !scores[participant.id].isNullOrEmpty() }

    val sumIndicatorState: SumIndicatorState
        get() = when {
            currentSum > totalPointsPerRound -> SumIndicatorState.ERROR
            allFilled && currentSum == totalPointsPerRound.toLong() -> SumIndicatorState.SUCCESS
            else -> SumIndicatorState.NEUTRAL
        }

    /** A negative precomputed complement can still reach the exact total; it is never valid. */
    val canValidate: Boolean
        get() = allFilled &&
            sumIndicatorState == SumIndicatorState.SUCCESS &&
            participants.all { (scores[it.id]?.toIntOrNull() ?: -1) >= 0 } &&
            isSevenHolderValid

    /** Integer scores keyed by participant, or null while any draft is missing or not an Int. */
    fun toScores(): Map<UUID, Int>? = participants.associate { participant ->
        participant.id to (scores[participant.id]?.toIntOrNull() ?: return null)
    }
}

enum class SumIndicatorState {
    NEUTRAL,
    SUCCESS,
    ERROR
}

data class RoundEntryParticipant(
    val id: UUID,
    val name: String
)
