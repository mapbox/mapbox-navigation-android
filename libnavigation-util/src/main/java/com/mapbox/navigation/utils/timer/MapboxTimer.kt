package com.mapbox.navigation.utils.timer

import com.mapbox.navigation.utils.thread.ThreadController
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Schedules a delay of [restartAfter] milliseconds and then restarts.
 *
 * @param restartAfter Time delay until the timer should restart.
 * @param executeLambda lambda function that is to be executed after [restartAfter] milliseconds.
 */
class MapboxTimer(private val restartAfter: Long, private val executeLambda: () -> Unit) {
    private val mainControllerJobScope = ThreadController.getMainScopeAndRootJob()
    private var timerJob: Job = Job()
    init {
        timerJob.cancel()
    }

    fun start() {
        if (timerJob.isActive) {
            return
        }
        timerJob = mainControllerJobScope.scope.launch {
            while (isActive) {
                delay(restartAfter)
                executeLambda()
            }
        }
    }

    fun stop() {
        mainControllerJobScope.job.cancelChildren()
    }
}
