package com.bluemix.clients_lead.core.common.utils

import android.content.Context
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Manages 7-day trial period with device-based restrictions
 */
class TrialManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("trial_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val TRIAL_DAYS = 7L
        private const val KEY_TRIAL_START = "trial_start_time"
        private const val KEY_TRIAL_EXPIRED = "trial_expired"
        private const val KEY_ACCOUNTS_CREATED = "accounts_created_count"
    }

    /**
     * Check if trial is active and valid
     */
    fun isTrialValid(): Boolean {
        val trialStart = getTrialStartTime()

        if (trialStart == 0L) {
            // First time - start trial
            startTrial()
            return true
        }

        val now = System.currentTimeMillis()
        val daysPassed = TimeUnit.MILLISECONDS.toDays(now - trialStart)

        val isValid = daysPassed < TRIAL_DAYS && !isTrialExpired()

        Timber.d("🕐 Trial Check: Days passed = $daysPassed, Valid = $isValid")

        return isValid
    }

    /**
     * Get remaining trial days
     */
    fun getRemainingDays(): Long {
        val trialStart = getTrialStartTime()
        if (trialStart == 0L) return TRIAL_DAYS

        val now = System.currentTimeMillis()
        val daysPassed = TimeUnit.MILLISECONDS.toDays(now - trialStart)

        return (TRIAL_DAYS - daysPassed).coerceAtLeast(0)
    }

    /**
     * Get trial expiry timestamp
     */
    fun getTrialExpiryTime(): Long {
        val trialStart = getTrialStartTime()
        if (trialStart == 0L) return 0L

        return trialStart + TimeUnit.DAYS.toMillis(TRIAL_DAYS)
    }

    /**
     * Check if user has exceeded account creation limit
     * Prevents abuse by creating multiple accounts
     */
    fun canCreateAccount(): Boolean {
        val accountsCreated = prefs.getInt(KEY_ACCOUNTS_CREATED, 0)
        val maxAccounts = 3 // Allow max 3 account creations per device

        Timber.d("📱 Accounts created on this device: $accountsCreated / $maxAccounts")

        return accountsCreated < maxAccounts
    }

    /**
     * Record that user created a new account
     */
    fun recordAccountCreation() {
        val count = prefs.getInt(KEY_ACCOUNTS_CREATED, 0)
        prefs.edit().putInt(KEY_ACCOUNTS_CREATED, count + 1).apply()

        Timber.d("📝 Recorded account creation. Total: ${count + 1}")
    }

    /**
     * Mark trial as expired (called when backend returns 401)
     */
    fun markTrialExpired() {
        prefs.edit().putBoolean(KEY_TRIAL_EXPIRED, true).apply()
        Timber.w("⏰ Trial marked as EXPIRED")
    }

    /**
     * Get device info for backend verification
     */
    fun getDeviceFingerprint(): String {
        return DeviceIdentifier.getDeviceId(context)
    }

    /**
     * Clear trial data (for testing only - remove in production)
     */
    fun clearTrialData() {
        prefs.edit().clear().apply()
        Timber.d("🗑️ Trial data cleared")
    }

    private fun startTrial() {
        val now = System.currentTimeMillis()
        prefs.edit().putLong(KEY_TRIAL_START, now).apply()

        Timber.d("🎬 Trial STARTED at $now")
    }

    private fun getTrialStartTime(): Long {
        return prefs.getLong(KEY_TRIAL_START, 0L)
    }

    private fun isTrialExpired(): Boolean {
        return prefs.getBoolean(KEY_TRIAL_EXPIRED, false)
    }
}