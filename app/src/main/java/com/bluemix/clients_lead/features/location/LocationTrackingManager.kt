package com.bluemix.clients_lead.features.location


import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat


/**
 * Manager for location tracking service.
 * Separates service management from ViewModel.
 */
class LocationTrackingManager(
    private val context: Context
) {
    fun startTracking() {
        val intent = Intent(context, LocationTrackerService::class.java).apply {
            action = LocationTrackerService.Action.START.name
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun stopTracking() {
        val intent = Intent(context, LocationTrackerService::class.java).apply {
            action = LocationTrackerService.Action.STOP.name
        }
        context.startService(intent)
    }
}
