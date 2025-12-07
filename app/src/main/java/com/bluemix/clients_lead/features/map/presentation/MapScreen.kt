package com.bluemix.clients_lead.features.map.presentation

import android.Manifest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.setValue
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material.icons.filled.Info
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bluemix.clients_lead.domain.model.Client
import com.bluemix.clients_lead.features.expense.presentation.TripExpenseSheet
import com.bluemix.clients_lead.features.map.vm.MapViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import org.koin.androidx.compose.koinViewModel
import ui.AppTheme
import ui.components.Scaffold
import ui.components.Text
import ui.components.topbar.TopBar
import ui.components.topbar.TopBarDefaults

// Default location (Mumbai, India)
private val DefaultLocation = LatLng(19.0760, 72.8777)

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel = koinViewModel(),
    onNavigateToClientDetail: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var isRefreshing by remember { mutableStateOf(false) }
    var showExpenseSheet by remember { mutableStateOf(false) }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(DefaultLocation, 12f)
    }

    val locationPermissions = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    LaunchedEffect(Unit) {
        if (!locationPermissions.allPermissionsGranted) {
            locationPermissions.launchMultiplePermissionRequest()
        } else {
            // If permissions are already granted, ensure tracking state is synced
            viewModel.refreshTrackingState()
        }
    }

    LaunchedEffect(uiState.clients) {
        if (uiState.clients.isNotEmpty() && !uiState.isLoading) {
            val firstClientWithLocation = uiState.clients.firstOrNull {
                it.latitude != null && it.longitude != null
            }
            firstClientWithLocation?.let { client ->
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(client.latitude!!, client.longitude!!),
                        12f
                    )
                )
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Map",
                            style = AppTheme.typography.h2,
                            color = AppTheme.colors.text
                        )

                        val rotation by animateFloatAsState(
                            targetValue = if (isRefreshing) 360f else 0f,
                            animationSpec = tween(600),
                            finishedListener = { isRefreshing = false },
                            label = "refreshRotation"
                        )

                        IconButton(
                            onClick = {
                                isRefreshing = true
                                viewModel.refresh()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = AppTheme.colors.text,
                                modifier = Modifier.graphicsLayer { rotationZ = rotation }
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(
                        isMyLocationEnabled = locationPermissions.permissions.any { it.status.isGranted },
                        mapType = MapType.NORMAL
                    ),
                    uiSettings = MapUiSettings(
                        zoomControlsEnabled = false,
                        myLocationButtonEnabled = true,
                        compassEnabled = true,
                        mapToolbarEnabled = false
                    )
                ) {
                    // Security: only render markers when tracking is enabled
                    if (uiState.isTrackingEnabled) {
                        uiState.clients.forEach { client ->
                            if (client.latitude != null && client.longitude != null) {
                                val position = LatLng(client.latitude, client.longitude)

                                Marker(
                                    state = MarkerState(position = position),
                                    title = client.name,
                                    snippet = client.address ?: "No address",
                                    icon = BitmapDescriptorFactory.defaultMarker(
                                        when (client.status) {
                                            "active" -> BitmapDescriptorFactory.HUE_GREEN
                                            "inactive" -> BitmapDescriptorFactory.HUE_AZURE
                                            "completed" -> BitmapDescriptorFactory.HUE_BLUE
                                            else -> BitmapDescriptorFactory.HUE_RED
                                        }
                                    ),
                                    onClick = {
                                        viewModel.selectClient(client)
                                        true
                                    }
                                )
                            }
                        }
                    }
                }

                // Animated Error Banner
                AnimatedVisibility(
                    visible = uiState.error != null,
                    modifier = Modifier.align(Alignment.TopCenter),
                    enter = slideInVertically() + fadeIn(),
                    exit = slideOutVertically() + fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .padding(16.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(AppTheme.colors.error)
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = AppTheme.colors.onError,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = uiState.error ?: "Unknown error",
                                style = AppTheme.typography.body2,
                                color = AppTheme.colors.onError
                            )
                        }
                    }
                }

                // Animated Loading Card
                AnimatedVisibility(
                    visible = uiState.isLoading,
                    modifier = Modifier.align(Alignment.Center),
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(AppTheme.colors.surface)
                            .padding(24.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = AppTheme.colors.primary
                            )
                            Text(
                                text = "Loading clients...",
                                style = AppTheme.typography.body1,
                                color = AppTheme.colors.text
                            )
                        }
                    }
                }

                // "Clocked-In" badge
                AnimatedVisibility(
                    visible = uiState.userClockedIn && uiState.isTrackingEnabled,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 70.dp, start = 16.dp),
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(AppTheme.colors.success.copy(alpha = 0.15f))
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Clocked-In",
                            style = AppTheme.typography.label2,
                            color = AppTheme.colors.success
                        )
                    }
                }

                // Small "Tracking Active" indicator (top-right)
                AnimatedVisibility(
                    visible = uiState.isTrackingEnabled,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 70.dp, end = 16.dp),
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(AppTheme.colors.primary.copy(alpha = 0.12f))
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = AppTheme.colors.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Tracking Active",
                                style = AppTheme.typography.label2,
                                color = AppTheme.colors.primary
                            )
                        }
                    }
                }

                // Floating Action Button (bottom-right)
                AnimatedVisibility(
                    visible = uiState.isTrackingEnabled,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 16.dp, end = 16.dp),
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    ui.components.FloatingActionButton(
                        onClick = { showExpenseSheet = true },
                        icon = Icons.Default.Add,
                        contentDescription = "Add Trip Expense"
                    )
                }

                // Bottom Sheet for selected client (only when tracking enabled)
                AnimatedVisibility(
                    visible = uiState.selectedClient != null && uiState.isTrackingEnabled,
                    modifier = Modifier.align(Alignment.BottomCenter),
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    uiState.selectedClient?.let { client ->
                        AnimatedClientBottomSheet(
                            client = client,
                            cameraPositionState = cameraPositionState,
                            onClose = { viewModel.selectClient(null) },
                            onViewDetails = { onNavigateToClientDetail(client.id) }
                        )
                    }
                }

                // Permission Prompt (still shown if permissions missing)
                AnimatedVisibility(
                    visible = !locationPermissions.allPermissionsGranted,
                    modifier = Modifier.align(Alignment.BottomCenter),
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    AnimatedPermissionPrompt(
                        onGrant = { locationPermissions.launchMultiplePermissionRequest() }
                    )
                }

                // =========================
                // Full-screen Tracking Warning
                // =========================
                if (!uiState.isTrackingEnabled) {
                    TrackingRequiredOverlay(
                        modifier = Modifier.fillMaxSize(),
                        onEnableTracking = { viewModel.enableTracking() },
                        onRefreshStatus = { viewModel.refreshTrackingState() }
                    )
                }
            }
        }

        // Expense Sheet Modal
        if (showExpenseSheet) {
            TripExpenseSheet(
                onDismiss = { showExpenseSheet = false }
            )
        }
    }
}

@Composable
private fun TrackingRequiredOverlay(
    modifier: Modifier = Modifier,
    onEnableTracking: () -> Unit,
    onRefreshStatus: () -> Unit
) {
    Box(
        modifier = modifier
            // 95% opacity overlay to block map visibility
            .background(AppTheme.colors.background.copy(alpha = 0.95f))
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.LocationOff,
                contentDescription = null,
                modifier = Modifier.size(52.dp),
                tint = AppTheme.colors.primary
            )

            Text(
                text = "Location tracking required",
                style = AppTheme.typography.h3,
                color = AppTheme.colors.text,
                textAlign = TextAlign.Center
            )

            Text(
                text = "To protect client data and verify that you are in the correct area, background location tracking must remain active while using the map.",
                style = AppTheme.typography.body2,
                color = AppTheme.colors.textSecondary,
                textAlign = TextAlign.Center
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.fillMaxWidth()
            ) {
                TrackingBenefitItem("Verify that you are in the correct service area")
                TrackingBenefitItem("Show clients that are actually near your location")
                TrackingBenefitItem("Securely track field visits for compliance")
                TrackingBenefitItem("Prevent unauthorized access to sensitive client data")
            }

            Button(
                onClick = onEnableTracking,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Text(
                    text = "Enable Location Tracking",
                    style = AppTheme.typography.button
                )
            }

            OutlinedButton(
                onClick = onRefreshStatus,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Refresh tracking status",
                    style = AppTheme.typography.button
                )
            }

            Text(
                text = "We only use your location to verify your working area and log visits. Your data is transmitted securely and never shared with other users.",
                style = AppTheme.typography.body2,
                color = AppTheme.colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun TrackingBenefitItem(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(AppTheme.colors.primary)
        )
        Text(
            text = text,
            style = AppTheme.typography.body2,
            color = AppTheme.colors.textSecondary
        )
    }
}

/**
 * Simple animated bottom sheet for a selected client.
 * Only used when tracking is enabled.
 */
@Composable
private fun AnimatedClientBottomSheet(
    client: Client,
    cameraPositionState: CameraPositionState,
    onClose: () -> Unit,
    onViewDetails: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(AppTheme.colors.surface)
            .padding(16.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = client.name,
                    style = AppTheme.typography.h3,
                    color = AppTheme.colors.text
                )
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = AppTheme.colors.text
                    )
                }
            }

            client.address?.let {
                Text(
                    text = it,
                    style = AppTheme.typography.body2,
                    color = AppTheme.colors.textSecondary
                )
            }

            Spacer(modifier = Modifier.size(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onViewDetails,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "View Details",
                        style = AppTheme.typography.button
                    )
                }

                Button(
                    onClick = {
                        // Center map on this client's location
                        if (client.latitude != null && client.longitude != null) {
                            cameraPositionState.move(
                                CameraUpdateFactory.newLatLngZoom(
                                    LatLng(client.latitude, client.longitude),
                                    16f
                                )
                            )
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Directions,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Focus on Map",
                        style = AppTheme.typography.button
                    )
                }
            }
        }
    }
}

/**
 * Simple permission prompt shown at the bottom when location permissions are missing.
 */
@Composable
private fun AnimatedPermissionPrompt(
    onGrant: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(AppTheme.colors.surface)
            .padding(16.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = null,
                    tint = AppTheme.colors.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Location permission required",
                    style = AppTheme.typography.body1,
                    color = AppTheme.colors.text
                )
            }

            Text(
                text = "Please grant location permission so we can show clients near you and start background tracking.",
                style = AppTheme.typography.body2,
                color = AppTheme.colors.textSecondary
            )

            Button(
                onClick = onGrant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Grant Permission",
                    style = AppTheme.typography.button
                )
            }
        }
    }
}