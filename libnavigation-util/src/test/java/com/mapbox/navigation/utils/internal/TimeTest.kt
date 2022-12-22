package com.mapbox.navigation.utils.internal

import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class TimeTest {

    @Test
    fun seconds() {
        val tolerance = 1
        val diff = abs(System.currentTimeMillis() / 1000 - Time.SystemImpl.seconds())
        assertTrue(diff < tolerance)
    }

    @Test
    fun millis() {
        val tolerance = 100
        val diff = abs(System.currentTimeMillis() - Time.SystemImpl.millis())
        assertTrue(diff < tolerance)
    }

    @Test
    fun nanoTime() {
        val tolerance = 100000000
        val diff = abs(System.nanoTime() - Time.SystemImpl.nanoTime())
        assertTrue(diff < tolerance)
    }
}
