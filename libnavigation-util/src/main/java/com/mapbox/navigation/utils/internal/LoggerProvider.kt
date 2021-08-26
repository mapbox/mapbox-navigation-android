package com.mapbox.navigation.utils.internal

import com.mapbox.annotation.module.MapboxModuleType
import com.mapbox.base.common.logger.Logger
import com.mapbox.base.common.logger.model.Message
import com.mapbox.base.common.logger.model.Tag
import com.mapbox.common.module.provider.MapboxModuleProvider

/**
 * Singleton provider of [Logger].
 */
object LoggerProvider {

    val logger = MapboxModuleProvider.createModule<Logger>(
        MapboxModuleType.CommonLogger
    ) {
        arrayOf()
    }
}

/**
 * Alias of [LoggerProvider.logger]#v
 */
fun logV(tag: Tag? = null, msg: Message, tr: Throwable? = null) {
    LoggerProvider.logger.v(tag, msg, tr)
}

/**
 * Alias of [LoggerProvider.logger]#d
 */
fun logD(tag: Tag? = null, msg: Message, tr: Throwable? = null) {
    LoggerProvider.logger.d(tag, msg, tr)
}

/**
 * Alias of [LoggerProvider.logger]#i
 */
fun logI(tag: Tag? = null, msg: Message, tr: Throwable? = null) {
    LoggerProvider.logger.i(tag, msg, tr)
}

/**
 * Alias of [LoggerProvider.logger]#w
 */
fun logW(tag: Tag? = null, msg: Message, tr: Throwable? = null) {
    LoggerProvider.logger.w(tag, msg, tr)
}

/**
 * Alias of [LoggerProvider.logger]#e
 */
fun logE(tag: Tag? = null, msg: Message, tr: Throwable? = null) {
    LoggerProvider.logger.e(tag, msg, tr)
}
