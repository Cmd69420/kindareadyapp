package com.bluemix.clients_lead.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Bottom navigation configuration
 */
data class BottomNavItem(
    val route: Route,
    val icon: ImageVector,
    val title: String
)

val bottomNavItems = listOf(
    BottomNavItem(
        route = Route.Map,
        icon = Icons.Default.Map,
        title = "Map"
    ),
    BottomNavItem(
        route = Route.Clients,
        icon = Icons.Default.People,
        title = "Clients"
    ),
    BottomNavItem(
        route = Route.Activity,
        icon = Icons.Default.Timeline,
        title = "Activity"
    ),
    BottomNavItem(
        route = Route.Profile,
        icon = Icons.Default.Person,
        title = "Profile"
    )
)
