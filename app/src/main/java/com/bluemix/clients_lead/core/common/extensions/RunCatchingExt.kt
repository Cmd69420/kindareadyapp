package com.bluemix.clients_lead.core.common.extensions


import com.bluemix.clients_lead.core.common.utils.AppError
import com.bluemix.clients_lead.core.common.utils.AppResult
import kotlinx.coroutines.CancellationException
import java.io.IOException

suspend inline fun <T> runAppCatching(
    mapper: (Throwable) -> AppError = ::defaultErrorMapper,
    block: suspend () -> T
): AppResult<T> = try {
    AppResult.Success(block())
} catch (t: Throwable) {
    if (t is CancellationException) throw t
    AppResult.Error(mapper(t))
}

fun defaultErrorMapper(t: Throwable): AppError = when (t) {
    is IOException -> AppError.Network(cause = t)
    else -> AppError.Unknown(cause = t)
}
