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
import kotlinx.coroutines.launch
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

    private val locationSettingsMonitor = LocationSettingsMonitor(appContext)
    private val _trackingState = MutableStateFlow(false)
    val trackingState: StateFlow<Boolean> = _trackingState.asStateFlow()

    init {
        updateTrackingState()
        startLocationSettingsMonitoring()  // 👈 Add this
    }

    private fun startLocationSettingsMonitoring() {
        locationSettingsMonitor.startMonitoring()

        // React to location settings changes
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            locationSettingsMonitor.isLocationEnabled.collect { enabled ->
                Timber.tag(TAG).d("Location enabled state changed: $enabled")

                // If location was disabled and service is running, stop it
                if (!enabled && isServiceRunning(LocationTrackerService::class.java)) {
                    Timber.tag(TAG).w("⚠️ Location disabled by user, stopping tracking service")
                    stopTracking()
                }

                // Update tracking state
                updateTrackingState()
            }
        }
    }

    // 👇 Add cleanup method
    fun cleanup() {
        locationSettingsMonitor.stopMonitoring()
    }







    fun isCurrentlyTracking(): Boolean = _trackingState.value

    fun isLocationEnabled(): Boolean {
        return locationSettingsMonitor.isLocationEnabled.value
    }


    suspend fun startTracking() {
        Timber.tag(TAG).d("Request received to START location tracking")

        if (!hasLocationPermissions()) {
            Timber.tag(TAG).e("❌ Cannot start tracking - Location permissions not granted!")
            _trackingState.value = false
            return
        }
        val locationManager = LocationManager(appContext)
        if (!locationManager.isLocationEnabled()) {
            Timber.tag(TAG).e("❌ Cannot start tracking - Location services disabled!")
            _trackingState.value = false
            return
        }

        if (isServiceRunning(LocationTrackerService::class.java)) {
            Timber.tag(TAG).w("⚠️ Service already running, skipping start")
            _trackingState.value = true
            return
        }

        try {
            trackingManager.startTracking()
            Timber.tag(TAG).d("➡️ Forwarded START to LocationTrackingManager")

            // ✅ Give service time to actually start
            kotlinx.coroutines.delay(150)

            updateTrackingState()
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ Failed forwarding start to small manager")
            _trackingState.value = false
        }
    }

    suspend fun stopTracking() {
        Timber.tag(TAG).d("Request received to STOP location tracking")

        if (!isServiceRunning(LocationTrackerService::class.java)) {
            Timber.tag(TAG).w("⚠️ Service not running, skipping stop")
            _trackingState.value = false
            return
        }

        try {
            trackingManager.stopTracking()
            Timber.tag(TAG).d("➡️ Forwarded STOP to LocationTrackingManager")

            // ✅ Give service time to actually stop
            kotlinx.coroutines.delay(150)

            // ✅ NOW verify the service actually stopped
            updateTrackingState()
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ Failed forwarding stop to small manager")
            _trackingState.value = false
        }
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
