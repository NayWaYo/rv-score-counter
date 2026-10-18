package com.rvetiveau.rvscorecount.ui.screens.home

import com.rvetiveau.rvscorecount.domain.usecase.GetAvailableGamesUseCase
import com.rvetiveau.rvscorecount.ui.components.gamePresentation
import com.rvetiveau.rvscorecount.ui.navigation.NavigationEvent
import com.rvetiveau.rvscorecount.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import javax.inject.Inject

/** Resolves the Games catalog for the Accueil grid and emits one-shot navigation intents. */
@HiltViewModel
class HomeViewModel @Inject constructor(
    getAvailableGamesUseCase: GetAvailableGamesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _navigationEvents = Channel<NavigationEvent>(Channel.BUFFERED)
    val navigationEvents: Flow<NavigationEvent> = _navigationEvents.receiveAsFlow()

    init {
        _uiState.value = HomeUiState(tiles = getAvailableGamesUseCase().map { it.gameId.toTileUiModel() })
    }

    fun onGameSelected(gameId: String) {
        viewModelScope.launch { _navigationEvents.send(NavigationEvent.NavigateTo(Routes.Setup(gameId))) }
    }

    fun onHistorySelected() {
        viewModelScope.launch { _navigationEvents.send(NavigationEvent.NavigateTo(Routes.History)) }
    }

    private fun String.toTileUiModel(): GameTileUiModel {
        val presentation = gamePresentation(this)
        return GameTileUiModel(gameId = this, nameRes = presentation.nameRes, logoRes = presentation.logoRes)
    }
}
