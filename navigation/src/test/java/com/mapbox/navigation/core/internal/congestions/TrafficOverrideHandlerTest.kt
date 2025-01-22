@file:OptIn(ExperimentalMapboxNavigationAPI::class)

package com.mapbox.navigation.core.internal.congestions

import com.mapbox.api.directions.v5.models.StepIntersection
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.internal.CongestionNumericOverride
import com.mapbox.navigation.base.internal.route.overriddenTraffic
import com.mapbox.navigation.base.internal.route.update
import com.mapbox.navigation.base.options.TrafficOverrideOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.base.trip.model.RouteStepProgress
import com.mapbox.navigation.core.directions.session.RoutesExtra
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.testing.TestSystemClock
import com.mapbox.navigation.testing.factories.createDirectionsResponse
import com.mapbox.navigation.testing.factories.createDirectionsRoute
import com.mapbox.navigation.testing.factories.createNavigationRoutes
import com.mapbox.navigation.testing.factories.createRouteLeg
import com.mapbox.navigation.testing.factories.createRouteLegAnnotation
import com.mapbox.navigation.utils.internal.LoggerFrontend
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class TrafficOverrideHandlerTest {

    private val logger = mockk<LoggerFrontend>(relaxed = true)

    @get:Rule
    val loggerRule = LoggingFrontendTestRule(logger)

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @get:Rule
    val clock = TestSystemClock()

    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    private val sut = TrafficOverrideHandler(
        TrafficOverrideOptions.Builder()
            .highSpeedThresholdInKmPerHour(80)
            .build(),
    )

    @Test
    fun `if current speed is null routes shouldn't be updated`() {
        val routeProgress = mockRouteProgress()
        val navigationRoute = routeProgress.navigationRoute
        val locationMatcherResult = mockLocationMatcherResult(currentSpeedMsPerSecond = null)
        val routesUpdateResult = mockRoutesUpdateResult(navigationRoute)
        val observer = mockk<(List<NavigationRoute>) -> Unit>()

        with(sut) {
            registerRouteTrafficRefreshObserver(observer)
            onNewLocationMatcherResult(locationMatcherResult)
            onRoutesChanged(routesUpdateResult)
            onRouteProgressChanged(routeProgress)
        }

        verify(exactly = 0) { observer.invoke(any()) }
    }

    @Test
    fun `if current speed is above 100 km per h routes congestion should be cleared`() {
        val originalCongestionNumeric = listOf(70, 70, 70, 70, 70, 70, 70, 70, 70, 70)
        val expectedCongestionNumeric = listOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0)

        val routeProgress = mockRouteProgress(originalCongestionNumeric)
        val navigationRoute = routeProgress.navigationRoute
        val locationMatcherResult = mockLocationMatcherResult(currentSpeedMsPerSecond = 28.0)
        val routesUpdateResult = mockRoutesUpdateResult(navigationRoute)
        val observer = mockk<(List<NavigationRoute>) -> Unit>(relaxUnitFun = true)
        val navigationRoutesSlot = slot<List<NavigationRoute>>()
        every { observer.invoke(capture(navigationRoutesSlot)) } just runs

        with(sut) {
            registerRouteTrafficRefreshObserver(observer)
            onNewLocationMatcherResult(locationMatcherResult)
            onRoutesChanged(routesUpdateResult)
            onRouteProgressChanged(routeProgress)
        }

        verify(exactly = 1) {
            observer.invoke(capture(navigationRoutesSlot))
        }
        val capturedNavigationRoute = navigationRoutesSlot.captured.first()

        assertEquals(expectedCongestionNumeric, capturedNavigationRoute.getCongestionNumeric())
    }

    @Test
    fun `low speed on motorway shouldn't change congestion after 10 seconds`() {
        val originalCongestionNumeric = listOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)

        val routeProgress = mockRouteProgress(originalCongestionNumeric, isOnMotorway = true)
        val navigationRoute = routeProgress.navigationRoute
        val locationMatcherResult = mockLocationMatcherResult(currentSpeedMsPerSecond = 5.0)
        val routesUpdateResult = mockRoutesUpdateResult(navigationRoute)
        val observer = mockk<(List<NavigationRoute>) -> Unit>(relaxUnitFun = true)
        val navigationRoutesSlot = slot<List<NavigationRoute>>()
        every { observer.invoke(capture(navigationRoutesSlot)) } just runs

        with(sut) {
            registerRouteTrafficRefreshObserver(observer)
            onNewLocationMatcherResult(locationMatcherResult)
            onRoutesChanged(routesUpdateResult)
            onRouteProgressChanged(routeProgress)
        }

        clock.advanceTimeBy(10.seconds)
        with(sut) {
            onNewLocationMatcherResult(locationMatcherResult)
            onRouteProgressChanged(routeProgress)
        }

        verify(exactly = 0) { observer.invoke(any()) }
    }

    @Test
    fun `low speed on motorway should change congestion after 20 seconds`() {
        val originalCongestionNumeric = listOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        val expectedCongestionNumeric = listOf(40, 40, 40, 40, 40, 40, 40, 40, 0, 0, 0, 0)

        val routeProgress = mockRouteProgress(originalCongestionNumeric, isOnMotorway = true)
        val navigationRoute = routeProgress.navigationRoute
        val locationMatcherResult = mockLocationMatcherResult(currentSpeedMsPerSecond = 5.0)
        val routesUpdateResult = mockRoutesUpdateResult(navigationRoute)
        val observer = mockk<(List<NavigationRoute>) -> Unit>(relaxUnitFun = true)
        val navigationRoutesSlot = slot<List<NavigationRoute>>()
        every { observer.invoke(capture(navigationRoutesSlot)) } just runs

        with(sut) {
            registerRouteTrafficRefreshObserver(observer)
            onNewLocationMatcherResult(locationMatcherResult)
            onRoutesChanged(routesUpdateResult)
            onRouteProgressChanged(routeProgress)
        }

        clock.advanceTimeBy(21.seconds)

        with(sut) {
            onNewLocationMatcherResult(locationMatcherResult)
            onRouteProgressChanged(routeProgress)
        }

        verify(exactly = 1) {
            observer.invoke(capture(navigationRoutesSlot))
        }
        val capturedNavigationRoute = navigationRoutesSlot.captured.first()

        assertEquals(expectedCongestionNumeric, capturedNavigationRoute.getCongestionNumeric())
    }

    @Test
    fun `restore original traffic if user drops their speed`() {
        val originalCongestionNumeric = listOf(40, 40, 40, 40, 40, 40, 40, 40, 40)
        val expectedCongestionNumeric = listOf(40, 40, 40, 60, 60, 60, 40, 40, 40)
        val congestionOverride = CongestionNumericOverride(0, 3, 3, listOf(60, 60, 60))

        val routeProgress =
            mockRouteProgress(
                originalCongestionNumeric,
                legProgressGeometryIndex = 3,
                congestionOverride = congestionOverride,
            )
        val navigationRoute = routeProgress.navigationRoute
        val locationMatcherResult = mockLocationMatcherResult(currentSpeedMsPerSecond = 18.0)
        val routesUpdateResult = mockRoutesUpdateResult(navigationRoute)
        val observer = mockk<(List<NavigationRoute>) -> Unit>(relaxUnitFun = true)
        val navigationRoutesSlot = slot<List<NavigationRoute>>()
        every { observer.invoke(capture(navigationRoutesSlot)) } just runs

        with(sut) {
            registerRouteTrafficRefreshObserver(observer)
            onNewLocationMatcherResult(locationMatcherResult)
            onRoutesChanged(routesUpdateResult)
            onRouteProgressChanged(routeProgress)
        }

        verify(exactly = 1) {
            observer.invoke(capture(navigationRoutesSlot))
        }
        val capturedNavigationRoute = navigationRoutesSlot.captured.first()

        assertNull(capturedNavigationRoute.overriddenTraffic)
        assertEquals(expectedCongestionNumeric, capturedNavigationRoute.getCongestionNumeric())
    }

    private fun mockRoutesUpdateResult(route: NavigationRoute) = mockk<RoutesUpdatedResult> {
        every { navigationRoutes } returns listOf(route)
        every { reason } returns RoutesExtra.ROUTES_UPDATE_REASON_NEW
    }

    private fun mockLocationMatcherResult(currentSpeedMsPerSecond: Double?): LocationMatcherResult =
        mockk {
            every { enhancedLocation.speed } returns currentSpeedMsPerSecond
        }

    private fun createTestRoute(
        congestionNumeric: List<Int>,
        congestionOverride: CongestionNumericOverride?,
        isOnMotorway: Boolean,
    ): NavigationRoute {
        val distances = mutableListOf<Double>()
        val freeFlow = mutableListOf<Int>()
        repeat(congestionNumeric.size) {
            distances.add(350.0)
            freeFlow.add(100)
        }
        val intersection = mockk<StepIntersection>(relaxed = true) {
            every { lanes() } returns emptyList()
            every { mapboxStreetsV8()?.roadClass() } returns if (isOnMotorway) "motorway" else ""
        }
        return createNavigationRoutes(
            createDirectionsResponse(
                routes = listOf(
                    createDirectionsRoute(
                        legs = listOf(
                            createRouteLeg(
                                createRouteLegAnnotation(
                                    congestionNumeric = congestionNumeric,
                                    distance = distances,
                                    freeFlowSpeed = freeFlow,
                                ),
                                steps = listOf(
                                    mockk(relaxed = true) {
                                        every { intersections() } returns listOf(intersection)
                                    },
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        ).first().update(
            directionsRouteBlock = { this },
            waypointsBlock = { this },
            overriddenTraffic = congestionOverride,
        )
    }

    private fun mockRouteProgress(
        congestionNumeric: List<Int> = emptyList(),
        legProgressGeometryIndex: Int = 0,
        routeProgressState: RouteProgressState = RouteProgressState.TRACKING,
        congestionOverride: CongestionNumericOverride? = null,
        isOnMotorway: Boolean = false,
    ): RouteProgress {
        val route = createTestRoute(congestionNumeric, congestionOverride, isOnMotorway)

        val stepProgress = mockk<RouteStepProgress> {
            every { step } returns route.directionsRoute.legs()?.first()?.steps()?.first()
            every { intersectionIndex } returns 0
            every { stepIndex } returns 0
        }
        val legProgress = mockk<RouteLegProgress> {
            every { legIndex } returns 0
            every { geometryIndex } returns 0
            every { routeLeg } returns route.directionsRoute.legs()?.first()
            every { geometryIndex } returns legProgressGeometryIndex
            every { currentStepProgress } returns stepProgress
            every { upcomingStep } returns null
        }

        return mockk {
            every { navigationRoute } returns route
            every { currentState } returns routeProgressState
            every { currentLegProgress } returns legProgress
        }
    }

    private fun NavigationRoute.getCongestionNumeric() =
        directionsRoute.legs()?.first()?.annotation()?.congestionNumeric()
}
