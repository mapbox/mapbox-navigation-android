package com.mapbox.navigation.ui.maps.route.line.api

import android.content.Context
import android.graphics.Color
import androidx.appcompat.content.res.AppCompatResources
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.core.constants.Constants
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.geojson.utils.PolylineUtils
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.QueriedFeature
import com.mapbox.maps.QueryFeaturesCallback
import com.mapbox.maps.RenderedQueryOptions
import com.mapbox.maps.ScreenBox
import com.mapbox.maps.ScreenCoordinate
import com.mapbox.navigation.base.internal.NativeRouteParserWrapper
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.route.toNavigationRoute
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineUtils
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants
import com.mapbox.navigation.ui.maps.route.line.MapboxRouteLineApiExtensions.clearRouteLine
import com.mapbox.navigation.ui.maps.route.line.MapboxRouteLineApiExtensions.findClosestRoute
import com.mapbox.navigation.ui.maps.route.line.MapboxRouteLineApiExtensions.setAlternativeTrafficColor
import com.mapbox.navigation.ui.maps.route.line.MapboxRouteLineApiExtensions.setNavigationRoutes
import com.mapbox.navigation.ui.maps.route.line.MapboxRouteLineApiExtensions.setPrimaryTrafficColor
import com.mapbox.navigation.ui.maps.route.line.MapboxRouteLineApiExtensions.setRoutes
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLine
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineColorResources
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineError
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineResources
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineUpdateValue
import com.mapbox.navigation.ui.maps.route.line.model.RoutePoints
import com.mapbox.navigation.ui.maps.route.line.model.RouteSetValue
import com.mapbox.navigation.ui.maps.route.line.model.RouteStyleDescriptor
import com.mapbox.navigation.ui.maps.route.line.model.VanishingPointState
import com.mapbox.navigation.ui.maps.testing.TestingUtil.loadNavigationRoute
import com.mapbox.navigation.ui.maps.testing.TestingUtil.loadRoute
import com.mapbox.navigation.utils.internal.InternalJobControlFactory
import com.mapbox.navigation.utils.internal.JobControl
import com.mapbox.navigation.utils.internal.LoggerFrontend
import com.mapbox.navigation.utils.internal.LoggerProvider
import com.mapbox.navigator.RouteInterface
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.test.runBlockingTest
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class MapboxRouteLineApiTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()
    private val parentJob = SupervisorJob()
    private val testScope = CoroutineScope(parentJob + coroutineRule.testDispatcher)
    private val ctx: Context = mockk()

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        mockkObject(InternalJobControlFactory)
        every {
            InternalJobControlFactory.createDefaultScopeJobControl()
        } returns JobControl(parentJob, testScope)
        mockkStatic(AppCompatResources::class)
        every { AppCompatResources.getDrawable(any(), any()) } returns mockk()

        mockkObject(NativeRouteParserWrapper)
        every {
            NativeRouteParserWrapper.parseDirectionsResponse(any(), any(), any())
        } answers {
            val response = JSONObject(this.firstArg<String>())
            val routesCount = response.getJSONArray("routes").length()
            val idBase = if (response.has("uuid")) {
                response.getString("uuid")
            } else {
                "local@${UUID.randomUUID()}"
            }
            val nativeRoutes = mutableListOf<RouteInterface>().apply {
                repeat(routesCount) {
                    add(
                        mockk {
                            every { routeInfo } returns mockk(relaxed = true)
                            every { routeId } returns "$idBase#$it"
                            every { routerOrigin } returns com.mapbox.navigator.RouterOrigin.ONBOARD
                        }
                    )
                }
            }
            ExpectedFactory.createValue(nativeRoutes)
        }
    }

    @After
    fun cleanUp() {
        unmockkStatic(AppCompatResources::class)
        unmockkObject(InternalJobControlFactory)
        unmockkObject(NativeRouteParserWrapper)
    }

    @Test
    fun getPrimaryRoute() = coroutineRule.runBlockingTest {
        val options = MapboxRouteLineOptions.Builder(ctx).build()
        val route = loadRoute("short_route.json", "abc")
        val api = MapboxRouteLineApi(options).also {
            it.setRoutes(listOf(RouteLine(route, null)))
        }

        val result = api.getPrimaryRoute()

        assertEquals(
            route,
            result
        )
    }

    @Test
    fun getVanishPointOffset() {
        val options = MapboxRouteLineOptions.Builder(ctx)
            .withVanishingRouteLineEnabled(true)
            .build()
        options.vanishingRouteLine!!.vanishPointOffset = 99.9

        val result = MapboxRouteLineApi(options).getVanishPointOffset()

        assertEquals(
            99.9,
            result, 0.0
        )
    }

    @Test
    fun getRoutes() = coroutineRule.runBlockingTest {
        val options = MapboxRouteLineOptions.Builder(ctx).build()
        val route = loadRoute("short_route.json", uuid = "abc")
        val routes = listOf(RouteLine(route, null))

        val api = MapboxRouteLineApi(options)
        api.setRoutes(routes)

        val result = api.getRoutes()

        assertEquals(
            result.size,
            routes.size
        )
        assertEquals(
            result[0],
            routes[0].route
        )
    }

    @Test
    fun setRoutes_whenRouteCoordinatesAreEmpty() = coroutineRule.runBlockingTest {
        val options = MapboxRouteLineOptions.Builder(ctx).build()
        val api = MapboxRouteLineApi(options)
        val route = loadRoute("short_route.json")
        val augmentedLineString =
            LineString.fromJson("{\"type\":\"LineString\",\"coordinates\":[]}")
                .toPolyline(Constants.PRECISION_6)
        val augmentedRouteJson = route.toJson()
            .replace("etylgAl`guhFpJrBh@kHbC{[nAZ", augmentedLineString)
        val augmentedRoute = DirectionsRoute.fromJson(augmentedRouteJson)

        val result = api.setRoutes(listOf(RouteLine(augmentedRoute, null)))

        assertNotNull(result.error)
        assertNull(result.value)
    }

    @Test
    fun setRoutes_setsVanishPointToZero() = coroutineRule.runBlockingTest {
        val options = MapboxRouteLineOptions.Builder(ctx)
            .withVanishingRouteLineEnabled(true)
            .build()
        options.vanishingRouteLine!!.vanishPointOffset = 99.9

        val api = MapboxRouteLineApi(options)
        val route = loadRoute("short_route.json")
        val routes = listOf(RouteLine(route, null))
        api.setRoutes(routes)

        val result = api.getVanishPointOffset()

        assertEquals(
            0.0,
            result, 0.0
        )
        assertEquals(0.0, options.vanishingRouteLine!!.vanishPointOffset, 0.0)
    }

    @Test
    fun setRoutes_alternativeRouteColorsReflectsStyleDescriptor() = coroutineRule.runBlockingTest {
        mockkObject(MapboxRouteLineUtils)
        val expectedAlternativeBaseExpression = "[step, [line-progress], " +
            "[rgba, 0.0, 0.0, 0.0, 0.0]," +
            " 0.0, [rgba, 255.0, 0.0, 0.0, 1.0]]"
        val expectedAlternativeCasingExpression = "[step, [line-progress], " +
            "[rgba, 0.0, 0.0, 0.0, 0.0], 0.0, [rgba, 0.0, 255.0, 0.0, 1.0]]"
        val options = MapboxRouteLineOptions.Builder(ctx)
            .withRouteStyleDescriptors(
                listOf(
                    RouteStyleDescriptor("someProperty", Color.RED, Color.GREEN)
                )
            )
            .build()
        val api = MapboxRouteLineApi(options)
        val route = loadRoute("short_route.json")
        val alternativeRoute = loadRoute("multileg_route.json")
        val routes = listOf(
            RouteLine(route, null),
            RouteLine(alternativeRoute, "someProperty")
        )

        val result = api.setRoutes(routes).value!!

        assertEquals(
            expectedAlternativeBaseExpression,
            result.alternativeRouteLinesData[0]
                .dynamicData
                .baseExpressionProvider
                .generateExpression()
                .toString()
        )
        assertEquals(
            expectedAlternativeCasingExpression,
            result.alternativeRouteLinesData[0]
                .dynamicData
                .casingExpressionProvider
                .generateExpression()
                .toString()
        )
        unmockkObject(MapboxRouteLineUtils)
    }

    @Test
    fun setRoutes_doesNotResetVanishingPointWhenSameRoute() = coroutineRule.runBlockingTest {
        val options = MapboxRouteLineOptions.Builder(ctx)
            .withVanishingRouteLineEnabled(true)
            .build()
        options.vanishingRouteLine!!.vanishPointOffset = 99.9

        val api = MapboxRouteLineApi(options)
        val route = loadRoute("short_route.json")
        val routes = listOf(RouteLine(route, null))
        api.setRoutes(routes)
        api.setVanishingOffset(25.0)

        api.setRoutes(routes)

        assertEquals(25.0, api.getVanishPointOffset(), 0.0)
        assertEquals(25.0, options.vanishingRouteLine!!.vanishPointOffset, 0.0)
    }

    @Test
    fun setRoutesNoAlternativeRouteDuplicates() = coroutineRule.runBlockingTest {
        val options = MapboxRouteLineOptions.Builder(ctx).build()
        val api = MapboxRouteLineApi(options)
        val routes = listOf(
            RouteLine(loadRoute("short_route.json"), null),
            RouteLine(loadRoute("multileg_route.json"), null),
            RouteLine(loadRoute("route-with-road-classes.txt"), null)
        )

        val result = api.setRoutes(routes)

        assertNotEquals(
            result.value!!.alternativeRouteLinesData[0],
            result.value!!.alternativeRouteLinesData[1]
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
        val route = loadRoute("short_route.json")
        val routes = listOf(RouteLine(route, null))
        val consumer = MapboxNavigationConsumer<Expected<RouteLineError, RouteSetValue>> { result ->
            callbackCalled = true

            assertEquals(
                expectedCasingExpression,
                result.value!!.primaryRouteLineData.dynamicData.casingExpressionProvider
                    .generateExpression().toString()
            )
            assertEquals(
                expectedRouteLineExpression,
                result.value!!.primaryRouteLineData.dynamicData.baseExpressionProvider
                    .generateExpression().toString()
            )
            assertEquals(
                expectedTrafficLineExpression,
                result.value!!.primaryRouteLineData.dynamicData.trafficExpressionProvider!!
                    .generateExpression().toString()
            )
            assertEquals(
                expectedPrimaryRouteSourceGeometry,
                result.value!!.primaryRouteLineData.featureCollection.features()!![0].geometry()
                    .toString()
            )
            assertTrue(
                result.value!!.alternativeRouteLinesData[0].featureCollection.features()!!.isEmpty()
            )
            assertTrue(
                result.value!!.alternativeRouteLinesData[1].featureCollection.features()!!.isEmpty()
            )
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
    fun setRoutes_trimOffsetValueFromVanishingRouteLine() =
        coroutineRule.runBlockingTest {
            val vanishingRouteLine = mockk<VanishingRouteLine>(relaxed = true) {
                every { vanishPointOffset } returns 9.9
            }
            val realOptions = MapboxRouteLineOptions.Builder(ctx).build()
            val options = mockk<MapboxRouteLineOptions>()
            every { options.resourceProvider } returns realOptions.resourceProvider
            every { options.vanishingRouteLine } returns vanishingRouteLine
            every { options.displayRestrictedRoadSections } returns false
            every {
                options.styleInactiveRouteLegsIndependently
            } returns realOptions.styleInactiveRouteLegsIndependently
            every { options.displaySoftGradientForTraffic } returns false
            every { options.softGradientTransition } returns 30.0
            every { options.routeStyleDescriptors } returns listOf()

            val api = MapboxRouteLineApi(options)
            val route = loadRoute("short_route.json")
            val routes = listOf(RouteLine(route, null))

            val result = api.setRoutes(routes).value!!

            assertEquals(
                9.9,
                result.primaryRouteLineData.dynamicData.trimOffset!!.offset,
                0.0
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
        val route = loadRoute("short_route.json")
        val altRoute1 = loadRoute("route-with-road-classes.txt")
        val altRoute2 = loadRoute("multileg_route.json")
        val routes = listOf(
            RouteLine(route, null),
            RouteLine(altRoute1, "alternativeRoute1"),
            RouteLine(altRoute2, "alternativeRoute2")
        )

        val result = api.setRoutes(routes)

        assertEquals(
            "{\"alternativeRoute1\":true}",
            result.value!!.alternativeRouteLinesData[0].featureCollection.features()!!
                .first().properties().toString()
        )
        assertEquals(
            "{\"alternativeRoute2\":true}",
            result.value!!.alternativeRouteLinesData[1].featureCollection.features()!!
                .first().properties().toString()
        )
    }

    @Test
    fun updateUpcomingRoutePointIndex() = coroutineRule.runBlockingTest {
        val route = loadNavigationRoute("short_route.json")
        val mockVanishingRouteLine = mockk<VanishingRouteLine>(relaxUnitFun = true) {
            every { vanishPointOffset } returns 0.0
        }
        val options = mockRouteOptions()
        every { options.vanishingRouteLine } returns mockVanishingRouteLine
        val api = MapboxRouteLineApi(options)
        val routeProgress = mockRouteProgress(route, stepIndexValue = 2)
        api.updateVanishingPointState(RouteProgressState.TRACKING)
        api.setNavigationRoutes(listOf(route))

        api.updateUpcomingRoutePointIndex(routeProgress)

        verify { mockVanishingRouteLine.primaryRouteRemainingDistancesIndex = 6 }
    }

    // This test is to address issue #4995. The test name doesn't accurately describe the condition.
    // Although the cause of the error condition is unclear at this time the code can be modified
    // to protect against such an exception occurring.
    @Test
    fun updateUpcomingRoutePointIndex_whenCompleteRoutePointsNotContainLegIndex() =
        coroutineRule.runBlockingTest {
            val route = loadNavigationRoute("short_route.json")
            val parsedRoutePoints = MapboxRouteLineUtils.routePointsProvider(route)
            mockkObject(MapboxRouteLineUtils)
            every {
                MapboxRouteLineUtils.routePointsProvider(route)
            } returns RoutePoints(listOf(), parsedRoutePoints!!.flatList)
            val mockVanishingRouteLine = mockk<VanishingRouteLine>(relaxUnitFun = true) {
                every { vanishPointOffset } returns 0.0
            }
            val options = mockRouteOptions()
            every { options.vanishingRouteLine } returns mockVanishingRouteLine
            val api = MapboxRouteLineApi(options)
            val routeProgress = mockRouteProgress(route, stepIndexValue = 2)
            api.updateVanishingPointState(RouteProgressState.TRACKING)
            api.setNavigationRoutes(listOf(route))

            api.updateUpcomingRoutePointIndex(routeProgress)

            verify { mockVanishingRouteLine.primaryRouteRemainingDistancesIndex = 6 }

            unmockkObject(MapboxRouteLineUtils)
        }

    @Test
    fun updateUpcomingRoutePointIndex_whenCurrentStepProgressIsNull() =
        coroutineRule.runBlockingTest {
            val route = loadNavigationRoute("short_route.json")
            val mockVanishingRouteLine = mockk<VanishingRouteLine>(relaxUnitFun = true) {
                every { vanishPointOffset } returns 0.0
            }
            val options = mockRouteOptions()
            every { options.vanishingRouteLine } returns mockVanishingRouteLine
            val api = MapboxRouteLineApi(options)
            val routeProgress = mockRouteProgress(route, stepIndexValue = 2)
            every { routeProgress.currentLegProgress } returns mockk {
                every { legIndex } returns 0
                every { currentStepProgress } returns null
            }
            api.updateVanishingPointState(RouteProgressState.TRACKING)
            api.setNavigationRoutes(listOf(route))

            api.updateUpcomingRoutePointIndex(routeProgress)

            verify { mockVanishingRouteLine.primaryRouteRemainingDistancesIndex = null }
        }

    @Test
    fun updateUpcomingRoutePointIndex_whenCurrentLegProgressIsNull() =
        coroutineRule.runBlockingTest {
            val route = loadNavigationRoute("short_route.json")
            val mockVanishingRouteLine = mockk<VanishingRouteLine>(relaxUnitFun = true) {
                every { vanishPointOffset } returns 0.0
            }
            val options = mockRouteOptions()
            every { options.vanishingRouteLine } returns mockVanishingRouteLine
            val api = MapboxRouteLineApi(options)
            val routeProgress = mockRouteProgress(route, stepIndexValue = 2)
            every { routeProgress.currentLegProgress } returns null
            api.updateVanishingPointState(RouteProgressState.TRACKING)
            api.setNavigationRoutes(listOf(route))

            api.updateUpcomingRoutePointIndex(routeProgress)

            verify { mockVanishingRouteLine.primaryRouteRemainingDistancesIndex = null }
        }

    @Test
    fun updateWithRouteProgress() = coroutineRule.runBlockingTest {
        val route = loadNavigationRoute("short_route.json")
        val mockVanishingRouteLine = mockk<VanishingRouteLine>(relaxUnitFun = true) {
            every { vanishPointOffset } returns 0.0
        }
        val options = mockRouteOptions()
        every { options.vanishingRouteLine } returns mockVanishingRouteLine
        val api = MapboxRouteLineApi(options)
        val routeProgress = mockRouteProgress(route, stepIndexValue = 2)
        api.updateVanishingPointState(RouteProgressState.TRACKING)
        api.setNavigationRoutes(listOf(route))

        api.updateWithRouteProgress(routeProgress) {}

        verify { mockVanishingRouteLine.primaryRouteRemainingDistancesIndex = 6 }
        verify {
            mockVanishingRouteLine.updateVanishingPointState(RouteProgressState.TRACKING)
        }
    }

    @Test
    fun `updateWithRouteProgress - abort if no primary route`() = coroutineRule.runBlockingTest {
        val route = loadNavigationRoute("short_route.json")
        val options = mockRouteOptions()
        val api = MapboxRouteLineApi(options)
        val routeProgress = mockRouteProgress(route, stepIndexValue = 2)
        val consumer =
            mockk<MapboxNavigationConsumer<Expected<RouteLineError, RouteLineUpdateValue>>>(
                relaxUnitFun = true
            )

        api.updateWithRouteProgress(routeProgress, consumer)

        val resultSlot = slot<Expected<RouteLineError, RouteLineUpdateValue>>()
        verify { consumer.accept(capture(resultSlot)) }
        assertEquals(
            "You're calling #updateWithRouteProgress without any routes being set.",
            resultSlot.captured.error!!.errorMessage
        )
    }

    @Test
    fun `updateWithRouteProgress - abort if primary route different`() =
        coroutineRule.runBlockingTest {
            val route1 = loadNavigationRoute("multileg-route-two-legs.json", uuid = "abc")
            val route2 = loadNavigationRoute("short_route.json", uuid = "def")
            val mockVanishingRouteLine = mockk<VanishingRouteLine>(relaxUnitFun = true) {
                every { vanishPointOffset } returns 0.0
            }
            val options = mockRouteOptions()
            every { options.vanishingRouteLine } returns mockVanishingRouteLine
            val api = MapboxRouteLineApi(options)
            api.setNavigationRoutes(listOf(route1))
            val routeProgress = mockRouteProgress(route2, stepIndexValue = 2)
            val consumer =
                mockk<MapboxNavigationConsumer<Expected<RouteLineError, RouteLineUpdateValue>>>(
                    relaxUnitFun = true
                )

            api.updateWithRouteProgress(routeProgress, consumer)

            val resultSlot = slot<Expected<RouteLineError, RouteLineUpdateValue>>()
            verify { consumer.accept(capture(resultSlot)) }
            assertEquals(
                "Provided primary route (#setNavigationRoutes, ID: abc#0) and navigated " +
                    "route (#updateWithRouteProgress, ID: def#0) are not the same. " +
                    "Aborting the update.",
                resultSlot.captured.error!!.errorMessage
            )
        }

    @Test
    fun updateWithRouteProgress_whenDeEmphasizeInactiveLegSegments_vanishingRouteLineDisabled() =
        coroutineRule.runBlockingTest {
            val expectedTrafficExp = "[step, [line-progress], [rgba, 0.0, 0.0, 0.0, 0.0], 0.0, " +
                "[rgba, 0.0, 0.0, 0.0, 0.0]]"
            val route = loadNavigationRoute("multileg-route-two-legs.json")
            val options = mockRouteOptions()
            every { options.vanishingRouteLine } returns null
            every { options.styleInactiveRouteLegsIndependently } returns true
            val api = MapboxRouteLineApi(options)
            val routeProgress = mockRouteProgress(route)
            api.updateVanishingPointState(RouteProgressState.TRACKING)
            api.setNavigationRoutes(listOf(route))
            api.updateWithRouteProgress(routeProgress) {}

            val result = api.setVanishingOffset(0.0).value!!

            assertEquals(
                expectedTrafficExp,
                result.primaryRouteLineDynamicData.trafficExpressionProvider!!
                    .generateExpression().toString()
            )
            assertEquals(-1, api.activeLegIndex)
        }

    @Test
    fun updateWithRouteProgress_whenDeEmphasizeInactiveLegSegments_setsActiveLegIndex() =
        coroutineRule.runBlockingTest {
            val route = loadNavigationRoute("multileg-route-two-legs.json")
            val mockVanishingRouteLine = mockk<VanishingRouteLine>(relaxUnitFun = true) {
                every { vanishPointOffset } returns 0.0
            }
            val options = mockRouteOptions()
            every { options.vanishingRouteLine } returns mockVanishingRouteLine
            every { options.styleInactiveRouteLegsIndependently } returns true
            val api = MapboxRouteLineApi(options)
            val routeProgress = mockRouteProgress(route)
            api.updateVanishingPointState(RouteProgressState.TRACKING)
            api.setNavigationRoutes(listOf(route))

            api.updateWithRouteProgress(routeProgress) {}

            assertEquals(0, api.activeLegIndex)
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
        val route = loadRoute("short_route.json")
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
        mockkObject(MapboxRouteLineUtils)
        val options = MapboxRouteLineOptions.Builder(ctx).build()
        val api = MapboxRouteLineApi(options)

        val result = api.clearRouteLine()

        assertTrue(result.value!!.alternativeRouteSourceSources.first().features()!!.isEmpty())
        assertTrue(result.value!!.alternativeRouteSourceSources[1].features()!!.isEmpty())
        assertTrue(result.value!!.primaryRouteSource.features()!!.isEmpty())
        assertTrue(result.value!!.waypointsSource.features()!!.isEmpty())
        verify { MapboxRouteLineUtils.trimRouteDataCacheToSize(size = 0) }
        unmockkObject(MapboxRouteLineUtils)
    }

    @Test
    fun findClosestRoute_whenClickPoint() = runBlockingTest {
        mockkObject(MapboxRouteLineUtils)
        every { MapboxRouteLineUtils.getLayerIdsForPrimaryRoute(any(), any()) } returns setOf()
        val feature1 = mockk<QueriedFeature> {
            every { feature.id() } returns "abc#0"
        }
        val feature2 = mockk<QueriedFeature> {
            every { feature.id() } returns "def#0"
        }
        val route1 = loadRoute("short_route.json", uuid = "abc")
        val route2 = loadRoute("short_route.json", uuid = "def")
        val options = MapboxRouteLineOptions.Builder(ctx).build()
        val api = MapboxRouteLineApi(options).also {
            it.setRoutes(listOf(RouteLine(route1, null), RouteLine(route2, null)))
        }
        val point = Point.fromLngLat(139.7745686, 35.677573)
        val mockExpected = mockk<Expected<String, List<QueriedFeature>>> {
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
        unmockkObject(MapboxRouteLineUtils)
    }

    @Test
    fun findClosestRoute_whenRectPoint() = runBlockingTest {
        mockkObject(MapboxRouteLineUtils)
        every { MapboxRouteLineUtils.getLayerIdsForPrimaryRoute(any(), any()) } returns setOf()
        val feature1 = mockk<QueriedFeature> {
            every { feature.id() } returns "abc#0"
        }
        val feature2 = mockk<QueriedFeature> {
            every { feature.id() } returns "def#0"
        }
        val route1 = loadRoute("short_route.json", uuid = "abc")
        val route2 = loadRoute("short_route.json", uuid = "def")
        val options = MapboxRouteLineOptions.Builder(ctx).build()
        val api = MapboxRouteLineApi(options).also {
            it.setRoutes(listOf(RouteLine(route1, null), RouteLine(route2, null)))
        }
        val point = Point.fromLngLat(139.7745686, 35.677573)
        val emptyExpected = mockk<Expected<String, List<QueriedFeature>>> {
            every { value } returns listOf()
        }
        val mockExpected = mockk<Expected<String, List<QueriedFeature>>> {
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
        unmockkObject(MapboxRouteLineUtils)
    }

    @Test
    fun findClosestRoute_whenPrimaryRoute() = runBlockingTest {
        mockkObject(MapboxRouteLineUtils)
        every { MapboxRouteLineUtils.getLayerIdsForPrimaryRoute(any(), any()) } returns setOf()
        val feature1 = mockk<QueriedFeature> {
            every { feature.id() } returns "abc#0"
        }
        val feature2 = mockk<QueriedFeature> {
            every { feature.id() } returns "def#0"
        }
        val route1 = loadRoute("short_route.json", uuid = "abc")
        val route2 = loadRoute("short_route.json", uuid = "def")
        val options = MapboxRouteLineOptions.Builder(ctx).build()
        val api = MapboxRouteLineApi(options).also {
            it.setRoutes(listOf(RouteLine(route1, null), RouteLine(route2, null)))
        }
        val point = Point.fromLngLat(139.7745686, 35.677573)
        val emptyExpected = mockk<Expected<String, List<QueriedFeature>>> {
            every { value } returns listOf()
        }
        val mockExpected = mockk<Expected<String, List<QueriedFeature>>> {
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
                        .contains(RouteLayerConstants.LAYER_GROUP_2_MAIN)
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
        unmockkObject(MapboxRouteLineUtils)
    }

    @Test
    fun cancel() {
        val mockParentJob = mockk<CompletableJob>(relaxed = true)
        val mockJobControl = mockk<JobControl> {
            every { job } returns mockParentJob
        }
        every { InternalJobControlFactory.createDefaultScopeJobControl() } returns mockJobControl

        MapboxRouteLineApi(MapboxRouteLineOptions.Builder(ctx).build()).cancel()

        verify { mockParentJob.cancelChildren() }
    }

    @Test
    fun setRoadClasses() = coroutineRule.runBlockingTest {
        val route = loadRoute("route-with-road-classes.txt")
        val colors = RouteLineColorResources.Builder()
            .routeLowCongestionColor(5)
            .routeUnknownCongestionColor(1)
            .build()
        val resources = RouteLineResources.Builder().routeLineColorResources(colors).build()
        val options = MapboxRouteLineOptions.Builder(ctx).withRouteLineResources(resources).build()
        val api = MapboxRouteLineApi(options)
        val defaultResult = api.setRoutes(listOf(RouteLine(route, null))).value!!
        val defaultTrafficExpression = defaultResult
            .primaryRouteLineData
            .dynamicData
            .trafficExpressionProvider!!
            .generateExpression()
        api.setRoadClasses(listOf("service"))
        val result = api.setRoutes(listOf(RouteLine(route, null))).value!!

        val resultExpression = result
            .primaryRouteLineData
            .dynamicData
            .trafficExpressionProvider!!
            .generateExpression()

        assertNotEquals(defaultTrafficExpression.toString(), resultExpression.toString())
    }

    @Test
    fun setRoadClasses_setVanishingOffset() = coroutineRule.runBlockingTest {
        val route = loadRoute("route-with-road-classes.txt")
        val colors = RouteLineColorResources.Builder()
            .routeLowCongestionColor(5)
            .routeUnknownCongestionColor(1)
            .build()
        val resources = RouteLineResources.Builder().routeLineColorResources(colors).build()
        val options = MapboxRouteLineOptions.Builder(ctx)
            .withRouteLineResources(resources)
            .withVanishingRouteLineEnabled(true)
            .build()
        val api = MapboxRouteLineApi(options)
        api.setRoutes(listOf(RouteLine(route, null)))
        val defaultResult = api.setVanishingOffset(0.0).value!!
            .primaryRouteLineDynamicData
            .trafficExpressionProvider!!
            .generateExpression()
        api.setRoadClasses(listOf("service"))
        api.setRoutes(listOf(RouteLine(route, null)))

        val result = api.setVanishingOffset(0.0).value!!
            .primaryRouteLineDynamicData
            .trafficExpressionProvider!!
            .generateExpression()

        assertNotEquals(defaultResult.toString(), result.toString())
    }

    @Test
    fun setPrimaryTrafficColorExtension() = coroutineRule.runBlockingTest {
        val options = MapboxRouteLineOptions.Builder(ctx).build()
        val api = MapboxRouteLineApi(options)
        val route = loadRoute("short_route.json")
        val routes = listOf(RouteLine(route, null))

        val result = api.setRoutes(routes).setPrimaryTrafficColor(Color.MAGENTA).value!!

        assertEquals(
            "[step, [line-progress], [rgba, 255.0, 0.0, 255.0, 1.0], 0.0, " +
                "[rgba, 255.0, 0.0, 255.0, 1.0]]",
            result.primaryRouteLineData
                .dynamicData
                .trafficExpressionProvider!!
                .generateExpression().toString()
        )
    }

    @Test
    fun setPrimaryTrafficExpressionExtension() = coroutineRule.runBlockingTest {
        val options = MapboxRouteLineOptions.Builder(ctx).build()
        val api = MapboxRouteLineApi(options)
        val route = loadRoute("short_route.json")
        val routes = listOf(RouteLine(route, null))
        val replacementExpression =
            MapboxRouteLineUtils.getRouteLineExpression(0.0, Color.CYAN, Color.CYAN)

        val result = api.setRoutes(routes).setPrimaryTrafficColor(replacementExpression).value!!

        assertEquals(
            replacementExpression.toString(),
            result.primaryRouteLineData
                .dynamicData
                .trafficExpressionProvider!!
                .generateExpression().toString()
        )
    }

    @Test
    fun setAlternativeTrafficColorExtension() = coroutineRule.runBlockingTest {
        val options = MapboxRouteLineOptions.Builder(ctx).build()
        val api = MapboxRouteLineApi(options)
        val route = loadRoute("short_route.json")
        val altRoute1 = loadRoute("route-with-road-classes.txt")
        val altRoute2 = loadRoute("multileg_route.json")
        val routes = listOf(
            RouteLine(route, null),
            RouteLine(altRoute1, "alternativeRoute1"),
            RouteLine(altRoute2, "alternativeRoute2")
        )

        val result = api.setRoutes(routes).setAlternativeTrafficColor(Color.MAGENTA).value!!

        assertEquals(
            "[step, [line-progress], [rgba, 0.0, 0.0, 0.0, 0.0], 0.0, " +
                "[rgba, 0.0, 0.0, 0.0, 0.0]]",
            result
                .primaryRouteLineData
                .dynamicData
                .trafficExpressionProvider!!
                .generateExpression().toString()
        )
        assertEquals(
            "[step, [line-progress], [rgba, 255.0, 0.0, 255.0, 1.0], 0.0, " +
                "[rgba, 255.0, 0.0, 255.0, 1.0]]",
            result
                .alternativeRouteLinesData[0]
                .dynamicData
                .trafficExpressionProvider!!
                .generateExpression().toString()
        )
        assertEquals(
            "[step, [line-progress], [rgba, 255.0, 0.0, 255.0, 1.0], 0.0, " +
                "[rgba, 255.0, 0.0, 255.0, 1.0]]",
            result
                .alternativeRouteLinesData[1]
                .dynamicData
                .trafficExpressionProvider!!
                .generateExpression().toString()
        )
    }

    @Test
    fun `setNavigationRouteLines uses distinct routes`() = coroutineRule.runBlockingTest {
        val logger = mockk<LoggerFrontend>(relaxed = true)
        LoggerProvider.setLoggerFrontend(logger)
        val options = MapboxRouteLineOptions.Builder(ctx).build()
        val api = MapboxRouteLineApi(options)
        val route1 = loadRoute("short_route.json", uuid = "abc").toNavigationRoute(
            routerOrigin = RouterOrigin.Offboard
        )
        val route2 = loadRoute("short_route.json", uuid = "abc").toNavigationRoute(
            routerOrigin = RouterOrigin.Offboard
        )

        val result = api.setNavigationRoutes(listOf(route1, route2))

        result.value!!.alternativeRouteLinesData.forEach {
            assertEquals(FeatureCollection.fromFeatures(listOf()), it.featureCollection)
        }
        verify {
            logger.logW(
                "Routes provided to MapboxRouteLineApi contain duplicates " +
                    "(based on NavigationRoute#id) - using only distinct instances",
                "MapboxRouteLineApi"
            )
        }
    }

    @Test
    fun `setNavigationRouteLines trims data cache`() = coroutineRule.runBlockingTest {
        mockkObject(MapboxRouteLineUtils)
        val options = MapboxRouteLineOptions.Builder(ctx).build()
        val api = MapboxRouteLineApi(options)
        val route = loadRoute("short_route.json").toNavigationRoute(
            routerOrigin = RouterOrigin.Offboard
        )

        api.setNavigationRoutes(listOf(route))
        api.setNavigationRoutes(emptyList())

        verifyOrder {
            MapboxRouteLineUtils.trimRouteDataCacheToSize(1)
            MapboxRouteLineUtils.trimRouteDataCacheToSize(0)
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
                        6
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

    private fun mockRouteOptions(): MapboxRouteLineOptions {
        val realOptions = MapboxRouteLineOptions.Builder(ctx).build()
        return mockk {
            every { vanishingRouteLine } returns mockk()
            every { resourceProvider } returns realOptions.resourceProvider
            every { displayRestrictedRoadSections } returns false
            every { styleInactiveRouteLegsIndependently } returns false
            every { displaySoftGradientForTraffic } returns false
            every { softGradientTransition } returns 30.0
            every { routeStyleDescriptors } returns listOf()
        }
    }
}
