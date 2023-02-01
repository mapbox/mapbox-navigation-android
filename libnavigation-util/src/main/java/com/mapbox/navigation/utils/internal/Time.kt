package com.mapbox.navigation.utils.internal

import android.os.SystemClock
import java.util.concurrent.TimeUnit

interface Time {

    fun nanoTime(): Long

    fun millis(): Long

    fun seconds(): Long
    object SystemImpl : Time {
        override fun nanoTime(): Long = System.nanoTime()

        override fun millis(): Long = System.currentTimeMillis()

        override fun seconds(): Long = TimeUnit.MILLISECONDS.toSeconds(millis())
    }

    object SystemClockImpl : Time {
        override fun nanoTime(): Long = SystemClock.elapsedRealtimeNanos()

        override fun millis(): Long = SystemClock.elapsedRealtime()

        override fun seconds(): Long = TimeUnit.MILLISECONDS.toSeconds(millis())
    }
}
