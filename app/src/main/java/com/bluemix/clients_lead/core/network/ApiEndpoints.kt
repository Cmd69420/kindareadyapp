package com.bluemix.clients_lead.core.network

/**
 * Central API endpoint definitions
 * All backend routes are defined here for easy maintenance
 */
object ApiEndpoints {

    // Base URL - Change this based on your environment
    // For Android Emulator: "http://10.0.2.2:5000"
    // For Physical Device: "http://YOUR_LOCAL_IP:5000" (e.g., "http://192.168.1.5:5000")
    // For Production: "https://api.yourdomain.com"
    const val BASE_URL = "https://geo-track-1.onrender.com"



    /**
     * Authentication endpoints
     */
    object Auth {
        const val SIGNUP = "/auth/signup"
        const val LOGIN = "/auth/login"
        const val PROFILE = "/auth/profile"
        //todo below
        //const val FORGOT_PASSWORD = "/auth/forgot-password"
        //const val RESET_PASSWORD = "/auth/reset-password"
    }

    /**
     * Clients endpoints
     */
    object Clients {
        const val BASE = "/clients"
        const val UPLOAD_EXCEL = "/clients/upload-excel"

        // Dynamic route for single client
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

        // Get all expenses for current user (with optional filters)
        // Query params: ?startDate=...&endDate=...&transportMode=...&clientId=...
        const val MY_EXPENSES = "$BASE/my-expenses"

        // Get total amount spent by current user
        const val MY_TOTAL = "/expenses/my-total"

        // Dynamic route for single expense
        fun byId(expenseId: String) = "$BASE/$expenseId"
    }

    /**
     * Utility endpoints
     */
    object Utility {
        const val ROOT = "/"
        const val DB_TEST = "/dbtest"
    }
}