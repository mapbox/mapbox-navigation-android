package com.mapbox.navigation.core.arrival

import android.os.SystemClock
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import java.util.concurrent.TimeUnit

/**
 * The default controller for arrival. This will move onto the next leg automatically
 * if there is one.
 */
open class AutoArrivalController : ArrivalController {

    private var routeLegCompletedTime: Long? = null
    private var currentRouteLeg: RouteLeg? = null

    /**
     * Moves onto the next leg after 5 seconds have passed.
     */
    override fun navigateNextRouteLeg(routeLegProgress: RouteLegProgress): Boolean {
        if (currentRouteLeg != routeLegProgress.routeLeg) {
            currentRouteLeg = routeLegProgress.routeLeg
            routeLegCompletedTime = SystemClock.elapsedRealtimeNanos()
        }

        val elapsedTimeNanos = SystemClock.elapsedRealtimeNanos() - (routeLegCompletedTime ?: 0L)
        val shouldNavigateNextRouteLeg = elapsedTimeNanos >= AUTO_ARRIVAL_NANOS
        if (shouldNavigateNextRouteLeg) {
            currentRouteLeg = null
            routeLegCompletedTime = null
        }
        return shouldNavigateNextRouteLeg
    }

    internal companion object {
        val AUTO_ARRIVAL_NANOS = TimeUnit.SECONDS.toNanos(5L)
    }
}
