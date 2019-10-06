package com.mapbox.services.android.navigation.v5.internal.utils

interface TimeProvider {
    fun nanoTime(): Long
    fun millis(): Long

    object SystemTime : TimeProvider {
        override fun nanoTime(): Long = System.nanoTime()

        override fun millis(): Long = System.currentTimeMillis()
    }
}
