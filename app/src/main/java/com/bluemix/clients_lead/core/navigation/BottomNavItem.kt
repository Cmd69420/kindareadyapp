package com.bluemix.clients_lead.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Bottom navigation configuration with outlined and filled icon variants
 */
data class BottomNavItem(
    val route: Route,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val title: String
)

val bottomNavItems = listOf(
    BottomNavItem(
        route = Route.Map,
        icon = Icons.Outlined.Map,
        selectedIcon = Icons.Filled.Map,
        title = "Map"
    ),
    BottomNavItem(
        route = Route.Clients,
        icon = Icons.Outlined.People,
        selectedIcon = Icons.Filled.People,
        title = "Clients"
    ),
    BottomNavItem(
        route = Route.Activity,
        icon = Icons.Outlined.Timeline,
        selectedIcon = Icons.Filled.Timeline,
        title = "Activity"
    ),
    BottomNavItem(
        route = Route.Profile,
        icon = Icons.Outlined.Person,
        selectedIcon = Icons.Filled.Person,
        title = "Profile"
    )
)