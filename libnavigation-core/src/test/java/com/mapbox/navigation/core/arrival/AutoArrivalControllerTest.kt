package com.mapbox.navigation.core.arrival

import android.os.SystemClock
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.core.arrival.AutoArrivalController.Companion.AUTO_ARRIVAL_NANOS
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

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
        val routeLeg = mockk<RouteLeg>()

        mockNanos(100)
        assertFalse(arrivalController.navigateNextRouteLeg(mockProgress(routeLeg)))
        mockNanos(100 + AUTO_ARRIVAL_NANOS - 1)
        assertFalse(arrivalController.navigateNextRouteLeg(mockProgress(routeLeg)))
        mockNanos(100 + AUTO_ARRIVAL_NANOS)
        assertTrue(arrivalController.navigateNextRouteLeg(mockProgress(routeLeg)))
    }

    @Test
    fun `should restart timer if rerouted`() {
        val routeLeg = mockk<RouteLeg>()

        mockNanos(100)
        assertFalse(arrivalController.navigateNextRouteLeg(mockProgress(routeLeg)))
        mockNanos(100L + AUTO_ARRIVAL_NANOS)
        val reroutedRouteLeg = mockk<RouteLeg>()
        assertFalse(arrivalController.navigateNextRouteLeg(mockProgress(reroutedRouteLeg)))
        mockNanos(100 + AUTO_ARRIVAL_NANOS + AUTO_ARRIVAL_NANOS - 1)
        assertFalse(arrivalController.navigateNextRouteLeg(mockProgress(reroutedRouteLeg)))

        mockNanos(100 + AUTO_ARRIVAL_NANOS + AUTO_ARRIVAL_NANOS)
        assertTrue(arrivalController.navigateNextRouteLeg(mockProgress(reroutedRouteLeg)))
    }

    private fun mockNanos(nanos: Long) = every {
        SystemClock.elapsedRealtimeNanos()
    } returns nanos

    private fun mockProgress(mockedRouteLeg: RouteLeg) = mockk<RouteLegProgress> {
        every { routeLeg } returns mockedRouteLeg
    }
}
