package com.rvetiveau.rvscorecount.ui.navigation

/** Startup decision state for the app root (AD-6, FR-16). */
sealed interface RootUiState {
    data object Loading : RootUiState
    data class Ready(val startDestination: Routes) : RootUiState
    data class Error(val throwable: Throwable) : RootUiState
}
