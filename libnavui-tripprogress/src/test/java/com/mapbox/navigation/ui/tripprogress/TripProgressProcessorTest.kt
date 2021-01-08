package com.mapbox.navigation.ui.tripprogress

import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteProgressState
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class TripProgressProcessorTest {

    @Test
    fun processWhenCalculateTripProgress() {
        val routeProgress = mockk<RouteProgress> {
            every { durationRemaining } returns 600.0
            every { distanceRemaining } returns 100f
            every { currentLegProgress } returns mockk<RouteLegProgress> {
                every { durationRemaining } returns 2.0
            }
            every { distanceTraveled } returns 50f
            every { route } returns mockk {
                every { currentState } returns RouteProgressState.LOCATION_TRACKING
            }
        }
        val expectedEta = Calendar.getInstance().also {
            it.add(Calendar.SECOND, 600)
        }

        val result =
            TripProgressProcessor().process(
                TripProgressAction.CalculateTripProgress(routeProgress)
            ) as TripProgressResult.RouteProgressCalculation

        assertEquals(
            expectedEta.timeInMillis.toDouble(),
            result.estimatedTimeToArrival.toDouble(), 30000.0
        )
        assertEquals(100.0, result.distanceRemaining, 0.0)
        assertEquals(2.0, result.currentLegTimeRemaining, 0.0)
        assertEquals(600.0, result.totalTimeRemaining, 0.0)
        assertEquals(0.3333333432674408, result.percentRouteTraveled, 0.0)
    }
}
