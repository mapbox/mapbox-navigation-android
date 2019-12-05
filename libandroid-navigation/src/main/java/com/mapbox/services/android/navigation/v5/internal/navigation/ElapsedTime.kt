package com.mapbox.services.android.navigation.v5.internal.navigation

import com.mapbox.navigation.utils.extensions.ifNonNull
import com.mapbox.services.android.navigation.v5.internal.exception.NavigationException
import com.mapbox.services.android.navigation.v5.internal.utils.Time
import kotlin.math.roundToLong

class ElapsedTime(
    private val timeProvider: Time = Time.SystemImpl
) {
    var start: Long? = null
        private set
    var end: Long? = null
        private set

    companion object {
        private const val ELAPSED_TIME_DENOMINATOR = 1e+9
        private const val PRECISION = 100.0
    }

    val elapsedTime: Double
        get() {
            return ifNonNull(start, end) { startTime, endTime ->
                val elapsedTimeInNanoseconds = endTime - startTime
                val elapsedTimeInSeconds = elapsedTimeInNanoseconds / ELAPSED_TIME_DENOMINATOR
                (elapsedTimeInSeconds * PRECISION).roundToLong() / PRECISION
            } ?: throw NavigationException("Must call start() and end() before calling getElapsedTime()")
        }

    fun start() {
        start = timeProvider.nanoTime()
    }

    fun end() {
        if (start == null) {
            throw NavigationException("Must call start() before calling end()")
        }
        end = timeProvider.nanoTime()
    }
}
