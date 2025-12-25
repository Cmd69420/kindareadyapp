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
     * Clears entire back stack including Gate and Auth
     */
    fun navigateToMain() {
        navController.navigate(Route.Map) {
            // ✅ Clear everything including Gate and Auth screens
            popUpTo(0) {
                inclusive = true
            }
            launchSingleTop = true
        }
    }

    /**
     * Navigate to authentication screen
     * Clears entire back stack including any previous user sessions
     */
    fun navigateToAuth() {
        navController.navigate(Route.Auth) {
            // ✅ Clear EVERYTHING - removes all user sessions from stack
            popUpTo(0) {
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
     * Navigate to create client screen
     */
    fun navigateToCreateClient() {
        navController.navigate(Route.CreateClient)
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