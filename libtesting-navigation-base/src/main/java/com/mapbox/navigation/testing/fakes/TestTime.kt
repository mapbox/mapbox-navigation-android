package com.mapbox.navigation.testing.fakes

import com.mapbox.navigation.utils.internal.Time

class TestTime : Time {

    private var nanoTime = 0L

    override fun nanoTime(): Long {
        return nanoTime
    }

    override fun millis(): Long {
        return nanoTime / 1_000_000
    }

    override fun seconds(): Long {
        return nanoTime / 1_000_000_000
    }

    fun setSeconds(seconds: Long) {
        nanoTime = seconds * 1_000_000_000
    }
}
