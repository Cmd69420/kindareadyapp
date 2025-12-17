package com.bluemix.clients_lead.features.expense.presentation

import android.net.Uri
import timber.log.Timber
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.bluemix.clients_lead.domain.model.TransportMode
import com.bluemix.clients_lead.features.expense.vm.TripExpenseViewModel
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel
import ui.AppTheme
import ui.components.Button
import ui.components.ButtonVariant
import ui.components.Text
import ui.components.textfield.OutlinedTextField
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TripExpenseSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TripExpenseViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    var showAddUrlDialog by remember { mutableStateOf(false) }
    var receiptUrlInput by remember { mutableStateOf("") }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.addReceipt(it.toString()) }
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
                    .padding(top = 80.dp)
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

                    // HEADER SECTION
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White
                            )
                        }

                        Text(
                            text = "New Trip Expense",
                            style = AppTheme.typography.h2,
                            color = Color.White
                        )

                        TextButton(
                            onClick = {
                                Timber.d("SAVE BUTTON PRESSED")
                                viewModel.submitExpense(onSuccess = { onDismiss() })
                            },
                            enabled = !uiState.isSubmitting
                        ) {
                            if (uiState.isSubmitting) {
                                androidx.compose.material3.CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFF5E92F3)
                                )
                            } else {
                                Text(
                                    text = "Save",
                                    style = AppTheme.typography.button,
                                    color = Color(0xFF5E92F3)
                                )
                            }
                        }
                    }

                    // ERROR MESSAGE
                    if (uiState.error != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFFF5252).copy(alpha = 0.15f))
                                .padding(12.dp)
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
                                    color = Color(0xFFFF5252)
                                )
                            }
                        }
                    }

                    // TRIP INFORMATION SECTION
                    SectionCard(title = "Trip Information") {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            OutlinedTextField(
                                value = uiState.startLocation,
                                onValueChange = { viewModel.updateStartLocation(it) },
                                label = { Text("Start Location", color = Color(0xFFB0B0B0)) },
                                placeholder = { Text("Main Office, Delhi", color = Color(0xFF606060)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = Color(0xFF5E92F3)
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = uiState.endLocation,
                                onValueChange = { viewModel.updateEndLocation(it) },
                                label = { Text("End Location", color = Color(0xFFB0B0B0)) },
                                placeholder = { Text("Enter destination", color = Color(0xFF606060)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Place,
                                        contentDescription = null,
                                        tint = Color(0xFF5E92F3)
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = formatDate(uiState.travelDate),
                                    onValueChange = {},
                                    label = { Text("Date & Time", color = Color(0xFFB0B0B0)) },
                                    enabled = false,
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.CalendarToday,
                                            contentDescription = null,
                                            tint = Color(0xFF5E92F3)
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                )

                                OutlinedTextField(
                                    value = uiState.distanceKm.toString(),
                                    onValueChange = {
                                        it.toDoubleOrNull()?.let { d -> viewModel.updateDistance(d) }
                                    },
                                    label = { Text("Distance", color = Color(0xFFB0B0B0)) },
                                    placeholder = { Text("0.0", color = Color(0xFF606060)) },
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

                    // EXPENSE DETAILS SECTION
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

                    // RECEIPTS SECTION
                    SectionCard(title = "Receipts") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            uiState.receiptUrls.forEach { uri ->
                                ReceiptImage(
                                    uri = uri,
                                    onRemove = { viewModel.removeReceipt(uri) }
                                )
                            }

                            if (uiState.receiptUrls.size < 5) {
                                AddReceiptButton(onClick = { showAddUrlDialog = true })
                            }
                        }
                    }

                    // SUMMARY CARD
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
                                    "${uiState.distanceKm} KM",
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
                    androidx.compose.material3.Button(
                        onClick = { viewModel.submitExpense(onSuccess = { onDismiss() }) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        enabled = !uiState.isSubmitting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF5E92F3),
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFF404040),
                            disabledContentColor = Color(0xFF808080)
                        )
                    ) {
                        if (uiState.isSubmitting) {
                            androidx.compose.material3.CircularProgressIndicator(
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

                    Spacer(modifier = Modifier.height(32.dp))

                    // ADD URL DIALOG
                    if (showAddUrlDialog) {
                        androidx.compose.material3.AlertDialog(
                            onDismissRequest = { showAddUrlDialog = false },
                            containerColor = Color(0xFF1A1A1A),
                            tonalElevation = 8.dp,
                            shape = RoundedCornerShape(20.dp),
                            title = {
                                Text(
                                    text = "Attach Receipt URL",
                                    style = AppTheme.typography.h3,
                                    color = Color.White
                                )
                            },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(
                                        text = "Upload the photo to drive and paste the URL below",
                                        style = AppTheme.typography.body2,
                                        color = Color(0xFFB0B0B0)
                                    )

                                    OutlinedTextField(
                                        value = receiptUrlInput,
                                        onValueChange = { receiptUrlInput = it },
                                        placeholder = { Text("https://example.com/receipt.jpg", color = Color(0xFF606060)) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 56.dp)
                                    )
                                }
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        val url = receiptUrlInput.trim()
                                        if (url.startsWith("http://") || url.startsWith("https://")) {
                                            viewModel.addReceipt(url)
                                        } else {
                                            viewModel.updateNotes("Invalid image URL")
                                        }
                                        receiptUrlInput = ""
                                        showAddUrlDialog = false
                                    }
                                ) {
                                    Text(
                                        text = "Add",
                                        style = AppTheme.typography.button,
                                        color = Color(0xFF5E92F3)
                                    )
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showAddUrlDialog = false }) {
                                    Text(
                                        text = "Cancel",
                                        style = AppTheme.typography.button,
                                        color = Color(0xFF808080)
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
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
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1.3f)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected) Color(0xFF2962FF).copy(alpha = 0.2f)
                else Color(0xFF1A1A1A)
            )
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
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) Color(0xFF5E92F3) else Color(0xFF808080),
                modifier = Modifier.size(36.dp)
            )
            Text(
                text = label,
                style = AppTheme.typography.body1,
                color = if (isSelected) Color(0xFF5E92F3) else Color.White
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
            model = uri,
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
private fun AddReceiptButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = 2.dp,
                color = Color(0xFF5E92F3).copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .background(Color(0xFF2962FF).copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center
    ) {
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

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}