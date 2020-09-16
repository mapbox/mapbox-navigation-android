package com.mapbox.navigation.base.trip.model

import com.mapbox.navigation.testing.BuilderTest
import io.mockk.mockk
import org.junit.Test
import kotlin.reflect.KClass

class RouteProgressTest : BuilderTest<RouteProgress, RouteProgress.Builder>() {
    override fun getImplementationClass(): KClass<RouteProgress> = RouteProgress::class

    override fun getFilledUpBuilder(): RouteProgress.Builder {
        return RouteProgress.Builder(mockk(relaxed = true))
            .routeGeometryWithBuffer(mockk(relaxed = true))
            .bannerInstructions(mockk(relaxed = true))
            .voiceInstructions(mockk(relaxed = true))
            .currentState(mockk(relaxed = true))
            .currentLegProgress(mockk(relaxed = true))
            .upcomingStepPoints(mockk(relaxed = true))
            .inTunnel(true)
            .distanceRemaining(123f)
            .distanceTraveled(456f)
            .durationRemaining(789.0)
            .fractionTraveled(101112f)
            .remainingWaypoints(131415)
            .upcomingRouteAlerts(mockk(relaxed = true))
    }

    @Test
    override fun trigger() {
        // only used to trigger JUnit4 to run this class if all test cases come from the parent
    }
}
