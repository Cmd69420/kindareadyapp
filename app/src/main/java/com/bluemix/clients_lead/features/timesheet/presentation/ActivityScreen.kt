package com.bluemix.clients_lead.features.timesheet.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsNotFixed
import androidx.compose.material.icons.filled.GpsOff
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bluemix.clients_lead.domain.model.LocationLog
import com.bluemix.clients_lead.features.timesheet.vm.ActivityViewModel
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel
import ui.AppTheme
import ui.components.Scaffold
import ui.components.topbar.TopBar
import ui.components.topbar.TopBarDefaults
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ActivityScreen(
    viewModel: ActivityViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

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
                    ui.components.Text(
                        text = "Activity",
                        style = AppTheme.typography.h2,
                        color = AppTheme.colors.text
                    )

                    // Stats Badge
                    AnimatedVisibility(
                        visible = uiState.logs.isNotEmpty(),
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut() + scaleOut()
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(AppTheme.colors.primary.copy(alpha = 0.1f))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            ui.components.Text(
                                text = "${uiState.logs.size} logs",
                                style = AppTheme.typography.label2,
                                color = AppTheme.colors.primary
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Crossfade(
            targetState = when {
                uiState.isLoading -> "loading"
                uiState.error != null -> "error"
                uiState.logs.isEmpty() -> "empty"
                else -> "content"
            },
            animationSpec = tween(300),
            label = "activityCrossfade"
        ) { state ->
            when (state) {
                "loading" -> LoadingContent(paddingValues)
                "error" -> ErrorContent(
                    paddingValues = paddingValues,
                    error = uiState.error ?: "Unknown error",
                    onRetry = { viewModel.refresh() }
                )

                "empty" -> EmptyContent(paddingValues)
                "content" -> AnimatedActivityContent(
                    paddingValues = paddingValues,
                    logs = uiState.logs
                )
            }
        }
    }
}

@Composable
private fun LoadingContent(paddingValues: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "loadingPulse")
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.9f,
                targetValue = 1.1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "loadingScale"
            )

            Icon(
                imageVector = Icons.Default.LocationSearching,
                contentDescription = null,
                modifier = Modifier
                    .size(64.dp)
                    .scale(scale),
                tint = AppTheme.colors.primary
            )
            ui.components.Text(
                text = "Loading activity logs...",
                style = AppTheme.typography.body1,
                color = AppTheme.colors.textSecondary
            )
        }
    }
}

@Composable
private fun ErrorContent(
    paddingValues: PaddingValues,
    error: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = AppTheme.colors.error
            )
            ui.components.Text(
                text = error,
                style = AppTheme.typography.body1,
                color = AppTheme.colors.error,
                textAlign = TextAlign.Center
            )
            Button(onClick = onRetry) {
                ui.components.Text("Retry")
            }
        }
    }
}

@Composable
private fun EmptyContent(paddingValues: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.LocationOff,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = AppTheme.colors.textDisabled
            )
            ui.components.Text(
                text = "No location logs yet",
                style = AppTheme.typography.h2,
                color = AppTheme.colors.text
            )
            ui.components.Text(
                text = "Enable location tracking in settings\nto see your activity history",
                style = AppTheme.typography.body2,
                color = AppTheme.colors.textSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun AnimatedActivityContent(
    paddingValues: PaddingValues,
    logs: List<LocationLog>
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        itemsIndexed(
            items = logs,
            key = { _, log -> log.id }
        ) { index, log ->
            TimelineLocationLogItem(
                log = log,
                index = index,
                isLast = index == logs.lastIndex
            )
        }
    }
}

@Composable
private fun TimelineLocationLogItem(
    log: LocationLog,
    index: Int,
    isLast: Boolean
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay((index * 60).toLong())
        isVisible = true
    }

    val offsetX by animateDpAsState(
        targetValue = if (isVisible) 0.dp else (-20).dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "itemOffsetX"
    )

    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(400),
        label = "itemAlpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .offset(x = offsetX)
            .graphicsLayer { this.alpha = alpha }
    ) {
        // Timeline Indicator Column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(32.dp)
        ) {
            // Timeline Dot with pulse
            val infiniteTransition = rememberInfiniteTransition(label = "dotPulse_$index")
            val dotScale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.2f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500, delayMillis = index * 200),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dotScale"
            )

            Box(
                modifier = Modifier
                    .size(12.dp)
                    .scale(dotScale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                AppTheme.colors.primary,
                                AppTheme.colors.primary.copy(alpha = 0.6f)
                            )
                        )
                    )
            )

            // Timeline Line
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(100.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    AppTheme.colors.primary.copy(alpha = 0.3f),
                                    AppTheme.colors.primary.copy(alpha = 0.1f)
                                )
                            )
                        )
                )
            }
        }

        // Content Card
        AnimatedLogCard(log = log, index = index)
    }
}

@Composable
private fun AnimatedLogCard(log: LocationLog, index: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, bottom = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(AppTheme.colors.surface)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header with Time and Accuracy
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = AppTheme.colors.primary
                    )
                    ui.components.Text(
                        text = formatTimestamp(log.timestamp),
                        style = AppTheme.typography.h4,
                        color = AppTheme.colors.text
                    )
                }

                log.accuracy?.let { accuracy ->
                    AccuracyBadge(accuracy = accuracy)
                }
            }

            // Location Coordinates Card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppTheme.colors.background)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CoordinateColumn(
                    label = "Latitude",
                    value = String.format("%.6f", log.latitude),
                    modifier = Modifier.weight(1f)
                )

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(40.dp)
                        .background(AppTheme.colors.disabled)
                )

                CoordinateColumn(
                    label = "Longitude",
                    value = String.format("%.6f", log.longitude),
                    modifier = Modifier.weight(1f)
                )
            }

            // Quick Copy Location
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = AppTheme.colors.success
                )
                ui.components.Text(
                    text = "${String.format("%.4f", log.latitude)}, ${
                        String.format(
                            "%.4f",
                            log.longitude
                        )
                    }",
                    style = AppTheme.typography.label3,
                    color = AppTheme.colors.textSecondary
                )
            }
        }
    }
}

@Composable
private fun AccuracyBadge(accuracy: Double) {
    val backgroundColor by animateColorAsState(
        targetValue = when {
            accuracy < 20 -> AppTheme.colors.success.copy(alpha = 0.15f)
            accuracy < 50 -> AppTheme.colors.tertiary.copy(alpha = 0.15f)
            else -> AppTheme.colors.error.copy(alpha = 0.15f)
        },
        animationSpec = tween(300),
        label = "accuracyBackground"
    )

    val textColor = when {
        accuracy < 20 -> AppTheme.colors.success
        accuracy < 50 -> AppTheme.colors.tertiary
        else -> AppTheme.colors.error
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(backgroundColor)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = when {
                accuracy < 20 -> Icons.Default.GpsFixed
                accuracy < 50 -> Icons.Default.GpsNotFixed
                else -> Icons.Default.GpsOff
            },
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = textColor
        )
        ui.components.Text(
            text = "±${accuracy.toInt()}m",
            style = AppTheme.typography.label3,
            color = textColor
        )
    }
}

@Composable
private fun CoordinateColumn(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ui.components.Text(
            text = label,
            style = AppTheme.typography.label3,
            color = AppTheme.colors.textSecondary
        )
        Spacer(modifier = Modifier.height(4.dp))
        ui.components.Text(
            text = value,
            style = AppTheme.typography.body2,
            color = AppTheme.colors.text
        )
    }
}

private fun formatTimestamp(timestamp: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
        val date = inputFormat.parse(timestamp)
        outputFormat.format(date ?: Date())
    } catch (e: Exception) {
        timestamp
    }
}
