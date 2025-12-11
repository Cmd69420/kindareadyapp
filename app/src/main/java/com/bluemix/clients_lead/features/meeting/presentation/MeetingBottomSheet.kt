package com.bluemix.clients_lead.features.meeting.presentation

import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bluemix.clients_lead.domain.model.Client
import com.bluemix.clients_lead.domain.model.Meeting
import ui.AppTheme
import ui.components.Button
import ui.components.Icon
import ui.components.IconButton
import ui.components.Text
import ui.components.textfield.OutlinedTextField
import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeFormatter

/**
 * Bottom sheet that appears when user enters client proximity
 */
@Composable
fun MeetingBottomSheet(
    client: Client,
    activeMeeting: Meeting?,
    isLoading: Boolean,
    onStartMeeting: () -> Unit,
    onEndMeeting: (comments: String, attachments: List<Uri>) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var comments by remember { mutableStateOf("") }
    var attachments by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var showEndConfirmation by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        attachments = uris
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(AppTheme.colors.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (activeMeeting != null) "Meeting in Progress" else "Start Meeting",
                        style = AppTheme.typography.h3,
                        color = AppTheme.colors.text
                    )
                    Text(
                        text = client.name,
                        style = AppTheme.typography.body2,
                        color = AppTheme.colors.textSecondary
                    )
                }

                // FIXED: Proper close button styling
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = AppTheme.colors.textSecondary  // Changed from text to textSecondary
                    )
                }
            }

            // Client info card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppTheme.colors.background)
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    client.address?.let {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = AppTheme.colors.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = it,
                                style = AppTheme.typography.body2,
                                color = AppTheme.colors.textSecondary
                            )
                        }
                    }

                    client.phone?.let {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                tint = AppTheme.colors.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = it,
                                style = AppTheme.typography.body2,
                                color = AppTheme.colors.textSecondary
                            )
                        }
                    }
                }
            }

            // Meeting duration (if active)
            if (activeMeeting != null) {
                MeetingDurationCard(meeting = activeMeeting)
            }

            // Comments section (only shown if meeting is active)
            AnimatedVisibility(
                visible = activeMeeting != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Meeting Notes",
                        style = AppTheme.typography.body1,
                        color = AppTheme.colors.text
                    )

                    OutlinedTextField(
                        value = comments,
                        onValueChange = { comments = it },
                        placeholder = { Text("Add comments about this meeting...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp),
                        maxLines = 6
                    )
                }
            }

            // Attachments section (only shown if meeting is active)
            AnimatedVisibility(
                visible = activeMeeting != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Attachments",
                            style = AppTheme.typography.body1,
                            color = AppTheme.colors.text
                        )

                        TextButton(onClick = { filePickerLauncher.launch("*/*") }) {
                            Icon(
                                imageVector = Icons.Default.AttachFile,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Files")
                        }
                    }

                    if (attachments.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            attachments.forEach { uri ->
                                AttachmentItem(
                                    uri = uri,
                                    onRemove = { attachments = attachments - uri }
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "No files attached",
                            style = AppTheme.typography.body2,
                            color = AppTheme.colors.textSecondary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }

            // Action buttons
            if (activeMeeting == null) {
                // Start meeting button
                Button(
                    onClick = onStartMeeting,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = AppTheme.colors.onPrimary,
                            strokeWidth = 2.dp
                        )

                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Starting...",
                            style = AppTheme.typography.button
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Start Meeting",
                        style = AppTheme.typography.button
                    )
                }
            }

            else {
                // End meeting button
                Button(
                    onClick = { showEndConfirmation = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = AppTheme.colors.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "End Meeting",
                        style = AppTheme.typography.button
                    )
                }
            }

            // Info text
            Text(
                text = if (activeMeeting == null) {
                    "Starting a meeting will log your visit time and location for this client."
                } else {
                    "Add notes and attachments before ending the meeting. All data will be saved to your meeting history."
                },
                style = AppTheme.typography.body2,
                color = AppTheme.colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    // End meeting confirmation dialog
    if (showEndConfirmation) {
        AlertDialog(
            onDismissRequest = { showEndConfirmation = false },
            confirmButton = {
                Button(
                    onClick = {
                        showEndConfirmation = false
                        onEndMeeting(comments, attachments)
                    }
                ) {
                    Text("End Meeting")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndConfirmation = false }) {
                    Text("Cancel")
                }
            },
            title = { Text("End Meeting?") },
            text = {
                Text("Are you sure you want to end this meeting? Your notes and attachments will be saved.")
            }
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun MeetingDurationCard(meeting: Meeting) {
    val duration = remember(meeting.startTime) {
        try {
            val start = Instant.parse(meeting.startTime)
            val now = Instant.now()
            Duration.between(start, now)
        } catch (e: Exception) {
            Duration.ZERO
        }
    }

    var currentDuration by remember { mutableStateOf(duration) }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            currentDuration = try {
                val start = Instant.parse(meeting.startTime)
                val now = Instant.now()
                Duration.between(start, now)
            } catch (e: Exception) {
                Duration.ZERO
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AppTheme.colors.primary.copy(alpha = 0.1f))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Meeting Duration",
                    style = AppTheme.typography.label2,
                    color = AppTheme.colors.textSecondary
                )
                Text(
                    text = formatDuration(currentDuration),
                    style = AppTheme.typography.h2,
                    color = AppTheme.colors.primary
                )
            }

            Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = null,
                tint = AppTheme.colors.primary,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
private fun AttachmentItem(
    uri: Uri,
    onRemove: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(AppTheme.colors.background)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = AppTheme.colors.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = uri.lastPathSegment ?: "Unknown file",
                    style = AppTheme.typography.body2,
                    color = AppTheme.colors.text,
                    maxLines = 1
                )
            }

            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove",
                    tint = AppTheme.colors.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private fun formatDuration(duration: Duration): String {
    val hours = duration.toHours()
    val minutes = duration.toMinutes() % 60
    val seconds = duration.seconds % 60

    return when {
        hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, seconds)
        else -> String.format("%02d:%02d", minutes, seconds)
    }
}