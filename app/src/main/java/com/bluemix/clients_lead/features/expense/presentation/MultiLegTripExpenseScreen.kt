package com.bluemix.clients_lead.features.expense.presentation

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.ui.unit.offset
import com.bluemix.clients_lead.domain.model.DraftExpense
import com.bluemix.clients_lead.domain.model.toDraftLocation
import com.bluemix.clients_lead.domain.model.toLocationPlace
import com.bluemix.clients_lead.domain.model.toDraftString
import com.bluemix.clients_lead.domain.model.toTransportMode
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.bluemix.clients_lead.core.design.ui.components.textfield.OutlinedTextField
import com.bluemix.clients_lead.domain.model.LocationPlace
import com.bluemix.clients_lead.domain.model.TransportMode
import com.bluemix.clients_lead.features.expense.presentation.components.MiniRouteMap
import com.bluemix.clients_lead.features.expense.vm.MultiLegExpenseViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel
import timber.log.Timber
import ui.AppTheme
import ui.components.Text
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MultiLegTripExpenseSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MultiLegExpenseViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    var showImagePickerDialog by remember { mutableStateOf(false) }
    var currentImageUri by remember { mutableStateOf<Uri?>(null) }

    val cameraPermission = rememberPermissionState(permission = Manifest.permission.CAMERA)

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && currentImageUri != null) {
            viewModel.processAndUploadImage(context, currentImageUri!!)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.processAndUploadImage(context, it) }
    }

    LaunchedEffect(uiState.successMessage) {
        if (uiState.successMessage != null) {
            delay(500)
            onDismiss()
            viewModel.resetSuccess()
        }
    }

    AnimatedVisibility(
        visible = true,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))

        ) {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(top = 60.dp)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(Color(0xFF1A1A1A))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // HEADER
                    MultiLegExpenseSheetHeader(
                        onDismiss = onDismiss,
                        onSaveDraft = { viewModel.saveDraft(showConfirmation = true) },
                        onViewDrafts = { viewModel.toggleDraftsList() },
                        draftCount = uiState.availableDrafts.size,
                        isSubmitting = uiState.isSubmitting,
                        isSaving = uiState.isSaving,
                        lastSaved = uiState.lastSaved
                    )
                    AnimatedVisibility(
                        visible = uiState.successMessage != null,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF4CAF50).copy(alpha = 0.15f))
                                .padding(12.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = uiState.successMessage ?: "",
                                    style = AppTheme.typography.body2,
                                    color = Color(0xFF4CAF50)
                                )
                            }
                        }
                    }



                    // ERROR MESSAGE
                    if (uiState.error != null) {
                        ErrorCard(message = uiState.error!!)
                    }

                    // TRIP NAME
                    OutlinedTextField(
                        value = uiState.tripName,
                        onValueChange = { viewModel.updateTripName(it) },
                        label = { Text("Trip Name", color = Color(0xFFB0B0B0)) },
                        placeholder = {
                            Text(
                                "e.g., Mumbai to London Business Trip",
                                color = Color(0xFF606060)
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Flight, null, tint = Color(0xFF5E92F3))
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // LEG TABS
                    LegTabRow(
                        legs = uiState.legs,
                        currentLegIndex = uiState.currentEditingLegIndex,
                        onLegClick = { viewModel.switchToLeg(it) },
                        onAddLeg = { viewModel.addNewLeg() },
                        onRemoveLeg = { viewModel.removeLeg(it) }
                    )

                    // CURRENT LEG EDITOR
                    val currentLeg = uiState.legs.getOrNull(uiState.currentEditingLegIndex)
                    if (currentLeg != null) {
                        LegEditor(
                            leg = currentLeg,
                            legIndex = uiState.currentEditingLegIndex,
                            viewModel = viewModel,
                            isLoadingLocation = uiState.isLoadingCurrentLocation
                        )
                    }
                    CombinedRoutePreview(legs = uiState.legs)


                    // RECEIPTS
                    SectionCard(title = "Receipts (All Legs)") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            uiState.receiptImages.forEach { uri ->
                                ReceiptImage(uri = uri, onRemove = { viewModel.removeReceipt(uri) })
                            }

                            if (uiState.receiptImages.size < 10) {
                                AddReceiptButton(
                                    onClick = { showImagePickerDialog = true },
                                    isProcessing = uiState.isProcessingImage
                                )
                            }
                        }
                    }

                    // TRIP SUMMARY
                    TripSummaryCard(
                        totalLegs = uiState.legs.size,
                        totalDistance = uiState.totalDistanceKm,
                        totalAmount = uiState.totalAmountSpent
                    )

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }

            AnimatedVisibility(
                visible = uiState.canSubmit || uiState.isSubmitting,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 24.dp, end = 24.dp),
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                FloatingActionButton(
                    onClick = {
                        viewModel.submitExpense { onDismiss() }
                    },
                    containerColor = Color(0xFF5E92F3),
                    contentColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(8.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (uiState.isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Submit Expense",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }

    if (showImagePickerDialog) {
        ImagePickerDialog(
            onDismiss = { showImagePickerDialog = false },
            onCameraClick = {
                showImagePickerDialog = false
                if (cameraPermission.status.isGranted) {
                    val photoFile = File(context.cacheDir, "temp_photo_${System.currentTimeMillis()}.jpg")
                    currentImageUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
                    cameraLauncher.launch(currentImageUri!!)
                } else {
                    cameraPermission.launchPermissionRequest()
                }
            },
            onGalleryClick = {
                showImagePickerDialog = false
                galleryLauncher.launch("image/*")
            }
        )
    }

    if (uiState.isProcessingImage) {
        ImageProcessingOverlay(progress = uiState.imageProcessingProgress)
    }
    if (uiState.showDraftsList) {
        DraftExpenseListScreen(
            drafts = uiState.availableDrafts,
            onDraftClick = { draftId ->
                viewModel.toggleDraftsList()
                viewModel.loadDraft(draftId)
            },
            onDeleteDraft = { draftId ->
                viewModel.deleteDraft(draftId)
            },
            onDismiss = { viewModel.toggleDraftsList() }
        )
    }

}


@Composable
private fun LegTabRow(
    legs: List<com.bluemix.clients_lead.features.expense.vm.TripLegUiModel>,
    currentLegIndex: Int,
    onLegClick: (Int) -> Unit,
    onAddLeg: () -> Unit,
    onRemoveLeg: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Journey Legs", style = AppTheme.typography.h3, color = Color.White)

            OutlinedButton(
                onClick = onAddLeg,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF5E92F3)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF5E92F3))
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add Leg", style = AppTheme.typography.button)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            legs.forEachIndexed { index, leg ->
                LegTab(
                    legNumber = leg.legNumber,
                    isSelected = index == currentLegIndex,
                    onClick = { onLegClick(index) },
                    onRemove = if (legs.size > 1) { { onRemoveLeg(index) } } else null
                )
            }
        }
    }
}

@Composable
private fun LegTab(
    legNumber: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    onRemove: (() -> Unit)?
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) Color(0xFF2962FF).copy(alpha = 0.25f) else Color(0xFF2A2A2A))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) Color(0xFF5E92F3) else Color(0xFF404040),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Leg $legNumber",
                style = AppTheme.typography.body1,
                color = if (isSelected) Color(0xFF5E92F3) else Color.White
            )

            if (onRemove != null) {
                IconButton(onClick = onRemove, modifier = Modifier.size(20.dp)) {
                    Icon(Icons.Default.Close, "Remove", tint = Color(0xFF808080), modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

@Composable
private fun LegEditor(
    leg: com.bluemix.clients_lead.features.expense.vm.TripLegUiModel,
    legIndex: Int,
    viewModel: MultiLegExpenseViewModel,
    isLoadingLocation: Boolean
) {
    SectionCard(title = "Leg ${leg.legNumber} Details") {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // START LOCATION
            OutlinedTextField(
                value = leg.startLocation?.displayName ?: "",
                onValueChange = {},
                label = { Text("Start Location", color = Color(0xFFB0B0B0)) },
                enabled = false,
                leadingIcon = { Icon(Icons.Default.LocationOn, null, tint = Color(0xFF5E92F3)) },
                trailingIcon = {
                    if (isLoadingLocation) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color(0xFF5E92F3))
                    } else {
                        IconButton(onClick = { viewModel.loadCurrentLocationForLeg(legIndex) }) {
                            Icon(Icons.Default.MyLocation, "Get location", tint = Color(0xFF5E92F3))
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            // END LOCATION with search
            Box {
                OutlinedTextField(
                    value = leg.endLocationQuery,
                    onValueChange = { viewModel.searchEndLocationForLeg(legIndex, it) },
                    label = { Text("End Location", color = Color(0xFFB0B0B0)) },
                    placeholder = { Text("Search destination...", color = Color(0xFF606060)) },
                    leadingIcon = { Icon(Icons.Default.Place, null, tint = Color(0xFF5E92F3)) },
                    trailingIcon = {
                        if (leg.isSearching) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color(0xFF5E92F3))
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                if (leg.searchResults.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 60.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column {
                            leg.searchResults.take(5).forEach { place ->
                                LocationSearchResultItem(
                                    place = place,
                                    onClick = { viewModel.selectEndLocationForLeg(legIndex, place) }
                                )
                            }
                        }
                    }
                }
            }

            // ✅ NEW: ROUTE PREVIEW FOR THIS LEG
            AnimatedVisibility(
                visible = leg.routePolyline?.isNotEmpty() == true &&
                        leg.startLocation != null &&
                        leg.endLocation != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Route Preview", style = AppTheme.typography.body1, color = Color(0xFFB0B0B0))
                    leg.routePolyline?.let { polyline ->
                        MiniRouteMap(
                            routePolyline = polyline,
                            startLocation = LatLng(
                                leg.startLocation!!.latitude,
                                leg.startLocation!!.longitude
                            ),
                            endLocation = LatLng(
                                leg.endLocation!!.latitude,
                                leg.endLocation!!.longitude
                            ),
                            distanceKm = leg.distanceKm,
                            durationMinutes = leg.estimatedDuration,
                            transportMode = leg.transportMode.name
                        )
                    }
                }
            }

            // TRANSPORT MODE
            Text("Transport Mode", style = AppTheme.typography.body1, color = Color.White)
            TransportModeGrid(
                selectedMode = leg.transportMode,
                onModeSelected = { viewModel.updateLegTransportMode(legIndex, it) }
            )

            // AMOUNT
            OutlinedTextField(
                value = if (leg.amountSpent == 0.0) "" else leg.amountSpent.toString(),
                onValueChange = {
                    if (it.isEmpty()) viewModel.updateLegAmount(legIndex, 0.0)
                    else it.toDoubleOrNull()?.let { amt -> viewModel.updateLegAmount(legIndex, amt) }
                },
                label = { Text("Amount Spent", color = Color(0xFFB0B0B0)) },
                leadingIcon = { Icon(Icons.Default.CurrencyRupee, null, tint = Color(0xFF5E92F3)) },
                modifier = Modifier.fillMaxWidth()
            )

            // NOTES
            OutlinedTextField(
                value = leg.notes,
                onValueChange = { viewModel.updateLegNotes(legIndex, it) },
                label = { Text("Notes", color = Color(0xFFB0B0B0)) },
                modifier = Modifier.fillMaxWidth().height(100.dp),
                maxLines = 3
            )
        }
    }
}

@Composable
private fun TransportModeGrid(
    selectedMode: TransportMode,
    onModeSelected: (TransportMode) -> Unit
) {
    val modes = listOf(
        TransportMode.FLIGHT to Icons.Default.Flight,
        TransportMode.TRAIN to Icons.Default.Train,
        TransportMode.BUS to Icons.Default.DirectionsBus,
        TransportMode.TAXI to Icons.Default.LocalTaxi,
        TransportMode.CAR to Icons.Default.DirectionsCar,
        TransportMode.BIKE to Icons.Default.DirectionsBike
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.height(140.dp)
    ) {
        items(modes) { (mode, icon) ->
            TransportModeCard(
                icon = icon,
                label = mode.name,
                isSelected = selectedMode == mode,
                onClick = { onModeSelected(mode) }
            )
        }
    }
}

@Composable
private fun TransportModeCard(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1.3f)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) Color(0xFF2962FF).copy(alpha = 0.2f) else Color(0xFF1A1A1A))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) Color(0xFF5E92F3) else Color(0xFF303030),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, label, tint = if (isSelected) Color(0xFF5E92F3) else Color(0xFF808080), modifier = Modifier.size(36.dp))
            Text(label, style = AppTheme.typography.body1, color = if (isSelected) Color(0xFF5E92F3) else Color.White)
        }
    }
}

@Composable
private fun CombinedRoutePreview(
    legs: List<com.bluemix.clients_lead.features.expense.vm.TripLegUiModel>
) {
    // Only show if at least one leg has a route
    val hasAnyRoute = legs.any { it.routePolyline?.isNotEmpty() == true }

    if (!hasAnyRoute) return

    AnimatedVisibility(
        visible = true,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        SectionCard(title = "Complete Journey Route") {
            CombinedRouteMap(legs = legs)
        }
    }
}

@Composable
private fun CombinedRouteMap(legs: List<com.bluemix.clients_lead.features.expense.vm.TripLegUiModel>) {
    var showExpandedMap by remember { mutableStateOf(false) }

    // Combine all polylines and create color map
    val allPolylines = legs.mapNotNull { it.routePolyline }
    if (allPolylines.isEmpty()) return

    val totalDistance = legs.sumOf { it.distanceKm }
    val totalDuration = legs.sumOf { it.estimatedDuration }

    val startLocation = legs.firstOrNull()?.startLocation
    val endLocation = legs.lastOrNull()?.endLocation

    if (startLocation == null || endLocation == null) return

    // Mini preview
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { showExpandedMap = true }
    ) {
        val cameraPositionState = rememberCameraPositionState()

        LaunchedEffect(allPolylines) {
            try {
                val boundsBuilder = LatLngBounds.builder()
                allPolylines.flatten().forEach { boundsBuilder.include(it) }
                val bounds = boundsBuilder.build()
                cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 50))
            } catch (e: Exception) {
                cameraPositionState.position = CameraPosition.fromLatLngZoom(
                    LatLng(startLocation.latitude, startLocation.longitude),
                    10f
                )
            }
        }

        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = false, mapType = MapType.NORMAL),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                scrollGesturesEnabled = false,
                zoomGesturesEnabled = false
            )
        ) {
            // Draw each leg with different colors
            legs.forEachIndexed { index, leg ->
                leg.routePolyline?.let { polyline ->
                    val color = when (index % 4) {
                        0 -> Color(0xFF5E92F3) // Blue
                        1 -> Color(0xFF4CAF50) // Green
                        2 -> Color(0xFFFF9800) // Orange
                        else -> Color(0xFFE91E63) // Pink
                    }
                    Polyline(points = polyline, color = color, width = 8f)
                }
            }

            // Start marker
            Marker(
                state = MarkerState(position = LatLng(startLocation.latitude, startLocation.longitude)),
                title = "Start",
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
            )

            // End marker
            Marker(
                state = MarkerState(position = LatLng(endLocation.latitude, endLocation.longitude)),
                title = "End",
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
            )

            // Intermediate waypoints
            legs.dropLast(1).forEach { leg ->
                leg.endLocation?.let { loc ->
                    Marker(
                        state = MarkerState(position = LatLng(loc.latitude, loc.longitude)),
                        title = "Waypoint",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)
                    )
                }
            }
        }

        // Tap to expand hint
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(8.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1A1A1A).copy(alpha = 0.9f))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.ZoomOutMap, null, tint = Color(0xFF5E92F3), modifier = Modifier.size(14.dp))
                Text("Tap to view ${legs.size} legs", style = AppTheme.typography.label3, color = Color(0xFF5E92F3), fontSize = 11.sp)
            }
        }

        // Stats overlay
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1A1A1A).copy(alpha = 0.9f))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("${String.format("%.1f", totalDistance)} KM", style = AppTheme.typography.body2, color = Color.White, fontSize = 12.sp)
                if (totalDuration > 0) {
                    Text(formatDuration(totalDuration), style = AppTheme.typography.label3, color = Color(0xFFB0B0B0), fontSize = 10.sp)
                }
            }
        }
    }

    // Full screen dialog (reuse similar logic as MiniRouteMap)
    if (showExpandedMap) {
        MultiLegExpandedMapDialog(
            legs = legs,
            onDismiss = { showExpandedMap = false }
        )
    }
}

@Composable
private fun MultiLegExpandedMapDialog(
    legs: List<com.bluemix.clients_lead.features.expense.vm.TripLegUiModel>,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            val cameraPositionState = rememberCameraPositionState()
            val allPolylines = legs.mapNotNull { it.routePolyline }

            LaunchedEffect(allPolylines) {
                try {
                    val boundsBuilder = LatLngBounds.builder()
                    allPolylines.flatten().forEach { boundsBuilder.include(it) }
                    cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 100))
                } catch (e: Exception) { }
            }

            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(zoomControlsEnabled = true)
            ) {
                legs.forEachIndexed { index, leg ->
                    leg.routePolyline?.let { polyline ->
                        val color = when (index % 4) {
                            0 -> Color(0xFF5E92F3)
                            1 -> Color(0xFF4CAF50)
                            2 -> Color(0xFFFF9800)
                            else -> Color(0xFFE91E63)
                        }
                        Polyline(points = polyline, color = color, width = 12f)
                    }
                }

                legs.firstOrNull()?.startLocation?.let { start ->
                    Marker(
                        state = MarkerState(position = LatLng(start.latitude, start.longitude)),
                        title = "Trip Start"
                    )
                }

                legs.lastOrNull()?.endLocation?.let { end ->
                    Marker(
                        state = MarkerState(position = LatLng(end.latitude, end.longitude)),
                        title = "Trip End"
                    )
                }
            }

            // Close button
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(Color(0xFF1A1A1A).copy(alpha = 0.9f), RoundedCornerShape(12.dp))
            ) {
                Icon(Icons.Default.Close, "Close", tint = Color.White)
            }

            // Legend
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1A1A1A).copy(alpha = 0.95f))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Journey Legs", style = AppTheme.typography.h3, color = Color.White)
                    legs.forEachIndexed { index, leg ->
                        val color = when (index % 4) {
                            0 -> Color(0xFF5E92F3)
                            1 -> Color(0xFF4CAF50)
                            2 -> Color(0xFFFF9800)
                            else -> Color(0xFFE91E63)
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp, 4.dp)
                                    .background(color, RoundedCornerShape(2.dp))
                            )
                            Text(
                                "Leg ${leg.legNumber}: ${leg.transportMode.name}",
                                style = AppTheme.typography.body2,
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatDuration(minutes: Int): String {
    return when {
        minutes < 60 -> "$minutes min"
        minutes < 1440 -> "${minutes / 60}h ${minutes % 60}m"
        else -> "${minutes / 1440}d ${(minutes % 1440) / 60}h"
    }
}

@Composable
private fun TripSummaryCard(totalLegs: Int, totalDistance: Double, totalAmount: Double) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF2962FF).copy(alpha = 0.15f))
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Trip Summary", style = AppTheme.typography.h3, color = Color(0xFF5E92F3))
            SummaryRow(Icons.Default.Route, "Total Legs", "$totalLegs")
            SummaryRow(Icons.Default.Straighten, "Total Distance", "${String.format("%.1f", totalDistance)} KM")
            SummaryRow(Icons.Default.AccountBalanceWallet, "Total Amount", "₹ ${String.format("%.2f", totalAmount)}")
        }
    }
}

@Composable
private fun SummaryRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Color(0xFF5E92F3), modifier = Modifier.size(20.dp))
            Text(label, style = AppTheme.typography.body1, color = Color(0xFFB0B0B0))
        }
        Text(value, style = AppTheme.typography.h3, color = Color.White)
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = AppTheme.typography.h3, color = Color.White)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF0D0D0D))
                .padding(16.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun ErrorCard(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFFF5252).copy(alpha = 0.15f))
            .padding(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Error, null, tint = Color(0xFFFF5252), modifier = Modifier.size(20.dp))
            Text(message, style = AppTheme.typography.body2, color = Color(0xFFFF5252))
        }
    }
}

@Composable
private fun ReceiptImage(uri: String, onRemove: () -> Unit) {
    Box(modifier = Modifier.size(80.dp).clip(RoundedCornerShape(12.dp))) {
        AsyncImage(
            model = "data:image/webp;base64,$uri",
            contentDescription = "Receipt",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        IconButton(
            onClick = onRemove,
            modifier = Modifier.align(Alignment.TopEnd).size(28.dp).background(Color(0xFFFF5252), RoundedCornerShape(14.dp))
        ) {
            Icon(Icons.Default.Close, "Remove", tint = Color.White, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun AddReceiptButton(onClick: () -> Unit, isProcessing: Boolean = false) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(2.dp, if (isProcessing) Color(0xFF808080) else Color(0xFF5E92F3).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .clickable(enabled = !isProcessing, onClick = onClick)
            .background(if (isProcessing) Color(0xFF404040) else Color(0xFF2962FF).copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center
    ) {
        if (isProcessing) {
            CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp, color = Color(0xFF5E92F3))
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Default.Add, "Attach Receipt", tint = Color(0xFF5E92F3), modifier = Modifier.size(28.dp))
                Text("Attach", style = AppTheme.typography.label3, color = Color(0xFF5E92F3))
            }
        }
    }
}

@Composable
private fun LocationSearchResultItem(place: LocationPlace, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(place.displayName, style = AppTheme.typography.body1, color = Color.White, maxLines = 2)
        Text(
            "${String.format("%.4f", place.latitude)}, ${String.format("%.4f", place.longitude)}",
            style = AppTheme.typography.label3,
            color = Color(0xFF808080)
        )
    }
    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF3A3A3A)))
}

@Composable
private fun ImagePickerDialog(onDismiss: () -> Unit, onCameraClick: () -> Unit, onGalleryClick: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A1A),
        tonalElevation = 8.dp,
        shape = RoundedCornerShape(20.dp),
        title = { Text("Add Receipt", style = AppTheme.typography.h3, color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF2A2A2A))
                        .clickable(onClick = onCameraClick)
                        .padding(16.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CameraAlt, null, tint = Color(0xFF5E92F3), modifier = Modifier.size(28.dp))
                        Column {
                            Text("Take Photo", style = AppTheme.typography.body1, color = Color.White)
                            Text("Use camera to capture receipt", style = AppTheme.typography.label3, color = Color(0xFF808080))
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF2A2A2A))
                        .clickable(onClick = onGalleryClick)
                        .padding(16.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PhotoLibrary, null, tint = Color(0xFF5E92F3), modifier = Modifier.size(28.dp))
                        Column {
                            Text("Choose from Gallery", style = AppTheme.typography.body1, color = Color.White)
                            Text("Select existing photo", style = AppTheme.typography.label3, color = Color(0xFF808080))
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", style = AppTheme.typography.button, color = Color(0xFF808080)) } }
    )
}

@Composable
private fun ImageProcessingOverlay(progress: String?) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF1A1A1A))
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(modifier = Modifier.size(40.dp), color = Color(0xFF5E92F3), strokeWidth = 3.dp)
                Text(progress ?: "Processing image...", style = AppTheme.typography.body1, color = Color.White, textAlign = TextAlign.Center)
            }
        }
    }
}


@Composable
private fun MultiLegExpenseSheetHeader(
    onDismiss: () -> Unit,
    onSaveDraft: () -> Unit,
    onViewDrafts: () -> Unit,
    draftCount: Int,
    isSubmitting: Boolean,
    isSaving: Boolean,
    lastSaved: Long?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top Row: Close | Title | Drafts
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, "Close", tint = Color.White)
            }

            Text(
                text = "Multi-Leg Trip",
                style = AppTheme.typography.h2,
                color = Color.White
            )

            // View Drafts Button
            Box {
                IconButton(onClick = onViewDrafts) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = "View drafts",
                        tint = Color(0xFF5E92F3)
                    )
                }

                // Draft count badge
                if (draftCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-4).dp, y = 4.dp)
                            .size(18.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(Color(0xFFFF5252)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (draftCount > 9) "9+" else draftCount.toString(),
                            style = AppTheme.typography.label3,
                            color = Color.White,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        // Auto-save Indicator + Manual Save Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Last Saved Indicator
            AnimatedVisibility(
                visible = lastSaved != null,
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally()
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isSaving) Icons.Default.CloudSync else Icons.Default.CloudDone,
                        contentDescription = null,
                        tint = if (isSaving) Color(0xFF808080) else Color(0xFF4CAF50),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isSaving) "Saving..." else "Saved ${formatRelativeTime(lastSaved ?: 0)}",
                        style = AppTheme.typography.label2,
                        color = if (isSaving) Color(0xFF808080) else Color(0xFF4CAF50)
                    )
                }
            }

            // Right: Manual Save Button
            TextButton(
                onClick = onSaveDraft,
                enabled = !isSaving && !isSubmitting,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFF5E92F3)
                    )
                    Spacer(Modifier.width(6.dp))
                }
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (isSaving) Color(0xFF808080) else Color(0xFF5E92F3)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = if (isSaving) "Saving" else "Save Draft",
                    style = AppTheme.typography.button,
                    color = if (isSaving) Color(0xFF808080) else Color(0xFF5E92F3)
                )
            }
        }

        // Divider
        Divider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = Color(0xFF2A2A2A),
            thickness = 1.dp
        )
    }
}

// Helper function
private fun formatRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 5_000 -> "just now"
        diff < 60_000 -> "${diff / 1000}s ago"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timestamp))
    }
}
