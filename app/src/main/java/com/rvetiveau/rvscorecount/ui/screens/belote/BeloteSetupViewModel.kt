package com.rvetiveau.rvscorecount.ui.screens.belote

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.domain.usecase.BeloteTeamDraft
import com.rvetiveau.rvscorecount.domain.usecase.CreateBelotePartyUseCase
import com.rvetiveau.rvscorecount.domain.usecase.InvalidPartyConfigurationException
import com.rvetiveau.rvscorecount.ui.navigation.NavigationEvent
import com.rvetiveau.rvscorecount.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Owns the draft of a Partie of Belote and turns it into a persisted Partie. */
@HiltViewModel
class BeloteSetupViewModel @Inject constructor(
    private val createBelotePartyUseCase: CreateBelotePartyUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(BeloteSetupUiState())
    val uiState: StateFlow<BeloteSetupUiState> = _uiState.asStateFlow()

    private val _navigationEvents = Channel<NavigationEvent>(Channel.BUFFERED)
    val navigationEvents: Flow<NavigationEvent> = _navigationEvents.receiveAsFlow()

    /** Prefills the localized default Équipe names once; later calls never reset the draft. */
    fun initialize(defaultTeamNames: List<String>) {
        _uiState.update { state ->
            if (state.isInitialized) state else state.copy(teamNames = defaultTeamNames, isInitialized = true)
        }
    }

    fun onTeamNameChanged(team: Int, name: String) {
        _uiState.update { state ->
            state.copy(teamNames = state.teamNames.replaced(team, name), creationErrorRes = null)
        }
    }

    fun onPlayerNameChanged(team: Int, seat: Int, name: String) {
        _uiState.update { state ->
            val players = state.playerNames.replaced(team, state.playerNames[team].replaced(seat, name))
            state.copy(playerNames = players, creationErrorRes = null)
        }
    }

    fun onEndModeChanged(mode: BeloteEndMode) {
        _uiState.update { it.copy(endMode = mode, creationErrorRes = null) }
    }

    /** Digits only, at most four: the value is checked by [BeloteSetupUiState.endRule]. */
    fun onEndValueChanged(value: String) {
        if (value.length > MAX_END_DIGITS || !value.all(Char::isDigit)) return
        _uiState.update { state ->
            when (state.endMode) {
                BeloteEndMode.TARGET_SCORE -> state.copy(targetScoreInput = value)
                BeloteEndMode.ROUND_COUNT -> state.copy(roundCountInput = value)
            }.copy(creationErrorRes = null)
        }
    }

    fun onStartParty() {
        val state = _uiState.value
        val endRule = state.endRule
        if (!state.canStart || endRule == null) return
        _uiState.update { it.copy(isCreatingParty = true, creationErrorRes = null) }

        viewModelScope.launch {
            try {
                val teams = state.teamNames.zip(state.playerNames) { name, players -> BeloteTeamDraft(name, players) }
                createBelotePartyUseCase(teams, endRule).fold(
                    onSuccess = { partyId ->
                        _uiState.update { it.copy(creationCompleted = true) }
                        _navigationEvents.send(NavigationEvent.ReplaceAll(Routes.BeloteParty(partyId.toString())))
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

    private fun <T> List<T>.replaced(index: Int, value: T): List<T> =
        toMutableList().apply { set(index, value) }

    private companion object {
        const val MAX_END_DIGITS = 4
    }
}
