package com.rvetiveau.rvscorecount.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.rvetiveau.rvscorecount.ui.screens.history.HistoryRoute
import com.rvetiveau.rvscorecount.ui.screens.home.HomeRoute
import com.rvetiveau.rvscorecount.ui.screens.party.PartyRoute
import com.rvetiveau.rvscorecount.ui.screens.partydetail.PartyDetailRoute
import com.rvetiveau.rvscorecount.ui.screens.results.ResultsRoute
import com.rvetiveau.rvscorecount.ui.screens.round.RoundEntryRoute
import com.rvetiveau.rvscorecount.ui.screens.setup.SetupRoute
import java.util.UUID

/** Single NavHost registering every destination (AD-6). */
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
            RoundEntryRoute(
                partyId = UUID.fromString(route.partyId),
                onNavigate = { event -> navController.dispatch(event) }
            )
        }
        composable<Routes.Results> { backStackEntry ->
            val route: Routes.Results = backStackEntry.toRoute()
            ResultsRoute(
                partyId = UUID.fromString(route.partyId),
                onNavigate = { event -> navController.dispatch(event) }
            )
        }
        composable<Routes.History> {
            HistoryRoute(onNavigate = { event -> navController.dispatch(event) })
        }
        composable<Routes.PartyDetail> { backStackEntry ->
            val route: Routes.PartyDetail = backStackEntry.toRoute()
            PartyDetailRoute(
                partyId = UUID.fromString(route.partyId),
                onNavigate = { event -> navController.dispatch(event) }
            )
        }
    }
}

/** Only this file may hold a NavController; screens emit [NavigationEvent] instead (AD-4). */
private fun NavHostController.dispatch(event: NavigationEvent) {
    when (event) {
        is NavigationEvent.NavigateTo -> navigate(event.route) { launchSingleTop = true }
        NavigationEvent.NavigateBack -> navigateUp()
        is NavigationEvent.NavigateBackTo -> popBackStack(route = event.route, inclusive = event.inclusive)
        is NavigationEvent.ReplaceAll -> navigate(event.route) {
            popUpTo(graph.id) { inclusive = true }
            launchSingleTop = true
        }
    }
}
