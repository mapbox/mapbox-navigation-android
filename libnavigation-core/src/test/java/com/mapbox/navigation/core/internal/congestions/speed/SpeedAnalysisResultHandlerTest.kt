package com.mapbox.navigation.core.internal.congestions.speed

import com.mapbox.api.directions.v5.models.LegAnnotation
import com.mapbox.api.directions.v5.models.LegStep
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.api.directions.v5.models.StepIntersection
import com.mapbox.navigation.base.internal.CongestionNumericOverride
import com.mapbox.navigation.base.internal.route.overriddenTraffic
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.base.trip.model.RouteStepProgress
import com.mapbox.navigation.core.internal.congestions.model.MetersPerSecond
import com.mapbox.navigation.core.internal.congestions.model.SpeedAnalysisResult
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.utils.internal.LoggerFrontend
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Rule
import org.junit.Test

class SpeedAnalysisResultHandlerTest {

    private val logger = mockk<LoggerFrontend>(relaxed = true)

    @get:Rule
    val loggerRule = LoggingFrontendTestRule(logger)

    private val analise = SpeedAnalysisResultHandlerImpl(80)

    @Test
    fun `skip analysis if route progress state is not tracking`() {
        val routeProgress = mockRouteProgress(routeProgressState = RouteProgressState.OFF_ROUTE)

        val result = analise(routeProgress, mockk())
        assertTrue(result is SpeedAnalysisResult.SkippedAnalysis)
    }

    @Test
    fun `high speed should be detected if current speed is above threshold`() {
        val location = mockLocation(speedKMPH = 100f)
        val routeProgress = mockRouteProgress()

        val actualResult = analise(routeProgress, location)
        val expectedSpeed = MetersPerSecond.fromKilometersPerHour(100f)
        assertTrue(actualResult is SpeedAnalysisResult.HighSpeedDetected)
        assertEquals(
            expectedSpeed,
            (actualResult as SpeedAnalysisResult.HighSpeedDetected).currentSpeed,
        )
    }

    @Test
    fun `wrong prediction should restore congestion`() {
        val override = CongestionNumericOverride(0, 0, 1, listOf())
        val location = mockLocation(speedKMPH = 65f)
        val routeProgress = mockRouteProgress(
            freeFlowSpeedAnnotations = listOf(100),
            congestionOverride = override,
        )

        val actual = analise(routeProgress, location)
        assertTrue(actual is SpeedAnalysisResult.WrongFalsePositiveOverrideDetected)
        assertEquals(
            override,
            (actual as SpeedAnalysisResult.WrongFalsePositiveOverrideDetected)
                .congestionNumericOverride,
        )
    }

    @Test
    fun `low speed on motorway should be detected if current speed is half of free flow`() {
        val location = mockLocation(speedKMPH = 45f)
        val routeProgress = mockRouteProgress(
            freeFlowSpeedAnnotations = listOf(100),
            isOnMotorway = true,
        )

        val actual = analise(routeProgress, location)

        assertTrue(actual is SpeedAnalysisResult.LowSpeedDetected)
    }

    @Test
    fun `low speed on non-motorway should be ok`() {
        val location = mockLocation(speedKMPH = 45f)
        val routeProgress = mockRouteProgress(
            freeFlowSpeedAnnotations = listOf(100),
        )

        val actual = analise(routeProgress, location)

        assertTrue(actual is SpeedAnalysisResult.SpeedIsOk)
    }

    private fun mockLocation(speedKMPH: Float): LocationMatcherResult {
        return mockk {
            every {
                enhancedLocation.speed
            } returns MetersPerSecond.fromKilometersPerHour(speedKMPH).value.toDouble()
        }
    }

    private fun mockRouteProgress(
        legProgressGeometryIndex: Int = 0,
        freeFlowSpeedAnnotations: List<Int> = listOf(),
        routeProgressState: RouteProgressState = RouteProgressState.TRACKING,
        congestionOverride: CongestionNumericOverride? = null,
        isOnMotorway: Boolean = false,
    ): RouteProgress {
        val annotations = mockk<LegAnnotation> {
            every { freeflowSpeed() } returns freeFlowSpeedAnnotations
            every { congestionNumeric() } returns listOf(0, 0, 0)
        }
        val currentRouteLeg = mockk<RouteLeg> {
            every { annotation() } returns annotations
        }
        val intersection = mockk<StepIntersection> {
            every { mapboxStreetsV8()?.roadClass() } returns if (isOnMotorway)"motorway" else ""
        }
        val legStep = mockk<LegStep> {
            every { intersections() } returns listOf(intersection)
        }
        val stepProgress = mockk<RouteStepProgress> {
            every { step } returns legStep
            every { intersectionIndex } returns 0
        }
        val legProgress = mockk<RouteLegProgress> {
            every { legIndex } returns 0
            every { geometryIndex } returns 0
            every { routeLeg } returns currentRouteLeg
            every { geometryIndex } returns legProgressGeometryIndex
            every { currentStepProgress } returns stepProgress
        }
        val route = mockk<NavigationRoute> {
            every { overriddenTraffic } returns congestionOverride
        }

        return mockk {
            every { navigationRoute } returns route
            every { currentState } returns routeProgressState
            every { currentLegProgress } returns legProgress
        }
    }
}
