package com.mapbox.navigation.ui.maps.route.line.api

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.core.constants.Constants
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.testing.FileUtils.loadJsonFixture
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.base.internal.model.route.RouteConstants
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineUtils
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineColorResources
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineExpressionData
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineResources
import com.mapbox.navigation.ui.maps.route.line.model.VanishingPointState
import com.mapbox.navigation.utils.internal.JobControl
import com.mapbox.navigation.utils.internal.ThreadController
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class VanishingRouteLineTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()
    private val parentJob = SupervisorJob()
    private val testScope = CoroutineScope(parentJob + coroutineRule.testDispatcher)

    @Before
    fun setUp() {
        mockkObject(ThreadController)
        every { ThreadController.getMainScopeAndRootJob() } returns JobControl(parentJob, testScope)
        every { ThreadController.IODispatcher } returns coroutineRule.testDispatcher
    }

    @After
    fun cleanUp() {
        unmockkObject(ThreadController)
    }

    @Test
    fun initWithRoute() = coroutineRule.runBlockingTest {
        val route = getRoute()

        val vanishingRouteLine = VanishingRouteLine().also {
            it.initWithRoute(route)
        }

        assertEquals(
            0.0000025451727518618744,
            vanishingRouteLine.primaryRouteLineGranularDistances!!.distance,
            0.0
        )
        assertEquals(
            8,
            vanishingRouteLine.primaryRouteLineGranularDistances!!.distancesArray.size()
        )
    }

    @Test
    fun initWithRoute_clearState() = coroutineRule.runBlockingTest {
        val route = getRoute()
        val vanishingRouteLine = VanishingRouteLine()

        vanishingRouteLine.initWithRoute(route)
        assertEquals(
            8,
            vanishingRouteLine.primaryRouteLineGranularDistances!!.distancesArray.size()
        )
        unmockkObject(ThreadController)
        val jobControl = ThreadController.getMainScopeAndRootJob()
        mockkObject(ThreadController)

        every { ThreadController.getMainScopeAndRootJob() } returns jobControl
        Runnable {
            vanishingRouteLine.initWithRoute(getVeryLongRoute())
        }.run()
        jobControl.job.cancel()

        assertNull(vanishingRouteLine.primaryRouteLineGranularDistances)
    }

    @Test
    fun updateVanishingPointState_when_LOCATION_TRACKING() {
        val vanishingRouteLine = VanishingRouteLine().also {
            it.updateVanishingPointState(RouteProgressState.TRACKING)
        }

        assertEquals(VanishingPointState.ENABLED, vanishingRouteLine.vanishingPointState)
    }

    @Test
    fun updateVanishingPointState_when_ROUTE_COMPLETE() {
        val vanishingRouteLine = VanishingRouteLine().also {
            it.updateVanishingPointState(RouteProgressState.COMPLETE)
        }

        assertEquals(
            VanishingPointState.ONLY_INCREASE_PROGRESS,
            vanishingRouteLine.vanishingPointState
        )
    }

    @Test
    fun updateVanishingPointState_when_other() {
        val vanishingRouteLine = VanishingRouteLine().also {
            it.updateVanishingPointState(RouteProgressState.OFF_ROUTE)
        }

        assertEquals(VanishingPointState.DISABLED, vanishingRouteLine.vanishingPointState)
    }

    @Test
    fun clear() = coroutineRule.runBlockingTest {
        val vanishingRouteLine = VanishingRouteLine().also {
            it.initWithRoute(getRoute())
        }
        assertNotNull(vanishingRouteLine.primaryRoutePoints)
        assertNotNull(vanishingRouteLine.primaryRouteLineGranularDistances)

        vanishingRouteLine.clear()

        assertNull(vanishingRouteLine.primaryRoutePoints)
        assertNull(vanishingRouteLine.primaryRouteLineGranularDistances)
    }

    @Test
    fun getTraveledRouteLineExpressions() {
        val expectedTrafficExpression = "[step, [line-progress], [rgba, 0.0, 0.0, 0.0, 0.0], 0.0," +
            " [rgba, 255.0, 255.0, 255.0, 1.0]]"
        val expectedRouteLineExpression = "[step, [line-progress], [rgba, 0.0, 0.0, 0.0, 0.0]," +
            " 0.0, [rgba, 0.0, 0.0, 3.0, 0.0]]"
        val expectedCasingExpression = "[step, [line-progress], [rgba, 0.0, 0.0, 10.0, 0.0]," +
            " 0.0, [rgba, 0.0, 0.0, 4.0, 0.0]]"

        val colorResources = RouteLineColorResources.Builder()
            .routeModerateColor(-1)
            .routeUnknownTrafficColor(-1)
            .build()

        val route = getRoute()
        val lineString = LineString.fromPolyline(route.geometry() ?: "", Constants.PRECISION_6)
        val vanishingRouteLine = VanishingRouteLine()
        vanishingRouteLine.initWithRoute(route)
        vanishingRouteLine.primaryRouteRemainingDistancesIndex = 1
        val segments: List<RouteLineExpressionData> =
            MapboxRouteLineUtils.calculateRouteLineSegments(
                getRoute(),
                listOf(),
                true,
                colorResources,
                RouteConstants.RESTRICTED_ROAD_SECTION_SCALE,
                false
            )

        val result = vanishingRouteLine.getTraveledRouteLineExpressions(
            lineString.coordinates()[0],
            segments,
            genericMockResourceProvider,
            -1
        )

        assertEquals(expectedTrafficExpression, result!!.trafficLineExpression.toString())
        assertEquals(expectedRouteLineExpression, result.routeLineExpression.toString())
        assertEquals(expectedCasingExpression, result.routeLineCasingExpression.toString())
    }

    @Test
    fun getTraveledRouteLineExpressions_multilegRoute_deEmphasizeNonActiveLegs() {
        val expectedTrafficExp = "[step, [line-progress], [rgba, 0.0, 0.0, 0.0, 0.0], " +
            "0.13240839439705454, [rgba, 86.0, 168.0, 251.0, 1.0], 0.18199725933538882, " +
            "[rgba, 255.0, 149.0, 0.0, 1.0], 0.2256358178763112, [rgba, 86.0, 168.0, 251.0, 1.0]," +
            " 0.32147751186805656, [rgba, 255.0, 149.0, 0.0, 1.0], 0.3838765722116185, " +
            "[rgba, 86.0, 168.0, 251.0, 1.0], 0.4891841628737826, [rgba, 0.0, 0.0, 0.0, 0.0]]"
        val expectedRouteLineExp = "[step, [line-progress], [rgba, 0.0, 0.0, 0.0, 0.0], " +
            "0.13240839439705454, [rgba, 86.0, 168.0, 251.0, 1.0], 0.4891841628737826, " +
            "[rgba, 0.0, 0.0, 0.0, 0.0]]"
        val expectedCasingExp = "[step, [line-progress], [rgba, 0.0, 0.0, 0.0, 0.0]," +
            " 0.13240839439705454, [rgba, 47.0, 122.0, 198.0, 1.0], 0.4891841628737826," +
            " [rgba, 0.0, 0.0, 0.0, 0.0]]"
        val route = getMultilegWithTwoLegs()
        val vanishingRouteLine = VanishingRouteLine()
        vanishingRouteLine.initWithRoute(route)
        vanishingRouteLine.primaryRouteRemainingDistancesIndex = 7
        vanishingRouteLine.vanishPointOffset = 0.1322571610688955
        val segments = listOf(
            RouteLineExpressionData(0.0, -27392, 0),
            RouteLineExpressionData(0.07932429566550842, -45747, 0),
            RouteLineExpressionData(0.10338667841237215, -11097861, 0),
            RouteLineExpressionData(0.18199725933538882, -27392, 0),
            RouteLineExpressionData(0.2256358178763112, -11097861, 0),
            RouteLineExpressionData(0.32147751186805656, -27392, 0),
            RouteLineExpressionData(0.3838765722116185, -11097861, 0),
            RouteLineExpressionData(0.4891841628737826, 0, 1),
            RouteLineExpressionData(0.5402820600662328, 0, 1),
            RouteLineExpressionData(0.9738127865054893, 0, 1)
        )

        val result = vanishingRouteLine.getTraveledRouteLineExpressions(
            Point.fromLngLat(-122.52351984901476, 37.97384101461195),
            segments,
            RouteLineResources.Builder().build(),
            0
        )

        assertEquals(expectedTrafficExp, result!!.trafficLineExpression.toString())
        assertEquals(expectedRouteLineExp, result.routeLineExpression.toString())
        assertEquals(expectedCasingExp, result.routeLineCasingExpression.toString())
    }

    private fun getRoute(): DirectionsRoute {
        val routeAsJson = loadJsonFixture("short_route.json")
        return DirectionsRoute.fromJson(routeAsJson)
    }

    private fun getVeryLongRoute(): DirectionsRoute {
        val routeAsJson = loadJsonFixture("cross-country-route.json")
        return DirectionsRoute.fromJson(routeAsJson)
    }

    private val genericMockResourceProvider = mockk<RouteLineResources> {
        every { routeLineColorResources } returns mockk<RouteLineColorResources> {
            every { routeUnknownTrafficColor } returns 1
            every { routeLineTraveledColor } returns 0
            every { routeDefaultColor } returns 3
            every { routeCasingColor } returns 4
            every { routeLowCongestionColor } returns 5
            every { routeModerateColor } returns 6
            every { routeSevereColor } returns 7
            every { routeHeavyColor } returns 8
            every { alternativeRouteUnknownTrafficColor } returns 9
            every { routeLineTraveledCasingColor } returns 10
            every { inActiveRouteLegsColor } returns 11
        }
        every { trafficBackfillRoadClasses } returns listOf()
    }

    private fun getMultilegWithTwoLegs(): DirectionsRoute {
        val routeAsJson = loadJsonFixture("multileg-route-two-legs.json")
        return DirectionsRoute.fromJson(routeAsJson)
    }
}
