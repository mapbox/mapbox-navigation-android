package com.mapbox.navigation.core.internal.congestions.processor

import com.mapbox.api.directions.v5.models.LegAnnotation
import com.mapbox.api.directions.v5.models.LegStep
import com.mapbox.api.directions.v5.models.ManeuverModifier
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.api.directions.v5.models.StepIntersection
import com.mapbox.api.directions.v5.models.StepManeuver
import com.mapbox.navigation.base.internal.CongestionNumericOverride
import com.mapbox.navigation.base.internal.route.overriddenTraffic
import com.mapbox.navigation.base.internal.route.update
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteStepProgress
import com.mapbox.navigation.core.internal.congestions.model.CongestionRangeGroup
import com.mapbox.navigation.core.internal.congestions.model.MetersPerSecond
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
import junit.framework.TestCase.assertNull
import org.junit.Rule
import org.junit.Test

class DecreaseTrafficUpdateActionHandlerTest {

    private val logger = mockk<LoggerFrontend>(relaxed = true)

    @get:Rule
    val loggerRule = LoggingFrontendTestRule(logger)

    private val actionHandler = DecreaseTrafficUpdateActionHandler(
        CongestionRangeGroup(0..39, 40..59, 60..79, 80..100),
    )

    @Test
    fun `reset heavy congestion to low`() {
        val originalCongestionNumeric = listOf(70, 70, 70, 70, 70, 70, 70, 70, 70, 70)
        val expectedCongestionNumeric = listOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        val action = createAction(originalCongestionNumeric)
        val expectedOverride = CongestionNumericOverride(0, 0, 10, originalCongestionNumeric)

        val actualResult = actionHandler.handleAction(action)

        assertEquals(expectedCongestionNumeric, actualResult?.getCongestionNumeric())
        assertEquals(expectedOverride, actualResult?.overriddenTraffic)
    }

    @Test
    fun `reset heavy congestion to low and severe to moderate`() {
        val originalCongestionNumeric = listOf(70, 70, 70, 70, 90, 90, 70, 70, 70, 70)
        val expectedCongestionNumeric = listOf(0, 0, 0, 0, 59, 59, 0, 0, 0, 0)
        val action = createAction(originalCongestionNumeric)

        val actualResult = actionHandler.handleAction(action)?.getCongestionNumeric()

        assertEquals(expectedCongestionNumeric, actualResult)
    }

    @Test
    fun `reset heavy congestion to low for 4800 meters ahead (8 segments)`() {
        val originalCongestionNumeric = listOf(70, 70, 70, 70, 70, 70, 70, 70, 70, 70)
        val expectedCongestionNumeric = listOf(0, 0, 0, 0, 0, 0, 0, 0, 70, 70)
        val expectedOverride =
            CongestionNumericOverride(0, 0, 8, originalCongestionNumeric.take(8))
        val action = createAction(originalCongestionNumeric, speedInKmPerHour = 80f)

        val actualResult = actionHandler.handleAction(action)

        assertEquals(expectedCongestionNumeric, actualResult?.getCongestionNumeric())
        assertEquals(expectedOverride, actualResult?.overriddenTraffic)
    }

    @Test
    fun `reset heavy congestion to low for 4800 meters ahead (8 segments) starting from 2nd element`() {
        val originalCongestionNumeric = listOf(70, 70, 70, 70, 70, 70, 70, 70, 70, 70)
        val expectedCongestionNumeric = listOf(70, 0, 0, 0, 0, 0, 0, 0, 0, 70)
        val expectedOverride =
            CongestionNumericOverride(0, 1, 8, originalCongestionNumeric.subList(1, 9))
        val action = createAction(
            originalCongestionNumeric,
            speedInKmPerHour = 80f,
            legProgressGeometryIndex = 1,
        )

        val actualResult = actionHandler.handleAction(action)

        assertEquals(expectedCongestionNumeric, actualResult?.getCongestionNumeric())
        assertEquals(expectedOverride, actualResult?.overriddenTraffic)
    }

    @Test
    fun `don't reset congestion if its level is low`() {
        val originalCongestionNumeric = listOf(39, 39, 39, 39, 39, 39, 39, 39, 39, 39)
        val action = createAction(originalCongestionNumeric)

        val actualResult = actionHandler.handleAction(action)

        assertNull(actualResult)
    }

    @Test
    fun `don't reset congestion if it's already overridden`() {
        val originalCongestionNumeric = listOf(59, 59, 59, 59, 59, 59, 59, 59, 59, 59)
        val override = CongestionNumericOverride(0, 0, 10, listOf())
        val action = createAction(originalCongestionNumeric, override = override)

        val actualResult = actionHandler.handleAction(action)

        assertNull(actualResult)
    }

    @Test
    fun `reset congestion if user passed previous override`() {
        val originalCongestionNumeric = listOf(59, 59, 59, 59, 59, 59, 59, 59, 59, 59)
        val expectedCongestionNumeric = listOf(59, 59, 59, 59, 59, 0, 0, 0, 0, 0)
        val override =
            CongestionNumericOverride(0, 0, 5, listOf())
        val expectedOverride =
            CongestionNumericOverride(0, 5, 5, listOf(59, 59, 59, 59, 59))
        val action = createAction(
            originalCongestionNumeric,
            override = override,
            legProgressGeometryIndex = 5,
        )

        val actualResult = actionHandler.handleAction(action)

        assertEquals(expectedOverride, actualResult?.overriddenTraffic)
        assertEquals(expectedCongestionNumeric, actualResult?.getCongestionNumeric())
    }

    @Test
    fun `reset heavy congestion to low up to 2nd intersection after on-ramp`() {
        val originalCongestionNumeric = listOf(70, 70, 70, 70, 70, 70, 70, 70, 70, 70)
        val expectedCongestionNumeric = listOf(0, 0, 0, 0, 70, 70, 70, 70, 70, 70)
        val expectedOverride =
            CongestionNumericOverride(0, 0, 4, originalCongestionNumeric.take(4))
        val action = createAction(originalCongestionNumeric, addOnRampOnFirstStep = true)

        val actualResult = actionHandler.handleAction(action)

        assertEquals(expectedCongestionNumeric, actualResult?.getCongestionNumeric())
        assertEquals(expectedOverride, actualResult?.overriddenTraffic)
    }

    @Test
    fun `reset heavy congestion to low up to exit from motorway`() {
        val originalCongestionNumeric = listOf(70, 70, 70, 70, 70, 70, 70, 70, 70, 70)
        val expectedCongestionNumeric = listOf(0, 0, 0, 0, 0, 0, 0, 0, 70, 70)
        val expectedOverride =
            CongestionNumericOverride(0, 0, 8, originalCongestionNumeric.take(8))
        val action = createAction(originalCongestionNumeric, exitFromMotorwayIndex = 8)

        val actualResult = actionHandler.handleAction(action)

        assertEquals(expectedCongestionNumeric, actualResult?.getCongestionNumeric())
        assertEquals(expectedOverride, actualResult?.overriddenTraffic)
    }

    @Test
    fun `reset heavy congestion to low up to 1st intersection of upcoming step`() {
        val originalCongestionNumeric = listOf(70, 70, 70, 70, 70, 70, 70, 70, 70, 70)
        val expectedCongestionNumeric = listOf(0, 0, 0, 0, 0, 0, 0, 70, 70, 70)
        val expectedOverride =
            CongestionNumericOverride(0, 0, 7, originalCongestionNumeric.take(7))
        val action = createAction(
            originalCongestionNumeric,
            indexOfFirstIntersectionOfUpcomingStep = 7,
        )

        val actualResult = actionHandler.handleAction(action)

        assertEquals(expectedCongestionNumeric, actualResult?.getCongestionNumeric())
        assertEquals(expectedOverride, actualResult?.overriddenTraffic)
    }

    private fun createAction(
        originalCongestionNumeric: List<Int>,
        legProgressGeometryIndex: Int = 0,
        speedInKmPerHour: Float = 100f,
        addOnRampOnFirstStep: Boolean = false,
        exitFromMotorwayIndex: Int? = null,
        override: CongestionNumericOverride? = null,
        indexOfFirstIntersectionOfUpcomingStep: Int = 100,
    ): TrafficUpdateAction.DecreaseTraffic {
        val firstLegStep = createFirstMockLegStep(addOnRampOnFirstStep)
        val secondLegStep = createSecondMockLegStep(exitFromMotorwayIndex)

        return TrafficUpdateAction.DecreaseTraffic(
            MetersPerSecond.fromKilometersPerHour(speedInKmPerHour),
            mockLegProgress(
                originalCongestionNumeric,
                legProgressGeometryIndex,
                firstLegStep,
                secondLegStep,
                indexOfFirstIntersectionOfUpcomingStep,
            ),
            createTestRoute(originalCongestionNumeric, override, firstLegStep, secondLegStep),
        )
    }

    private fun createFirstMockLegStep(addOnRampOnFirstStep: Boolean): LegStep {
        val onRampManeuver = mockk<StepManeuver>(relaxed = true) {
            every { type() } returns StepManeuver.ON_RAMP
        }
        return mockk(relaxed = true) {
            if (addOnRampOnFirstStep) {
                every { maneuver() } returns onRampManeuver
            }
            every { intersections() } returns emptyList()
        }
    }

    private fun createSecondMockLegStep(exitFromMotorwayIndex: Int?): LegStep {
        val exitFromMotorwayIntersection =
            mockExitFromMotorwayIntersection(exitFromMotorwayIndex)
        return mockk(relaxed = true) {
            every { intersections() } returns listOfNotNull(
                mockk(relaxed = true) { every { geometryIndex() } returns 3 },
                mockk(relaxed = true) { every { geometryIndex() } returns 4 },
                exitFromMotorwayIntersection,
            )
        }
    }

    private fun mockExitFromMotorwayIntersection(exitFromMotorwayIndex: Int?): StepIntersection? {
        exitFromMotorwayIndex ?: return null

        return mockk(relaxed = true) {
            every { lanes() } returns listOf(
                mockk(relaxed = true) {
                    every { indications() } returns listOf(ManeuverModifier.SLIGHT_RIGHT)
                },
            )
            every { geometryIndex() } returns exitFromMotorwayIndex
        }
    }

    private fun createTestRoute(
        congestionNumeric: List<Int>,
        override: CongestionNumericOverride?,
        firstLegStep: LegStep,
        secondLegStep: LegStep,
    ) = createNavigationRoutes(
        createDirectionsResponse(
            routes = listOf(
                createDirectionsRoute(
                    legs = listOf(
                        createRouteLeg(
                            createRouteLegAnnotation(
                                congestionNumeric = congestionNumeric,
                            ),
                            steps = listOf(firstLegStep, secondLegStep),
                        ),
                    ),
                ),
            ),
        ),
    ).first().update(
        directionsRouteBlock = { this },
        waypointsBlock = { this },
        overriddenTraffic = override,
    )

    private fun mockLegProgress(
        congestionNumeric: List<Int>,
        legProgressGeometryIndex: Int,
        firstLegStep: LegStep,
        secondLegStep: LegStep,
        indexOfFirstIntersectionOfUpcomingStep: Int,
    ): RouteLegProgress {
        val distances = mutableListOf<Double>()
        repeat(congestionNumeric.size) {
            distances.add(350.0)
        }
        val annotations = mockk<LegAnnotation> {
            every { congestionNumeric() } returns congestionNumeric
            every { distance() } returns distances
        }

        val currentRouteLeg = mockk<RouteLeg> {
            every { annotation() } returns annotations
            every { steps() } returns listOf(firstLegStep, secondLegStep)
        }
        val stepProgress = mockk<RouteStepProgress> {
            every { step } returns secondLegStep
            every { stepIndex } returns 1
            every { intersectionIndex } returns 0
        }
        return mockk {
            every { upcomingStep } returns mockk {
                every { intersections() } returns listOf(
                    mockk {
                        every { geometryIndex() } returns indexOfFirstIntersectionOfUpcomingStep
                    },
                )
            }
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
