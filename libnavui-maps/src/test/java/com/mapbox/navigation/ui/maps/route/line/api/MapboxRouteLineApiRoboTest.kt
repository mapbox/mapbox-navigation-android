package com.mapbox.navigation.ui.maps.route.line.api

import android.content.Context
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.core.constants.Constants
import com.mapbox.geojson.LineString
import com.mapbox.geojson.utils.PolylineUtils
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.navigation.base.internal.NativeRouteParserWrapper
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.route.toNavigationRoute
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.core.routealternatives.AlternativeRouteMetadata
import com.mapbox.navigation.testing.FileUtils
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineUtils
import com.mapbox.navigation.ui.maps.route.line.MapboxRouteLineApiExtensions.getRouteDrawData
import com.mapbox.navigation.ui.maps.route.line.MapboxRouteLineApiExtensions.setNavigationRoutes
import com.mapbox.navigation.ui.maps.route.line.MapboxRouteLineApiExtensions.setRoutes
import com.mapbox.navigation.ui.maps.route.line.MapboxRouteLineApiExtensions.showRouteWithLegIndexHighlighted
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLine
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineColorResources
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineError
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineResources
import com.mapbox.navigation.ui.maps.route.line.model.RouteSetValue
import com.mapbox.navigation.ui.maps.testing.TestingUtil.loadRoute
import com.mapbox.navigation.utils.internal.InternalJobControlFactory
import com.mapbox.navigation.utils.internal.JobControl
import com.mapbox.navigator.RouteInterface
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.TimeUnit

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class MapboxRouteLineApiRoboTest {

    lateinit var ctx: Context

    @get:Rule
    var coroutineRule = MainCoroutineRule()
    private val parentJob = SupervisorJob()
    private val testScope = CoroutineScope(parentJob + coroutineRule.testDispatcher)

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
        mockkObject(InternalJobControlFactory)
        every {
            InternalJobControlFactory.createDefaultScopeJobControl()
        } returns JobControl(parentJob, testScope)

        mockkObject(NativeRouteParserWrapper)
        every {
            NativeRouteParserWrapper.parseDirectionsResponse(any(), any(), any())
        } answers {
            val routesCount =
                JSONObject(this.firstArg<String>())
                    .getJSONArray("routes")
                    .length()
            val nativeRoutes = mutableListOf<RouteInterface>().apply {
                repeat(routesCount) {
                    add(
                        mockk {
                            every { routeInfo } returns mockk(relaxed = true)
                            every { routeId } returns "$it"
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
        unmockkObject(InternalJobControlFactory)
        unmockkObject(NativeRouteParserWrapper)
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
        val route = loadRoute("short_route.json")
        val routes = listOf(RouteLine(route, null))

        val result = api.setRoutes(routes)

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

    @Test
    fun `setNavigationRoutes takes into account inactiveLegStyling and legIndex`() =
        coroutineRule.runBlockingTest {
            val route = loadRoute("multileg_route.json").toNavigationRoute(RouterOrigin.Custom())
            val altRoute = loadRoute("multileg_route_with_overlap.json")
                .toNavigationRoute(RouterOrigin.Custom())

            val apiWithIndependentInactiveStylingEnabled = MapboxRouteLineApi(
                MapboxRouteLineOptions.Builder(ctx)
                    .styleInactiveRouteLegsIndependently(true)
                    .build()
            )
            val apiWithIndependentInactiveStylingDisabled = MapboxRouteLineApi(
                MapboxRouteLineOptions.Builder(ctx)
                    .build()
            )

            val independentStylingEnabledLegZeroResult = apiWithIndependentInactiveStylingEnabled
                .setNavigationRoutes(listOf(route, altRoute))
            val independentStylingEnabledLegOneResult = apiWithIndependentInactiveStylingEnabled
                .setNavigationRoutes(listOf(route, altRoute), activeLegIndex = 1)
            val independentStylingDisabledLegZeroResult = apiWithIndependentInactiveStylingDisabled
                .setNavigationRoutes(listOf(route, altRoute))
            val independentStylingDisabledLegOneResult = apiWithIndependentInactiveStylingDisabled
                .setNavigationRoutes(listOf(route, altRoute), activeLegIndex = 1)

            val differentValuesToCheck = listOf(
                independentStylingEnabledLegZeroResult,
                independentStylingEnabledLegOneResult,
                independentStylingDisabledLegZeroResult,
            )
            val equalValuesToCheck = listOf(
                independentStylingDisabledLegZeroResult,
                independentStylingDisabledLegOneResult,
            )
            val allValuesToCheck = (differentValuesToCheck + equalValuesToCheck).toSet()
            checkNotEquals(differentValuesToCheck) {
                it.primaryRouteLineData.dynamicData.trafficExpressionProvider!!.generateExpression()
            }
            checkEquals(equalValuesToCheck) {
                it.primaryRouteLineData.dynamicData.trafficExpressionProvider!!.generateExpression()
            }
            println("failing")
            checkNotEquals(differentValuesToCheck) {
                it.primaryRouteLineData.dynamicData.baseExpressionProvider.generateExpression()
            }
            checkEquals(equalValuesToCheck) {
                it.primaryRouteLineData.dynamicData.baseExpressionProvider.generateExpression()
            }
            checkNotEquals(differentValuesToCheck) {
                it.primaryRouteLineData.dynamicData.casingExpressionProvider.generateExpression()
            }
            checkEquals(equalValuesToCheck) {
                it.primaryRouteLineData.dynamicData.casingExpressionProvider.generateExpression()
            }
            checkEquals(allValuesToCheck) {
                it.primaryRouteLineData.dynamicData.trailExpressionProvider!!.generateExpression()
            }
            checkEquals(allValuesToCheck) {
                it.primaryRouteLineData.dynamicData.trailCasingExpressionProvider!!
                    .generateExpression()
            }

            checkEquals(allValuesToCheck) {
                it.alternativeRouteLinesData.first().dynamicData.trafficExpressionProvider!!
                    .generateExpression()
            }
            checkEquals(allValuesToCheck) {
                it.alternativeRouteLinesData.first().dynamicData.baseExpressionProvider
                    .generateExpression()
            }
            checkEquals(allValuesToCheck) {
                it.alternativeRouteLinesData.first().dynamicData.casingExpressionProvider
                    .generateExpression()
            }
            checkEquals(allValuesToCheck) {
                it.alternativeRouteLinesData.first().dynamicData.trailExpressionProvider!!
                    .generateExpression()
            }
            checkEquals(allValuesToCheck) {
                it.alternativeRouteLinesData.first().dynamicData.trailCasingExpressionProvider!!
                    .generateExpression()
            }
        }

    private fun checkNotEquals(
        values: Iterable<Expected<RouteLineError, RouteSetValue>>,
        expressionExtractor: (RouteSetValue) -> Expression
    ) {
        val expressions = values.map { expressionExtractor(it.value!!) }
        expressions.forEachIndexed { index, expression ->
            println("$index; $expression")
        }
        for (i in 0 until expressions.size) {
            for (j in i + 1 until expressions.size) {
                assertNotEquals(expressions[i], expressions[j])
            }
        }
    }

    private fun checkEquals(
        values: Iterable<Expected<RouteLineError, RouteSetValue>>,
        expressionExtractor: (RouteSetValue) -> Expression
    ) {
        val expressions = values.map { expressionExtractor(it.value!!) }
        if (expressions.isNotEmpty()) {
            val first = expressions.first()
            expressions.drop(1).forEach {
                assertEquals(first, it)
            }
        }
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
        val expectedAlternative2TrafficLineExpression = "[step, [line-progress], " +
            "[rgba, 0.0, 0.0, 0.0, 0.0], 0.0, [rgba, 134.0, 148.0, 165.0, 1.0], " +
            "0.09121273901463474, [rgba, 190.0, 160.0, 135.0, 1.0], 0.09968837805505427, " +
            "[rgba, 134.0, 148.0, 165.0, 1.0], 0.7428990170270018, " +
            "[rgba, 190.0, 160.0, 135.0, 1.0], 0.7533307965366008, " +
            "[rgba, 134.0, 148.0, 165.0, 1.0], 0.791100847370589, " +
            "[rgba, 190.0, 160.0, 135.0, 1.0], 0.8172213571475897, " +
            "[rgba, 134.0, 148.0, 165.0, 1.0], 0.8647489907014745, " +
            "[rgba, 190.0, 160.0, 135.0, 1.0], 0.8661754437401568, " +
            "[rgba, 134.0, 148.0, 165.0, 1.0], 0.8880749995256708, " +
            "[rgba, 181.0, 130.0, 129.0, 1.0], 0.92754943810966, [rgba, 134.0, 148.0, 165.0, 1.0]]"
        val options = MapboxRouteLineOptions.Builder(ctx).build()
        val api = MapboxRouteLineApi(options)
        val route = loadRoute("short_route.json")
        val altRoute1 = loadRoute("route-with-road-classes.txt")
        val altRoute2 = loadRoute("multileg_route.json")
        val routes = listOf(
            RouteLine(route, null),
            RouteLine(altRoute1, null),
            RouteLine(altRoute2, null)
        )

        val result = api.setRoutes(routes)

        assertEquals(
            expectedPrimaryTrafficLineExpression,
            result.value!!.primaryRouteLineData.dynamicData.trafficExpressionProvider!!
                .generateExpression().toString()
        )
        assertEquals(
            expectedAlternative1TrafficLineExpression,
            result.value!!.alternativeRouteLinesData[0].dynamicData.trafficExpressionProvider!!
                .generateExpression().toString()
        )
        assertEquals(
            expectedAlternative2TrafficLineExpression,
            result.value!!.alternativeRouteLinesData[1].dynamicData.trafficExpressionProvider!!
                .generateExpression().toString()
        )
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
        val route = loadRoute("short_route.json")
        val routes = listOf(RouteLine(route, null))
        api.setRoutes(routes)

        val result = api.getRouteDrawData()

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

    @Test
    fun updateTraveledRouteLine() = coroutineRule.runBlockingTest {
        val options = MapboxRouteLineOptions.Builder(ctx)
            .withVanishingRouteLineEnabled(true)
            .displayRestrictedRoadSections(false)
            .vanishingRouteLineUpdateInterval(0)
            .build()
        val api = MapboxRouteLineApi(options)
        val expectedCasingExpression = "[literal, [0.0, 0.3240769449298392]]"
        val expectedRouteExpression = "[literal, [0.0, 0.3240769449298392]]"
        val expectedTrafficExpression = "[literal, [0.0, 0.3240769449298392]]"
        val expectedRestrictedExpression = "[literal, [0.0, 0.3240769449298392]]"
        val route = loadRoute("short_route.json")
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

        assertEquals(
            expectedCasingExpression,
            result.value!!.primaryRouteLineDynamicData
                .casingExpressionProvider.generateExpression().toString()
        )
        assertEquals(
            expectedRouteExpression,
            result.value!!.primaryRouteLineDynamicData
                .baseExpressionProvider.generateExpression().toString()
        )
        assertEquals(
            expectedTrafficExpression,
            result.value!!
                .primaryRouteLineDynamicData.trafficExpressionProvider!!
                .generateExpression().toString()
        )
        assertEquals(
            expectedRestrictedExpression,
            result.value!!
                .primaryRouteLineDynamicData.restrictedSectionExpressionProvider!!
                .generateExpression().toString()
        )
    }

    @Test
    fun updateTraveledRouteLine_pointUpdateIntervalRespected() =
        coroutineRule.runBlockingTest {
            val options = MapboxRouteLineOptions.Builder(ctx)
                .withVanishingRouteLineEnabled(true)
                .displayRestrictedRoadSections(false)
                .vanishingRouteLineUpdateInterval(TimeUnit.MILLISECONDS.toNanos(1200))
                .build()
            val api = MapboxRouteLineApi(options)
            val route = loadRoute("short_route.json")
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

            pauseDispatcher {
                val result1 = api.updateTraveledRouteLine(lineString.coordinates()[1])
                assertTrue(result1.isValue)

                Thread.sleep(1000L)
                api.updateUpcomingRoutePointIndex(routeProgress) // only update the progress
                Thread.sleep(300L) // in summary we've waited for 1.3s since last point update
                val result2 = api.updateTraveledRouteLine(lineString.coordinates()[1])
                assertTrue(result2.isValue) // should succeed because threshold was 1.2s

                Thread.sleep(500L) // wait less than threshold
                val result3 = api.updateTraveledRouteLine(lineString.coordinates()[1])
                assertTrue(result3.isError)
            }
        }

    @Test
    fun updateTraveledRouteLine_whenRouteHasRestrictions() = coroutineRule.runBlockingTest {
        val options = MapboxRouteLineOptions.Builder(ctx)
            .withVanishingRouteLineEnabled(true)
            .displayRestrictedRoadSections(true)
            .vanishingRouteLineUpdateInterval(0)
            .build()
        val api = MapboxRouteLineApi(options)
        val expectedRestrictedExpression = "[literal, [0.0, 0.05416168943228483]]"
        val route = loadRoute("route-with-restrictions.json")
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

        assertEquals(
            expectedRestrictedExpression,
            result.value!!.primaryRouteLineDynamicData.restrictedSectionExpressionProvider!!
                .generateExpression().toString()
        )
    }

    @Test
    fun updateTraveledRouteLine_whenRouteRestrictionsEnabledButHasNone() =
        coroutineRule.runBlockingTest {
            val expectedCasingExpression = "[literal, [0.0, 0.3240769449298392]]"
            val expectedRouteExpression = "[literal, [0.0, 0.3240769449298392]]"
            val expectedTrafficExpression = "[literal, [0.0, 0.3240769449298392]]"
            val restrictedTrafficExpression = "[literal, [0.0, 0.3240769449298392]]"
            val options = MapboxRouteLineOptions.Builder(ctx)
                .withVanishingRouteLineEnabled(true)
                .displayRestrictedRoadSections(true)
                .vanishingRouteLineUpdateInterval(0)
                .build()
            val api = MapboxRouteLineApi(options)
            val route = loadRoute("short_route.json")
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

            assertEquals(
                expectedCasingExpression,
                result.value!!.primaryRouteLineDynamicData
                    .casingExpressionProvider.generateExpression().toString()
            )
            assertEquals(
                expectedRouteExpression,
                result.value!!.primaryRouteLineDynamicData
                    .baseExpressionProvider.generateExpression()
                    .toString()
            )
            assertEquals(
                expectedTrafficExpression,
                result.value!!
                    .primaryRouteLineDynamicData.trafficExpressionProvider!!
                    .generateExpression().toString()
            )
            assertEquals(
                restrictedTrafficExpression,
                result.value!!
                    .primaryRouteLineDynamicData.restrictedSectionExpressionProvider!!
                    .generateExpression().toString()
            )
        }

    @Test
    fun updateWithRouteProgress_whenDeEmphasizeInactiveLegSegments() =
        coroutineRule.runBlockingTest {
            val expectedTrafficExp = "[step, [line-progress], [rgba, 0.0, 0.0, 0.0, 0.0], 0.0," +
                " [rgba, 86.0, 168.0, 251.0, 1.0], 0.10338667841237215, " +
                "[rgba, 255.0, 149.0, 0.0, 1.0], 0.1235746096999951, " +
                "[rgba, 86.0, 168.0, 251.0, 1.0], 0.27090572440007177, " +
                "[rgba, 255.0, 149.0, 0.0, 1.0], 0.32147751186805656, " +
                "[rgba, 86.0, 168.0, 251.0, 1.0], 0.48807892461540975, [rgba, 0.0, 0.0, 0.0, 0.0]]"
            val realOptions = MapboxRouteLineOptions.Builder(ctx)
                .styleInactiveRouteLegsIndependently(true)
                .build()
            val route = loadRoute("multileg-route-two-legs.json")
            val mockVanishingRouteLine = mockk<VanishingRouteLine>(relaxUnitFun = true) {
                every { primaryRoutePoints } returns null
                every { vanishPointOffset } returns 0.0
                every { primaryRouteLineGranularDistances } returns null
            }
            val options = mockk<MapboxRouteLineOptions> {
                every { vanishingRouteLine } returns mockVanishingRouteLine
                every { resourceProvider } returns realOptions.resourceProvider
                every {
                    styleInactiveRouteLegsIndependently
                } returns realOptions.styleInactiveRouteLegsIndependently
                every { displayRestrictedRoadSections } returns false
                every { displaySoftGradientForTraffic } returns false
                every { softGradientTransition } returns 30.0
                every { routeStyleDescriptors } returns listOf()
            }
            val api = MapboxRouteLineApi(options)
            val routeProgress = mockk<RouteProgress> {
                every { currentLegProgress } returns mockk {
                    every { legIndex } returns 0
                    every { currentStepProgress } returns mockk {
                        every { stepPoints } returns PolylineUtils.decode(
                            route.legs()!![0].steps()!![0].geometry()!!,
                            6
                        )
                        every { distanceTraveled } returns 0f
                        every { step } returns mockk {
                            every { distance() } returns route.legs()!![0].steps()!![0].distance()
                        }
                        every { stepIndex } returns 0
                    }
                }
                every { currentState } returns RouteProgressState.TRACKING
            }
            api.updateVanishingPointState(RouteProgressState.TRACKING)
            api.setRoutes(listOf(RouteLine(route, null)))
            api.updateWithRouteProgress(routeProgress) {}

            val result = api.setVanishingOffset(0.0).value!!

            assertEquals(
                expectedTrafficExp,
                result.primaryRouteLineDynamicData
                    .trafficExpressionProvider!!.generateExpression().toString()
            )
        }

    @Test
    fun highlightActiveLeg() = coroutineRule.runBlockingTest {
        var callbackCalled = false
        val expectedTrafficExp = "[step, [line-progress], [rgba, 0.0, 0.0, 0.0, 0.0], 0.0," +
            " [rgba, 0.0, 0.0, 0.0, 0.0], 0.48807892461540975, [rgba, 255.0, 149.0, 0.0, 1.0]," +
            " 0.5402820600662328, [rgba, 86.0, 168.0, 251.0, 1.0], 0.5691365275126837, " +
            "[rgba, 255.0, 149.0, 0.0, 1.0], 0.589630336547089, [rgba, 86.0, 168.0, 251.0, 1.0]," +
            " 0.883680810453678, [rgba, 255.0, 149.0, 0.0, 1.0], 0.93904468262125, " +
            "[rgba, 86.0, 168.0, 251.0, 1.0]]"
        val expectedRouteLineExp = "[step, [line-progress], [rgba, 86.0, 168.0, 251.0, 1.0], 0.0," +
            " [rgba, 0.0, 0.0, 0.0, 0.0], 0.48807892461540975, [rgba, 86.0, 168.0, 251.0, 1.0]]"
        val expectedCasingExp = "[step, [line-progress], [rgba, 47.0, 122.0, 198.0, 1.0], 0.0," +
            " [rgba, 0.0, 0.0, 0.0, 0.0], 0.48807892461540975, [rgba, 47.0, 122.0, 198.0, 1.0]]"
        val route = loadRoute("multileg-route-two-legs.json")
        val options = MapboxRouteLineOptions.Builder(ctx)
            .styleInactiveRouteLegsIndependently(true)
            .build()
        val api = MapboxRouteLineApi(options)
        val routeProgress = mockk<RouteProgress> {
            every { currentLegProgress } returns mockk {
                every { legIndex } returns 1
                every { currentStepProgress } returns mockk {
                    every { stepPoints } returns PolylineUtils.decode(
                        route.legs()!![0].steps()!![0].geometry()!!,
                        6
                    )
                    every { distanceTraveled } returns 0f
                    every { step } returns mockk {
                        every { distance() } returns route.legs()!![0].steps()!![0].distance()
                    }
                    every { stepIndex } returns 0
                }
                every { currentState } returns RouteProgressState.UNCERTAIN
            }
        }
        api.setRoutes(listOf(RouteLine(route, null)))

        api.updateWithRouteProgress(routeProgress) { result ->
            assertEquals(
                expectedTrafficExp,
                result.value!!.primaryRouteLineDynamicData
                    .trafficExpressionProvider!!.generateExpression().toString()
            )
            assertEquals(
                expectedRouteLineExp,
                result.value!!
                    .primaryRouteLineDynamicData.baseExpressionProvider
                    .generateExpression().toString()
            )
            assertEquals(
                expectedCasingExp,
                result.value!!.primaryRouteLineDynamicData
                    .casingExpressionProvider.generateExpression().toString()
            )
            callbackCalled = true
        }
        assertTrue(callbackCalled)
    }

    @Test
    fun showRouteWithLegIndexHighlighted() = coroutineRule.runBlockingTest {
        val expectedTrafficExp = "[step, [line-progress], [rgba, 0.0, 0.0, 0.0, 0.0], 0.0," +
            " [rgba, 0.0, 0.0, 0.0, 0.0], 0.48807892461540975, [rgba, 255.0, 149.0, 0.0, 1.0]," +
            " 0.5402820600662328, [rgba, 86.0, 168.0, 251.0, 1.0], 0.5691365275126837, " +
            "[rgba, 255.0, 149.0, 0.0, 1.0], 0.589630336547089, [rgba, 86.0, 168.0, 251.0, 1.0]," +
            " 0.883680810453678, [rgba, 255.0, 149.0, 0.0, 1.0], 0.93904468262125, " +
            "[rgba, 86.0, 168.0, 251.0, 1.0]]"
        val expectedRouteLineExp = "[step, [line-progress], [rgba, 86.0, 168.0, 251.0, 1.0], 0.0," +
            " [rgba, 0.0, 0.0, 0.0, 0.0], 0.48807892461540975, [rgba, 86.0, 168.0, 251.0, 1.0]]"
        val expectedCasingExp = "[step, [line-progress], [rgba, 47.0, 122.0, 198.0, 1.0], 0.0," +
            " [rgba, 0.0, 0.0, 0.0, 0.0], 0.48807892461540975, [rgba, 47.0, 122.0, 198.0, 1.0]]"
        val route = loadRoute("multileg-route-two-legs.json")
        val options = MapboxRouteLineOptions.Builder(ctx)
            .styleInactiveRouteLegsIndependently(true)
            .build()
        val api = MapboxRouteLineApi(options)
        api.setRoutes(listOf(RouteLine(route, null)))

        val result = api.showRouteWithLegIndexHighlighted(1).value!!

        assertEquals(
            expectedTrafficExp,
            result.primaryRouteLineDynamicData
                .trafficExpressionProvider!!.generateExpression().toString()
        )
        assertEquals(
            expectedRouteLineExp,
            result.primaryRouteLineDynamicData
                .baseExpressionProvider.generateExpression().toString()
        )
        assertEquals(
            expectedCasingExp,
            result.primaryRouteLineDynamicData.casingExpressionProvider
                .generateExpression().toString()
        )
    }

    @Test
    fun setVanishingOffset() {
        val options = MapboxRouteLineOptions.Builder(ctx)
            .withVanishingRouteLineEnabled(true)
            .build()
        val trafficExpression = "[step, [line-progress], [rgba, 0.0, 0.0, 0.0, 0.0], 0.5," +
            " [rgba, 86.0, 168.0, 251.0, 1.0]]"
        val routeLineExpression = "[step, [line-progress], [rgba, 0.0, 0.0, 0.0, 0.0], 0.5," +
            " [rgba, 86.0, 168.0, 251.0, 1.0]]"
        val casingExpression = "[step, [line-progress], [rgba, 0.0, 0.0, 0.0, 0.0], 0.5," +
            " [rgba, 47.0, 122.0, 198.0, 1.0]]"

        val api = MapboxRouteLineApi(
            options
        )

        val result = api.setVanishingOffset(.5)

        assertEquals(
            trafficExpression,
            result.value!!.primaryRouteLineDynamicData
                .trafficExpressionProvider!!.generateExpression().toString()
        )
        assertEquals(
            routeLineExpression,
            result.value!!.primaryRouteLineDynamicData
                .baseExpressionProvider.generateExpression().toString()
        )
        assertEquals(
            casingExpression,
            result.value!!.primaryRouteLineDynamicData
                .casingExpressionProvider.generateExpression().toString()
        )
    }

    @Test
    fun setVanishingOffset_withRestrictionsEnabledPrimaryRouteNull() {
        val options = MapboxRouteLineOptions.Builder(ctx)
            .withVanishingRouteLineEnabled(true)
            .displayRestrictedRoadSections(true)
            .build()
        val trafficExpression = "[step, [line-progress], [rgba, 0.0, 0.0, 0.0, 0.0], 0.5," +
            " [rgba, 86.0, 168.0, 251.0, 1.0]]"
        val routeLineExpression = "[step, [line-progress], [rgba, 0.0, 0.0, 0.0, 0.0], 0.5," +
            " [rgba, 86.0, 168.0, 251.0, 1.0]]"
        val casingExpression = "[step, [line-progress], [rgba, 0.0, 0.0, 0.0, 0.0], 0.5," +
            " [rgba, 47.0, 122.0, 198.0, 1.0]]"

        val api = MapboxRouteLineApi(
            options
        )

        val result = api.setVanishingOffset(.5)

        assertEquals(
            trafficExpression,
            result.value!!.primaryRouteLineDynamicData
                .trafficExpressionProvider!!.generateExpression().toString()
        )
        assertEquals(
            routeLineExpression,
            result.value!!.primaryRouteLineDynamicData
                .baseExpressionProvider.generateExpression().toString()
        )
        assertEquals(
            casingExpression,
            result.value!!.primaryRouteLineDynamicData
                .casingExpressionProvider.generateExpression().toString()
        )
        assertNull(result.value!!.primaryRouteLineDynamicData.restrictedSectionExpressionProvider)
    }

    @Test
    fun setVanishingOffset_withRestrictionsEnabled() = coroutineRule.runBlockingTest {
        val options = MapboxRouteLineOptions.Builder(ctx)
            .withVanishingRouteLineEnabled(true)
            .displayRestrictedRoadSections(true)
            .build()
        val trafficExpression = "[step, [line-progress], [rgba, 0.0, 0.0, 0.0, 0.0], 0.5, " +
            "[rgba, 255.0, 149.0, 0.0, 1.0], 0.5032854217424586, [rgba, 86.0, 168.0, 251.0, 1.0]," +
            " 0.8610097571779957, [rgba, 255.0, 149.0, 0.0, 1.0], 0.8921736630023819, " +
            "[rgba, 86.0, 168.0, 251.0, 1.0]]"
        val routeLineExpression = "[step, [line-progress], [rgba, 0.0, 0.0, 0.0, 0.0], 0.5," +
            " [rgba, 86.0, 168.0, 251.0, 1.0]]"
        val casingExpression = "[step, [line-progress], [rgba, 0.0, 0.0, 0.0, 0.0], 0.5," +
            " [rgba, 47.0, 122.0, 198.0, 1.0]]"
        val restrictedExpression = "[step, [line-progress], [rgba, 0.0, 0.0, 0.0, 0.0], 0.5," +
            " [rgba, 0.0, 0.0, 0.0, 0.0], 0.5032854217424586, [rgba, 0.0, 0.0, 0.0, 1.0]," +
            " 0.5207714038134984, [rgba, 0.0, 0.0, 0.0, 0.0]]"
        val route = loadRoute("route-with-restrictions.json")

        val api = MapboxRouteLineApi(
            options
        )
        api.setRoutes(listOf(RouteLine(route, null)))

        val result = api.setVanishingOffset(.5)

        println(
            result.value!!.primaryRouteLineDynamicData
                .trafficExpressionProvider!!.generateExpression().toString()
        )
        assertEquals(
            trafficExpression,
            result.value!!.primaryRouteLineDynamicData
                .trafficExpressionProvider!!.generateExpression().toString()
        )
        assertEquals(
            routeLineExpression,
            result.value!!.primaryRouteLineDynamicData
                .baseExpressionProvider.generateExpression().toString()
        )
        assertEquals(
            casingExpression,
            result.value!!.primaryRouteLineDynamicData
                .casingExpressionProvider.generateExpression().toString()
        )
        assertEquals(
            restrictedExpression,
            result.value!!.primaryRouteLineDynamicData
                .restrictedSectionExpressionProvider!!.generateExpression().toString()
        )
    }

    @Test
    fun setVanishingOffset_whenHasRestrictionsButDisabled() = coroutineRule.runBlockingTest {
        val options = MapboxRouteLineOptions.Builder(ctx)
            .withVanishingRouteLineEnabled(true)
            .displayRestrictedRoadSections(false)
            .build()
        val trafficExpression = "[step, [line-progress], [rgba, 0.0, 0.0, 0.0, 0.0], 0.5, " +
            "[rgba, 255.0, 149.0, 0.0, 1.0], 0.5032854217424586, [rgba, 86.0, 168.0, 251.0, 1.0]," +
            " 0.8610097571779957, [rgba, 255.0, 149.0, 0.0, 1.0], 0.8921736630023819, " +
            "[rgba, 86.0, 168.0, 251.0, 1.0]]"
        val routeLineExpression = "[step, [line-progress], [rgba, 0.0, 0.0, 0.0, 0.0], 0.5," +
            " [rgba, 86.0, 168.0, 251.0, 1.0]]"
        val casingExpression = "[step, [line-progress], [rgba, 0.0, 0.0, 0.0, 0.0], 0.5," +
            " [rgba, 47.0, 122.0, 198.0, 1.0]]"
        val route = loadRoute("route-with-restrictions.json")

        val api = MapboxRouteLineApi(
            options
        )
        api.setRoutes(listOf(RouteLine(route, null)))

        val result = api.setVanishingOffset(.5)

        assertEquals(
            trafficExpression,
            result.value!!.primaryRouteLineDynamicData
                .trafficExpressionProvider!!.generateExpression().toString()
        )
        assertEquals(
            routeLineExpression,
            result.value!!.primaryRouteLineDynamicData
                .baseExpressionProvider.generateExpression().toString()
        )
        assertEquals(
            casingExpression,
            result.value!!.primaryRouteLineDynamicData
                .casingExpressionProvider.generateExpression().toString()
        )
        assertNull(result.value!!.primaryRouteLineDynamicData.restrictedSectionExpressionProvider)
    }

    @Test
    fun setRouteAsyncCallsReturnsCorrectRouteSuspend() = coroutineRule.runBlockingTest {
        val shortRoute = listOf(RouteLine(loadRoute("short_route.json"), null))
        val longRoute = listOf(RouteLine(loadRoute("cross-country-route.json"), null))
        val options = MapboxRouteLineOptions.Builder(ctx).build()
        val api = MapboxRouteLineApi(options)

        val longRouteDef = async {
            val result = api.setRoutes(longRoute)
            (
                result
                    .value!!
                    .primaryRouteLineData
                    .dynamicData
                    .trafficExpressionProvider!!
                    .generateExpression()
                    .contents as ArrayList<*>
                ).size
        }
        delay(40)
        val shortRouteDef = async {
            val result = api.setRoutes(shortRoute)
            (
                result
                    .value!!
                    .primaryRouteLineData
                    .dynamicData
                    .trafficExpressionProvider!!
                    .generateExpression()
                    .contents as ArrayList<*>
                ).size
        }

        assertEquals(7, shortRouteDef.await())
        assertEquals(625, longRouteDef.await())
    }

    @Test
    fun alternativelyStyleSegmentsNotInLeg() = coroutineRule.runBlockingTest {
        val colorOptions = RouteLineColorResources.Builder()
            .inActiveRouteLegsColor(Color.YELLOW)
            .build()
        val resources = RouteLineResources.Builder().routeLineColorResources(colorOptions).build()
        val options = MapboxRouteLineOptions.Builder(ctx).withRouteLineResources(resources).build()
        val route = loadRoute("multileg-route-two-legs.json")
        val segments = MapboxRouteLineUtils.calculateRouteLineSegments(
            route,
            listOf(),
            true,
            options.resourceProvider.routeLineColorResources
        )
        val api = MapboxRouteLineApi(options)

        val result = api.alternativelyStyleSegmentsNotInLeg(1, segments)

        assertEquals(11, result.size)
        assertEquals(-256, result.first().segmentColor)
        assertEquals(0, result.first().legIndex)
        assertEquals(-256, result[4].segmentColor)
        assertEquals(0, result[4].legIndex)
        assertEquals(-27392, result[5].segmentColor)
        assertEquals(1, result[5].legIndex)
    }

    @Test
    fun `set routes - with alternative metadata - vanished until deviation point`() =
        coroutineRule.runBlockingTest {
            val options = MapboxRouteLineOptions.Builder(ctx).build()
            val api = MapboxRouteLineApi(options)
            val response = DirectionsResponse.fromJson(
                FileUtils.loadJsonFixture(
                    "route_response_alternative_start.json"
                )
            )
            val routeOptions = response.routes().first().routeOptions()!!
            val routes = NavigationRoute.create(
                directionsResponse = DirectionsResponse.fromJson(
                    FileUtils.loadJsonFixture(
                        "route_response_alternative_start.json"
                    )
                ),
                routeOptions = routeOptions
            )
            val alternativeRouteMetadata = mockk<AlternativeRouteMetadata> {
                every { navigationRoute } returns routes[1]
                every { forkIntersectionOfAlternativeRoute } returns mockk {
                    every { location } returns mockk()
                    every { geometryIndexInRoute } returns 2
                    every { geometryIndexInLeg } returns 2
                    every { legIndex } returns 0
                }
                every { forkIntersectionOfPrimaryRoute } returns mockk {
                    every { location } returns mockk()
                    every { geometryIndexInRoute } returns 2
                    every { geometryIndexInLeg } returns 2
                    every { legIndex } returns 0
                }
                every { infoFromFork } returns mockk {
                    every { distance } returns 1588.7698034413877
                    every { duration } returns 372.0307335579433
                }
                every { infoFromStartOfPrimary } returns mockk {
                    every { distance } returns 1652.4918669972706
                    every { duration } returns 400.6847335579434
                }
            }

            val result = api.setNavigationRoutes(routes, listOf(alternativeRouteMetadata)).value!!

            assertEquals(
                "[step, [line-progress], [rgba, 0.0, 0.0, 0.0, 0.0], 0.0, " +
                    "[rgba, 86.0, 168.0, 251.0, 1.0]]",
                result
                    .primaryRouteLineData
                    .dynamicData
                    .trafficExpressionProvider!!
                    .generateExpression().toString()
            )
            assertEquals(
                "[step, [line-progress], [rgba, 0.0, 0.0, 0.0, 0.0], " +
                    "0.038547771702788815, [rgba, 134.0, 148.0, 165.0, 1.0]]",
                result
                    .alternativeRouteLinesData[0]
                    .dynamicData
                    .baseExpressionProvider
                    .generateExpression().toString()
            )
            assertEquals(
                "[step, [line-progress], [rgba, 0.0, 0.0, 0.0, 0.0], " +
                    "0.038547771702788815, [rgba, 114.0, 126.0, 141.0, 1.0]]",
                result
                    .alternativeRouteLinesData[0]
                    .dynamicData
                    .casingExpressionProvider
                    .generateExpression().toString()
            )
            assertEquals(
                "[step, [line-progress], [rgba, 0.0, 0.0, 0.0, 0.0], " +
                    "0.038547771702788815, [rgba, 134.0, 148.0, 165.0, 1.0], " +
                    "0.6557962353895171, [rgba, 190.0, 160.0, 135.0, 1.0], " +
                    "0.7379144868819574, [rgba, 134.0, 148.0, 165.0, 1.0]]",
                result
                    .alternativeRouteLinesData[0]
                    .dynamicData
                    .trafficExpressionProvider!!
                    .generateExpression().toString()
            )
        }

    @Test
    fun getAlternativeRoutesDeviationOffsetsTest() {
        val routeData = FileUtils.loadJsonFixture("route_response_alternative_start.json")
        val response = DirectionsResponse.fromJson(routeData)
        val routeOptions = response.routes().first().routeOptions()!!
        val routes = NavigationRoute.create(
            directionsResponse = DirectionsResponse.fromJson(
                routeData
            ),
            routeOptions = routeOptions
        )
        val alternativeRouteMetadata = mockk<AlternativeRouteMetadata> {
            every { navigationRoute } returns routes[1]
            every { forkIntersectionOfAlternativeRoute } returns mockk {
                every { location } returns mockk()
                every { geometryIndexInRoute } returns 2
                every { geometryIndexInLeg } returns 2
                every { legIndex } returns 0
            }
            every { forkIntersectionOfPrimaryRoute } returns mockk {
                every { location } returns mockk()
                every { geometryIndexInRoute } returns 2
                every { geometryIndexInLeg } returns 2
                every { legIndex } returns 0
            }
            every { infoFromFork } returns mockk {
                every { distance } returns 1588.7698034413877
                every { duration } returns 372.0307335579433
            }
            every { infoFromStartOfPrimary } returns mockk {
                every { distance } returns 1652.4918669972706
                every { duration } returns 400.6847335579434
            }
        }

        val result = MapboxRouteLineUtils.getAlternativeRoutesDeviationOffsets(
            listOf(alternativeRouteMetadata)
        )

        assertEquals(1, result.size)
        assertEquals(0.038547771702788815, result["1"])
    }
}
