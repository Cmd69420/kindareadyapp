package com.bluemix.clients_lead.core.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import ui.AppTheme
import ui.components.Scaffold
import ui.components.Text

/**
 * Main scaffold with bottom navigation bar for tab destinations.
 * Optimized with memoization to prevent unnecessary recompositions.
 */
@Composable
fun MainScaffold(
    currentRoute: Route,
    navigationManager: NavigationManager,
    content: @Composable () -> Unit
) {
    // Memoize bottom bar state to prevent recreation on every recomposition
    val bottomBarState = remember(currentRoute) {
        bottomNavItems.map { item ->
            BottomNavState(
                item = item,
                isSelected = when (currentRoute) {
                    Route.Map -> item.route == Route.Map
                    Route.Clients -> item.route == Route.Clients
                    Route.Activity -> item.route == Route.Activity
                    Route.Profile -> item.route == Route.Profile
                    else -> false
                }
            )
        }
    }

    Scaffold(
        modifier = Modifier.background(AppTheme.colors.background),
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            NavigationBar(containerColor = AppTheme.colors.background) {
                bottomBarState.forEach { state ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                tint = if (state.isSelected) {
                                    AppTheme.colors.primary
                                } else {
                                    AppTheme.colors.text.copy(alpha = 0.6f)
                                },
                                imageVector = state.item.icon,
                                contentDescription = state.item.title
                            )
                        },
                        label = {
                            Text(
                                text = state.item.title,
                                style = AppTheme.typography.label3
                            )
                        },
                        selected = state.isSelected,
                        onClick = {
                            if (state.isSelected) return@NavigationBarItem
                            navigationManager.navigateToTab(state.item.route)
                        }
                    )
                }
            }
        }
    ) { padding ->
        Surface(Modifier.padding(padding)) {
            content()
        }
    }
}

/**
 * Data class to hold bottom nav item state
 */
private data class BottomNavState(
    val item: BottomNavItem,
    val isSelected: Boolean
)
