package com.rvetiveau.rvscorecount.ui.screens.home

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

/** Immutable Home state: the grid of Game tiles resolved from the Hilt engine registry. */
data class HomeUiState(
    val tiles: List<GameTileUiModel> = emptyList()
)

data class GameTileUiModel(
    val gameId: String,
    @StringRes val nameRes: Int,
    @DrawableRes val logoRes: Int
)
