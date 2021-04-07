package com.mapbox.navigation.core.arrival

import android.os.SystemClock
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class AutoArrivalControllerTest {

    private val arrivalController = AutoArrivalController()

    @Before
    fun setup() {
        mockkStatic(SystemClock::class)
    }

    @After
    fun teardown() {
        unmockkObject(SystemClock.elapsedRealtimeNanos())
    }

    @Test
    fun `should navigate next at predicted arrival`() {
        val arrivalInSeconds = arrivalController.arrivalOptions().arrivalInSeconds?.toInt() ?: 0
        val routeLeg = mockk<RouteLeg>()

        mockSecond(100)
        assertFalse(arrivalController.navigateNextRouteLeg(mockProgress(routeLeg)))
        mockSecond(100 + arrivalInSeconds - 1)
        assertFalse(arrivalController.navigateNextRouteLeg(mockProgress(routeLeg)))
        mockSecond(100 + arrivalInSeconds)
        assertTrue(arrivalController.navigateNextRouteLeg(mockProgress(routeLeg)))
    }

    @Test
    fun `should restart timer if rerouted`() {
        val arrivalInSeconds = arrivalController.arrivalOptions().arrivalInSeconds?.toInt() ?: 0
        val routeLeg = mockk<RouteLeg>()

        mockSecond(100)
        assertFalse(arrivalController.navigateNextRouteLeg(mockProgress(routeLeg)))
        mockSecond(100 + arrivalInSeconds)
        val reroutedRouteLeg = mockk<RouteLeg>()
        assertFalse(arrivalController.navigateNextRouteLeg(mockProgress(reroutedRouteLeg)))
        mockSecond(100 + arrivalInSeconds + arrivalInSeconds - 1)
        assertFalse(arrivalController.navigateNextRouteLeg(mockProgress(reroutedRouteLeg)))

        mockSecond(100 + arrivalInSeconds + arrivalInSeconds)
        assertTrue(arrivalController.navigateNextRouteLeg(mockProgress(reroutedRouteLeg)))
    }

    private fun mockSecond(second: Int) = every {
        SystemClock.elapsedRealtimeNanos()
    } returns TimeUnit.SECONDS.toNanos(second.toLong())

    private fun mockProgress(mockedRouteLeg: RouteLeg) = mockk<RouteLegProgress> {
        every { routeLeg } returns mockedRouteLeg
    }
}
