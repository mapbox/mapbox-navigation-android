package com.mapbox.navigation.ui.androidauto.navigation

import android.os.Build
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
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O])
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
            every { currentLegTimeRemaining } returns 154000.0
        }
        val tripProgressApi = mockk<MapboxTripProgressApi> {
            every { getTripProgress(routeProgress) } returns updateValue
        }
        val mapper = CarNavigationEtaMapper(tripProgressApi)

        val result = mapper.getDestinationTravelEstimate(routeProgress)

        assertEquals(1234567, result.arrivalTimeAtDestination!!.timeSinceEpochMillis)
        assertEquals(50.0, result.remainingDistance!!.displayDistance, 0.0)
        assertEquals(154030, result.remainingTimeSeconds)
    }
}
