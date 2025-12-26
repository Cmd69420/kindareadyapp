package com.bluemix.clients_lead.features.location


import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.app.ActivityManager
import com.bluemix.clients_lead.core.network.ApiEndpoints
import android.os.Binder
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import com.bluemix.clients_lead.features.location.BatteryUtils
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.bluemix.clients_lead.R
import com.bluemix.clients_lead.core.common.utils.AppResult
import com.bluemix.clients_lead.domain.repository.ILocationRepository
import com.bluemix.clients_lead.domain.usecases.InsertLocationLog
import com.bluemix.clients_lead.core.network.SessionManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import timber.log.Timber

/**
 * Foreground service for continuous location tracking with periodic database saves.
 *
 * Features:
 * - Real-time location updates via SharedFlow
 * - Configurable periodic database saves (default: 5 minutes)
 * - Proper lifecycle management and memory leak prevention
 * - Authentication checks before starting
 */
class LocationTrackerService : Service() {

    // Lifecycle-aware coroutine management
    private val serviceJob = SupervisorJob()
    private val scope = CoroutineScope(serviceJob + Dispatchers.IO)

    private val httpClient: HttpClient by inject()
    private val sessionManager: SessionManager by inject()
    private val locationRepository: ILocationRepository by inject()
    private val insertLocationLog: InsertLocationLog by inject()

    // SharedFlow for broadcasting location updates to UI
    private val _locationFlow = MutableSharedFlow<Location>(replay = 1)
    val locationFlow: SharedFlow<Location> = _locationFlow

    // Tracking state
    private var locationTrackingJob: Job? = null
    private var periodicSaveJob: Job? = null
    private var latestLocation: Location? = null
    private var lastSavedTime = System.currentTimeMillis()

    // Configuration
    private val saveInterval = 1 * 60 * 1000L // 1 minute (configurable)

    // Notification components
    private lateinit var notificationManager: NotificationManager
    private lateinit var notificationBuilder: NotificationCompat.Builder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // CRITICAL: Call startForeground() IMMEDIATELY before doing anything else
        // This MUST happen in onStartCommand(), not in start()

        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationBuilder = NotificationCompat.Builder(this, LOCATION_CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Location Tracker")
            .setContentText("Starting location tracking...")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)

        // START FOREGROUND IMMEDIATELY
        startForeground(NOTIFICATION_ID, notificationBuilder.build())

        // Now handle the intent
        when (intent?.action) {
            Action.START.name -> {
                val userId = sessionManager.getCurrentUserId()
                if (userId != null) {
                    start(userId)
                } else {
                    Timber.e("Cannot start tracking: User not authenticated")
                    stop()
                }
            }
            Action.STOP.name -> stop()
        }

        return START_NOT_STICKY

    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Timber.w("🚫 App removed from recents → stopping location tracking service")
        stop()
        super.onTaskRemoved(rootIntent)
    }


    override fun onBind(intent: Intent?): IBinder {
        return LocationBinder()
    }

    inner class LocationBinder : Binder() {
        fun getService(): LocationTrackerService = this@LocationTrackerService
    }

    private fun start(userId: String) {
        // Prevent duplicate start
        if (locationTrackingJob?.isActive == true) {
            Timber.w("Location tracking already running")
            return
        }

        Timber.d("Starting location tracking for user: $userId")

        val locationManager = LocationManager(applicationContext)
        if (!locationManager.hasLocationPermission()) {
            Timber.e("Cannot start tracking: Location permission not granted")
            stop()
            return
        }
        if (!locationManager.isLocationEnabled()) {
            Timber.e("Cannot start tracking: Location services disabled")
            stop()
            return
        }


        // Start location tracking
        locationTrackingJob = scope.launch {
            try {
                locationManager.trackLocation().collect { location ->

                    if (!locationManager.isLocationEnabled()) {
                        Timber.w("🚫 GPS turned OFF while service running → stopping service")
                        stop()
                        return@collect
                    }

                    latestLocation = location
                    _locationFlow.emit(location)

                    val latitude = String.format("%.4f", location.latitude)
                    val longitude = String.format("%.4f", location.longitude)

                    notificationManager.notify(
                        NOTIFICATION_ID,
                        notificationBuilder
                            .setContentText("Location: $latitude / $longitude")
                            .build()
                    )
                }

            } catch (e: CancellationException) {
                Timber.d("Location tracking cancelled")
                throw e // Re-throw to properly cancel coroutine
            } catch (e: Exception) {
                Timber.e(e, "Location tracking error")
                stop()
            }
        }

        // Start periodic database save
        startPeriodicDatabaseSave(userId)
    }

    private fun startPeriodicDatabaseSave(userId: String) {
        // Prevent duplicate periodic saves
        if (periodicSaveJob?.isActive == true) {
            Timber.w("Periodic save already running")
            return
        }

        Timber.d("Starting periodic database save (interval: ${saveInterval / 1000}s)")

        periodicSaveJob = scope.launch {
            // Wait until first valid GPS fix is received
            while (latestLocation == null && isActive) {
                Timber.d("Waiting for first GPS fix before initial DB save…")
                delay(1000)
            }

            // FIRST SAVE IMMEDIATELY
            latestLocation?.let { location ->
                Timber.d("Saving FIRST location immediately (clock-in)")
                saveLocationToDatabase(userId, location)
                lastSavedTime = System.currentTimeMillis()
            }

            // START PERIODIC SAVING
            delay(saveInterval)

            while (isActive) {
                val currentTime = System.currentTimeMillis()
                val timeSinceLastSave = currentTime - lastSavedTime

                if (timeSinceLastSave >= saveInterval) {
                    latestLocation?.let { location ->
                        Timber.d("Saving location to database (periodic update)")
                        saveLocationToDatabase(userId, location)
                        lastSavedTime = currentTime
                    } ?: Timber.w("No GPS fix available to save")
                }

                // Check every minute
                delay(60 * 1000L)
            }
        }
    }

    suspend fun clearUserPincode() {
        try {
            httpClient.post(ApiEndpoints.User.CLEAR_PINCODE)
            Timber.d("Pincode cleared successfully on backend")
        } catch (e: Exception) {
            Timber.e("Failed to clear pincode: ${e.message}")
        }
    }

    private suspend fun saveLocationToDatabase(userId: String, location: Location) {
        val battery = BatteryUtils.getBatteryPercentage(this)
        try {
            when (val result = insertLocationLog(
                userId = userId,
                latitude = location.latitude,
                longitude = location.longitude,
                accuracy = location.accuracy.toDouble(),
                battery = battery
            )) {
                is AppResult.Success -> {
                    Timber.d("Location saved: ${result.data.id} at ${result.data.timestamp} | Battery: $battery%")
                }
                is AppResult.Error -> {
                    Timber.e(result.error.cause, "Failed to save location: ${result.error.message}")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception while saving location")
        }
    }

    private fun stop() {
        Timber.d("Stopping location tracking service")

        scope.launch {
            clearUserPincode()
        }

        // Cancel tracking jobs
        locationTrackingJob?.cancel()
        periodicSaveJob?.cancel()

        // Clear references
        latestLocation = null

        // Stop foreground and service
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()

        // Ensure all jobs are cancelled
        locationTrackingJob?.cancel()
        periodicSaveJob?.cancel()
        serviceJob.cancelChildren()
        serviceJob.cancel()
        scope.cancel()

        Timber.d("Service destroyed")
    }

    enum class Action {
        START, STOP
    }

    companion object {
        const val LOCATION_CHANNEL = "location_channel"
        private const val NOTIFICATION_ID = 1
    }
}

fun isTrackingServiceRunning(context: Context): Boolean {
    try {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (service.service.className == LocationTrackerService::class.java.name) {
                return true
            }
        }
    } catch (_: Exception) {}

    // Fallback check – foreground service notification exists
    val notificationServiceId = 1  // same as NOTIFICATION_ID in service
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val notifications = notificationManager.activeNotifications
    return notifications.any { it.id == notificationServiceId }
}