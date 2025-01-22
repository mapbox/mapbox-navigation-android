package com.mapbox.navigation.core.internal.congestions.processor

import com.mapbox.api.directions.v5.models.LegAnnotation
import com.mapbox.api.directions.v5.models.LegStep
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.api.directions.v5.models.StepIntersection
import com.mapbox.navigation.base.internal.CongestionNumericOverride
import com.mapbox.navigation.base.internal.route.overriddenTraffic
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteStepProgress
import com.mapbox.navigation.core.internal.congestions.model.CongestionRangeGroup
import com.mapbox.navigation.core.internal.congestions.model.TrafficUpdateAction
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.factories.createDirectionsResponse
import com.mapbox.navigation.testing.factories.createDirectionsRoute
import com.mapbox.navigation.testing.factories.createNavigationRoutes
import com.mapbox.navigation.testing.factories.createRouteLeg
import com.mapbox.navigation.testing.factories.createRouteLegAnnotation
import com.mapbox.navigation.utils.internal.LoggerFrontend
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import org.junit.Rule
import org.junit.Test

class IncreaseTrafficUpdateActionHandlerTest {

    private val logger = mockk<LoggerFrontend>(relaxed = true)

    @get:Rule
    val loggerRule = LoggingFrontendTestRule(logger)

    private val actionHandler = IncreaseTrafficUpdateActionHandler(
        CongestionRangeGroup(0..39, 40..59, 60..79, 80..100),
    )

    @Test
    fun `handler should update only 8 segments if last near congestion is low`() {
        val originalCongestionNumeric = listOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        val expectedCongestionNumeric = listOf(40, 40, 40, 40, 40, 40, 40, 40, 0, 0, 0, 0)
        val expectedOverride = CongestionNumericOverride(0, 0, 12, null)

        val action = createIncreaseAction(originalCongestionNumeric)

        val actualResult = actionHandler.handleAction(action)
        val actualCongestionNumeric = actualResult.getCongestionNumeric()

        assertEquals(expectedOverride, actualResult.overriddenTraffic)
        assertEquals(expectedCongestionNumeric, actualCongestionNumeric)
    }

    @Test
    fun `handler should increase severeness by 1 level for 12 segments`() {
        val originalCongestionNumeric = listOf(40, 40, 40, 40, 40, 40, 40, 40, 0, 0, 0, 0, 1)
        val expectedCongestionNumeric = listOf(60, 60, 60, 60, 60, 60, 60, 60, 40, 40, 40, 40, 1)

        val action = createIncreaseAction(originalCongestionNumeric)
        val actualResult = actionHandler.handleAction(action).getCongestionNumeric()

        assertEquals(expectedCongestionNumeric, actualResult)
    }

    @Test
    fun `handler should update all congestion numeric if less then 12 segments left`() {
        val originalCongestionNumeric = listOf(40, 40, 40, 40, 40, 40, 40, 40, 0)
        val expectedCongestionNumeric = listOf(60, 60, 60, 60, 60, 60, 60, 60, 40)
        val expectedOverride = CongestionNumericOverride(0, 0, 9, null)

        val action = createIncreaseAction(originalCongestionNumeric)
        val actualResult = actionHandler.handleAction(action)

        assertEquals(expectedCongestionNumeric, actualResult.getCongestionNumeric())
        assertEquals(expectedOverride, actualResult.overriddenTraffic)
    }

    @Test
    fun `handler should update left congestion numeric starting from index 1`() {
        val originalCongestionNumeric = listOf(0, 40, 40)
        val expectedCongestionNumeric = listOf(0, 60, 60)
        val expectedOverride = CongestionNumericOverride(0, 1, 2, null)

        val action = createIncreaseAction(originalCongestionNumeric, legProgressGeometryIndex = 1)
        val actualResult = actionHandler.handleAction(action)

        assertEquals(expectedCongestionNumeric, actualResult.getCongestionNumeric())
        assertEquals(expectedOverride, actualResult.overriddenTraffic)
    }

    @Test
    fun `handler should increase severeness by 1 level up to exit from motorway`() {
        val originalCongestionNumeric =
            listOf(40, 40, 40, 40, 40, 40, 40, 40, 0, /* exit starts here */0, 0, 0, 1)
        val expectedCongestionNumeric =
            listOf(60, 60, 60, 60, 60, 60, 60, 60, 40, /* exit starts here */0, 0, 0, 1)
        val expectedOverride = CongestionNumericOverride(0, 0, 9, null)

        val action = createIncreaseAction(originalCongestionNumeric, nonMotorwayIndexStart = 9)
        val actualResult = actionHandler.handleAction(action)

        assertEquals(expectedCongestionNumeric, actualResult.getCongestionNumeric())
        assertEquals(expectedOverride, actualResult.overriddenTraffic)
    }

    private fun createIncreaseAction(
        originalCongestionNumeric: List<Int>,
        legProgressGeometryIndex: Int = 0,
        nonMotorwayIndexStart: Int? = null,
    ) = TrafficUpdateAction.IncreaseTraffic(
        createTestRoute(originalCongestionNumeric),
        mockLegProgress(
            originalCongestionNumeric,
            legProgressGeometryIndex,
            nonMotorwayIndexStart ?: originalCongestionNumeric.size,
        ),
        90,
    )

    private fun createTestRoute(congestionNumeric: List<Int>) = createNavigationRoutes(
        createDirectionsResponse(
            routes = listOf(
                createDirectionsRoute(
                    legs = listOf(
                        createRouteLeg(
                            createRouteLegAnnotation(
                                congestionNumeric = congestionNumeric,
                            ),
                        ),
                    ),
                ),
            ),
        ),
    ).first()

    private fun mockLegProgress(
        congestionNumeric: List<Int>,
        legProgressGeometryIndex: Int,
        nonMotorwayIndexStart: Int,
    ): RouteLegProgress {
        val annotations = mockk<LegAnnotation> {
            every { congestionNumeric() } returns congestionNumeric
        }
        val intersectionLists = mutableListOf<StepIntersection>()
        repeat(congestionNumeric.size) {
            intersectionLists.add(
                if (nonMotorwayIndexStart <= it) {
                    mockk {
                        every { mapboxStreetsV8()?.roadClass() } returns "link"
                        every { geometryIndex() } returns it
                    }
                } else {
                    mockk {
                        every { mapboxStreetsV8()?.roadClass() } returns "motorway"
                        every { geometryIndex() } returns it
                    }
                },
            )
        }
        val currentLegStep = mockk<LegStep> {
            every { intersections() } returns intersectionLists
        }
        val nextLegStep = mockk<LegStep> {
            every { intersections() } returns emptyList()
        }
        val currentRouteLeg = mockk<RouteLeg> {
            every { annotation() } returns annotations
            every { steps() } returns listOf(currentLegStep, nextLegStep)
        }
        val stepProgress = mockk<RouteStepProgress> {
            every { step } returns currentLegStep
            every { stepIndex } returns 0
            every { intersectionIndex } returns 0
        }
        return mockk {
            every { legIndex } returns 0
            every { geometryIndex } returns 0
            every { routeLeg } returns currentRouteLeg
            every { geometryIndex } returns legProgressGeometryIndex
            every { currentStepProgress } returns stepProgress
        }
    }

    private fun NavigationRoute.getCongestionNumeric() =
        directionsRoute.legs()?.first()?.annotation()?.congestionNumeric()
}
