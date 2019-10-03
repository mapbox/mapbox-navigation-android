package com.mapbox.services.android.navigation.v5.internal.navigation

import java.util.Timer
import java.util.TimerTask

internal class BatteryChargeReporter(
    private val timer: Timer,
    private val task: TimerTask
) {

    companion object {
        private const val NO_DELAY = 0L
    }

    fun scheduleAt(periodInMilliseconds: Long) {
        timer.scheduleAtFixedRate(task,
            NO_DELAY, periodInMilliseconds)
    }

    fun stop() {
        timer.cancel()
    }
}
