package com.mapbox.navigation.core.utils

import com.mapbox.navigation.utils.internal.Time

/**
 * Executes delays and provides a possibility to "resume" last delay in case it was cancelled.
 * Non thread safe.
 */
internal class Delayer(private val interval: Long, private val timeProvider: Time) {

    private var delayRemaining: Long = interval

    suspend fun delay() {
        delayRemaining = interval
        delayInternal(interval)
    }

    suspend fun resumeDelay() {
        val toDelay = delayRemaining
        delayInternal(toDelay)
    }

    private suspend fun delayInternal(millis: Long) {
        val startMillis = timeProvider.millis()
        try {
            kotlinx.coroutines.delay(millis)
        } finally {
            val endMillis = timeProvider.millis()
            delayRemaining = millis - (endMillis - startMillis)
        }
    }
}
