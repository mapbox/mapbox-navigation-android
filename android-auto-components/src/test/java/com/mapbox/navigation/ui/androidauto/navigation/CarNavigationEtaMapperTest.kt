package com.mapbox.navigation.ui.androidauto.navigation

import androidx.car.app.model.Distance
import androidx.car.app.navigation.model.TravelEstimate
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.tripdata.progress.api.MapboxTripProgressApi
import com.mapbox.navigation.tripdata.progress.model.TripProgressUpdateValue
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CarNavigationEtaMapperTest {

    @Before
    fun setup() {
        mockkStatic(CarDistanceFormatter::class)
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun from() {
        every { CarDistanceFormatter.carDistance(any()) } returns mockk {
            every { displayDistance } returns 50.0
        }
        val routeProgress = mockk<RouteProgress>()
        val updateValue = mockk<TripProgressUpdateValue> {
            every { estimatedTimeToArrival } returns 1234567
            every { distanceRemaining } returns 45.0
            every { currentLegTimeRemaining } returns 600.0
            every { totalTimeRemaining } returns 154000.0
        }
        val tripProgressApi = mockk<MapboxTripProgressApi> {
            every { getTripProgress(routeProgress) } returns updateValue
        }
        val mapper = CarNavigationEtaMapper(tripProgressApi)

        val result = mapper.getDestinationTravelEstimate(routeProgress)

        assertEquals(1234567, result.arrivalTimeAtDestination!!.timeSinceEpochMillis)
        assertEquals(50.0, result.remainingDistance!!.displayDistance, 0.0)
        assertEquals(154000, result.remainingTimeSeconds)
    }

    @Test
    fun `remaining time covers the whole route on a multi-leg route`() {
        val routeProgress = mockk<RouteProgress>()
        val mapper = CarNavigationEtaMapper(
            tripProgressApi(currentLegTimeRemaining = 300.0, totalTimeRemaining = 1800.0),
        )

        val result = mapper.getDestinationTravelEstimate(routeProgress)

        assertEquals(1800, result.remainingTimeSeconds)
    }

    @Test
    fun `remaining time is unknown when the total time remaining is not finite`() {
        val routeProgress = mockk<RouteProgress>()
        val mapper = CarNavigationEtaMapper(
            tripProgressApi(currentLegTimeRemaining = 300.0, totalTimeRemaining = Double.NaN),
        )

        val result = mapper.getDestinationTravelEstimate(routeProgress)

        assertEquals(TravelEstimate.REMAINING_TIME_UNKNOWN, result.remainingTimeSeconds)
    }

    private fun tripProgressApi(
        currentLegTimeRemaining: Double,
        totalTimeRemaining: Double,
    ): MapboxTripProgressApi {
        every { CarDistanceFormatter.carDistance(any()) } returns Distance.create(
            1.0,
            Distance.UNIT_KILOMETERS,
        )
        val updateValue = mockk<TripProgressUpdateValue> {
            every { estimatedTimeToArrival } returns 1234567
            every { distanceRemaining } returns 1000.0
            every { this@mockk.currentLegTimeRemaining } returns currentLegTimeRemaining
            every { this@mockk.totalTimeRemaining } returns totalTimeRemaining
        }
        return mockk {
            every { getTripProgress(any()) } returns updateValue
        }
    }
}
