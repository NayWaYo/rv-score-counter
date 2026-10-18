package com.rvetiveau.rvscorecount.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.domain.usecase.DeletePartyUseCase
import com.rvetiveau.rvscorecount.domain.usecase.ObservePartyHistoryUseCase
import com.rvetiveau.rvscorecount.ui.navigation.NavigationEvent
import com.rvetiveau.rvscorecount.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * Observes the finished Parties (Story 6.1): deletions and newly finished Parties update the list
 * by themselves. Deletion always goes through a confirmation, then [DeletePartyUseCase].
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val observePartyHistoryUseCase: ObservePartyHistoryUseCase,
    private val deletePartyUseCase: DeletePartyUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<HistoryUiState>(HistoryUiState.Loading)
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private val _navigationEvents = Channel<NavigationEvent>(Channel.BUFFERED)
    val navigationEvents: Flow<NavigationEvent> = _navigationEvents.receiveAsFlow()

    private var observation: Job? = null

    init {
        observe()
    }

    private fun observe() {
        observation?.cancel()
        _uiState.value = HistoryUiState.Loading
        observation = viewModelScope.launch {
            try {
                observePartyHistoryUseCase().collect { entries ->
                    val current = _uiState.value as? HistoryUiState.Loaded
                    _uiState.value = when {
                        entries.isEmpty() -> HistoryUiState.Empty
                        current != null -> current.copy(entries = entries)
                        else -> HistoryUiState.Loaded(entries)
                    }
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Throwable) {
                _uiState.value = HistoryUiState.LoadError
            }
        }
    }

    fun retry() {
        if (_uiState.value is HistoryUiState.LoadError) observe()
    }

    fun onEntrySelected(partyId: UUID) {
        viewModelScope.launch {
            _navigationEvents.send(NavigationEvent.NavigateTo(Routes.PartyDetail(partyId.toString())))
        }
    }

    fun onDeleteRequested(partyId: UUID) {
        val state = _uiState.value as? HistoryUiState.Loaded ?: return
        if (state.isDeleting || state.entries.none { it.partyId == partyId }) return
        _uiState.value = state.copy(pendingDeletionId = partyId, deleteErrorRes = null)
    }

    fun onDeleteDismissed() {
        val state = _uiState.value as? HistoryUiState.Loaded ?: return
        if (state.isDeleting) return
        _uiState.value = state.copy(pendingDeletionId = null)
    }

    fun onDeleteConfirmed() {
        val state = _uiState.value as? HistoryUiState.Loaded ?: return
        val partyId = state.pendingDeletionId ?: return
        if (state.isDeleting) return
        _uiState.value = state.copy(isDeleting = true)

        viewModelScope.launch {
            val result = deletePartyUseCase(partyId)
            // On success the observed list has already dropped (or will drop) the entry.
            when (val current = _uiState.value) {
                is HistoryUiState.Loaded -> _uiState.value = current.copy(
                    pendingDeletionId = null,
                    isDeleting = false,
                    deleteErrorRes = if (result.isSuccess) null else R.string.history_delete_error
                )
                else -> Unit
            }
        }
    }
}
