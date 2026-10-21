package com.rvetiveau.rvscorecount.ui.screens.home

import com.rvetiveau.rvscorecount.domain.engine.GameIds
import com.rvetiveau.rvscorecount.domain.usecase.GetGameCatalogUseCase
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
    getGameCatalogUseCase: GetGameCatalogUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _navigationEvents = Channel<NavigationEvent>(Channel.BUFFERED)
    val navigationEvents: Flow<NavigationEvent> = _navigationEvents.receiveAsFlow()

    init {
        _uiState.value = HomeUiState(tiles = getGameCatalogUseCase().map { it.toTileUiModel() })
    }

    fun onGameSelected(gameId: String) {
        val route = if (gameId == GameIds.BELOTE) Routes.BeloteSetup else Routes.Setup(gameId)
        viewModelScope.launch { _navigationEvents.send(NavigationEvent.NavigateTo(route)) }
    }

    fun onHistorySelected() {
        viewModelScope.launch { _navigationEvents.send(NavigationEvent.NavigateTo(Routes.History)) }
    }

    private fun String.toTileUiModel(): GameTileUiModel {
        val presentation = gamePresentation(this)
        return GameTileUiModel(gameId = this, nameRes = presentation.nameRes, logoRes = presentation.logoRes)
    }
}
