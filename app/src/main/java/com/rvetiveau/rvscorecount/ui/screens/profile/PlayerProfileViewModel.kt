package com.rvetiveau.rvscorecount.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rvetiveau.rvscorecount.domain.model.PlayerRecords
import com.rvetiveau.rvscorecount.domain.model.PlayerSummary
import com.rvetiveau.rvscorecount.domain.usecase.DeletePlayerOutcome
import com.rvetiveau.rvscorecount.domain.usecase.DeletePlayerUseCase
import com.rvetiveau.rvscorecount.domain.usecase.GetPlayerRecordsUseCase
import com.rvetiveau.rvscorecount.domain.usecase.MergePlayersOutcome
import com.rvetiveau.rvscorecount.domain.usecase.MergePlayersUseCase
import com.rvetiveau.rvscorecount.domain.usecase.ObservePlayersUseCase
import com.rvetiveau.rvscorecount.ui.navigation.NavigationEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface PlayerProfileUiState {
    data object Loading : PlayerProfileUiState
    data object NotFound : PlayerProfileUiState
    data object LoadError : PlayerProfileUiState

    /** [otherPlayers]: whom the Joueur can be merged with (Story 10.4), in the order of the suggestions. */
    data class Loaded(
        val records: PlayerRecords,
        val otherPlayers: List<PlayerSummary> = emptyList(),
        val dialog: ProfileDialog? = null
    ) : PlayerProfileUiState
}

/** The dialog open over the profile (Story 10.4); [failed] means the last write did not go through. */
sealed interface ProfileDialog {
    data object ChooseMergeTarget : ProfileDialog
    data class ConfirmMerge(val target: PlayerSummary, val isSaving: Boolean = false, val failed: Boolean = false) : ProfileDialog
    data class MergeRefused(val target: PlayerSummary, val gameId: String, val playedAt: Long) : ProfileDialog
    data class ConfirmDelete(val isSaving: Boolean = false, val failed: Boolean = false) : ProfileDialog
}

/** A Joueur's profile (Story 10.3), computed when the screen opens and after a merge (Story 10.4). */
@HiltViewModel
class PlayerProfileViewModel @Inject constructor(
    private val getPlayerRecordsUseCase: GetPlayerRecordsUseCase,
    private val observePlayersUseCase: ObservePlayersUseCase,
    private val mergePlayersUseCase: MergePlayersUseCase,
    private val deletePlayerUseCase: DeletePlayerUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<PlayerProfileUiState>(PlayerProfileUiState.Loading)
    val uiState: StateFlow<PlayerProfileUiState> = _uiState.asStateFlow()

    private val _navigationEvents = Channel<NavigationEvent>(Channel.BUFFERED)
    val navigationEvents: Flow<NavigationEvent> = _navigationEvents.receiveAsFlow()

    private var initializedPlayerId: UUID? = null

    /** Idempotent for the same [playerId]: a recomposition never computes the records again. */
    fun initialize(playerId: UUID) {
        if (initializedPlayerId == playerId) return
        initializedPlayerId = playerId
        _uiState.value = PlayerProfileUiState.Loading
        viewModelScope.launch { load(playerId) }
    }

    private suspend fun load(playerId: UUID) {
        _uiState.value = try {
            getPlayerRecordsUseCase(playerId)?.let { records ->
                PlayerProfileUiState.Loaded(records, observePlayersUseCase().first().filter { it.id != playerId })
            } ?: PlayerProfileUiState.NotFound
        } catch (cancellation: CancellationException) {
            initializedPlayerId = null
            throw cancellation
        } catch (_: Exception) {
            PlayerProfileUiState.LoadError
        }
    }

    fun onMergeRequested() = openDialog { state -> ProfileDialog.ChooseMergeTarget.takeIf { state.otherPlayers.isNotEmpty() } }

    fun onMergeTargetSelected(targetId: UUID) = openDialog { state ->
        state.otherPlayers.firstOrNull { it.id == targetId }?.let { ProfileDialog.ConfirmMerge(it) }
    }

    /** Only a Joueur without any Partie left can be deleted. */
    fun onDeleteRequested() = openDialog { state ->
        ProfileDialog.ConfirmDelete().takeIf { state.records.player.lastPlayedAt == null }
    }

    fun onDialogDismissed() {
        updateLoaded { state ->
            val saving = (state.dialog as? ProfileDialog.ConfirmMerge)?.isSaving == true ||
                (state.dialog as? ProfileDialog.ConfirmDelete)?.isSaving == true
            if (saving) state else state.copy(dialog = null)
        }
    }

    fun onMergeConfirmed() {
        val state = _uiState.value as? PlayerProfileUiState.Loaded ?: return
        val dialog = state.dialog as? ProfileDialog.ConfirmMerge ?: return
        if (dialog.isSaving) return
        val playerId = state.records.player.id
        updateLoaded { it.copy(dialog = dialog.copy(isSaving = true, failed = false)) }

        viewModelScope.launch {
            val outcome = attempt { mergePlayersUseCase(playerId, dialog.target.id) }
            when (outcome) {
                MergePlayersOutcome.Merged -> load(playerId)
                is MergePlayersOutcome.PlayedTogether -> updateLoaded {
                    it.copy(dialog = ProfileDialog.MergeRefused(dialog.target, outcome.gameId, outcome.playedAt))
                }
                MergePlayersOutcome.UnknownPlayer, MergePlayersOutcome.SamePlayer, null -> updateLoaded {
                    it.copy(dialog = dialog.copy(isSaving = false, failed = true))
                }
            }
        }
    }

    fun onDeleteConfirmed() {
        val state = _uiState.value as? PlayerProfileUiState.Loaded ?: return
        val dialog = state.dialog as? ProfileDialog.ConfirmDelete ?: return
        if (dialog.isSaving) return
        updateLoaded { it.copy(dialog = dialog.copy(isSaving = true, failed = false)) }

        viewModelScope.launch {
            when (attempt { deletePlayerUseCase(state.records.player.id) }) {
                DeletePlayerOutcome.Deleted -> _navigationEvents.send(NavigationEvent.NavigateBack)
                DeletePlayerOutcome.HasParties, DeletePlayerOutcome.UnknownPlayer, null -> updateLoaded {
                    it.copy(dialog = ProfileDialog.ConfirmDelete(failed = true))
                }
            }
        }
    }

    /** The outcome of [block], or null when it failed; a cancellation still cancels. */
    private suspend fun <T> attempt(block: suspend () -> T): T? = try {
        block()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Exception) {
        null
    }

    private fun openDialog(dialogFor: (PlayerProfileUiState.Loaded) -> ProfileDialog?) {
        updateLoaded { state -> dialogFor(state)?.let { state.copy(dialog = it) } ?: state }
    }

    private fun updateLoaded(transform: (PlayerProfileUiState.Loaded) -> PlayerProfileUiState.Loaded) {
        _uiState.update { state -> if (state is PlayerProfileUiState.Loaded) transform(state) else state }
    }
}
