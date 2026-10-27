package com.mapbox.navigation.testing.ui.utils

/**
 * [System.nanoTime] readings taken right before and right after a block of code.
 * [System.nanoTime] is monotonic and excludes time the device spends suspended.
 */
class TimeWindow(val beforeNanos: Long, val afterNanos: Long)

/**
 * Runs [block] and returns the [TimeWindow] it ran in. Whatever [block] triggers happens
 * somewhere between [TimeWindow.beforeNanos] and [TimeWindow.afterNanos].
 */
suspend fun measureTimeWindow(block: suspend () -> Unit): TimeWindow {
    val before = System.nanoTime()
    block()
    return TimeWindow(before, System.nanoTime())
}
