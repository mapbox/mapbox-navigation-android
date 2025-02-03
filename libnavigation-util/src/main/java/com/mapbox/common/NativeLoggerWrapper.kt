package com.mapbox.common

/**
 * Wrapper on top of native logging functions that pipes information to Common SDK but also allows for unit testing the native functions.
 * It has to live in a `com.mapbox.common` package to be able to access the package-private `com.mapbox.common.Log` class.
 */
internal object NativeLoggerWrapper {
    fun debug(message: String, category: String?) {
        Log.debug(message, category)
    }

    fun info(message: String, category: String?) {
        Log.info(message, category)
    }

    fun warning(message: String, category: String?) {
        Log.warning(message, category)
    }

    fun error(message: String, category: String?) {
        Log.error(message, category)
    }

    fun getLogLevel(category: String) = LogConfiguration.getLoggingLevel(category)
}
