package com.rvetiveau.rvscorecount.ui.screens.setup

import androidx.annotation.StringRes
import com.rvetiveau.rvscorecount.domain.model.suggestPlayerNames
import java.util.UUID

/**
 * In-memory ordered draft of Participants before Party creation (AC 1-6, 9). List order is
 * the single source of truth for the future listPosition and the Manche 1 Donneur; Story 2.3
 * owns turning this draft into a persisted Party. Its finished Parties are one tap away, in the
 * Historique of this Jeu (Story 8.2).
 */
data class SetupUiState(
    val gameId: String = "",
    val minPlayers: Int = 0,
    val maxPlayers: Int = 0,
    val isInitialized: Boolean = false,
    val nameInput: String = "",
    val participants: List<ParticipantDraft> = emptyList(),
    /** Names of earlier Parties, the most recently played first. */
    val knownPlayerNames: List<String> = emptyList(),
    @StringRes val messageRes: Int? = null,
    val isCreatingParty: Boolean = false,
    val creationCompleted: Boolean = false,
    @StringRes val creationErrorRes: Int? = null
) {
    val canAdd: Boolean get() = isInitialized && participants.size < maxPlayers
    val canStart: Boolean
        get() = isInitialized && participants.size >= minPlayers && !isCreatingParty && !creationCompleted
    val isMaxReached: Boolean get() = isInitialized && !canAdd

    /** Known names matching the name being typed, minus those already in the draft. */
    val nameSuggestions: List<String>
        get() = if (canAdd) suggestPlayerNames(knownPlayerNames, nameInput, participants.map { it.name }) else emptyList()
}

/** Pre-confirmation Participant row; distinct from the persistable [com.rvetiveau.rvscorecount.domain.model.Participant]. */
data class ParticipantDraft(
    val id: UUID = UUID.randomUUID(),
    val name: String
)
