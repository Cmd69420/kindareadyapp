package com.bluemix.clients_lead.core.network

/**
 * Central API endpoint definitions
 * All backend routes are defined here for easy maintenance
 */
object ApiEndpoints {

    const val BASE_URL = "https://geo-track-1.onrender.com"

    /**
     * Authentication endpoints
     */
    object Auth {
        const val SIGNUP = "/auth/signup"
        const val LOGIN = "/auth/login"
        const val PROFILE = "/auth/profile"
    }

    /**
     * Clients endpoints
     */
    object Clients {
        const val BASE = "/clients"
        const val UPLOAD_EXCEL = "/clients/upload-excel"

        fun byId(clientId: String) = "$BASE/$clientId"
    }

    object User {
        const val CLEAR_PINCODE = "/auth/clear-pincode"
    }

    /**
     * Location logs endpoints
     */
    object Location {
        const val LOGS = "/location-logs"
    }

    /**
     * Trip Expenses endpoints
     */
    object Expenses {
        const val BASE = "/expenses"
        const val UPLOAD_RECEIPT = "/expenses/receipts"
        const val MY_EXPENSES = "$BASE/my-expenses"
        const val MY_TOTAL = "/expenses/my-total"

        fun byId(expenseId: String) = "$BASE/$expenseId"
    }

    /**
     * Meeting endpoints
     */
    object Meetings {
        const val BASE = "/meetings"

        // Get active meeting for a specific client
        fun activeMeeting(clientId: String) = "$BASE/active/$clientId"

        // Get all meetings for a user
        fun userMeetings(userId: String) = "$BASE/user/$userId"

        // Get all meetings for a client
        fun clientMeetings(clientId: String) = "$BASE/client/$clientId"

        // Dynamic route for single meeting
        fun byId(meetingId: String) = "$BASE/$meetingId"

        // Upload attachment to a meeting
        fun attachments(meetingId: String) = "$BASE/$meetingId/attachments"
    }

    /**
     * Utility endpoints
     */
    object Utility {
        const val ROOT = "/"
        const val DB_TEST = "/dbtest"
    }
}