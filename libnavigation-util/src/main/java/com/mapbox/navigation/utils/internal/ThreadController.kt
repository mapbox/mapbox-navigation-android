package com.mapbox.navigation.utils.internal

import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.android.asCoroutineDispatcher
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

class ThreadController constructor(
    private val sdkLooper: Looper = Looper.myLooper()
        ?: error(
            "You can't create SDK from a thread without looper. " +
                "Make sure you create the Navigation SDK from the main thread " +
                "or called Looper.prepare on current thread."
        ),
) {

    private val sdkDispatcher: MainCoroutineDispatcher = if (sdkLooper == Looper.getMainLooper()) {
        Dispatchers.Main
    } else {
        Handler(sdkLooper).asCoroutineDispatcher("mapbox navigation SDK dispatcher")
    }

    companion object {
        val IODispatcher: CoroutineDispatcher = Dispatchers.IO
        val DefaultDispatcher: CoroutineDispatcher = Dispatchers.Default
    }

    internal var ioRootJob = SupervisorJob()
    internal var mainRootJob = SupervisorJob()

    fun assertSDKThread() {
        require(Looper.myLooper() == sdkLooper) {
            "Current lopper doesn't match the same the SDK were created from. " +
                "You should call the SDK's methods from the thread were the SDK was created."
        }
    }

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
     * Same as [getIOScopeAndRootJob], but using the SDK thread dispatcher.
     */
    fun getSDKScopeAndRootJob(immediate: Boolean = false): JobControl {
        val parentJob = SupervisorJob(mainRootJob)
        val dispatcher = if (immediate) sdkDispatcher.immediate else sdkDispatcher
        return JobControl(parentJob, CoroutineScope(parentJob + dispatcher))
    }
}
