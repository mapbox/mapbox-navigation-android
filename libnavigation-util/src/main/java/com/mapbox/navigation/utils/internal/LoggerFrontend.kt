package com.mapbox.navigation.utils.internal

import com.mapbox.common.LoggingLevel
import com.mapbox.common.NativeLoggerWrapper

private const val NAV_SDK_CATEGORY = "nav-sdk"

interface LoggerFrontend {
    fun getLogLevel(): LoggingLevel?
    fun logV(msg: String, category: String? = null)
    fun logD(msg: String, category: String? = null)
    fun logI(msg: String, category: String? = null)
    fun logE(msg: String, category: String? = null)
    fun logW(msg: String, category: String? = null)
}

internal class MapboxCommonLoggerFrontend : LoggerFrontend {

    override fun getLogLevel() = NativeLoggerWrapper.getLogLevel(NAV_SDK_CATEGORY)

    override fun logV(msg: String, category: String?) {
        val message = createMessage(msg, category)
        // There's no com.mapbox.common.Log.verbose available - using Log.debug instead
        NativeLoggerWrapper.debug(message, NAV_SDK_CATEGORY)
    }

    override fun logD(msg: String, category: String?) {
        val message = createMessage(msg, category)
        NativeLoggerWrapper.debug(message, NAV_SDK_CATEGORY)
    }

    override fun logI(msg: String, category: String?) {
        val message = createMessage(msg, category)
        NativeLoggerWrapper.info(message, NAV_SDK_CATEGORY)
    }

    override fun logE(msg: String, category: String?) {
        val message = createMessage(msg, category)
        NativeLoggerWrapper.error(message, NAV_SDK_CATEGORY)
    }

    override fun logW(msg: String, category: String?) {
        val message = createMessage(msg, category)
        NativeLoggerWrapper.warning(message, NAV_SDK_CATEGORY)
    }
}

private fun createMessage(message: String, category: String?): String =
    "${if (category != null) "[".plus(category).plus("] ") else ""}$message"
