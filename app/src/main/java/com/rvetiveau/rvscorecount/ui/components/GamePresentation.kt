package com.rvetiveau.rvscorecount.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.domain.engine.GameIds

/** Presentation of a Jeu in the UI layer; an unmapped gameId must fail loudly, not disappear. */
data class GamePresentation(
    @StringRes val nameRes: Int,
    @DrawableRes val logoRes: Int
)

fun gamePresentation(gameId: String): GamePresentation = when (gameId) {
    GameIds.PAPAYOO -> GamePresentation(
        nameRes = R.string.game_name_papayoo,
        logoRes = R.drawable.ic_papayoo_logo
    )
    GameIds.BELOTE -> GamePresentation(
        nameRes = R.string.game_name_belote,
        logoRes = R.drawable.ic_belote_logo
    )
    GameIds.FREE -> GamePresentation(
        nameRes = R.string.game_name_free,
        logoRes = R.drawable.ic_free_logo
    )
    else -> error("No presentation registered for gameId '$gameId'")
}
