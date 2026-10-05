package com.mapbox.navigation.testing.utils

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class DelayedResponseModifier(
    private val delayMillis: Long,
    private val originalResponseModifier: ((String) -> String)? = null
) : (String) -> String {

    private var waitingTask: Job? = null

    override fun invoke(p1: String): String {
        runBlocking {
            waitingTask = launch { delay(delayMillis) }
            waitingTask!!.join()
        }
        waitingTask = null
        return originalResponseModifier?.invoke(p1) ?: p1
    }

    fun interruptDelay() {
        waitingTask?.cancel()
    }
}
