package com.bluemix.clients_lead.features.Clients.presentation.components

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bluemix.clients_lead.domain.model.Client
import com.bluemix.clients_lead.domain.model.VisitStatus
import ui.AppTheme

/**
 * Card displaying last visit information for a client
 * Shows:
 * - Time since last visit
 * - Visit status indicator
 * - Last visit notes (if any)
 * - Visit history button
 */
@Composable
fun LastVisitCard(
    client: Client,
    onViewHistory: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val visitStatus = client.getVisitStatusColor()
    val lastVisit = client.getFormattedLastVisit()

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = AppTheme.colors.surface
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
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
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = AppTheme.colors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Last Visit",
                        style = AppTheme.typography.h4,
                        color = AppTheme.colors.text
                    )
                }

                // Status indicator
                VisitStatusBadge(visitStatus)
            }

            Divider(color = AppTheme.colors.onSurface.copy(alpha = 0.1f))

            // Visit time and status
            if (lastVisit != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = lastVisit,
                            style = AppTheme.typography.h3,
                            color = AppTheme.colors.text,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = getVisitStatusMessage(visitStatus),
                            style = AppTheme.typography.body2,
                            color = AppTheme.colors.textSecondary
                        )
                    }

                    IconButton(onClick = onViewHistory) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "View visit history",
                            tint = AppTheme.colors.primary
                        )
                    }
                }

                // Last visit notes
                client.lastVisitNotes?.let { notes ->
                    AnimatedVisibility(
                        visible = notes.isNotBlank(),
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(AppTheme.colors.background)
                                .padding(12.dp)
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Notes:",
                                    style = AppTheme.typography.label2,
                                    color = AppTheme.colors.textSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = notes,
                                    style = AppTheme.typography.body2,
                                    color = AppTheme.colors.text
                                )
                            }
                        }
                    }
                }
            } else {
                // Never visited
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.EventBusy,
                        contentDescription = null,
                        tint = AppTheme.colors.textSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "No visits recorded yet",
                        style = AppTheme.typography.body1,
                        color = AppTheme.colors.textSecondary
                    )
                    Text(
                        text = "Start a meeting to log your first visit",
                        style = AppTheme.typography.body2,
                        color = AppTheme.colors.textSecondary.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun VisitStatusBadge(status: VisitStatus) {
    val (backgroundColor, textColor, icon, label) = when (status) {
        VisitStatus.NEVER_VISITED -> listOf(
            Color(0xFFEA4335).copy(alpha = 0.15f),
            Color(0xFFEA4335),
            Icons.Default.Error,
            "Never Visited"
        )
        VisitStatus.RECENT -> listOf(
            Color(0xFF34A853).copy(alpha = 0.15f),
            Color(0xFF34A853),
            Icons.Default.CheckCircle,
            "Recent"
        )
        VisitStatus.MODERATE -> listOf(
            Color(0xFFFBBC04).copy(alpha = 0.15f),
            Color(0xFFFBBC04),
            Icons.Default.Warning,
            "Follow-up Soon"
        )
        VisitStatus.OVERDUE -> listOf(
            Color(0xFFFF6D00).copy(alpha = 0.15f),
            Color(0xFFFF6D00),
            Icons.Default.PriorityHigh,
            "Overdue"
        )
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor as Color)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon as androidx.compose.ui.graphics.vector.ImageVector,
                contentDescription = null,
                tint = textColor as Color,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label as String,
                style = AppTheme.typography.label2,
                color = textColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun getVisitStatusMessage(status: VisitStatus): String {
    return when (status) {
        VisitStatus.NEVER_VISITED -> "This client has never been visited"
        VisitStatus.RECENT -> "Recently contacted - follow-up not urgent"
        VisitStatus.MODERATE -> "Consider scheduling a follow-up visit soon"
        VisitStatus.OVERDUE -> "High priority - visit overdue"
    }
}

/**
 * Compact version for list items
 */
@Composable
fun LastVisitChip(
    client: Client,
    modifier: Modifier = Modifier
) {
    val visitStatus = client.getVisitStatusColor()
    val lastVisit = client.getFormattedLastVisit() ?: "Never"

    val (backgroundColor, textColor, icon) = when (visitStatus) {
        VisitStatus.NEVER_VISITED -> Triple(
            Color(0xFFEA4335).copy(alpha = 0.1f),
            Color(0xFFEA4335),
            "❌"
        )
        VisitStatus.RECENT -> Triple(
            Color(0xFF34A853).copy(alpha = 0.1f),
            Color(0xFF34A853),
            "✓"
        )
        VisitStatus.MODERATE -> Triple(
            Color(0xFFFBBC04).copy(alpha = 0.1f),
            Color(0xFFFBBC04),
            "⚠"
        )
        VisitStatus.OVERDUE -> Triple(
            Color(0xFFFF6D00).copy(alpha = 0.1f),
            Color(0xFFFF6D00),
            "⚡"
        )
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = icon,
                fontSize = 10.sp
            )
            Text(
                text = lastVisit,
                style = AppTheme.typography.label2,
                color = textColor,
                fontSize = 11.sp
            )
        }
    }
}