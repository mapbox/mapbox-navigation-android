package com.mapbox.navigation.ui.maps.route.routeline.internal

import android.os.Build
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.core.constants.Constants
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.geojson.utils.PolylineUtils
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.ui.internal.route.RouteConstants.ALTERNATIVE_ROUTE_CASING_LAYER_ID
import com.mapbox.navigation.ui.internal.route.RouteConstants.ALTERNATIVE_ROUTE_LAYER_ID
import com.mapbox.navigation.ui.internal.route.RouteConstants.PRIMARY_ROUTE_CASING_LAYER_ID
import com.mapbox.navigation.ui.internal.route.RouteConstants.PRIMARY_ROUTE_LAYER_ID
import com.mapbox.navigation.ui.internal.route.RouteConstants.PRIMARY_ROUTE_TRAFFIC_LAYER_ID
import com.mapbox.navigation.ui.internal.route.RouteConstants.WAYPOINT_LAYER_ID
import com.mapbox.navigation.ui.maps.route.routeline.api.RouteLineResourceProvider
import com.mapbox.navigation.ui.maps.route.routeline.model.IdentifiableRoute
import com.mapbox.navigation.ui.maps.route.routeline.model.RouteLineState
import com.mapbox.navigation.ui.maps.route.routeline.model.VanishingPointState
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Scanner

@Config(sdk = [Build.VERSION_CODES.O_MR1])
@RunWith(RobolectricTestRunner::class)
class MapboxRouteControllerLineActionsTest {

    @Test
    fun getPrimaryRoute() {
        val route = getRoute()
        val actions = MapboxRouteLineActions(genericMockResourceProvider).also {
            it.updateVanishingPointState(RouteProgressState.LOCATION_TRACKING)
            it.getDrawRoutesState(listOf(route))
        }

        val result = actions.getPrimaryRoute()

        assertEquals(route, result)
    }

    @Test
    fun getHidePrimaryRouteState() {
        val routeResourceProvider = mockk<RouteLineResourceProvider>()

        val result = MapboxRouteLineActions(routeResourceProvider).getHidePrimaryRouteState()

        assertEquals(result.getLayerVisibilityChanges()[0].first, PRIMARY_ROUTE_TRAFFIC_LAYER_ID)
        assertEquals(result.getLayerVisibilityChanges()[0].second, Visibility.NONE)
        assertEquals(result.getLayerVisibilityChanges()[1].first, PRIMARY_ROUTE_LAYER_ID)
        assertEquals(result.getLayerVisibilityChanges()[1].second, Visibility.NONE)
        assertEquals(result.getLayerVisibilityChanges()[2].first, PRIMARY_ROUTE_CASING_LAYER_ID)
        assertEquals(result.getLayerVisibilityChanges()[2].second, Visibility.NONE)
    }

    @Test
    fun getShowPrimaryRouteState() {
        val routeResourceProvider = mockk<RouteLineResourceProvider>()

        val result = MapboxRouteLineActions(routeResourceProvider).getShowPrimaryRouteState()

        assertEquals(result.getLayerVisibilityChanges()[0].first, PRIMARY_ROUTE_TRAFFIC_LAYER_ID)
        assertEquals(result.getLayerVisibilityChanges()[0].second, Visibility.VISIBLE)
        assertEquals(result.getLayerVisibilityChanges()[1].first, PRIMARY_ROUTE_LAYER_ID)
        assertEquals(result.getLayerVisibilityChanges()[1].second, Visibility.VISIBLE)
        assertEquals(result.getLayerVisibilityChanges()[2].first, PRIMARY_ROUTE_CASING_LAYER_ID)
        assertEquals(result.getLayerVisibilityChanges()[2].second, Visibility.VISIBLE)
    }

    @Test
    fun getHideAlternativeRoutesState() {
        val routeResourceProvider = mockk<RouteLineResourceProvider>()

        val result = MapboxRouteLineActions(routeResourceProvider).getHideAlternativeRoutesState()

        assertEquals(result.getLayerVisibilityChanges()[0].first, ALTERNATIVE_ROUTE_LAYER_ID)
        assertEquals(result.getLayerVisibilityChanges()[0].second, Visibility.NONE)
        assertEquals(result.getLayerVisibilityChanges()[1].first, ALTERNATIVE_ROUTE_CASING_LAYER_ID)
        assertEquals(result.getLayerVisibilityChanges()[1].second, Visibility.NONE)
    }

    @Test
    fun getShowAlternativeRoutesState() {
        val routeResourceProvider = mockk<RouteLineResourceProvider>()

        val result = MapboxRouteLineActions(routeResourceProvider).getShowAlternativeRoutesState()

        assertEquals(result.getLayerVisibilityChanges()[0].first, ALTERNATIVE_ROUTE_LAYER_ID)
        assertEquals(result.getLayerVisibilityChanges()[0].second, Visibility.VISIBLE)
        assertEquals(result.getLayerVisibilityChanges()[1].first, ALTERNATIVE_ROUTE_CASING_LAYER_ID)
        assertEquals(result.getLayerVisibilityChanges()[1].second, Visibility.VISIBLE)
    }

    @Test
    fun getHideOriginAndDestinationPointsState() {
        val routeResourceProvider = mockk<RouteLineResourceProvider>()

        val result =
            MapboxRouteLineActions(routeResourceProvider).getHideOriginAndDestinationPointsState()

        assertEquals(result.getLayerVisibilityChanges()[0].first, WAYPOINT_LAYER_ID)
        assertEquals(result.getLayerVisibilityChanges()[0].second, Visibility.NONE)
    }

    @Test
    fun getShowOriginAndDestinationPointsState() {
        val routeResourceProvider = mockk<RouteLineResourceProvider>()

        val result =
            MapboxRouteLineActions(routeResourceProvider).getShowOriginAndDestinationPointsState()

        assertEquals(result.getLayerVisibilityChanges()[0].first, WAYPOINT_LAYER_ID)
        assertEquals(result.getLayerVisibilityChanges()[0].second, Visibility.VISIBLE)
    }

    @Test // todo needs more testing
    fun getUpdatePrimaryRouteIndexStateSetsPrimaryRoute() {
        val route = getRoute()
        val actions = MapboxRouteLineActions(genericMockResourceProvider).also {
            it.getUpdatePrimaryRouteIndexState(route)
        }

        assertEquals(route, actions.getPrimaryRoute())
    }

    @Test
    fun redraw() {
        val expectedCasingExpression = "[step, [line-progress], [rgba, 0.0, 0.0, 2.0, 0.0], 0.0, " +
            "[rgba, 0.0, 0.0, 4.0, 0.0]]"
        val expectedRouteLineExpression = "[step, [line-progress], [rgba, 0.0, 0.0, 2.0, 0.0], " +
            "0.0, [rgba, 0.0, 0.0, 3.0, 0.0]]"
        val expectedTrafficLineExpression = "[step, [line-progress], " +
            "[rgba, 0.0, 0.0, 0.0, 0.0], 0.0, " +
            "[rgba, 0.0, 0.0, 1.0, 0.0], 0.9429639111009005, " +
            "[rgba, 0.0, 0.0, 6.0, 0.0]]"
        val expectedPrimaryRouteSourceGeometry = "LineString{type=LineString, bbox=null, " +
            "coordinates=[Point{type=Point, bbox=null, coordinates=[-122.523671, 37.975379]}," +
            " Point{type=Point, bbox=null, coordinates=[-122.523729, 37.975194]}, " +
            "Point{type=Point, bbox=null, coordinates=[-122.523579, 37.975173]}, " +
            "Point{type=Point, bbox=null, coordinates=[-122.523117, 37.975107]}, " +
            "Point{type=Point, bbox=null, coordinates=[-122.523131, 37.975067]}]}"
        val expectedWaypointFeature0 =
            "Point{type=Point, bbox=null, coordinates=[-122.523671, 37.975379]}"
        val expectedWaypointFeature1 =
            "Point{type=Point, bbox=null, coordinates=[-122.523131, 37.975067]}"
        val route = getRoute()
        val routes = listOf(route)

        val actions = MapboxRouteLineActions(genericMockResourceProvider)
        actions.getDrawRoutesState(routes)

        val result = actions.redraw()

        assertEquals(expectedCasingExpression, result.getCasingLineExpression().toString())
        assertEquals(expectedRouteLineExpression, result.getRouteLineExpression().toString())
        assertEquals(expectedTrafficLineExpression, result.getTrafficLineExpression().toString())
        assertEquals(
            expectedPrimaryRouteSourceGeometry,
            result.getPrimaryRouteSource().features()!![0].geometry().toString()
        )
        assertTrue(result.getAlternateRoutesSource().features()!!.isEmpty())
        assertEquals(
            expectedWaypointFeature0,
            result.getWaypointsSource().features()!![0].geometry().toString()
        )
        assertEquals(
            expectedWaypointFeature1,
            result.getWaypointsSource().features()!![1].geometry().toString()
        )
    }

    @Test
    fun getDrawRoutesStateSetsVanishPointToZero() {
        val route = getRoute()
        val routes = listOf(route)
        val actions = MapboxRouteLineActions(genericMockResourceProvider)
        actions.setVanishingOffset(.5)
        actions.getDrawRoutesState(routes)

        val result = actions.getVanishPointOffset()

        assertEquals(0.0, result, 0.0)
    }

    @Test
    fun getDrawRoutesState() {
        val expectedCasingExpression = "[step, [line-progress], [rgba, 0.0, 0.0, 2.0, 0.0], 0.0, " +
            "[rgba, 0.0, 0.0, 4.0, 0.0]]"
        val expectedRouteLineExpression = "[step, [line-progress], [rgba, 0.0, 0.0, 2.0, 0.0], " +
            "0.0, [rgba, 0.0, 0.0, 3.0, 0.0]]"
        val expectedTrafficLineExpression = "[step, [line-progress], " +
            "[rgba, 0.0, 0.0, 0.0, 0.0], 0.0, " +
            "[rgba, 0.0, 0.0, 1.0, 0.0], 0.9429639111009005, " +
            "[rgba, 0.0, 0.0, 6.0, 0.0]]"
        val expectedPrimaryRouteSourceGeometry = "LineString{type=LineString, bbox=null, " +
            "coordinates=[Point{type=Point, bbox=null, coordinates=[-122.523671, 37.975379]}," +
            " Point{type=Point, bbox=null, coordinates=[-122.523729, 37.975194]}, " +
            "Point{type=Point, bbox=null, coordinates=[-122.523579, 37.975173]}, " +
            "Point{type=Point, bbox=null, coordinates=[-122.523117, 37.975107]}, " +
            "Point{type=Point, bbox=null, coordinates=[-122.523131, 37.975067]}]}"
        val expectedWaypointFeature0 =
            "Point{type=Point, bbox=null, coordinates=[-122.523671, 37.975379]}"
        val expectedWaypointFeature1 =
            "Point{type=Point, bbox=null, coordinates=[-122.523131, 37.975067]}"
        val route = getRoute()
        val routes = listOf(route)

        val result = MapboxRouteLineActions(genericMockResourceProvider).getDrawRoutesState(routes)

        assertEquals(expectedCasingExpression, result.getCasingLineExpression().toString())
        assertEquals(expectedRouteLineExpression, result.getRouteLineExpression().toString())
        assertEquals(expectedTrafficLineExpression, result.getTrafficLineExpression().toString())
        assertEquals(
            expectedPrimaryRouteSourceGeometry,
            result.getPrimaryRouteSource().features()!![0].geometry().toString()
        )
        assertTrue(result.getAlternateRoutesSource().features()!!.isEmpty())
        assertEquals(
            expectedWaypointFeature0,
            result.getWaypointsSource().features()!![0].geometry().toString()
        )
        assertEquals(
            expectedWaypointFeature1,
            result.getWaypointsSource().features()!![1].geometry().toString()
        )
    }

    @Test
    fun getDrawIdentifiableRoutesStateSetsVanishPointToZero() {
        val route = getRoute()
        val routes = listOf(IdentifiableRoute(route, ""))
        val actions = MapboxRouteLineActions(genericMockResourceProvider)
        actions.setVanishingOffset(.5)
        actions.getDrawIdentifiableRoutesState(routes)

        val result = actions.getVanishPointOffset()

        assertEquals(0.0, result, 0.0)
    }

    @Test
    fun getDrawIdentifiableRoutesState() {
        val expectedCasingExpression = "[step, [line-progress], [rgba, 0.0, 0.0, 2.0, 0.0], 0.0, " +
            "[rgba, 0.0, 0.0, 4.0, 0.0]]"
        val expectedRouteLineExpression = "[step, [line-progress], [rgba, 0.0, 0.0, 2.0, 0.0], " +
            "0.0, [rgba, 0.0, 0.0, 3.0, 0.0]]"
        val expectedTrafficLineExpression = "[step, [line-progress], " +
            "[rgba, 0.0, 0.0, 0.0, 0.0], 0.0, " +
            "[rgba, 0.0, 0.0, 1.0, 0.0], 0.9429639111009005, " +
            "[rgba, 0.0, 0.0, 6.0, 0.0]]"
        val expectedPrimaryRouteSourceGeometry = "LineString{type=LineString, bbox=null, " +
            "coordinates=[Point{type=Point, bbox=null, coordinates=[-122.523671, 37.975379]}," +
            " Point{type=Point, bbox=null, coordinates=[-122.523729, 37.975194]}, " +
            "Point{type=Point, bbox=null, coordinates=[-122.523579, 37.975173]}, " +
            "Point{type=Point, bbox=null, coordinates=[-122.523117, 37.975107]}, " +
            "Point{type=Point, bbox=null, coordinates=[-122.523131, 37.975067]}]}"
        val expectedWaypointFeature0 =
            "Point{type=Point, bbox=null, coordinates=[-122.523671, 37.975379]}"
        val expectedWaypointFeature1 =
            "Point{type=Point, bbox=null, coordinates=[-122.523131, 37.975067]}"
        val route = getRoute()
        val routes = listOf(IdentifiableRoute(route, ""))

        val result =
            MapboxRouteLineActions(genericMockResourceProvider).getDrawIdentifiableRoutesState(
                routes
            )

        assertEquals(expectedCasingExpression, result.getCasingLineExpression().toString())
        assertEquals(expectedRouteLineExpression, result.getRouteLineExpression().toString())
        assertEquals(expectedTrafficLineExpression, result.getTrafficLineExpression().toString())
        assertEquals(
            expectedPrimaryRouteSourceGeometry,
            result.getPrimaryRouteSource().features()!![0].geometry().toString()
        )
        assertTrue(result.getAlternateRoutesSource().features()!!.isEmpty())
        assertEquals(
            expectedWaypointFeature0,
            result.getWaypointsSource().features()!![0].geometry().toString()
        )
        assertEquals(
            expectedWaypointFeature1,
            result.getWaypointsSource().features()!![1].geometry().toString()
        )
    }

    @Test
    fun getTraveledRouteLineUpdate() {
        val expectedCasingExpression =
            "[step, [line-progress], [rgba, 0.0, 0.0, 2.0, 0.0], 0.3240769449298392, " +
                "[rgba, 0.0, 0.0, 4.0, 0.0]]"
        val expectedRouteExpression =
            "[step, [line-progress], [rgba, 0.0, 0.0, 2.0, 0.0], 0.3240769449298392, " +
                "[rgba, 0.0, 0.0, 3.0, 0.0]]"
        val expectedTrafficExpression =
            "[step, [line-progress], [rgba, 0.0, 0.0, 0.0, 0.0], 0.3240769449298392, " +
                "[rgba, 0.0, 0.0, 1.0, 0.0], 0.9429639111009005, [rgba, 0.0, 0.0, 6.0, 0.0]]"
        val route = getRoute()
        val lineString = LineString.fromPolyline(route.geometry() ?: "", Constants.PRECISION_6)
        val routeProgress = mockk<RouteProgress> {
            every { currentLegProgress } returns mockk {
                every { legIndex } returns 0
                every { currentStepProgress } returns mockk {
                    every { stepPoints } returns PolylineUtils.decode(
                        route.legs()!![0].steps()!![2].geometry()!!,
                        6
                    )
                    every { distanceTraveled } returns 0f
                    every { step } returns mockk {
                        every { distance() } returns route.legs()!![0].steps()!![2].distance()
                    }
                    every { stepIndex } returns 2
                }
            }
        }

        val actions = MapboxRouteLineActions(genericMockResourceProvider).also {
            it.updateVanishingPointState(RouteProgressState.LOCATION_TRACKING)
        }
        actions.getDrawRoutesState(listOf(route))
        actions.updateUpcomingRoutePointIndex(routeProgress)

        val result =
            actions.getTraveledRouteLineUpdate(lineString.coordinates()[1])
                as RouteLineState.TraveledRouteLineUpdateState.TraveledRouteLineUpdate

        assertEquals(expectedCasingExpression, result.getCasingLineExpression().toString())
        assertEquals(expectedRouteExpression, result.getRouteLineExpression().toString())
        assertEquals(expectedTrafficExpression, result.getTrafficExpression().toString())
    }

    @Test
    fun getTraveledRouteLineUpdateWhenVanishingRouteLineInhibited() {
        val actions = MapboxRouteLineActions(genericMockResourceProvider)

        val result = actions.getTraveledRouteLineUpdate(Point.fromLngLat(-122.4727051, 37.7577627))

        assertTrue(result is RouteLineState.TraveledRouteLineUpdateState.TraveledRouteLineNoUpdate)
    }

    @Test
    fun getTraveledRouteLineUpdateWhenPointOffRouteLine() {
        val route = getRoute()
        val actions = MapboxRouteLineActions(genericMockResourceProvider).also {
            it.updateVanishingPointState(RouteProgressState.LOCATION_TRACKING)
            it.getDrawRoutesState(listOf(route))
        }
        val result = actions.getTraveledRouteLineUpdate(Point.fromLngLat(-122.4727051, 37.7577627))

        assertTrue(result is RouteLineState.TraveledRouteLineUpdateState.TraveledRouteLineNoUpdate)
    }

    @Test
    fun updateVanishingPointState_When_LOCATION_TRACKING() {
        val result = MapboxRouteLineActions(genericMockResourceProvider).updateVanishingPointState(
            RouteProgressState.LOCATION_TRACKING
        )

        assertEquals(VanishingPointState.ENABLED, result.getVanishingPointState())
    }

    @Test
    fun updateVanishingPointState_When_ROUTE_COMPLETE() {
        val result = MapboxRouteLineActions(genericMockResourceProvider).updateVanishingPointState(
            RouteProgressState.ROUTE_COMPLETE
        )

        assertEquals(VanishingPointState.ONLY_INCREASE_PROGRESS, result.getVanishingPointState())
    }

    @Test
    fun updateVanishingPointState_When_other() {
        val result = MapboxRouteLineActions(genericMockResourceProvider).updateVanishingPointState(
            RouteProgressState.OFF_ROUTE
        )

        assertEquals(VanishingPointState.DISABLED, result.getVanishingPointState())
    }

    @Test
    fun updateVanishingPointWhenLineCoordinatesIsLessThanTwoPoints() {
        val routeProgress = mockk<RouteProgress> {
            every { currentLegProgress } returns null
        }
        val actions = MapboxRouteLineActions(genericMockResourceProvider).also {
            it.updateVanishingPointState(RouteProgressState.LOCATION_TRACKING)
            it.updateUpcomingRoutePointIndex(routeProgress)
        }

        val result = actions.getTraveledRouteLineUpdate(Point.fromLngLat(-122.4727051, 37.7577627))

        assertTrue(result is RouteLineState.TraveledRouteLineUpdateState.TraveledRouteLineNoUpdate)
    }

    @Test
    fun clearRouteData() {
        val actions = MapboxRouteLineActions(genericMockResourceProvider)

        val result = actions.clearRouteData()

        assertTrue(result.getAlternateRoutesSource().features()!!.isEmpty())
        assertTrue(result.getPrimaryRouteSource().features()!!.isEmpty())
        assertTrue(result.getWaypointsSource().features()!!.isEmpty())
    }

    private fun getRoute(): DirectionsRoute {
        val routeAsJson = loadJsonFixture("short_route.json")
        return DirectionsRoute.fromJson(routeAsJson)
    }

    private fun loadJsonFixture(filename: String): String? {
        val classLoader = javaClass.classLoader
        val inputStream = classLoader?.getResourceAsStream(filename)
        val scanner = Scanner(inputStream, "UTF-8").useDelimiter("\\A")
        return if (scanner.hasNext()) scanner.next() else ""
    }

    private val genericMockResourceProvider = mockk<RouteLineResourceProvider> {
        every { getRouteUnknownTrafficColor() } returns 1
        every { getRouteLineTraveledColor() } returns 2
        every { getRouteLineBaseColor() } returns 3
        every { getRouteLineCasingColor() } returns 4
        every { getRouteLowTrafficColor() } returns 5
        every { getRouteModerateTrafficColor() } returns 6
        every { getRouteSevereTrafficColor() } returns 7
        every { getRouteHeavyTrafficColor() } returns 8
        every { getTrafficBackfillRoadClasses() } returns listOf()
    }
}
