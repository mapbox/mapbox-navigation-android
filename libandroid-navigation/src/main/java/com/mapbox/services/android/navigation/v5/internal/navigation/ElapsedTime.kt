package com.mapbox.services.android.navigation.v5.internal.navigation

import com.mapbox.services.android.navigation.v5.internal.exception.NavigationException
import kotlin.math.roundToLong

internal class ElapsedTime {
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
            if (start == null || end == null) {
                throw NavigationException("Must call start() and end() before calling getElapsedTime()")
            }
            val elapsedTimeInNanoseconds = end!! - start!!
            val elapsedTimeInSeconds = elapsedTimeInNanoseconds / ELAPSED_TIME_DENOMINATOR
            return (elapsedTimeInSeconds * PRECISION).roundToLong() / PRECISION
        }

    fun start() {
        start = System.nanoTime()
    }

    fun end() {
        if (start == null) {
            throw NavigationException("Must call start() before calling end()")
        }
        end = System.nanoTime()
    }
}
