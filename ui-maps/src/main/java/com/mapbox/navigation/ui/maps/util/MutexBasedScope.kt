package com.mapbox.navigation.ui.maps.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Launches every task under mutex lock.
 * Useful in case you want to guarantee the order of invocations with suspend functions.
 */
internal class MutexBasedScope(
    private val originalScope: CoroutineScope,
) {

    private val mutex = Mutex()

    fun launchWithMutex(block: suspend () -> Unit): Job {
        return originalScope.launch {
            mutex.withLock {
                block()
            }
        }
    }

    fun cancelChildren() {
        originalScope.coroutineContext.cancelChildren()
    }
}
