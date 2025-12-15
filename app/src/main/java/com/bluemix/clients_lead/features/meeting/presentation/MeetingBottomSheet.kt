package com.bluemix.clients_lead.features.meeting.presentation

import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        attachments = uris
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // Dim backdrop
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f))
                .clickable(
                    onClick = onDismiss,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                )
        )

        // Bottom sheet container
        Box(
            modifier = modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(top = 80.dp)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(AppTheme.colors.surface)
                .clickable(
                    onClick = {},
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {

                // HEADER
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

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = AppTheme.colors.textSecondary
                        )
                    }
                }

                // CLIENT INFO CARD
                ClientInfoCard(client)

                // MEETING DURATION
                if (activeMeeting != null) {
                    MeetingDurationCard(activeMeeting)
                }

                // COMMENTS
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

                // ATTACHMENTS
                AnimatedVisibility(
                    visible = activeMeeting != null,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

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
                                androidx.compose.material3.Icon(
                                    imageVector = Icons.Default.AttachFile,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Files")
                            }
                        }

                        if (attachments.isEmpty()) {
                            Text(
                                text = "No files attached",
                                style = AppTheme.typography.body2,
                                color = AppTheme.colors.textSecondary
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                attachments.forEach { uri ->
                                    AttachmentItem(uri) {
                                        attachments = attachments - uri
                                    }
                                }
                            }
                        }
                    }
                }

                // ACTION BUTTON
                MeetingActionButton(
                    activeMeeting = activeMeeting,
                    isLoading = isLoading,
                    onStartMeeting = onStartMeeting,
                    onEndRequest = { showEndConfirmation = true }
                )

                // INFO TEXT
                Text(
                    text = if (activeMeeting == null)
                        "Starting a meeting will log your visit time and location for this client."
                    else
                        "Add notes and attachments before ending the meeting. All data will be saved to your meeting history.",
                    style = AppTheme.typography.body2,
                    textAlign = TextAlign.Center,
                    color = AppTheme.colors.textSecondary,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        if (showEndConfirmation) {
            EndMeetingDialog(
                comments = comments,
                attachments = attachments,
                onCancel = { showEndConfirmation = false },
                onConfirm = {
                    showEndConfirmation = false
                    onEndMeeting(comments, attachments)
                }
            )
        }
    }
}


// -------------------- COMPONENTS -------------------------------

@Composable
private fun ClientInfoCard(client: Client) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AppTheme.colors.background)
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

            client.address?.let {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = AppTheme.colors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = it,
                        style = AppTheme.typography.body2,
                        color = AppTheme.colors.textSecondary
                    )
                }
            }

            client.phone?.let {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        tint = AppTheme.colors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = it,
                        style = AppTheme.typography.body2,
                        color = AppTheme.colors.textSecondary
                    )
                }
            }
        }
    }
}


@Composable
private fun MeetingActionButton(
    activeMeeting: Meeting?,
    isLoading: Boolean,
    onStartMeeting: () -> Unit,
    onEndRequest: () -> Unit
) {
    Button(
        onClick = if (activeMeeting == null) onStartMeeting else onEndRequest,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        enabled = !isLoading
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = AppTheme.colors.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (activeMeeting == null) "Starting..." else "Ending...",
                    style = AppTheme.typography.button
                )
            } else {
                Icon(
                    imageVector = if (activeMeeting == null) Icons.Default.PlayArrow else Icons.Default.Stop,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (activeMeeting == null) "Start Meeting" else "End Meeting",
                    style = AppTheme.typography.button
                )
            }
        }
    }
}



@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun MeetingDurationCard(meeting: Meeting) {
    val initial = remember(meeting.startTime) {
        try {
            Duration.between(Instant.parse(meeting.startTime), Instant.now())
        } catch (e: Exception) {
            Duration.ZERO
        }
    }

    var duration by remember { mutableStateOf(initial) }

    LaunchedEffect(Unit) {
        while (true) {
            duration = try {
                Duration.between(Instant.parse(meeting.startTime), Instant.now())
            } catch (e: Exception) {
                Duration.ZERO
            }
            kotlinx.coroutines.delay(1000)
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
                    text = formatDuration(duration),
                    style = AppTheme.typography.h2,
                    color = AppTheme.colors.primary
                )
            }

            Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = AppTheme.colors.primary
            )
        }
    }
}


@Composable
private fun AttachmentItem(uri: Uri, onRemove: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(AppTheme.colors.background)
            .padding(12.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = AppTheme.colors.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = uri.lastPathSegment ?: "File",
                    style = AppTheme.typography.body2,
                    color = AppTheme.colors.text
                )
            }

            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = AppTheme.colors.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}


@Composable
private fun EndMeetingDialog(
    comments: String,
    attachments: List<Uri>,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(20.dp))
                .background(AppTheme.colors.surface)
                .padding(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                Text(
                    text = "End Meeting?",
                    style = AppTheme.typography.h3,
                    color = AppTheme.colors.text
                )

                Text(
                    text = "Are you sure you want to end this meeting? Your notes and attachments will be saved.",
                    style = AppTheme.typography.body2,
                    color = AppTheme.colors.textSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Cancel",
                            color = AppTheme.colors.text
                        )
                    }

                    androidx.compose.material3.Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppTheme.colors.primary,
                            contentColor = AppTheme.colors.onPrimary
                        )
                    ) {
                        Text("End",
                            color = AppTheme.colors.black)
                    }
                }
            }
        }
    }
}


private fun formatDuration(d: Duration): String {
    val hours = d.toHours()
    val minutes = d.toMinutes() % 60
    val seconds = d.seconds % 60

    return if (hours > 0)
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    else
        String.format("%02d:%02d", minutes, seconds)
}
