package com.mapbox.navigation.base.internal.performance

import androidx.annotation.RestrictTo
import com.mapbox.navigation.utils.internal.logI
import kotlin.time.Duration

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
private const val TAG = "PERFORMANCE"

internal class LogcatPerformanceLogging : PerformanceObserver {
    override fun syncSectionStarted(name: String) {
        logI(TAG) { "$name section started" }
    }

    override fun syncSectionCompleted(name: String, duration: Duration?) {
        logI(TAG) { "$name section completed in $duration" }
    }
}
