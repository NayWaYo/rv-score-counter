package com.rvetiveau.rvscorecount.ui.navigation

/**
 * Navigation intents emitted by screen ViewModels toward the navigation layer.
 * Screens never call NavController directly (AD-4); the graph translates these events.
 */
sealed interface NavigationEvent {
    data class NavigateTo(val route: Routes) : NavigationEvent
    data object NavigateBack : NavigationEvent
    data class NavigateBackTo(val route: Routes, val inclusive: Boolean = false) : NavigationEvent

    /** Empties the back stack and shows [route] alone (e.g. Résultats finaux, retour à l'Accueil). */
    data class ReplaceAll(val route: Routes) : NavigationEvent
}
