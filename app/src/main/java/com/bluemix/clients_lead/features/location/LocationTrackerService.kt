package com.bluemix.clients_lead.features.location


import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.Binder
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


    private val sessionManager: SessionManager by inject()
    val userId = sessionManager.getCurrentUserId()

    private val locationRepository: ILocationRepository by inject()

    // SharedFlow for broadcasting location updates to UI
    private val _locationFlow = MutableSharedFlow<Location>(replay = 1)
    val locationFlow: SharedFlow<Location> = _locationFlow
    private val insertLocationLog: InsertLocationLog by inject() // Use case
    // Tracking state
    private var locationTrackingJob: Job? = null
    private var periodicSaveJob: Job? = null
    private var latestLocation: Location? = null
    private var lastSavedTime = System.currentTimeMillis()

    // Configuration
    private val saveInterval = 5 * 60 * 1000L // 5 minutes (configurable)

    override fun onBind(intent: Intent?): IBinder {
        return LocationBinder()
    }

    inner class LocationBinder : Binder() {
        fun getService(): LocationTrackerService = this@LocationTrackerService
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            Action.START.name -> {
                // Check authentication before starting
                scope.launch {
                    val userId = sessionManager.getCurrentUserId()
                    if (userId != null) {
                        start(userId)
                    } else {
                        Timber.e("Cannot start location tracking: User not authenticated")
                        stopSelf()
                    }
                }

            }

            Action.STOP.name -> stop()
        }
        // Don't restart service if killed by system
        return START_NOT_STICKY
    }

    private fun start(userId: String) {
        // Prevent duplicate start
        if (locationTrackingJob?.isActive == true) {
            Timber.w("Location tracking already running")
            return
        }

        Timber.d("Starting location tracking for user: $userId")

        val locationManager = LocationManager(applicationContext)
        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat
            .Builder(this, LOCATION_CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Location Tracker")
            .setContentText("Tracking location...")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)

        // Start foreground service
        startForeground(NOTIFICATION_ID, notification.build())

        // Start location tracking
        locationTrackingJob = scope.launch {
            try {
                locationManager.trackLocation().collect { location ->
                    latestLocation = location

                    // Emit to UI
                    _locationFlow.emit(location)

                    // Update notification
                    val latitude = String.format("%.4f", location.latitude)
                    val longitude = String.format("%.4f", location.longitude)

                    notificationManager.notify(
                        NOTIFICATION_ID,
                        notification.setContentText("Location: $latitude / $longitude").build()
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
            // Initial delay to avoid immediate save on start
            delay(saveInterval)

            while (isActive) {
                val currentTime = System.currentTimeMillis()
                val timeSinceLastSave = currentTime - lastSavedTime

                if (timeSinceLastSave >= saveInterval) {
                    latestLocation?.let { location ->
                        Timber.d("Saving location to database (periodic trigger)")
                        saveLocationToDatabase(userId, location)
                        lastSavedTime = currentTime
                    } ?: Timber.w("No location data available to save")
                }

                // Check every minute
                delay(60 * 1000L)
            }
        }
    }

    private suspend fun saveLocationToDatabase(userId: String, location: Location) {
        try {
            when (val result = insertLocationLog(
                userId = userId,
                latitude = location.latitude,
                longitude = location.longitude,
                accuracy = location.accuracy.toDouble()
            )) {
                is AppResult.Success -> {
                    Timber.d("Location saved: ${result.data.id} at ${result.data.timestamp}")
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

//class LocationTrackerService : Service() {
//
//    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
//    private val supabase: SupabaseClient by inject()
//    private val locationRepository: ILocationRepository by inject()
//
//    private val _locationFlow = MutableSharedFlow<Location>(replay = 1)
//    val locationFlow: SharedFlow<Location> = _locationFlow
//
//    // Initialize to current time to prevent immediate save on start
//    private var lastSavedTime = System.currentTimeMillis()
//    private var latestLocation: Location? = null
//
//    override fun onBind(intent: Intent?): IBinder {
//        return LocationBinder()
//    }
//
//    inner class LocationBinder : Binder() {
//        fun getService(): LocationTrackerService = this@LocationTrackerService
//    }
//
//    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        when (intent?.action) {
//            Action.START.name -> start()
//            Action.STOP.name -> stop()
//        }
//        return super.onStartCommand(intent, flags, startId)
//    }
//
//    private fun start() {
//        val locationManager =
//            LocationManager(applicationContext)
//        val notificationManager =
//            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//
//        val notification = NotificationCompat
//            .Builder(this, LOCATION_CHANNEL)
//            .setSmallIcon(R.drawable.ic_launcher_foreground)
//            .setContentTitle("Location Tracker")
//            .setStyle(NotificationCompat.BigTextStyle())
//
//        startForeground(1, notification.build())
//
//        // Start periodic database save
//        startPeriodicDatabaseSave()
//
//        scope.launch {
//            val userId = supabase.auth.currentUserOrNull()?.id
//
//            if (userId == null) {
//                Log.e("LocationService", "❌ User not authenticated")
//                stop()
//                return@launch
//            }
//
//            Log.d("LocationService", "✅ Location tracking started for user: $userId")
//
//            locationManager.trackLocation().collect { location ->
//                latestLocation = location
//                _locationFlow.emit(location)
//
//                val latitude = String.format("%.4f", location.latitude)
//                val longitude = String.format("%.4f", location.longitude)
//
//                notificationManager.notify(
//                    1,
//                    notification.setContentText(
//                        "Location: $latitude / $longitude"
//                    ).build()
//                )
//            }
//        }
//    }
//
//    private fun startPeriodicDatabaseSave() {
//        scope.launch {
//            val userId = supabase.auth.currentUserOrNull()?.id
//
//            if (userId == null) {
//                Log.e("LocationService", "❌ Cannot start periodic save: User not authenticated")
//                return@launch
//            }
//
//            Log.d("LocationService", "✅ Periodic database save started for user: $userId")
//
//            val saveInterval = 5 * 60 * 1000L // 1 hour
//
//            while (isActive) {
//                val currentTime = System.currentTimeMillis()
//                val timeSinceLastSave = currentTime - lastSavedTime
//
//                if (timeSinceLastSave >= saveInterval) {
//                    latestLocation?.let { location ->
//                        Log.d("LocationService", "⏰ Saving location to database (hourly trigger)")
//                        insertLocationToDatabase(userId, location)
//                        lastSavedTime = currentTime
//                    } ?: Log.w("LocationService", "⚠️ No location data available to save")
//                }
//
//                delay(60 * 1000L) // Check every minute
//            }
//        }
//    }
//
//    private suspend fun insertLocationToDatabase(
//        userId: String,
//        location: Location
//    ) {
//        Log.d(
//            "LocationService",
//            "📍 Attempting to save location: $userId, ${location.latitude}, ${location.longitude}"
//        )
//
//        locationRepository.insertLocationLog(
//            userId = userId,
//            latitude = location.latitude,
//            longitude = location.longitude,
//            accuracy = location.accuracy.toDouble()
//        ).onSuccess { savedLog ->
//            Log.d(
//                "LocationService",
//                "✅ Location saved successfully: ${savedLog.id} at ${savedLog.timestamp}"
//            )
//        }.onFailure { error ->
//            Log.e("LocationService", "❌ Failed to save location: ${error.message}", error)
//        }
//    }
//
//    private fun stop() {
//        Log.d("LocationService", "🛑 Stopping location tracking service")
//        stopForeground(STOP_FOREGROUND_REMOVE)
//        stopSelf()
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        scope.cancel()
//        Log.d("LocationService", "💀 Service destroyed")
//    }
//
//    enum class Action {
//        START, STOP
//    }
//
//    companion object {
//        const val LOCATION_CHANNEL = "location_channel"
//    }
//}

// domain/model/LocationTrackingSettings.kt
//package com.bluemix.clients_lead.domain.model
//
//enum class LoggingInterval(val milliseconds: Long, val displayName: String) {
//    EVERY_5_MINUTES(5 * 60 * 1000L, "Every 5 minutes"),
//    EVERY_15_MINUTES(15 * 60 * 1000L, "Every 15 minutes"),
//    EVERY_30_MINUTES(30 * 60 * 1000L, "Every 30 minutes"),
//    EVERY_HOUR(60 * 60 * 1000L, "Every hour"),
//    EVERY_2_HOURS(2 * 60 * 60 * 1000L, "Every 2 hours")
//}
