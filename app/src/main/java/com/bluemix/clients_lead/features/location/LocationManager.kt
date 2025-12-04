package com.bluemix.clients_lead.features.location

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import android.location.LocationManager as AndroidLocationManager

class LocationManager(
    private val context: Context
) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    /**
     * Check if location permissions are granted
     */
    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Check if location services are enabled on the device
     */
    fun isLocationEnabled(): Boolean {
        val locationManager =
            context.getSystemService(Context.LOCATION_SERVICE) as AndroidLocationManager
        return locationManager.isProviderEnabled(AndroidLocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(AndroidLocationManager.NETWORK_PROVIDER)
    }

    /**
     * Get the last known location (one-time fetch)
     * @param onSuccess callback with formatted latitude and longitude
     * @param onError callback when location fetch fails
     */
    fun getLocation(
        onSuccess: (latitude: String, longitude: String) -> Unit,
        onError: (message: String) -> Unit = {}
    ) {
        // Check permissions
        if (!hasLocationPermission()) {
            onError("Location permission not granted")
            return
        }

        // Check if location services are enabled
        if (!isLocationEnabled()) {
            onError("Location services are disabled")
            return
        }

        try {
            fusedLocationClient
                .lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        val latitude = String.format("%.6f", location.latitude)
                        val longitude = String.format("%.6f", location.longitude)
                        onSuccess(latitude, longitude)
                    } else {
                        onError("Location not available. Try again.")
                    }
                }
                .addOnFailureListener { exception ->
                    onError("Failed to get location: ${exception.message}")
                }
        } catch (e: SecurityException) {
            onError("Security exception: ${e.message}")
        }
    }

    /**
     * Suspend version of getLocation for use in coroutines
     * @return Location object or null if unavailable
     */
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    suspend fun getLastKnownLocation(): Location? {
        if (!hasLocationPermission()) {
            throw SecurityException("Location permission not granted")
        }

        if (!isLocationEnabled()) {
            throw IllegalStateException("Location services are disabled")
        }

        return try {
            fusedLocationClient.lastLocation.await()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Track location updates using Flow
     * @param interval Update interval in milliseconds (default: 1000ms = 1 second)
     * @param priority Location accuracy priority (default: HIGH_ACCURACY)
     * @return Flow of Location objects
     */
    fun trackLocation(
        interval: Long = 1000L,
        priority: Int = Priority.PRIORITY_HIGH_ACCURACY
    ): Flow<Location> = callbackFlow {

        // Check permissions
        if (!hasLocationPermission()) {
            close(SecurityException("Location permission not granted"))
            return@callbackFlow
        }

        // Check if location services are enabled
        if (!isLocationEnabled()) {
            close(IllegalStateException("Location services are disabled"))
            return@callbackFlow
        }

        // Create location callback
        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                super.onLocationResult(result)
                result.locations.lastOrNull()?.let { location ->
                    trySend(location).isSuccess
                }
            }
        }

        // Create location request
        val request = LocationRequest.Builder(priority, interval)
            .setMinUpdateIntervalMillis(interval / 2) // Allow faster updates if available
            .setWaitForAccurateLocation(false) // Don't wait for accurate location
            .build()

        try {
            // Request location updates
            fusedLocationClient.requestLocationUpdates(
                request,
                locationCallback,
                Looper.getMainLooper()
            ).addOnFailureListener { exception ->
                close(exception)
            }
        } catch (e: SecurityException) {
            close(e)
        }

        // Cleanup when Flow is cancelled or closed
        awaitClose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    /**
     * Track location with balanced power consumption
     * Updates every 5 seconds with BALANCED_POWER_ACCURACY
     */
    fun trackLocationBalanced(): Flow<Location> =
        trackLocation(
            interval = 5000L,
            priority = Priority.PRIORITY_BALANCED_POWER_ACCURACY
        )

    /**
     * Track location with low power consumption
     * Updates every 10 seconds with LOW_POWER accuracy
     */
    fun trackLocationLowPower(): Flow<Location> =
        trackLocation(
            interval = 10000L,
            priority = Priority.PRIORITY_LOW_POWER
        )


}

/**
 * Extension function to format Location for display
 */
fun Location.toFormattedString(): String {
    val lat = String.format("%.6f", latitude)
    val lng = String.format("%.6f", longitude)
    return "Lat: $lat, Lng: $lng"
}

/**
 * Extension function to get short coordinates (last 4 digits after decimal)
 * Use only for display purposes, not for actual calculations
 */
fun Location.toShortString(): String {
    val latParts = latitude.toString().split(".")
    val lngParts = longitude.toString().split(".")

    val latShort = if (latParts.size > 1) {
        "${latParts[0]}.${latParts[1].take(4)}"
    } else {
        latParts[0]
    }

    val lngShort = if (lngParts.size > 1) {
        "${lngParts[0]}.${lngParts[1].take(4)}"
    } else {
        lngParts[0]
    }

    return "..$latShort / ..$lngShort"
}
