package com.bluemix.clients_lead.core.common.utils

import android.util.Log

enum class LogLevel { VERBOSE, DEBUG, INFO, WARN, ERROR, NONE }

interface Logger {
    var minLevel: LogLevel
    fun v(tag: String, msg: String)
    fun d(tag: String, msg: String)
    fun i(tag: String, msg: String)
    fun w(tag: String, msg: String, tr: Throwable? = null)
    fun e(tag: String, msg: String, tr: Throwable? = null)
}

class PrintLogger(
    override var minLevel: LogLevel = LogLevel.DEBUG,
    private val sink: (String) -> Unit = ::println
) : Logger {
    private fun ok(level: LogLevel) = level.ordinal >= minLevel.ordinal
    override fun v(tag: String, msg: String) {
        if (ok(LogLevel.VERBOSE)) sink("V/$tag: $msg")
    }

    override fun d(tag: String, msg: String) {
        if (ok(LogLevel.DEBUG)) sink("D/$tag: $msg")
    }

    override fun i(tag: String, msg: String) {
        if (ok(LogLevel.INFO)) sink("I/$tag: $msg")
    }

    override fun w(tag: String, msg: String, tr: Throwable?) {
        if (ok(LogLevel.WARN)) sink("W/$tag: $msg${tr?.let { " : ${it.stackTraceToString()}" } ?: ""}")
    }

    override fun e(tag: String, msg: String, tr: Throwable?) {
        if (ok(LogLevel.ERROR)) sink("E/$tag: $msg${tr?.let { " : ${it.stackTraceToString()}" } ?: ""}")
    }
}

class AndroidLogger(
    override var minLevel: LogLevel = LogLevel.DEBUG
) : Logger {
    private fun ok(level: LogLevel) = level.ordinal >= minLevel.ordinal

    override fun v(tag: String, msg: String) {
        if (ok(LogLevel.VERBOSE)) Log.v(tag, msg)
    }

    override fun d(tag: String, msg: String) {
        if (ok(LogLevel.DEBUG)) Log.d(tag, msg)
    }

    override fun i(tag: String, msg: String) {
        if (ok(LogLevel.INFO)) Log.i(tag, msg)
    }

    override fun w(tag: String, msg: String, tr: Throwable?) {
        if (ok(LogLevel.WARN)) {
            if (tr != null) Log.w(tag, msg, tr)
            else Log.w(tag, msg)
        }
    }

    override fun e(tag: String, msg: String, tr: Throwable?) {
        if (ok(LogLevel.ERROR)) {
            if (tr != null) Log.e(tag, msg, tr)
            else Log.e(tag, msg)
        }
    }
}