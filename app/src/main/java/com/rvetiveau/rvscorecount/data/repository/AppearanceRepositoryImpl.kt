package com.rvetiveau.rvscorecount.data.repository

import android.content.Context
import com.rvetiveau.rvscorecount.domain.repository.AppearanceRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Stores the picked theme in SharedPreferences. Read once, synchronously, when created: the first
 * frame already has the right theme instead of flashing the other one.
 */
@Singleton
class AppearanceRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context
) : AppearanceRepository {

    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    private val _darkTheme = MutableStateFlow(
        if (preferences.contains(DARK_THEME_KEY)) preferences.getBoolean(DARK_THEME_KEY, true) else null
    )
    override val darkTheme: StateFlow<Boolean?> = _darkTheme.asStateFlow()

    override fun setDarkTheme(dark: Boolean) {
        preferences.edit().putBoolean(DARK_THEME_KEY, dark).apply()
        _darkTheme.value = dark
    }

    private companion object {
        const val PREFERENCES_NAME = "appearance"
        const val DARK_THEME_KEY = "dark_theme"
    }
}
