package com.mapbox.navigation.utils.timer

import com.mapbox.navigation.utils.thread.ThreadController
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Schedules a delay of [restartAfterMillis] milliseconds and then restarts.
 *
 * @param restartAfterMillis Time delay until the timer should restart.
 * @param executeLambda lambda function that is to be executed after [restartAfterMillis] milliseconds.
 */
class MapboxTimer {
    private val jobControl = ThreadController.getMainScopeAndRootJob()

    var restartAfterMillis = TimeUnit.MINUTES.toMillis(1)

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
