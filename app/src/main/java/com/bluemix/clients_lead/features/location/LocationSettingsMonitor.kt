// FILE: app/src/main/java/com/bluemix/clients_lead/features/location/LocationSettingsMonitor.kt
// ============================================
package com.bluemix.clients_lead.features.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * Monitors system location settings changes (GPS on/off from quick settings)
 */
class LocationSettingsMonitor(private val context: Context) {

    private val _isLocationEnabled = MutableStateFlow(false)
    val isLocationEnabled: StateFlow<Boolean> = _isLocationEnabled.asStateFlow()

    private val locationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == LocationManager.PROVIDERS_CHANGED_ACTION) {
                val enabled = checkLocationEnabled()
                Timber.d("📍 Location settings changed: enabled=$enabled")
                _isLocationEnabled.value = enabled
            }
        }
    }

    fun startMonitoring() {
        // Initial check
        _isLocationEnabled.value = checkLocationEnabled()

        // Register receiver
        val filter = IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION)
        context.registerReceiver(locationReceiver, filter)
        Timber.d("✅ Location settings monitoring started")
    }

    fun stopMonitoring() {
        try {
            context.unregisterReceiver(locationReceiver)
            Timber.d("🛑 Location settings monitoring stopped")
        } catch (e: Exception) {
            Timber.e(e, "Failed to unregister location receiver")
        }
    }

    private fun checkLocationEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
}