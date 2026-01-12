// features/expense/presentation/components/MiniRouteMap.kt
package com.bluemix.clients_lead.features.expense.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    val cameraPositionState = rememberCameraPositionState()

    // ✅ Auto-fit map to show entire route
    LaunchedEffect(routePolyline) {
        try {
            val boundsBuilder = LatLngBounds.builder()
            routePolyline.forEach { boundsBuilder.include(it) }
            val bounds = boundsBuilder.build()

            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngBounds(bounds, 50)
            )
        } catch (e: Exception) {
            // Fallback to center on start
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
        // Mini Map
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(16.dp))
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
                // ✅ Draw route polyline
                Polyline(
                    points = routePolyline,
                    color = Color(0xFF5E92F3),
                    width = 8f
                )

                // ✅ Start marker (green)
                Marker(
                    state = MarkerState(position = startLocation),
                    title = "Start",
                    icon = com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(
                        com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_GREEN
                    )
                )

                // ✅ End marker (red)
                Marker(
                    state = MarkerState(position = endLocation),
                    title = "End",
                    icon = com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(
                        com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_RED
                    )
                )
            }

            // ✅ Transport mode badge
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
        }

        // Route Info Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Distance
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

            // Duration (if available)
            if (durationMinutes > 0) {
                Text(
                    text = formatDuration(durationMinutes),
                    style = AppTheme.typography.body2,
                    color = Color(0xFFB0B0B0),
                    fontSize = 13.sp
                )
            }

            // Route type indicator
            Text(
                text = "Route via $transportMode",
                style = AppTheme.typography.body3,
                color = Color(0xFF808080),
                fontSize = 11.sp
            )
        }
    }
}

private fun formatDuration(minutes: Int): String {
    return when {
        minutes < 60 -> "$minutes min"
        minutes < 1440 -> {
            val hours = minutes / 60
            val mins = minutes % 60
            "${hours}h ${mins}m"
        }
        else -> {
            val days = minutes / 1440
            val hours = (minutes % 1440) / 60
            "${days}d ${hours}h"
        }
    }
}