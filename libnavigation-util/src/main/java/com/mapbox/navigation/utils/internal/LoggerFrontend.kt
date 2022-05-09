package com.mapbox.navigation.utils.internal

interface LoggerFrontend {
    fun logV(msg: String, category: String? = null)
    fun logD(msg: String, category: String? = null)
    fun logI(msg: String, category: String? = null)
    fun logE(msg: String, category: String? = null)
    fun logW(msg: String, category: String? = null)
}

internal class NoLoggingFrontend : LoggerFrontend {
    override fun logV(msg: String, category: String?) {
    }

    override fun logD(msg: String, category: String?) {
    }

    override fun logI(msg: String, category: String?) {
    }

    override fun logE(msg: String, category: String?) {
    }

    override fun logW(msg: String, category: String?) {
    }
}

internal class MapboxCommonLoggerFrontend : LoggerFrontend {
    override fun logV(msg: String, category: String?) {
        val message = createMessage(msg, category)
        // There's no com.mapbox.common.Logger.v available - using Logger.d instead
        com.mapbox.common.Logger.d(NAV_SDK_CATEGORY, message)
    }

    override fun logD(msg: String, category: String?) {
        val message = createMessage(msg, category)
        com.mapbox.common.Logger.d(NAV_SDK_CATEGORY, message)
    }

    override fun logI(msg: String, category: String?) {
        val message = createMessage(msg, category)
        com.mapbox.common.Logger.i(NAV_SDK_CATEGORY, message)
    }

    override fun logE(msg: String, category: String?) {
        val message = createMessage(msg, category)
        com.mapbox.common.Logger.e(NAV_SDK_CATEGORY, message)
    }

    override fun logW(msg: String, category: String?) {
        val message = createMessage(msg, category)
        com.mapbox.common.Logger.w(NAV_SDK_CATEGORY, message)
    }
}
