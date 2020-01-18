package com.mapbox.navigation.utils.timer

import com.mapbox.navigation.utils.thread.ThreadController
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Schedules a delay of [restartAfter] milliseconds and then restarts.
 *
 * @param restartAfter Time delay until the timer should restart.
 * @param delayLambda lambda function that is to be executed after [restartAfter] milliseconds.
 */
class MapboxTimer(private val restartAfter: Long, private val delayLambda: () -> Unit) {
    private val mainControllerJobScope = ThreadController.getMainScopeAndRootJob()

    fun start() {
        mainControllerJobScope.scope.launch {
            while (isActive) {
                delay(restartAfter)
                delayLambda()
            }
        }
    }

    fun stop() {
        mainControllerJobScope.job.children.forEach { job ->
            job.cancel()
        }
    }
}
