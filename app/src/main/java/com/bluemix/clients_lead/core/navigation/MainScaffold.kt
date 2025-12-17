package com.bluemix.clients_lead.core.navigation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ui.AppTheme
import ui.components.Icon
import ui.components.Scaffold
import ui.components.Text

/**
 * Main scaffold with modern bottom navigation bar for tab destinations.
 * Features pill-style selection with gradient backgrounds and smooth animations.
 */
@Composable
fun MainScaffold(
    currentRoute: Route,
    navigationManager: NavigationManager,
    content: @Composable () -> Unit
) {
    Scaffold(
        modifier = Modifier.background(AppTheme.colors.background),
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            ModernBottomNavigation(
                currentRoute = currentRoute,
                navigationManager = navigationManager
            )
        }
    ) { padding ->
        Surface(Modifier.padding(padding)) {
            content()
        }
    }
}

/**
 * Modern Bottom Navigation with pill-style selection
 */
@Composable
private fun ModernBottomNavigation(
    currentRoute: Route,
    navigationManager: NavigationManager
) {
    // Top border gradient
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF334155).copy(alpha = 0.3f), // slate-700
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = 10f
                )
            )
    ) {
        // Main navigation container
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F172A).copy(alpha = 0.95f)) // slate-900
                .padding(horizontal = 4.dp, vertical = 8.dp)
                .navigationBarsPadding(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            bottomNavItems.forEach { item ->
                val isSelected = when (currentRoute) {
                    Route.Map -> item.route == Route.Map
                    Route.Clients -> item.route == Route.Clients
                    Route.Activity -> item.route == Route.Activity
                    Route.Profile -> item.route == Route.Profile
                    else -> false
                }

                ModernNavItem(
                    item = item,
                    isSelected = isSelected,
                    onClick = {
                        if (!isSelected) {
                            navigationManager.navigateToTab(item.route)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ModernNavItem(
    item: BottomNavItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon container with animated background
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (isSelected) {
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF3B82F6).copy(alpha = 0.2f),
                                Color(0xFF3B82F6).copy(alpha = 0.15f)
                            )
                        )
                    } else {
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Transparent
                            )
                        )
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isSelected) item.selectedIcon else item.icon,
                contentDescription = item.title,
                modifier = Modifier.size(24.dp),
                tint = if (isSelected) Color(0xFF60A5FA) else Color(0xFF64748B)
            )
        }

        Spacer(Modifier.height(4.dp))

        // Label
        Text(
            text = item.title,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
            color = if (isSelected) Color(0xFF60A5FA) else Color(0xFF64748B)
        )
    }
}