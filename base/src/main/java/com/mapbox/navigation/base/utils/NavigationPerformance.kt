package com.mapbox.navigation.base.utils

import androidx.annotation.MainThread
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.internal.performance.LogcatPerformanceLogging
import com.mapbox.navigation.base.internal.performance.PerformanceTracker

/**
 * API to control performance related information Navigation SDK provides.
 */
object NavigationPerformance {

    private var loggingEnabled = false
    private val logcatLoggingObserver = LogcatPerformanceLogging()

    /***
     * Controls if Nav SDK logs performance related information to logcat on with info level.
     * @param isEnabled defines if logging is enabled
     */
    @ExperimentalPreviewMapboxNavigationAPI
    @MainThread
    fun performanceInfoLoggingEnabled(isEnabled: Boolean) {
        when {
            isEnabled && !loggingEnabled -> {
                PerformanceTracker.addObserver(logcatLoggingObserver)
            }
            !isEnabled && loggingEnabled -> {
                PerformanceTracker.removeObserver(logcatLoggingObserver)
            }
        }
        loggingEnabled = isEnabled
    }
}
