package com.mapbox.navigation.core.routerefresh

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class CancellableHandler(
    private val scope: CoroutineScope
) {

    private val jobs = linkedSetOf<Job>()

    fun postDelayed(timeout: Long, block: suspend () -> Unit, cancellationCallback: () -> Unit) {
        val job = scope.launch {
            try {
                delay(timeout)
                block()
            } catch (ex: CancellationException) {
                cancellationCallback()
                throw ex
            }
        }
        jobs.add(job)
        job.invokeOnCompletion { jobs.remove(job) }
    }

    fun cancelAll() {
        HashSet(jobs).forEach {
            it.cancel()
        }
    }
}
