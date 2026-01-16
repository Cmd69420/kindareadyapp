// features/expense/presentation/DraftExpenseListScreen.kt
package com.bluemix.clients_lead.features.expense.presentation

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bluemix.clients_lead.domain.model.DraftExpense
import kotlinx.coroutines.launch
import ui.AppTheme
import ui.components.Text
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DraftExpenseListScreen(
    drafts: List<DraftExpense>,
    onDraftClick: (String) -> Unit,
    onDeleteDraft: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF1A1A1A),
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Saved Drafts",
                        style = AppTheme.typography.h2,
                        color = Color.White
                    )
                    Text(
                        text = "${drafts.size} ${if (drafts.size == 1) "draft" else "drafts"}",
                        style = AppTheme.typography.body2,
                        color = Color(0xFF808080)
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }

            Divider(color = Color(0xFF2A2A2A))

            // Draft List
            if (drafts.isEmpty()) {
                EmptyDraftsState()
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = drafts,
                        key = { it.id }
                    ) { draft ->
                        DraftExpenseCard(
                            draft = draft,
                            onClick = { onDraftClick(draft.id) },
                            onDelete = { onDeleteDraft(draft.id) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DraftExpenseCard(
    draft: DraftExpense,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0D0D0D))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF2962FF).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (draft.isMultiLeg) Icons.Default.Route else Icons.Default.DirectionsCar,
                    contentDescription = null,
                    tint = Color(0xFF5E92F3),
                    modifier = Modifier.size(24.dp)
                )
            }

            // Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Title
                Text(
                    text = draft.tripName ?: buildString {
                        append(draft.startLocation?.displayName?.take(20) ?: "Unknown")
                        append(" → ")
                        append(draft.endLocation?.displayName?.take(20) ?: "Unknown")
                    },
                    style = AppTheme.typography.h3,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Details Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (draft.distanceKm > 0) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Straighten,
                                contentDescription = null,
                                tint = Color(0xFF808080),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "${String.format("%.1f", draft.distanceKm)} km",
                                style = AppTheme.typography.label2,
                                color = Color(0xFF808080)
                            )
                        }
                    }

                    if (draft.amountSpent > 0) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CurrencyRupee,
                                contentDescription = null,
                                tint = Color(0xFF808080),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = String.format("%.2f", draft.amountSpent),
                                style = AppTheme.typography.label2,
                                color = Color(0xFF808080)
                            )
                        }
                    }

                    if (draft.receiptImages.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Receipt,
                                contentDescription = null,
                                tint = Color(0xFF808080),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "${draft.receiptImages.size}",
                                style = AppTheme.typography.label2,
                                color = Color(0xFF808080)
                            )
                        }
                    }
                }

                // Last Modified
                Text(
                    text = "Modified ${formatRelativeTime(draft.lastModified)}",
                    style = AppTheme.typography.label3,
                    color = Color(0xFF606060)
                )
            }

            // Delete Button
            IconButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete draft",
                    tint = Color(0xFFFF5252),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    if (showDeleteDialog) {
        DeleteDraftDialog(
            onConfirm = {
                showDeleteDialog = false
                onDelete()
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

@Composable
private fun EmptyDraftsState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Inbox,
                contentDescription = null,
                tint = Color(0xFF404040),
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = "No saved drafts",
                style = AppTheme.typography.h3,
                color = Color(0xFF808080)
            )
            Text(
                text = "Your draft expenses will appear here",
                style = AppTheme.typography.body2,
                color = Color(0xFF606060)
            )
        }
    }
}

@Composable
private fun DeleteDraftDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A1A),
        tonalElevation = 8.dp,
        shape = RoundedCornerShape(20.dp),
        icon = {
            Icon(
                imageVector = Icons.Default.DeleteForever,
                contentDescription = null,
                tint = Color(0xFFFF5252),
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Delete Draft?",
                style = AppTheme.typography.h3,
                color = Color.White
            )
        },
        text = {
            Text(
                text = "This action cannot be undone. The draft and all its data will be permanently deleted.",
                style = AppTheme.typography.body2,
                color = Color(0xFFB0B0B0)
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF5252),
                    contentColor = Color.White
                )
            ) {
                Text("Delete", style = AppTheme.typography.button)
            }
        },
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

private fun formatRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> "just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        diff < 604800_000 -> "${diff / 86400_000}d ago"
        else -> {
            val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}