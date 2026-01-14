package com.bluemix.clients_lead.core.common.utils

import android.content.Context
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Manages trial period restrictions for users with generic emails (gmail, yahoo, etc.)
 *
 * IMPORTANT: Trial restrictions ONLY apply to users with generic email domains.
 * Company email users (@companydomain.com) bypass all trial logic automatically.
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
     * Check if trial is valid for the CURRENT logged-in user.
     * Returns TRUE if:
     * 1. User is NOT a trial user (company email) - bypass all checks
     * 2. User IS a trial user AND trial period is still valid
     *
     * @param isTrialUser - from backend indicating if user has generic email
     */
    fun isTrialValid(isTrialUser: Boolean): Boolean {
        // ✅ Company email users always have valid "trial" (no restrictions)
        if (!isTrialUser) {
            Timber.d("🏢 Company user - bypassing trial checks")
            return true
        }

        // Trial user - check actual trial status
        val trialStart = getTrialStartTime()

        if (trialStart == 0L) {
            // First time - start trial
            startTrial()
            return true
        }

        val now = System.currentTimeMillis()
        val daysPassed = TimeUnit.MILLISECONDS.toDays(now - trialStart)

        val isValid = daysPassed < TRIAL_DAYS && !isTrialExpired()

        Timber.d("🕒 Trial User Check: Days passed = $daysPassed, Valid = $isValid")

        return isValid
    }

    /**
     * Get remaining trial days.
     * Only relevant for trial users.
     *
     * @param isTrialUser - whether current user is trial user
     */
    fun getRemainingDays(isTrialUser: Boolean): Long {
        // Company users have "unlimited" days
        if (!isTrialUser) {
            return Long.MAX_VALUE
        }

        val trialStart = getTrialStartTime()
        if (trialStart == 0L) return TRIAL_DAYS

        val now = System.currentTimeMillis()
        val daysPassed = TimeUnit.MILLISECONDS.toDays(now - trialStart)

        return (TRIAL_DAYS - daysPassed).coerceAtLeast(0)
    }

    /**
     * Get trial expiry timestamp.
     * Only relevant for trial users.
     */
    fun getTrialExpiryTime(): Long {
        val trialStart = getTrialStartTime()
        if (trialStart == 0L) return 0L

        return trialStart + TimeUnit.DAYS.toMillis(TRIAL_DAYS)
    }

    /**
     * Check if user can create a new account on this device.
     * This applies to ALL users to prevent device abuse.
     */
    fun canCreateAccount(): Boolean {
        val accountsCreated = prefs.getInt(KEY_ACCOUNTS_CREATED, 0)
        val maxAccounts = 3 // Allow max 3 account creations per device

        Timber.d("📱 Accounts created on this device: $accountsCreated / $maxAccounts")

        return accountsCreated < maxAccounts
    }

    /**
     * Record that user created a new account.
     * Called after successful signup.
     */
    fun recordAccountCreation() {
        val count = prefs.getInt(KEY_ACCOUNTS_CREATED, 0)
        prefs.edit().putInt(KEY_ACCOUNTS_CREATED, count + 1).apply()

        Timber.d("📝 Recorded account creation. Total: ${count + 1}")
    }

    /**
     * Mark trial as expired (called when backend returns 401 with TRIAL_EXPIRED).
     * Only affects trial users.
     */
    fun markTrialExpired() {
        prefs.edit().putBoolean(KEY_TRIAL_EXPIRED, true).apply()
        Timber.w("⏰ Trial marked as EXPIRED")
    }

    /**
     * Get device fingerprint for backend verification.
     */
    fun getDeviceFingerprint(): String {
        return DeviceIdentifier.getDeviceId(context)
    }

    /**
     * Clear trial data (for testing only - remove in production).
     */
    fun clearTrialData() {
        prefs.edit().clear().apply()
        Timber.d("🗑️ Trial data cleared")
    }

    /**
     * Check if user should see upgrade prompt.
     * Only trial users see the upgrade banner.
     *
     * @param isTrialUser - from backend
     */
    fun shouldShowUpgradeBanner(isTrialUser: Boolean): Boolean {
        if (!isTrialUser) return false // Company users don't see banner

        val daysRemaining = getRemainingDays(isTrialUser)
        return daysRemaining <= 3 // Show banner in last 3 days
    }

    fun clearTrialIfCompanyUser(isTrialUser: Boolean) {
        if (!isTrialUser) {
            prefs.edit()
                .remove(KEY_TRIAL_START)
                .remove(KEY_TRIAL_EXPIRED)
                .apply()

            Timber.d("🧹 Trial state cleared for company user")
        }
    }



    // ============================================
    // Private Helper Methods
    // ============================================

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