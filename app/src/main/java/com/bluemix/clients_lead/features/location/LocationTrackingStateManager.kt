package com.bluemix.clients_lead.features.location

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * Centralized manager for location tracking state.
 *
 * This version keeps the original implementation completely intact AND now
 * integrates the smaller LocationTrackingManager so that toggles in Map and
 * toggles in Settings are always synchronized.
 */
class LocationTrackingStateManager(
    private val appContext: Context,
    private val trackingManager: LocationTrackingManager   // <-- injected small manager
) {

    private val _trackingState = MutableStateFlow(false)
    val trackingState: StateFlow<Boolean> = _trackingState.asStateFlow()

    init {
        updateTrackingState()
    }

    fun isCurrentlyTracking(): Boolean = _trackingState.value

    fun startTracking() {
        Timber.tag(TAG).d("Request received to START location tracking")

        // Permission validation kept unchanged
        if (!hasLocationPermissions()) {
            Timber.tag(TAG).e("❌ Cannot start tracking - Location permissions not granted!")
            _trackingState.value = false
            return
        }

        // Avoid duplicate start
        if (isServiceRunning(LocationTrackerService::class.java)) {
            Timber.tag(TAG).w("⚠️ Service already running, skipping start")
            _trackingState.value = true
            return
        }

        // NEW — Forward request to the small manager
        try {
            trackingManager.startTracking()
            Timber.tag(TAG).d("➡ Forwarded START to LocationTrackingManager")

            _trackingState.value = true
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ Failed forwarding start to small manager")
            _trackingState.value = false
        }

        updateTrackingState()
    }

    fun stopTracking() {
        Timber.tag(TAG).d("Request received to STOP location tracking")

        if (!isServiceRunning(LocationTrackerService::class.java)) {
            Timber.tag(TAG).w("⚠️ Service not running, skipping stop")
            _trackingState.value = false
            return
        }

        // NEW — Forward stop to the small manager
        try {
            trackingManager.stopTracking()
            Timber.tag(TAG).d("➡ Forwarded STOP to LocationTrackingManager")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ Failed forwarding stop to small manager")
        }

        _trackingState.value = false
        updateTrackingState()
    }

    fun updateTrackingState() {
        val running = isServiceRunning(LocationTrackerService::class.java)
        Timber.tag(TAG).d("Tracking state refreshed from system. isRunning = $running")
        _trackingState.value = running
    }

    private fun hasLocationPermissions(): Boolean {
        val fine = ContextCompat.checkSelfPermission(appContext, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(appContext, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @Suppress("DEPRECATION")
    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        return try {
            val manager = appContext.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                ?: return false

            val running = manager.getRunningServices(Int.MAX_VALUE).any {
                it.service.className == serviceClass.name
            }

            Timber.tag(TAG).d("Service running check = $running")
            running
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error checking if service is running")
            false
        }
    }

    companion object {
        private const val TAG = "LocationTrackingStateMgr"
    }
}
