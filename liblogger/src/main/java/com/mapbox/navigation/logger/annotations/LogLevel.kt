package com.mapbox.navigation.logger.annotations

import androidx.annotation.IntDef
import com.mapbox.navigation.logger.DEBUG
import com.mapbox.navigation.logger.ERROR
import com.mapbox.navigation.logger.INFO
import com.mapbox.navigation.logger.NONE
import com.mapbox.navigation.logger.VERBOSE
import com.mapbox.navigation.logger.WARN

/**
 * Log level indicates which logs are allowed to be emitted by the Mapbox Maps SDK for Android.
 */
@IntDef(VERBOSE, DEBUG, INFO, WARN, ERROR, NONE)
@Retention(AnnotationRetention.SOURCE)
internal annotation class LogLevel
