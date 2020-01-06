package com.mapbox.navigation.utils

import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher

data class JobControl(val job: Job, val scope: CoroutineScope)

const val MAX_THREAD_COUNT = 2

object ThreadController {
    private val maxCoresUsed = Runtime.getRuntime().availableProcessors().coerceAtMost(
        MAX_THREAD_COUNT
    )
    @UseExperimental(ObsoleteCoroutinesApi::class)
    private val IODispatchContext =
        Executors.newFixedThreadPool(maxCoresUsed).asCoroutineDispatcher()

    private val rootJob = SupervisorJob()
    private val scope = CoroutineScope(rootJob + IODispatchContext)

    /**
     * This method cancels all coroutines that are children of this job. The call affects
     * all coroutines that where started via ThreadController.scope.launch(). It is basically
     * a kill switch for all non-UI scoped coroutines.
     */
    fun cancelAllNonUICoroutines() {
        rootJob.cancel()
    }

    /**
     * This method creates a [Job] object that is a child of the [rootJob]. Using
     * this job a [CoroutineScope] is created. The return object is the [JobControl] data class. This
     * data class contains both the new [Job] object and the [CoroutineScope] that uses the [Job] object.
     * This construct allows the caller to cancel all coroutines created from the returned [CoroutineScope].
     * Example:
     * val jobController:JobController = ThreadController.getScopeAndRootJob()
     * val job_1 = jobController.scope.launch{ doSomethingUsefull_1()}
     * val job_2 = jobController.scope.launch{ doSomethingUsefull_2()}
     * val job_3 = jobController.scope.launch{ doSomethingUsefull_3()}
     * val job_4 = jobController.scope.launch{ doSomethingUsefull_4()}
     *
     * The code launches four coroutines. Each one becomes a parent of ThreadController.job
     * To cancel all coroutines: jobController.job.cancel()
     * To cancel a specific coroutine: job_1.cancel(), etc.
     */
    fun getScopeAndRootJob(): JobControl {
        val parentJob = SupervisorJob(rootJob)
        return JobControl(parentJob, scope)
    }

    /**
     * Same as [cancelAllNonUICoroutines], but using the MainThread dispatcher.
     */
    fun getMainScopeAndRootJob(): JobControl {
        val parentJob = SupervisorJob(rootJob)
        return JobControl(parentJob, CoroutineScope(parentJob + Dispatchers.Main))
    }
}
