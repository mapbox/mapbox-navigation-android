package com.mapbox.navigation.utils.internal

import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * Schedules a delay of [restartAfterMillis] milliseconds and then restarts.
 */
class MapboxTimer {
    private val jobControl = ThreadController.getMainScopeAndRootJob()
    /**
     * Time delay until the timer should restart.
     */
    var restartAfterMillis = TimeUnit.MINUTES.toMillis(1)

    /**
     * @param executeLambda lambda function that is to be executed after [restartAfterMillis] milliseconds.
     * @return [Job]
     */
    fun startTimer(executeLambda: () -> Unit): Job {
        return jobControl.scope.launch {
            while (isActive) {
                delay(restartAfterMillis)
                executeLambda()
            }
        }
    }

    fun stopJobs() {
        jobControl.job.cancelChildren()
    }
}
