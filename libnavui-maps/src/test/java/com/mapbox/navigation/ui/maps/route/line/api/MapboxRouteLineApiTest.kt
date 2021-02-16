package com.mapbox.navigation.ui.maps.route.line.api

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.bindgen.Expected
import com.mapbox.core.constants.Constants
import com.mapbox.geojson.Feature
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.geojson.utils.PolylineUtils
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.QueryFeaturesCallback
import com.mapbox.maps.RenderedQueryOptions
import com.mapbox.maps.ScreenBox
import com.mapbox.maps.ScreenCoordinate
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.testing.FileUtils.loadJsonFixture
import com.mapbox.navigation.ui.base.model.route.RouteLayerConstants.ALTERNATIVE_ROUTE1_CASING_LAYER_ID
import com.mapbox.navigation.ui.base.model.route.RouteLayerConstants.ALTERNATIVE_ROUTE1_LAYER_ID
import com.mapbox.navigation.ui.base.model.route.RouteLayerConstants.ALTERNATIVE_ROUTE1_TRAFFIC_LAYER_ID
import com.mapbox.navigation.ui.base.model.route.RouteLayerConstants.ALTERNATIVE_ROUTE2_CASING_LAYER_ID
import com.mapbox.navigation.ui.base.model.route.RouteLayerConstants.ALTERNATIVE_ROUTE2_LAYER_ID
import com.mapbox.navigation.ui.base.model.route.RouteLayerConstants.ALTERNATIVE_ROUTE2_TRAFFIC_LAYER_ID
import com.mapbox.navigation.ui.base.model.route.RouteLayerConstants.PRIMARY_ROUTE_CASING_LAYER_ID
import com.mapbox.navigation.ui.base.model.route.RouteLayerConstants.PRIMARY_ROUTE_LAYER_ID
import com.mapbox.navigation.ui.base.model.route.RouteLayerConstants.PRIMARY_ROUTE_TRAFFIC_LAYER_ID
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLine
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineState
import com.mapbox.navigation.ui.maps.route.line.model.VanishingPointState
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class MapboxRouteLineApiTest {

    lateinit var ctx: Context

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun getPrimaryRoute() {
        val options = MapboxRouteLineOptions.Builder(ctx).build()
        val route = getRoute()
        val api = MapboxRouteLineApi(options).also {
            it.setRoutes(listOf(RouteLine(route, null)))
        }

        val result = api.getPrimaryRoute()

        assertEquals(route, result)
    }

    @Test
    fun getHidePrimaryRouteState() {
        val options = MapboxRouteLineOptions.Builder(ctx).build()
        val api = MapboxRouteLineApi(options)

        val result = api.hidePrimaryRoute()

        assertEquals(3, result.getLayerVisibilityChanges().size)
        assertEquals(result.getLayerVisibilityChanges()[0].first, PRIMARY_ROUTE_TRAFFIC_LAYER_ID)
        assertEquals(result.getLayerVisibilityChanges()[0].second, Visibility.NONE)
        assertEquals(result.getLayerVisibilityChanges()[1].first, PRIMARY_ROUTE_LAYER_ID)
        assertEquals(result.getLayerVisibilityChanges()[1].second, Visibility.NONE)
        assertEquals(result.getLayerVisibilityChanges()[2].first, PRIMARY_ROUTE_CASING_LAYER_ID)
        assertEquals(result.getLayerVisibilityChanges()[2].second, Visibility.NONE)
    }

    @Test
    fun getShowPrimaryRouteState() {
        val options = MapboxRouteLineOptions.Builder(ctx).build()
        val api = MapboxRouteLineApi(options)

        val result = api.showPrimaryRoute()

        assertEquals(3, result.getLayerVisibilityChanges().size)
        assertEquals(result.getLayerVisibilityChanges()[0].first, PRIMARY_ROUTE_TRAFFIC_LAYER_ID)
        assertEquals(result.getLayerVisibilityChanges()[0].second, Visibility.VISIBLE)
        assertEquals(result.getLayerVisibilityChanges()[1].first, PRIMARY_ROUTE_LAYER_ID)
        assertEquals(result.getLayerVisibilityChanges()[1].second, Visibility.VISIBLE)
        assertEquals(result.getLayerVisibilityChanges()[2].first, PRIMARY_ROUTE_CASING_LAYER_ID)
        assertEquals(result.getLayerVisibilityChanges()[2].second, Visibility.VISIBLE)
    }

    @Test
    fun getHideAlternativeRoutesState() {
        val options = MapboxRouteLineOptions.Builder(ctx).build()
        val api = MapboxRouteLineApi(options)

        val result = api.hideAlternativeRoutes()

        assertEquals(6, result.getLayerVisibilityChanges().size)
        assertEquals(result.getLayerVisibilityChanges()[0].first, ALTERNATIVE_ROUTE1_LAYER_ID)
        assertEquals(result.getLayerVisibilityChanges()[0].second, Visibility.NONE)
        assertEquals(
            result.getLayerVisibilityChanges()[1].first,
            ALTERNATIVE_ROUTE1_CASING_LAYER_ID
        )
        assertEquals(result.getLayerVisibilityChanges()[1].second, Visibility.NONE)
        assertEquals(result.getLayerVisibilityChanges()[2].first, ALTERNATIVE_ROUTE2_LAYER_ID)
        assertEquals(result.getLayerVisibilityChanges()[2].second, Visibility.NONE)
        assertEquals(
            result.getLayerVisibilityChanges()[3].first,
            ALTERNATIVE_ROUTE2_CASING_LAYER_ID
        )
        assertEquals(result.getLayerVisibilityChanges()[3].second, Visibility.NONE)
        assertEquals(
            result.getLayerVisibilityChanges()[4].first,
            ALTERNATIVE_ROUTE1_TRAFFIC_LAYER_ID
        )
        assertEquals(result.getLayerVisibilityChanges()[4].second, Visibility.NONE)
        assertEquals(
            result.getLayerVisibilityChanges()[5].first,
            ALTERNATIVE_ROUTE2_TRAFFIC_LAYER_ID
        )
        assertEquals(result.getLayerVisibilityChanges()[5].second, Visibility.NONE)
    }

    @Test
    fun getShowAlternativeRoutesState() {
        val options = MapboxRouteLineOptions.Builder(ctx).build()
        val api = MapboxRouteLineApi(options)

        val result = api.showAlternativeRoutes()

        assertEquals(6, result.getLayerVisibilityChanges().size)
        assertEquals(result.getLayerVisibilityChanges()[0].first, ALTERNATIVE_ROUTE1_LAYER_ID)
        assertEquals(result.getLayerVisibilityChanges()[0].second, Visibility.VISIBLE)
        assertEquals(
            result.getLayerVisibilityChanges()[1].first,
            ALTERNATIVE_ROUTE1_CASING_LAYER_ID
        )
        assertEquals(result.getLayerVisibilityChanges()[1].second, Visibility.VISIBLE)
        assertEquals(result.getLayerVisibilityChanges()[2].first, ALTERNATIVE_ROUTE2_LAYER_ID)
        assertEquals(result.getLayerVisibilityChanges()[2].second, Visibility.VISIBLE)
        assertEquals(
            result.getLayerVisibilityChanges()[3].first,
            ALTERNATIVE_ROUTE2_CASING_LAYER_ID
        )
        assertEquals(result.getLayerVisibilityChanges()[3].second, Visibility.VISIBLE)
        assertEquals(
            result.getLayerVisibilityChanges()[4].first,
            ALTERNATIVE_ROUTE1_TRAFFIC_LAYER_ID
        )
        assertEquals(result.getLayerVisibilityChanges()[4].second, Visibility.VISIBLE)
        assertEquals(
            result.getLayerVisibilityChanges()[5].first,
            ALTERNATIVE_ROUTE2_TRAFFIC_LAYER_ID
        )
        assertEquals(result.getLayerVisibilityChanges()[5].second, Visibility.VISIBLE)
    }

    @Test // todo needs more testing
    fun getUpdatePrimaryRouteIndexStateSetsPrimaryRoute() {
        val route1 = getRoute()
        val route2 = getRoute()
        val options = MapboxRouteLineOptions.Builder(ctx).build()
        val api = MapboxRouteLineApi(options).also {
            it.setRoutes(listOf(RouteLine(route1, null), RouteLine(route2, null)))
        }

        api.updateToPrimaryRoute(route2)

        assertEquals(route2, api.getPrimaryRoute())
    }

    @Test
    fun setRoutes_setsVanishPointToZero() {
        val options = MapboxRouteLineOptions.Builder(ctx)
            .withVanishingRouteLineEnabled(true)
            .build()
        options.vanishingRouteLine!!.vanishPointOffset = 99.9

        val api = MapboxRouteLineApi(options)
        val route = getRoute()
        val routes = listOf(RouteLine(route, null))
        api.setRoutes(routes)

        val result = api.getVanishPointOffset()

        assertEquals(0.0, result, 0.0)
        assertEquals(0.0, options.vanishingRouteLine!!.vanishPointOffset, 0.0)
    }

    @Test
    fun setRoutes() {
        val options = MapboxRouteLineOptions.Builder(ctx).build()
        val api = MapboxRouteLineApi(options)
        val expectedCasingExpression = "[step, [line-progress], [rgba, 0.0, 0.0, 0.0, 0.0], 0.0," +
            " [rgba, 47.0, 122.0, 198.0, 1.0]]"
        val expectedRouteLineExpression = "[step, [line-progress], [rgba, 0.0, 0.0, 0.0, 0.0], " +
            "0.0, [rgba, 86.0, 168.0, 251.0, 1.0]]"
        val expectedTrafficLineExpression = "[step, [line-progress], " +
            "[rgba, 0.0, 0.0, 0.0, 0.0], 0.0, [rgba, 86.0, 168.0, 251.0, 1.0], " +
            "0.9429639111009005, [rgba, 255.0, 149.0, 0.0, 1.0]]"
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
        val routes = listOf(RouteLine(route, null))

        val result = api.setRoutes(routes)

        assertEquals(expectedCasingExpression, result.getCasingLineExpression().toString())
        assertEquals(expectedRouteLineExpression, result.getRouteLineExpression().toString())
        assertEquals(expectedTrafficLineExpression, result.getTrafficLineExpression().toString())
        assertEquals(
            expectedPrimaryRouteSourceGeometry,
            result.getPrimaryRouteSource().features()!![0].geometry().toString()
        )
        assertTrue(result.getAlternativeRoute1Source().features()!!.isEmpty())
        assertTrue(result.getAlternativeRoute2Source().features()!!.isEmpty())
        assertEquals(
            expectedWaypointFeature0,
            result.getOriginAndDestinationPointsSource().features()!![0].geometry().toString()
        )
        assertEquals(
            expectedWaypointFeature1,
            result.getOriginAndDestinationPointsSource().features()!![1].geometry().toString()
        )
    }

    @Test
    fun getTraveledRouteLineUpdate() {
        val options = MapboxRouteLineOptions.Builder(ctx)
            .withVanishingRouteLineEnabled(true)
            .build()
        val api = MapboxRouteLineApi(options)
        val expectedCasingExpression =
            "[step, [line-progress], [rgba, 0.0, 0.0, 0.0, 0.0], 0.3240769449298392, " +
                "[rgba, 47.0, 122.0, 198.0, 1.0]]"
        val expectedRouteExpression =
            "[step, [line-progress], [rgba, 0.0, 0.0, 0.0, 0.0], 0.3240769449298392," +
                " [rgba, 86.0, 168.0, 251.0, 1.0]]"
        val expectedTrafficExpression =
            "[step, [line-progress], [rgba, 0.0, 0.0, 0.0, 0.0], 0.3240769449298392, " +
                "[rgba, 86.0, 168.0, 251.0, 1.0], 0.9429639111009005, " +
                "[rgba, 255.0, 149.0, 0.0, 1.0]]"
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

        api.updateVanishingPointState(RouteProgressState.LOCATION_TRACKING)
        api.setRoutes(listOf(RouteLine(route, null)))
        api.updateUpcomingRoutePointIndex(routeProgress)

        val result = api.updateTraveledRouteLine(lineString.coordinates()[1])
            as RouteLineState.VanishingRouteLineUpdateState

        assertEquals(expectedCasingExpression, result.getCasingLineExpression().toString())
        assertEquals(expectedRouteExpression, result.getRouteLineExpression().toString())
        assertEquals(expectedTrafficExpression, result.getTrafficLineExpression().toString())
    }

    @Test
    fun getTraveledRouteLineUpdateWhenVanishingRouteLineInhibited() {
        val options = MapboxRouteLineOptions.Builder(ctx)
            .withVanishingRouteLineEnabled(true)
            .build()
        val api = MapboxRouteLineApi(options)

        val result = api.updateTraveledRouteLine(Point.fromLngLat(-122.4727051, 37.7577627))

        assertNull(result)
    }

    @Test
    fun getTraveledRouteLineUpdateWhenPointOffRouteLine() {
        val options = MapboxRouteLineOptions.Builder(ctx)
            .withVanishingRouteLineEnabled(true)
            .build()
        val route = getRoute()
        val api = MapboxRouteLineApi(options)

        api.updateVanishingPointState(RouteProgressState.LOCATION_TRACKING)
        api.setRoutes(listOf(RouteLine(route, null)))

        val result = api.updateTraveledRouteLine(Point.fromLngLat(-122.4727051, 37.7577627))

        assertNull(result)
    }

    @Test
    fun updateVanishingPointState_When_LOCATION_TRACKING() {
        val options = MapboxRouteLineOptions.Builder(ctx)
            .withVanishingRouteLineEnabled(true)
            .build()

        MapboxRouteLineApi(options).updateVanishingPointState(
            RouteProgressState.LOCATION_TRACKING
        )

        assertEquals(
            VanishingPointState.ENABLED,
            options.vanishingRouteLine!!.vanishingPointState
        )
    }

    @Test
    fun clearRouteData() {
        val options = MapboxRouteLineOptions.Builder(ctx).build()
        val api = MapboxRouteLineApi(options)

        val result = api.clearRouteLine()

        assertTrue(result.getAlternativeRoute1Source().features()!!.isEmpty())
        assertTrue(result.getAlternativeRoute2Source().features()!!.isEmpty())
        assertTrue(result.getPrimaryRouteSource().features()!!.isEmpty())
        assertTrue(result.getOriginAndDestinationPointsSource().features()!!.isEmpty())
    }

    @Test
    fun setVanishingOffset() {
        val options = MapboxRouteLineOptions.Builder(ctx)
            .withVanishingRouteLineEnabled(true)
            .build()
        val trafficExpression = "[step, [line-progress], [rgba, 0.0, 0.0, 0.0, 0.0], 0.5," +
            " [rgba, 47.0, 122.0, 198.0, 1.0]]"
        val routeLineExpression = "[step, [line-progress], [rgba, 0.0, 0.0, 0.0, 0.0], 0.5," +
            " [rgba, 86.0, 168.0, 251.0, 1.0]]"
        val casingExpression = "[step, [line-progress], [rgba, 0.0, 0.0, 0.0, 0.0], 0.5," +
            " [rgba, 47.0, 122.0, 198.0, 1.0]]"

        val api = MapboxRouteLineApi(
            options
        )

        val result = api.setVanishingOffset(.5)

        assertEquals(trafficExpression, result!!.getCasingLineExpression().toString())
        assertEquals(routeLineExpression, result.getRouteLineExpression().toString())
        assertEquals(casingExpression, result.getCasingLineExpression().toString())
    }

    @ExperimentalCoroutinesApi
    @Test
    fun findClosestRoute_whenClickPoint() = runBlockingTest {
        val uuids = listOf(UUID.randomUUID(), UUID.randomUUID())
        mockkStatic(UUID::class)
        every { UUID.randomUUID() } returnsMany uuids
        val feature1 = mockk<Feature> {
            every { id() } returns uuids[0].toString()
        }
        val feature2 = mockk<Feature> {
            every { id() } returns uuids[1].toString()
        }
        val route1 = getRoute()
        val route2 = getRoute()
        val options = MapboxRouteLineOptions.Builder(ctx).build()
        val api = MapboxRouteLineApi(options).also {
            it.setRoutes(listOf(RouteLine(route1, null), RouteLine(route2, null)))
        }
        val point = Point.fromLngLat(139.7745686, 35.677573)
        val mockExpected = mockk<Expected<List<Feature>, String>> {
            every { value } returns listOf(feature2, feature1)
        }
        val querySlot = slot<QueryFeaturesCallback>()
        val screenCoordinate = mockk<ScreenCoordinate> {
            every { x } returns 100.0
            every { y } returns 100.0
        }
        val mockkMap = mockk<MapboxMap>(relaxed = true) {
            every { pixelForCoordinate(point) } returns screenCoordinate
            every {
                queryRenderedFeatures(any<ScreenCoordinate>(), any(), capture(querySlot))
            } answers { querySlot.captured.run(mockExpected) }
        }

        val result = api.findClosestRoute(point, mockkMap, 50f)

        assertEquals(1, result.getRouteIndex())
        unmockkStatic(UUID::class)
    }

    @ExperimentalCoroutinesApi
    @Test
    fun findClosestRoute_whenRectPoint() = runBlockingTest {
        val uuids = listOf(UUID.randomUUID(), UUID.randomUUID())
        mockkStatic(UUID::class)
        every { UUID.randomUUID() } returnsMany uuids
        val feature1 = mockk<Feature> {
            every { id() } returns uuids[0].toString()
        }
        val feature2 = mockk<Feature> {
            every { id() } returns uuids[1].toString()
        }
        val route1 = getRoute()
        val route2 = getRoute()
        val options = MapboxRouteLineOptions.Builder(ctx).build()
        val api = MapboxRouteLineApi(options).also {
            it.setRoutes(listOf(RouteLine(route1, null), RouteLine(route2, null)))
        }
        val point = Point.fromLngLat(139.7745686, 35.677573)
        val emptyExpected = mockk<Expected<List<Feature>, String>> {
            every { value } returns listOf()
        }
        val mockExpected = mockk<Expected<List<Feature>, String>> {
            every { value } returns listOf(feature2, feature1)
        }
        val querySlot = slot<QueryFeaturesCallback>()
        val screenCoordinate = mockk<ScreenCoordinate> {
            every { x } returns 100.0
            every { y } returns 100.0
        }
        val mockkMap = mockk<MapboxMap>(relaxed = true) {
            every { pixelForCoordinate(point) } returns screenCoordinate
            every {
                queryRenderedFeatures(any<ScreenCoordinate>(), any(), capture(querySlot))
            } answers { querySlot.captured.run(emptyExpected) }
            every { queryRenderedFeatures(any<ScreenBox>(), any(), capture(querySlot)) } answers {
                querySlot.captured.run(mockExpected)
            }
        }

        val result = api.findClosestRoute(point, mockkMap, 50f)

        assertEquals(1, result.getRouteIndex())
        unmockkStatic(UUID::class)
    }

    @ExperimentalCoroutinesApi
    @Test
    fun findClosestRoute_whenPrimaryRoute() = runBlockingTest {
        val uuids = listOf(UUID.randomUUID(), UUID.randomUUID())
        mockkStatic(UUID::class)
        every { UUID.randomUUID() } returnsMany uuids
        val feature1 = mockk<Feature> {
            every { id() } returns uuids[0].toString()
        }
        val feature2 = mockk<Feature> {
            every { id() } returns uuids[1].toString()
        }
        val route1 = getRoute()
        val route2 = getRoute()
        val options = MapboxRouteLineOptions.Builder(ctx).build()
        val api = MapboxRouteLineApi(options).also {
            it.setRoutes(listOf(RouteLine(route1, null), RouteLine(route2, null)))
        }
        val point = Point.fromLngLat(139.7745686, 35.677573)
        val emptyExpected = mockk<Expected<List<Feature>, String>> {
            every { value } returns listOf()
        }
        val mockExpected = mockk<Expected<List<Feature>, String>> {
            every { value } returns listOf(feature1, feature2)
        }
        val querySlot = slot<QueryFeaturesCallback>()
        val renderedQueryOptionsSlot = slot<RenderedQueryOptions>()
        val screenCoordinate = mockk<ScreenCoordinate> {
            every { x } returns 100.0
            every { y } returns 100.0
        }
        val mockkMap = mockk<MapboxMap>(relaxed = true) {
            every { pixelForCoordinate(point) } returns screenCoordinate
            every {
                queryRenderedFeatures(
                    any<ScreenCoordinate>(),
                    capture(renderedQueryOptionsSlot),
                    capture(querySlot)
                )
            } answers {
                if (
                    renderedQueryOptionsSlot.captured.layerIds!!
                        .contains(ALTERNATIVE_ROUTE1_LAYER_ID)
                ) {
                    querySlot.captured.run(emptyExpected)
                } else {
                    querySlot.captured.run(mockExpected)
                }
            }
            every { queryRenderedFeatures(any<ScreenBox>(), any(), capture(querySlot)) } answers {
                querySlot.captured.run(emptyExpected)
            }
        }

        val result = api.findClosestRoute(point, mockkMap, 50f)

        assertEquals(0, result.getRouteIndex())
        unmockkStatic(UUID::class)
    }

    private fun getRoute(): DirectionsRoute {
        val routeAsJson = loadJsonFixture("short_route.json")
        return DirectionsRoute.fromJson(routeAsJson)
    }
}
