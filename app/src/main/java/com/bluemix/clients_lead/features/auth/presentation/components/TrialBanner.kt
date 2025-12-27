package com.bluemix.clients_lead.features.auth.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ui.AppTheme
import ui.components.Text

@Composable
fun TrialBanner(
    daysRemaining: Long,
    isExpired: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isExpired || daysRemaining <= 7,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (isExpired) {
                        Brush.horizontalGradient(
                            colors = listOf(
                                AppTheme.colors.error.copy(alpha = 0.15f),
                                AppTheme.colors.error.copy(alpha = 0.1f)
                            )
                        )
                    } else {
                        Brush.horizontalGradient(
                            colors = listOf(
                                AppTheme.colors.primary.copy(alpha = 0.15f),
                                AppTheme.colors.primary.copy(alpha = 0.1f)
                            )
                        )
                    }
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isExpired) Icons.Default.Warning else Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = if (isExpired) AppTheme.colors.error else AppTheme.colors.primary
                )

                if (isExpired) {
                    Text(
                        text = "Trial period has ended. Please contact support.",
                        style = AppTheme.typography.body2,
                        color = AppTheme.colors.error,
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    Text(
                        text = "$daysRemaining day${if (daysRemaining != 1L) "s" else ""} remaining in trial",
                        style = AppTheme.typography.body2,
                        color = AppTheme.colors.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}