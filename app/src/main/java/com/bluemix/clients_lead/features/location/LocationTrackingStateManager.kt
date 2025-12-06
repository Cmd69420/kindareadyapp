package com.bluemix.clients_lead.features.location

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * Centralized manager for location tracking state.
 *
 * Responsibilities:
 * - Maintain an observable tracking state using [StateFlow]
 * - Start/stop [LocationTrackerService] using the appropriate foreground APIs
 * - Verify whether the service is actually running via system checks
 * - Provide an imperative `isCurrentlyTracking()` check for security enforcement
 */
class LocationTrackingStateManager(
    private val appContext: Context
) {

    private val _trackingState = MutableStateFlow(false)
    val trackingState: StateFlow<Boolean> = _trackingState.asStateFlow()

    init {
        // Initialize state from actual service status
        updateTrackingState()
    }

    /**
     * Returns the current in-memory tracking state.
     * This is always backed by the last system verification.
     */
    fun isCurrentlyTracking(): Boolean = _trackingState.value

    /**
     * Triggers the foreground [LocationTrackerService] with the START action.
     * Also refreshes the internal state after the start request.
     */
    fun startTracking() {
        Timber.tag(TAG).d("Request received to START location tracking")

        val intent = Intent(appContext, LocationTrackerService::class.java).apply {
            action = LocationTrackerService.Action.START.name
        }

        // Required for foreground services on modern Android
        ContextCompat.startForegroundService(appContext, intent)

        // Update state based on actual running status
        updateTrackingState()
    }

    /**
     * Sends a STOP action to [LocationTrackerService] and updates internal state.
     */
    fun stopTracking() {
        Timber.tag(TAG).d("Request received to STOP location tracking")

        val intent = Intent(appContext, LocationTrackerService::class.java).apply {
            action = LocationTrackerService.Action.STOP.name
        }

        // STOP can be sent as a normal service command
        appContext.startService(intent)

        // Update state based on actual running status
        updateTrackingState()
    }

    /**
     * Refreshes [trackingState] by checking whether [LocationTrackerService]
     * is actually running in the system.
     *
     * Uses [isTrackingServiceRunning] which leverages [android.app.ActivityManager]
     * and foreground notification presence.
     */
    fun updateTrackingState() {
        val running = isTrackingServiceRunning(appContext)
        Timber.tag(TAG).d("Tracking state refreshed from system. isRunning = $running")
        _trackingState.value = running
    }

    companion object {
        private const val TAG = "LocationTrackingStateMgr"
    }
}
