package com.mapbox.navigation.core.routerefresh

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class CancellableHandler(
    private val scope: CoroutineScope
) {

    private val jobs = linkedMapOf<Job, () -> Unit>()

    fun postDelayed(timeout: Long, block: Runnable, cancellationCallback: () -> Unit) {
        val job = scope.launch {
            delay(timeout)
            block.run()
        }
        jobs[job] = cancellationCallback
        job.invokeOnCompletion { jobs.remove(job) }
    }

    fun cancelAll() {
        HashMap(jobs).forEach {
            it.value.invoke()
            it.key.cancel()
        }
    }
}
