package com.mapbox.navigation.base.internal.performance

import androidx.annotation.RestrictTo
import com.mapbox.navigation.utils.internal.logI
import kotlin.time.Duration

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
private const val TAG = "PERFORMANCE"

internal class LogcatPerformanceLogging : PerformanceObserver {
    override fun sectionStarted(name: String, id: Int) {
        logI(TAG) { "$name($id) section started" }
    }

    override fun sectionCompleted(name: String, id: Int, duration: Duration?) {
        logI(TAG) { "$name($id) section completed in $duration" }
    }
}
