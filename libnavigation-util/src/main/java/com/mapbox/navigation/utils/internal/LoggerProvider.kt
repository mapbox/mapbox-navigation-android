package com.mapbox.navigation.utils.internal

import androidx.annotation.VisibleForTesting
import com.mapbox.base.common.logger.Logger

/**
 * Singleton provider of [Logger].
 */
object LoggerProvider {
    @VisibleForTesting
    fun setLoggerFrontend(frontend: LoggerFrontend) {
        this.frontend = frontend
    }

    @VisibleForTesting
    fun getLoggerFrontend(): LoggerFrontend = frontend

    internal var frontend: LoggerFrontend = MapboxCommonLoggerFrontend()
        private set
}

/**
 * @param msg to log.
 * @param category optional string to identify the source or category of the log message.
 * Noting that the category is appended to the log message to give extra context along with the `[nav-sdk]` parent category.
 * As an example, this is how the logs would look like `D/Mapbox: [nav-sdk] [ConnectivityHandler] NetworkStatus=ReachableViaWiFi`.
 */
fun logV(msg: String, category: String? = null) {
    LoggerProvider.frontend.logD(msg, category)
}

/**
 * @param msg to log.
 * @param category optional string to identify the source or category of the log message.
 * Noting that the category is appended to the log message to give extra context along with the `[nav-sdk]` parent category.
 * As an example, this is how the logs would look like `D/Mapbox: [nav-sdk] [ConnectivityHandler] NetworkStatus=ReachableViaWiFi`.
 */
fun logD(msg: String, category: String? = null) {
    LoggerProvider.frontend.logD(msg, category)
}

/**
 * @param msg to log.
 * @param category optional string to identify the source or category of the log message.
 * Noting that the category is appended to the log message to give extra context along with the `[nav-sdk]` parent category.
 * As an example, this is how the logs would look like `I/Mapbox: [nav-sdk] [ConnectivityHandler] NetworkStatus=ReachableViaWiFi`.
 */
fun logI(msg: String, category: String? = null) {
    LoggerProvider.frontend.logI(msg, category)
}

/**
 * @param msg to log.
 * @param category optional string to identify the source or category of the log message.
 * Noting that the category is appended to the log message to give extra context along with the `[nav-sdk]` parent category.
 * As an example, this is how the logs would look like `W/Mapbox: [nav-sdk] [ConnectivityHandler] NetworkStatus=ReachableViaWiFi`.
 */
fun logW(msg: String, category: String? = null) {
    LoggerProvider.frontend.logW(msg, category)
}

/**
 * @param msg to log.
 * @param category optional string to identify the source or category of the log message.
 * Noting that the category is appended to the log message to give extra context along with the `[nav-sdk]` parent category.
 * As an example, this is how the logs would look like `E/Mapbox: [nav-sdk] [ConnectivityHandler] NetworkStatus=ReachableViaWiFi`.
 */
fun logE(msg: String, category: String? = null) {
    LoggerProvider.frontend.logE(msg, category)
}
