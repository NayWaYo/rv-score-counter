package com.rvetiveau.rvscorecount.ui.screens.setup

import androidx.annotation.StringRes
import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.domain.model.duplicatePlayerName

/** Adding a name to the Participants of a Partie being created: the new list, or why it is refused. */
sealed interface DraftAddition {
    data class Added(val participants: List<ParticipantDraft>) : DraftAddition
    data class Refused(@StringRes val messageRes: Int) : DraftAddition
}

/**
 * [name] trimmed joins the end of the list, shared by the Papayoo and the Jeu libre (Story 11.2): a
 * blank name, a name already seated (case and spaces ignored) or a full table is refused.
 */
fun List<ParticipantDraft>.withAdded(name: String, maxPlayers: Int): DraftAddition {
    val trimmed = name.trim()
    return when {
        trimmed.isEmpty() -> DraftAddition.Refused(R.string.setup_error_empty_name)
        duplicatePlayerName(map { it.name } + trimmed) != null -> DraftAddition.Refused(R.string.setup_error_duplicate_name)
        size >= maxPlayers -> DraftAddition.Refused(R.string.setup_max_players_reached)
        else -> DraftAddition.Added(this + ParticipantDraft(name = trimmed))
    }
}

/** The list with the Participant at [fromIndex] moved to [toIndex]; null for a no-op or out-of-range move. */
fun List<ParticipantDraft>.moved(fromIndex: Int, toIndex: Int): List<ParticipantDraft>? {
    if (fromIndex == toIndex || fromIndex !in indices || toIndex !in indices) return null
    return toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
}
