package com.mapbox.navigation.route.offboard.router

import com.mapbox.navigation.utils.NavigationException
import com.mapbox.navigation.utils.internal.Time

internal class ElapsedTime(
    private val timeProvider: Time = Time.SystemImpl
) {
    var start: Long? = null
        private set
    var end: Long? = null
        private set

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
