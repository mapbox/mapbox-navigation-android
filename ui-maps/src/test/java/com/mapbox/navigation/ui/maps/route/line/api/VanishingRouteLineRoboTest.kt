package com.mapbox.navigation.ui.maps.route.line.api

import android.content.Context
import android.graphics.Color
import androidx.appcompat.content.res.AppCompatResources
import com.mapbox.core.constants.Constants
import com.mapbox.geojson.LineString
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.testing.NativeRouteParserRule
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineUtils
import com.mapbox.navigation.ui.maps.internal.route.line.toData
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineViewOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineColorResources
import com.mapbox.navigation.ui.maps.testing.TestingUtil.loadNavigationRoute
import com.mapbox.navigation.utils.internal.InternalJobControlFactory
import com.mapbox.navigation.utils.internal.JobControl
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
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

    private val ctx: Context = mockk()

    private val parentJob = SupervisorJob()
    private val testScope = CoroutineScope(parentJob + coroutineRule.testDispatcher)
    private lateinit var testJobControl: JobControl

    @Before
    fun setUp() {
        mockkStatic(AppCompatResources::class)
        every { AppCompatResources.getDrawable(any(), any()) } returns mockk(relaxed = true) {
            every { intrinsicWidth } returns 24
            every { intrinsicHeight } returns 24
        }
        mockkObject(InternalJobControlFactory)
        every {
            InternalJobControlFactory.createDefaultScopeJobControl()
        } returns JobControl(parentJob, testScope)
        testJobControl = InternalJobControlFactory.createDefaultScopeJobControl()
    }

    @After
    fun cleanUp() {
        unmockkObject(InternalJobControlFactory)
        unmockkStatic(AppCompatResources::class)
    }

    @Test
    fun getTraveledRouteLineExpressionsWithZeroPoint() = coroutineRule.runBlockingTest {
        val expectedTrafficExpression = "[literal, [0.0, 0.0]]"
        val expectedRouteLineExpression = "[literal, [0.0, 0.0]]"
        val expectedCasingExpression = "[literal, [0.0, 0.0]]"

        val route = loadNavigationRoute("short_route.json")
        val lineString = LineString.fromPolyline(
            route.directionsRoute.geometry() ?: "",
            Constants.PRECISION_6,
        )
        val vanishingRouteLine = VanishingRouteLine()
        vanishingRouteLine.upcomingRouteGeometrySegmentIndex = 1

        val result = vanishingRouteLine.getTraveledRouteLineExpressions(
            lineString.coordinates()[0],
            granularDistances = MapboxRouteLineUtils.granularDistancesProvider(route)!!,
        )

        val viewOptions = MapboxRouteLineViewOptions.Builder(ctx)
            .routeLineColorResources(
                RouteLineColorResources.Builder()
                    .routeLineTraveledColor(Color.BLUE)
                    .routeLineTraveledCasingColor(Color.GREEN)
                    .build(),
            )
            .build()
            .toData()

        assertEquals(
            expectedTrafficExpression,
            getAppliedExpression(
                result!!.trafficLineExpressionCommandHolder,
                viewOptions,
                "line-trim-offset",
            ).toString(),
        )
        assertEquals(
            expectedRouteLineExpression,
            getAppliedExpression(
                result!!.routeLineExpressionCommandHolder,
                viewOptions,
                "line-trim-offset",
            ).toString(),
        )
        assertEquals(
            expectedCasingExpression,
            getAppliedExpression(
                result!!.routeLineCasingExpressionCommandHolder,
                viewOptions,
                "line-trim-offset",
            ).toString(),
        )

        assertTrue(
            result.routeLineExpressionCommandHolder.provider is LightRouteLineExpressionProvider,
        )
        assertTrue(
            result.trafficLineExpressionCommandHolder.provider is LightRouteLineExpressionProvider,
        )
        assertTrue(
            result.routeLineCasingExpressionCommandHolder.provider
            is LightRouteLineExpressionProvider,
        )
    }

    @Test
    fun getTraveledRouteLineExpressionsWithNonZeroPoint() = coroutineRule.runBlockingTest {
        val expectedTrafficExpressionContents = listOf(
            StringChecker("literal"),
            ListChecker(
                DoubleChecker(0.0),
                DoubleChecker(0.2638437072304186),
            ),
        )
        val expectedRouteLineExpressionContents = listOf(
            StringChecker("literal"),
            ListChecker(
                DoubleChecker(0.0),
                DoubleChecker(0.2638437072304186),
            ),
        )
        val expectedCasingExpressionContents = listOf(
            StringChecker("literal"),
            ListChecker(
                DoubleChecker(0.0),
                DoubleChecker(0.2638437072304186),
            ),
        )

        val route = loadNavigationRoute("short_route.json")
        val lineString = LineString.fromPolyline(
            route.directionsRoute.geometry() ?: "",
            Constants.PRECISION_6,
        )
        val vanishingRouteLine = VanishingRouteLine()
        vanishingRouteLine.upcomingRouteGeometrySegmentIndex = 3

        val result = vanishingRouteLine.getTraveledRouteLineExpressions(
            lineString.coordinates()[1],
            granularDistances = MapboxRouteLineUtils.granularDistancesProvider(route)!!,
        )
        val viewOptions = MapboxRouteLineViewOptions.Builder(ctx)
            .routeLineColorResources(
                RouteLineColorResources.Builder()
                    .routeLineTraveledColor(Color.BLUE)
                    .routeLineTraveledCasingColor(Color.GREEN)
                    .build(),
            )
            .build()
            .toData()

        checkExpression(
            expectedTrafficExpressionContents,
            getAppliedExpression(
                result!!.trafficLineExpressionCommandHolder,
                viewOptions,
                "line-trim-offset",
            ),
        )
        checkExpression(
            expectedRouteLineExpressionContents,
            getAppliedExpression(
                result!!.routeLineExpressionCommandHolder,
                viewOptions,
                "line-trim-offset",
            ),
        )
        checkExpression(
            expectedCasingExpressionContents,
            getAppliedExpression(
                result!!.routeLineCasingExpressionCommandHolder,
                viewOptions,
                "line-trim-offset",
            ),
        )

        assertTrue(
            result.routeLineExpressionCommandHolder.provider is LightRouteLineExpressionProvider,
        )
        assertTrue(
            result.trafficLineExpressionCommandHolder.provider is LightRouteLineExpressionProvider,
        )
        assertTrue(
            result.routeLineCasingExpressionCommandHolder.provider
            is LightRouteLineExpressionProvider,
        )
    }
}
