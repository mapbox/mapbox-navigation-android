package com.mapbox.navigation.navigator

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.navigator.internal.RouteProgressMapper
import com.mapbox.navigator.NavigationStatus
import com.mapbox.navigator.RouteState
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class RouteProgressMapperTest {

    private val routeProgressMapper = RouteProgressMapper()

    @Test
    fun `route progress is null when route is null`() {
        val navigationStatus: NavigationStatus = mockk()

        val routeProgress = routeProgressMapper.getRouteProgress(
            null,
            null,
            navigationStatus
        )

        assertNull(routeProgress)
    }

    @Test
    fun `route progress minimum requirements`() {
        val directionsRoute: DirectionsRoute = mockk {
            every { legs() } returns listOf(
                mockk {
                    every { distance() } returns 100.0
                    every { steps() } returns listOf(
                        mockk {
                            every { distance() } returns 20.0
                            every { geometry() } returns "y{v|bA{}diiGOuDpBiMhM{k@~Syj@bLuZlEiM"
                        }
                    )
                }
            )
        }
        val navigationStatus: NavigationStatus = mockk {
            every { stepIndex } returns 0
            every { legIndex } returns 0
            every { remainingLegDistance } returns 80.0f
            every { remainingLegDuration } returns 10000
            every { remainingStepDistance } returns 15.0f
            every { remainingStepDuration } returns 300
            every { routeState } returns RouteState.TRACKING
            every { bannerInstruction } returns null
            every { voiceInstruction } returns null
            every { inTunnel } returns false
        }

        val routeProgress = routeProgressMapper.getRouteProgress(
            directionsRoute,
            null,
            navigationStatus
        )

        assertNotNull(routeProgress)
    }
}
