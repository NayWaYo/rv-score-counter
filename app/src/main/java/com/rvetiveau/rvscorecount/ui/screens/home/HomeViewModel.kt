package com.rvetiveau.rvscorecount.ui.screens.home

import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.domain.engine.GameIds
import com.rvetiveau.rvscorecount.domain.usecase.GetAvailableGamesUseCase
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

    // Presentation lives in the UI layer; an unmapped registered gameId must fail loudly, not disappear.
    private fun String.toTileUiModel(): GameTileUiModel = when (this) {
        GameIds.PAPAYOO -> GameTileUiModel(
            gameId = this,
            nameRes = R.string.game_name_papayoo,
            logoRes = R.drawable.ic_papayoo_logo
        )
        else -> error("No Home presentation registered for gameId '$this'")
    }
}
