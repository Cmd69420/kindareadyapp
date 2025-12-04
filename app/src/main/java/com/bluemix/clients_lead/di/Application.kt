package com.bluemix.clients_lead.di

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.bluemix.clients_lead.features.location.LocationTrackerService
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level


class App : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@App)
            modules(appModule,authModule,clientModule,locationModule,profileModule)
        }

        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            // Location tracking channel
            val locationChannel = NotificationChannel(
                LocationTrackerService.LOCATION_CHANNEL,
                "Location Tracking",  // User-visible name
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows your current location while tracking is active"
                // Optional: Disable sound for this channel
                setSound(null, null)
            }

            // Get notification manager
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Create the channel
            notificationManager.createNotificationChannel(locationChannel)

            // Add more channels here if needed in the future
            // notificationManager.createNotificationChannel(otherChannel)
        }
    }
}
