package com.bluemix.clients_lead.features.expense.presentation

import android.net.Uri
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.bluemix.clients_lead.domain.model.TransportMode
import com.bluemix.clients_lead.features.expense.vm.TripExpenseViewModel
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
    viewModel: TripExpenseViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.addReceipt(it.toString()) }
    }

    AnimatedVisibility(
        visible = true,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppTheme.colors.background)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = AppTheme.colors.text
                    )
                }

                Text(
                    text = "New Trip Expense",
                    style = AppTheme.typography.h2,
                    color = AppTheme.colors.text
                )

                TextButton(onClick = {
                    viewModel.submitExpense()
                    onDismiss()
                }) {
                    Text(
                        text = "Save",
                        style = AppTheme.typography.button,
                        color = AppTheme.colors.primary
                    )
                }
            }

            // Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Trip Information Section
                SectionHeader("Trip Information")

                OutlinedTextField(
                    value = uiState.startLocation,
                    onValueChange = { newValue: String -> viewModel.updateStartLocation(newValue) },
                    label = { Text("Start Location") },
                    placeholder = { Text("Main Office, Delhi") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = uiState.endLocation,
                    onValueChange = { newValue: String -> viewModel.updateEndLocation(newValue) },
                    label = { Text("End Location") },
                    placeholder = { Text("Enter destination") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = formatDate(uiState.travelDate),
                        onValueChange = { },
                        label = { Text("Travel Date & Time") },
                        modifier = Modifier.weight(1f),
                        enabled = false
                    )

                    OutlinedTextField(
                        value = uiState.distanceKm.toString(),
                        onValueChange = { value: String ->
                            value.toDoubleOrNull()?.let { distance ->
                                viewModel.updateDistance(distance)
                            }
                        },
                        label = { Text("Distance (KM)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Transport Mode Section
                SectionHeader("Transport Mode")

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
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

                // Expense Details Section
                SectionHeader("Expense Details")

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Amount Spent (₹)",
                        style = AppTheme.typography.body2,
                        color = AppTheme.colors.textSecondary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    OutlinedTextField(
                        value = if (uiState.amountSpent == 0.0) "" else uiState.amountSpent.toString(),
                        onValueChange = { value: String ->
                            if (value.isEmpty()) {
                                viewModel.updateAmount(0.0)
                            } else {
                                value.toDoubleOrNull()?.let { amount ->
                                    viewModel.updateAmount(amount)
                                }
                            }
                        },
                        placeholder = { Text("0.00") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = uiState.notes,
                    onValueChange = { newValue: String -> viewModel.updateNotes(newValue) },
                    label = { Text("Add Comment / Notes") },
                    placeholder = { Text("e.g. Toll charges, parking fee...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    maxLines = 4
                )

                // Receipts Section
                SectionHeader("Receipts")

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.receiptUrls.forEach { uri ->
                        ReceiptImage(
                            uri = uri,
                            onRemove = { viewModel.removeReceipt(uri) }
                        )
                    }

                    if (uiState.receiptUrls.size < 5) {
                        AddReceiptButton(
                            onClick = { imagePickerLauncher.launch("image/*") }
                        )
                    }
                }

                // Summary
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Total distance:",
                        style = AppTheme.typography.body1,
                        color = AppTheme.colors.textSecondary
                    )
                    Text(
                        text = "${uiState.distanceKm} KM",
                        style = AppTheme.typography.h3,
                        color = AppTheme.colors.text
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Total amount:",
                        style = AppTheme.typography.body1,
                        color = AppTheme.colors.textSecondary
                    )
                    Text(
                        text = "₹ ${String.format("%.2f", uiState.amountSpent)}",
                        style = AppTheme.typography.h3,
                        color = AppTheme.colors.text
                    )
                }

                // Submit Button
                Button(
                    text = "Submit Expense",
                    onClick = {
                        viewModel.submitExpense()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    variant = ButtonVariant.Primary
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = AppTheme.typography.h3,
        color = AppTheme.colors.text
    )
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
            .aspectRatio(1.5f)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected)
                    AppTheme.colors.primary.copy(alpha = 0.1f)
                else
                    AppTheme.colors.surface
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected)
                    AppTheme.colors.primary
                else
                    AppTheme.colors.outline,
                shape = RoundedCornerShape(12.dp)
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
                tint = if (isSelected) AppTheme.colors.primary else AppTheme.colors.textSecondary,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = label,
                style = AppTheme.typography.body2,
                color = if (isSelected) AppTheme.colors.primary else AppTheme.colors.text
            )
        }
    }
}

@Composable
private fun ReceiptImage(
    uri: String,
    onRemove: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(RoundedCornerShape(8.dp))
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
                .size(24.dp)
                .background(AppTheme.colors.error, shape = RoundedCornerShape(12.dp))
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove",
                tint = AppTheme.colors.onError,
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
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = 2.dp,
                color = AppTheme.colors.primary,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .background(AppTheme.colors.surface),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Attach Receipt",
                tint = AppTheme.colors.primary,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = "Attach\nReceipt",
                style = AppTheme.typography.label3,
                color = AppTheme.colors.primary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}