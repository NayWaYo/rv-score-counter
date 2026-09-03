package com.rvetiveau.rvscorecount.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.rvetiveau.rvscorecount.R
import com.rvetiveau.rvscorecount.ui.screens.home.HomeRoute
import com.rvetiveau.rvscorecount.ui.screens.party.PartyRoute
import com.rvetiveau.rvscorecount.ui.screens.round.RoundEntryRoute
import com.rvetiveau.rvscorecount.ui.screens.setup.SetupRoute
import java.util.UUID

/**
 * Single NavHost registering every destination (AD-6, AC-3). Screen bodies are
 * temporary placeholders until epics 2-6 deliver the real screens; only the
 * navigation contract (routes + arguments) is final in this story.
 */
@Composable
fun RvScoreCounterNavHost(
    startDestination: Routes,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable<Routes.Home> {
            HomeRoute(onNavigate = { event -> navController.dispatch(event) })
        }
        composable<Routes.Setup> { backStackEntry ->
            val route: Routes.Setup = backStackEntry.toRoute()
            SetupRoute(gameId = route.gameId, onNavigate = { event -> navController.dispatch(event) })
        }
        composable<Routes.Party> { backStackEntry ->
            val route: Routes.Party = backStackEntry.toRoute()
            PartyRoute(
                partyId = UUID.fromString(route.partyId),
                onNavigate = { event -> navController.dispatch(event) }
            )
        }
        composable<Routes.RoundEntry> { backStackEntry ->
            val route: Routes.RoundEntry = backStackEntry.toRoute()
            RoundEntryRoute(partyId = UUID.fromString(route.partyId))
        }
        composable<Routes.Results> { backStackEntry ->
            val route: Routes.Results = backStackEntry.toRoute()
            RoutePlaceholder(stringResource(R.string.route_placeholder_results, route.partyId))
        }
        composable<Routes.History> {
            RoutePlaceholder(stringResource(R.string.route_placeholder_history))
        }
        composable<Routes.PartyDetail> { backStackEntry ->
            val route: Routes.PartyDetail = backStackEntry.toRoute()
            RoutePlaceholder(stringResource(R.string.route_placeholder_party_detail, route.partyId))
        }
    }
}

@Composable
private fun RoutePlaceholder(label: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = label)
    }
}

/** Only this file may hold a NavController; screens emit [NavigationEvent] instead (AD-4). */
private fun NavHostController.dispatch(event: NavigationEvent) {
    when (event) {
        is NavigationEvent.NavigateTo -> navigate(event.route) { launchSingleTop = true }
        NavigationEvent.NavigateBack -> navigateUp()
        is NavigationEvent.NavigateBackTo -> popBackStack(route = event.route, inclusive = event.inclusive)
    }
}
