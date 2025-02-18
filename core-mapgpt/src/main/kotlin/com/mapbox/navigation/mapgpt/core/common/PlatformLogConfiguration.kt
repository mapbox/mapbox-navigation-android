package com.mapbox.navigation.mapgpt.core.common

import com.mapbox.common.LogConfiguration
import com.mapbox.common.LogWriterBackend
import com.mapbox.common.LoggingLevel

object PlatformLogConfiguration {
    fun getLoggingLevel(): SharedLogLevel? {
        return LogConfiguration.getLoggingLevel()?.toSharedLevel()
    }

    fun getLoggingLevelForCategory(category: String): SharedLogLevel? {
        return LogConfiguration.getLoggingLevel(category)?.toSharedLevel()
    }

    fun registerLogObserver(logObserver: SharedLogObserver) {
        val androidLogWriterBackend = AndroidLogWriterBackend(logObserver)
        LogConfiguration.registerLogWriterBackend(androidLogWriterBackend)
    }

    fun resetLoggingLevelForCategory(category: String) {
        LogConfiguration.resetLoggingLevel(category)
    }

    fun setLoggingLevelForCategory(
        category: String,
        upTo: SharedLogLevel?,
    ) {
        LogConfiguration.setLoggingLevel(category, upTo?.toLoggingLevel())
    }

    fun setLoggingLevelForUpTo(upTo: SharedLogLevel?) {
        LogConfiguration.setLoggingLevel(upTo?.toLoggingLevel())
    }
}

private fun LoggingLevel.toSharedLevel(): SharedLogLevel {
    return when (this) {
        LoggingLevel.DEBUG -> SharedLogLevel.Debug
        LoggingLevel.INFO -> SharedLogLevel.Info
        LoggingLevel.WARNING -> SharedLogLevel.Warning
        LoggingLevel.ERROR -> SharedLogLevel.Error
        else -> throw IllegalArgumentException("Unknown MBXLoggingLevel: $this")
    }
}

private fun SharedLogLevel.toLoggingLevel(): LoggingLevel {
    return when (this) {
        SharedLogLevel.Debug -> LoggingLevel.DEBUG
        SharedLogLevel.Info -> LoggingLevel.INFO
        SharedLogLevel.Warning -> LoggingLevel.WARNING
        SharedLogLevel.Error -> LoggingLevel.ERROR
    }
}

private class AndroidLogWriterBackend(
    val logObserver: SharedLogObserver,
) : LogWriterBackend {
    override fun writeLog(level: LoggingLevel, message: String) {
        when (level) {
            LoggingLevel.DEBUG -> logObserver.onDebug(message)
            LoggingLevel.INFO -> logObserver.onInfo(message)
            LoggingLevel.WARNING -> logObserver.onWarning(message)
            LoggingLevel.ERROR -> logObserver.onError(message)
        }
    }
}
