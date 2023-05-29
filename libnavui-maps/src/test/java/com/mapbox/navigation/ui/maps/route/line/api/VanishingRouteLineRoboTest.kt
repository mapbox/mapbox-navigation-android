package com.mapbox.navigation.ui.maps.route.line.api

import android.graphics.Color
import com.mapbox.core.constants.Constants
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.testing.NativeRouteParserRule
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineUtils
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineColorResources
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineExpressionData
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineResources
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineTrimExpressionProvider
import com.mapbox.navigation.ui.maps.testing.TestingUtil.loadNavigationRoute
import com.mapbox.navigation.utils.internal.InternalJobControlFactory
import com.mapbox.navigation.utils.internal.JobControl
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class VanishingRouteLineRoboTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    @get:Rule
    val nativeRouteParserRule = NativeRouteParserRule()

    private val parentJob = SupervisorJob()
    private val testScope = CoroutineScope(parentJob + coroutineRule.testDispatcher)
    private lateinit var testJobControl: JobControl

    @Before
    fun setUp() {
        mockkObject(InternalJobControlFactory)
        every {
            InternalJobControlFactory.createDefaultScopeJobControl()
        } returns JobControl(parentJob, testScope)
        testJobControl = InternalJobControlFactory.createDefaultScopeJobControl()
    }

    @After
    fun cleanUp() {
        unmockkObject(InternalJobControlFactory)
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
            .routeModerateCongestionColor(-1)
            .routeUnknownCongestionColor(-1)
            .build()

        val route = loadNavigationRoute("short_route.json")
        val lineString = LineString.fromPolyline(
            route.directionsRoute.geometry() ?: "",
            Constants.PRECISION_6
        )
        val vanishingRouteLine = VanishingRouteLine()
        vanishingRouteLine.upcomingRouteGeometrySegmentIndex = 1
        val segments: List<RouteLineExpressionData> =
            MapboxRouteLineUtils.calculateRouteLineSegments(
                route,
                listOf(),
                true,
                colorResources
            )

        val result = vanishingRouteLine.getTraveledRouteLineExpressions(
            lineString.coordinates()[0],
            granularDistances = MapboxRouteLineUtils.granularDistancesProvider(route)!!,
            segments,
            null,
            genericMockResourceProvider,
            -1,
            0.0,
            false
        )

        assertEquals(
            expectedTrafficExpression,
            result!!.trafficLineExpression.generateExpression().toString()
        )
        assertEquals(
            expectedRouteLineExpression,
            result.routeLineExpression.generateExpression().toString()
        )
        assertEquals(
            expectedCasingExpression,
            result.routeLineCasingExpression.generateExpression().toString()
        )
    }

    @Test
    fun getTraveledRouteLineExpressionsWithPoint() {
        val expectedTrafficExpression = "[literal, [0.0, 0.0]]"
        val expectedRouteLineExpression = "[literal, [0.0, 0.0]]"
        val expectedCasingExpression = "[literal, [0.0, 0.0]]"

        val route = loadNavigationRoute("short_route.json")
        val lineString = LineString.fromPolyline(
            route.directionsRoute.geometry() ?: "",
            Constants.PRECISION_6
        )
        val vanishingRouteLine = VanishingRouteLine()
        vanishingRouteLine.upcomingRouteGeometrySegmentIndex = 1

        val result = vanishingRouteLine.getTraveledRouteLineExpressions(
            lineString.coordinates()[0],
            granularDistances = MapboxRouteLineUtils.granularDistancesProvider(route)!!,
        )

        assertEquals(
            expectedTrafficExpression,
            result!!.trafficLineExpression.generateExpression().toString()
        )
        assertEquals(
            expectedRouteLineExpression,
            result.routeLineExpression.generateExpression().toString()
        )
        assertEquals(
            expectedCasingExpression,
            result.routeLineCasingExpression.generateExpression().toString()
        )
        assertTrue(result.trafficLineExpression is RouteLineTrimExpressionProvider)
        assertTrue(result.routeLineExpression is RouteLineTrimExpressionProvider)
        assertTrue(result.routeLineCasingExpression is RouteLineTrimExpressionProvider)
    }

    @Test
    fun getTraveledRouteLineExpressions_withRestrictedLineExpressionData() {
        val expectedRestrictedExpressionContents = listOf(
            StringChecker("step"),
            StringChecker("[line-progress]"),
            StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            DoubleChecker(0.0),
            StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            DoubleChecker(0.4476941554901612),
            StringChecker("[rgba, 0.0, 0.0, 12.0, 0.0]"),
            DoubleChecker(0.4677574367125704),
            StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            DoubleChecker(0.5021643413784516),
            StringChecker("[rgba, 0.0, 0.0, 12.0, 0.0]"),
            DoubleChecker(0.5196445159361185),
            StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
        )
        val colorResources = RouteLineColorResources.Builder()
            .restrictedRoadColor(Color.CYAN)
            .build()

        val route = loadNavigationRoute("route-with-restrictions.json")
        val lineString = LineString.fromPolyline(
            route.directionsRoute.geometry() ?: "",
            Constants.PRECISION_6
        )
        val vanishingRouteLine = VanishingRouteLine()
        vanishingRouteLine.upcomingRouteGeometrySegmentIndex = 1
        val segments: List<RouteLineExpressionData> =
            MapboxRouteLineUtils.calculateRouteLineSegments(
                route,
                listOf(),
                true,
                colorResources
            )
        val restrictedSegments = MapboxRouteLineUtils.extractRouteRestrictionData(
            route,
            MapboxRouteLineUtils.granularDistancesProvider
        )

        val result = vanishingRouteLine.getTraveledRouteLineExpressions(
            lineString.coordinates()[0],
            granularDistances = MapboxRouteLineUtils.granularDistancesProvider(route)!!,
            segments,
            restrictedSegments,
            genericMockResourceProvider,
            0,
            0.0,
            false
        )

        checkExpression(
            expectedRestrictedExpressionContents,
            result!!.restrictedRoadExpression!!.generateExpression()
        )
    }

    @Test
    fun getTraveledRouteLineExpressions_whenSoftGradientTrue() {
        val expectedTrafficExpression = "[interpolate, [linear], [line-progress], 0.0, " +
            "[rgba, 255.0, 255.0, 255.0, 1.0]]"
        val expectedRouteLineExpression = "[step, [line-progress], [rgba, 0.0, 0.0, 0.0, 0.0]," +
            " 0.0, [rgba, 0.0, 0.0, 3.0, 0.0]]"
        val expectedCasingExpression = "[step, [line-progress], [rgba, 0.0, 0.0, 10.0, 0.0]," +
            " 0.0, [rgba, 0.0, 0.0, 4.0, 0.0]]"

        val colorResources = RouteLineColorResources.Builder()
            .routeModerateCongestionColor(-1)
            .routeUnknownCongestionColor(-1)
            .build()

        val route = loadNavigationRoute("short_route.json")
        val lineString = LineString.fromPolyline(
            route.directionsRoute.geometry() ?: "",
            Constants.PRECISION_6
        )
        val vanishingRouteLine = VanishingRouteLine()
        vanishingRouteLine.upcomingRouteGeometrySegmentIndex = 1
        val segments: List<RouteLineExpressionData> =
            MapboxRouteLineUtils.calculateRouteLineSegments(
                route,
                listOf(),
                true,
                colorResources
            )

        val result = vanishingRouteLine.getTraveledRouteLineExpressions(
            lineString.coordinates()[0],
            granularDistances = MapboxRouteLineUtils.granularDistancesProvider(route)!!,
            segments,
            null,
            genericMockResourceProvider,
            -1,
            20.0,
            true
        )

        assertEquals(
            expectedTrafficExpression,
            result!!.trafficLineExpression.generateExpression().toString()
        )
        assertEquals(
            expectedRouteLineExpression,
            result.routeLineExpression.generateExpression().toString()
        )
        assertEquals(
            expectedCasingExpression,
            result.routeLineCasingExpression.generateExpression().toString()
        )
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
            " [rgba, 255.0, 255.0, 0.0, 1.0]]"
        val route = loadNavigationRoute("multileg-route-two-legs.json")
        val vanishingRouteLine = VanishingRouteLine()
        vanishingRouteLine.upcomingRouteGeometrySegmentIndex = 7
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
            granularDistances = MapboxRouteLineUtils.granularDistancesProvider(route)!!,
            segments,
            null,
            RouteLineResources.Builder()
                .routeLineColorResources(
                    RouteLineColorResources.Builder()
                        .inActiveRouteLegsCasingColor(Color.YELLOW)
                        .build()
                )
                .build(),
            0,
            0.0,
            false
        )

        assertEquals(
            expectedTrafficExp,
            result!!.trafficLineExpression.generateExpression().toString()
        )
        assertEquals(
            expectedRouteLineExp,
            result.routeLineExpression.generateExpression().toString()
        )
        assertEquals(
            expectedCasingExp,
            result.routeLineCasingExpression.generateExpression().toString()
        )
    }

    private val genericMockResourceProvider = mockk<RouteLineResources> {
        every { routeLineColorResources } returns mockk<RouteLineColorResources> {
            every { routeUnknownCongestionColor } returns 1
            every { routeLineTraveledColor } returns 0
            every { routeDefaultColor } returns 3
            every { routeCasingColor } returns 4
            every { routeLowCongestionColor } returns 5
            every { routeModerateCongestionColor } returns 6
            every { routeSevereCongestionColor } returns 7
            every { routeHeavyCongestionColor } returns 8
            every { alternativeRouteUnknownCongestionColor } returns 9
            every { routeLineTraveledCasingColor } returns 10
            every { inActiveRouteLegsColor } returns 11
            every { restrictedRoadColor } returns 12
            every { inActiveRouteLegsCasingColor } returns 13
        }
        every { trafficBackfillRoadClasses } returns listOf()
    }
}
