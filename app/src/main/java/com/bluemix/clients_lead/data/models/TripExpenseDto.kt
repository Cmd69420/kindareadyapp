package com.bluemix.clients_lead.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data Transfer Object for Trip Expenses
 * Maps to backend API response/request structure
 */
@Serializable
data class TripExpenseDto(
    @SerialName("id")
    val id: String,

    @SerialName("user_id")
    val userId: String,

    @SerialName("start_location")
    val startLocation: String,

    @SerialName("end_location")
    val endLocation: String? = null,

    @SerialName("travel_date")
    val travelDate: Long,

    @SerialName("distance_km")
    val distanceKm: Double,

    @SerialName("transport_mode")
    val transportMode: String,

    @SerialName("amount_spent")
    val amountSpent: Double,

    @SerialName("currency")
    val currency: String = "₹",

    @SerialName("notes")
    val notes: String? = null,

    @SerialName("receipt_urls")
    val receiptUrls: List<String> = emptyList(),

    @SerialName("client_id")
    val clientId: String? = null,

    @SerialName("client_name")
    val clientName: String? = null,

    @SerialName("created_at")
    val createdAt: String,

    @SerialName("updated_at")
    val updatedAt: String? = null
)

/**
 * Request body for creating/updating expense
 */
@Serializable
data class TripExpenseCreateDto(
    @SerialName("start_location")
    val startLocation: String,

    @SerialName("end_location")
    val endLocation: String? = null,

    @SerialName("travel_date")
    val travelDate: Long,

    @SerialName("distance_km")
    val distanceKm: Double,

    @SerialName("transport_mode")
    val transportMode: String,

    @SerialName("amount_spent")
    val amountSpent: Double,

    @SerialName("currency")
    val currency: String = "₹",

    @SerialName("notes")
    val notes: String? = null,

    @SerialName("receipt_urls")
    val receiptUrls: List<String> = emptyList(),

    @SerialName("client_id")
    val clientId: String? = null
)

/**
 * Response wrapper for expense operations
 */
@Serializable
data class ExpenseResponse(
    @SerialName("message")
    val message: String? = null,

    @SerialName("expense")
    val expense: TripExpenseDto
)

/**
 * Response for list of expenses
 */
@Serializable
data class ExpensesListResponse(
    @SerialName("expenses")
    val expenses: List<TripExpenseDto>,

    @SerialName("total")
    val total: Int,

    @SerialName("totalAmount")
    val totalAmount: Double? = null
)

/**
 * Receipt upload response
 */
@Serializable
data class ReceiptUploadResponse(
    @SerialName("url")
    val url: String,

    @SerialName("fileName")
    val fileName: String
)