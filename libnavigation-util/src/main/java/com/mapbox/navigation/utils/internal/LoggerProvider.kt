package com.mapbox.navigation.utils.internal

import com.mapbox.annotation.module.MapboxModuleType
import com.mapbox.base.common.logger.Logger
import com.mapbox.base.common.logger.model.Message
import com.mapbox.base.common.logger.model.Tag
import com.mapbox.common.LogConfiguration
import com.mapbox.common.LogWriterBackend
import com.mapbox.common.LoggingLevel
import com.mapbox.common.module.provider.MapboxModuleProvider

/**
 * Singleton provider of [Logger].
 */
internal object LoggerProvider {

    init {
        LogConfiguration.getInstance().registerLogWriterBackend(NavigationLogBackend())
    }

    internal val logger = MapboxModuleProvider.createModule<Logger>(
        MapboxModuleType.CommonLogger
    ) {
        arrayOf()
    }
}

/**
 * Alias of [LoggerProvider.logger]#v
 */
fun logV(tag: String? = null, msg: String) {
    // There's no com.mapbox.common.Logger.v available - using Logger.d instead
    com.mapbox.common.Logger.d(tag, msg)
}

/**
 * Alias of [LoggerProvider.logger]#d
 */
fun logD(tag: String? = null, msg: String) {
    com.mapbox.common.Logger.d(tag, msg)
}

/**
 * Alias of [LoggerProvider.logger]#i
 */
fun logI(tag: String? = null, msg: String) {
    com.mapbox.common.Logger.i(tag, msg)
}

/**
 * Alias of [LoggerProvider.logger]#w
 */
fun logW(tag: String? = null, msg: String) {
    com.mapbox.common.Logger.w(tag, msg)
}

/**
 * Alias of [LoggerProvider.logger]#e
 */
fun logE(tag: String? = null, msg: String) {
    com.mapbox.common.Logger.e(tag, msg)
}

private class NavigationLogBackend : LogWriterBackend {
    override fun writeLog(level: LoggingLevel, message: String, category: String?) {
        when (level) {
            LoggingLevel.DEBUG -> {
                LoggerProvider.logger.d(tag = category?.let { Tag(it) }, msg = Message(message))
            }
            LoggingLevel.INFO -> {
                LoggerProvider.logger.i(tag = category?.let { Tag(it) }, msg = Message(message))
            }
            LoggingLevel.WARNING -> {
                LoggerProvider.logger.w(tag = category?.let { Tag(it) }, msg = Message(message))
            }
            LoggingLevel.ERROR -> {
                LoggerProvider.logger.e(tag = category?.let { Tag(it) }, msg = Message(message))
            }
            else -> {
                LoggerProvider.logger.v(tag = Tag("unspecified"), msg = Message(message))
            }
        }
    }
}
