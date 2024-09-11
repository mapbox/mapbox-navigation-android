package com.mapbox.navigation.testing

import com.mapbox.navigation.utils.internal.Time
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.rules.ExternalResource
import kotlin.time.Duration

class TestSystemClock(
    private val initialTime: Duration = Duration.ZERO,
) : ExternalResource() {

    private val systemClock = Time.SystemClockImpl
    private var time = Duration.ZERO

    val elapsedMillis: Long
        get() = time.inWholeMilliseconds

    override fun before() {
        time = initialTime
        mockkObject(systemClock)
        every { systemClock.millis() } answers { time.inWholeMilliseconds }
        every { systemClock.seconds() } answers { time.inWholeSeconds }
        every { systemClock.nanoTime() } answers { time.inWholeNanoseconds }
    }

    override fun after() {
        unmockkObject(systemClock)
    }

    fun advanceTimeBy(duration: Duration) {
        time += duration
    }
}
