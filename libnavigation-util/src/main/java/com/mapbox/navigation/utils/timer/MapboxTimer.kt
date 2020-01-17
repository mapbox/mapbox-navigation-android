package com.mapbox.navigation.utils.timer

import com.mapbox.navigation.utils.thread.ThreadController
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Schedules a delay of [restartAfter] seconds and then restarts.
 *
 * @param restartAfter Time delay until the timer should restart
 * @param listener Hook to receive the events from [MapboxTimerListener]
 */
class MapboxTimer(private val restartAfter: Long, private val listener: MapboxTimerListener) {
    private val mainControllerJobScope = ThreadController.getMainScopeAndRootJob()

    fun start() {
        mainControllerJobScope.scope.launch {
            while (isActive) {
                delay(restartAfter)
                listener.onTimerExpired()
            }
        }
    }

    fun stop() {
        mainControllerJobScope.job.children.forEach { job ->
            job.cancel()
        }
    }
}
