package com.bluemix.clients_lead.features.expense.presentation



import android.Manifest

// Add these imports at the top:
import androidx.compose.ui.unit.sp
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.bluemix.clients_lead.core.design.ui.components.textfield.OutlinedTextField
import com.bluemix.clients_lead.domain.model.LocationPlace
import com.bluemix.clients_lead.domain.model.TransportMode
import com.bluemix.clients_lead.features.expense.vm.TripExpenseViewModel
import com.bluemix.clients_lead.features.expense.presentation.components.MiniRouteMap // ✅ Import MiniRouteMap
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.maps.model.LatLng // ✅ Import LatLng
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel
import timber.log.Timber
import ui.AppTheme
import ui.components.Text
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Replace the TripExpenseSheet function in TripExpenseScreen.kt

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun TripExpenseSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TripExpenseViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    var showImagePickerDialog by remember { mutableStateOf(false) }
    var currentImageUri by remember { mutableStateOf<Uri?>(null) }

    val cameraPermission = rememberPermissionState(
        permission = Manifest.permission.CAMERA
    )

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && currentImageUri != null) {
            Timber.d("📸 Photo captured: $currentImageUri")
            viewModel.processAndUploadImage(context, currentImageUri!!)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                Timber.d("🖼️ Image selected from gallery: $uri")
                viewModel.processAndUploadImage(context, it)
            } catch (e: OutOfMemoryError) {
                Timber.e(e, "Out of memory loading gallery image")
                viewModel.setError("Image too large. Please try a smaller image.")
            } catch (e: Exception) {
                Timber.e(e, "Failed to load image from gallery")
                viewModel.setError("Failed to load image: ${e.message}")
            }
        }
    }

    // Auto-dismiss on success
    LaunchedEffect(uiState.successMessage) {
        if (uiState.successMessage != null) {
            delay(500)
            onDismiss()
            viewModel.resetSuccess()
        }
    }

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
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {

                // ✅ IMPROVED HEADER
                ExpenseSheetHeader(
                    onDismiss = onDismiss,
                    onSaveDraft = { viewModel.saveDraft(showConfirmation = true) },
                    onViewDrafts = { viewModel.toggleDraftsList() },
                    draftCount = uiState.availableDrafts.size,
                    isSubmitting = uiState.isSubmitting,
                    isSaving = uiState.isSaving,
                    lastSaved = uiState.lastSaved
                )

                // ✅ SUCCESS MESSAGE
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
                AnimatedVisibility(
                    visible = uiState.error != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFF5252).copy(alpha = 0.15f))
                            .padding(12.dp)
                            .clickable { viewModel.resetError() }
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = uiState.error ?: "",
                                style = AppTheme.typography.body2,
                                color = Color(0xFFFF5252),
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // TRIP INFORMATION SECTION
                SectionCard(title = "Trip Information") {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                        // START LOCATION
                        OutlinedTextField(
                            value = uiState.startLocation?.displayName ?: "",
                            onValueChange = {},
                            label = { Text("Start Location", color = Color(0xFFB0B0B0)) },
                            placeholder = {
                                Text("Tap button to get current location", color = Color(0xFF606060))
                            },
                            enabled = false,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = Color(0xFF5E92F3)
                                )
                            },
                            trailingIcon = {
                                if (uiState.isLoadingCurrentLocation) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = Color(0xFF5E92F3)
                                    )
                                } else {
                                    IconButton(
                                        onClick = { viewModel.loadCurrentLocation() }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MyLocation,
                                            contentDescription = "Get current location",
                                            tint = Color(0xFF5E92F3)
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // END LOCATION
                        Box {
                            OutlinedTextField(
                                value = uiState.endLocationQuery,
                                onValueChange = { viewModel.searchEndLocation(it) },
                                label = { Text("End Location", color = Color(0xFFB0B0B0)) },
                                placeholder = {
                                    Text("Search destination...", color = Color(0xFF606060))
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Place,
                                        contentDescription = null,
                                        tint = Color(0xFF5E92F3)
                                    )
                                },
                                trailingIcon = {
                                    if (uiState.isSearching) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp,
                                            color = Color(0xFF5E92F3)
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )

                            // SEARCH RESULTS
                            if (uiState.searchResults.isNotEmpty()) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 60.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFF2A2A2A)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    elevation = CardDefaults.cardElevation(8.dp)
                                ) {
                                    Column {
                                        uiState.searchResults.take(5).forEach { place ->
                                            LocationSearchResultItem(
                                                place = place,
                                                onClick = {
                                                    viewModel.selectEndLocation(place)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // DATE
                            OutlinedTextField(
                                value = formatDate(uiState.travelDate),
                                onValueChange = {},
                                enabled = false,
                                label = { Text("Date & Time", color = Color(0xFFB0B0B0)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        tint = Color(0xFF5E92F3)
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )

                            // DISTANCE
                            OutlinedTextField(
                                value = String.format("%.1f", uiState.distanceKm),
                                onValueChange = {},
                                enabled = false,
                                label = { Text("Distance", color = Color(0xFFB0B0B0)) },
                                trailingIcon = {
                                    Text(
                                        "KM",
                                        style = AppTheme.typography.body2,
                                        color = Color(0xFF808080)
                                    )
                                },
                                modifier = Modifier.weight(0.8f)
                            )
                        }
                    }
                }

                // TRANSPORT MODE SECTION
                SectionCard(title = "Transport Mode") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            TransportModeCard(
                                icon = Icons.Default.DirectionsBus,
                                label = "Bus",
                                isSelected = uiState.transportMode == TransportMode.BUS,
                                onClick = { viewModel.updateTransportMode(TransportMode.BUS) },
                                modifier = Modifier.weight(1f)
                            )
                            TransportModeCard(
                                icon = Icons.Default.Train,
                                label = "Train",
                                isSelected = uiState.transportMode == TransportMode.TRAIN,
                                onClick = { viewModel.updateTransportMode(TransportMode.TRAIN) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            TransportModeCard(
                                icon = Icons.Default.DirectionsBike,
                                label = "Bike",
                                isSelected = uiState.transportMode == TransportMode.BIKE,
                                onClick = { viewModel.updateTransportMode(TransportMode.BIKE) },
                                modifier = Modifier.weight(1f)
                            )
                            TransportModeCard(
                                icon = Icons.Default.DirectionsCar,
                                label = "Rickshaw",
                                isSelected = uiState.transportMode == TransportMode.RICKSHAW,
                                onClick = { viewModel.updateTransportMode(TransportMode.RICKSHAW) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // ROUTE PREVIEW
                AnimatedVisibility(
                    visible = uiState.routePolyline?.isNotEmpty() == true &&
                            uiState.startLocation != null &&
                            uiState.endLocation != null,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    SectionCard(title = "Route Preview") {
                        uiState.routePolyline?.let { polyline ->
                            MiniRouteMap(
                                routePolyline = polyline,
                                startLocation = LatLng(
                                    uiState.startLocation!!.latitude,
                                    uiState.startLocation!!.longitude
                                ),
                                endLocation = LatLng(
                                    uiState.endLocation!!.latitude,
                                    uiState.endLocation!!.longitude
                                ),
                                distanceKm = uiState.distanceKm,
                                durationMinutes = uiState.estimatedDuration,
                                transportMode = uiState.transportMode.name
                            )
                        }
                    }
                }

                // EXPENSE DETAILS
                SectionCard(title = "Expense Details") {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = if (uiState.amountSpent == 0.0) "" else uiState.amountSpent.toString(),
                            onValueChange = {
                                if (it.isEmpty()) viewModel.updateAmount(0.0)
                                else it.toDoubleOrNull()?.let { amt -> viewModel.updateAmount(amt) }
                            },
                            label = { Text("Amount Spent", color = Color(0xFFB0B0B0)) },
                            placeholder = { Text("0.00", color = Color(0xFF606060)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.CurrencyRupee,
                                    contentDescription = null,
                                    tint = Color(0xFF5E92F3)
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = uiState.notes,
                            onValueChange = { viewModel.updateNotes(it) },
                            label = { Text("Comments / Notes", color = Color(0xFFB0B0B0)) },
                            placeholder = { Text("e.g. Toll charges, parking fee...", color = Color(0xFF606060)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Notes,
                                    contentDescription = null,
                                    tint = Color(0xFF5E92F3)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            maxLines = 4
                        )
                    }
                }

                // RECEIPTS
                SectionCard(title = "Receipts") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        uiState.receiptImages.forEach { uri ->
                            ReceiptImage(
                                uri = uri,
                                onRemove = { viewModel.removeReceipt(uri) }
                            )
                        }

                        if (uiState.receiptImages.size < 5) {
                            AddReceiptButton(
                                onClick = { showImagePickerDialog = true },
                                isProcessing = uiState.isProcessingImage
                            )
                        }
                    }
                }

                // SUMMARY
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF2962FF).copy(alpha = 0.15f))
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Route,
                                    contentDescription = null,
                                    tint = Color(0xFF5E92F3),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    "Total Distance",
                                    style = AppTheme.typography.body1,
                                    color = Color(0xFFB0B0B0)
                                )
                            }
                            Text(
                                "${String.format("%.1f", uiState.distanceKm)} KM",
                                style = AppTheme.typography.h3,
                                color = Color(0xFF5E92F3)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = Color(0xFF5E92F3),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    "Total Amount",
                                    style = AppTheme.typography.body1,
                                    color = Color(0xFFB0B0B0)
                                )
                            }
                            Text(
                                "₹ ${String.format("%.2f", uiState.amountSpent)}",
                                style = AppTheme.typography.h3,
                                color = Color(0xFF5E92F3)
                            )
                        }
                    }
                }

                // SUBMIT BUTTON
                Button(
                    onClick = { viewModel.submitExpense(onSuccess = { onDismiss() }) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = !uiState.isSubmitting &&
                            uiState.startLocation != null &&
                            uiState.endLocation != null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF5E92F3),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFF404040),
                        disabledContentColor = Color(0xFF808080)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (uiState.isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Submitting...",
                            style = AppTheme.typography.button,
                            color = Color.White
                        )
                    } else {
                        Text(
                            text = "Submit Expense",
                            style = AppTheme.typography.button,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // DRAFTS LIST SHEET
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

    // IMAGE PICKER DIALOG
    if (showImagePickerDialog) {
        ImagePickerDialog(
            onDismiss = { showImagePickerDialog = false },
            onCameraClick = {
                showImagePickerDialog = false
                if (cameraPermission.status.isGranted) {
                    val photoFile = File(
                        context.cacheDir,
                        "temp_photo_${System.currentTimeMillis()}.jpg"
                    )
                    currentImageUri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        photoFile
                    )
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

    // IMAGE PROCESSING OVERLAY
    if (uiState.isProcessingImage) {
        ImageProcessingOverlay(progress = uiState.imageProcessingProgress)
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title,
            style = AppTheme.typography.h3,
            color = Color.White
        )

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
private fun TransportModeCard(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    isEnabled: Boolean = true, // ✅ NEW
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1.3f)
            .clip(RoundedCornerShape(16.dp))
            .background(
                when {
                    !isEnabled -> Color(0xFF0D0D0D)
                    isSelected -> Color(0xFF2962FF).copy(alpha = 0.2f)
                    else -> Color(0xFF1A1A1A)
                }
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = when {
                    !isEnabled -> Color(0xFF404040)
                    isSelected -> Color(0xFF5E92F3)
                    else -> Color(0xFF303030)
                },
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(enabled = isEnabled, onClick = onClick)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = when {
                    !isEnabled -> Color(0xFF404040)
                    isSelected -> Color(0xFF5E92F3)
                    else -> Color(0xFF808080)
                },
                modifier = Modifier.size(36.dp)
            )
            Text(
                text = label,
                style = AppTheme.typography.body1,
                color = when {
                    !isEnabled -> Color(0xFF404040)
                    isSelected -> Color(0xFF5E92F3)
                    else -> Color.White
                }
            )
        }

        // ✅ Show "Not Available" badge
        if (!isEnabled) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(8.dp)
                    .background(Color(0xFFFF5252), RoundedCornerShape(4.dp))
            )
        }
    }
}

@Composable
private fun ReceiptImage(uri: String, onRemove: () -> Unit) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(RoundedCornerShape(12.dp))
    ) {
        AsyncImage(
            model = "data:image/webp;base64,$uri",
            contentDescription = "Receipt",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(28.dp)
                .background(Color(0xFFFF5252), shape = RoundedCornerShape(14.dp))
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun AddReceiptButton(
    onClick: () -> Unit,
    isProcessing: Boolean = false
) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = 2.dp,
                color = if (isProcessing) Color(0xFF808080) else Color(0xFF5E92F3).copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(enabled = !isProcessing, onClick = onClick)
            .background(
                if (isProcessing) Color(0xFF404040) else Color(0xFF2962FF).copy(alpha = 0.1f)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isProcessing) {
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                strokeWidth = 2.dp,
                color = Color(0xFF5E92F3)
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Attach Receipt",
                    tint = Color(0xFF5E92F3),
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "Attach",
                    style = AppTheme.typography.label3,
                    color = Color(0xFF5E92F3)
                )
            }
        }
    }
}


@Composable
private fun LocationSearchResultItem(
    place: LocationPlace,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = place.displayName,
            style = AppTheme.typography.body1,
            color = Color.White,
            maxLines = 2
        )

        Text(
            text = "${String.format("%.4f", place.latitude)}, ${String.format("%.4f", place.longitude)}",
            style = AppTheme.typography.label3,
            color = Color(0xFF808080)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color(0xFF3A3A3A))
    )
}


private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Composable
private fun ImagePickerDialog(
    onDismiss: () -> Unit,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A1A),
        tonalElevation = 8.dp,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "Add Receipt",
                style = AppTheme.typography.h3,
                color = Color.White
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Camera Option
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF2A2A2A))
                        .clickable(onClick = onCameraClick)
                        .padding(16.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = Color(0xFF5E92F3),
                            modifier = Modifier.size(28.dp)

                        )
                        Column {
                            Text(
                                text = "Take Photo",
                                style = AppTheme.typography.body1,
                                color = Color.White
                            )
                            Text(
                                text = "Use camera to capture receipt",
                                style = AppTheme.typography.label3,
                                color = Color(0xFF808080)
                            )
                        }
                    }
                }

                // Gallery Option
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF2A2A2A))
                        .clickable(onClick = onGalleryClick)
                        .padding(16.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            tint = Color(0xFF5E92F3),
                            modifier = Modifier.size(28.dp)
                        )
                        Column {
                            Text(
                                text = "Choose from Gallery",
                                style = AppTheme.typography.body1,
                                color = Color.White
                            )
                            Text(
                                text = "Select existing photo",
                                style = AppTheme.typography.label3,
                                color = Color(0xFF808080)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancel",
                    style = AppTheme.typography.button,
                    color = Color(0xFF808080)
                )
            }
        }
    )
}

@Composable
private fun ImageProcessingOverlay(progress: String?) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
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
                CircularProgressIndicator(
                    modifier = Modifier.size(40.dp),
                    color = Color(0xFF5E92F3),
                    strokeWidth = 3.dp
                )
                Text(
                    text = progress ?: "Processing image...",
                    style = AppTheme.typography.body1,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// Improved header component for TripExpenseScreen.kt
// Replace the existing ExpenseSheetHeader composable with this

@Composable
private fun ExpenseSheetHeader(
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
            // Close Button
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }

            // Title
            Text(
                text = "New Trip Expense",
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

// Helper function (add to bottom of file if not already present)
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