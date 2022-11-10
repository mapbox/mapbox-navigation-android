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

interface ThreadController {

    companion object {
        /***
         * Static duplicate of [ThreadController.getIODispatcher] for cases when it's
         * not convenient to access an instance of [ThreadController]
         */
        val IODispatcher: CoroutineDispatcher = Dispatchers.IO

        /***
         * Static duplicate of [ThreadController.getIODispatcher] for cases when it's
         * not convenient to access an instance of [ThreadController]
         */
        val DefaultDispatcher: CoroutineDispatcher = Dispatchers.Default
    }

    /***
     * Throws an exception if the SDK shouldn't be used from current thread.
     */
    fun assertSDKThread()

    /***
     * Deprecated version of [createChildSDKScope]
     */
    @Deprecated(
        replaceWith = ReplaceWith("createChildSDKScope()"),
        message = "JobControl doesn't make much sense as coroutine context already keeps link to its job."
    )
    fun getSDKScopeAndRootJob(immediate: Boolean = false): JobControl

    /***
     * Creates a new scope, which is child of the root scope [ThreadController] keeps internally.
     * Cancellations or failure of the child scopes created with [createChildSDKScope] doesn't affect other child scopes.
     * @param immediate specifies if child scope will use immediate dispatcher. It's similar to the difference between [Dispatchers.Main] and [MainCoroutineDispatcher.immediate].
     * @see cancelSDKScope
     */
    fun createChildSDKScope(immediate: Boolean = false): CoroutineScope

    /***
     * Cancels all scopes which were created using [createChildSDKScope] by the same [ThreadController] instance.
     * @see createChildSDKScope
     */
    fun cancelSDKScope()

    /***
     * @return dispatcher that should be used for IO related operations: read from disk, waiting for network, etc.
     */
    fun getIODispatcher(): CoroutineDispatcher

    /***
     * @return dispatcher that should be used for CPU intensive operations: json serialization, geometry decoding, etc.
     */
    fun getComputationDispatcher(): CoroutineDispatcher
}

/***
 * Implementation of [ThreadController] for Android.
 * It expects that the SDK creates new instance of [AndroidThreadController] on during the SDK creation
 * and remembers the thread from which the SDK was created. Let's refer this thread as the SDK thread.
 * Scopes created by [createChildSDKScope] will schedule work to the SDK thread.
 * [assertSDKThread] verifies that current thread is SDK thread.
 */
class AndroidThreadController constructor(
    private val sdkLooper: Looper = Looper.myLooper()
        ?: error(
            "You can't create the SDK from a thread which doesn't have prepared looper. " +
                "Make sure you created the Navigation SDK from the main thread " +
                "or you setup looper by calling Looper.prepare on a worker thread."
        ),
): ThreadController {

    private val sdkDispatcher: MainCoroutineDispatcher = if (sdkLooper == Looper.getMainLooper()) {
        Dispatchers.Main
    } else {
        Handler(sdkLooper).asCoroutineDispatcher("mapbox navigation SDK dispatcher")
    }

    internal var ioRootJob = SupervisorJob()
    internal var mainRootJob = SupervisorJob()

    override fun assertSDKThread() {
        require(Looper.myLooper() == sdkLooper) {
            "Current lopper doesn't match the same the SDK were created from. " +
                "You should call the SDK's methods from the thread were the SDK was created."
        }
    }

    override fun cancelSDKScope() {
        mainRootJob.cancelChildren()
    }

    override fun getIODispatcher(): CoroutineDispatcher {
        return ThreadController.IODispatcher
    }

    override fun getComputationDispatcher(): CoroutineDispatcher {
        return ThreadController.DefaultDispatcher
    }

    override fun getSDKScopeAndRootJob(immediate: Boolean): JobControl {
        val parentJob = SupervisorJob(mainRootJob)
        return JobControl(parentJob, CoroutineScope(parentJob + getDispatcher(immediate)))
    }

    override fun createChildSDKScope(immediate: Boolean): CoroutineScope {
        val parentJob = SupervisorJob(mainRootJob)
        return CoroutineScope(parentJob + getDispatcher(immediate))
    }

    private fun getDispatcher(immediate: Boolean) =
        if (immediate) sdkDispatcher.immediate else sdkDispatcher
}
