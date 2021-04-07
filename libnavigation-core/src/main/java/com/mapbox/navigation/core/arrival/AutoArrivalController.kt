package com.mapbox.navigation.core.arrival

import android.os.SystemClock
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import java.util.concurrent.TimeUnit

/**
 * The default controller for arrival. This will move onto the next leg automatically
 * if there is one.
 */
class AutoArrivalController : ArrivalController {

    private var routeLegCompletedTime: Long? = null
    private var currentRouteLeg: RouteLeg? = null

    /**
     * Default arrival options.
     */
    override fun arrivalOptions(): ArrivalOptions = ArrivalOptions.Builder().build()

    /**
     * By default this will move onto the next step after [ArrivalOptions.arrivalInSeconds]
     * seconds have passed.
     */
    override fun navigateNextRouteLeg(routeLegProgress: RouteLegProgress): Boolean {
        if (currentRouteLeg != routeLegProgress.routeLeg) {
            currentRouteLeg = routeLegProgress.routeLeg
            routeLegCompletedTime = SystemClock.elapsedRealtimeNanos()
        }

        val elapsedTimeNanos = SystemClock.elapsedRealtimeNanos() - (routeLegCompletedTime ?: 0L)
        val arrivalInSeconds = arrivalOptions().arrivalInSeconds?.toLong() ?: 0L
        val arrivalInNanos = TimeUnit.SECONDS.toNanos(arrivalInSeconds)
        val shouldNavigateNextRouteLeg = elapsedTimeNanos >= arrivalInNanos
        if (shouldNavigateNextRouteLeg) {
            currentRouteLeg = null
            routeLegCompletedTime = null
        }
        return shouldNavigateNextRouteLeg
    }
}
