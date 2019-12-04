package com.mapbox.navigation.utils.time

interface Time {
    fun nanoTime(): Long
    fun millis(): Long

    object SystemImpl : Time {
        override fun nanoTime(): Long = System.nanoTime()

        override fun millis(): Long = System.currentTimeMillis()
    }
}
