package com.bluemix.clients_lead.features.settings.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bluemix.clients_lead.features.settings.vm.ProfileViewModel
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel
import ui.AppTheme
import ui.components.Button
import ui.components.ButtonVariant
import ui.components.Scaffold
import ui.components.Switch
import ui.components.Text
import ui.components.topbar.TopBar
import ui.components.topbar.TopBarDefaults
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = koinViewModel(),
    onNavigateToAuth: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.background(AppTheme.colors.background),
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopBar(
                colors = TopBarDefaults.topBarColors(
                    containerColor = AppTheme.colors.background,
                    scrolledContainerColor = AppTheme.colors.surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Profile",
                        style = AppTheme.typography.h2,
                        color = AppTheme.colors.text
                    )
                }
            }
        }
    ) { paddingValues ->
        Crossfade(
            targetState = when {
                uiState.isLoading -> "loading"
                uiState.error != null -> "error"
                uiState.profile != null -> "content"
                else -> "empty"
            },
            animationSpec = tween(300),
            label = "profileCrossfade"
        ) { state ->
            when (state) {
                "loading" -> LoadingContent(paddingValues)
                "error" -> ErrorContent(
                    paddingValues = paddingValues,
                    error = uiState.error ?: "Unknown error",
                    onRetry = { viewModel.refresh() }
                )
                "content" -> AnimatedProfileContent(
                    paddingValues = paddingValues,
                    uiState = uiState,
                    onToggleTracking = viewModel::toggleLocationTracking,
                    onSignOutClick = { showLogoutDialog = true }
                )
            }
        }
    }

    // Animated Logout Dialog
    AnimatedLogoutDialog(
        show = showLogoutDialog,
        onDismiss = { showLogoutDialog = false },
        onConfirm = {
            showLogoutDialog = false
            viewModel.handleSignOut(onNavigateToAuth)
        }
    )
}

@Composable
private fun LoadingContent(paddingValues: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(color = AppTheme.colors.primary)
            Text(
                text = "Loading profile...",
                style = AppTheme.typography.body1,
                color = AppTheme.colors.textSecondary
            )
        }
    }
}

@Composable
private fun ErrorContent(
    paddingValues: PaddingValues,
    error: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = AppTheme.colors.error
            )
            Text(
                text = error,
                style = AppTheme.typography.body1,
                color = AppTheme.colors.error,
                textAlign = TextAlign.Center
            )
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

@Composable
private fun AnimatedProfileContent(
    paddingValues: PaddingValues,
    uiState: com.bluemix.clients_lead.features.settings.vm.ProfileUiState,
    onToggleTracking: (Boolean) -> Unit,
    onSignOutClick: () -> Unit
) {
    val profile = uiState.profile ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Animated User Info Card with Gradient
        AnimatedUserCard(
            fullName = profile.fullName ?: profile.email ?: "User",
            userId = profile.userId
        )

        // NEW: Total Expenses Card
        AnimatedTotalExpenseCard(
            totalSpent = uiState.totalSpent
        )

        // Animated Settings Section
        AnimatedSection(title = "Settings", index = 1) {
            AnimatedTrackingToggle(
                isEnabled = uiState.isTrackingEnabled,
                onToggle = onToggleTracking
            )
        }

        // Animated Account Section
        AnimatedSection(title = "Account", index = 2) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AnimatedProfileMenuItem(
                    icon = Icons.Default.Email,
                    title = "Email",
                    subtitle = profile.email ?: "N/A",
                    index = 0
                )

                profile.department?.let {
                    AnimatedProfileMenuItem(
                        icon = Icons.Default.Business,
                        title = "Department",
                        subtitle = it,
                        index = 1
                    )
                }

                if (profile.workHoursStart != null && profile.workHoursEnd != null) {
                    AnimatedProfileMenuItem(
                        icon = Icons.Default.AccessTime,
                        title = "Work Hours",
                        subtitle = "${profile.workHoursStart} - ${profile.workHoursEnd}",
                        index = 2
                    )
                }

                AnimatedProfileMenuItem(
                    icon = Icons.Default.DateRange,
                    title = "Member Since",
                    subtitle = formatDate(profile.createdAt),
                    index = 3
                )
            }
        }

        // Animated Danger Zone
        AnimatedDangerZone(onSignOutClick = onSignOutClick)

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun AnimatedUserCard(fullName: String, userId: String) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100)
        isVisible = true
    }

    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "userCardScale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(600),
        label = "userCardAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .graphicsLayer { this.alpha = alpha }
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        AppTheme.colors.primary.copy(alpha = 0.15f),
                        AppTheme.colors.primary.copy(alpha = 0.05f)
                    )
                )
            )
            .padding(28.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Animated Avatar with Pulse
            val infiniteTransition = rememberInfiniteTransition(label = "avatarPulse")
            val avatarScale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.08f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = EaseInOut),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "avatarPulse"
            )

            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scale(avatarScale)
                    .clip(CircleShape)
                    .background(AppTheme.colors.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile",
                    modifier = Modifier.size(56.dp),
                    tint = AppTheme.colors.onPrimary
                )
            }

            Text(
                text = fullName,
                style = AppTheme.typography.h1,
                color = AppTheme.colors.text
            )

            // Animated ID Badge
            AnimatedIdBadge(userId = userId)
        }
    }
}

@Composable
private fun AnimatedTotalExpenseCard(totalSpent: Double) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(250)
        isVisible = true
    }

    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.85f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "expenseCardScale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(500),
        label = "expenseCardAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .graphicsLayer { this.alpha = alpha }
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        AppTheme.colors.success.copy(alpha = 0.15f),
                        AppTheme.colors.primary.copy(alpha = 0.1f)
                    )
                )
            )
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Total Expenses",
                    style = AppTheme.typography.body2,
                    color = AppTheme.colors.textSecondary
                )
                Text(
                    text = "₹${String.format("%.2f", totalSpent)}",
                    style = AppTheme.typography.h1.copy(fontSize = 32.sp),
                    color = AppTheme.colors.text
                )
            }

            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(AppTheme.colors.success.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = "Total Expenses",
                    modifier = Modifier.size(32.dp),
                    tint = AppTheme.colors.success
                )
            }
        }
    }
}

@Composable
private fun AnimatedIdBadge(userId: String) {
    var isExpanded by remember { mutableStateOf(false) }

    val width by animateDpAsState(
        targetValue = if (isExpanded) 200.dp else 120.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "badgeWidth"
    )

    Box(
        modifier = Modifier
            .width(width)
            .clip(RoundedCornerShape(12.dp))
            .background(AppTheme.colors.surface.copy(alpha = 0.6f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (isExpanded) userId.take(16) else "ID: ${userId.take(8)}...",
            style = AppTheme.typography.body3,
            color = AppTheme.colors.textSecondary
        )
    }
}

@Composable
private fun AnimatedSection(
    title: String,
    index: Int,
    content: @Composable () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay((index * 100).toLong())
        isVisible = true
    }

    val offsetY by animateDpAsState(
        targetValue = if (isVisible) 0.dp else 20.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "sectionOffset"
    )

    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(400),
        label = "sectionAlpha"
    )

    Column(
        modifier = Modifier
            .offset(y = offsetY)
            .graphicsLayer { this.alpha = alpha },
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            style = AppTheme.typography.h3,
            color = AppTheme.colors.text
        )

        content()
    }
}

@Composable
private fun AnimatedTrackingToggle(
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100)
        isVisible = true
    }

    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.9f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "toggleScale"
    )

    val backgroundColor by animateColorAsState(
        targetValue = if (isEnabled)
            AppTheme.colors.success.copy(alpha = 0.1f)
        else
            AppTheme.colors.surface,
        animationSpec = tween(300),
        label = "toggleBackground"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Animated Icon
                val iconColor by animateColorAsState(
                    targetValue = if (isEnabled) AppTheme.colors.success else AppTheme.colors.primary,
                    animationSpec = tween(300),
                    label = "iconColor"
                )

                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Location",
                    tint = iconColor,
                    modifier = Modifier.size(28.dp)
                )

                Column {
                    Text(
                        text = "Background Location",
                        style = AppTheme.typography.body1,
                        color = AppTheme.colors.text
                    )

                    AnimatedContent(
                        targetState = isEnabled,
                        transitionSpec = {
                            fadeIn() + slideInVertically() togetherWith
                                    fadeOut() + slideOutVertically()
                        },
                        label = "statusText"
                    ) { enabled ->
                        Text(
                            text = if (enabled) "Active" else "Inactive",
                            style = AppTheme.typography.body3,
                            color = if (enabled) AppTheme.colors.success else AppTheme.colors.textSecondary
                        )
                    }
                }
            }

            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle
            )
        }
    }
}

@Composable
private fun AnimatedProfileMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    index: Int
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay((index * 80).toLong())
        isVisible = true
    }

    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.9f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "menuItemScale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(300),
        label = "menuItemAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .graphicsLayer { this.alpha = alpha }
            .clip(RoundedCornerShape(16.dp))
            .background(AppTheme.colors.surface)
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(AppTheme.colors.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = AppTheme.colors.primary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = AppTheme.typography.label2,
                    color = AppTheme.colors.textSecondary
                )
                Text(
                    text = subtitle,
                    style = AppTheme.typography.body1,
                    color = AppTheme.colors.text
                )
            }
        }
    }
}

@Composable
private fun AnimatedDangerZone(onSignOutClick: () -> Unit) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(400)
        isVisible = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(400),
        label = "dangerZoneAlpha"
    )

    Column(
        modifier = Modifier.graphicsLayer { this.alpha = alpha },
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Danger Zone",
            style = AppTheme.typography.h3,
            color = AppTheme.colors.error
        )

        Button(
            variant = ButtonVariant.Destructive,
            onClick = onSignOutClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Sign Out",
                style = AppTheme.typography.button
            )
        }

    }
}

@Composable
private fun AnimatedLogoutDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AnimatedVisibility(
        visible = show,
        enter = fadeIn() + scaleIn(initialScale = 0.8f),
        exit = fadeOut() + scaleOut(targetScale = 0.8f)
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = AppTheme.colors.surface,
            tonalElevation = 0.dp,
            icon = {
                Icon(
                    imageVector = Icons.Default.Logout,
                    contentDescription = null,
                    tint = AppTheme.colors.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Sign Out",
                    style = AppTheme.typography.h3,
                    color = AppTheme.colors.text
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to sign out? Location tracking will be stopped.",
                    style = AppTheme.typography.body1,
                    color = AppTheme.colors.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = onConfirm,
                    variant = ButtonVariant.Destructive
                ) {
                    Text(
                        text = "Sign Out",
                        style = AppTheme.typography.button
                    )
                }
            },
            dismissButton = {
                Button(
                    onClick = onDismiss,
                    variant = ButtonVariant.Secondary
                ) {
                    Text(
                        text = "Cancel",
                        style = AppTheme.typography.button
                    )
                }
            }
        )

    }
}

private fun formatDate(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        val date = inputFormat.parse(dateString)
        outputFormat.format(date ?: Date())
    } catch (e: Exception) {
        "Unknown"
    }
}