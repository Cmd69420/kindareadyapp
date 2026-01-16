// features/expense/presentation/components/MiniRouteMap.kt
package com.bluemix.clients_lead.features.expense.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*
import ui.AppTheme

@Composable
fun MiniRouteMap(
    routePolyline: List<LatLng>,
    startLocation: LatLng,
    endLocation: LatLng,
    distanceKm: Double,
    durationMinutes: Int,
    transportMode: String,
    modifier: Modifier = Modifier
) {
    if (routePolyline.isEmpty()) return

    var showExpandedMap by remember { mutableStateOf(false) }

    // Mini Map Preview (Clickable)
    MiniMapPreview(
        routePolyline = routePolyline,
        startLocation = startLocation,
        endLocation = endLocation,
        distanceKm = distanceKm,
        durationMinutes = durationMinutes,
        transportMode = transportMode,
        onClick = { showExpandedMap = true },
        modifier = modifier
    )

    // Full Screen Map Dialog
    if (showExpandedMap) {
        ExpandedMapDialog(
            routePolyline = routePolyline,
            startLocation = startLocation,
            endLocation = endLocation,
            distanceKm = distanceKm,
            durationMinutes = durationMinutes,
            transportMode = transportMode,
            onDismiss = { showExpandedMap = false }
        )
    }
}

@Composable
private fun MiniMapPreview(
    routePolyline: List<LatLng>,
    startLocation: LatLng,
    endLocation: LatLng,
    distanceKm: Double,
    durationMinutes: Int,
    transportMode: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cameraPositionState = rememberCameraPositionState()

    LaunchedEffect(routePolyline) {
        try {
            val boundsBuilder = LatLngBounds.builder()
            routePolyline.forEach { boundsBuilder.include(it) }
            val bounds = boundsBuilder.build()

            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngBounds(bounds, 50)
            )
        } catch (e: Exception) {
            cameraPositionState.position = CameraPosition.fromLatLngZoom(startLocation, 10f)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0D0D0D)),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Clickable Map
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onClick)
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    isMyLocationEnabled = false,
                    mapType = MapType.NORMAL
                ),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    compassEnabled = false,
                    myLocationButtonEnabled = false,
                    scrollGesturesEnabled = false,
                    zoomGesturesEnabled = false,
                    tiltGesturesEnabled = false,
                    rotationGesturesEnabled = false
                )
            ) {
                Polyline(
                    points = routePolyline,
                    color = Color(0xFF5E92F3),
                    width = 8f
                )

                Marker(
                    state = MarkerState(position = startLocation),
                    title = "Start",
                    icon = com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(
                        com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_GREEN
                    )
                )

                Marker(
                    state = MarkerState(position = endLocation),
                    title = "End",
                    icon = com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(
                        com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_RED
                    )
                )
            }

            // Transport mode badge
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1A1A1A).copy(alpha = 0.9f))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = transportMode,
                    style = AppTheme.typography.label2,
                    color = Color(0xFF5E92F3),
                    fontSize = 11.sp
                )
            }

            // ✅ "Tap to Expand" hint
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
                    Icon(
                        imageVector = Icons.Default.ZoomOutMap,
                        contentDescription = null,
                        tint = Color(0xFF5E92F3),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Tap to expand",
                        style = AppTheme.typography.label3,
                        color = Color(0xFF5E92F3),
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Route Info Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    tint = Color(0xFF5E92F3),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "${String.format("%.1f", distanceKm)} KM",
                    style = AppTheme.typography.body2,
                    color = Color.White,
                    fontSize = 13.sp
                )
            }

            if (durationMinutes > 0) {
                Text(
                    text = formatDuration(durationMinutes),
                    style = AppTheme.typography.body2,
                    color = Color(0xFFB0B0B0),
                    fontSize = 13.sp
                )
            }

            Text(
                text = "Route via $transportMode",
                style = AppTheme.typography.body3,
                color = Color(0xFF808080),
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun ExpandedMapDialog(
    routePolyline: List<LatLng>,
    startLocation: LatLng,
    endLocation: LatLng,
    distanceKm: Double,
    durationMinutes: Int,
    transportMode: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            val cameraPositionState = rememberCameraPositionState()

            LaunchedEffect(routePolyline) {
                try {
                    val boundsBuilder = LatLngBounds.builder()
                    routePolyline.forEach { boundsBuilder.include(it) }
                    val bounds = boundsBuilder.build()

                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngBounds(bounds, 100)
                    )
                } catch (e: Exception) {
                    cameraPositionState.position = CameraPosition.fromLatLngZoom(startLocation, 12f)
                }
            }

            // Full screen interactive map
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    isMyLocationEnabled = false,
                    mapType = MapType.NORMAL
                ),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = true,
                    compassEnabled = true,
                    myLocationButtonEnabled = false,
                    scrollGesturesEnabled = true,
                    zoomGesturesEnabled = true,
                    tiltGesturesEnabled = true,
                    rotationGesturesEnabled = true
                )
            ) {
                Polyline(
                    points = routePolyline,
                    color = Color(0xFF5E92F3),
                    width = 12f
                )

                Marker(
                    state = MarkerState(position = startLocation),
                    title = "Start Location",
                    snippet = "Trip begins here",
                    icon = com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(
                        com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_GREEN
                    )
                )

                Marker(
                    state = MarkerState(position = endLocation),
                    title = "End Location",
                    snippet = "Trip ends here",
                    icon = com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(
                        com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_RED
                    )
                )
            }

            // Top Bar with Route Info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            ) {
                // Close button and title
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1A1A1A).copy(alpha = 0.95f))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Route Preview",
                        style = AppTheme.typography.h3,
                        color = Color.White
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }

                // Route details card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1A1A1A).copy(alpha = 0.95f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Transport mode
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when (transportMode.uppercase()) {
                                    "BUS" -> Icons.Default.DirectionsBus
                                    "TRAIN" -> Icons.Default.Train
                                    "BIKE" -> Icons.Default.DirectionsBike
                                    "CAR", "RICKSHAW" -> Icons.Default.DirectionsCar
                                    else -> Icons.Default.Route
                                },
                                contentDescription = null,
                                tint = Color(0xFF5E92F3),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = transportMode,
                                style = AppTheme.typography.h3,
                                color = Color(0xFF5E92F3)
                            )
                        }

                        Divider(color = Color(0xFF2A2A2A))

                        // Distance
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Distance",
                                style = AppTheme.typography.body2,
                                color = Color(0xFFB0B0B0)
                            )
                            Text(
                                text = "${String.format("%.1f", distanceKm)} KM",
                                style = AppTheme.typography.body1,
                                color = Color.White
                            )
                        }

                        // Duration
                        if (durationMinutes > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Estimated Time",
                                    style = AppTheme.typography.body2,
                                    color = Color(0xFFB0B0B0)
                                )
                                Text(
                                    text = formatDuration(durationMinutes),
                                    style = AppTheme.typography.body1,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // Map controls hint at bottom
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1A1A1A).copy(alpha = 0.8f))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Pinch to zoom • Drag to pan",
                    style = AppTheme.typography.label2,
                    color = Color(0xFFB0B0B0),
                    fontSize = 12.sp
                )
            }
        }
    }
}

private fun formatDuration(minutes: Int): String {
    return when {
        minutes < 60 -> "$minutes min"
        minutes < 1440 -> {
            val hours = minutes / 60
            val mins = minutes % 60
            if (mins > 0) "${hours}h ${mins}m" else "${hours}h"
        }
        else -> {
            val days = minutes / 1440
            val hours = (minutes % 1440) / 60
            "${days}d ${hours}h"
        }
    }
}