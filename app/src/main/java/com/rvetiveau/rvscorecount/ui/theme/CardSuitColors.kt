package com.rvetiveau.rvscorecount.ui.theme

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.domain.model.CardSuit

fun RvColors.suitColor(suit: CardSuit): Color = when (suit) {
    CardSuit.SPADE -> spade
    CardSuit.HEART -> heart
    CardSuit.CLUB -> club
    CardSuit.DIAMOND -> diamond
}

fun RvColors.suitTint(suit: CardSuit): Color = when (suit) {
    CardSuit.SPADE -> spadeTint
    CardSuit.HEART -> heartTint
    CardSuit.CLUB -> clubTint
    CardSuit.DIAMOND -> diamondTint
}

/** The suit's colour in the current theme. */
val CardSuit.color: Color
    @Composable
    @ReadOnlyComposable
    get() = RvTheme.colors.suitColor(this)

/** The suit's background tint in the current theme. */
val CardSuit.tint: Color
    @Composable
    @ReadOnlyComposable
    get() = RvTheme.colors.suitTint(this)

@get:StringRes
val CardSuit.pictogramRes: Int
    get() = when (this) {
        CardSuit.SPADE -> R.string.suit_spade_pictogram
        CardSuit.HEART -> R.string.suit_heart_pictogram
        CardSuit.CLUB -> R.string.suit_club_pictogram
        CardSuit.DIAMOND -> R.string.suit_diamond_pictogram
    }

@get:StringRes
val CardSuit.nameRes: Int
    get() = when (this) {
        CardSuit.SPADE -> R.string.suit_spade
        CardSuit.HEART -> R.string.suit_heart
        CardSuit.CLUB -> R.string.suit_club
        CardSuit.DIAMOND -> R.string.suit_diamond
    }
