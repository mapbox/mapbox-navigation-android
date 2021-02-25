package com.mapbox.navigation.ui.tripprogress.api

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.ui.base.model.Expected
import com.mapbox.navigation.ui.base.model.tripprogress.TripProgressUpdateFormatter
import com.mapbox.navigation.ui.tripprogress.TripProgressProcessor
import com.mapbox.navigation.ui.tripprogress.model.TripProgressUpdateValue
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class MapboxTripProgressApiTest {

    @Test
    fun getTripProgress() {
        val progressFormatter = mockk<TripProgressUpdateFormatter>()
        val expectedEta = Calendar.getInstance().also {
            it.add(Calendar.SECOND, 600)
        }
        val routeProgress = mockk<RouteProgress> {
            every { durationRemaining } returns 600.0
            every { distanceRemaining } returns 100f
            every { currentLegProgress } returns mockk<RouteLegProgress> {
                every { durationRemaining } returns 2.0
            }
            every { distanceTraveled } returns 50f
            every { route } returns mockk<DirectionsRoute> {
                every { currentState } returns RouteProgressState.LOCATION_TRACKING
            }
        }

        val result = MapboxTripProgressApi(
            progressFormatter,
            TripProgressProcessor()
        ).getTripProgress(routeProgress) as Expected.Success<TripProgressUpdateValue>

        assertEquals(progressFormatter, result.value.formatter)
        assertEquals(100.0, result.value.distanceRemaining, 0.0)
        assertEquals(2.0, result.value.currentLegTimeRemaining, 0.0)
        assertEquals(600.0, result.value.totalTimeRemaining, 0.0)
        assertEquals(0.3333333432674408, result.value.percentRouteTraveled, 0.0)
        assertEquals(
            expectedEta.timeInMillis.toDouble(),
            result.value.estimatedTimeToArrival.toDouble(),
            30000.0
        )
    }
}
