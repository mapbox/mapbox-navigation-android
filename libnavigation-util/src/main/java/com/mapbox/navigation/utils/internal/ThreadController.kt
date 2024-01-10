package com.mapbox.navigation.utils.internal

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

fun <T> CoroutineScope.monitorChannelWithException(
    channel: ReceiveChannel<T>,
    predicate: suspend (T) -> Unit,
    onCancellation: (() -> Unit) = {}
): Job {
    var isChannelValid = true
    return launch {
        while (isActive && isChannelValid) {
            try {
                predicate(channel.receive())
            } catch (e: Exception) {
                e.ifChannelException {
                    isChannelValid = false
                    onCancellation()
                }
            }
        }
    }
}

fun Exception.ifChannelException(action: () -> Unit) {
    when (this) {
        is CancellationException,
        is ClosedSendChannelException,
        is ClosedReceiveChannelException -> action()
        else -> throw this
    }
}

data class JobControl(val job: Job, val scope: CoroutineScope)

class ThreadController {

    companion object {
        val IODispatcher: CoroutineDispatcher = Dispatchers.IO
        val DefaultDispatcher: CoroutineDispatcher = Dispatchers.Default
    }

    internal var ioRootJob = SupervisorJob()
    internal var mainRootJob = SupervisorJob()

    /**
     * This method cancels all coroutines that are children of io and navigator jobs.
     * The call affects all coroutines that where started via ThreadController.ioScope.launch() and
     * ThreadController.navigatorScope.launch().
     * It is basically a kill switch for all non-UI scoped coroutines.
     */
    fun cancelAllNonUICoroutines() {
        ioRootJob.cancelChildren()
    }

    /**
     * This method cancels all coroutines that are children of this job. The call affects
     * all coroutines that where started via ThreadController.mainScope.launch(). It is basically
     * a kill switch for all UI scoped coroutines.
     */
    fun cancelAllUICoroutines() {
        mainRootJob.cancelChildren()
    }

    /**
     * This method creates a [Job] object that is a child of the [ioRootJob]. Using
     * this job a [CoroutineScope] is created. The return object is the [JobControl] data class. This
     * data class contains both the new [Job] object and the [CoroutineScope] that uses the [Job] object.
     * This construct allows the caller to cancel all coroutines created from the returned [CoroutineScope].
     * Example:
     * val jobController:JobController = ThreadController.getIOScopeAndRootJob()
     * val job_1 = jobController.ioScope.launch{ doSomethingUsefull_1()}
     * val job_2 = jobController.ioScope.launch{ doSomethingUsefull_2()}
     * val job_3 = jobController.ioScope.launch{ doSomethingUsefull_3()}
     * val job_4 = jobController.ioScope.launch{ doSomethingUsefull_4()}
     *
     * The code launches four coroutines. Each one becomes a parent of ThreadController.job
     * To cancel all coroutines: jobController.job.cancel()
     * To cancel a specific coroutine: job_1.cancel(), etc.
     */
    fun getIOScopeAndRootJob(): JobControl {
        val parentJob = SupervisorJob(ioRootJob)
        return JobControl(parentJob, CoroutineScope(parentJob + IODispatcher))
    }

    /**
     * Same as [getIOScopeAndRootJob], but using the MainThread dispatcher.
     */
    fun getMainScopeAndRootJob(): JobControl {
        val parentJob = SupervisorJob(mainRootJob)
        return JobControl(parentJob, CoroutineScope(parentJob + Dispatchers.Main))
    }

    fun destroy() {
        val reason = CancellationException("thread controller is destroyed")
        mainRootJob.cancel(reason)
        ioRootJob.cancel(reason)
    }
}
