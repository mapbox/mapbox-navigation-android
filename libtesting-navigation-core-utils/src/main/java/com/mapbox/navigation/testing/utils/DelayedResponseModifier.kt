package com.mapbox.navigation.testing.utils

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class DelayedResponseModifier(
    private val delayMillis: Long,
    private val originalResponseModifier: ((String) -> String)? = null
) : (String) -> String {

    private val lock = Any()
    private var waitingTask: Job? = null
    private var interruptRequested = false

    override fun invoke(p1: String): String {
        runBlocking {
            val task = synchronized(lock) {
                if (interruptRequested) {
                    interruptRequested = false
                    null
                } else {
                    launch { delay(delayMillis) }.also { waitingTask = it }
                }
            }
            task?.join()
        }
        synchronized(lock) { waitingTask = null }
        return originalResponseModifier?.invoke(p1) ?: p1
    }

    /**
     * Ends the delay of the request being handled. If no request is being handled yet
     * (the server may not have picked it up), the next request is not delayed.
     */
    fun interruptDelay() {
        synchronized(lock) {
            val task = waitingTask
            if (task != null) task.cancel() else interruptRequested = true
        }
    }
}
