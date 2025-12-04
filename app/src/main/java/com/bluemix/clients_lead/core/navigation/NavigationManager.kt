package com.bluemix.clients_lead.core.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination

/**
 * Centralized navigation logic to decouple navigation from UI.
 * Provides reusable, testable navigation actions.
 */
class NavigationManager(private val navController: NavController) {

    /**
     * Navigate to main authenticated screen (Map)
     * Clears entire back stack
     */
    fun navigateToMain() {
        navController.navigate(Route.Map) {
            popUpTo(Route.Gate) { inclusive = true }
            launchSingleTop = true
        }
    }

    /**
     * Navigate to authentication screen
     * Clears entire back stack
     */
    fun navigateToAuth() {
        navController.navigate(Route.Auth) {
            popUpTo(navController.graph.findStartDestination().id) {
                inclusive = true
            }
            launchSingleTop = true
            restoreState = false
        }
    }

    /**
     * Navigate to client detail screen
     */
    fun navigateToClientDetail(clientId: String) {
        navController.navigate(Route.ClientDetail(clientId))
    }

    /**
     * Navigate to tab destination with state preservation
     */
    fun navigateToTab(route: Route) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    /**
     * Navigate back
     */
    fun navigateBack() {
        navController.popBackStack()
    }

    /**
     * Check if can navigate back
     */
    fun canNavigateBack(): Boolean {
        return navController.previousBackStackEntry != null
    }
}
