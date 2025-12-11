package com.bluemix.clients_lead.core.common.extensions

import com.bluemix.clients_lead.core.common.utils.AppError
import com.bluemix.clients_lead.core.common.utils.AppResult

suspend fun <T> safeApiCall(apiCall: suspend () -> T): AppResult<T> {
    return try {
        AppResult.Success(apiCall())
    } catch (e: Exception) {
        AppResult.Error(AppError.Network(cause = e))  // Wrap in AppError
    }
}