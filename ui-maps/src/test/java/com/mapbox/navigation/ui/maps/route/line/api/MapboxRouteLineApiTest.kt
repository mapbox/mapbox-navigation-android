@file:OptIn(ExperimentalMapboxNavigationAPI::class)

package com.mapbox.navigation.ui.maps.route.line.api

import android.content.Context
import android.graphics.Color
import androidx.appcompat.content.res.AppCompatResources
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.bindgen.Value
import com.mapbox.core.constants.Constants
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.geojson.utils.PolylineUtils
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.RenderedQueryGeometry
import com.mapbox.maps.RenderedQueryOptions
import com.mapbox.maps.ScreenCoordinate
import com.mapbox.maps.Style
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.internal.route.update
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.core.internal.LowMemoryManager
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.testing.NativeRouteParserRule
import com.mapbox.navigation.testing.factories.createNavigationRoute
import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineUtils
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineUtils.layerGroup1SourceLayerIds
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineUtils.layerGroup2SourceLayerIds
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineUtils.layerGroup3SourceLayerIds
import com.mapbox.navigation.ui.maps.internal.route.line.toData
import com.mapbox.navigation.ui.maps.route.line.MapboxRouteLineApiExtensions.clearRouteLine
import com.mapbox.navigation.ui.maps.route.line.MapboxRouteLineApiExtensions.findClosestRoute
import com.mapbox.navigation.ui.maps.route.line.MapboxRouteLineApiExtensions.setNavigationRouteLines
import com.mapbox.navigation.ui.maps.route.line.MapboxRouteLineApiExtensions.setNavigationRoutes
import com.mapbox.navigation.ui.maps.route.line.MapboxRouteLineApiExtensions.updateWithRouteProgress
import com.mapbox.navigation.ui.maps.route.line.RouteLineHistoryRecordingApiSender
import com.mapbox.navigation.ui.maps.route.line.model.ClosestRouteValue
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineApiOptions
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineViewOptions
import com.mapbox.navigation.ui.maps.route.line.model.NavigationRouteLine
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineColorResources
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineError
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineUpdateValue
import com.mapbox.navigation.ui.maps.route.line.model.RouteNotFound
import com.mapbox.navigation.ui.maps.testing.TestRoute
import com.mapbox.navigation.ui.maps.testing.TestingUtil.loadNavigationRoute
import com.mapbox.navigation.utils.internal.InternalJobControlFactory
import com.mapbox.navigation.utils.internal.JobControl
import com.mapbox.navigation.utils.internal.LoggerFrontend
import io.mockk.MockKAnnotations
import io.mockk.clearAllMocks
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.spyk
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.pauseDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@OptIn(ExperimentalCoroutinesApi::class)
class MapboxRouteLineApiTest {

    private val logger = mockk<LoggerFrontend>(relaxed = true)

    @get:Rule
    val loggerRule = LoggingFrontendTestRule(logger)

    @get:Rule
    val nativeRouteParserRule = NativeRouteParserRule()

    @get:Rule
    val coroutineRule = MainCoroutineRule()
    private val parentJob = SupervisorJob()
    private val testScope = CoroutineScope(parentJob + coroutineRule.testDispatcher)
    private val calculationsScope = coroutineRule.createTestScope()
    private val ctx = mockk<Context>()

    private val shortRoute by lazy { TestRoute(fileName = "short_route.json") }
    private val multiLegRouteTwoLegs by lazy {
        TestRoute(fileName = "multileg-route-two-legs.json")
    }
    private val multilegRouteWithOverlap by lazy {
        TestRoute(fileName = "multileg_route_with_overlap.json")
    }
    private val defaultApiOptions = MapboxRouteLineApiOptions.Builder().build()
    private val vanishingRouteLine = mockk<VanishingRouteLine>(relaxed = true)
    private val sender = mockk<RouteLineHistoryRecordingApiSender>(relaxed = true)

    private val lowMemoryManager = mockk<LowMemoryManager>(relaxed = true)
    private val lowMemoryObserverSlot = slot<LowMemoryManager.Observer>()

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        mockkObject(InternalJobControlFactory)
        every {
            InternalJobControlFactory.createDefaultScopeJobControl()
        } returns JobControl(parentJob, testScope)
        every {
            InternalJobControlFactory.createImmediateMainScopeJobControl()
        } returns JobControl(parentJob, testScope)
        mockkStatic(AppCompatResources::class)
        every { AppCompatResources.getDrawable(any(), any()) } returns mockk()

        every { lowMemoryManager.addObserver(capture(lowMemoryObserverSlot)) } returns Unit

        mockkObject(LowMemoryManager.Companion)
        every { LowMemoryManager.create() } returns mockk(relaxed = true)
    }

    @After
    fun cleanUp() {
        unmockkStatic(AppCompatResources::class)
        unmockkObject(InternalJobControlFactory)
        unmockkObject(LowMemoryManager.Companion)
    }

    private fun createRouteLineApi(
        options: MapboxRouteLineApiOptions = mockRouteOptions(),
        calculationsScope: CoroutineScope = this.calculationsScope,
        vanishingRouteLine: VanishingRouteLine? = this.vanishingRouteLine,
        sender: RouteLineHistoryRecordingApiSender = this.sender,
        lowMemoryManager: LowMemoryManager = this.lowMemoryManager,
    ) = MapboxRouteLineApi(
        options,
        calculationsScope,
        vanishingRouteLine,
        sender,
        lowMemoryManager,
    )

    @Test
    fun getPrimaryRoute() = coroutineRule.runBlockingTest {
        val route = loadNavigationRoute("short_route.json", "abc")
        val api = MapboxRouteLineApi(defaultApiOptions).also {
            it.setNavigationRouteLines(listOf(NavigationRouteLine(route, null)))
        }

        val result = api.getPrimaryNavigationRoute()

        assertEquals(
            route,
            result,
        )
    }

    @Test
    fun getVanishPointOffset() {
        val options = MapboxRouteLineApiOptions.Builder()
            .vanishingRouteLineEnabled(true)
            .build()
        every { vanishingRouteLine.vanishPointOffset } returns 99.9

        val result = createRouteLineApi(options).getVanishPointOffset()

        assertEquals(
            99.9,
            result,
            0.0,
        )
    }

    @Test
    fun getRoutes() = coroutineRule.runBlockingTest {
        val options = MapboxRouteLineApiOptions.Builder().build()
        val route = loadNavigationRoute("short_route.json", uuid = "abc")
        val routes = listOf(route)

        val api = MapboxRouteLineApi(options)
        api.setNavigationRoutes(routes)

        val result = api.getNavigationRoutes()

        assertEquals(
            result.size,
            routes.size,
        )
        assertEquals(
            result[0],
            routes[0],
        )
    }

    @Test
    fun setRoutes_whenRouteCoordinatesAreEmpty() = coroutineRule.runBlockingTest {
        val options = MapboxRouteLineApiOptions.Builder().build()
        val api = MapboxRouteLineApi(options)
        val route = loadNavigationRoute("short_route.json")
        val augmentedLineString =
            LineString.fromJson("{\"type\":\"LineString\",\"coordinates\":[]}")
                .toPolyline(Constants.PRECISION_6)
        val augmentedRouteJson = route.directionsRoute.toJson()
            .replace("etylgAl`guhFpJrBh@kHbC{[nAZ", augmentedLineString)

        val augmentedRoute = createNavigationRoute(
            directionsRoute = DirectionsRoute.fromJson(augmentedRouteJson),
        )

        val result = api.setNavigationRouteLines(
            listOf(NavigationRouteLine(augmentedRoute, null)),
        )

        assertNotNull(result.error)
        assertNull(result.value)
    }

    @Test
    fun setRoutes_setsVanishPointToZero() = coroutineRule.runBlockingTest {
        val options = MapboxRouteLineApiOptions.Builder()
            .vanishingRouteLineEnabled(true)
            .build()
        every { vanishingRouteLine.vanishPointOffset } returns 99.9

        val api = createRouteLineApi(options)

        val route = loadNavigationRoute("short_route.json")
        val routes = listOf(NavigationRouteLine(route, null))
        api.setNavigationRouteLines(routes)

        verify { vanishingRouteLine.vanishPointOffset = 0.0 }
    }

    @Test
    fun setRoutesNoAlternativeRouteDuplicates() = coroutineRule.runBlockingTest {
        val options = MapboxRouteLineApiOptions.Builder().build()
        val api = MapboxRouteLineApi(options)
        val routes = listOf(
            NavigationRouteLine(loadNavigationRoute("short_route.json"), null),
            NavigationRouteLine(loadNavigationRoute("multileg_route.json"), null),
            NavigationRouteLine(loadNavigationRoute("route-with-road-classes.txt"), null),
        )

        val result = api.setNavigationRouteLines(routes)

        assertNotEquals(
            result.value!!.alternativeRouteLinesData[0],
            result.value!!.alternativeRouteLinesData[1],
        )
    }

    @Test
    fun setRoutes_trimOffsetValueFromVanishingRouteLine() =
        coroutineRule.runBlockingTest {
            every { vanishingRouteLine.vanishPointOffset } returns 9.9
            val options = MapboxRouteLineApiOptions.Builder()
                .calculateRestrictedRoadSections(false)
                .build()

            val api = createRouteLineApi(options)

            val route = loadNavigationRoute("short_route.json")
            val routes = listOf(NavigationRouteLine(route, null))

            val result = api.setNavigationRouteLines(routes).value!!

            assertEquals(
                9.9,
                result.primaryRouteLineData.dynamicData!!.trimOffset!!.offset,
                0.0,
            )
        }

    @Test
    fun updateUpcomingRoutePointIndex() = coroutineRule.runBlockingTest {
        val route = shortRoute.navigationRoute
        every { vanishingRouteLine.vanishPointOffset } returns 0.0
        val api = createRouteLineApi()
        val routeProgress = shortRoute.mockRouteProgress(stepIndexValue = 2)
        api.updateVanishingPointState(RouteProgressState.TRACKING)
        api.setNavigationRoutes(listOf(route))

        api.updateUpcomingRoutePointIndex(routeProgress)

        verify { vanishingRouteLine.upcomingRouteGeometrySegmentIndex = 4 }
    }

    @Test
    fun updateWithRouteProgress() = coroutineRule.runBlockingTest {
        val route = shortRoute.navigationRoute
        every { vanishingRouteLine.vanishPointOffset } returns 0.0
        val api = createRouteLineApi()
        val routeProgress = shortRoute.mockRouteProgress(stepIndexValue = 2)
        api.setNavigationRoutes(listOf(route))

        api.updateWithRouteProgress(routeProgress) {}

        verify { vanishingRouteLine.upcomingRouteGeometrySegmentIndex = 4 }
        verify {
            vanishingRouteLine.updateVanishingPointState(RouteProgressState.TRACKING)
        }
    }

    @Test
    fun updateWithRouteProgressGetsCancelled() = coroutineRule.runBlockingTest {
        val routeProgressScope = coroutineRule.createTestScope()
        every {
            InternalJobControlFactory.createImmediateMainScopeJobControl()
        } returns JobControl(parentJob, routeProgressScope)
        val route = shortRoute.navigationRoute
        every { vanishingRouteLine.vanishPointOffset } returns 0.0
        val api = createRouteLineApi()
        val routeProgress = shortRoute.mockRouteProgress(stepIndexValue = 2)
        api.setNavigationRoutes(listOf(route))
        val consumer = mockk<
            MapboxNavigationConsumer<Expected<RouteLineError, RouteLineUpdateValue>>,
            >(relaxed = true)

        routeProgressScope.pauseDispatcher {
            api.updateWithRouteProgress(routeProgress, consumer)
            api.updateWithRouteProgress(routeProgress) {}
        }

        val errorCaptor = slot<Expected<RouteLineError, RouteLineUpdateValue>>()
        verify(exactly = 1) { consumer.accept(capture(errorCaptor)) }

        assertEquals(
            "Skipping #updateWithRouteProgress because a newer one is available.",
            errorCaptor.captured.error!!.errorMessage,
        )
    }

    @Test
    fun `updateWithRouteProgress - abort if no primary route`() = coroutineRule.runBlockingTest {
        val route = loadNavigationRoute("short_route.json")
        val options = mockRouteOptions()
        val api = MapboxRouteLineApi(options)
        val routeProgress = mockRouteProgress(route, stepIndexValue = 2)
        val consumer =
            mockk<MapboxNavigationConsumer<Expected<RouteLineError, RouteLineUpdateValue>>>(
                relaxUnitFun = true,
            )

        api.updateWithRouteProgress(routeProgress, consumer)

        val resultSlot = slot<Expected<RouteLineError, RouteLineUpdateValue>>()
        verify { consumer.accept(capture(resultSlot)) }
        assertEquals(
            "You're calling #updateWithRouteProgress without any routes being set.",
            resultSlot.captured.error!!.errorMessage,
        )
    }

    @Test
    fun `updateWithRouteProgress - abort if primary route different`() =
        coroutineRule.runBlockingTest {
            val route1 = loadNavigationRoute("multileg-route-two-legs.json", uuid = "abc")
            val route2 = loadNavigationRoute("short_route.json", uuid = "def")
            every { vanishingRouteLine.vanishPointOffset } returns 0.0
            val api = createRouteLineApi()
            api.setNavigationRoutes(listOf(route1))
            val routeProgress = mockRouteProgress(route2, stepIndexValue = 2)
            val consumer =
                mockk<MapboxNavigationConsumer<Expected<RouteLineError, RouteLineUpdateValue>>>(
                    relaxUnitFun = true,
                )

            api.updateWithRouteProgress(routeProgress, consumer)

            val resultSlot = slot<Expected<RouteLineError, RouteLineUpdateValue>>()
            verify { consumer.accept(capture(resultSlot)) }
            assertEquals(
                "Provided primary route (#setNavigationRoutes, ID: abc#0) and navigated " +
                    "route (#updateWithRouteProgress, ID: def#0) are not the same. " +
                    "Aborting the update.",
                resultSlot.captured.error!!.errorMessage,
            )
        }

    @Test
    fun updateWithRouteProgress_whenDeEmphasizeInactiveLegSegments_vanishingRouteLineDisabled() =
        coroutineRule.runBlockingTest {
            val expectedTrafficExpContents = Value.valueOf(0.5)
            val route = loadNavigationRoute("multileg-route-two-legs.json")
            val options = MapboxRouteLineApiOptions.Builder()
                .vanishingRouteLineEnabled(false)
                .styleInactiveRouteLegsIndependently(true)
                .vanishingRouteLineEnabled(true)
                .build()
            val viewData = MapboxRouteLineViewOptions.Builder(ctx).build().toData()
            val api = MapboxRouteLineApi(options)
            val routeProgress = mockRouteProgress(route)
            api.updateVanishingPointState(RouteProgressState.TRACKING)
            api.setNavigationRoutes(listOf(route))
            api.updateWithRouteProgress(routeProgress) {}

            val result = api.setVanishingOffset(0.5).value!!

            assertEquals(
                expectedTrafficExpContents,
                getAppliedValue(
                    result.primaryRouteLineDynamicData!!.trafficExpressionCommandHolder!!,
                    viewData,
                    "line-trim-start",
                ),
            )
            assertTrue(
                result.primaryRouteLineDynamicData.trafficExpressionCommandHolder!!.provider
                is LightRouteLineValueProvider,
            )
            assertTrue(
                result.primaryRouteLineDynamicData.trafficExpressionCommandHolder.applier
                is LineTrimCommandApplier,
            )
            assertEquals(0, api.activeLegIndex)
        }

    @Test
    fun updateWithRouteProgress_whenDeEmphasizeInactiveLegSegments_setsActiveLegIndex() =
        coroutineRule.runBlockingTest {
            val route = multiLegRouteTwoLegs.navigationRoute
            every { vanishingRouteLine.vanishPointOffset } returns 0.0
            val options = mockRouteOptions()
            every { options.styleInactiveRouteLegsIndependently } returns true
            val api = createRouteLineApi(options)
            val routeProgress = multiLegRouteTwoLegs.mockRouteProgress()
            api.updateVanishingPointState(RouteProgressState.TRACKING)
            api.setNavigationRoutes(listOf(route))

            api.updateWithRouteProgress(routeProgress) {}

            assertEquals(0, api.activeLegIndex)
        }

    @Test
    fun updateTraveledRouteLineWhenVanishingRouteLineInhibited() {
        val options = MapboxRouteLineApiOptions.Builder()
            .vanishingRouteLineEnabled(true)
            .build()
        val api = MapboxRouteLineApi(options)

        val result = api.updateTraveledRouteLine(Point.fromLngLat(-122.4727051, 37.7577627))

        assertNotNull(result.error)
    }

    @Test
    fun updateTraveledRouteLineWhenPointOffRouteLine() = coroutineRule.runBlockingTest {
        val options = MapboxRouteLineApiOptions.Builder()
            .vanishingRouteLineEnabled(true)
            .build()
        val route = loadNavigationRoute("short_route.json")
        val api = MapboxRouteLineApi(options)

        api.updateVanishingPointState(RouteProgressState.TRACKING)
        api.setNavigationRouteLines(listOf(NavigationRouteLine(route, null)))

        val result = api.updateTraveledRouteLine(Point.fromLngLat(-122.4727051, 37.7577627))

        assertNotNull(result.error)
    }

    @Test
    fun updateVanishingPointState_When_LOCATION_TRACKING() {
        val options = MapboxRouteLineApiOptions.Builder()
            .vanishingRouteLineEnabled(true)
            .build()

        createRouteLineApi(options).updateVanishingPointState(RouteProgressState.TRACKING)

        verify { vanishingRouteLine.updateVanishingPointState(RouteProgressState.TRACKING) }
    }

    @Test
    fun clearRouteLine() = coroutineRule.runBlockingTest {
        mockkObject(MapboxRouteLineUtils)
        val options = MapboxRouteLineApiOptions.Builder().build()
        val api = MapboxRouteLineApi(options)

        val result = api.clearRouteLine()

        assertTrue(result.value!!.alternativeRoutesSources.first().features()!!.isEmpty())
        assertTrue(result.value!!.alternativeRoutesSources[1].features()!!.isEmpty())
        assertTrue(result.value!!.primaryRouteSource.features()!!.isEmpty())
        assertTrue(result.value!!.waypointsSource.features()!!.isEmpty())
        verify { MapboxRouteLineUtils.trimRouteDataCacheToSize(size = 0) }
        unmockkObject(MapboxRouteLineUtils)
    }

    @Test
    fun findClosestRoute_success() = runTest {
        mockkObject(MapboxRouteLineUtils, CompositeClosestRouteHandlerProvider)
        val compositeClosestRouteHandler = mockk<CompositeClosestRouteHandler>(relaxed = true)
        every {
            CompositeClosestRouteHandlerProvider.createHandler(any())
        } returns compositeClosestRouteHandler
        val primaryLayers = layerGroup1SourceLayerIds
        val alternativeLayers = layerGroup2SourceLayerIds + layerGroup3SourceLayerIds
        every {
            MapboxRouteLineUtils.getLayerIdsForPrimaryRoute(any<Style>(), any())
        } returns primaryLayers
        val route1 = loadNavigationRoute("short_route.json", uuid = "abc")
        val route2 = loadNavigationRoute("short_route.json", uuid = "def")
        val options = MapboxRouteLineApiOptions.Builder().build()
        val api = MapboxRouteLineApi(options).also {
            it.setNavigationRouteLines(
                listOf(NavigationRouteLine(route1, null), NavigationRouteLine(route2, null)),
            )
        }
        val point = Point.fromLngLat(139.7745686, 35.677573)
        val screenCoordinate = mockk<ScreenCoordinate> {
            every { x } returns 100.0
            every { y } returns 100.0
        }
        val mockkMap = mockk<MapboxMap>(relaxed = true) {
            every { isValid() } returns true
            every { pixelForCoordinate(point) } returns screenCoordinate
        }
        coEvery {
            compositeClosestRouteHandler.handle(mockkMap, screenCoordinate, any())
        } returns ExpectedFactory.createValue(1)

        val result = api.findClosestRoute(point, mockkMap, 50f)

        assertEquals(route2, result.value!!.navigationRoute)
        coVerify {
            compositeClosestRouteHandler.handle(
                mockkMap,
                screenCoordinate,
                match {
                    it.first().features()!!.first().id() == "abc#0" &&
                        it[1].features()!!.first().id() == "def#0"
                },
            )
        }
        verify {
            CompositeClosestRouteHandlerProvider.createHandler(
                match {
                    val handler1 = it[0] as SinglePointClosestRouteHandler
                    val handler2 = it[1] as RectClosestRouteHandler
                    val handler3 = it[2] as SinglePointClosestRouteHandler
                    val handler4 = it[3] as RectClosestRouteHandler
                    it.size == 4 &&
                        handler1.layerIds.toSet() == primaryLayers &&
                        handler2.layerIds.toSet() == primaryLayers &&
                        handler2.padding == 50f &&
                        handler3.layerIds.toSet() == alternativeLayers &&
                        handler4.layerIds.toSet() == alternativeLayers &&
                        handler4.padding == 50f
                },
            )
        }
        unmockkObject(MapboxRouteLineUtils, CompositeClosestRouteHandlerProvider)
    }

    @Test
    fun findClosestRoute_failure() = runTest {
        mockkObject(MapboxRouteLineUtils, CompositeClosestRouteHandlerProvider)
        val compositeClosestRouteHandler = mockk<CompositeClosestRouteHandler>(relaxed = true)
        every {
            CompositeClosestRouteHandlerProvider.createHandler(any())
        } returns compositeClosestRouteHandler
        every {
            MapboxRouteLineUtils.getLayerIdsForPrimaryRoute(any<Style>(), any())
        } returns setOf()
        val route1 = loadNavigationRoute("short_route.json", uuid = "abc")
        val route2 = loadNavigationRoute("short_route.json", uuid = "def")
        val options = MapboxRouteLineApiOptions.Builder().build()
        val api = MapboxRouteLineApi(options).also {
            it.setNavigationRouteLines(
                listOf(NavigationRouteLine(route1, null), NavigationRouteLine(route2, null)),
            )
        }
        val point = Point.fromLngLat(139.7745686, 35.677573)
        val screenCoordinate = mockk<ScreenCoordinate> {
            every { x } returns 100.0
            every { y } returns 100.0
        }
        val mockkMap = mockk<MapboxMap>(relaxed = true) {
            every { isValid() } returns true
            every { pixelForCoordinate(point) } returns screenCoordinate
        }
        coEvery {
            compositeClosestRouteHandler.handle(mockkMap, screenCoordinate, any())
        } returns ExpectedFactory.createError(Unit)

        val result = api.findClosestRoute(point, mockkMap, 50f)

        assertEquals("No route found in query area.", result.error!!.errorMessage)
        unmockkObject(MapboxRouteLineUtils, CompositeClosestRouteHandlerProvider)
    }

    @Test
    fun findClosestRoute_mapIsInvalid() = runTest {
        mockkObject(MapboxRouteLineUtils, CompositeClosestRouteHandlerProvider)
        every { CompositeClosestRouteHandlerProvider.createHandler(any()) } returns mockk()
        every {
            MapboxRouteLineUtils.getLayerIdsForPrimaryRoute(any<Style>(), any())
        } returns setOf()
        val route1 = loadNavigationRoute("short_route.json", uuid = "abc")
        val route2 = loadNavigationRoute("short_route.json", uuid = "def")
        val options = MapboxRouteLineApiOptions.Builder().build()
        val api = MapboxRouteLineApi(options).also {
            it.setNavigationRouteLines(
                listOf(NavigationRouteLine(route1, null), NavigationRouteLine(route2, null)),
            )
        }
        val point = Point.fromLngLat(139.7745686, 35.677573)
        val mockkMap = mockk<MapboxMap>(relaxed = true) {
            every { isValid() } returns false
        }

        val result = api.findClosestRoute(point, mockkMap, 50f)

        assertTrue(result.isError)
        assertEquals("MapboxMap instance is invalid", result.error!!.errorMessage)

        verify(exactly = 0) {
            mockkMap.queryRenderedFeatures(
                any<RenderedQueryGeometry>(),
                any<RenderedQueryOptions>(),
                any(),
            )
        }
        unmockkObject(MapboxRouteLineUtils, CompositeClosestRouteHandlerProvider)
    }

    @Test
    fun findClosestRoute_routeIdsChanged() = coroutineRule.runBlockingTest {
        mockkObject(MapboxRouteLineUtils, CompositeClosestRouteHandlerProvider)
        var continuation: Continuation<Expected<Unit, Int>>? = null
        every { CompositeClosestRouteHandlerProvider.createHandler(any()) } returns mockk {
            coEvery { handle(any(), any(), any()) } coAnswers {
                suspendCoroutine {
                    continuation = it
                }
            }
        }
        every {
            MapboxRouteLineUtils.getLayerIdsForPrimaryRoute(any<Style>(), any())
        } returns setOf()
        val route1 = loadNavigationRoute("short_route.json", uuid = "abc")
        val route2 = loadNavigationRoute("short_route.json", uuid = "def")
        val route3 = loadNavigationRoute("short_route.json", uuid = "abc2")
        val options = MapboxRouteLineApiOptions.Builder().build()
        val api = MapboxRouteLineApi(options).also {
            it.setNavigationRoutes(listOf(route1, route2))
        }
        val point = Point.fromLngLat(139.7745686, 35.677573)
        val mockkMap = mockk<MapboxMap>(relaxed = true) {
            every { isValid() } returns true
        }
        clearAllMocks(answers = false)

        var actual: Expected<RouteNotFound, ClosestRouteValue>? = null
        api.findClosestRoute(point, mockkMap, 50f) {
            actual = it
        }

        api.setNavigationRoutes(listOf(route3, route2)) {}
        continuation!!.resume(ExpectedFactory.createValue(0))

        assertNotNull(actual)
        assertTrue(actual!!.isError)
        assertEquals("Routes have changed", actual!!.error!!.errorMessage)

        unmockkObject(MapboxRouteLineUtils, CompositeClosestRouteHandlerProvider)
    }

    @Test
    fun findClosestRoute_routeAnnotationsChanged() = runTest {
        mockkObject(MapboxRouteLineUtils, CompositeClosestRouteHandlerProvider)
        every { CompositeClosestRouteHandlerProvider.createHandler(any()) } returns mockk {
            coEvery { handle(any(), any(), any()) } returns ExpectedFactory.createValue(0)
        }
        every {
            MapboxRouteLineUtils.getLayerIdsForPrimaryRoute(any<Style>(), any())
        } returns setOf()
        val route1 = loadNavigationRoute("short_route.json", uuid = "abc")
        val route2 = loadNavigationRoute("short_route.json", uuid = "def")
        val route3 = route1.update(
            directionsRouteBlock = {
                toBuilder()
                    .legs(
                        route1.directionsRoute.legs()!!.map { leg ->
                            leg.toBuilder()
                                .annotation(
                                    leg.annotation()!!.toBuilder()
                                        .distance(leg.annotation()!!.distance()!!.map { it + 0.1 })
                                        .build(),
                                )
                                .build()
                        },
                    )
                    .build()
            },
            waypointsBlock = { this },
        )

        val options = MapboxRouteLineApiOptions.Builder().build()
        val api = MapboxRouteLineApi(options).also {
            it.setNavigationRoutes(listOf(route1, route2))
        }
        val point = Point.fromLngLat(139.7745686, 35.677573)
        val mockkMap = mockk<MapboxMap>(relaxed = true) {
            every { isValid() } returns true
        }
        clearAllMocks(answers = false)

        var actual: Expected<RouteNotFound, ClosestRouteValue>? = null
        api.findClosestRoute(point, mockkMap, 50f) {
            actual = it
        }

        api.setNavigationRoutes(listOf(route3, route2)) {}

        assertNotNull(actual)
        assertTrue(actual!!.isValue)
        assertEquals(route1, actual!!.value!!.navigationRoute)

        unmockkObject(MapboxRouteLineUtils, CompositeClosestRouteHandlerProvider)
    }

    @Test
    fun findClosestRoute_doesNotHoldMutexWhileQuerying() = runTest {
        mockkObject(MapboxRouteLineUtils, CompositeClosestRouteHandlerProvider)
        every { CompositeClosestRouteHandlerProvider.createHandler(any()) } returns mockk {
            coEvery { handle(any(), any(), any()) } coAnswers {
                suspendCancellableCoroutine { }
            }
        }
        every {
            MapboxRouteLineUtils.getLayerIdsForPrimaryRoute(any<Style>(), any())
        } returns setOf()
        val route1 = loadNavigationRoute("short_route.json", uuid = "abc")
        val route2 = loadNavigationRoute("short_route.json", uuid = "def")
        val options = MapboxRouteLineApiOptions.Builder().build()
        val api = MapboxRouteLineApi(options).also {
            it.setNavigationRoutes(listOf(route1, route2))
        }
        val point = Point.fromLngLat(139.7745686, 35.677573)
        val mockkMap = mockk<MapboxMap>(relaxed = true) {
            every { isValid() } returns true
        }
        clearAllMocks(answers = false)

        api.findClosestRoute(point, mockkMap, 50f) {}

        // make sure it returns
        api.setNavigationRoutes(listOf(route2, route1))

        unmockkObject(MapboxRouteLineUtils)
    }

    @Test
    fun cancel() {
        val scope = TestScope()
        val job = scope.launch { delay(10000) }
        val jobControl = JobControl(mockk(), scope)
        every { InternalJobControlFactory.createDefaultScopeJobControl() } returns jobControl

        assertTrue(job.isActive)

        MapboxRouteLineApi(MapboxRouteLineApiOptions.Builder().build()).cancel()

        assertTrue(job.isCancelled)
        assertFalse(job.isActive)
    }

    @Test
    fun setRoadClasses() = coroutineRule.runBlockingTest {
        val route = loadNavigationRoute("route-with-road-classes.txt")
        val colors = RouteLineColorResources.Builder()
            .routeLowCongestionColor(5)
            .routeUnknownCongestionColor(1)
            .build()
        val viewData = MapboxRouteLineViewOptions.Builder(ctx)
            .routeLineColorResources(colors)
            .build()
            .toData()
        val options = MapboxRouteLineApiOptions.Builder().build()
        val api = MapboxRouteLineApi(options)
        val defaultResult = api.setNavigationRouteLines(
            listOf(NavigationRouteLine(route, null)),
        ).value!!
        val defaultTrafficExpressionApplier = defaultResult
            .primaryRouteLineData
            .dynamicData!!
            .trafficExpressionCommandHolder!!
        api.setRoadClasses(listOf("service"))
        val result = api.setNavigationRouteLines(
            listOf(NavigationRouteLine(route, null)),
        ).value!!

        val resultExpressionApplier = result
            .primaryRouteLineData
            .dynamicData!!
            .trafficExpressionCommandHolder!!

        val defaultAppliedExpression = getAppliedExpression(
            defaultTrafficExpressionApplier,
            viewData,
            "line-gradient",
        )
        val resultAppliedExpression = getAppliedExpression(
            resultExpressionApplier,
            viewData,
            "line-gradient",
        )
        assertNotEquals(defaultAppliedExpression.toString(), resultAppliedExpression.toString())
    }

    @Test
    fun `setNavigationRouteLines uses distinct routes`() = coroutineRule.runBlockingTest {
        val options = MapboxRouteLineApiOptions.Builder().build()
        val api = MapboxRouteLineApi(options)
        val route1 = loadNavigationRoute("short_route.json", uuid = "abc")
        val route2 = loadNavigationRoute("short_route.json", uuid = "abc")

        val result = api.setNavigationRoutes(listOf(route1, route2))

        result.value!!.alternativeRouteLinesData.forEach {
            assertEquals(FeatureCollection.fromFeatures(listOf()), it.featureCollection)
        }
        verify {
            logger.logW(
                "Routes provided to MapboxRouteLineApi contain duplicates " +
                    "(based on NavigationRoute#id) - using only distinct instances",
                "MapboxRouteLineApi",
            )
        }
    }

    @Test
    fun `setNavigationRouteLines trims data cache`() = coroutineRule.runBlockingTest {
        mockkObject(MapboxRouteLineUtils)
        val options = MapboxRouteLineApiOptions.Builder().build()
        val api = MapboxRouteLineApi(options)
        val route = loadNavigationRoute("short_route.json")

        api.setNavigationRoutes(listOf(route))
        api.setNavigationRoutes(emptyList())

        verifyOrder {
            MapboxRouteLineUtils.trimRouteDataCacheToSize(1)
            MapboxRouteLineUtils.trimRouteDataCacheToSize(0)
        }

        unmockkObject(MapboxRouteLineUtils)
    }

    @Test
    fun `updateWithRouteProgress multileg route when styleInactiveRouteLegsIndependently false and vanishing route line disabled`() =
        coroutineRule.runBlockingTest {
            mockkObject(MapboxRouteLineUtils)
            var callbackCalled = false
            val expectedMaskingTrafficExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 136.0, 136.0, 136.0, 1.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 136.0, 136.0, 136.0, 1.0]"),
                DoubleChecker(0.4852533),
                StringChecker("[rgba, 204.0, 204.0, 204.0, 1.0]"),
                DoubleChecker(0.4868083),
                StringChecker("[rgba, 136.0, 136.0, 136.0, 1.0]"),
                DoubleChecker(0.494403),
                StringChecker("[rgba, 204.0, 204.0, 204.0, 1.0]"),
                DoubleChecker(0.4960591),
                StringChecker("[rgba, 136.0, 136.0, 136.0, 1.0]"),
                DoubleChecker(0.5021971),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            )
            val expectedMaskingBaseExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 0.0, 255.0, 1.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 0.0, 255.0, 1.0]"),
                DoubleChecker(0.5021971),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            )
            val expectedMaskingExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            )
            val colors = RouteLineColorResources.Builder()
                .routeLowCongestionColor(Color.GRAY)
                .routeUnknownCongestionColor(Color.LTGRAY)
                .routeDefaultColor(Color.BLUE)
                .build()
            val options = MapboxRouteLineApiOptions.Builder()
                .styleInactiveRouteLegsIndependently(false)
                .vanishingRouteLineEnabled(false)
                .build()
            val viewData = MapboxRouteLineViewOptions.Builder(ctx)
                .routeLineColorResources(colors)
                .build()
                .toData()
            val api = MapboxRouteLineApi(options)
            val routeProgress = mockRouteProgress(multilegRouteWithOverlap.navigationRoute)
            every { routeProgress.currentLegProgress!!.legIndex } returns 1
            every { routeProgress.currentRouteGeometryIndex } returns 43
            api.setNavigationRoutes(listOf(multilegRouteWithOverlap.navigationRoute))

            api.updateWithRouteProgress(routeProgress) {
                runBlocking {
                    val result = it.value!!
                    val maskingData = result.routeLineMaskingLayerDynamicData!!
                    val trafficMaskingExpression = getAppliedExpression(
                        maskingData.trafficExpressionCommandHolder!!,
                        viewData,
                        "line-gradient",
                    )
                    val baseMaskingExpression = getAppliedExpression(
                        maskingData.baseExpressionCommandHolder,
                        viewData,
                        "line-gradient",
                    )
                    val casingMaskingExpression = getAppliedExpression(
                        maskingData.casingExpressionCommandHolder,
                        viewData,
                        "line-gradient",
                    )
                    val trailMaskingExpression = getAppliedExpression(
                        maskingData.trailExpressionCommandHolder!!,
                        viewData,
                        "line-gradient",
                    )
                    val trailCasingMaskingExpression = getAppliedExpression(
                        maskingData.trailCasingExpressionCommandHolder!!,
                        viewData,
                        "line-gradient",
                    )

                    checkExpression(
                        expectedMaskingTrafficExpressionContents,
                        trafficMaskingExpression,
                    )
                    checkExpression(expectedMaskingBaseExpressionContents, baseMaskingExpression)
                    checkExpression(expectedMaskingExpressionContents, casingMaskingExpression)
                    checkExpression(expectedMaskingExpressionContents, trailMaskingExpression)
                    checkExpression(expectedMaskingExpressionContents, trailCasingMaskingExpression)

                    assertNull(result.primaryRouteLineDynamicData)

                    assertTrue(
                        maskingData.trafficExpressionCommandHolder
                            .provider is LightRouteLineValueProvider,
                    )
                    assertTrue(
                        maskingData.trafficExpressionCommandHolder.applier
                        is LineGradientCommandApplier,
                    )

                    assertTrue(
                        maskingData.baseExpressionCommandHolder.provider
                        is LightRouteLineValueProvider,
                    )
                    assertTrue(
                        maskingData.baseExpressionCommandHolder.applier
                        is LineGradientCommandApplier,
                    )
                    assertTrue(
                        maskingData.casingExpressionCommandHolder.provider
                        is LightRouteLineValueProvider,
                    )
                    assertTrue(
                        maskingData.casingExpressionCommandHolder.applier
                        is LineGradientCommandApplier,
                    )
                    assertTrue(
                        maskingData.trailExpressionCommandHolder.provider
                        is LightRouteLineValueProvider,
                    )
                    assertTrue(
                        maskingData.trafficExpressionCommandHolder.applier
                        is LineGradientCommandApplier,
                    )
                    assertTrue(
                        maskingData.trailCasingExpressionCommandHolder
                            .provider is LightRouteLineValueProvider,
                    )
                    assertTrue(
                        maskingData.trailCasingExpressionCommandHolder
                            .applier is LineGradientCommandApplier,
                    )

                    callbackCalled = true
                }
            }

            assertTrue(callbackCalled)
            unmockkObject(MapboxRouteLineUtils)
        }

    @Test
    fun `updateWithRouteProgress multileg route when styleInactiveRouteLegsIndependently false and vanishing route line enabled`() =
        coroutineRule.runBlockingTest {
            mockkObject(MapboxRouteLineUtils)
            var callbackCalled = false
            val expectedMaskingTrafficExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 136.0, 136.0, 136.0, 1.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 136.0, 136.0, 136.0, 1.0]"),
                DoubleChecker(0.4852533),
                StringChecker("[rgba, 204.0, 204.0, 204.0, 1.0]"),
                DoubleChecker(0.4868083),
                StringChecker("[rgba, 136.0, 136.0, 136.0, 1.0]"),
                DoubleChecker(0.4944029),
                StringChecker("[rgba, 204.0, 204.0, 204.0, 1.0]"),
                DoubleChecker(0.4960591),
                StringChecker("[rgba, 136.0, 136.0, 136.0, 1.0]"),
                DoubleChecker(0.5021971),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            )
            val expectedMaskingBaseExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 0.0, 255.0, 1.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 0.0, 255.0, 1.0]"),
                DoubleChecker(0.5021971),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            )
            val expectedTrailMaskingExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 255.0, 0.0, 0.0, 1.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 255.0, 0.0, 0.0, 1.0]"),
                DoubleChecker(0.5021971),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            )
            val expectedCasingMaskingExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            )
            val expectedTrailCasingMaskingExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 255.0, 0.0, 255.0, 1.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 255.0, 0.0, 255.0, 1.0]"),
                DoubleChecker(0.5021971),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            )
            val colors = RouteLineColorResources.Builder()
                .routeLowCongestionColor(Color.GRAY)
                .routeUnknownCongestionColor(Color.LTGRAY)
                .routeDefaultColor(Color.BLUE)
                .routeLineTraveledColor(Color.RED)
                .routeLineTraveledCasingColor(Color.MAGENTA)
                .build()
            val options = MapboxRouteLineApiOptions.Builder()
                .vanishingRouteLineEnabled(true)
                .build()
            val viewData = MapboxRouteLineViewOptions.Builder(ctx)
                .routeLineColorResources(colors)
                .build()
                .toData()
            val api = MapboxRouteLineApi(options)
            val routeProgress = mockRouteProgress(multilegRouteWithOverlap.navigationRoute)
            every { routeProgress.currentLegProgress!!.legIndex } returns 1
            every { routeProgress.currentRouteGeometryIndex } returns 43
            api.setNavigationRoutes(listOf(multilegRouteWithOverlap.navigationRoute))

            api.updateWithRouteProgress(routeProgress) {
                runBlocking {
                    val result = it.value!!
                    val maskingData = result.routeLineMaskingLayerDynamicData!!
                    val trafficMaskingExpression = getAppliedExpression(
                        maskingData.trafficExpressionCommandHolder!!,
                        viewData,
                        "line-gradient",
                    )
                    val baseMaskingExpression = getAppliedExpression(
                        maskingData.baseExpressionCommandHolder,
                        viewData,
                        "line-gradient",
                    )
                    val casingMaskingExpression = getAppliedExpression(
                        maskingData.casingExpressionCommandHolder,
                        viewData,
                        "line-gradient",
                    )
                    val trailMaskingExpression = getAppliedExpression(
                        maskingData.trailExpressionCommandHolder!!,
                        viewData,
                        "line-gradient",
                    )
                    val trailCasingMaskingExpression = getAppliedExpression(
                        maskingData.trailCasingExpressionCommandHolder!!,
                        viewData,
                        "line-gradient",
                    )

                    checkExpression(
                        expectedMaskingTrafficExpressionContents,
                        trafficMaskingExpression,
                    )
                    checkExpression(expectedMaskingBaseExpressionContents, baseMaskingExpression)
                    checkExpression(
                        expectedCasingMaskingExpressionContents,
                        casingMaskingExpression,
                    )
                    checkExpression(expectedTrailMaskingExpressionContents, trailMaskingExpression)
                    checkExpression(
                        expectedTrailCasingMaskingExpressionContents,
                        trailCasingMaskingExpression,
                    )

                    assertNull(result.primaryRouteLineDynamicData)

                    assertTrue(
                        maskingData.trafficExpressionCommandHolder
                            .provider is LightRouteLineValueProvider,
                    )
                    assertTrue(
                        maskingData.trafficExpressionCommandHolder.applier
                        is LineGradientCommandApplier,
                    )

                    assertTrue(
                        maskingData.baseExpressionCommandHolder.provider
                        is LightRouteLineValueProvider,
                    )
                    assertTrue(
                        maskingData.baseExpressionCommandHolder.applier
                        is LineGradientCommandApplier,
                    )
                    assertTrue(
                        maskingData.casingExpressionCommandHolder.provider
                        is LightRouteLineValueProvider,
                    )
                    assertTrue(
                        maskingData.casingExpressionCommandHolder.applier
                        is LineGradientCommandApplier,
                    )
                    assertTrue(
                        maskingData.trailExpressionCommandHolder.provider
                        is LightRouteLineValueProvider,
                    )
                    assertTrue(
                        maskingData.trafficExpressionCommandHolder.applier
                        is LineGradientCommandApplier,
                    )
                    assertTrue(
                        maskingData.trailCasingExpressionCommandHolder
                            .provider is LightRouteLineValueProvider,
                    )
                    assertTrue(
                        maskingData.trailCasingExpressionCommandHolder
                            .applier is LineGradientCommandApplier,
                    )

                    callbackCalled = true
                }
            }

            assertTrue(callbackCalled)
            unmockkObject(MapboxRouteLineUtils)
        }

    @Test
    fun `updateWithRouteProgress does not produce updates if legs have not changed`() =
        coroutineRule.runBlockingTest {
            mockkObject(MapboxRouteLineUtils)
            var callbackCalled = false
            val options = MapboxRouteLineApiOptions.Builder()
                .vanishingRouteLineEnabled(true)
                .styleInactiveRouteLegsIndependently(true)
                .build()
            val api = MapboxRouteLineApi(options)
            val routeProgress = mockRouteProgress(multilegRouteWithOverlap.navigationRoute)
            every { routeProgress.currentLegProgress!!.legIndex } returns 1
            every { routeProgress.currentRouteGeometryIndex } returns 43
            api.setNavigationRoutes(
                listOf(multilegRouteWithOverlap.navigationRoute),
                activeLegIndex = 1,
            )

            api.updateWithRouteProgress(routeProgress) {
                runBlocking {
                    val result = it.value!!
                    assertNull(result.primaryRouteLineDynamicData)
                    assertNull(result.routeLineMaskingLayerDynamicData)
                    callbackCalled = true
                }
            }
            assertTrue(callbackCalled)

            every { routeProgress.currentRouteGeometryIndex } returns 44
            callbackCalled = false
            api.updateWithRouteProgress(routeProgress) {
                runBlocking {
                    val result = it.value!!
                    assertNull(result.primaryRouteLineDynamicData)
                    assertNull(result.routeLineMaskingLayerDynamicData)
                    callbackCalled = true
                }
            }
            assertTrue(callbackCalled)

            unmockkObject(MapboxRouteLineUtils)
        }

    @Test
    fun getRouteLineDynamicDataForMaskingLayersTest() = coroutineRule.runBlockingTest {
        val expectedTrafficExpressionContents = listOf(
            StringChecker("step"),
            StringChecker("[line-progress]"),
            StringChecker("[rgba, 136.0, 136.0, 136.0, 1.0]"),
            DoubleChecker(0.0),
            StringChecker("[rgba, 136.0, 136.0, 136.0, 1.0]"),
            DoubleChecker(0.4852533),
            StringChecker("[rgba, 204.0, 204.0, 204.0, 1.0]"),
            DoubleChecker(0.4868083),
            StringChecker("[rgba, 136.0, 136.0, 136.0, 1.0]"),
            DoubleChecker(0.494403),
            StringChecker("[rgba, 204.0, 204.0, 204.0, 1.0]"),
            DoubleChecker(0.4960591),
            StringChecker("[rgba, 136.0, 136.0, 136.0, 1.0]"),
            DoubleChecker(0.5021971),
            StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
        )
        val expectedBaseExpressionContents = listOf(
            StringChecker("step"),
            StringChecker("[line-progress]"),
            StringChecker("[rgba, 0.0, 0.0, 255.0, 1.0]"),
            DoubleChecker(0.0),
            StringChecker("[rgba, 0.0, 0.0, 255.0, 1.0]"),
            DoubleChecker(0.5021971),
            StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
        )
        val expectedCasingExpressionContents = listOf(
            StringChecker("step"),
            StringChecker("[line-progress]"),
            StringChecker("[rgba, 0.0, 255.0, 255.0, 1.0]"),
            DoubleChecker(0.0),
            StringChecker("[rgba, 0.0, 255.0, 255.0, 1.0]"),
            DoubleChecker(0.5021971),
            StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
        )
        val expectedTrailExpressionContents = listOf(
            StringChecker("step"),
            StringChecker("[line-progress]"),
            StringChecker("[rgba, 255.0, 0.0, 0.0, 1.0]"),
            DoubleChecker(0.0),
            StringChecker("[rgba, 255.0, 0.0, 0.0, 1.0]"),
            DoubleChecker(0.5021971),
            StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
        )
        val expectedTrailCasingExpressionContents = listOf(
            StringChecker("step"),
            StringChecker("[line-progress]"),
            StringChecker("[rgba, 255.0, 0.0, 255.0, 1.0]"),
            DoubleChecker(0.0),
            StringChecker("[rgba, 255.0, 0.0, 255.0, 1.0]"),
            DoubleChecker(0.5021971),
            StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
        )
        val expectedRestrictedExpressionContents = listOf(
            StringChecker("rgba"),
            DoubleChecker(0.0),
            DoubleChecker(0.0),
            DoubleChecker(0.0),
            DoubleChecker(0.0),
        )
        val colors = RouteLineColorResources.Builder()
            .routeLowCongestionColor(Color.GRAY)
            .routeUnknownCongestionColor(Color.LTGRAY)
            .routeDefaultColor(Color.BLUE)
            .routeLineTraveledColor(Color.RED)
            .routeLineTraveledCasingColor(Color.MAGENTA)
            .routeCasingColor(Color.CYAN)
            .inActiveRouteLegsColor(Color.YELLOW)
            .inactiveRouteLegCasingColor(Color.BLACK)
            .build()
        val options = MapboxRouteLineApiOptions.Builder()
            .build()
        val viewData = MapboxRouteLineViewOptions.Builder(ctx)
            .routeLineColorResources(colors)
            .build()
            .toData()
        val segments = MapboxRouteLineUtils.calculateRouteLineSegments(
            multilegRouteWithOverlap.navigationRoute,
            listOf(),
            true,
            options,
        )

        val result = MapboxRouteLineApi(options).getRouteLineDynamicDataForMaskingLayers(
            segments,
            null,
            multilegRouteWithOverlap.navigationRoute.directionsRoute.distance(),
            1,
        )

        assertNull(result.trimOffset)
        checkExpression(
            expectedRestrictedExpressionContents,
            getAppliedExpression(
                result.restrictedSectionExpressionCommandHolder!!,
                viewData,
                "line-gradient",
            ),
        )
        checkExpression(
            expectedTrafficExpressionContents,
            getAppliedExpression(
                result.trafficExpressionCommandHolder!!,
                viewData,
                "line-gradient",
            ),
        )
        checkExpression(
            expectedBaseExpressionContents,
            getAppliedExpression(
                result.baseExpressionCommandHolder,
                viewData,
                "line-gradient",
            ),
        )
        checkExpression(
            expectedCasingExpressionContents,
            getAppliedExpression(
                result.casingExpressionCommandHolder,
                viewData,
                "line-gradient",
            ),
        )
        checkExpression(
            expectedTrailExpressionContents,
            getAppliedExpression(
                result.trailExpressionCommandHolder!!,
                viewData,
                "line-gradient",
            ),
        )
        checkExpression(
            expectedTrailCasingExpressionContents,
            getAppliedExpression(
                result.trailCasingExpressionCommandHolder!!,
                viewData,
                "line-gradient",
            ),
        )

        assertTrue(
            result.trafficExpressionCommandHolder.provider is LightRouteLineValueProvider,
        )
        assertTrue(
            result.trafficExpressionCommandHolder.applier is LineGradientCommandApplier,
        )

        assertTrue(
            result.baseExpressionCommandHolder.provider is LightRouteLineValueProvider,
        )
        assertTrue(
            result.baseExpressionCommandHolder.applier is LineGradientCommandApplier,
        )
        assertTrue(
            result.casingExpressionCommandHolder.provider is LightRouteLineValueProvider,
        )
        assertTrue(
            result.casingExpressionCommandHolder.applier is LineGradientCommandApplier,
        )
        assertTrue(
            result.trailExpressionCommandHolder.provider is LightRouteLineValueProvider,
        )
        assertTrue(
            result.trafficExpressionCommandHolder.applier is LineGradientCommandApplier,
        )
        assertTrue(
            result.trailCasingExpressionCommandHolder.provider is LightRouteLineValueProvider,
        )
        assertTrue(
            result.trailCasingExpressionCommandHolder.applier is LineGradientCommandApplier,
        )
        assertTrue(
            result.restrictedSectionExpressionCommandHolder.provider
            is LightRouteLineValueProvider,
        )
        assertTrue(
            result.restrictedSectionExpressionCommandHolder.applier is LineGradientCommandApplier,
        )
    }

    @Test
    fun getRouteLineDynamicDataForMaskingLayersVanishingTest() = coroutineRule.runBlockingTest {
        val options = MapboxRouteLineApiOptions.Builder()
            .build()
        val segments = MapboxRouteLineUtils.calculateRouteLineSegments(
            multilegRouteWithOverlap.navigationRoute,
            listOf(),
            true,
            options,
        )

        val result = MapboxRouteLineApi(options).getRouteLineDynamicDataForMaskingLayers(
            segments,
            0.2,
            multilegRouteWithOverlap.navigationRoute.directionsRoute.distance(),
            1,
        )

        assertEquals(0.2, result.trimOffset?.offset)
    }

    @Test
    fun getRouteLineDynamicDataForMaskingLayersForRouteProgressTest() =
        coroutineRule.runBlockingTest {
            val expectedTrafficExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 136.0, 136.0, 136.0, 1.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 136.0, 136.0, 136.0, 1.0]"),
                DoubleChecker(0.4852533),
                StringChecker("[rgba, 204.0, 204.0, 204.0, 1.0]"),
                DoubleChecker(0.4868083),
                StringChecker("[rgba, 136.0, 136.0, 136.0, 1.0]"),
                DoubleChecker(0.494403),
                StringChecker("[rgba, 204.0, 204.0, 204.0, 1.0]"),
                DoubleChecker(0.4960591),
                StringChecker("[rgba, 136.0, 136.0, 136.0, 1.0]"),
                DoubleChecker(0.5021971),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            )
            val expectedBaseExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 0.0, 255.0, 1.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 0.0, 255.0, 1.0]"),
                DoubleChecker(0.5021971),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            )
            val expectedCasingExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 255.0, 255.0, 1.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 255.0, 255.0, 1.0]"),
                DoubleChecker(0.5021971),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            )
            val expectedTrailExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 255.0, 0.0, 0.0, 1.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 255.0, 0.0, 0.0, 1.0]"),
                DoubleChecker(0.5021971),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            )
            val expectedTrailCasingExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 255.0, 0.0, 255.0, 1.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 255.0, 0.0, 255.0, 1.0]"),
                DoubleChecker(0.5021971),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            )
            val expectedRestrictedExpressionContents = listOf(
                StringChecker("rgba"),
                DoubleChecker(0.0),
                DoubleChecker(0.0),
                DoubleChecker(0.0),
                DoubleChecker(0.0),
            )
            val colors = RouteLineColorResources.Builder()
                .routeLowCongestionColor(Color.GRAY)
                .routeUnknownCongestionColor(Color.LTGRAY)
                .routeDefaultColor(Color.BLUE)
                .routeLineTraveledColor(Color.RED)
                .routeLineTraveledCasingColor(Color.MAGENTA)
                .routeCasingColor(Color.CYAN)
                .inActiveRouteLegsColor(Color.YELLOW)
                .inactiveRouteLegCasingColor(Color.BLACK)
                .build()
            val options = MapboxRouteLineApiOptions.Builder()
                .build()
            val viewData = MapboxRouteLineViewOptions.Builder(ctx)
                .routeLineColorResources(colors)
                .build()
                .toData()
            val routeProgress = mockRouteProgress(multilegRouteWithOverlap.navigationRoute)
            every { routeProgress.currentLegProgress!!.legIndex } returns 1
            every { routeProgress.currentRouteGeometryIndex } returns 43
            val api = MapboxRouteLineApi(options)
            api.setNavigationRoutes(listOf(multilegRouteWithOverlap.navigationRoute))

            val result = api.getRouteLineDynamicDataForMaskingLayers(
                multilegRouteWithOverlap.navigationRoute,
                null,
                routeProgress.currentLegProgress!!,
            )!!

            assertNull(result.trimOffset)
            checkExpression(
                expectedRestrictedExpressionContents,
                getAppliedExpression(
                    result.restrictedSectionExpressionCommandHolder!!,
                    viewData,
                    "line-gradient",
                ),
            )
            checkExpression(
                expectedTrafficExpressionContents,
                getAppliedExpression(
                    result.trafficExpressionCommandHolder!!,
                    viewData,
                    "line-gradient",
                ),
            )
            checkExpression(
                expectedBaseExpressionContents,
                getAppliedExpression(
                    result.baseExpressionCommandHolder,
                    viewData,
                    "line-gradient",
                ),
            )
            checkExpression(
                expectedCasingExpressionContents,
                getAppliedExpression(
                    result.casingExpressionCommandHolder,
                    viewData,
                    "line-gradient",
                ),
            )
            checkExpression(
                expectedTrailExpressionContents,
                getAppliedExpression(
                    result.trailExpressionCommandHolder!!,
                    viewData,
                    "line-gradient",
                ),
            )
            checkExpression(
                expectedTrailCasingExpressionContents,
                getAppliedExpression(
                    result.trailCasingExpressionCommandHolder!!,
                    viewData,
                    "line-gradient",
                ),
            )

            assertTrue(
                result.trafficExpressionCommandHolder.provider is LightRouteLineValueProvider,
            )
            assertTrue(
                result.trafficExpressionCommandHolder.applier is LineGradientCommandApplier,
            )

            assertTrue(
                result.baseExpressionCommandHolder.provider is LightRouteLineValueProvider,
            )
            assertTrue(
                result.baseExpressionCommandHolder.applier is LineGradientCommandApplier,
            )
            assertTrue(
                result.casingExpressionCommandHolder.provider is LightRouteLineValueProvider,
            )
            assertTrue(
                result.casingExpressionCommandHolder.applier is LineGradientCommandApplier,
            )
            assertTrue(
                result.trailExpressionCommandHolder.provider is LightRouteLineValueProvider,
            )
            assertTrue(
                result.trafficExpressionCommandHolder.applier is LineGradientCommandApplier,
            )
            assertTrue(
                result.trailCasingExpressionCommandHolder.provider
                is LightRouteLineValueProvider,
            )
            assertTrue(
                result.trailCasingExpressionCommandHolder.applier is LineGradientCommandApplier,
            )
            assertTrue(
                result.restrictedSectionExpressionCommandHolder.provider
                is LightRouteLineValueProvider,
            )
            assertTrue(
                result.restrictedSectionExpressionCommandHolder.applier
                is LineGradientCommandApplier,
            )
        }

    @Test
    fun getRouteLineDynamicDataForMaskingLayersForRouteProgressVanishingTest() =
        coroutineRule.runBlockingTest {
            val options = MapboxRouteLineApiOptions.Builder()
                .build()
            val routeProgress = mockRouteProgress(multilegRouteWithOverlap.navigationRoute)
            every { routeProgress.currentLegProgress!!.legIndex } returns 1
            every { routeProgress.currentRouteGeometryIndex } returns 43
            val api = MapboxRouteLineApi(options)
            api.setNavigationRoutes(listOf(multilegRouteWithOverlap.navigationRoute))

            val result = api.getRouteLineDynamicDataForMaskingLayers(
                multilegRouteWithOverlap.navigationRoute,
                0.1,
                routeProgress.currentLegProgress!!,
            )!!

            assertEquals(0.1, result.trimOffset?.offset)
        }

    @Test
    fun getRouteLineDynamicDataForMaskingLayersForRouteProgressWhenSingleLegRouteTest() {
        val options = MapboxRouteLineApiOptions.Builder().build()
        val routeProgress = mockRouteProgress(shortRoute.navigationRoute)
        every { routeProgress.currentLegProgress!!.legIndex } returns 0

        val result = MapboxRouteLineApi(options).getRouteLineDynamicDataForMaskingLayers(
            shortRoute.navigationRoute,
            null,
            routeProgress.currentLegProgress!!,
        )

        assertNull(result)
    }

    @Test
    fun getRouteLineDynamicDataForMaskingLayers_when_routeLegIndexGreaterThanLegsTest() {
        val options = MapboxRouteLineApiOptions.Builder().build()
        val routeProgress = mockRouteProgress(shortRoute.navigationRoute)
        every { routeProgress.currentLegProgress!!.legIndex } returns 3

        val result = MapboxRouteLineApi(options).getRouteLineDynamicDataForMaskingLayers(
            shortRoute.navigationRoute,
            null,
            routeProgress.currentLegProgress!!,
        )

        assertNull(result)
    }

    @Test
    fun setNavigationRouteLinesPushesEvents() = runBlocking {
        val options = MapboxRouteLineApiOptions.Builder().build()
        val api = createRouteLineApi(options)
        api.setNavigationRoutes(listOf(multilegRouteWithOverlap.navigationRoute), 1)
        verifyOrder {
            sender.sendOptionsEvent(options)
            sender.sendSetRoutesEvent(
                listOf(NavigationRouteLine(multilegRouteWithOverlap.navigationRoute, null)),
                1,
            )
        }
    }

    @Test
    fun updateTraveledRouteLineDoesNotPushEventIfSkipped() = runBlocking { }

    @Test
    fun doesNotStartMemoryMonitoringOnCreation() {
        createRouteLineApi()

        verify(exactly = 0) {
            lowMemoryManager.addObserver(any())
            lowMemoryManager.removeObserver(any())
        }
    }

    @Test
    fun startsMemoryMonitoringOnRoutesSet() = runBlocking {
        createRouteLineApi().setNavigationRoutes(listOf(multilegRouteWithOverlap.navigationRoute))

        verify(exactly = 1) {
            lowMemoryManager.addObserver(any())
        }

        verify(exactly = 0) {
            lowMemoryManager.removeObserver(any())
        }
    }

    @Test
    fun startsMemoryMonitoringOnlyOnceOnMultipleRoutesSet() = runBlocking {
        val api = createRouteLineApi()
        api.setNavigationRoutes(listOf(loadNavigationRoute("short_route.json", uuid = "abc")))

        clearMocks(lowMemoryManager)
        api.setNavigationRoutes(listOf(loadNavigationRoute("short_route.json", uuid = "def")))

        verify(exactly = 0) {
            lowMemoryManager.addObserver(any())
            lowMemoryManager.removeObserver(any())
        }
    }

    @Test
    fun doesNotStartMemoryMonitoringOnEmptyRoutesListSet() = runBlocking {
        createRouteLineApi().setNavigationRoutes(emptyList())

        verify(exactly = 0) {
            lowMemoryManager.addObserver(any())
            lowMemoryManager.removeObserver(any())
        }
    }

    @Test
    fun restartsMemoryMonitoringOnSetVanishingOffset() = runBlocking {
        val options = MapboxRouteLineApiOptions.Builder()
            .styleInactiveRouteLegsIndependently(true)
            .vanishingRouteLineEnabled(true)
            .build()

        val api = createRouteLineApi(options)

        api.setNavigationRoutes(listOf(multilegRouteWithOverlap.navigationRoute))
        api.cancel()

        clearMocks(lowMemoryManager)

        api.setVanishingOffset(.5)

        verify(exactly = 1) {
            lowMemoryManager.addObserver(any())
        }

        verify(exactly = 0) {
            lowMemoryManager.removeObserver(any())
        }
    }

    @Test
    fun doesNotRestartMemoryMonitoringOnSetVanishingOffsetIfRoutesEmpty() = runBlocking {
        val options = MapboxRouteLineApiOptions.Builder()
            .styleInactiveRouteLegsIndependently(true)
            .vanishingRouteLineEnabled(true)
            .build()

        val api = MapboxRouteLineApi(options)

        api.setNavigationRoutes(listOf(multilegRouteWithOverlap.navigationRoute))
        api.cancel()

        clearMocks(lowMemoryManager)

        api.clearRouteLine()
        api.setVanishingOffset(.5)

        verify(exactly = 0) {
            lowMemoryManager.addObserver(any())
            lowMemoryManager.removeObserver(any())
        }
    }

    @Test
    fun restartsMemoryMonitoringOnUpdateWithRouteProgress() = runBlocking {
        val route = multilegRouteWithOverlap
        val api = createRouteLineApi()

        api.setNavigationRoutes(listOf(route.navigationRoute))
        api.cancel()

        clearMocks(lowMemoryManager)
        api.updateWithRouteProgress(multilegRouteWithOverlap.mockRouteProgress())

        verify(exactly = 1) {
            lowMemoryManager.addObserver(any())
        }

        verify(exactly = 0) {
            lowMemoryManager.removeObserver(any())
        }
    }

    @Test
    fun doesNotRestartMemoryMonitoringOnUpdateWithRouteProgressIfRoutesEmpty() = runBlocking {
        val route = multilegRouteWithOverlap
        val api = createRouteLineApi()

        api.setNavigationRoutes(listOf(route.navigationRoute))
        api.cancel()

        clearMocks(lowMemoryManager)

        api.clearRouteLine()
        api.updateWithRouteProgress(multilegRouteWithOverlap.mockRouteProgress())

        verify(exactly = 0) {
            lowMemoryManager.addObserver(any())
            lowMemoryManager.removeObserver(any())
        }
    }

    @Test
    fun restartsMemoryMonitoringOnUpdateTraveledRouteLine() = runBlocking {
        val route = multilegRouteWithOverlap

        val api = createRouteLineApi()
        api.setNavigationRoutes(listOf(route.navigationRoute))
        api.updateWithRouteProgress(route.mockRouteProgress())
        api.cancel()

        clearMocks(lowMemoryManager)
        api.updateTraveledRouteLine(Point.fromLngLat(-122.370112, 45.579391))

        verify(exactly = 1) {
            lowMemoryManager.addObserver(any())
        }

        verify(exactly = 0) {
            lowMemoryManager.removeObserver(any())
        }
    }

    @Test
    fun doesNotRestartMemoryMonitoringOnUpdateTraveledRouteLineIfRoutesEmpty() = runBlocking {
        val route = multilegRouteWithOverlap

        val api = createRouteLineApi()
        api.setNavigationRoutes(listOf(route.navigationRoute))
        api.updateWithRouteProgress(route.mockRouteProgress())
        api.cancel()

        clearMocks(lowMemoryManager)
        api.clearRouteLine()
        api.updateTraveledRouteLine(Point.fromLngLat(-122.370112, 45.579391))

        verify(exactly = 0) {
            lowMemoryManager.addObserver(any())
            lowMemoryManager.removeObserver(any())
        }
    }

    @Test
    fun stopMemoryMonitoringOnClearRouteLine() = runBlocking {
        val api = createRouteLineApi()
        api.setNavigationRoutes(listOf(multilegRouteWithOverlap.navigationRoute))
        api.clearRouteLine()

        verify(exactly = 1) {
            lowMemoryManager.removeObserver(any())
        }
    }

    @Test
    fun stopMemoryMonitoringOnCancel() = runBlocking {
        val api = createRouteLineApi()
        api.setNavigationRoutes(listOf(multilegRouteWithOverlap.navigationRoute))
        api.cancel()

        verify(exactly = 1) {
            lowMemoryManager.removeObserver(any())
        }
    }

    @Test
    fun doesNotStopMemoryMonitoringIfMonitoringIsNotStarted() = runBlocking {
        val api = createRouteLineApi()
        api.clearRouteLine()
        api.cancel()

        verify(exactly = 0) {
            lowMemoryManager.removeObserver(any())
        }
    }

    @Test
    fun clearMapboxRouteLineUtilsOnLowMemoryEvent() = runBlocking {
        mockkObject(MapboxRouteLineUtils)

        val api = createRouteLineApi()
        api.setNavigationRoutes(listOf(multilegRouteWithOverlap.navigationRoute))

        lowMemoryObserverSlot.captured.onLowMemory()

        verify(exactly = 1) {
            MapboxRouteLineUtils.trimRouteDataCacheToSize(size = 0)
        }

        unmockkObject(MapboxRouteLineUtils)
    }

    @Test
    fun clearMapboxRouteLineUtilsOnCancel() {
        mockkObject(MapboxRouteLineUtils)

        createRouteLineApi().cancel()

        verify(exactly = 1) {
            MapboxRouteLineUtils.trimRouteDataCacheToSize(size = 0)
        }

        unmockkObject(MapboxRouteLineUtils)
    }

    private fun mockRouteProgress(route: NavigationRoute, stepIndexValue: Int = 0): RouteProgress =
        mockk {
            every { currentLegProgress } returns mockk {
                every { legIndex } returns 0
                every { currentStepProgress } returns mockk {
                    every { stepPoints } returns PolylineUtils.decode(
                        route.directionsRoute.legs()!![0].steps()!![stepIndexValue].geometry()!!,
                        6,
                    )
                    every { distanceTraveled } returns 0f
                    every { step } returns mockk {
                        every { distance() } returns
                            route.directionsRoute.legs()!![0].steps()!![stepIndexValue].distance()
                    }
                    every { stepIndex } returns stepIndexValue
                }
            }
            every { currentState } returns RouteProgressState.TRACKING
            every { navigationRoute } returns route
        }

    private fun mockRouteOptions(): MapboxRouteLineApiOptions {
        return spyk(MapboxRouteLineApiOptions.Builder().build()) {
            every { calculateRestrictedRoadSections } returns false
            every { styleInactiveRouteLegsIndependently } returns false
        }
    }
}
