package com.mapbox.navigation.utils.internal

import com.mapbox.annotation.module.MapboxModuleType
import com.mapbox.base.common.logger.Logger
import com.mapbox.base.common.logger.model.Message
import com.mapbox.base.common.logger.model.Tag
import com.mapbox.common.LogConfiguration
import com.mapbox.common.LogWriterBackend
import com.mapbox.common.LoggingLevel
import com.mapbox.common.module.provider.MapboxModuleProvider

private const val TAG = "Mapbox"
private const val CATEGORY = "nav-sdk"

/**
 * Singleton provider of [Logger].
 */
object LoggerProvider {

    fun initialize() {
        LogConfiguration.getInstance().registerLogWriterBackend(NavigationLogBackend())
    }

    internal val logger = MapboxModuleProvider.createModule<Logger>(
        MapboxModuleType.CommonLogger
    ) {
        arrayOf()
    }
}

/**
 * Alias of [com.mapbox.common.Logger.d].
 *
 * @param msg to log.
 * @param category optional string to identify the source or category of the log message.
 * Noting that the category is appended to the log message to give extra context along with the `[nav-sdk]` parent category.
 * As an example, this is how the logs would look like `D/Mapbox: [nav-sdk] [ConnectivityHandler] NetworkStatus=ReachableViaWiFi`.
 */
fun logV(msg: String, category: String? = null) {
    val message = createMessage(msg, category)
    // There's no com.mapbox.common.Logger.v available - using Logger.d instead
    com.mapbox.common.Logger.d(CATEGORY, message)
}

/**
 * Alias of [com.mapbox.common.Logger.d].
 *
 * @param msg to log.
 * @param category optional string to identify the source or category of the log message.
 * Noting that the category is appended to the log message to give extra context along with the `[nav-sdk]` parent category.
 * As an example, this is how the logs would look like `D/Mapbox: [nav-sdk] [ConnectivityHandler] NetworkStatus=ReachableViaWiFi`.
 */
fun logD(msg: String, category: String? = null) {
    val message = createMessage(msg, category)
    com.mapbox.common.Logger.d(CATEGORY, message)
}

/**
 * Alias of [com.mapbox.common.Logger.i].
 *
 * @param msg to log.
 * @param category optional string to identify the source or category of the log message.
 * Noting that the category is appended to the log message to give extra context along with the `[nav-sdk]` parent category.
 * As an example, this is how the logs would look like `I/Mapbox: [nav-sdk] [ConnectivityHandler] NetworkStatus=ReachableViaWiFi`.
 */
fun logI(msg: String, category: String? = null) {
    val message = createMessage(msg, category)
    com.mapbox.common.Logger.i(CATEGORY, message)
}

/**
 * Alias of [com.mapbox.common.Logger.w].
 *
 * @param msg to log.
 * @param category optional string to identify the source or category of the log message.
 * Noting that the category is appended to the log message to give extra context along with the `[nav-sdk]` parent category.
 * As an example, this is how the logs would look like `W/Mapbox: [nav-sdk] [ConnectivityHandler] NetworkStatus=ReachableViaWiFi`.
 */
fun logW(msg: String, category: String? = null) {
    val message = createMessage(msg, category)
    com.mapbox.common.Logger.w(CATEGORY, message)
}

/**
 * Alias of [com.mapbox.common.Logger.e].
 *
 * @param msg to log.
 * @param category optional string to identify the source or category of the log message.
 * Noting that the category is appended to the log message to give extra context along with the `[nav-sdk]` parent category.
 * As an example, this is how the logs would look like `E/Mapbox: [nav-sdk] [ConnectivityHandler] NetworkStatus=ReachableViaWiFi`.
 */
fun logE(msg: String, category: String? = null) {
    val message = createMessage(msg, category)
    com.mapbox.common.Logger.e(CATEGORY, message)
}

private fun createMessage(message: String, category: String?): String =
    "${if (category != null) "[".plus(category).plus("] ") else ""}$message"

private class NavigationLogBackend : LogWriterBackend {

    private val tag = Tag(TAG)

    override fun writeLog(level: LoggingLevel, message: String, category: String?) {
        val msg = Message(createMessage(message, category))
        when (level) {
            LoggingLevel.DEBUG -> {
                LoggerProvider.logger.d(tag = tag, msg = msg)
            }
            LoggingLevel.INFO -> {
                LoggerProvider.logger.i(tag = tag, msg = msg)
            }
            LoggingLevel.WARNING -> {
                LoggerProvider.logger.w(tag = tag, msg = msg)
            }
            LoggingLevel.ERROR -> {
                LoggerProvider.logger.e(tag = tag, msg = msg)
            }
            else -> {
                LoggerProvider.logger.v(tag = tag, msg = msg)
            }
        }
    }
}
