package com.rvetiveau.rvscorecount.ui.navigation

import androidx.lifecycle.ViewModel
import com.rvetiveau.rvscorecount.domain.repository.AppearanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

/** The picked theme for the whole app (null: follow the phone) and the switch that flips it. */
@HiltViewModel
class AppearanceViewModel @Inject constructor(
    private val appearanceRepository: AppearanceRepository
) : ViewModel() {

    val darkTheme: StateFlow<Boolean?> = appearanceRepository.darkTheme

    /** Picks the opposite of the theme shown, which may come from the phone's setting. */
    fun onToggle(currentlyDark: Boolean) {
        appearanceRepository.setDarkTheme(!currentlyDark)
    }
}
