package com.mapbox.navigation.ui.maps.route.line.api

import android.content.Context
import android.graphics.Color
import android.util.SparseArray
import androidx.test.core.app.ApplicationProvider
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.bindgen.Expected
import com.mapbox.core.constants.Constants
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.geojson.utils.PolylineUtils
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.QueriedFeature
import com.mapbox.maps.QueryFeaturesCallback
import com.mapbox.maps.RenderedQueryOptions
import com.mapbox.maps.ScreenBox
import com.mapbox.maps.ScreenCoordinate
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.testing.FileUtils.loadJsonFixture
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.base.model.route.RouteLayerConstants.ALTERNATIVE_ROUTE1_LAYER_ID
import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineApiExtensions.clearRouteLine
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineApiExtensions.findClosestRoute
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineApiExtensions.getRouteDrawData
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineApiExtensions.setRoutes
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineUtils.parseRoutePoints
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLine
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineError
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineGranularDistances
import com.mapbox.navigation.ui.maps.route.line.model.RouteSetValue
import com.mapbox.navigation.ui.maps.route.line.model.RouteStyleDescriptor
import com.mapbox.navigation.ui.maps.route.line.model.VanishingPointState
import com.mapbox.navigation.utils.internal.JobControl
import com.mapbox.navigation.utils.internal.ThreadController
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class MapboxRouteLineApiTest {

    lateinit var ctx: Context

    @get:Rule
    var coroutineRule = MainCoroutineRule()
    private val parentJob = SupervisorJob()
    private val testScope = CoroutineScope(parentJob + coroutineRule.testDispatcher)

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
        mockkObject(ThreadController)
        every { ThreadController.getMainScopeAndRootJob() } returns JobControl(parentJob, testScope)
        every { ThreadController.IODispatcher } returns coroutineRule.testDispatcher
    }

    @After
    fun cleanUp() {
        unmockkObject(ThreadController)
    }

    @Test
    fun getPrimaryRoute() = coroutineRule.runBlockingTest {
        val options = MapboxRouteLineOptions.Builder(ctx).build()
        val route = getRoute()
        val api = MapboxRouteLineApi(options).also {
            it.setRoutes(listOf(RouteLine(route, null)))
        }

        val result = api.getPrimaryRoute()

        assertEquals(route, result)
    }

    @Test
    fun getVanishPointOffset() {
        val options = MapboxRouteLineOptions.Builder(ctx)
            .withVanishingRouteLineEnabled(true)
            .build()
        options.vanishingRouteLine!!.vanishPointOffset = 99.9

        val result = MapboxRouteLineApi(options).getVanishPointOffset()

        assertEquals(99.9, result, 0.0)
    }

    @Test
    fun getRoutes() = coroutineRule.runBlockingTest {
        val options = MapboxRouteLineOptions.Builder(ctx).build()
        val route = getRoute()
        val routes = listOf(RouteLine(route, null))

        val api = MapboxRouteLineApi(options)
        api.setRoutes(routes)

        val result = api.getRoutes()

        assertEquals(result.size, routes.size)
        assertEquals(result[0], routes[0].route)
    }

    @Test
    fun setRoutes_clearsVanishingRouteLine() = coroutineRule.runBlockingTest {
        val vanishingRouteLine = mockk<VanishingRouteLine>(relaxed = true)
        val realOptions = MapboxRouteLineOptions.Builder(ctx).build()
        val options = mockk<MapboxRouteLineOptions>()
        every { options.routeLayerProvider } returns realOptions.routeLayerProvider
        every { options.resourceProvider } returns realOptions.resourceProvider
        every { options.vanishingRouteLine } returns vanishingRouteLine

        val api = MapboxRouteLineApi(options)
        val route = getRoute()
        val routes = listOf(RouteLine(route, null))

        api.setRoutes(routes)

        verify { vanishingRouteLine.clear() }
    }

    @Test
    fun setRoutes_notClearsVanishingRouteLine_WhenSamePrimaryRoute() =
        coroutineRule.runBlockingTest {
            val vanishingRouteLine = mockk<VanishingRouteLine>(relaxed = true)
            val realOptions = MapboxRouteLineOptions.Builder(ctx).build()
            val options = mockk<MapboxRouteLineOptions>()
            every { options.routeLayerProvider } returns realOptions.routeLayerProvider
            every { options.resourceProvider } returns realOptions.resourceProvider
            every { options.vanishingRouteLine } returns vanishingRouteLine

            val api = MapboxRouteLineApi(options)
            val route = getRoute()
            val routes = listOf(RouteLine(route, null))

            api.setRoutes(routes)
            api.setRoutes(routes)

            verify(exactly = 1) { vanishingRouteLine.clear() }
        }

    @Test
    fun setRoutes_notClearsVanishingRouteLine_WhenSamePrimaryRoute_differentAnnotations() =
        coroutineRule.runBlockingTest {
            val vanishingRouteLine = mockk<VanishingRouteLine>(relaxed = true)
            val realOptions = MapboxRouteLineOptions.Builder(ctx).build()
            val options = mockk<MapboxRouteLineOptions>()
            every { options.routeLayerProvider } returns realOptions.routeLayerProvider
            every { options.resourceProvider } returns realOptions.resourceProvider
            every { options.vanishingRouteLine } returns vanishingRouteLine

            val api = MapboxRouteLineApi(options)
            val route = getRoute()
            val routeAsJson = route.toJson().replace("unknown", "severe")
            val sameRouteDifferentAnnotations = DirectionsRoute.fromJson(routeAsJson)

            api.setRoutes(listOf(RouteLine(route, null)))
            api.setRoutes(listOf(RouteLine(sameRouteDifferentAnnotations, null)))

            verify(exactly = 1) { vanishingRouteLine.clear() }
        }

    @Test
    fun setRoutes_setsVanishPointToZero() = coroutineRule.runBlockingTest {
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
    fun setRoutes_doesNotResetVanishingPointWhenSameRoute() = coroutineRule.runBlockingTest {
        val options = MapboxRouteLineOptions.Builder(ctx)
            .withVanishingRouteLineEnabled(true)
            .build()
        options.vanishingRouteLine!!.vanishPointOffset = 99.9

        val api = MapboxRouteLineApi(options)
        val route = getRoute()
        val routes = listOf(RouteLine(route, null))
        api.setRoutes(routes)
        api.setVanishingOffset(25.0)

        api.setRoutes(routes)

        assertEquals(25.0, api.getVanishPointOffset(), 0.0)
        assertEquals(25.0, options.vanishingRouteLine!!.vanishPointOffset, 0.0)
    }

    @Test
    fun setRoutes() = coroutineRule.runBlockingTest {
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

        assertEquals(expectedCasingExpression, result.value!!.casingLineExpression.toString())
        assertEquals(expectedRouteLineExpression, result.value!!.routeLineExpression.toString())
        assertEquals(
            expectedTrafficLineExpression,
            result.value!!.trafficLineExpression.toString()
        )
        assertEquals(
            expectedPrimaryRouteSourceGeometry,
            result.value!!.primaryRouteSource.features()!![0].geometry().toString()
        )
        assertTrue(result.value!!.alternativeRoute1Source.features()!!.isEmpty())
        assertTrue(result.value!!.alternativeRoute2Source.features()!!.isEmpty())
        assertEquals(
            expectedWaypointFeature0,
            result.value!!.waypointsSource.features()!![0].geometry().toString()
        )
        assertEquals(
            expectedWaypointFeature1,
            result.value!!.waypointsSource.features()!![1].geometry().toString()
        )
    }

    @Test
    fun setRoutesNoAlternativeRouteDuplicates() = coroutineRule.runBlockingTest {
        val options = MapboxRouteLineOptions.Builder(ctx).build()
        val api = MapboxRouteLineApi(options)
        val routes = listOf(
            RouteLine(getRoute(), null),
            RouteLine(getMultilegRoute(), null),
            RouteLine(getRouteWithRoadClasses(), null)
        )

        val result = api.setRoutes(routes)

        assertNotEquals(
            result.value!!.alternativeRoute1Source,
            result.value!!.alternativeRoute2Source
        )
        assertNotEquals(
            result.value!!.altRoute1TrafficExpression,
            result.value!!.altRoute2TrafficExpression
        )
    }

    @Test
    fun setRoutesWithCallback() {
        var callbackCalled = false
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
        val consumer = MapboxNavigationConsumer<Expected<RouteLineError, RouteSetValue>> { t ->
            callbackCalled = true
            val result = t
            assertEquals(expectedCasingExpression, result.value!!.casingLineExpression.toString())
            assertEquals(
                expectedRouteLineExpression,
                result.value!!.routeLineExpression.toString()
            )
            assertEquals(
                expectedTrafficLineExpression,
                result.value!!.trafficLineExpression.toString()
            )
            assertEquals(
                expectedPrimaryRouteSourceGeometry,
                result.value!!.primaryRouteSource.features()!![0].geometry().toString()
            )
            assertTrue(result.value!!.alternativeRoute1Source.features()!!.isEmpty())
            assertTrue(result.value!!.alternativeRoute2Source.features()!!.isEmpty())
            assertEquals(
                expectedWaypointFeature0,
                result.value!!.waypointsSource.features()!![0].geometry().toString()
            )
            assertEquals(
                expectedWaypointFeature1,
                result.value!!.waypointsSource.features()!![1].geometry().toString()
            )
        }

        api.setRoutes(routes, consumer)

        assertTrue(callbackCalled)
    }

    @Test
    fun setRoutesTrafficExpressionsWithAlternativeRoutes() = coroutineRule.runBlockingTest {
        val expectedPrimaryTrafficLineExpression = "[step, [line-progress], " +
            "[rgba, 0.0, 0.0, 0.0, 0.0], 0.0, [rgba, 86.0, 168.0, 251.0, 1.0], " +
            "0.9429639111009005, [rgba, 255.0, 149.0, 0.0, 1.0]]"
        val expectedAlternative1TrafficLineExpression = "[step, [line-progress], " +
            "[rgba, 0.0, 0.0, 0.0, 0.0], 0.0, [rgba, 134.0, 148.0, 165.0, 1.0], " +
            "0.4277038222190263, [rgba, 190.0, 160.0, 135.0, 1.0], 0.49556716073574053, " +
            "[rgba, 134.0, 148.0, 165.0, 1.0]]"
        val expectedAlternative2TrafficLineExpression = "[step, [line-progress], [rgba, 0.0, 0.0," +
            " 0.0, 0.0], 0.0, [rgba, 134.0, 148.0, 165.0, 1.0], 0.09121273901463474," +
            " [rgba, 190.0, 160.0, 135.0, 1.0], 0.09968837805505427, [rgba, 134.0, 148.0, 165.0," +
            " 1.0], 0.7454688534239129, [rgba, 190.0, 160.0, 135.0, 1.0], 0.7559006329335118," +
            " [rgba, 134.0, 148.0, 165.0, 1.0], 0.7936706837675, [rgba, 190.0, 160.0, 135.0, " +
            "1.0], 0.8197911935445008, [rgba, 134.0, 148.0, 165.0, 1.0], 0.8673188270983855, " +
            "[rgba, 190.0, 160.0, 135.0, 1.0], 0.8687452801370679, [rgba, 134.0, 148.0, 165.0, " +
            "1.0], 0.8906448359225819, [rgba, 181.0, 130.0, 129.0, 1.0], 0.930119274506571, " +
            "[rgba, 134.0, 148.0, 165.0, 1.0]]"
        val options = MapboxRouteLineOptions.Builder(ctx).build()
        val api = MapboxRouteLineApi(options)
        val route = getRoute()
        val altRoute1 = getRouteWithRoadClasses()
        val altRoute2 = getMultilegRoute()
        val routes = listOf(
            RouteLine(route, null),
            RouteLine(altRoute1, null),
            RouteLine(altRoute2, null)
        )

        val result = api.setRoutes(routes)

        assertEquals(
            expectedPrimaryTrafficLineExpression,
            result.value!!.trafficLineExpression.toString()
        )
        assertEquals(
            expectedAlternative1TrafficLineExpression,
            result.value!!.altRoute1TrafficExpression.toString()
        )
        assertEquals(
            expectedAlternative2TrafficLineExpression,
            result.value!!.altRoute2TrafficExpression.toString()
        )
    }

    @Test
    fun setRoutesAlternativeRouteColorOverride() = coroutineRule.runBlockingTest {
        val routeStyleDescriptors = listOf(
            RouteStyleDescriptor("alternativeRoute1", Color.YELLOW, Color.CYAN),
            RouteStyleDescriptor("alternativeRoute2", Color.BLUE, Color.GREEN)
        )
        val options = MapboxRouteLineOptions.Builder(ctx)
            .withRouteStyleDescriptors(routeStyleDescriptors)
            .build()
        val api = MapboxRouteLineApi(options)
        val route = getRoute()
        val altRoute1 = getRouteWithRoadClasses()
        val altRoute2 = getMultilegRoute()
        val routes = listOf(
            RouteLine(route, null),
            RouteLine(altRoute1, "alternativeRoute1"),
            RouteLine(altRoute2, "alternativeRoute2")
        )

        val result = api.setRoutes(routes)

        assertEquals(
            "{\"alternativeRoute1\":true}",
            result.value!!.alternativeRoute1Source.features()!!.first().properties().toString()
        )
        assertEquals(
            "{\"alternativeRoute2\":true}",
            result.value!!.alternativeRoute2Source.features()!!.first().properties().toString()
        )
    }

    @Test
    fun setRoutes_notCallsVanishingRouteLine_initWithRoute_whenGranularDistancesNotNull() =
        coroutineRule.runBlockingTest {
            val vanishingRouteLine = mockk<VanishingRouteLine>(relaxed = true) {
                every { primaryRouteLineGranularDistances } returns RouteLineGranularDistances(
                    25.0,
                    SparseArray()
                )
            }
            val realOptions = MapboxRouteLineOptions.Builder(ctx).build()
            val options = mockk<MapboxRouteLineOptions>()
            every { options.routeLayerProvider } returns realOptions.routeLayerProvider
            every { options.resourceProvider } returns realOptions.resourceProvider
            every { options.vanishingRouteLine } returns vanishingRouteLine

            val api = MapboxRouteLineApi(options)
            val route = getRoute()
            val routes = listOf(RouteLine(route, null))

            api.setRoutes(routes)

            verify(exactly = 0) { vanishingRouteLine.initWithRoute(route) }
        }

    @Test
    fun setRoutes_callsVanishingRouteLine_initWithRoute_whenGranularDistancesIsNull() =
        coroutineRule.runBlockingTest {
            val vanishingRouteLine = mockk<VanishingRouteLine>(relaxed = true) {
                every { primaryRouteLineGranularDistances } returns null
            }
            val realOptions = MapboxRouteLineOptions.Builder(ctx).build()
            val options = mockk<MapboxRouteLineOptions>()
            every { options.routeLayerProvider } returns realOptions.routeLayerProvider
            every { options.resourceProvider } returns realOptions.resourceProvider
            every { options.vanishingRouteLine } returns vanishingRouteLine

            val api = MapboxRouteLineApi(options)
            val route = getRoute()
            val routes = listOf(RouteLine(route, null))

            api.setRoutes(routes)

            verify { vanishingRouteLine.initWithRoute(route) }
        }

    @Test
    fun getRouteDrawData() = coroutineRule.runBlockingTest {
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
        api.setRoutes(routes)

        val result = api.getRouteDrawData()

        assertEquals(expectedCasingExpression, result.value!!.casingLineExpression.toString())
        assertEquals(expectedRouteLineExpression, result.value!!.routeLineExpression.toString())
        assertEquals(expectedTrafficLineExpression, result.value!!.trafficLineExpression.toString())
        assertEquals(
            expectedPrimaryRouteSourceGeometry,
            result.value!!.primaryRouteSource.features()!![0].geometry().toString()
        )
        assertTrue(result.value!!.alternativeRoute1Source.features()!!.isEmpty())
        assertTrue(result.value!!.alternativeRoute2Source.features()!!.isEmpty())
        assertEquals(
            expectedWaypointFeature0,
            result.value!!.waypointsSource.features()!![0].geometry().toString()
        )
        assertEquals(
            expectedWaypointFeature1,
            result.value!!.waypointsSource.features()!![1].geometry().toString()
        )
    }

    @Test
    fun updateTraveledRouteLine() = coroutineRule.runBlockingTest {
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

        api.updateVanishingPointState(RouteProgressState.TRACKING)
        api.setRoutes(listOf(RouteLine(route, null)))
        api.updateUpcomingRoutePointIndex(routeProgress)

        val result = api.updateTraveledRouteLine(lineString.coordinates()[1])

        assertEquals(expectedCasingExpression, result.value!!.casingLineExpression.toString())
        assertEquals(expectedRouteExpression, result.value!!.routeLineExpression.toString())
        assertEquals(expectedTrafficExpression, result.value!!.trafficLineExpression.toString())
    }

    @Test
    fun updateUpcomingRoutePointIndex() = coroutineRule.runBlockingTest {
        val realOptions = MapboxRouteLineOptions.Builder(ctx).build()
        val route = getRoute()
        val mockVanishingRouteLine = mockk<VanishingRouteLine>(relaxUnitFun = true) {
            every { primaryRoutePoints } returns parseRoutePoints(route)
            every { vanishPointOffset } returns 0.0
            every { primaryRouteLineGranularDistances } returns null
        }
        val options = mockk<MapboxRouteLineOptions> {
            every { vanishingRouteLine } returns mockVanishingRouteLine
            every { resourceProvider } returns realOptions.resourceProvider
        }
        val api = MapboxRouteLineApi(options)
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
        api.updateVanishingPointState(RouteProgressState.TRACKING)
        api.setRoutes(listOf(RouteLine(route, null)))

        api.updateUpcomingRoutePointIndex(routeProgress)

        verify { mockVanishingRouteLine.primaryRouteRemainingDistancesIndex = 6 }
    }

    @Test
    fun updateUpcomingRoutePointIndex_whenPrimaryRoutePointsIsNull() =
        coroutineRule.runBlockingTest {
            val realOptions = MapboxRouteLineOptions.Builder(ctx).build()
            val route = getRoute()
            val mockVanishingRouteLine = mockk<VanishingRouteLine>(relaxUnitFun = true) {
                every { primaryRoutePoints } returns null
                every { vanishPointOffset } returns 0.0
                every { primaryRouteLineGranularDistances } returns null
            }
            val options = mockk<MapboxRouteLineOptions> {
                every { vanishingRouteLine } returns mockVanishingRouteLine
                every { resourceProvider } returns realOptions.resourceProvider
            }
            val api = MapboxRouteLineApi(options)
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
            api.updateVanishingPointState(RouteProgressState.TRACKING)
            api.setRoutes(listOf(RouteLine(route, null)))

            api.updateUpcomingRoutePointIndex(routeProgress)

            verify { mockVanishingRouteLine.primaryRouteRemainingDistancesIndex = null }
        }

    @Test
    fun updateUpcomingRoutePointIndex_whenCurrentStepProgressIsNull() =
        coroutineRule.runBlockingTest {
            val realOptions = MapboxRouteLineOptions.Builder(ctx).build()
            val route = getRoute()
            val mockVanishingRouteLine = mockk<VanishingRouteLine>(relaxUnitFun = true) {
                every { primaryRoutePoints } returns parseRoutePoints(route)
                every { vanishPointOffset } returns 0.0
                every { primaryRouteLineGranularDistances } returns null
            }
            val options = mockk<MapboxRouteLineOptions> {
                every { vanishingRouteLine } returns mockVanishingRouteLine
                every { resourceProvider } returns realOptions.resourceProvider
            }
            val api = MapboxRouteLineApi(options)
            val routeProgress = mockk<RouteProgress> {
                every { currentLegProgress } returns mockk {
                    every { legIndex } returns 0
                    every { currentStepProgress } returns null
                }
            }
            api.updateVanishingPointState(RouteProgressState.TRACKING)
            api.setRoutes(listOf(RouteLine(route, null)))

            api.updateUpcomingRoutePointIndex(routeProgress)

            verify { mockVanishingRouteLine.primaryRouteRemainingDistancesIndex = null }
        }

    @Test
    fun updateUpcomingRoutePointIndex_whenCurrentLegProgressIsNull() =
        coroutineRule.runBlockingTest {
            val realOptions = MapboxRouteLineOptions.Builder(ctx).build()
            val route = getRoute()
            val mockVanishingRouteLine = mockk<VanishingRouteLine>(relaxUnitFun = true) {
                every { primaryRoutePoints } returns parseRoutePoints(route)
                every { vanishPointOffset } returns 0.0
                every { primaryRouteLineGranularDistances } returns null
            }
            val options = mockk<MapboxRouteLineOptions> {
                every { vanishingRouteLine } returns mockVanishingRouteLine
                every { resourceProvider } returns realOptions.resourceProvider
            }
            val api = MapboxRouteLineApi(options)
            val routeProgress = mockk<RouteProgress> {
                every { currentLegProgress } returns null
            }
            api.updateVanishingPointState(RouteProgressState.TRACKING)
            api.setRoutes(listOf(RouteLine(route, null)))

            api.updateUpcomingRoutePointIndex(routeProgress)

            verify { mockVanishingRouteLine.primaryRouteRemainingDistancesIndex = null }
        }

    @Test
    fun updateWithRouteProgress() = coroutineRule.runBlockingTest {
        val realOptions = MapboxRouteLineOptions.Builder(ctx).build()
        val route = getRoute()
        val mockVanishingRouteLine = mockk<VanishingRouteLine>(relaxUnitFun = true) {
            every { primaryRoutePoints } returns null
            every { vanishPointOffset } returns 0.0
            every { primaryRouteLineGranularDistances } returns null
        }
        val options = mockk<MapboxRouteLineOptions> {
            every { vanishingRouteLine } returns mockVanishingRouteLine
            every { resourceProvider } returns realOptions.resourceProvider
        }
        val api = MapboxRouteLineApi(options)
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
            every { currentState } returns RouteProgressState.TRACKING
        }
        api.updateVanishingPointState(RouteProgressState.TRACKING)
        api.setRoutes(listOf(RouteLine(route, null)))

        api.updateWithRouteProgress(routeProgress)

        verify { mockVanishingRouteLine.primaryRouteRemainingDistancesIndex = null }
        verify {
            mockVanishingRouteLine.updateVanishingPointState(RouteProgressState.TRACKING)
        }
    }

    @Test
    fun updateTraveledRouteLineWhenVanishingRouteLineInhibited() {
        val options = MapboxRouteLineOptions.Builder(ctx)
            .withVanishingRouteLineEnabled(true)
            .build()
        val api = MapboxRouteLineApi(options)

        val result = api.updateTraveledRouteLine(Point.fromLngLat(-122.4727051, 37.7577627))

        assertNotNull(result.error)
    }

    @Test
    fun updateTraveledRouteLineWhenPointOffRouteLine() = coroutineRule.runBlockingTest {
        val options = MapboxRouteLineOptions.Builder(ctx)
            .withVanishingRouteLineEnabled(true)
            .build()
        val route = getRoute()
        val api = MapboxRouteLineApi(options)

        api.updateVanishingPointState(RouteProgressState.TRACKING)
        api.setRoutes(listOf(RouteLine(route, null)))

        val result = api.updateTraveledRouteLine(Point.fromLngLat(-122.4727051, 37.7577627))

        assertNotNull(result.error)
    }

    @Test
    fun updateVanishingPointState_When_LOCATION_TRACKING() {
        val options = MapboxRouteLineOptions.Builder(ctx)
            .withVanishingRouteLineEnabled(true)
            .build()

        MapboxRouteLineApi(options).updateVanishingPointState(
            RouteProgressState.TRACKING
        )

        assertEquals(
            VanishingPointState.ENABLED,
            options.vanishingRouteLine!!.vanishingPointState
        )
    }

    @Test
    fun clearRouteLine() = coroutineRule.runBlockingTest {
        val options = MapboxRouteLineOptions.Builder(ctx).build()
        val api = MapboxRouteLineApi(options)

        val result = api.clearRouteLine()

        assertTrue(result.value!!.altRoute1Source.features()!!.isEmpty())
        assertTrue(result.value!!.altRoute2Source.features()!!.isEmpty())
        assertTrue(result.value!!.primaryRouteSource.features()!!.isEmpty())
        assertTrue(result.value!!.waypointsSource.features()!!.isEmpty())
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

        assertEquals(trafficExpression, result.value!!.casingLineExpression.toString())
        assertEquals(routeLineExpression, result.value!!.routeLineExpression.toString())
        assertEquals(casingExpression, result.value!!.casingLineExpression.toString())
    }

    @ExperimentalCoroutinesApi
    @Test
    fun findClosestRoute_whenClickPoint() = runBlockingTest {
        val uuids = listOf(UUID.randomUUID(), UUID.randomUUID())
        mockkStatic(UUID::class)
        every { UUID.randomUUID() } returnsMany uuids
        val feature1 = mockk<QueriedFeature> {
            every { feature.id() } returns uuids[0].toString()
        }
        val feature2 = mockk<QueriedFeature> {
            every { feature.id() } returns uuids[1].toString()
        }
        val route1 = getRoute()
        val route2 = getRoute()
        val options = MapboxRouteLineOptions.Builder(ctx).build()
        val api = MapboxRouteLineApi(options).also {
            it.setRoutes(listOf(RouteLine(route1, null), RouteLine(route2, null)))
        }
        val point = Point.fromLngLat(139.7745686, 35.677573)
        val mockExpected = mockk<com.mapbox.bindgen.Expected<String, List<QueriedFeature>>> {
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

        assertEquals(route2, result.value!!.route)
        unmockkStatic(UUID::class)
    }

    @ExperimentalCoroutinesApi
    @Test
    fun findClosestRoute_whenRectPoint() = runBlockingTest {
        val uuids = listOf(UUID.randomUUID(), UUID.randomUUID())
        mockkStatic(UUID::class)
        every { UUID.randomUUID() } returnsMany uuids
        val feature1 = mockk<QueriedFeature> {
            every { feature.id() } returns uuids[0].toString()
        }
        val feature2 = mockk<QueriedFeature> {
            every { feature.id() } returns uuids[1].toString()
        }
        val route1 = getRoute()
        val route2 = getRoute()
        val options = MapboxRouteLineOptions.Builder(ctx).build()
        val api = MapboxRouteLineApi(options).also {
            it.setRoutes(listOf(RouteLine(route1, null), RouteLine(route2, null)))
        }
        val point = Point.fromLngLat(139.7745686, 35.677573)
        val emptyExpected = mockk<com.mapbox.bindgen.Expected<String, List<QueriedFeature>>> {
            every { value } returns listOf()
        }
        val mockExpected = mockk<com.mapbox.bindgen.Expected<String, List<QueriedFeature>>> {
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

        assertEquals(route2, result.value!!.route)
        unmockkStatic(UUID::class)
    }

    @ExperimentalCoroutinesApi
    @Test
    fun findClosestRoute_whenPrimaryRoute() = runBlockingTest {
        val uuids = listOf(UUID.randomUUID(), UUID.randomUUID())
        mockkStatic(UUID::class)
        every { UUID.randomUUID() } returnsMany uuids
        val feature1 = mockk<QueriedFeature> {
            every { feature.id() } returns uuids[0].toString()
        }
        val feature2 = mockk<QueriedFeature> {
            every { feature.id() } returns uuids[1].toString()
        }
        val route1 = getRoute()
        val route2 = getRoute()
        val options = MapboxRouteLineOptions.Builder(ctx).build()
        val api = MapboxRouteLineApi(options).also {
            it.setRoutes(listOf(RouteLine(route1, null), RouteLine(route2, null)))
        }
        val point = Point.fromLngLat(139.7745686, 35.677573)
        val emptyExpected = mockk<com.mapbox.bindgen.Expected<String, List<QueriedFeature>>> {
            every { value } returns listOf()
        }
        val mockExpected = mockk<com.mapbox.bindgen.Expected<String, List<QueriedFeature>>> {
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

        assertEquals(route1, result.value!!.route)
        unmockkStatic(UUID::class)
    }

    @Test
    fun setRouteAsyncCallsReturnsCorrectRouteSuspend() = coroutineRule.runBlockingTest {
        val shortRoute = listOf(RouteLine(getRoute(), null))
        val longRoute = listOf(RouteLine(getVeryLongRoute(), null))
        val options = MapboxRouteLineOptions.Builder(ctx).build()
        val api = MapboxRouteLineApi(options)

        val longRouteDef = async {
            val result = api.setRoutes(longRoute)
            (result.value!!.trafficLineExpression.contents as ArrayList<*>).size
        }
        delay(40)
        val shortRouteDef = async {
            val result = api.setRoutes(shortRoute)
            (result.value!!.trafficLineExpression.contents as ArrayList<*>).size
        }

        assertEquals(7, shortRouteDef.await())
        assertEquals(625, longRouteDef.await())
    }

    private fun getRoute(): DirectionsRoute {
        val routeAsJson = loadJsonFixture("short_route.json")
        return DirectionsRoute.fromJson(routeAsJson)
    }

    private fun getMultilegRoute(): DirectionsRoute {
        val routeAsJson = loadJsonFixture("multileg_route.json")
        return DirectionsRoute.fromJson(routeAsJson)
    }

    private fun getRouteWithRoadClasses(): DirectionsRoute {
        val routeAsJson = loadJsonFixture("route-with-road-classes.txt")
        return DirectionsRoute.fromJson(routeAsJson)
    }

    private fun getVeryLongRoute(): DirectionsRoute {
        val routeAsJson = loadJsonFixture("cross-country-route.json")
        return DirectionsRoute.fromJson(routeAsJson)
    }

    private fun getRouteWithNoRoadRestrictions(): DirectionsRoute {
        val routeAsJson = loadJsonFixture("another-route-with-restrictions.json")
        return DirectionsRoute.fromJson(routeAsJson)
    }
}
