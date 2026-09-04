package com.rvetiveau.rvscorecount.ui.screens.round

import com.rvetiveau.rvscorecount.domain.model.CardSuit
import java.util.UUID

/**
 * [suit] is null before any roll (Story 3.1, AC 2, 7); no Round exists until Story 3.4 validates.
 * [content] is null while the Partie/Participants aggregate is loading or failed (AC 1, 10):
 * score entry only renders once it is non-null.
 */
data class RoundEntryUiState(
    val suit: CardSuit? = null,
    val content: RoundEntryContent? = null,
    val isLoading: Boolean = true,
    val loadError: Boolean = false
) {
    /** Informative-only in this story (AC 5): no validation or blocking derives from it. */
    val currentSum: Long get() = content?.currentSum ?: 0L

    /** Locked out as soon as any field holds a value, including "0" (AC 8). */
    val rerollEnabled: Boolean get() = content != null && !content.hasAnyScoreEntered
}

/** One row per Participant ordered by listPosition (AC 1). [scores] holds the raw text draft
 * keyed by participant id; an absent or empty entry means "not entered" — never coerced to 0. */
data class RoundEntryContent(
    val participants: List<RoundEntryParticipant>,
    val scores: Map<UUID, String> = emptyMap(),
    val totalPointsPerRound: Int,
    val precomputedParticipantId: UUID? = null
) {
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

    val canValidate: Boolean
        get() = allFilled && sumIndicatorState == SumIndicatorState.SUCCESS
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
