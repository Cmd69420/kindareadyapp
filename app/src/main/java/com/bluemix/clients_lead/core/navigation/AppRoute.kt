package com.bluemix.clients_lead.core.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes using Kotlin Serialization.
 * Requires: implementation(libs.plugins.kotlin.serialization)
 */
@Serializable
sealed class Route {
    @Serializable
    data object Gate : Route()

    @Serializable
    data object Auth : Route()

    @Serializable
    data object Map : Route()

    @Serializable
    data object Clients : Route()

    @Serializable
    data object Activity : Route()

    @Serializable
    data object Profile : Route()

    @Serializable
    data class ClientDetail(val clientId: String) : Route()

    // NEW SCREEN FOR EXPENSE FORM
    @Serializable
    data object ExpenseForm : Route()

    @Serializable
    data object MultiLegExpenseForm : Route()


    @Serializable
    data object CreateClient : Route()
}
