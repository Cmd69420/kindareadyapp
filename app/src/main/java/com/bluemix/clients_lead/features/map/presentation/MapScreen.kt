package com.bluemix.clients_lead.features.map.presentation

import android.Manifest
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bluemix.clients_lead.domain.model.Client
import com.bluemix.clients_lead.features.map.vm.MapViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import ui.AppTheme
import ui.components.Scaffold
import ui.components.topbar.TopBar
import ui.components.topbar.TopBarDefaults
import ui.components.Text

/**
 * Map screen displaying clients with geographic locations.
 *
 * Features:
 * - Interactive Google Maps with client markers
 * - Color-coded markers based on client status
 * - Permission handling for location access
 * - Client details bottom sheet
 * - Navigate to client location
 */

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

            // Animated Stats Badge
            AnimatedVisibility(
                visible = !uiState.isLoading && uiState.error == null && uiState.clients.isNotEmpty(),
                modifier = Modifier.align(Alignment.TopCenter),
                enter = slideInVertically() + fadeIn() + expandVertically(),
                exit = slideOutVertically() + fadeOut() + shrinkVertically()
            ) {
                AnimatedStatsCard(clientCount = uiState.clients.size)
            }

            // Enhanced Bottom Sheet
            AnimatedVisibility(
                visible = uiState.selectedClient != null,
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

            // Animated Permission Prompt
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
        }
    }
}

private fun RowScope.mutableStateOf(bool: Boolean) {}

@Composable
private fun AnimatedStatsCard(clientCount: Int) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(300)
        isVisible = true
    }

    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.8f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "statsScale"
    )

    Box(
        modifier = Modifier
            .padding(top = 16.dp)
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        AppTheme.colors.surface,
                        AppTheme.colors.surface.copy(alpha = 0.95f)
                    )
                )
            )
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = AppTheme.colors.primary,
                modifier = Modifier.size(22.dp)
            )

            AnimatedContent(
                targetState = clientCount,
                transitionSpec = {
                    (slideInVertically { it } + fadeIn()) togetherWith
                            (slideOutVertically { -it } + fadeOut())
                },
                label = "clientCount"
            ) { count ->
                Text(
                    text = "$count Client${if (count != 1) "s" else ""}",
                    style = AppTheme.typography.h4,
                    color = AppTheme.colors.text
                )
            }
        }
    }
}

@Composable
private fun AnimatedClientBottomSheet(
    client: Client,
    cameraPositionState: CameraPositionState,
    onClose: () -> Unit,
    onViewDetails: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100)
        isVisible = true
    }

    val offsetY by animateDpAsState(
        targetValue = if (isVisible) 0.dp else 20.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "sheetOffset"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = offsetY)
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(AppTheme.colors.surface)
            .padding(20.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = client.name,
                        style = AppTheme.typography.h2,
                        color = AppTheme.colors.text
                    )

                    // Animated Status Badge
                    AnimatedStatusBadge(status = client.status)
                }

                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = AppTheme.colors.textSecondary
                    )
                }
            }

            // Details Section
            client.address?.let { address ->
                DetailItem(
                    icon = Icons.Default.LocationOn,
                    text = address
                )
            }

            if (client.latitude != null && client.longitude != null) {
                DetailItem(
                    icon = Icons.Default.MyLocation,
                    text = "${String.format("%.4f", client.latitude)}, ${String.format("%.4f", client.longitude)}"
                )
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AnimatedActionButton(
                    icon = Icons.Default.Directions,
                    text = "Navigate",
                    onClick = {
                        if (client.latitude != null && client.longitude != null) {
                            coroutineScope.launch {
                                cameraPositionState.animate(
                                    CameraUpdateFactory.newLatLngZoom(
                                        LatLng(client.latitude, client.longitude),
                                        16f
                                    ),
                                    durationMs = 800
                                )
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                )

                AnimatedActionButton(
                    icon = Icons.Default.Info,
                    text = "Details",
                    onClick = onViewDetails,
                    modifier = Modifier.weight(1f),
                    isPrimary = true
                )
            }
        }
    }
}

@Composable
private fun AnimatedStatusBadge(status: String) {
    val backgroundColor by animateColorAsState(
        targetValue = when (status) {
            "active" -> AppTheme.colors.success.copy(alpha = 0.15f)
            "inactive" -> AppTheme.colors.disabled
            "completed" -> AppTheme.colors.tertiary.copy(alpha = 0.15f)
            else -> AppTheme.colors.surface
        },
        animationSpec = tween(300),
        label = "badgeBackground"
    )

    Box(
        modifier = Modifier
            .padding(top = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(backgroundColor)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = status.uppercase(),
            style = AppTheme.typography.label3,
            color = when (status) {
                "active" -> AppTheme.colors.success
                "inactive" -> AppTheme.colors.textDisabled
                "completed" -> AppTheme.colors.tertiary
                else -> AppTheme.colors.text
            }
        )
    }
}

@Composable
private fun DetailItem(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AppTheme.colors.textSecondary,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = text,
            style = AppTheme.typography.body2,
            color = AppTheme.colors.textSecondary
        )
    }
}

@Composable
private fun AnimatedActionButton(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = false
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "buttonScale"
    )

    Button(
        onClick = onClick,
        modifier = modifier
            .scale(scale)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    }
                )
            },
        colors = if (isPrimary) ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(text, style = AppTheme.typography.button)
    }
}

@Composable
private fun AnimatedPermissionPrompt(onGrant: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(AppTheme.colors.surface)
            .padding(20.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.LocationOff,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = AppTheme.colors.primary
            )
            Text(
                text = "Location permission required",
                style = AppTheme.typography.h4,
                color = AppTheme.colors.text,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Enable location to see your position on the map",
                style = AppTheme.typography.body2,
                color = AppTheme.colors.textSecondary,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onGrant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Grant Permission")
            }
        }
    }
}