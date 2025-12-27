package com.bluemix.clients_lead.core.common.utils

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings
import java.security.MessageDigest
import java.util.UUID

/**
 * Creates a unique device identifier that persists across app reinstalls
 * but is unique per device. Combines multiple device attributes.
 */
object DeviceIdentifier {

    /**
     * Get a unique, stable device ID
     * This combines Android ID with device hardware info
     */
    @SuppressLint("HardwareIds")
    fun getDeviceId(context: Context): String {
        val prefs = context.getSharedPreferences("device_prefs", Context.MODE_PRIVATE)

        // Check if we already have a stored device ID
        val existingId = prefs.getString("device_id", null)
        if (existingId != null) {
            return existingId
        }

        // Generate new device ID combining multiple factors
        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "unknown"

        val deviceInfo = buildString {
            append(androidId)
            append(Build.BOARD)
            append(Build.BRAND)
            append(Build.DEVICE)
            append(Build.MANUFACTURER)
            append(Build.MODEL)
            append(Build.PRODUCT)
        }

        // Hash the combined string to create a unique ID
        val deviceId = hashString(deviceInfo)

        // Store it for future use
        prefs.edit().putString("device_id", deviceId).apply()

        return deviceId
    }

    /**
     * Get installation ID (unique per app install)
     * This changes if user uninstalls and reinstalls
     */
    fun getInstallationId(context: Context): String {
        val prefs = context.getSharedPreferences("device_prefs", Context.MODE_PRIVATE)

        var installId = prefs.getString("installation_id", null)
        if (installId == null) {
            installId = UUID.randomUUID().toString()
            prefs.edit().putString("installation_id", installId).apply()
        }

        return installId
    }

    /**
     * Check if this is the first time app is launched on this device
     */
    fun isFirstLaunch(context: Context): Boolean {
        val prefs = context.getSharedPreferences("device_prefs", Context.MODE_PRIVATE)
        val isFirst = !prefs.getBoolean("has_launched", false)

        if (isFirst) {
            // Record first launch timestamp
            prefs.edit()
                .putBoolean("has_launched", true)
                .putLong("first_launch_time", System.currentTimeMillis())
                .apply()
        }

        return isFirst
    }

    /**
     * Get the timestamp of first launch
     */
    fun getFirstLaunchTime(context: Context): Long {
        val prefs = context.getSharedPreferences("device_prefs", Context.MODE_PRIVATE)
        return prefs.getLong("first_launch_time", System.currentTimeMillis())
    }

    private fun hashString(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}