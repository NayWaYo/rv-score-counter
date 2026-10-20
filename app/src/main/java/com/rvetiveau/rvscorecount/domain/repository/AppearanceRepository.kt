package com.rvetiveau.rvscorecount.domain.repository

import kotlinx.coroutines.flow.StateFlow

/** The theme picked with the light/dark switch, kept across launches. */
interface AppearanceRepository {
    /** True for dark, false for light; null until a theme is picked, so the phone's setting applies. */
    val darkTheme: StateFlow<Boolean?>

    fun setDarkTheme(dark: Boolean)
}
