package com.mapbox.navigation.ui.maps.route.line.api

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.core.constants.Constants
import com.mapbox.geojson.LineString
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.testing.FileUtils.loadJsonFixture
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineUtils
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineColorResources
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineExpressionData
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineResources
import com.mapbox.navigation.ui.maps.route.line.model.VanishingPointState
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class VanishingRouteLineTest {

    @Test
    fun updateVanishingPointState_when_LOCATION_TRACKING() {
        val vanishingRouteLine = VanishingRouteLine().also {
            it.updateVanishingPointState(RouteProgressState.LOCATION_TRACKING)
        }

        assertEquals(VanishingPointState.ENABLED, vanishingRouteLine.vanishingPointState)
    }

    @Test
    fun updateVanishingPointState_when_ROUTE_COMPLETE() {
        val vanishingRouteLine = VanishingRouteLine().also {
            it.updateVanishingPointState(RouteProgressState.ROUTE_COMPLETE)
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
    fun clear() {
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
        val expectedRouteLineExpression = "[step, [line-progress], [rgba, 0.0, 0.0, 2.0, 0.0]," +
            " 0.0, [rgba, 0.0, 0.0, 3.0, 0.0]]"
        val expectedCasingExpression = "[step, [line-progress], [rgba, 0.0, 0.0, 2.0, 0.0]," +
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
                colorResources
            )

        val result = vanishingRouteLine.getTraveledRouteLineExpressions(
            lineString.coordinates()[0],
            segments,
            genericMockResourceProvider
        )

        assertEquals(expectedTrafficExpression, result!!.trafficLineExpression.toString())
        assertEquals(expectedRouteLineExpression, result.routeLineExpression.toString())
        assertEquals(expectedCasingExpression, result.routeLineCasingExpression.toString())
    }

    private fun getRoute(): DirectionsRoute {
        val routeAsJson = loadJsonFixture("short_route.json")
        return DirectionsRoute.fromJson(routeAsJson)
    }

    private val genericMockResourceProvider = mockk<RouteLineResources> {
        every { routeLineColorResources } returns mockk<RouteLineColorResources> {
            every { routeUnknownTrafficColor } returns 1
            every { routeLineTraveledColor } returns 2
            every { routeDefaultColor } returns 3
            every { routeCasingColor } returns 4
            every { routeLowCongestionColor } returns 5
            every { routeModerateColor } returns 6
            every { routeSevereColor } returns 7
            every { routeHeavyColor } returns 8
            every { alternativeRouteUnknownTrafficColor } returns 9
            every { routeLineTraveledCasingColor } returns 10
        }
        every { trafficBackfillRoadClasses } returns listOf()
    }
}
