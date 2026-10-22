package com.rvetiveau.rvscorecount.ui.screens.round

import com.rvetiveau.rvscorecount.domain.model.CardSuit
import com.rvetiveau.rvscorecount.ui.components.StandingRow
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
    val saveError: Boolean = false,
    /** Section shown under the die: none, the Partie's Classement or the score form. */
    val panel: RoundEntryPanel = RoundEntryPanel.NONE,
    /** Number of the validated Manche being corrected; null for a new Manche. Its suit is fixed. */
    val correctedRoundNumber: Int? = null,
    /** The photo of a Participant's trumps being read or confirmed (Story 9.1); null otherwise. */
    val scan: TrumpScan? = null
) {
    val isCorrection: Boolean get() = correctedRoundNumber != null

    val currentSum: Long get() = content?.currentSum ?: 0L

    /** Locked out as soon as any field holds a value, including "0" (AC 8). */
    val rerollEnabled: Boolean get() = content != null && !isCorrection && !content.hasAnyScoreEntered

    /** The first roll stays available after score entry (Story 3.4, AC 8); only « Relancer » locks. */
    val rollEnabled: Boolean
        get() = content != null && !isCorrection && !isSaving && (suit == null || rerollEnabled)

    val canValidate: Boolean get() = suit != null && !isSaving && content?.canValidate == true
}

/** One row per Participant ordered by listPosition (AC 1). [scores] holds the raw text draft
 * keyed by participant id; an absent or empty entry means "not entered" — never coerced to 0.
 * [suggestion] is never written into [scores]: it only stands in for its still-empty field. */
data class RoundEntryContent(
    val participants: List<RoundEntryParticipant>,
    val scores: Map<UUID, String> = emptyMap(),
    val totalPointsPerRound: Int,
    val suggestion: ScoreSuggestion? = null,
    /** Points of the suit seven when the game records its holder (Papayoo : 40), else null. */
    val sevenPoints: Int? = null,
    val sevenHolderId: UUID? = null,
    val summary: RoundEntrySummary = RoundEntrySummary(),
    /** Cards worth their own number that a photo can count (Papayoo : 1 to 20); null hides the scan. */
    val faceValueCards: IntRange? = null,
    /** Trumps confirmed from a photo, by Participant; a score typed by hand drops its entry. */
    val scannedTrumps: Map<UUID, Set<Int>> = emptyMap()
) {
    /** Trumps already confirmed for another Participant than [participantId], with that Participant's name. */
    fun trumpsTakenByOthers(participantId: UUID): Map<Int, String> =
        participants.filter { it.id != participantId }.flatMap { other ->
            scannedTrumps[other.id].orEmpty().map { trump -> trump to other.name }
        }.toMap()

    /** The entered drafts plus the suggestion in its empty field: what « Valider la Manche » saves,
     * since the complement is the only score that reaches the total. */
    private val effectiveScores: Map<UUID, String>
        get() = suggestion
            ?.takeIf { scores[it.participantId].isNullOrEmpty() }
            ?.let { scores + (it.participantId to it.score.toString()) }
            ?: scores

    /** The chosen holder's score is below the seven's own points: impossible. */
    val isSevenHolderScoreTooLow: Boolean
        get() {
            val points = sevenPoints ?: return false
            val holderScore = sevenHolderId?.let { effectiveScores[it]?.toIntOrNull() } ?: return false
            return holderScore < points
        }

    private val isSevenHolderValid: Boolean
        get() = sevenPoints == null ||
            (sevenHolderId != null && effectiveScores[sevenHolderId]?.toIntOrNull() != null && !isSevenHolderScoreTooLow)

    val currentSum: Long get() = effectiveScores.values.sumOf { it.toIntOrNull()?.toLong() ?: 0L }

    val hasAnyScoreEntered: Boolean get() = scores.values.any { it.isNotEmpty() }

    val allFilled: Boolean
        get() = participants.all { participant -> !effectiveScores[participant.id].isNullOrEmpty() }

    val sumIndicatorState: SumIndicatorState
        get() = when {
            currentSum > totalPointsPerRound -> SumIndicatorState.ERROR
            allFilled && currentSum == totalPointsPerRound.toLong() -> SumIndicatorState.SUCCESS
            else -> SumIndicatorState.NEUTRAL
        }

    val canValidate: Boolean
        get() = allFilled &&
            sumIndicatorState == SumIndicatorState.SUCCESS &&
            participants.all { (effectiveScores[it.id]?.toIntOrNull() ?: -1) >= 0 } &&
            isSevenHolderValid

    /** Integer scores keyed by participant, the suggestion included, or null while any is missing. */
    fun toScores(): Map<UUID, Int>? = participants.associate { participant ->
        participant.id to (effectiveScores[participant.id]?.toIntOrNull() ?: return null)
    }
}

/**
 * A Participant's trumps read from a photo, to be corrected then confirmed (Story 9.1). While
 * [isReading], [selected] is empty; [readFailed] means the photo could not be read at all. [score]
 * is what confirming writes: the trumps, plus the 7's points when [includesSeven].
 */
data class TrumpScan(
    val participantId: UUID,
    val isReading: Boolean = true,
    val selected: Set<Int> = emptySet(),
    val score: Int = 0,
    val includesSeven: Boolean = false,
    val readFailed: Boolean = false,
    val nothingRecognized: Boolean = false
)

/** The score that completes the total, offered in the only field still empty. Never negative. */
data class ScoreSuggestion(
    val participantId: UUID,
    val score: Int
)

enum class SumIndicatorState {
    NEUTRAL,
    SUCCESS,
    ERROR
}

data class RoundEntryParticipant(
    val id: UUID,
    val name: String
)

enum class RoundEntryPanel {
    NONE,
    STANDINGS,
    SCORES
}

/**
 * The Partie so far, read once when the screen loads: Classement with the Donneur badge. Its
 * statistics and Manches are on the Statistiques screen.
 */
data class RoundEntrySummary(
    val standings: List<StandingRow> = emptyList()
)
