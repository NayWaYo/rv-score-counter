package com.rvetiveau.rvscorecount.ui.screens.tarot

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.domain.engine.GameIds
import com.rvetiveau.rvscorecount.domain.engine.TarotRules
import com.rvetiveau.rvscorecount.domain.model.EndRule
import com.rvetiveau.rvscorecount.domain.model.ParticipantCreationData
import com.rvetiveau.rvscorecount.domain.model.suggestPlayerNames
import com.rvetiveau.rvscorecount.domain.usecase.CreateTarotPartyUseCase
import com.rvetiveau.rvscorecount.domain.usecase.InvalidPartyConfigurationException
import com.rvetiveau.rvscorecount.domain.usecase.ObserveKnownPlayerNamesUseCase
import com.rvetiveau.rvscorecount.domain.usecase.TarotPartyDraft
import com.rvetiveau.rvscorecount.ui.navigation.NavigationEvent
import com.rvetiveau.rvscorecount.ui.navigation.Routes
import com.rvetiveau.rvscorecount.ui.screens.setup.DraftAddition
import com.rvetiveau.rvscorecount.ui.screens.setup.ParticipantDraft
import com.rvetiveau.rvscorecount.ui.screens.setup.moved
import com.rvetiveau.rvscorecount.ui.screens.setup.withAdded
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Draft of a Partie of Tarot (Story 13.3): its Participants in table order and its end. */
data class TarotSetupUiState(
    val minPlayers: Int = 3,
    val maxPlayers: Int = 5,
    val nameInput: String = "",
    val participants: List<ParticipantDraft> = emptyList(),
    /** Names of earlier Parties, the most recently played first. */
    val knownPlayerNames: List<String> = emptyList(),
    @StringRes val messageRes: Int? = null,
    val hasRoundLimit: Boolean = false,
    /** Chosen with − and +, no keyboard: always within 1 to [CreateTarotPartyUseCase.MAX_ROUND_COUNT]. */
    val roundCount: Int = DEFAULT_ROUND_COUNT,
    val isCreatingParty: Boolean = false,
    val creationCompleted: Boolean = false,
    @StringRes val creationErrorRes: Int? = null
) {
    val canAdd: Boolean get() = participants.size < maxPlayers
    val isMaxReached: Boolean get() = !canAdd

    val nameSuggestions: List<String>
        get() = if (canAdd) suggestPlayerNames(knownPlayerNames, nameInput, participants.map { it.name }) else emptyList()

    val canDecreaseRoundCount: Boolean get() = roundCount > 1
    val canIncreaseRoundCount: Boolean get() = roundCount < CreateTarotPartyUseCase.MAX_ROUND_COUNT

    /** Null for an end by hand. */
    val endRule: EndRule.RoundCount? get() = if (hasRoundLimit) EndRule.RoundCount(roundCount) else null

    val canStart: Boolean
        get() = participants.size in minPlayers..maxPlayers && !isCreatingParty && !creationCompleted

    companion object {
        const val DEFAULT_ROUND_COUNT = 10
    }
}

/** Owns the draft of a Partie of Tarot (Story 13.3) and turns it into a persisted Partie. */
@HiltViewModel
class TarotSetupViewModel @Inject constructor(
    private val createTarotPartyUseCase: CreateTarotPartyUseCase,
    rules: TarotRules,
    observeKnownPlayerNamesUseCase: ObserveKnownPlayerNamesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TarotSetupUiState(minPlayers = rules.minPlayers, maxPlayers = rules.maxPlayers))
    val uiState: StateFlow<TarotSetupUiState> = _uiState.asStateFlow()

    private val _navigationEvents = Channel<NavigationEvent>(Channel.BUFFERED)
    val navigationEvents: Flow<NavigationEvent> = _navigationEvents.receiveAsFlow()

    init {
        viewModelScope.launch {
            observeKnownPlayerNamesUseCase().collect { names -> _uiState.update { it.copy(knownPlayerNames = names) } }
        }
    }

    fun onNameChanged(name: String) {
        _uiState.update { it.copy(nameInput = name, messageRes = null, creationErrorRes = null) }
    }

    fun onAddParticipant() = addParticipant(_uiState.value.nameInput)

    /** A tapped known name joins the table at once, whatever was being typed. */
    fun onSuggestionSelected(name: String) = addParticipant(name)

    private fun addParticipant(name: String) {
        when (val addition = _uiState.value.participants.withAdded(name, _uiState.value.maxPlayers)) {
            is DraftAddition.Refused -> _uiState.update { it.copy(messageRes = addition.messageRes) }
            is DraftAddition.Added -> _uiState.update {
                it.copy(participants = addition.participants, nameInput = "", messageRes = null, creationErrorRes = null)
            }
        }
    }

    fun onRemoveParticipant(id: UUID) {
        _uiState.update { state ->
            state.copy(participants = state.participants.filterNot { it.id == id }, messageRes = null, creationErrorRes = null)
        }
    }

    fun onMoveParticipant(fromIndex: Int, toIndex: Int) {
        _uiState.update { state ->
            state.participants.moved(fromIndex, toIndex)?.let { state.copy(participants = it, creationErrorRes = null) } ?: state
        }
    }

    fun onRoundLimitChanged(hasRoundLimit: Boolean) {
        _uiState.update { it.copy(hasRoundLimit = hasRoundLimit, creationErrorRes = null) }
    }

    /** One Manche more ([delta] 1) or less (−1), kept within 1 to [CreateTarotPartyUseCase.MAX_ROUND_COUNT]. */
    fun onRoundCountStep(delta: Int) {
        _uiState.update { state ->
            state.copy(
                roundCount = (state.roundCount + delta).coerceIn(1, CreateTarotPartyUseCase.MAX_ROUND_COUNT),
                creationErrorRes = null
            )
        }
    }

    /** Guards a double tap: [TarotSetupUiState.canStart] excludes an in-flight or completed creation. */
    fun onStartParty() {
        val state = _uiState.value
        if (!state.canStart) return
        _uiState.update { it.copy(isCreatingParty = true, creationErrorRes = null) }

        viewModelScope.launch {
            try {
                val draft = TarotPartyDraft(
                    endRule = state.endRule,
                    participants = state.participants.map { ParticipantCreationData(id = it.id, name = it.name) }
                )
                createTarotPartyUseCase(draft).fold(
                    onSuccess = { partyId ->
                        _uiState.update { it.copy(creationCompleted = true) }
                        _navigationEvents.send(NavigationEvent.ReplaceAll(Routes.TarotParty(partyId.toString())))
                    },
                    onFailure = { failure ->
                        val messageRes = if (failure is InvalidPartyConfigurationException) {
                            R.string.setup_error_invalid_state
                        } else {
                            R.string.setup_error_creation_failed
                        }
                        _uiState.update { it.copy(creationErrorRes = messageRes) }
                    }
                )
            } finally {
                _uiState.update { it.copy(isCreatingParty = false) }
            }
        }
    }

    /** Historique of the Tarot only (Story 8.2); the draft stays below it on the back stack. */
    fun onHistorySelected() {
        viewModelScope.launch { _navigationEvents.send(NavigationEvent.NavigateTo(Routes.GameHistory(GameIds.TAROT))) }
    }
}
