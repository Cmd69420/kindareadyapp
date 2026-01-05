package com.bluemix.clients_lead.features.map.presentation

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.shrinkVertically
import androidx.compose.ui.zIndex
import com.bluemix.clients_lead.domain.model.Client
import com.bluemix.clients_lead.domain.model.VisitStatus
import com.bluemix.clients_lead.features.expense.presentation.MultiLegTripExpenseSheet
import com.bluemix.clients_lead.features.expense.presentation.TripExpenseSheet
import com.bluemix.clients_lead.features.location.LocationSettingsMonitor
import com.bluemix.clients_lead.features.map.vm.MapViewModel
import com.bluemix.clients_lead.features.meeting.presentation.MeetingBottomSheet
import com.bluemix.clients_lead.features.meeting.utils.ProximityDetector
import com.bluemix.clients_lead.features.meeting.vm.MeetingViewModel
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
import timber.log.Timber
import ui.AppTheme
import ui.components.Scaffold
import ui.components.Text
import ui.components.topbar.TopBar
import ui.components.topbar.TopBarDefaults



// Default location (Mumbai, India) - Initial camera position before user location loads
private val DefaultLocation = LatLng(19.0760, 72.8777)

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel = koinViewModel(),
    meetingViewModel: MeetingViewModel = koinViewModel(),
    onNavigateToClientDetail: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val meetingUiState by meetingViewModel.uiState.collectAsState()
    var showMeetingSheet by remember { mutableStateOf(false) }
    var proximityClient by remember { mutableStateOf<Client?>(null) }
    var lastProximityCheck by remember { mutableStateOf(0L) }
    var isRefreshing by remember { mutableStateOf(false) }
    var showExpenseTypeDialog by remember { mutableStateOf(false) }
    var showSingleLegExpense by remember { mutableStateOf(false) }
    var showMultiLegExpense by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var hasAutoFocusedOnUser by remember { mutableStateOf(false) }
    // 🔹 Map legend filter state
    var isLegendExpanded by remember { mutableStateOf(false) }
    var filteredStatuses by remember {
        mutableStateOf(
            setOf(
                VisitStatus.NEVER_VISITED,
                VisitStatus.RECENT,
                VisitStatus.MODERATE,
                VisitStatus.OVERDUE
            )
        )
    }

    val context = LocalContext.current
    val activity = context as? Activity

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(DefaultLocation, 16f)
    }
    BackHandler {
        showExitDialog = true
    }

    val locationPermissions = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )


    val locationSettingsMonitor = remember {
        LocationSettingsMonitor(context)
    }

    DisposableEffect(Unit) {
        locationSettingsMonitor.startMonitoring()
        onDispose {
            locationSettingsMonitor.stopMonitoring()
        }
    }

    val isLocationEnabled by locationSettingsMonitor.isLocationEnabled.collectAsState()

    // Show dialog when location is disabled
    if (!isLocationEnabled && uiState.isTrackingEnabled) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Location Services Disabled") },
            text = {
                Text("Please enable location services from your device settings to continue using the app.")
            },
            confirmButton = {
                Button(onClick = {
                    // Open location settings
                    context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { /* Handle later */ }) {
                    Text("Later")
                }
            }
        )
    }



    LaunchedEffect(Unit) {
        if (!locationPermissions.allPermissionsGranted) {
            locationPermissions.launchMultiplePermissionRequest()
        } else {
            viewModel.refreshTrackingState()
        }
    }
    LaunchedEffect(uiState.currentLocation, uiState.isTrackingEnabled) {
        if (
            uiState.currentLocation != null &&
            uiState.isTrackingEnabled &&
            !hasAutoFocusedOnUser
        ) {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(
                        uiState.currentLocation!!.latitude,
                        uiState.currentLocation!!.longitude
                    ),
                    16.5f // good “where am I” zoom
                ),
                durationMs = 1000
            )
            hasAutoFocusedOnUser = true
        }
    }


    LaunchedEffect(uiState.clients) {
        if (hasAutoFocusedOnUser &&
            uiState.clients.isNotEmpty() &&
            !uiState.isLoading
        ) {
            val firstClientWithLocation = uiState.clients.firstOrNull {
                it.latitude != null && it.longitude != null
            }
            firstClientWithLocation?.let { client ->
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(client.latitude!!, client.longitude!!),
                        16f
                    )
                )
            }
        }
    }

    LaunchedEffect(uiState.currentLocation, uiState.clients) {
        if (uiState.currentLocation != null && uiState.clients.isNotEmpty()) {
            val now = System.currentTimeMillis()
            if (now - lastProximityCheck > 10000) {
                lastProximityCheck = now

                uiState.clients.forEach { client ->
                    val isNewEntry = ProximityDetector.detectProximityEntry(
                        currentLocation = uiState.currentLocation!!,
                        client = client,
                        radiusMeters = 100.0,
                        cooldownMillis = 300000
                    )

                    if (isNewEntry) {
                        Timber.d("User entered proximity of client: ${client.name}")
                        viewModel.selectClient(null) // Close any open client sheet
                        proximityClient = client
                        showMeetingSheet = true
                        meetingViewModel.checkActiveMeeting(client.id)
                    }

                }
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
                // In MapScreen.kt, replace the GoogleMap marker rendering section:

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
                    if (uiState.isTrackingEnabled) {
                        Timber.d("🗺️ RENDERING MARKERS - Total clients: ${uiState.clients.size}")

                        uiState.clients.forEachIndexed { index, client ->
                            if (client.latitude != null && client.longitude != null) {
                                val visitStatus = client.getVisitStatusColor()

                                Timber.d("📍 Marker #$index: ${client.name}")
                                Timber.d("   - ID: ${client.id}")
                                Timber.d("   - Last Visit Date: ${client.lastVisitDate}")
                                Timber.d("   - Visit Status: $visitStatus")
                                Timber.d("   - Filtered: ${filteredStatuses.contains(visitStatus)}")

                                if (filteredStatuses.contains(visitStatus)) {
                                    val position = LatLng(client.latitude, client.longitude)

                                    val markerColor = when (visitStatus) {
                                        VisitStatus.NEVER_VISITED -> {
                                            Timber.d("   - Color: RED")
                                            BitmapDescriptorFactory.HUE_RED
                                        }
                                        VisitStatus.RECENT -> {
                                            Timber.d("   - Color: GREEN")
                                            BitmapDescriptorFactory.HUE_GREEN
                                        }
                                        VisitStatus.MODERATE -> {
                                            Timber.d("   - Color: YELLOW")
                                            BitmapDescriptorFactory.HUE_YELLOW
                                        }
                                        VisitStatus.OVERDUE -> {
                                            Timber.d("   - Color: ORANGE")
                                            BitmapDescriptorFactory.HUE_ORANGE
                                        }
                                    }

                                    val visitInfo = client.getFormattedLastVisit()?.let { "Last visit: $it" }
                                        ?: "Never visited"

                                    val snippet = buildString {
                                        append(visitInfo)
                                        client.address?.let {
                                            append(" • ")
                                            append(it)
                                        }
                                    }

                                    // ✅ Use key to force recomposition when lastVisitDate changes
                                    androidx.compose.runtime.key(client.id, client.lastVisitDate) {
                                        Timber.d("   - Rendering with key: ${client.id}, ${client.lastVisitDate}")

                                        Marker(
                                            state = MarkerState(position = position),
                                            title = client.name,
                                            snippet = snippet,
                                            icon = BitmapDescriptorFactory.defaultMarker(markerColor),
                                            onClick = {
                                                Timber.d("📍 Marker clicked: ${client.name}")
                                                viewModel.selectClient(client)
                                                true
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Timber.d("🗺️ MARKER RENDERING COMPLETE")
                    }
                }

                // Meeting Bottom Sheet - Highest priority (z-index 2)
                AnimatedVisibility(
                    visible = showMeetingSheet && proximityClient != null && uiState.isTrackingEnabled,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .zIndex(2f),
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    proximityClient?.let { client ->
                        MeetingBottomSheet(
                            client = client,
                            activeMeeting = meetingUiState.activeMeeting,
                            isLoading = meetingUiState.isLoading,
                            onStartMeeting = {
                                meetingViewModel.startMeeting(
                                    clientId = client.id,
                                    latitude = uiState.currentLocation?.latitude,
                                    longitude = uiState.currentLocation?.longitude,
                                    accuracy = null
                                )
                            },
                            onEndMeeting = { comments, clientStatus, attachments ->
                                meetingViewModel.endMeeting(comments, clientStatus, attachments)
                                showMeetingSheet = false
                                proximityClient = null
                                ProximityDetector.resetProximityState(client.id)
                            },
                            onDismiss = {
                                showMeetingSheet = false
                                proximityClient = null
                            }

                        )
                    }
                }

                // Client Bottom Sheet - Only shown when meeting sheet is NOT showing
                AnimatedVisibility(
                    visible = uiState.selectedClient != null && uiState.isTrackingEnabled && !showMeetingSheet,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .zIndex(1f),
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    uiState.selectedClient?.let { client ->
                        AnimatedClientBottomSheet(
                            client = client,
                            cameraPositionState = cameraPositionState,
                            onClose = { viewModel.selectClient(null) },
                            onViewDetails = { onNavigateToClientDetail(client.id) },
                            onStartMeeting = {
                                viewModel.selectClient(null)
                                proximityClient = client
                                showMeetingSheet = true
                                meetingViewModel.checkActiveMeeting(client.id)
                            },
                            onQuickVisit = { visitType ->  // ✅ ADD THIS
                                viewModel.updateQuickVisitStatus(client.id, visitType)
                            }



                        )
                    }
                }

                // Error Banner
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

                // Loading Card
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

                // "Tracking Active" indicator
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

                // Enhanced Map Legend - Top Right Position

                AnimatedVisibility(
                    visible = uiState.isTrackingEnabled,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 120.dp, end = 16.dp),  // Changed positioning
                    enter = fadeIn() + slideInHorizontally { it },
                    exit = fadeOut() + slideOutHorizontally { it }
                ) {
                    EnhancedMapLegend(
                        isExpanded = isLegendExpanded,
                        onToggle = { isLegendExpanded = !isLegendExpanded },
                        clientCounts = mapOf(
                            VisitStatus.NEVER_VISITED to uiState.clients.count { it.getVisitStatusColor() == VisitStatus.NEVER_VISITED },
                            VisitStatus.RECENT to uiState.clients.count { it.getVisitStatusColor() == VisitStatus.RECENT },
                            VisitStatus.MODERATE to uiState.clients.count { it.getVisitStatusColor() == VisitStatus.MODERATE },
                            VisitStatus.OVERDUE to uiState.clients.count { it.getVisitStatusColor() == VisitStatus.OVERDUE }
                        ),
                        filteredStatuses = filteredStatuses,
                        onFilterChange = { status ->
                            filteredStatuses = if (filteredStatuses.contains(status)) {
                                filteredStatuses - status
                            } else {
                                filteredStatuses + status
                            }
                        }
                    )
                }

                // Floating Action Button - Changed to Receipt icon
                AnimatedVisibility(
                    visible = uiState.isTrackingEnabled,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 16.dp, end = 16.dp),
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    ui.components.FloatingActionButton(
                        onClick = { showExpenseTypeDialog = true },
                        icon = Icons.Default.Receipt,
                        contentDescription = "Add Trip Expense"
                    )
                    if (showExpenseTypeDialog) {
                        AlertDialog(
                            onDismissRequest = { showExpenseTypeDialog = false },
                            title = { Text("Add Expense") },
                            text = {
                                Column {
                                    TextButton(
                                        onClick = {
                                            showExpenseTypeDialog = false
                                            showSingleLegExpense = true
                                        }
                                    ) {
                                        Text("🚌 Single Trip")
                                    }

                                    TextButton(
                                        onClick = {
                                            showExpenseTypeDialog = false
                                            showMultiLegExpense = true
                                        }
                                    ) {
                                        Text("✈️ Multi-Leg Journey")
                                    }
                                }
                            },
                            confirmButton = {},
                            dismissButton = {
                                TextButton(onClick = { showExpenseTypeDialog = false }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }


                }

                // Permission Prompt
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

                // Full-screen Tracking Warning
                if (!uiState.isTrackingEnabled) {
                    TrackingRequiredOverlay(
                        modifier = Modifier.fillMaxSize(),
                        onEnableTracking = {
                            if (!isLocationEnabled) {
                                context.startActivity(
                                    Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                                )
                            } else {
                                viewModel.enableTracking()
                            }
                        }
                        ,
                        onRefreshStatus = { viewModel.refreshTrackingState() }
                    )
                }
            }
        }

        // Expense Sheet Modal
        if (showSingleLegExpense) {
            TripExpenseSheet(
                onDismiss = { showSingleLegExpense = false }
            )
        }

        if (showMultiLegExpense) {
            MultiLegTripExpenseSheet(
                onDismiss = { showMultiLegExpense = false }
            )
        }

    }

    LaunchedEffect(meetingUiState.error) {
        meetingUiState.error?.let { error ->
            Timber.e("Meeting error: $error")
            meetingViewModel.clearError()
        }
    }

    if (showExitDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = {
                Text(
                    text = "Exit GeoTrack?",
                    style = AppTheme.typography.h3,
                    color = AppTheme.colors.text
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to exit the app?",
                    style = AppTheme.typography.body1,
                    color = AppTheme.colors.textSecondary
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        activity?.finish()
                    }
                ) {
                    Text(
                        text = "Exit",
                        style = AppTheme.typography.button,
                        color = AppTheme.colors.error
                    )
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = { showExitDialog = false }
                ) {
                    Text(
                        text = "Cancel",
                        style = AppTheme.typography.button,
                        color = AppTheme.colors.primary
                    )
                }
            },
            containerColor = AppTheme.colors.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun TrackingRequiredOverlay(
    modifier: Modifier = Modifier,
    onEnableTracking: () -> Unit,
    onRefreshStatus: () -> Unit
) {
    Box(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .padding(top = 120.dp, start = 24.dp, end = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.LocationOff,
                contentDescription = null,
                modifier = Modifier.size(52.dp),
                tint = Color(0xFF5E92F3)
            )

            Text(
                text = "Location tracking required",
                style = AppTheme.typography.h3,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            // Shortened description
            Text(
                text = "Background location access is required to show nearby clients and verify your working area.",
                style = AppTheme.typography.body2,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.fillMaxWidth()
            ) {
                TrackingBenefitItem("Verify you are in the correct service area")
                TrackingBenefitItem("Show clients near your location")
                TrackingBenefitItem("Securely log field visits")
                TrackingBenefitItem("Prevent unauthorized access")
            }

            Button(
                onClick = onEnableTracking,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF5E92F3),
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "Enable Location Tracking",
                    style = AppTheme.typography.button
                )
            }

            OutlinedButton(
                onClick = onRefreshStatus,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                ),
                border = BorderStroke(
                    1.dp,
                    Color.White.copy(alpha = 0.35f)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Refresh tracking status",
                    style = AppTheme.typography.button,
                    color = Color.White
                )
            }


            // Shortened footer
            Text(
                text = "Your location is used only to verify visits and is never shared.",
                style = AppTheme.typography.body2,
                color = Color.White.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun TrackingBenefitItem(text: String) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "•",
            style = AppTheme.typography.body1,
            color = Color.White.copy(alpha = 0.85f),
            modifier = Modifier.padding(end = 8.dp)
        )

        Text(
            text = text,
            style = AppTheme.typography.body2,
            color = Color.White.copy(alpha = 0.75f)
        )
    }
}


    @Composable
    private fun AnimatedClientBottomSheet(
        client: Client,
        cameraPositionState: CameraPositionState,
        onClose: () -> Unit,
        onViewDetails: () -> Unit,
        onStartMeeting: () -> Unit,
        onQuickVisit: (String) -> Unit = {} // New parameter for quick visit status update
    ) {
        var showVisitStatusMenu by remember { mutableStateOf(false) }
        val visitStatus = client.getVisitStatusColor()
        val lastVisit = client.getFormattedLastVisit()

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(AppTheme.colors.surface)
                .padding(20.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header with close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = client.name,
                            style = AppTheme.typography.h3,
                            color = AppTheme.colors.text,
                            fontWeight = FontWeight.Bold
                        )
                        // Visit status badge
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            VisitStatusIndicator(visitStatus)
                            lastVisit?.let {
                                Text(
                                    text = it,
                                    style = AppTheme.typography.body2,
                                    color = AppTheme.colors.textSecondary,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = AppTheme.colors.textSecondary
                        )
                    }
                }

                // Address
                client.address?.let { address ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = AppTheme.colors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = address,
                            style = AppTheme.typography.body2,
                            color = AppTheme.colors.textSecondary,
                            lineHeight = 18.sp
                        )
                    }
                }

                // Divider
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(AppTheme.colors.onSurface.copy(alpha = 0.1f))
                )

                // Action Buttons
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Primary: Start Meeting
                    Button(
                        onClick = onStartMeeting,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Handshake,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Start Meeting",
                            style = AppTheme.typography.button
                        )
                    }

                    // Secondary: Quick Visit Status
                    OutlinedButton(
                        onClick = { showVisitStatusMenu = !showVisitStatusMenu },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = AppTheme.colors.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Mark as Visited",
                            style = AppTheme.typography.button
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(
                            imageVector = if (showVisitStatusMenu) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Quick visit status options
                    AnimatedVisibility(
                        visible = showVisitStatusMenu,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(AppTheme.colors.background)
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            QuickVisitOption(
                                icon = "✅",
                                label = "Client met successfully",
                                onClick = {
                                    onQuickVisit("met_success")
                                    showVisitStatusMenu = false
                                }
                            )
                            QuickVisitOption(
                                icon = "🚫",
                                label = "Client not available",
                                onClick = {
                                    onQuickVisit("not_available")
                                    showVisitStatusMenu = false
                                }
                            )
                            QuickVisitOption(
                                icon = "🏢",
                                label = "Office closed",
                                onClick = {
                                    onQuickVisit("office_closed")
                                    showVisitStatusMenu = false
                                }
                            )
                            QuickVisitOption(
                                icon = "📞",
                                label = "Spoke on phone",
                                onClick = {
                                    onQuickVisit("phone_call")
                                    showVisitStatusMenu = false
                                }
                            )
                        }
                    }

                    // Tertiary actions row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onViewDetails,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Details",
                                style = AppTheme.typography.button,
                                fontSize = 13.sp
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                if (client.latitude != null && client.longitude != null) {
                                    cameraPositionState.move(
                                        CameraUpdateFactory.newLatLngZoom(
                                            LatLng(client.latitude, client.longitude),
                                            17f
                                        )
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Focus",
                                style = AppTheme.typography.button,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun VisitStatusIndicator(status: VisitStatus) {
        val (backgroundColor, textColor, label) = when (status) {
            VisitStatus.NEVER_VISITED -> Triple(
                Color(0xFFEA4335).copy(alpha = 0.15f),
                Color(0xFFEA4335),
                "Never Visited"
            )
            VisitStatus.RECENT -> Triple(
                Color(0xFF34A853).copy(alpha = 0.15f),
                Color(0xFF34A853),
                "Recent"
            )
            VisitStatus.MODERATE -> Triple(
                Color(0xFFFBBC04).copy(alpha = 0.15f),
                Color(0xFFFBBC04),
                "Follow-up"
            )
            VisitStatus.OVERDUE -> Triple(
                Color(0xFFFF6D00).copy(alpha = 0.15f),
                Color(0xFFFF6D00),
                "Overdue"
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(backgroundColor)
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = label,
                style = AppTheme.typography.label2,
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }

    @Composable
    private fun QuickVisitOption(
        icon: String,
        label: String,
        onClick: () -> Unit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = icon,
                fontSize = 18.sp
            )
            Text(
                text = label,
                style = AppTheme.typography.body2,
                color = AppTheme.colors.text
            )
        }
    }
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


@Composable
private fun MapLegend(
    modifier: Modifier = Modifier,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(AppTheme.colors.surface.copy(alpha = 0.95f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Map Legend",
                style = AppTheme.typography.label1,
                color = AppTheme.colors.text,
                fontWeight = FontWeight.Bold
            )
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = AppTheme.colors.textSecondary,
                modifier = Modifier.size(20.dp)
            )
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                LegendItem(
                    color = Color(0xFFEA4335),  // Red
                    label = "Never visited"
                )
                LegendItem(
                    color = Color(0xFF34A853),  // Green
                    label = "Recent (< 7 days)"
                )
                LegendItem(
                    color = Color(0xFFFBBC04),  // Yellow
                    label = "Moderate (7-30 days)"
                )
                LegendItem(
                    color = Color(0xFFFF6D00),  // Orange
                    label = "Overdue (30+ days)"
                )
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = AppTheme.typography.body2,
            color = AppTheme.colors.textSecondary,
            fontSize = 12.sp
        )
    }
}


@Composable
private fun EnhancedMapLegend(
    modifier: Modifier = Modifier,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    clientCounts: Map<VisitStatus, Int>,
    filteredStatuses: Set<VisitStatus>,
    onFilterChange: (VisitStatus) -> Unit
) {
    Column(
        modifier = modifier
            .width(200.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(AppTheme.colors.surface.copy(alpha = 0.98f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = AppTheme.colors.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Legend",
                    style = AppTheme.typography.label1,
                    color = AppTheme.colors.text,
                    fontWeight = FontWeight.Bold
                )
            }
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = AppTheme.colors.textSecondary,
                modifier = Modifier.size(20.dp)
            )
        }

        // Total count badge
        if (!isExpanded) {
            Text(
                text = "${clientCounts.values.sum()} clients",
                style = AppTheme.typography.body2,
                color = AppTheme.colors.textSecondary,
                fontSize = 11.sp
            )
        }

        // Expanded content
        AnimatedVisibility(visible = isExpanded) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Spacer(modifier = Modifier.height(2.dp))

                EnhancedLegendItem(
                    color = Color(0xFFEA4335),
                    label = "Never visited",
                    count = clientCounts[VisitStatus.NEVER_VISITED] ?: 0,
                    isEnabled = filteredStatuses.contains(VisitStatus.NEVER_VISITED),
                    onClick = { onFilterChange(VisitStatus.NEVER_VISITED) }
                )

                EnhancedLegendItem(
                    color = Color(0xFF34A853),
                    label = "Recent visit",
                    count = clientCounts[VisitStatus.RECENT] ?: 0,
                    isEnabled = filteredStatuses.contains(VisitStatus.RECENT),
                    onClick = { onFilterChange(VisitStatus.RECENT) }
                )

                EnhancedLegendItem(
                    color = Color(0xFFFBBC04),
                    label = "Follow-up soon",
                    count = clientCounts[VisitStatus.MODERATE] ?: 0,
                    isEnabled = filteredStatuses.contains(VisitStatus.MODERATE),
                    onClick = { onFilterChange(VisitStatus.MODERATE) }
                )

                EnhancedLegendItem(
                    color = Color(0xFFFF6D00),
                    label = "Overdue",
                    count = clientCounts[VisitStatus.OVERDUE] ?: 0,
                    isEnabled = filteredStatuses.contains(VisitStatus.OVERDUE),
                    onClick = { onFilterChange(VisitStatus.OVERDUE) }
                )
            }
        }
    }
}

@Composable
private fun EnhancedLegendItem(
    color: Color,
    label: String,
    count: Int,
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .background(
                if (isEnabled) Color.Transparent
                else AppTheme.colors.background.copy(alpha = 0.5f)
            )
            .padding(vertical = 4.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(if (isEnabled) color else color.copy(alpha = 0.3f))
            )
            Text(
                text = label,
                style = AppTheme.typography.body2,
                color = if (isEnabled) AppTheme.colors.text else AppTheme.colors.textSecondary.copy(alpha = 0.5f),
                fontSize = 12.sp
            )
        }

        // Count badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(color.copy(alpha = if (isEnabled) 0.15f else 0.05f))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = count.toString(),
                style = AppTheme.typography.label2,
                color = if (isEnabled) color else color.copy(alpha = 0.5f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}