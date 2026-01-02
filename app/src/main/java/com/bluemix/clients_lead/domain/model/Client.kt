package com.bluemix.clients_lead.domain.model

import kotlin.math.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

data class Client(
    val id: String,
    val name: String,
    val phone: String?,
    val email: String?,
    val address: String?,
    val latitude: Double?,
    val longitude: Double?,
    val pincode: String?,
    val hasLocation: Boolean,
    val status: String, // active, inactive, completed
    val notes: String?,
    val createdBy: String,
    val createdAt: String,
    val updatedAt: String,
    val lastVisitDate: String? = null,  // ISO 8601 format
    val lastVisitNotes: String? = null
) {
    /**
     * Calculate distance from this client to given coordinates (in kilometers)
     * Uses Haversine formula for accurate distance calculation
     */
    fun distanceFrom(fromLat: Double, fromLng: Double): Double? {
        if (latitude == null || longitude == null) return null

        val earthRadiusKm = 6371.0

        val dLat = Math.toRadians(latitude - fromLat)
        val dLng = Math.toRadians(longitude - fromLng)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(fromLat)) * cos(Math.toRadians(latitude)) *
                sin(dLng / 2) * sin(dLng / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadiusKm * c
    }

    /**
     * Format distance for display
     */
    fun formatDistance(fromLat: Double, fromLng: Double): String? {
        val distance = distanceFrom(fromLat, fromLng) ?: return null

        return when {
            distance < 1.0 -> "${(distance * 1000).toInt()}m away"
            distance < 10.0 -> String.format("%.1f km away", distance)
            else -> "${distance.toInt()} km away"
        }
    }

    /**
     * Get formatted last visit time
     * Returns "2 days ago", "3 weeks ago", etc.
     */
    fun getFormattedLastVisit(): String? {
        if (lastVisitDate == null) return null

        return try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            format.timeZone = TimeZone.getTimeZone("UTC")
            val visitDate = format.parse(lastVisitDate) ?: return null

            val now = Date()
            val diffMillis = now.time - visitDate.time

            when {
                diffMillis < 0 -> "Just now"
                diffMillis < TimeUnit.HOURS.toMillis(1) -> {
                    val mins = TimeUnit.MILLISECONDS.toMinutes(diffMillis)
                    if (mins < 1) "Just now" else "$mins ${if (mins == 1L) "minute" else "minutes"} ago"
                }
                diffMillis < TimeUnit.DAYS.toMillis(1) -> {
                    val hours = TimeUnit.MILLISECONDS.toHours(diffMillis)
                    "$hours ${if (hours == 1L) "hour" else "hours"} ago"
                }
                diffMillis < TimeUnit.DAYS.toMillis(7) -> {
                    val days = TimeUnit.MILLISECONDS.toDays(diffMillis)
                    "$days ${if (days == 1L) "day" else "days"} ago"
                }
                diffMillis < TimeUnit.DAYS.toMillis(30) -> {
                    val weeks = TimeUnit.MILLISECONDS.toDays(diffMillis) / 7
                    "$weeks ${if (weeks == 1L) "week" else "weeks"} ago"
                }
                diffMillis < TimeUnit.DAYS.toMillis(365) -> {
                    val months = TimeUnit.MILLISECONDS.toDays(diffMillis) / 30
                    "$months ${if (months == 1L) "month" else "months"} ago"
                }
                else -> {
                    val years = TimeUnit.MILLISECONDS.toDays(diffMillis) / 365
                    "$years ${if (years == 1L) "year" else "years"} ago"
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Check if last visit was recent (within 7 days)
     */
    fun hasRecentVisit(): Boolean {
        if (lastVisitDate == null) return false

        return try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            format.timeZone = TimeZone.getTimeZone("UTC")
            val visitDate = format.parse(lastVisitDate) ?: return false

            val now = Date()
            val diffMillis = now.time - visitDate.time
            diffMillis < TimeUnit.DAYS.toMillis(7)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get visit status color indicator
     */
    fun getVisitStatusColor(): VisitStatus {
        if (lastVisitDate == null) return VisitStatus.NEVER_VISITED

        return try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            format.timeZone = TimeZone.getTimeZone("UTC")
            val visitDate = format.parse(lastVisitDate) ?: return VisitStatus.NEVER_VISITED

            val now = Date()
            val diffMillis = now.time - visitDate.time
            val daysSince = TimeUnit.MILLISECONDS.toDays(diffMillis)

            when {
                daysSince < 7 -> VisitStatus.RECENT
                daysSince < 30 -> VisitStatus.MODERATE
                else -> VisitStatus.OVERDUE
            }
        } catch (e: Exception) {
            VisitStatus.NEVER_VISITED
        }
    }
}

enum class VisitStatus {
    NEVER_VISITED,  // No last visit
    RECENT,         // Within 7 days
    MODERATE,       // 7-30 days ago
    OVERDUE         // 30+ days ago
}