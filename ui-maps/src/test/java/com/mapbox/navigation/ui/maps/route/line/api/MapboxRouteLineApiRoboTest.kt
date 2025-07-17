package com.mapbox.navigation.ui.maps.route.line.api

import android.content.Context
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.Value
import com.mapbox.core.constants.Constants
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.geojson.utils.PolylineUtils
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.core.internal.LowMemoryManager
import com.mapbox.navigation.core.routealternatives.AlternativeRouteMetadata
import com.mapbox.navigation.testing.FileUtils
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.testing.NativeRouteParserRule
import com.mapbox.navigation.testing.factories.createNavigationRoutes
import com.mapbox.navigation.testing.factories.createWaypoint
import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineUtils
import com.mapbox.navigation.ui.maps.internal.route.line.toData
import com.mapbox.navigation.ui.maps.route.line.MapboxRouteLineApiExtensions.clearRouteLine
import com.mapbox.navigation.ui.maps.route.line.MapboxRouteLineApiExtensions.getRouteDrawData
import com.mapbox.navigation.ui.maps.route.line.MapboxRouteLineApiExtensions.setNavigationRouteLines
import com.mapbox.navigation.ui.maps.route.line.MapboxRouteLineApiExtensions.setNavigationRoutes
import com.mapbox.navigation.ui.maps.route.line.RouteLineHistoryRecordingApiSender
import com.mapbox.navigation.ui.maps.route.line.model.InactiveRouteColors
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineApiOptions
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineViewOptions
import com.mapbox.navigation.ui.maps.route.line.model.NavigationRouteLine
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineColorResources
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineDistancesIndex
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineError
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineGranularDistances
import com.mapbox.navigation.ui.maps.route.line.model.RouteSetValue
import com.mapbox.navigation.ui.maps.route.line.model.SegmentColorType
import com.mapbox.navigation.ui.maps.route.line.model.VanishingPointState
import com.mapbox.navigation.ui.maps.testing.TestResponse
import com.mapbox.navigation.ui.maps.testing.TestRoute
import com.mapbox.navigation.ui.maps.testing.TestingUtil.loadNavigationRoute
import com.mapbox.navigation.utils.internal.InternalJobControlFactory
import com.mapbox.navigation.utils.internal.JobControl
import com.mapbox.navigation.utils.internal.LoggerFrontend
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.pauseDispatcher
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

    private val logger = mockk<LoggerFrontend>(relaxed = true)

    @get:Rule
    val loggerRule = LoggingFrontendTestRule(logger)

    @get:Rule
    val nativeRouteParserRule = NativeRouteParserRule()

    private val shortRoute by lazy { TestRoute(fileName = "short_route.json") }
    private val routeWithRestrictions by lazy {
        TestRoute(fileName = "route-with-restrictions.json")
    }
    private val multiLegRouteTwoLegs by lazy {
        TestRoute(fileName = "multileg-route-two-legs.json")
    }
    private val multiLegRouteWithOverlap by lazy {
        TestRoute(fileName = "multileg_route_with_overlap.json")
    }
    private val multilegRouteWithOverlapAndAllCongestionLevels by lazy {
        TestRoute(fileName = "multileg_route_with_overlap_all_congestion.json")
    }
    private val threeLegRoute by lazy {
        TestResponse(
            fileName = "three_leg_route_response.json",
            routeOptions = RouteOptions.builder()
                .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
                .coordinates(
                    "11.56731,48.19105;" +
                        "11.5713133,48.1935488;11.5736816,48.1922755;" +
                        "11.5725295,48.1938032",
                )
                .steps(true)
                .overview("full")
                .geometries("polyline6")
                .enableRefresh(true)
                .annotations("distance,duration,congestion_numeric")
                .build(),
        )
    }

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
        mockkObject(InternalJobControlFactory)
        every {
            InternalJobControlFactory.createDefaultScopeJobControl()
        } answers {
            val defaultScope = coroutineRule.createTestScope()
            JobControl(defaultScope.coroutineContext.job, defaultScope)
        }
        every {
            InternalJobControlFactory.createImmediateMainScopeJobControl()
        } answers {
            val defaultScope = coroutineRule.createTestScope()
            JobControl(defaultScope.coroutineContext.job, defaultScope)
        }

        mockkObject(LowMemoryManager.Companion)
        every { LowMemoryManager.create() } returns mockk(relaxed = true)
    }

    @After
    fun cleanUp() {
        unmockkObject(InternalJobControlFactory)
        unmockkObject(LowMemoryManager.Companion)
    }

    @Test
    fun `set empty routes`() =
        coroutineRule.runBlockingTest {
            mockkObject(MapboxRouteLineUtils)
            var callbackCalled = false
            val apiOptions = MapboxRouteLineApiOptions.Builder()
                .build()
            val api = MapboxRouteLineApi(apiOptions)
            api.setNavigationRoutes(
                emptyList(),
            ) {
                runBlocking {
                    val result = it.value!!
                    val emptyGeoJsonSource = FeatureCollection.fromFeatures(emptyList())
                    assertEquals(emptyGeoJsonSource, result.primaryRouteLineData.featureCollection)
                    assertNull(result.primaryRouteLineData.dynamicData)
                    result.alternativeRouteLinesData.forEach {
                        assertEquals(emptyGeoJsonSource, it.featureCollection)
                    }
                    assertEquals(emptyGeoJsonSource, result.waypointsSource)
                    assertNull(result.routeLineMaskingLayerDynamicData)

                    callbackCalled = true
                }
            }

            assertTrue(callbackCalled)
            unmockkObject(MapboxRouteLineUtils)
        }

    @Test
    fun `setNewRoutes when styleInactiveRouteLegsIndependently true and vanishing route line enabled for leg 0`() =
        coroutineRule.runBlockingTest {
            mockkObject(MapboxRouteLineUtils)
            var callbackCalled = false
            val expectedTrafficExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 5.0, 0.0, 0.0, 1.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 5.0, 0.0, 0.0, 1.0]"),
                DoubleChecker(0.05157338744369842),
                StringChecker("[rgba, 4.0, 0.0, 0.0, 1.0]"),
                DoubleChecker(0.13481026827990547),
                StringChecker("[rgba, 3.0, 0.0, 0.0, 1.0]"),
                DoubleChecker(0.33639164971786856),
                StringChecker("[rgba, 2.0, 0.0, 0.0, 1.0]"),
                DoubleChecker(0.39703761698904767),
                StringChecker("[rgba, 1.0, 0.0, 0.0, 1.0]"),
                DoubleChecker(0.4933703471135659),
                StringChecker("[rgba, 6.0, 0.0, 0.0, 1.0]"),
                DoubleChecker(0.5021970255051508),
                StringChecker("[rgba, 0.0, 5.0, 0.0, 1.0]"),
                DoubleChecker(0.5077761805810133),
                StringChecker("[rgba, 0.0, 4.0, 0.0, 1.0]"),
                DoubleChecker(0.5177996298254119),
                StringChecker("[rgba, 0.0, 3.0, 0.0, 1.0]"),
                DoubleChecker(0.5453612048835845),
                StringChecker("[rgba, 0.0, 2.0, 0.0, 1.0]"),
                DoubleChecker(0.5657670973901471),
                StringChecker("[rgba, 0.0, 1.0, 0.0, 1.0]"),
                DoubleChecker(0.9393568422283114),
                StringChecker("[rgba, 0.0, 6.0, 0.0, 1.0]"),
            )
            val expectedBaseExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 0.0, 5.0, 1.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 0.0, 5.0, 1.0]"),
                DoubleChecker(0.5021971),
                StringChecker("[rgba, 0.0, 0.0, 1.0, 1.0]"),
            )
            val expectedCasingExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 7.0, 0.0, 0.0, 1.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 7.0, 0.0, 0.0, 1.0]"),
                DoubleChecker(0.5021971),
                StringChecker("[rgba, 0.0, 0.0, 2.0, 1.0]"),
            )
            val expectedTrailExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
                DoubleChecker(0.5021971),
                StringChecker("[rgba, 0.0, 0.0, 3.0, 1.0]"),
            )
            val expectedTrailCasingExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
                DoubleChecker(0.5021971),
                StringChecker("[rgba, 0.0, 0.0, 4.0, 1.0]"),
            )
            val expectedMaskingTrafficExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
                DoubleChecker(0.5021970255051508),
                StringChecker("[rgba, 0.0, 5.0, 0.0, 1.0]"),
                DoubleChecker(0.5077761805810133),
                StringChecker("[rgba, 0.0, 4.0, 0.0, 1.0]"),
                DoubleChecker(0.5177996298254119),
                StringChecker("[rgba, 0.0, 3.0, 0.0, 1.0]"),
                DoubleChecker(0.5453612048835845),
                StringChecker("[rgba, 0.0, 2.0, 0.0, 1.0]"),
                DoubleChecker(0.5657670973901471),
                StringChecker("[rgba, 0.0, 1.0, 0.0, 1.0]"),
                DoubleChecker(0.9393568422283114),
                StringChecker("[rgba, 0.0, 6.0, 0.0, 1.0]"),
            )
            val expectedMaskingBaseExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
                DoubleChecker(0.5021971),
                StringChecker("[rgba, 0.0, 0.0, 1.0, 1.0]"),
            )
            val expectedTrailMaskingExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
                DoubleChecker(0.5021971),
                StringChecker("[rgba, 0.0, 0.0, 3.0, 1.0]"),
            )
            val expectedCasingMaskingExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
                DoubleChecker(0.5021971),
                StringChecker("[rgba, 0.0, 0.0, 2.0, 1.0]"),
            )
            val expectedTrailCasingMaskingExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
                DoubleChecker(0.5021971),
                StringChecker("[rgba, 0.0, 0.0, 4.0, 1.0]"),
            )

            val colors = RouteLineColorResources.Builder()
                .routeDefaultColor(Color.argb(255, 0, 0, 1))
                .routeCasingColor(Color.argb(255, 0, 0, 2))
                .routeLineTraveledColor(Color.argb(255, 0, 0, 3))
                .routeLineTraveledCasingColor(Color.argb(255, 0, 0, 4))
                .inActiveRouteLegsColor(Color.argb(255, 0, 0, 5))
                .routeLowCongestionColor(Color.argb(255, 0, 1, 0))
                .routeModerateCongestionColor(Color.argb(255, 0, 2, 0))
                .routeHeavyCongestionColor(Color.argb(255, 0, 3, 0))
                .routeSevereCongestionColor(Color.argb(255, 0, 4, 0))
                .routeUnknownCongestionColor(Color.argb(255, 0, 5, 0))
                .routeClosureColor(Color.argb(255, 0, 6, 0))
                .inactiveRouteLegLowCongestionColor(Color.argb(255, 1, 0, 0))
                .inactiveRouteLegModerateCongestionColor(Color.argb(255, 2, 0, 0))
                .inactiveRouteLegHeavyCongestionColor(Color.argb(255, 3, 0, 0))
                .inactiveRouteLegSevereCongestionColor(Color.argb(255, 4, 0, 0))
                .inactiveRouteLegUnknownCongestionColor(Color.argb(255, 5, 0, 0))
                .inactiveRouteLegClosureColor(Color.argb(255, 6, 0, 0))
                .inactiveRouteLegCasingColor(Color.argb(255, 7, 0, 0))
                .build()
            val viewOptions = MapboxRouteLineViewOptions.Builder(ctx)
                .routeLineColorResources(colors)
                .build()
                .toData()
            val apiOptions = MapboxRouteLineApiOptions.Builder()
                .vanishingRouteLineEnabled(true)
                .styleInactiveRouteLegsIndependently(true)
                .build()
            val api = MapboxRouteLineApi(apiOptions)
            api.setNavigationRoutes(
                listOf(multilegRouteWithOverlapAndAllCongestionLevels.navigationRoute),
            ) {
                runBlocking {
                    val result = it.value!!
                    val trafficMaskingExpression = getAppliedExpression(
                        result.routeLineMaskingLayerDynamicData!!.trafficExpressionCommandHolder!!,
                        viewOptions,
                        "line-gradient",
                    )
                    val baseMaskingExpression = getAppliedExpression(
                        result.routeLineMaskingLayerDynamicData.baseExpressionCommandHolder,
                        viewOptions,
                        "line-gradient",
                    )
                    val casingMaskingExpression = getAppliedExpression(
                        result.routeLineMaskingLayerDynamicData.casingExpressionCommandHolder,
                        viewOptions,
                        "line-gradient",
                    )
                    val trailMaskingExpression = getAppliedExpression(
                        result.routeLineMaskingLayerDynamicData.trailExpressionCommandHolder!!,
                        viewOptions,
                        "line-gradient",
                    )
                    val trailCasingMaskingExpression = getAppliedExpression(
                        result.routeLineMaskingLayerDynamicData
                            .trailCasingExpressionCommandHolder!!,
                        viewOptions,
                        "line-gradient",
                    )

                    val trafficExpression = getAppliedExpression(
                        result.primaryRouteLineData.dynamicData!!.trafficExpressionCommandHolder!!,
                        viewOptions,
                        "line-gradient",
                    )
                    val baseExpression = getAppliedExpression(
                        result.primaryRouteLineData.dynamicData.baseExpressionCommandHolder,
                        viewOptions,
                        "line-gradient",
                    )
                    val casingExpression = getAppliedExpression(
                        result.primaryRouteLineData.dynamicData.casingExpressionCommandHolder,
                        viewOptions,
                        "line-gradient",
                    )
                    val trailExpression = getAppliedExpression(
                        result.primaryRouteLineData.dynamicData.trailExpressionCommandHolder!!,
                        viewOptions,
                        "line-gradient",
                    )
                    val trailCasingExpression = getAppliedExpression(
                        result.primaryRouteLineData.dynamicData
                            .trailCasingExpressionCommandHolder!!,
                        viewOptions,
                        "line-gradient",
                    )

                    checkExpression(
                        expectedMaskingTrafficExpressionContents,
                        trafficMaskingExpression,
                    )
                    checkExpression(
                        expectedMaskingBaseExpressionContents,
                        baseMaskingExpression,
                    )
                    checkExpression(
                        expectedCasingMaskingExpressionContents,
                        casingMaskingExpression,
                    )
                    checkExpression(
                        expectedTrailMaskingExpressionContents,
                        trailMaskingExpression,
                    )
                    checkExpression(
                        expectedTrailCasingMaskingExpressionContents,
                        trailCasingMaskingExpression,
                    )

                    checkExpression(expectedTrafficExpressionContents, trafficExpression)
                    checkExpression(expectedBaseExpressionContents, baseExpression)
                    checkExpression(expectedCasingExpressionContents, casingExpression)
                    checkExpression(expectedTrailExpressionContents, trailExpression)
                    checkExpression(
                        expectedTrailCasingExpressionContents,
                        trailCasingExpression,
                    )

                    val primaryLineData = result.primaryRouteLineData.dynamicData!!
                    assertTrue(
                        primaryLineData.trafficExpressionCommandHolder!!.provider
                        is HeavyRouteLineValueProvider,
                    )
                    assertTrue(
                        primaryLineData.baseExpressionCommandHolder.provider
                        is LightRouteLineValueProvider,
                    )
                    assertTrue(
                        primaryLineData.casingExpressionCommandHolder.provider
                        is LightRouteLineValueProvider,
                    )
                    assertTrue(
                        primaryLineData.trailExpressionCommandHolder!!.provider
                        is LightRouteLineValueProvider,
                    )
                    assertTrue(
                        primaryLineData.trailCasingExpressionCommandHolder!!.provider
                        is LightRouteLineValueProvider,
                    )

                    val maskingData = result.routeLineMaskingLayerDynamicData
                    assertTrue(
                        maskingData.trafficExpressionCommandHolder!!.provider
                        is LightRouteLineValueProvider,
                    )
                    assertTrue(
                        maskingData.baseExpressionCommandHolder.provider
                        is LightRouteLineValueProvider,
                    )
                    assertTrue(
                        maskingData.casingExpressionCommandHolder.provider
                        is LightRouteLineValueProvider,
                    )
                    assertTrue(
                        maskingData.trailExpressionCommandHolder!!.provider
                        is LightRouteLineValueProvider,
                    )
                    assertTrue(
                        maskingData.trailCasingExpressionCommandHolder!!.provider
                        is LightRouteLineValueProvider,
                    )

                    callbackCalled = true
                }
            }

            assertTrue(callbackCalled)
            unmockkObject(MapboxRouteLineUtils)
        }

    @Test
    fun `updateWithRouteProgress when styleInactiveRouteLegsIndependently true and vanishing route line enabled for leg 0`() =
        coroutineRule.runBlockingTest {
            mockkObject(MapboxRouteLineUtils)
            var callbackCalled = false
            val expectedTrafficExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 5.0, 0.0, 0.0, 1.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 5.0, 0.0, 0.0, 1.0]"),
                DoubleChecker(0.05157338744369842),
                StringChecker("[rgba, 4.0, 0.0, 0.0, 1.0]"),
                DoubleChecker(0.13481026827990547),
                StringChecker("[rgba, 3.0, 0.0, 0.0, 1.0]"),
                DoubleChecker(0.33639164971786856),
                StringChecker("[rgba, 2.0, 0.0, 0.0, 1.0]"),
                DoubleChecker(0.39703761698904767),
                StringChecker("[rgba, 1.0, 0.0, 0.0, 1.0]"),
                DoubleChecker(0.4933703471135659),
                StringChecker("[rgba, 6.0, 0.0, 0.0, 1.0]"),
                DoubleChecker(0.5021970255051508),
                StringChecker("[rgba, 0.0, 5.0, 0.0, 1.0]"),
                DoubleChecker(0.5077761805810133),
                StringChecker("[rgba, 0.0, 4.0, 0.0, 1.0]"),
                DoubleChecker(0.5177996298254119),
                StringChecker("[rgba, 0.0, 3.0, 0.0, 1.0]"),
                DoubleChecker(0.5453612048835845),
                StringChecker("[rgba, 0.0, 2.0, 0.0, 1.0]"),
                DoubleChecker(0.5657670973901471),
                StringChecker("[rgba, 0.0, 1.0, 0.0, 1.0]"),
                DoubleChecker(0.9393568422283114),
                StringChecker("[rgba, 0.0, 6.0, 0.0, 1.0]"),
            )
            val expectedBaseExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 0.0, 5.0, 1.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 0.0, 5.0, 1.0]"),
                DoubleChecker(0.5021971),
                StringChecker("[rgba, 0.0, 0.0, 1.0, 1.0]"),
            )
            val expectedCasingExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 7.0, 0.0, 0.0, 1.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 7.0, 0.0, 0.0, 1.0]"),
                DoubleChecker(0.5021971),
                StringChecker("[rgba, 0.0, 0.0, 2.0, 1.0]"),
            )
            val expectedTrailExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
                DoubleChecker(0.5021971),
                StringChecker("[rgba, 0.0, 0.0, 3.0, 1.0]"),
            )
            val expectedTrailCasingExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
                DoubleChecker(0.5021971),
                StringChecker("[rgba, 0.0, 0.0, 4.0, 1.0]"),
            )
            val expectedMaskingTrafficExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
                DoubleChecker(0.5021970255051508),
                StringChecker("[rgba, 0.0, 5.0, 0.0, 1.0]"),
                DoubleChecker(0.5077761805810133),
                StringChecker("[rgba, 0.0, 4.0, 0.0, 1.0]"),
                DoubleChecker(0.5177996298254119),
                StringChecker("[rgba, 0.0, 3.0, 0.0, 1.0]"),
                DoubleChecker(0.5453612048835845),
                StringChecker("[rgba, 0.0, 2.0, 0.0, 1.0]"),
                DoubleChecker(0.5657670973901471),
                StringChecker("[rgba, 0.0, 1.0, 0.0, 1.0]"),
                DoubleChecker(0.9393568422283114),
                StringChecker("[rgba, 0.0, 6.0, 0.0, 1.0]"),
            )
            val expectedMaskingBaseExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
                DoubleChecker(0.5021971),
                StringChecker("[rgba, 0.0, 0.0, 1.0, 1.0]"),
            )
            val expectedTrailMaskingExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
                DoubleChecker(0.5021971),
                StringChecker("[rgba, 0.0, 0.0, 3.0, 1.0]"),
            )
            val expectedCasingMaskingExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
                DoubleChecker(0.5021971),
                StringChecker("[rgba, 0.0, 0.0, 2.0, 1.0]"),
            )
            val expectedTrailCasingMaskingExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
                DoubleChecker(0.5021971),
                StringChecker("[rgba, 0.0, 0.0, 4.0, 1.0]"),
            )

            val colors = RouteLineColorResources.Builder()
                .routeDefaultColor(Color.argb(255, 0, 0, 1))
                .routeCasingColor(Color.argb(255, 0, 0, 2))
                .routeLineTraveledColor(Color.argb(255, 0, 0, 3))
                .routeLineTraveledCasingColor(Color.argb(255, 0, 0, 4))
                .inActiveRouteLegsColor(Color.argb(255, 0, 0, 5))
                .routeLowCongestionColor(Color.argb(255, 0, 1, 0))
                .routeModerateCongestionColor(Color.argb(255, 0, 2, 0))
                .routeHeavyCongestionColor(Color.argb(255, 0, 3, 0))
                .routeSevereCongestionColor(Color.argb(255, 0, 4, 0))
                .routeUnknownCongestionColor(Color.argb(255, 0, 5, 0))
                .routeClosureColor(Color.argb(255, 0, 6, 0))
                .inactiveRouteLegLowCongestionColor(Color.argb(255, 1, 0, 0))
                .inactiveRouteLegModerateCongestionColor(Color.argb(255, 2, 0, 0))
                .inactiveRouteLegHeavyCongestionColor(Color.argb(255, 3, 0, 0))
                .inactiveRouteLegSevereCongestionColor(Color.argb(255, 4, 0, 0))
                .inactiveRouteLegUnknownCongestionColor(Color.argb(255, 5, 0, 0))
                .inactiveRouteLegClosureColor(Color.argb(255, 6, 0, 0))
                .inactiveRouteLegCasingColor(Color.argb(255, 7, 0, 0))
                .build()
            val viewOptions = MapboxRouteLineViewOptions.Builder(ctx)
                .routeLineColorResources(colors)
                .build()
                .toData()
            val apiOptions = MapboxRouteLineApiOptions.Builder()
                .vanishingRouteLineEnabled(true)
                .styleInactiveRouteLegsIndependently(true)
                .build()
            val api = MapboxRouteLineApi(apiOptions)
            api.setNavigationRoutes(
                listOf(multilegRouteWithOverlapAndAllCongestionLevels.navigationRoute),
                -1,
            )

            val routeProgress = mockRouteProgress(
                multilegRouteWithOverlapAndAllCongestionLevels.navigationRoute,
            )
            every { routeProgress.currentLegProgress!!.legIndex } returns 0
            every { routeProgress.currentRouteGeometryIndex } returns 0
            api.updateWithRouteProgress(routeProgress) {
                runBlocking {
                    val result = it.value!!
                    val trafficExpression = getAppliedExpression(
                        result.primaryRouteLineDynamicData!!.trafficExpressionCommandHolder!!,
                        viewOptions,
                        "line-gradient",
                    )
                    val baseExpression = getAppliedExpression(
                        result.primaryRouteLineDynamicData.baseExpressionCommandHolder,
                        viewOptions,
                        "line-gradient",
                    )
                    val casingExpression = getAppliedExpression(
                        result.primaryRouteLineDynamicData.casingExpressionCommandHolder,
                        viewOptions,
                        "line-gradient",
                    )
                    val trailExpression = getAppliedExpression(
                        result.primaryRouteLineDynamicData.trailExpressionCommandHolder!!,
                        viewOptions,
                        "line-gradient",
                    )
                    val trailCasingExpression = getAppliedExpression(
                        result.primaryRouteLineDynamicData
                            .trailCasingExpressionCommandHolder!!,
                        viewOptions,
                        "line-gradient",
                    )
                    val trafficMaskingExpression = getAppliedExpression(
                        result.routeLineMaskingLayerDynamicData!!.trafficExpressionCommandHolder!!,
                        viewOptions,
                        "line-gradient",
                    )
                    val baseMaskingExpression = getAppliedExpression(
                        result.routeLineMaskingLayerDynamicData.baseExpressionCommandHolder,
                        viewOptions,
                        "line-gradient",
                    )
                    val casingMaskingExpression = getAppliedExpression(
                        result.routeLineMaskingLayerDynamicData.casingExpressionCommandHolder,
                        viewOptions,
                        "line-gradient",
                    )
                    val trailMaskingExpression = getAppliedExpression(
                        result.routeLineMaskingLayerDynamicData.trailExpressionCommandHolder!!,
                        viewOptions,
                        "line-gradient",
                    )
                    val trailCasingMaskingExpression = getAppliedExpression(
                        result.routeLineMaskingLayerDynamicData
                            .trailCasingExpressionCommandHolder!!,
                        viewOptions,
                        "line-gradient",
                    )

                    checkExpression(
                        expectedTrafficExpressionContents,
                        trafficExpression,
                    )
                    checkExpression(expectedBaseExpressionContents, baseExpression)
                    checkExpression(
                        expectedCasingExpressionContents,
                        casingExpression,
                    )
                    checkExpression(expectedTrailExpressionContents, trailExpression)
                    checkExpression(
                        expectedTrailCasingExpressionContents,
                        trailCasingExpression,
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

                    val routeData = result.primaryRouteLineDynamicData
                    assertTrue(
                        routeData.trafficExpressionCommandHolder!!.provider
                        is HeavyRouteLineValueProvider,
                    )
                    assertTrue(
                        routeData.baseExpressionCommandHolder.provider
                        is LightRouteLineValueProvider,
                    )
                    assertTrue(
                        routeData.casingExpressionCommandHolder.provider
                        is LightRouteLineValueProvider,
                    )
                    assertTrue(
                        routeData.trailExpressionCommandHolder!!.provider
                        is LightRouteLineValueProvider,
                    )
                    assertTrue(
                        routeData.trailCasingExpressionCommandHolder!!.provider
                        is LightRouteLineValueProvider,
                    )

                    val maskingData = result.routeLineMaskingLayerDynamicData
                    assertTrue(
                        maskingData.trafficExpressionCommandHolder!!.provider
                        is LightRouteLineValueProvider,
                    )
                    assertTrue(
                        maskingData.baseExpressionCommandHolder.provider
                        is LightRouteLineValueProvider,
                    )
                    assertTrue(
                        maskingData.casingExpressionCommandHolder.provider
                        is LightRouteLineValueProvider,
                    )
                    assertTrue(
                        maskingData.trailExpressionCommandHolder!!.provider
                        is LightRouteLineValueProvider,
                    )
                    assertTrue(
                        maskingData.trailCasingExpressionCommandHolder!!.provider
                        is LightRouteLineValueProvider,
                    )

                    callbackCalled = true
                }
            }

            assertTrue(callbackCalled)
            unmockkObject(MapboxRouteLineUtils)
        }

    @Test
    fun `setNewRouteData multileg route when styleInactiveRouteLegsIndependently true and vanishing route line enabled for leg 1`() =
        coroutineRule.runBlockingTest {
            mockkObject(MapboxRouteLineUtils)
            var callbackCalled = false
            val expectedTrafficExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 5.0, 0.0, 1.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 5.0, 0.0, 1.0]"),
                DoubleChecker(0.0515733874436984),
                StringChecker("[rgba, 0.0, 4.0, 0.0, 1.0]"),
                DoubleChecker(0.134810268279905),
                StringChecker("[rgba, 0.0, 3.0, 0.0, 1.0]"),
                DoubleChecker(0.336391649717869),
                StringChecker("[rgba, 0.0, 2.0, 0.0, 1.0]"),
                DoubleChecker(0.397037616989048),
                StringChecker("[rgba, 0.0, 1.0, 0.0, 1.0]"),
                DoubleChecker(0.493370347113566),
                StringChecker("[rgba, 0.0, 6.0, 0.0, 1.0]"),
                DoubleChecker(0.502197025505151),
                StringChecker("[rgba, 5.0, 0.0, 0.0, 1.0]"),
                DoubleChecker(0.507776180581013),
                StringChecker("[rgba, 4.0, 0.0, 0.0, 1.0]"),
                DoubleChecker(0.517799629825412),
                StringChecker("[rgba, 3.0, 0.0, 0.0, 1.0]"),
                DoubleChecker(0.545361204883585),
                StringChecker("[rgba, 2.0, 0.0, 0.0, 1.0]"),
                DoubleChecker(0.565767097390147),
                StringChecker("[rgba, 1.0, 0.0, 0.0, 1.0]"),
                DoubleChecker(0.939356842228311),
                StringChecker("[rgba, 6.0, 0.0, 0.0, 1.0]"),
            )
            val expectedBaseExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 0.0, 1.0, 1.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 0.0, 1.0, 1.0]"),
                DoubleChecker(0.5021971),
                StringChecker("[rgba, 0.0, 0.0, 5.0, 1.0]"),
            )
            val expectedCasingExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 0.0, 2.0, 1.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 0.0, 2.0, 1.0]"),
                DoubleChecker(0.5021971),
                StringChecker("[rgba, 7.0, 0.0, 0.0, 1.0]"),
            )
            val expectedTrailExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 0.0, 3.0, 1.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 0.0, 3.0, 1.0]"),
            )
            val expectedTrailCasingExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 0.0, 4.0, 1.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 0.0, 4.0, 1.0]"),
            )
            val expectedMaskingTrafficExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 5.0, 0.0, 1.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 5.0, 0.0, 1.0]"),
                DoubleChecker(0.0515733874436984),
                StringChecker("[rgba, 0.0, 4.0, 0.0, 1.0]"),
                DoubleChecker(0.134810268279905),
                StringChecker("[rgba, 0.0, 3.0, 0.0, 1.0]"),
                DoubleChecker(0.336391649717869),
                StringChecker("[rgba, 0.0, 2.0, 0.0, 1.0]"),
                DoubleChecker(0.397037616989048),
                StringChecker("[rgba, 0.0, 1.0, 0.0, 1.0]"),
                DoubleChecker(0.493370347113566),
                StringChecker("[rgba, 0.0, 6.0, 0.0, 1.0]"),
                DoubleChecker(0.502197025505151),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            )
            val expectedMaskingBaseExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 0.0, 1.0, 1.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 0.0, 1.0, 1.0]"),
                DoubleChecker(0.5021971),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            )
            val expectedTrailMaskingExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 0.0, 3.0, 1.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 0.0, 3.0, 1.0]"),
                DoubleChecker(0.5021971),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            )
            val expectedCasingMaskingExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 0.0, 2.0, 1.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 0.0, 2.0, 1.0]"),
                DoubleChecker(0.502198),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            )
            val expectedTrailCasingMaskingExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 0.0, 4.0, 1.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 0.0, 4.0, 1.0]"),
                DoubleChecker(0.5021971),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            )

            val colors = RouteLineColorResources.Builder()
                .routeDefaultColor(Color.argb(255, 0, 0, 1))
                .routeCasingColor(Color.argb(255, 0, 0, 2))
                .routeLineTraveledColor(Color.argb(255, 0, 0, 3))
                .routeLineTraveledCasingColor(Color.argb(255, 0, 0, 4))
                .inActiveRouteLegsColor(Color.argb(255, 0, 0, 5))
                .routeLowCongestionColor(Color.argb(255, 0, 1, 0))
                .routeModerateCongestionColor(Color.argb(255, 0, 2, 0))
                .routeHeavyCongestionColor(Color.argb(255, 0, 3, 0))
                .routeSevereCongestionColor(Color.argb(255, 0, 4, 0))
                .routeUnknownCongestionColor(Color.argb(255, 0, 5, 0))
                .routeClosureColor(Color.argb(255, 0, 6, 0))
                .inactiveRouteLegLowCongestionColor(Color.argb(255, 1, 0, 0))
                .inactiveRouteLegModerateCongestionColor(Color.argb(255, 2, 0, 0))
                .inactiveRouteLegHeavyCongestionColor(Color.argb(255, 3, 0, 0))
                .inactiveRouteLegSevereCongestionColor(Color.argb(255, 4, 0, 0))
                .inactiveRouteLegUnknownCongestionColor(Color.argb(255, 5, 0, 0))
                .inactiveRouteLegClosureColor(Color.argb(255, 6, 0, 0))
                .inactiveRouteLegCasingColor(Color.argb(255, 7, 0, 0))
                .build()
            val viewOptions = MapboxRouteLineViewOptions.Builder(ctx)
                .routeLineColorResources(colors)
                .build()
                .toData()
            val apiOptions = MapboxRouteLineApiOptions.Builder()
                .vanishingRouteLineEnabled(true)
                .styleInactiveRouteLegsIndependently(true)
                .build()
            val api = MapboxRouteLineApi(apiOptions)
            api.setNavigationRoutes(
                listOf(multilegRouteWithOverlapAndAllCongestionLevels.navigationRoute),
                1,
            ) {
                runBlocking {
                    val result = it.value!!
                    val trafficMaskingExpression = getAppliedExpression(
                        result.routeLineMaskingLayerDynamicData!!
                            .trafficExpressionCommandHolder!!,
                        viewOptions,
                        "line-gradient",
                    )
                    val baseMaskingExpression = getAppliedExpression(
                        result.routeLineMaskingLayerDynamicData!!
                            .baseExpressionCommandHolder,
                        viewOptions,
                        "line-gradient",
                    )
                    val casingMaskingExpression = getAppliedExpression(
                        result.routeLineMaskingLayerDynamicData!!
                            .casingExpressionCommandHolder,
                        viewOptions,
                        "line-gradient",
                    )
                    val trailMaskingExpression = getAppliedExpression(
                        result.routeLineMaskingLayerDynamicData!!
                            .trailExpressionCommandHolder!!,
                        viewOptions,
                        "line-gradient",
                    )
                    val trailCasingMaskingExpression = getAppliedExpression(
                        result.routeLineMaskingLayerDynamicData!!
                            .trailCasingExpressionCommandHolder!!,
                        viewOptions,
                        "line-gradient",
                    )

                    val trafficExpression = getAppliedExpression(
                        result.primaryRouteLineData.dynamicData!!
                            .trafficExpressionCommandHolder!!,
                        viewOptions,
                        "line-gradient",
                    )
                    val baseExpression = getAppliedExpression(
                        result.primaryRouteLineData.dynamicData
                            .baseExpressionCommandHolder,
                        viewOptions,
                        "line-gradient",
                    )
                    val casingExpression = getAppliedExpression(
                        result.primaryRouteLineData.dynamicData
                            .casingExpressionCommandHolder,
                        viewOptions,
                        "line-gradient",
                    )
                    val trailExpression = getAppliedExpression(
                        result.primaryRouteLineData.dynamicData
                            .trailExpressionCommandHolder!!,
                        viewOptions,
                        "line-gradient",
                    )
                    val trailCasingExpression = getAppliedExpression(
                        result.primaryRouteLineData.dynamicData
                            .trailCasingExpressionCommandHolder!!,
                        viewOptions,
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

                    checkExpression(expectedTrafficExpressionContents, trafficExpression)
                    checkExpression(expectedBaseExpressionContents, baseExpression)
                    checkExpression(expectedCasingExpressionContents, casingExpression)
                    checkExpression(expectedTrailExpressionContents, trailExpression)
                    checkExpression(expectedTrailCasingExpressionContents, trailCasingExpression)

                    val primaryLineData = result.primaryRouteLineData.dynamicData
                    assertTrue(
                        primaryLineData.trafficExpressionCommandHolder!!.provider
                        is HeavyRouteLineValueProvider,
                    )
                    assertTrue(
                        primaryLineData.baseExpressionCommandHolder.provider
                        is LightRouteLineValueProvider,
                    )
                    assertTrue(
                        primaryLineData.casingExpressionCommandHolder.provider
                        is LightRouteLineValueProvider,
                    )
                    assertTrue(
                        primaryLineData.trailExpressionCommandHolder!!.provider
                        is LightRouteLineValueProvider,
                    )
                    assertTrue(
                        primaryLineData.trailCasingExpressionCommandHolder!!.provider
                        is LightRouteLineValueProvider,
                    )

                    val maskingData = result.routeLineMaskingLayerDynamicData
                    assertTrue(
                        maskingData.trafficExpressionCommandHolder!!.provider
                        is LightRouteLineValueProvider,
                    )
                    assertTrue(
                        maskingData.baseExpressionCommandHolder.provider
                        is LightRouteLineValueProvider,
                    )
                    assertTrue(
                        maskingData.casingExpressionCommandHolder.provider
                        is LightRouteLineValueProvider,
                    )
                    assertTrue(
                        maskingData.trailExpressionCommandHolder!!.provider
                        is LightRouteLineValueProvider,
                    )
                    assertTrue(
                        maskingData.trailCasingExpressionCommandHolder!!.provider
                        is LightRouteLineValueProvider,
                    )

                    callbackCalled = true
                }
            }

            assertTrue(callbackCalled)
            unmockkObject(MapboxRouteLineUtils)
        }

    @Test
    fun `updateWithRouteProgress multileg route when styleInactiveRouteLegsIndependently true and vanishing route line enabled for leg 1`() =
        coroutineRule.runBlockingTest {
            mockkObject(MapboxRouteLineUtils)
            var callbackCalled = false
            val expectedTrafficExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 5.0, 0.0, 1.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 5.0, 0.0, 1.0]"),
                DoubleChecker(0.0515733874436984),
                StringChecker("[rgba, 0.0, 4.0, 0.0, 1.0]"),
                DoubleChecker(0.134810268279905),
                StringChecker("[rgba, 0.0, 3.0, 0.0, 1.0]"),
                DoubleChecker(0.336391649717869),
                StringChecker("[rgba, 0.0, 2.0, 0.0, 1.0]"),
                DoubleChecker(0.397037616989048),
                StringChecker("[rgba, 0.0, 1.0, 0.0, 1.0]"),
                DoubleChecker(0.493370347113566),
                StringChecker("[rgba, 0.0, 6.0, 0.0, 1.0]"),
                DoubleChecker(0.502197025505151),
                StringChecker("[rgba, 5.0, 0.0, 0.0, 1.0]"),
                DoubleChecker(0.507776180581013),
                StringChecker("[rgba, 4.0, 0.0, 0.0, 1.0]"),
                DoubleChecker(0.517799629825412),
                StringChecker("[rgba, 3.0, 0.0, 0.0, 1.0]"),
                DoubleChecker(0.545361204883585),
                StringChecker("[rgba, 2.0, 0.0, 0.0, 1.0]"),
                DoubleChecker(0.565767097390147),
                StringChecker("[rgba, 1.0, 0.0, 0.0, 1.0]"),
                DoubleChecker(0.939356842228311),
                StringChecker("[rgba, 6.0, 0.0, 0.0, 1.0]"),
            )
            val expectedBaseExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 0.0, 1.0, 1.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 0.0, 1.0, 1.0]"),
                DoubleChecker(0.5021971),
                StringChecker("[rgba, 0.0, 0.0, 5.0, 1.0]"),
            )
            val expectedCasingExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 0.0, 2.0, 1.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 0.0, 2.0, 1.0]"),
                DoubleChecker(0.5021971),
                StringChecker("[rgba, 7.0, 0.0, 0.0, 1.0]"),
            )
            val expectedTrailExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 0.0, 3.0, 1.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 0.0, 3.0, 1.0]"),
            )
            val expectedTrailCasingExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 0.0, 4.0, 1.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 0.0, 4.0, 1.0]"),
            )
            val expectedMaskingTrafficExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 5.0, 0.0, 1.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 5.0, 0.0, 1.0]"),
                DoubleChecker(0.0515733874436984),
                StringChecker("[rgba, 0.0, 4.0, 0.0, 1.0]"),
                DoubleChecker(0.134810268279905),
                StringChecker("[rgba, 0.0, 3.0, 0.0, 1.0]"),
                DoubleChecker(0.336391649717869),
                StringChecker("[rgba, 0.0, 2.0, 0.0, 1.0]"),
                DoubleChecker(0.397037616989048),
                StringChecker("[rgba, 0.0, 1.0, 0.0, 1.0]"),
                DoubleChecker(0.493370347113566),
                StringChecker("[rgba, 0.0, 6.0, 0.0, 1.0]"),
                DoubleChecker(0.502197025505151),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            )
            val expectedMaskingBaseExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 0.0, 1.0, 1.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 0.0, 1.0, 1.0]"),
                DoubleChecker(0.5021971),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            )
            val expectedTrailMaskingExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 0.0, 3.0, 1.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 0.0, 3.0, 1.0]"),
                DoubleChecker(0.5021971),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            )
            val expectedCasingMaskingExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 0.0, 2.0, 1.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 0.0, 2.0, 1.0]"),
                DoubleChecker(0.502198),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            )
            val expectedTrailCasingMaskingExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 0.0, 4.0, 1.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 0.0, 4.0, 1.0]"),
                DoubleChecker(0.5021971),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            )

            val colors = RouteLineColorResources.Builder()
                .routeDefaultColor(Color.argb(255, 0, 0, 1))
                .routeCasingColor(Color.argb(255, 0, 0, 2))
                .routeLineTraveledColor(Color.argb(255, 0, 0, 3))
                .routeLineTraveledCasingColor(Color.argb(255, 0, 0, 4))
                .inActiveRouteLegsColor(Color.argb(255, 0, 0, 5))
                .routeLowCongestionColor(Color.argb(255, 0, 1, 0))
                .routeModerateCongestionColor(Color.argb(255, 0, 2, 0))
                .routeHeavyCongestionColor(Color.argb(255, 0, 3, 0))
                .routeSevereCongestionColor(Color.argb(255, 0, 4, 0))
                .routeUnknownCongestionColor(Color.argb(255, 0, 5, 0))
                .routeClosureColor(Color.argb(255, 0, 6, 0))
                .inactiveRouteLegLowCongestionColor(Color.argb(255, 1, 0, 0))
                .inactiveRouteLegModerateCongestionColor(Color.argb(255, 2, 0, 0))
                .inactiveRouteLegHeavyCongestionColor(Color.argb(255, 3, 0, 0))
                .inactiveRouteLegSevereCongestionColor(Color.argb(255, 4, 0, 0))
                .inactiveRouteLegUnknownCongestionColor(Color.argb(255, 5, 0, 0))
                .inactiveRouteLegClosureColor(Color.argb(255, 6, 0, 0))
                .inactiveRouteLegCasingColor(Color.argb(255, 7, 0, 0))
                .build()
            val viewOptions = MapboxRouteLineViewOptions.Builder(ctx)
                .routeLineColorResources(colors)
                .build()
                .toData()
            val apiOptions = MapboxRouteLineApiOptions.Builder()
                .vanishingRouteLineEnabled(true)
                .styleInactiveRouteLegsIndependently(true)
                .build()
            val api = MapboxRouteLineApi(apiOptions)
            val routeProgress = mockRouteProgress(
                multilegRouteWithOverlapAndAllCongestionLevels.navigationRoute,
            )
            every { routeProgress.currentLegProgress!!.legIndex } returns 1
            every { routeProgress.currentRouteGeometryIndex } returns 43
            api.setNavigationRoutes(
                listOf(multilegRouteWithOverlapAndAllCongestionLevels.navigationRoute),
            )

            api.updateWithRouteProgress(routeProgress) {
                runBlocking {
                    val result = it.value!!

                    val trafficExpression = getAppliedExpression(
                        result.primaryRouteLineDynamicData!!
                            .trafficExpressionCommandHolder!!,
                        viewOptions,
                        "line-gradient",
                    )
                    val baseExpression = getAppliedExpression(
                        result.primaryRouteLineDynamicData
                            .baseExpressionCommandHolder,
                        viewOptions,
                        "line-gradient",
                    )
                    val casingExpression = getAppliedExpression(
                        result.primaryRouteLineDynamicData
                            .casingExpressionCommandHolder,
                        viewOptions,
                        "line-gradient",
                    )
                    val trailExpression = getAppliedExpression(
                        result.primaryRouteLineDynamicData
                            .trailExpressionCommandHolder!!,
                        viewOptions,
                        "line-gradient",
                    )
                    val trailCasingExpression = getAppliedExpression(
                        result.primaryRouteLineDynamicData
                            .trailCasingExpressionCommandHolder!!,
                        viewOptions,
                        "line-gradient",
                    )

                    val trafficMaskingExpression = getAppliedExpression(
                        result.routeLineMaskingLayerDynamicData!!
                            .trafficExpressionCommandHolder!!,
                        viewOptions,
                        "line-gradient",
                    )
                    val baseMaskingExpression = getAppliedExpression(
                        result.routeLineMaskingLayerDynamicData!!
                            .baseExpressionCommandHolder,
                        viewOptions,
                        "line-gradient",
                    )
                    val casingMaskingExpression = getAppliedExpression(
                        result.routeLineMaskingLayerDynamicData!!
                            .casingExpressionCommandHolder,
                        viewOptions,
                        "line-gradient",
                    )
                    val trailMaskingExpression = getAppliedExpression(
                        result.routeLineMaskingLayerDynamicData!!
                            .trailExpressionCommandHolder!!,
                        viewOptions,
                        "line-gradient",
                    )
                    val trailCasingMaskingExpression = getAppliedExpression(
                        result.routeLineMaskingLayerDynamicData!!
                            .trailCasingExpressionCommandHolder!!,
                        viewOptions,
                        "line-gradient",
                    )

                    checkExpression(expectedTrafficExpressionContents, trafficExpression)
                    checkExpression(expectedBaseExpressionContents, baseExpression)
                    checkExpression(expectedCasingExpressionContents, casingExpression)
                    checkExpression(expectedTrailExpressionContents, trailExpression)
                    checkExpression(expectedTrailCasingExpressionContents, trailCasingExpression)

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

                    val primaryLineData = result.primaryRouteLineDynamicData!!
                    assertTrue(
                        primaryLineData.trafficExpressionCommandHolder!!.provider
                        is HeavyRouteLineValueProvider,
                    )
                    assertTrue(
                        primaryLineData.baseExpressionCommandHolder.provider
                        is LightRouteLineValueProvider,
                    )
                    assertTrue(
                        primaryLineData.casingExpressionCommandHolder.provider
                        is LightRouteLineValueProvider,
                    )
                    assertTrue(
                        primaryLineData.trailExpressionCommandHolder!!.provider
                        is LightRouteLineValueProvider,
                    )
                    assertTrue(
                        primaryLineData.trailCasingExpressionCommandHolder!!.provider
                        is LightRouteLineValueProvider,
                    )

                    val maskingData = result.routeLineMaskingLayerDynamicData
                    assertTrue(
                        maskingData.trafficExpressionCommandHolder!!.provider
                        is LightRouteLineValueProvider,
                    )
                    assertTrue(
                        maskingData.baseExpressionCommandHolder.provider
                        is LightRouteLineValueProvider,
                    )
                    assertTrue(
                        maskingData.casingExpressionCommandHolder.provider
                        is LightRouteLineValueProvider,
                    )
                    assertTrue(
                        maskingData.trailExpressionCommandHolder!!.provider
                        is LightRouteLineValueProvider,
                    )
                    assertTrue(
                        maskingData.trailCasingExpressionCommandHolder!!.provider
                        is LightRouteLineValueProvider,
                    )

                    callbackCalled = true
                }
            }

            assertTrue(callbackCalled)
            unmockkObject(MapboxRouteLineUtils)
        }

    @Test
    fun `setNewRoutesData multileg route when styleInactiveRouteLegsIndependently true and vanishing route line disabled route leg 0`() =
        coroutineRule.runBlockingTest {
            mockkObject(MapboxRouteLineUtils)
            var callbackCalled = false
            val expectedTrafficExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 5.0, 0.0, 0.0, 1.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 5.0, 0.0, 0.0, 1.0]"),
                DoubleChecker(0.0515733874436984),
                StringChecker("[rgba, 4.0, 0.0, 0.0, 1.0]"),
                DoubleChecker(0.134810268279905),
                StringChecker("[rgba, 3.0, 0.0, 0.0, 1.0]"),
                DoubleChecker(0.336391649717869),
                StringChecker("[rgba, 2.0, 0.0, 0.0, 1.0]"),
                DoubleChecker(0.397037616989048),
                StringChecker("[rgba, 1.0, 0.0, 0.0, 1.0]"),
                DoubleChecker(0.493370347113566),
                StringChecker("[rgba, 6.0, 0.0, 0.0, 1.0]"),
                DoubleChecker(0.502197025505151),
                StringChecker("[rgba, 0.0, 5.0, 0.0, 1.0]"),
                DoubleChecker(0.507776180581013),
                StringChecker("[rgba, 0.0, 4.0, 0.0, 1.0]"),
                DoubleChecker(0.517799629825412),
                StringChecker("[rgba, 0.0, 3.0, 0.0, 1.0]"),
                DoubleChecker(0.545361204883585),
                StringChecker("[rgba, 0.0, 2.0, 0.0, 1.0]"),
                DoubleChecker(0.565767097390147),
                StringChecker("[rgba, 0.0, 1.0, 0.0, 1.0]"),
                DoubleChecker(0.939356842228311),
                StringChecker("[rgba, 0.0, 6.0, 0.0, 1.0]"),
            )
            val expectedBaseExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 0.0, 5.0, 1.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 0.0, 5.0, 1.0]"),
                DoubleChecker(0.5021971),
                StringChecker("[rgba, 0.0, 0.0, 1.0, 1.0]"),
            )
            val expectedCasingExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 7.0, 0.0, 0.0, 1.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 7.0, 0.0, 0.0, 1.0]"),
                DoubleChecker(0.5021971),
                StringChecker("[rgba, 0.0, 0.0, 2.0, 1.0]"),
            )
            val expectedTrailExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
                DoubleChecker(0.5021971),
                StringChecker("[rgba, 0.0, 0.0, 3.0, 1.0]"),
            )
            val expectedTrailCasingExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
                DoubleChecker(0.5021971),
                StringChecker("[rgba, 0.0, 0.0, 4.0, 1.0]"),
            )
            val expectedMaskingTrafficExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
                DoubleChecker(0.502197025505151),
                StringChecker("[rgba, 0.0, 5.0, 0.0, 1.0]"),
                DoubleChecker(0.507776180581013),
                StringChecker("[rgba, 0.0, 4.0, 0.0, 1.0]"),
                DoubleChecker(0.517799629825412),
                StringChecker("[rgba, 0.0, 3.0, 0.0, 1.0]"),
                DoubleChecker(0.545361204883585),
                StringChecker("[rgba, 0.0, 2.0, 0.0, 1.0]"),
                DoubleChecker(0.565767097390147),
                StringChecker("[rgba, 0.0, 1.0, 0.0, 1.0]"),
                DoubleChecker(0.939356842228311),
                StringChecker("[rgba, 0.0, 6.0, 0.0, 1.0]"),
            )
            val expectedMaskingBaseExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
                DoubleChecker(0.5021971),
                StringChecker("[rgba, 0.0, 0.0, 1.0, 1.0]"),
            )
            val expectedTrailMaskingExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
                DoubleChecker(0.5021971),
                StringChecker("[rgba, 0.0, 0.0, 3.0, 1.0]"),
            )
            val expectedCasingMaskingExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
                DoubleChecker(0.502198),
                StringChecker("[rgba, 0.0, 0.0, 2.0, 1.0]"),
            )
            val expectedTrailCasingMaskingExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
                DoubleChecker(0.5021971),
                StringChecker("[rgba, 0.0, 0.0, 4.0, 1.0]"),
            )

            val colors = RouteLineColorResources.Builder()
                .routeDefaultColor(Color.argb(255, 0, 0, 1))
                .routeCasingColor(Color.argb(255, 0, 0, 2))
                .routeLineTraveledColor(Color.argb(255, 0, 0, 3))
                .routeLineTraveledCasingColor(Color.argb(255, 0, 0, 4))
                .inActiveRouteLegsColor(Color.argb(255, 0, 0, 5))
                .routeLowCongestionColor(Color.argb(255, 0, 1, 0))
                .routeModerateCongestionColor(Color.argb(255, 0, 2, 0))
                .routeHeavyCongestionColor(Color.argb(255, 0, 3, 0))
                .routeSevereCongestionColor(Color.argb(255, 0, 4, 0))
                .routeUnknownCongestionColor(Color.argb(255, 0, 5, 0))
                .routeClosureColor(Color.argb(255, 0, 6, 0))
                .inactiveRouteLegLowCongestionColor(Color.argb(255, 1, 0, 0))
                .inactiveRouteLegModerateCongestionColor(Color.argb(255, 2, 0, 0))
                .inactiveRouteLegHeavyCongestionColor(Color.argb(255, 3, 0, 0))
                .inactiveRouteLegSevereCongestionColor(Color.argb(255, 4, 0, 0))
                .inactiveRouteLegUnknownCongestionColor(Color.argb(255, 5, 0, 0))
                .inactiveRouteLegClosureColor(Color.argb(255, 6, 0, 0))
                .inactiveRouteLegCasingColor(Color.argb(255, 7, 0, 0))
                .build()
            val viewOptions = MapboxRouteLineViewOptions.Builder(ctx)
                .routeLineColorResources(colors)
                .build()
                .toData()
            val apiOptions = MapboxRouteLineApiOptions.Builder()
                .vanishingRouteLineEnabled(false)
                .styleInactiveRouteLegsIndependently(true)
                .build()
            val api = MapboxRouteLineApi(apiOptions)
            api.setNavigationRoutes(
                listOf(multilegRouteWithOverlapAndAllCongestionLevels.navigationRoute),
            ) {
                runBlocking {
                    val result = it.value!!
                    val trafficExpression = getAppliedExpression(
                        result.primaryRouteLineData.dynamicData!!
                            .trafficExpressionCommandHolder!!,
                        viewOptions,
                        "line-gradient",
                    )
                    val baseExpression = getAppliedExpression(
                        result.primaryRouteLineData.dynamicData
                            .baseExpressionCommandHolder,
                        viewOptions,
                        "line-gradient",
                    )
                    val casingExpression = getAppliedExpression(
                        result.primaryRouteLineData.dynamicData
                            .casingExpressionCommandHolder,
                        viewOptions,
                        "line-gradient",
                    )
                    val trailExpression = getAppliedExpression(
                        result.primaryRouteLineData.dynamicData
                            .trailExpressionCommandHolder!!,
                        viewOptions,
                        "line-gradient",
                    )
                    val trailCasingExpression = getAppliedExpression(
                        result.primaryRouteLineData.dynamicData
                            .trailCasingExpressionCommandHolder!!,
                        viewOptions,
                        "line-gradient",
                    )

                    checkExpression(
                        expectedMaskingTrafficExpressionContents,
                        getAppliedExpression(
                            result.routeLineMaskingLayerDynamicData!!
                                .trafficExpressionCommandHolder!!,
                            viewOptions,
                            "line-gradient",
                        ),
                    )
                    checkExpression(
                        expectedMaskingBaseExpressionContents,
                        getAppliedExpression(
                            result.routeLineMaskingLayerDynamicData!!
                                .baseExpressionCommandHolder,
                            viewOptions,
                            "line-gradient",
                        ),
                    )
                    checkExpression(
                        expectedCasingMaskingExpressionContents,
                        getAppliedExpression(
                            result.routeLineMaskingLayerDynamicData!!
                                .casingExpressionCommandHolder,
                            viewOptions,
                            "line-gradient",
                        ),
                    )
                    checkExpression(
                        expectedTrailMaskingExpressionContents,
                        getAppliedExpression(
                            result.routeLineMaskingLayerDynamicData!!
                                .trailExpressionCommandHolder!!,
                            viewOptions,
                            "line-gradient",
                        ),
                    )
                    checkExpression(
                        expectedTrailCasingMaskingExpressionContents,
                        getAppliedExpression(
                            result.routeLineMaskingLayerDynamicData!!
                                .trailCasingExpressionCommandHolder!!,
                            viewOptions,
                            "line-gradient",
                        ),
                    )
                    checkExpression(expectedTrafficExpressionContents, trafficExpression)
                    checkExpression(expectedBaseExpressionContents, baseExpression)
                    checkExpression(expectedCasingExpressionContents, casingExpression)
                    checkExpression(expectedTrailExpressionContents, trailExpression)
                    checkExpression(expectedTrailCasingExpressionContents, trailCasingExpression)

                    val primaryLineData = result.primaryRouteLineData.dynamicData
                    assertTrue(
                        primaryLineData.trafficExpressionCommandHolder!!.provider
                        is HeavyRouteLineValueProvider,
                    )
                    assertTrue(
                        primaryLineData.baseExpressionCommandHolder.provider
                        is LightRouteLineValueProvider,
                    )
                    assertTrue(
                        primaryLineData.casingExpressionCommandHolder.provider
                        is LightRouteLineValueProvider,
                    )
                    assertTrue(
                        primaryLineData.trailExpressionCommandHolder!!.provider
                        is LightRouteLineValueProvider,
                    )
                    assertTrue(
                        primaryLineData.trailCasingExpressionCommandHolder!!.provider
                        is LightRouteLineValueProvider,
                    )

                    val maskingData = result.routeLineMaskingLayerDynamicData
                    assertTrue(
                        maskingData.trafficExpressionCommandHolder!!.provider
                        is LightRouteLineValueProvider,
                    )
                    assertTrue(
                        maskingData.baseExpressionCommandHolder.provider
                        is LightRouteLineValueProvider,
                    )
                    assertTrue(
                        maskingData.casingExpressionCommandHolder.provider
                        is LightRouteLineValueProvider,
                    )
                    assertTrue(
                        maskingData.trailExpressionCommandHolder!!.provider
                        is LightRouteLineValueProvider,
                    )
                    assertTrue(
                        maskingData.trailCasingExpressionCommandHolder!!.provider
                        is LightRouteLineValueProvider,
                    )

                    callbackCalled = true
                }
            }

            assertTrue(callbackCalled)
            unmockkObject(MapboxRouteLineUtils)
        }

    @Test
    fun `setNewRoutesData multileg route when styleInactiveRouteLegsIndependently true and vanishing route line disabled route leg 1`() =
        coroutineRule.runBlockingTest {
            mockkObject(MapboxRouteLineUtils)
            var callbackCalled = false
            val expectedTrafficExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 5.0, 0.0, 1.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 5.0, 0.0, 1.0]"),
                DoubleChecker(0.0515733874436984),
                StringChecker("[rgba, 0.0, 4.0, 0.0, 1.0]"),
                DoubleChecker(0.134810268279905),
                StringChecker("[rgba, 0.0, 3.0, 0.0, 1.0]"),
                DoubleChecker(0.336391649717869),
                StringChecker("[rgba, 0.0, 2.0, 0.0, 1.0]"),
                DoubleChecker(0.397037616989048),
                StringChecker("[rgba, 0.0, 1.0, 0.0, 1.0]"),
                DoubleChecker(0.493370347113566),
                StringChecker("[rgba, 0.0, 6.0, 0.0, 1.0]"),
                DoubleChecker(0.502197025505151),
                StringChecker("[rgba, 5.0, 0.0, 0.0, 1.0]"),
                DoubleChecker(0.507776180581013),
                StringChecker("[rgba, 4.0, 0.0, 0.0, 1.0]"),
                DoubleChecker(0.517799629825412),
                StringChecker("[rgba, 3.0, 0.0, 0.0, 1.0]"),
                DoubleChecker(0.545361204883585),
                StringChecker("[rgba, 2.0, 0.0, 0.0, 1.0]"),
                DoubleChecker(0.565767097390147),
                StringChecker("[rgba, 1.0, 0.0, 0.0, 1.0]"),
                DoubleChecker(0.939356842228311),
                StringChecker("[rgba, 6.0, 0.0, 0.0, 1.0]"),
            )
            val expectedBaseExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 0.0, 1.0, 1.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 0.0, 1.0, 1.0]"),
                DoubleChecker(0.5021971),
                StringChecker("[rgba, 0.0, 0.0, 5.0, 1.0]"),
            )
            val expectedCasingExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 0.0, 2.0, 1.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 0.0, 2.0, 1.0]"),
                DoubleChecker(0.5021971),
                StringChecker("[rgba, 7.0, 0.0, 0.0, 1.0]"),
            )
            val expectedTrailExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 0.0, 3.0, 1.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 0.0, 3.0, 1.0]"),
                DoubleChecker(0.5021971),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            )
            val expectedTrailCasingExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 0.0, 4.0, 1.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 0.0, 4.0, 1.0]"),
                DoubleChecker(0.5021971),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            )

            val expectedMaskingTrafficExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 5.0, 0.0, 1.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 5.0, 0.0, 1.0]"),
                DoubleChecker(0.0515733874436984),
                StringChecker("[rgba, 0.0, 4.0, 0.0, 1.0]"),
                DoubleChecker(0.134810268279905),
                StringChecker("[rgba, 0.0, 3.0, 0.0, 1.0]"),
                DoubleChecker(0.336391649717869),
                StringChecker("[rgba, 0.0, 2.0, 0.0, 1.0]"),
                DoubleChecker(0.397037616989048),
                StringChecker("[rgba, 0.0, 1.0, 0.0, 1.0]"),
                DoubleChecker(0.493370347113566),
                StringChecker("[rgba, 0.0, 6.0, 0.0, 1.0]"),
                DoubleChecker(0.502197025505151),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            )
            val expectedMaskingBaseExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 0.0, 1.0, 1.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 0.0, 1.0, 1.0]"),
                DoubleChecker(0.5021971),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            )
            val expectedTrailMaskingExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 0.0, 3.0, 1.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 0.0, 3.0, 1.0]"),
                DoubleChecker(0.5021971),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            )
            val expectedCasingMaskingExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 0.0, 2.0, 1.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 0.0, 2.0, 1.0]"),
                DoubleChecker(0.502198),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            )
            val expectedTrailCasingMaskingExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 0.0, 4.0, 1.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 0.0, 4.0, 1.0]"),
                DoubleChecker(0.5021971),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            )

            val colors = RouteLineColorResources.Builder()
                .routeDefaultColor(Color.argb(255, 0, 0, 1))
                .routeCasingColor(Color.argb(255, 0, 0, 2))
                .routeLineTraveledColor(Color.argb(255, 0, 0, 3))
                .routeLineTraveledCasingColor(Color.argb(255, 0, 0, 4))
                .inActiveRouteLegsColor(Color.argb(255, 0, 0, 5))
                .routeLowCongestionColor(Color.argb(255, 0, 1, 0))
                .routeModerateCongestionColor(Color.argb(255, 0, 2, 0))
                .routeHeavyCongestionColor(Color.argb(255, 0, 3, 0))
                .routeSevereCongestionColor(Color.argb(255, 0, 4, 0))
                .routeUnknownCongestionColor(Color.argb(255, 0, 5, 0))
                .routeClosureColor(Color.argb(255, 0, 6, 0))
                .inactiveRouteLegLowCongestionColor(Color.argb(255, 1, 0, 0))
                .inactiveRouteLegModerateCongestionColor(Color.argb(255, 2, 0, 0))
                .inactiveRouteLegHeavyCongestionColor(Color.argb(255, 3, 0, 0))
                .inactiveRouteLegSevereCongestionColor(Color.argb(255, 4, 0, 0))
                .inactiveRouteLegUnknownCongestionColor(Color.argb(255, 5, 0, 0))
                .inactiveRouteLegClosureColor(Color.argb(255, 6, 0, 0))
                .inactiveRouteLegCasingColor(Color.argb(255, 7, 0, 0))
                .build()
            val viewOptions = MapboxRouteLineViewOptions.Builder(ctx)
                .routeLineColorResources(colors)
                .build()
                .toData()
            val apiOptions = MapboxRouteLineApiOptions.Builder()
                .vanishingRouteLineEnabled(false)
                .styleInactiveRouteLegsIndependently(true)
                .build()
            val api = MapboxRouteLineApi(apiOptions)
            api.setNavigationRoutes(
                listOf(multilegRouteWithOverlapAndAllCongestionLevels.navigationRoute),
                1,
            ) {
                runBlocking {
                    val result = it.value!!
                    val trafficMaskingExpression = getAppliedExpression(
                        result.routeLineMaskingLayerDynamicData!!
                            .trafficExpressionCommandHolder!!,
                        viewOptions,
                        "line-gradient",
                    )
                    val baseMaskingExpression = getAppliedExpression(
                        result.routeLineMaskingLayerDynamicData!!
                            .baseExpressionCommandHolder,
                        viewOptions,
                        "line-gradient",
                    )
                    val casingMaskingExpression = getAppliedExpression(
                        result.routeLineMaskingLayerDynamicData!!
                            .casingExpressionCommandHolder,
                        viewOptions,
                        "line-gradient",
                    )
                    val trailMaskingExpression = getAppliedExpression(
                        result.routeLineMaskingLayerDynamicData!!
                            .trailExpressionCommandHolder!!,
                        viewOptions,
                        "line-gradient",
                    )
                    val trailCasingMaskingExpression = getAppliedExpression(
                        result.routeLineMaskingLayerDynamicData!!
                            .trailCasingExpressionCommandHolder!!,
                        viewOptions,
                        "line-gradient",
                    )

                    val trafficExpression = getAppliedExpression(
                        result.primaryRouteLineData.dynamicData!!
                            .trafficExpressionCommandHolder!!,
                        viewOptions,
                        "line-gradient",
                    )
                    val baseExpression = getAppliedExpression(
                        result.primaryRouteLineData.dynamicData
                            .baseExpressionCommandHolder,
                        viewOptions,
                        "line-gradient",
                    )
                    val casingExpression = getAppliedExpression(
                        result.primaryRouteLineData.dynamicData
                            .casingExpressionCommandHolder,
                        viewOptions,
                        "line-gradient",
                    )
                    val trailExpression = getAppliedExpression(
                        result.primaryRouteLineData.dynamicData
                            .trailExpressionCommandHolder!!,
                        viewOptions,
                        "line-gradient",
                    )
                    val trailCasingExpression = getAppliedExpression(
                        result.primaryRouteLineData.dynamicData
                            .trailCasingExpressionCommandHolder!!,
                        viewOptions,
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

                    checkExpression(expectedTrafficExpressionContents, trafficExpression)
                    checkExpression(expectedBaseExpressionContents, baseExpression)
                    checkExpression(expectedCasingExpressionContents, casingExpression)
                    checkExpression(expectedTrailExpressionContents, trailExpression)
                    checkExpression(expectedTrailCasingExpressionContents, trailCasingExpression)

                    val primaryLineData = result.primaryRouteLineData.dynamicData
                    assertTrue(
                        primaryLineData.trafficExpressionCommandHolder!!.provider
                        is HeavyRouteLineValueProvider,
                    )
                    assertTrue(
                        primaryLineData.baseExpressionCommandHolder.provider
                        is LightRouteLineValueProvider,
                    )
                    assertTrue(
                        primaryLineData.casingExpressionCommandHolder.provider
                        is LightRouteLineValueProvider,
                    )
                    assertTrue(
                        primaryLineData.trailExpressionCommandHolder!!.provider
                        is LightRouteLineValueProvider,
                    )
                    assertTrue(
                        primaryLineData.trailCasingExpressionCommandHolder!!.provider
                        is LightRouteLineValueProvider,
                    )

                    val maskingData = result.routeLineMaskingLayerDynamicData
                    assertTrue(
                        maskingData.trafficExpressionCommandHolder!!.provider
                        is LightRouteLineValueProvider,
                    )
                    assertTrue(
                        maskingData.baseExpressionCommandHolder.provider
                        is LightRouteLineValueProvider,
                    )
                    assertTrue(
                        maskingData.casingExpressionCommandHolder.provider
                        is LightRouteLineValueProvider,
                    )
                    assertTrue(
                        maskingData.trailExpressionCommandHolder!!.provider
                        is LightRouteLineValueProvider,
                    )
                    assertTrue(
                        maskingData.trailCasingExpressionCommandHolder!!.provider
                        is LightRouteLineValueProvider,
                    )

                    callbackCalled = true
                }
            }

            assertTrue(callbackCalled)
            unmockkObject(MapboxRouteLineUtils)
        }

    @Test
    fun `updateWithRouteProgress multileg route when styleInactiveRouteLegsIndependently true and vanishing route line disabled route leg 1`() =
        coroutineRule.runBlockingTest {
            mockkObject(MapboxRouteLineUtils)
            var callbackCalled = false
            val expectedTrafficExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 5.0, 0.0, 1.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 5.0, 0.0, 1.0]"),
                DoubleChecker(0.0515733874436984),
                StringChecker("[rgba, 0.0, 4.0, 0.0, 1.0]"),
                DoubleChecker(0.134810268279905),
                StringChecker("[rgba, 0.0, 3.0, 0.0, 1.0]"),
                DoubleChecker(0.336391649717869),
                StringChecker("[rgba, 0.0, 2.0, 0.0, 1.0]"),
                DoubleChecker(0.397037616989048),
                StringChecker("[rgba, 0.0, 1.0, 0.0, 1.0]"),
                DoubleChecker(0.493370347113566),
                StringChecker("[rgba, 0.0, 6.0, 0.0, 1.0]"),
                DoubleChecker(0.502197025505151),
                StringChecker("[rgba, 5.0, 0.0, 0.0, 1.0]"),
                DoubleChecker(0.507776180581013),
                StringChecker("[rgba, 4.0, 0.0, 0.0, 1.0]"),
                DoubleChecker(0.517799629825412),
                StringChecker("[rgba, 3.0, 0.0, 0.0, 1.0]"),
                DoubleChecker(0.545361204883585),
                StringChecker("[rgba, 2.0, 0.0, 0.0, 1.0]"),
                DoubleChecker(0.565767097390147),
                StringChecker("[rgba, 1.0, 0.0, 0.0, 1.0]"),
                DoubleChecker(0.939356842228311),
                StringChecker("[rgba, 6.0, 0.0, 0.0, 1.0]"),
            )
            val expectedBaseExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 0.0, 1.0, 1.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 0.0, 1.0, 1.0]"),
                DoubleChecker(0.5021971),
                StringChecker("[rgba, 0.0, 0.0, 5.0, 1.0]"),
            )
            val expectedCasingExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 0.0, 2.0, 1.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 0.0, 2.0, 1.0]"),
                DoubleChecker(0.5021971),
                StringChecker("[rgba, 7.0, 0.0, 0.0, 1.0]"),
            )
            val expectedTrailExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 0.0, 3.0, 1.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 0.0, 3.0, 1.0]"),
                DoubleChecker(0.5021971),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            )
            val expectedTrailCasingExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 0.0, 4.0, 1.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 0.0, 4.0, 1.0]"),
                DoubleChecker(0.5021971),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            )
            val expectedMaskingTrafficExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 5.0, 0.0, 1.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 5.0, 0.0, 1.0]"),
                DoubleChecker(0.0515733874436984),
                StringChecker("[rgba, 0.0, 4.0, 0.0, 1.0]"),
                DoubleChecker(0.134810268279905),
                StringChecker("[rgba, 0.0, 3.0, 0.0, 1.0]"),
                DoubleChecker(0.336391649717869),
                StringChecker("[rgba, 0.0, 2.0, 0.0, 1.0]"),
                DoubleChecker(0.397037616989048),
                StringChecker("[rgba, 0.0, 1.0, 0.0, 1.0]"),
                DoubleChecker(0.493370347113566),
                StringChecker("[rgba, 0.0, 6.0, 0.0, 1.0]"),
                DoubleChecker(0.502197025505151),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            )
            val expectedMaskingBaseExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 0.0, 1.0, 1.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 0.0, 1.0, 1.0]"),
                DoubleChecker(0.5021971),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            )
            val expectedTrailMaskingExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 0.0, 3.0, 1.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 0.0, 3.0, 1.0]"),
                DoubleChecker(0.5021971),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            )
            val expectedCasingMaskingExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 0.0, 2.0, 1.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 0.0, 2.0, 1.0]"),
                DoubleChecker(0.502198),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            )
            val expectedTrailCasingMaskingExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 0.0, 4.0, 1.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 0.0, 4.0, 1.0]"),
                DoubleChecker(0.5021971),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            )

            val colors = RouteLineColorResources.Builder()
                .routeDefaultColor(Color.argb(255, 0, 0, 1))
                .routeCasingColor(Color.argb(255, 0, 0, 2))
                .routeLineTraveledColor(Color.argb(255, 0, 0, 3))
                .routeLineTraveledCasingColor(Color.argb(255, 0, 0, 4))
                .inActiveRouteLegsColor(Color.argb(255, 0, 0, 5))
                .routeLowCongestionColor(Color.argb(255, 0, 1, 0))
                .routeModerateCongestionColor(Color.argb(255, 0, 2, 0))
                .routeHeavyCongestionColor(Color.argb(255, 0, 3, 0))
                .routeSevereCongestionColor(Color.argb(255, 0, 4, 0))
                .routeUnknownCongestionColor(Color.argb(255, 0, 5, 0))
                .routeClosureColor(Color.argb(255, 0, 6, 0))
                .inactiveRouteLegLowCongestionColor(Color.argb(255, 1, 0, 0))
                .inactiveRouteLegModerateCongestionColor(Color.argb(255, 2, 0, 0))
                .inactiveRouteLegHeavyCongestionColor(Color.argb(255, 3, 0, 0))
                .inactiveRouteLegSevereCongestionColor(Color.argb(255, 4, 0, 0))
                .inactiveRouteLegUnknownCongestionColor(Color.argb(255, 5, 0, 0))
                .inactiveRouteLegClosureColor(Color.argb(255, 6, 0, 0))
                .inactiveRouteLegCasingColor(Color.argb(255, 7, 0, 0))
                .build()
            val viewOptions = MapboxRouteLineViewOptions.Builder(ctx)
                .routeLineColorResources(colors)
                .build()
                .toData()
            val apiOptions = MapboxRouteLineApiOptions.Builder()
                .vanishingRouteLineEnabled(false)
                .styleInactiveRouteLegsIndependently(true)
                .build()
            val api = MapboxRouteLineApi(apiOptions)
            val routeProgress = mockRouteProgress(
                multilegRouteWithOverlapAndAllCongestionLevels.navigationRoute,
            )
            every { routeProgress.currentLegProgress!!.legIndex } returns 1
            every { routeProgress.currentRouteGeometryIndex } returns 43
            api.setNavigationRoutes(
                listOf(multilegRouteWithOverlapAndAllCongestionLevels.navigationRoute),
            )

            api.updateWithRouteProgress(routeProgress) {
                runBlocking {
                    val result = it.value!!
                    val trafficMaskingExpression = getAppliedExpression(
                        result.routeLineMaskingLayerDynamicData!!
                            .trafficExpressionCommandHolder!!,
                        viewOptions,
                        "line-gradient",
                    )
                    val baseMaskingExpression = getAppliedExpression(
                        result.routeLineMaskingLayerDynamicData!!
                            .baseExpressionCommandHolder,
                        viewOptions,
                        "line-gradient",
                    )
                    val casingMaskingExpression = getAppliedExpression(
                        result.routeLineMaskingLayerDynamicData!!
                            .casingExpressionCommandHolder,
                        viewOptions,
                        "line-gradient",
                    )
                    val trailMaskingExpression = getAppliedExpression(
                        result.routeLineMaskingLayerDynamicData!!
                            .trailExpressionCommandHolder!!,
                        viewOptions,
                        "line-gradient",
                    )
                    val trailCasingMaskingExpression = getAppliedExpression(
                        result.routeLineMaskingLayerDynamicData!!
                            .trailCasingExpressionCommandHolder!!,
                        viewOptions,
                        "line-gradient",
                    )

                    val trafficExpression = getAppliedExpression(
                        result.primaryRouteLineDynamicData!!
                            .trafficExpressionCommandHolder!!,
                        viewOptions,
                        "line-gradient",
                    )
                    val baseExpression = getAppliedExpression(
                        result.primaryRouteLineDynamicData!!
                            .baseExpressionCommandHolder,
                        viewOptions,
                        "line-gradient",
                    )
                    val casingExpression = getAppliedExpression(
                        result.primaryRouteLineDynamicData!!
                            .casingExpressionCommandHolder,
                        viewOptions,
                        "line-gradient",
                    )
                    val trailExpression = getAppliedExpression(
                        result.primaryRouteLineDynamicData!!
                            .trailExpressionCommandHolder!!,
                        viewOptions,
                        "line-gradient",
                    )
                    val trailCasingExpression = getAppliedExpression(
                        result.primaryRouteLineDynamicData!!
                            .trailCasingExpressionCommandHolder!!,
                        viewOptions,
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

                    checkExpression(expectedTrafficExpressionContents, trafficExpression)
                    checkExpression(expectedBaseExpressionContents, baseExpression)
                    checkExpression(expectedCasingExpressionContents, casingExpression)
                    checkExpression(expectedTrailExpressionContents, trailExpression)
                    checkExpression(expectedTrailCasingExpressionContents, trailCasingExpression)

                    val primaryLineData = result.primaryRouteLineDynamicData
                    assertTrue(
                        primaryLineData.trafficExpressionCommandHolder!!.provider
                        is HeavyRouteLineValueProvider,
                    )
                    assertTrue(
                        primaryLineData.baseExpressionCommandHolder.provider
                        is LightRouteLineValueProvider,
                    )
                    assertTrue(
                        primaryLineData.casingExpressionCommandHolder.provider
                        is LightRouteLineValueProvider,
                    )
                    assertTrue(
                        primaryLineData.trailExpressionCommandHolder!!.provider
                        is LightRouteLineValueProvider,
                    )
                    assertTrue(
                        primaryLineData.trailCasingExpressionCommandHolder!!.provider
                        is LightRouteLineValueProvider,
                    )

                    val maskingData = result.routeLineMaskingLayerDynamicData
                    assertTrue(
                        maskingData.trafficExpressionCommandHolder!!.provider
                        is LightRouteLineValueProvider,
                    )
                    assertTrue(
                        maskingData.baseExpressionCommandHolder.provider
                        is LightRouteLineValueProvider,
                    )
                    assertTrue(
                        maskingData.casingExpressionCommandHolder.provider
                        is LightRouteLineValueProvider,
                    )
                    assertTrue(
                        maskingData.trailExpressionCommandHolder!!.provider
                        is LightRouteLineValueProvider,
                    )
                    assertTrue(
                        maskingData.trailCasingExpressionCommandHolder!!.provider
                        is LightRouteLineValueProvider,
                    )

                    callbackCalled = true
                }
            }

            assertTrue(callbackCalled)
            unmockkObject(MapboxRouteLineUtils)
        }

    @Test
    fun `updateWithRouteProgress multileg route when styleInactiveRouteLegsIndependently true and vanishing route line disabled route leg 3 - trails are hidden from all inactive legs`() =
        coroutineRule.runBlockingTest {
            mockkObject(MapboxRouteLineUtils)
            var callbackCalled = false
            val expectedTrailExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
                DoubleChecker(0.223124949044229),
                StringChecker("[rgba, 0.0, 0.0, 3.0, 1.0]"),
                DoubleChecker(0.462531495099724),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            )
            val expectedTrailCasingExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
                DoubleChecker(0.223124949044229),
                StringChecker("[rgba, 0.0, 0.0, 4.0, 1.0]"),
                DoubleChecker(0.462531495099724),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            )
            val expectedTrailMaskingExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
                DoubleChecker(0.223124949044229),
                StringChecker("[rgba, 0.0, 0.0, 3.0, 1.0]"),
                DoubleChecker(0.462531495099724),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            )
            val expectedTrailCasingMaskingExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
                DoubleChecker(0.223124949044229),
                StringChecker("[rgba, 0.0, 0.0, 4.0, 1.0]"),
                DoubleChecker(0.462531495099724),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            )

            val colors = RouteLineColorResources.Builder()
                .routeDefaultColor(Color.argb(255, 0, 0, 1))
                .routeCasingColor(Color.argb(255, 0, 0, 2))
                .routeLineTraveledColor(Color.argb(255, 0, 0, 3))
                .routeLineTraveledCasingColor(Color.argb(255, 0, 0, 4))
                .inActiveRouteLegsColor(Color.argb(255, 0, 0, 5))
                .routeLowCongestionColor(Color.argb(255, 0, 1, 0))
                .routeModerateCongestionColor(Color.argb(255, 0, 2, 0))
                .routeHeavyCongestionColor(Color.argb(255, 0, 3, 0))
                .routeSevereCongestionColor(Color.argb(255, 0, 4, 0))
                .routeUnknownCongestionColor(Color.argb(255, 0, 5, 0))
                .routeClosureColor(Color.argb(255, 0, 6, 0))
                .inactiveRouteLegLowCongestionColor(Color.argb(255, 1, 0, 0))
                .inactiveRouteLegModerateCongestionColor(Color.argb(255, 2, 0, 0))
                .inactiveRouteLegHeavyCongestionColor(Color.argb(255, 3, 0, 0))
                .inactiveRouteLegSevereCongestionColor(Color.argb(255, 4, 0, 0))
                .inactiveRouteLegUnknownCongestionColor(Color.argb(255, 5, 0, 0))
                .inactiveRouteLegClosureColor(Color.argb(255, 6, 0, 0))
                .inactiveRouteLegCasingColor(Color.argb(255, 7, 0, 0))
                .build()
            val viewOptions = MapboxRouteLineViewOptions.Builder(ctx)
                .routeLineColorResources(colors)
                .build()
                .toData()
            val apiOptions = MapboxRouteLineApiOptions.Builder()
                .vanishingRouteLineEnabled(false)
                .styleInactiveRouteLegsIndependently(true)
                .build()
            val api = MapboxRouteLineApi(apiOptions)
            val routeProgress = mockRouteProgress(
                threeLegRoute.navigationRoutes.first(),
            )
            every { routeProgress.currentLegProgress!!.legIndex } returns 1
            every { routeProgress.currentRouteGeometryIndex } returns 36
            api.setNavigationRoutes(
                listOf(threeLegRoute.navigationRoutes.first()),
            )

            api.updateWithRouteProgress(routeProgress) {
                runBlocking {
                    val result = it.value!!

                    val trailMaskingExpression = getAppliedExpression(
                        result.routeLineMaskingLayerDynamicData!!
                            .trailExpressionCommandHolder!!,
                        viewOptions,
                        "line-gradient",
                    )
                    val trailCasingMaskingExpression = getAppliedExpression(
                        result.routeLineMaskingLayerDynamicData!!
                            .trailCasingExpressionCommandHolder!!,
                        viewOptions,
                        "line-gradient",
                    )

                    val trailExpression = getAppliedExpression(
                        result.primaryRouteLineDynamicData!!
                            .trailExpressionCommandHolder!!,
                        viewOptions,
                        "line-gradient",
                    )
                    val trailCasingExpression = getAppliedExpression(
                        result.primaryRouteLineDynamicData!!
                            .trailCasingExpressionCommandHolder!!,
                        viewOptions,
                        "line-gradient",
                    )

                    checkExpression(expectedTrailMaskingExpressionContents, trailMaskingExpression)
                    checkExpression(
                        expectedTrailCasingMaskingExpressionContents,
                        trailCasingMaskingExpression,
                    )

                    checkExpression(expectedTrailExpressionContents, trailExpression)
                    checkExpression(expectedTrailCasingExpressionContents, trailCasingExpression)

                    val primaryLineData = result.primaryRouteLineDynamicData
                    assertTrue(
                        primaryLineData.trailExpressionCommandHolder!!.provider
                        is LightRouteLineValueProvider,
                    )
                    assertTrue(
                        primaryLineData.trailCasingExpressionCommandHolder!!.provider
                        is LightRouteLineValueProvider,
                    )

                    val maskingData = result.routeLineMaskingLayerDynamicData
                    assertTrue(
                        maskingData.trailExpressionCommandHolder!!.provider
                        is LightRouteLineValueProvider,
                    )
                    assertTrue(
                        maskingData.trailCasingExpressionCommandHolder!!.provider
                        is LightRouteLineValueProvider,
                    )

                    callbackCalled = true
                }
            }

            assertTrue(callbackCalled)

            unmockkObject(MapboxRouteLineUtils)
        }

    @Test
    fun `updateWithRouteProgress multileg route when styleInactiveRouteLegsIndependently true and vanishing route line enabled route leg 3 - trails are hidden from upcoming inactive legs only`() =
        coroutineRule.runBlockingTest {
            mockkObject(MapboxRouteLineUtils)
            var callbackCalled = false
            val expectedTrailExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
                DoubleChecker(0.223124949044229),
                StringChecker("[rgba, 0.0, 0.0, 3.0, 1.0]"),
            )
            val expectedTrailCasingExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
                DoubleChecker(0.223124949044229),
                StringChecker("[rgba, 0.0, 0.0, 4.0, 1.0]"),
            )
            val expectedTrailMaskingExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
                DoubleChecker(0.223124949044229),
                StringChecker("[rgba, 0.0, 0.0, 3.0, 1.0]"),
                DoubleChecker(0.462531495099724),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            )
            val expectedTrailCasingMaskingExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
                DoubleChecker(0.223124949044229),
                StringChecker("[rgba, 0.0, 0.0, 4.0, 1.0]"),
                DoubleChecker(0.462531495099724),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            )

            val colors = RouteLineColorResources.Builder()
                .routeDefaultColor(Color.argb(255, 0, 0, 1))
                .routeCasingColor(Color.argb(255, 0, 0, 2))
                .routeLineTraveledColor(Color.argb(255, 0, 0, 3))
                .routeLineTraveledCasingColor(Color.argb(255, 0, 0, 4))
                .inActiveRouteLegsColor(Color.argb(255, 0, 0, 5))
                .routeLowCongestionColor(Color.argb(255, 0, 1, 0))
                .routeModerateCongestionColor(Color.argb(255, 0, 2, 0))
                .routeHeavyCongestionColor(Color.argb(255, 0, 3, 0))
                .routeSevereCongestionColor(Color.argb(255, 0, 4, 0))
                .routeUnknownCongestionColor(Color.argb(255, 0, 5, 0))
                .routeClosureColor(Color.argb(255, 0, 6, 0))
                .inactiveRouteLegLowCongestionColor(Color.argb(255, 1, 0, 0))
                .inactiveRouteLegModerateCongestionColor(Color.argb(255, 2, 0, 0))
                .inactiveRouteLegHeavyCongestionColor(Color.argb(255, 3, 0, 0))
                .inactiveRouteLegSevereCongestionColor(Color.argb(255, 4, 0, 0))
                .inactiveRouteLegUnknownCongestionColor(Color.argb(255, 5, 0, 0))
                .inactiveRouteLegClosureColor(Color.argb(255, 6, 0, 0))
                .inactiveRouteLegCasingColor(Color.argb(255, 7, 0, 0))
                .build()
            val viewOptions = MapboxRouteLineViewOptions.Builder(ctx)
                .routeLineColorResources(colors)
                .build()
                .toData()
            val apiOptions = MapboxRouteLineApiOptions.Builder()
                .vanishingRouteLineEnabled(true)
                .styleInactiveRouteLegsIndependently(true)
                .vanishingRouteLineUpdateIntervalNano(0)
                .build()
            val api = MapboxRouteLineApi(apiOptions)
            val routeProgress = mockRouteProgress(
                threeLegRoute.navigationRoutes.first(),
            )
            every { routeProgress.currentLegProgress!!.legIndex } returns 1
            every { routeProgress.currentRouteGeometryIndex } returns 36
            api.updateVanishingPointState(RouteProgressState.TRACKING)
            api.setNavigationRoutes(
                listOf(threeLegRoute.navigationRoutes.first()),
            )
            api.updateUpcomingRoutePointIndex(routeProgress)

            api.updateWithRouteProgress(routeProgress) {
                runBlocking {
                    val result = it.value!!

                    val trailMaskingExpression = getAppliedExpression(
                        result.routeLineMaskingLayerDynamicData!!
                            .trailExpressionCommandHolder!!,
                        viewOptions,
                        "line-gradient",
                    )
                    val trailCasingMaskingExpression = getAppliedExpression(
                        result.routeLineMaskingLayerDynamicData!!
                            .trailCasingExpressionCommandHolder!!,
                        viewOptions,
                        "line-gradient",
                    )

                    val trailExpression = getAppliedExpression(
                        result.primaryRouteLineDynamicData!!
                            .trailExpressionCommandHolder!!,
                        viewOptions,
                        "line-gradient",
                    )
                    val trailCasingExpression = getAppliedExpression(
                        result.primaryRouteLineDynamicData!!
                            .trailCasingExpressionCommandHolder!!,
                        viewOptions,
                        "line-gradient",
                    )

                    checkExpression(expectedTrailMaskingExpressionContents, trailMaskingExpression)
                    checkExpression(
                        expectedTrailCasingMaskingExpressionContents,
                        trailCasingMaskingExpression,
                    )

                    checkExpression(expectedTrailExpressionContents, trailExpression)
                    checkExpression(expectedTrailCasingExpressionContents, trailCasingExpression)

                    val primaryLineData = result.primaryRouteLineDynamicData
                    assertTrue(
                        primaryLineData.trailExpressionCommandHolder!!.provider
                        is LightRouteLineValueProvider,
                    )
                    assertTrue(
                        primaryLineData.trailCasingExpressionCommandHolder!!.provider
                        is LightRouteLineValueProvider,
                    )

                    val maskingData = result.routeLineMaskingLayerDynamicData
                    assertTrue(
                        maskingData.trailExpressionCommandHolder!!.provider
                        is LightRouteLineValueProvider,
                    )
                    assertTrue(
                        maskingData.trailCasingExpressionCommandHolder!!.provider
                        is LightRouteLineValueProvider,
                    )

                    callbackCalled = true
                }
            }

            assertTrue(callbackCalled)

            unmockkObject(MapboxRouteLineUtils)
        }

    @Test
    fun `setNewRoutesData multileg route when styleInactiveRouteLegsIndependently false and vanishing route line enabled route leg 3 - all primary trails are rendered`() =
        coroutineRule.runBlockingTest {
            mockkObject(MapboxRouteLineUtils)
            var callbackCalled = false
            val expectedTrailExpressionContents = listOf(
                StringChecker("rgba"),
                DoubleChecker(0.0),
                DoubleChecker(0.0),
                DoubleChecker(3.0),
                DoubleChecker(1.0),
            )
            val expectedTrailCasingExpressionContents = listOf(
                StringChecker("rgba"),
                DoubleChecker(0.0),
                DoubleChecker(0.0),
                DoubleChecker(4.0),
                DoubleChecker(1.0),
            )
            val expectedTrailMaskingExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
                DoubleChecker(0.223124949044229),
                StringChecker("[rgba, 0.0, 0.0, 3.0, 1.0]"),
                DoubleChecker(0.462531495099724),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            )
            val expectedTrailCasingMaskingExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
                DoubleChecker(0.223124949044229),
                StringChecker("[rgba, 0.0, 0.0, 4.0, 1.0]"),
                DoubleChecker(0.462531495099724),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            )

            val colors = RouteLineColorResources.Builder()
                .routeDefaultColor(Color.argb(255, 0, 0, 1))
                .routeCasingColor(Color.argb(255, 0, 0, 2))
                .routeLineTraveledColor(Color.argb(255, 0, 0, 3))
                .routeLineTraveledCasingColor(Color.argb(255, 0, 0, 4))
                .inActiveRouteLegsColor(Color.argb(255, 0, 0, 5))
                .routeLowCongestionColor(Color.argb(255, 0, 1, 0))
                .routeModerateCongestionColor(Color.argb(255, 0, 2, 0))
                .routeHeavyCongestionColor(Color.argb(255, 0, 3, 0))
                .routeSevereCongestionColor(Color.argb(255, 0, 4, 0))
                .routeUnknownCongestionColor(Color.argb(255, 0, 5, 0))
                .routeClosureColor(Color.argb(255, 0, 6, 0))
                .inactiveRouteLegLowCongestionColor(Color.argb(255, 1, 0, 0))
                .inactiveRouteLegModerateCongestionColor(Color.argb(255, 2, 0, 0))
                .inactiveRouteLegHeavyCongestionColor(Color.argb(255, 3, 0, 0))
                .inactiveRouteLegSevereCongestionColor(Color.argb(255, 4, 0, 0))
                .inactiveRouteLegUnknownCongestionColor(Color.argb(255, 5, 0, 0))
                .inactiveRouteLegClosureColor(Color.argb(255, 6, 0, 0))
                .inactiveRouteLegCasingColor(Color.argb(255, 7, 0, 0))
                .build()
            val viewOptions = MapboxRouteLineViewOptions.Builder(ctx)
                .routeLineColorResources(colors)
                .build()
                .toData()
            val apiOptions = MapboxRouteLineApiOptions.Builder()
                .vanishingRouteLineEnabled(true)
                .styleInactiveRouteLegsIndependently(false)
                .vanishingRouteLineUpdateIntervalNano(0)
                .build()
            val api = MapboxRouteLineApi(apiOptions)
            api.updateVanishingPointState(RouteProgressState.TRACKING)
            api.setNavigationRoutes(
                listOf(threeLegRoute.navigationRoutes.first()),
                activeLegIndex = 1,
            ) {
                runBlocking {
                    val result = it.value!!

                    val trailMaskingExpression = getAppliedExpression(
                        result.routeLineMaskingLayerDynamicData!!
                            .trailExpressionCommandHolder!!,
                        viewOptions,
                        "line-gradient",
                    )
                    val trailCasingMaskingExpression = getAppliedExpression(
                        result.routeLineMaskingLayerDynamicData!!
                            .trailCasingExpressionCommandHolder!!,
                        viewOptions,
                        "line-gradient",
                    )

                    val trailExpression = getAppliedExpression(
                        result.primaryRouteLineData.dynamicData!!
                            .trailExpressionCommandHolder!!,
                        viewOptions,
                        "line-gradient",
                    )
                    val trailCasingExpression = getAppliedExpression(
                        result.primaryRouteLineData.dynamicData
                            .trailCasingExpressionCommandHolder!!,
                        viewOptions,
                        "line-gradient",
                    )

                    checkExpression(expectedTrailMaskingExpressionContents, trailMaskingExpression)
                    checkExpression(
                        expectedTrailCasingMaskingExpressionContents,
                        trailCasingMaskingExpression,
                    )

                    checkExpression(expectedTrailExpressionContents, trailExpression)
                    checkExpression(expectedTrailCasingExpressionContents, trailCasingExpression)

                    val primaryLineData = result.primaryRouteLineData
                    assertTrue(
                        primaryLineData.dynamicData!!.trailExpressionCommandHolder!!.provider
                        is LightRouteLineValueProvider,
                    )
                    assertTrue(
                        primaryLineData.dynamicData.trailCasingExpressionCommandHolder!!.provider
                        is LightRouteLineValueProvider,
                    )

                    val maskingData = result.routeLineMaskingLayerDynamicData
                    assertTrue(
                        maskingData.trailExpressionCommandHolder!!.provider
                        is LightRouteLineValueProvider,
                    )
                    assertTrue(
                        maskingData.trailCasingExpressionCommandHolder!!.provider
                        is LightRouteLineValueProvider,
                    )

                    callbackCalled = true
                }
            }

            assertTrue(callbackCalled)

            unmockkObject(MapboxRouteLineUtils)
        }

    @Test
    fun `updateWithRouteProgress multileg route when styleInactiveRouteLegsIndependently false and vanishing route line enabled route leg 3 - only masking data is updated`() =
        coroutineRule.runBlockingTest {
            mockkObject(MapboxRouteLineUtils)
            var callbackCalled = false
            val expectedTrailMaskingExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
                DoubleChecker(0.223124949044229),
                StringChecker("[rgba, 0.0, 0.0, 3.0, 1.0]"),
                DoubleChecker(0.462531495099724),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            )
            val expectedTrailCasingMaskingExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
                DoubleChecker(0.223124949044229),
                StringChecker("[rgba, 0.0, 0.0, 4.0, 1.0]"),
                DoubleChecker(0.462531495099724),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            )

            val colors = RouteLineColorResources.Builder()
                .routeDefaultColor(Color.argb(255, 0, 0, 1))
                .routeCasingColor(Color.argb(255, 0, 0, 2))
                .routeLineTraveledColor(Color.argb(255, 0, 0, 3))
                .routeLineTraveledCasingColor(Color.argb(255, 0, 0, 4))
                .inActiveRouteLegsColor(Color.argb(255, 0, 0, 5))
                .routeLowCongestionColor(Color.argb(255, 0, 1, 0))
                .routeModerateCongestionColor(Color.argb(255, 0, 2, 0))
                .routeHeavyCongestionColor(Color.argb(255, 0, 3, 0))
                .routeSevereCongestionColor(Color.argb(255, 0, 4, 0))
                .routeUnknownCongestionColor(Color.argb(255, 0, 5, 0))
                .routeClosureColor(Color.argb(255, 0, 6, 0))
                .inactiveRouteLegLowCongestionColor(Color.argb(255, 1, 0, 0))
                .inactiveRouteLegModerateCongestionColor(Color.argb(255, 2, 0, 0))
                .inactiveRouteLegHeavyCongestionColor(Color.argb(255, 3, 0, 0))
                .inactiveRouteLegSevereCongestionColor(Color.argb(255, 4, 0, 0))
                .inactiveRouteLegUnknownCongestionColor(Color.argb(255, 5, 0, 0))
                .inactiveRouteLegClosureColor(Color.argb(255, 6, 0, 0))
                .inactiveRouteLegCasingColor(Color.argb(255, 7, 0, 0))
                .build()
            val viewOptions = MapboxRouteLineViewOptions.Builder(ctx)
                .routeLineColorResources(colors)
                .build()
                .toData()
            val apiOptions = MapboxRouteLineApiOptions.Builder()
                .vanishingRouteLineEnabled(true)
                .styleInactiveRouteLegsIndependently(false)
                .vanishingRouteLineUpdateIntervalNano(0)
                .build()
            val api = MapboxRouteLineApi(apiOptions)
            val routeProgress = mockRouteProgress(
                threeLegRoute.navigationRoutes.first(),
            )
            every { routeProgress.currentLegProgress!!.legIndex } returns 1
            every { routeProgress.currentRouteGeometryIndex } returns 36
            api.updateVanishingPointState(RouteProgressState.TRACKING)
            api.setNavigationRoutes(
                listOf(threeLegRoute.navigationRoutes.first()),
            )
            api.updateUpcomingRoutePointIndex(routeProgress)

            api.updateWithRouteProgress(routeProgress) {
                runBlocking {
                    val result = it.value!!

                    val trailMaskingExpression = getAppliedExpression(
                        result.routeLineMaskingLayerDynamicData!!
                            .trailExpressionCommandHolder!!,
                        viewOptions,
                        "line-gradient",
                    )
                    val trailCasingMaskingExpression = getAppliedExpression(
                        result.routeLineMaskingLayerDynamicData!!
                            .trailCasingExpressionCommandHolder!!,
                        viewOptions,
                        "line-gradient",
                    )

                    assertNull(result.primaryRouteLineDynamicData)

                    checkExpression(expectedTrailMaskingExpressionContents, trailMaskingExpression)
                    checkExpression(
                        expectedTrailCasingMaskingExpressionContents,
                        trailCasingMaskingExpression,
                    )

                    val maskingData = result.routeLineMaskingLayerDynamicData
                    assertTrue(
                        maskingData.trailExpressionCommandHolder!!.provider
                        is LightRouteLineValueProvider,
                    )
                    assertTrue(
                        maskingData.trailCasingExpressionCommandHolder!!.provider
                        is LightRouteLineValueProvider,
                    )

                    callbackCalled = true
                }
            }

            assertTrue(callbackCalled)

            unmockkObject(MapboxRouteLineUtils)
        }

    @Test
    fun setRoutes() = coroutineRule.runBlockingTest {
        val apiOptions = MapboxRouteLineApiOptions.Builder().build()
        val viewOptions = MapboxRouteLineViewOptions.Builder(ctx).build().toData()
        val api = MapboxRouteLineApi(apiOptions)
        val expectedCasingExpressionContents = listOf(
            StringChecker("rgba"),
            DoubleChecker(47.0),
            DoubleChecker(122.0),
            DoubleChecker(198.0),
            DoubleChecker(1.0),
        )
        val expectedRouteLineExpressionContents = listOf(
            StringChecker("rgba"),
            DoubleChecker(86.0),
            DoubleChecker(168.0),
            DoubleChecker(251.0),
            DoubleChecker(1.0),
        )
        val expectedTrafficLineExpressionContents = listOf(
            StringChecker("step"),
            StringChecker("[line-progress]"),
            StringChecker("[rgba, 255.0, 149.0, 0.0, 1.0]"),
            DoubleChecker(0.0),
            StringChecker("[rgba, 255.0, 149.0, 0.0, 1.0]"),
            DoubleChecker(0.057451),
            StringChecker("[rgba, 86.0, 168.0, 251.0, 1.0]"),
        )
        val expectedPrimaryRouteSourceGeometry = "LineString{type=LineString, bbox=null, " +
            "coordinates=[Point{type=Point, bbox=null, coordinates=[-122.523131, 37.975067]}," +
            " Point{type=Point, bbox=null, coordinates=[-122.523117, 37.975107]}, " +
            "Point{type=Point, bbox=null, coordinates=[-122.523579, 37.975173]}, " +
            "Point{type=Point, bbox=null, coordinates=[-122.523729, 37.975194]}, " +
            "Point{type=Point, bbox=null, coordinates=[-122.523671, 37.975379]}]}"
        val expectedWaypointFeature0 =
            "Point{type=Point, bbox=null, coordinates=[-122.523671, 37.975379]}"
        val expectedWaypointFeature1 =
            "Point{type=Point, bbox=null, coordinates=[-122.523131, 37.975067]}"
        val expectedMaskingExpressionContents = listOf(
            StringChecker("rgba"),
            DoubleChecker(0.0),
            DoubleChecker(0.0),
            DoubleChecker(0.0),
            DoubleChecker(0.0),
        )

        val route = loadNavigationRoute(
            "short_route.json",
            waypoints = listOf(
                createWaypoint(location = doubleArrayOf(-122.523671, 37.975379)),
                createWaypoint(location = doubleArrayOf(-122.523131, 37.975067)),
            ),
        )
        val routes = listOf(NavigationRouteLine(route, null))

        val result = api.setNavigationRouteLines(routes)

        checkExpression(
            expectedCasingExpressionContents,
            getAppliedExpression(
                result.value!!.primaryRouteLineData.dynamicData!!.casingExpressionCommandHolder,
                viewOptions,
                "line-gradient",
            ),
        )
        checkExpression(
            expectedRouteLineExpressionContents,
            getAppliedExpression(
                result.value!!.primaryRouteLineData.dynamicData!!.baseExpressionCommandHolder,
                viewOptions,
                "line-gradient",
            ),
        )
        checkExpression(
            expectedTrafficLineExpressionContents,
            getAppliedExpression(
                result.value!!.primaryRouteLineData.dynamicData!!.trafficExpressionCommandHolder!!,
                viewOptions,
                "line-gradient",
            ),
        )
        assertEquals(
            expectedPrimaryRouteSourceGeometry,
            result.value!!.primaryRouteLineData.featureCollection.features()!![0].geometry()
                .toString(),
        )
        assertTrue(
            result.value!!.alternativeRouteLinesData[0].featureCollection.features()!!.isEmpty(),
        )
        assertTrue(
            result.value!!.alternativeRouteLinesData[1].featureCollection.features()!!.isEmpty(),
        )
        assertEquals(
            expectedWaypointFeature0,
            result.value!!.waypointsSource.features()!![0].geometry().toString(),
        )
        assertEquals(
            expectedWaypointFeature1,
            result.value!!.waypointsSource.features()!![1].geometry().toString(),
        )
        checkExpression(
            expectedMaskingExpressionContents,
            getAppliedExpression(
                result.value!!.routeLineMaskingLayerDynamicData!!.trafficExpressionCommandHolder!!,
                viewOptions,
                "line-gradient",
            ),
        )
        checkExpression(
            expectedMaskingExpressionContents,
            getAppliedExpression(
                result.value!!.routeLineMaskingLayerDynamicData!!.baseExpressionCommandHolder,
                viewOptions,
                "line-gradient",
            ),
        )
        checkExpression(
            expectedMaskingExpressionContents,
            getAppliedExpression(
                result.value!!.routeLineMaskingLayerDynamicData!!.casingExpressionCommandHolder,
                viewOptions,
                "line-gradient",
            ),
        )
        checkExpression(
            expectedMaskingExpressionContents,
            getAppliedExpression(
                result.value!!.routeLineMaskingLayerDynamicData!!.trailExpressionCommandHolder!!,
                viewOptions,
                "line-gradient",
            ),
        )
        checkExpression(
            expectedMaskingExpressionContents,
            getAppliedExpression(
                result.value!!.routeLineMaskingLayerDynamicData!!
                    .trailCasingExpressionCommandHolder!!,
                viewOptions,
                "line-gradient",
            ),
        )

        val primaryLineData = result.value!!.primaryRouteLineData.dynamicData!!
        assertTrue(
            primaryLineData.trafficExpressionCommandHolder!!.provider
            is HeavyRouteLineValueProvider,
        )
        assertTrue(
            primaryLineData.baseExpressionCommandHolder.provider is LightRouteLineValueProvider,
        )
        assertTrue(
            primaryLineData.casingExpressionCommandHolder.provider is LightRouteLineValueProvider,
        )

        val maskingData = result.value!!.routeLineMaskingLayerDynamicData!!
        assertTrue(
            maskingData.trafficExpressionCommandHolder!!.provider is LightRouteLineValueProvider,
        )
        assertTrue(
            maskingData.baseExpressionCommandHolder.provider is LightRouteLineValueProvider,
        )
        assertTrue(
            maskingData.casingExpressionCommandHolder.provider is LightRouteLineValueProvider,
        )
        assertTrue(
            maskingData.trailExpressionCommandHolder!!.provider is LightRouteLineValueProvider,
        )
        assertTrue(
            maskingData.trailCasingExpressionCommandHolder!!.provider
            is LightRouteLineValueProvider,
        )
    }

    @Test
    fun setRoutes_maskingLayerExpressionsWithMultiLegRoute() = coroutineRule.runBlockingTest {
        val colors = RouteLineColorResources.Builder()
            .routeLowCongestionColor(Color.GRAY)
            .routeUnknownCongestionColor(Color.LTGRAY)
            .routeDefaultColor(Color.BLUE)
            .routeLineTraveledColor(Color.RED)
            .routeLineTraveledCasingColor(Color.MAGENTA)
            .inActiveRouteLegsColor(Color.YELLOW)
            .inactiveRouteLegCasingColor(Color.BLACK)
            .build()
        val viewOptions = MapboxRouteLineViewOptions.Builder(ctx)
            .routeLineColorResources(colors)
            .build()
            .toData()
        val apiOptions = MapboxRouteLineApiOptions.Builder()
            .vanishingRouteLineEnabled(true)
            .styleInactiveRouteLegsIndependently(true)
            .build()
        val api = MapboxRouteLineApi(apiOptions)

        val expectedMaskingTrafficExpressionContents = listOf(
            StringChecker("step"),
            StringChecker("[line-progress]"),
            StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            DoubleChecker(0.0),
            StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            DoubleChecker(0.5021971),
            StringChecker("[rgba, 136.0, 136.0, 136.0, 1.0]"),
            DoubleChecker(0.5077762),
            StringChecker("[rgba, 204.0, 204.0, 204.0, 1.0]"),
            DoubleChecker(0.509591),
            StringChecker("[rgba, 136.0, 136.0, 136.0, 1.0]"),
            DoubleChecker(0.5151899),
            StringChecker("[rgba, 204.0, 204.0, 204.0, 1.0]"),
            DoubleChecker(0.5177997),
            StringChecker("[rgba, 136.0, 136.0, 136.0, 1.0]"),
        )
        val expectedMaskingBaseExpressionContents = listOf(
            StringChecker("step"),
            StringChecker("[line-progress]"),
            StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            DoubleChecker(0.0),
            StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            DoubleChecker(0.5021971),
            StringChecker("[rgba, 0.0, 0.0, 255.0, 1.0]"),
        )
        val expectedMaskingCasingExpressionContents = listOf(
            StringChecker("step"),
            StringChecker("[line-progress]"),
            StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            DoubleChecker(0.0),
            StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            DoubleChecker(0.5021971),
            StringChecker("[rgba, 47.0, 122.0, 198.0, 1.0]"),
        )
        val expectedMaskingTrailExpressionContents = listOf(
            StringChecker("step"),
            StringChecker("[line-progress]"),
            StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            DoubleChecker(0.0),
            StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            DoubleChecker(0.5021971),
            StringChecker("[rgba, 255.0, 0.0, 0.0, 1.0]"),
        )
        val expectedMaskingTrailCasingExpressionContents = listOf(
            StringChecker("step"),
            StringChecker("[line-progress]"),
            StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            DoubleChecker(0.0),
            StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            DoubleChecker(0.5021971),
            StringChecker("[rgba, 255.0, 0.0, 255.0, 1.0]"),
        )

        val result = api.setNavigationRoutes(listOf(multiLegRouteWithOverlap.navigationRoute))

        checkExpression(
            expectedMaskingTrafficExpressionContents,
            getAppliedExpression(
                result.value!!.routeLineMaskingLayerDynamicData!!.trafficExpressionCommandHolder!!,
                viewOptions,
                "line-gradient",
            ),
        )
        checkExpression(
            expectedMaskingBaseExpressionContents,
            getAppliedExpression(
                result.value!!.routeLineMaskingLayerDynamicData!!.baseExpressionCommandHolder,
                viewOptions,
                "line-gradient",
            ),
        )
        checkExpression(
            expectedMaskingCasingExpressionContents,
            getAppliedExpression(
                result.value!!.routeLineMaskingLayerDynamicData!!.casingExpressionCommandHolder,
                viewOptions,
                "line-gradient",
            ),
        )
        checkExpression(
            expectedMaskingTrailExpressionContents,
            getAppliedExpression(
                result.value!!.routeLineMaskingLayerDynamicData!!.trailExpressionCommandHolder!!,
                viewOptions,
                "line-gradient",
            ),
        )
        checkExpression(
            expectedMaskingTrailCasingExpressionContents,
            getAppliedExpression(
                result.value!!.routeLineMaskingLayerDynamicData!!
                    .trailCasingExpressionCommandHolder!!,
                viewOptions,
                "line-gradient",
            ),
        )

        val maskingData = result.value!!.routeLineMaskingLayerDynamicData!!
        assertTrue(
            maskingData.trafficExpressionCommandHolder!!.provider is LightRouteLineValueProvider,
        )
        assertTrue(
            maskingData.baseExpressionCommandHolder.provider is LightRouteLineValueProvider,
        )
        assertTrue(
            maskingData.casingExpressionCommandHolder.provider is LightRouteLineValueProvider,
        )
        assertTrue(
            maskingData.trailExpressionCommandHolder!!.provider is LightRouteLineValueProvider,
        )
        assertTrue(
            maskingData.trailCasingExpressionCommandHolder!!.provider
            is LightRouteLineValueProvider,
        )
    }

    @Test
    fun setRoutes_displaySoftGradientForTraffic() = coroutineRule.runBlockingTest {
        val colors = RouteLineColorResources.Builder()
            .routeLowCongestionColor(Color.GRAY)
            .routeUnknownCongestionColor(Color.LTGRAY)
            .routeDefaultColor(Color.BLUE)
            .routeLineTraveledColor(Color.RED)
            .routeLineTraveledCasingColor(Color.MAGENTA)
            .inActiveRouteLegsColor(Color.YELLOW)
            .inactiveRouteLegUnknownCongestionColor(Color.CYAN)
            .inactiveRouteLegLowCongestionColor(Color.rgb(22, 99, 66))
            .inactiveRouteLegModerateCongestionColor(Color.rgb(33, 99, 66))
            .inactiveRouteLegHeavyCongestionColor(Color.rgb(44, 99, 66))
            .inactiveRouteLegSevereCongestionColor(Color.rgb(55, 99, 66))
            .build()
        val viewOptions = MapboxRouteLineViewOptions.Builder(ctx)
            .routeLineColorResources(colors)
            .displaySoftGradientForTraffic(true)
            .softGradientTransition(30.0)
            .build()
            .toData()
        val apiOptions = MapboxRouteLineApiOptions.Builder()
            .styleInactiveRouteLegsIndependently(true)
            .vanishingRouteLineEnabled(true)
            .build()
        val api = MapboxRouteLineApi(apiOptions)

        val expectedTrafficExpressionContents = listOf(
            StringChecker("interpolate"),
            StringChecker("[linear]"),
            StringChecker("[line-progress]"),
            DoubleChecker(0.0),
            StringChecker("[rgba, 22.0, 99.0, 66.0, 1.0]"),
            DoubleChecker(0.485253290658251),
            StringChecker("[rgba, 22.0, 99.0, 66.0, 1.0]"),
            DoubleChecker(0.486808287578294),
            StringChecker("[rgba, 0.0, 255.0, 255.0, 1.0]"),
            DoubleChecker(0.486808287578294),
            StringChecker("[rgba, 0.0, 255.0, 255.0, 1.0]"),
            DoubleChecker(0.49440281360297),
            StringChecker("[rgba, 22.0, 99.0, 66.0, 1.0]"),
            DoubleChecker(0.49440281360297),
            StringChecker("[rgba, 22.0, 99.0, 66.0, 1.0]"),
            DoubleChecker(0.496059084362542),
            StringChecker("[rgba, 0.0, 255.0, 255.0, 1.0]"),
            DoubleChecker(0.496059084362542),
            StringChecker("[rgba, 0.0, 255.0, 255.0, 1.0]"),
            DoubleChecker(0.502197025505151),
            StringChecker("[rgba, 22.0, 99.0, 66.0, 1.0]"),
            DoubleChecker(0.502197025505151),
            StringChecker("[rgba, 22.0, 99.0, 66.0, 1.0]"),
            DoubleChecker(0.507776180581013),
            StringChecker("[rgba, 136.0, 136.0, 136.0, 1.0]"),
            DoubleChecker(0.507776180581013),
            StringChecker("[rgba, 136.0, 136.0, 136.0, 1.0]"),
            DoubleChecker(0.509590441827215),
            StringChecker("[rgba, 204.0, 204.0, 204.0, 1.0]"),
            DoubleChecker(0.509590441827215),
            StringChecker("[rgba, 204.0, 204.0, 204.0, 1.0]"),
            DoubleChecker(0.515189898985002),
            StringChecker("[rgba, 136.0, 136.0, 136.0, 1.0]"),
            DoubleChecker(0.515189898985002),
            StringChecker("[rgba, 136.0, 136.0, 136.0, 1.0]"),
            DoubleChecker(0.517799629815412),
            StringChecker("[rgba, 204.0, 204.0, 204.0, 1.0]"),
            DoubleChecker(0.517799629825412),
            StringChecker("[rgba, 204.0, 204.0, 204.0, 1.0]"),
            DoubleChecker(0.530695051821649),
            StringChecker("[rgba, 136.0, 136.0, 136.0, 1.0]"),
            DoubleChecker(1.0),
            StringChecker("[rgba, 136.0, 136.0, 136.0, 1.0]"),
        )
        val expectedTrailExpressionContents = listOf(
            StringChecker("step"),
            StringChecker("[line-progress]"),
            StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            DoubleChecker(0.0),
            StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            DoubleChecker(0.502197025505151),
            StringChecker("[rgba, 255.0, 0.0, 0.0, 1.0]"),
        )
        val expectedMaskingTrafficExpressionContents = listOf(
            StringChecker("interpolate"),
            StringChecker("[linear]"),
            StringChecker("[line-progress]"),
            DoubleChecker(0.0),
            StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            DoubleChecker(0.502197025505151),
            StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            DoubleChecker(0.507776180581013),
            StringChecker("[rgba, 136.0, 136.0, 136.0, 1.0]"),
            DoubleChecker(0.507776180581013),
            StringChecker("[rgba, 136.0, 136.0, 136.0, 1.0]"),
            DoubleChecker(0.509590441827215),
            StringChecker("[rgba, 204.0, 204.0, 204.0, 1.0]"),
            DoubleChecker(0.509590441827215),
            StringChecker("[rgba, 204.0, 204.0, 204.0, 1.0]"),
            DoubleChecker(0.515189898985002),
            StringChecker("[rgba, 136.0, 136.0, 136.0, 1.0]"),
            DoubleChecker(0.515189898985002),
            StringChecker("[rgba, 136.0, 136.0, 136.0, 1.0]"),
            DoubleChecker(0.517799629815412),
            StringChecker("[rgba, 204.0, 204.0, 204.0, 1.0]"),
            DoubleChecker(0.517799629825412),
            StringChecker("[rgba, 204.0, 204.0, 204.0, 1.0]"),
            DoubleChecker(0.530695051821649),
            StringChecker("[rgba, 136.0, 136.0, 136.0, 1.0]"),
            DoubleChecker(1.0),
            StringChecker("[rgba, 136.0, 136.0, 136.0, 1.0]"),
        )
        val expectedMaskingTrailExpressionContents = listOf(
            StringChecker("step"),
            StringChecker("[line-progress]"),
            StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            DoubleChecker(0.0),
            StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            DoubleChecker(0.502197025505151),
            StringChecker("[rgba, 255.0, 0.0, 0.0, 1.0]"),
        )

        val result = api.setNavigationRoutes(listOf(multiLegRouteWithOverlap.navigationRoute))

        checkExpression(
            expectedTrafficExpressionContents,
            getAppliedExpression(
                result.value!!.primaryRouteLineData.dynamicData!!.trafficExpressionCommandHolder!!,
                viewOptions,
                "line-gradient",
            ),
        )
        checkExpression(
            expectedMaskingTrafficExpressionContents,
            getAppliedExpression(
                result.value!!.routeLineMaskingLayerDynamicData!!.trafficExpressionCommandHolder!!,
                viewOptions,
                "line-gradient",
            ),
        )
        checkExpression(
            expectedTrailExpressionContents,
            getAppliedExpression(
                result.value!!.primaryRouteLineData.dynamicData!!.trailExpressionCommandHolder!!,
                viewOptions,
                "line-gradient",
            ),
        )
        checkExpression(
            expectedMaskingTrailExpressionContents,
            getAppliedExpression(
                result.value!!.routeLineMaskingLayerDynamicData!!.trailExpressionCommandHolder!!,
                viewOptions,
                "line-gradient",
            ),
        )

        val primaryLineData = result.value!!.primaryRouteLineData.dynamicData!!
        assertTrue(
            primaryLineData.trafficExpressionCommandHolder!!.provider
            is HeavyRouteLineValueProvider,
        )
        assertTrue(
            primaryLineData.trailExpressionCommandHolder!!.provider is LightRouteLineValueProvider,
        )

        val maskingData = result.value!!.routeLineMaskingLayerDynamicData!!
        assertTrue(
            maskingData.trafficExpressionCommandHolder!!.provider is LightRouteLineValueProvider,
        )
        assertTrue(
            maskingData.trailExpressionCommandHolder!!.provider is LightRouteLineValueProvider,
        )
    }

    @Test
    fun setRoutesWithCallback() = coroutineRule.runBlockingTest {
        val api = MapboxRouteLineApi(MapboxRouteLineApiOptions.Builder().build())
        val expectedCasingExpressionContents = listOf(
            StringChecker("rgba"),
            DoubleChecker(47.0),
            DoubleChecker(122.0),
            DoubleChecker(198.0),
            DoubleChecker(1.0),
        )
        val expectedRouteLineExpressionContents = listOf(
            StringChecker("rgba"),
            DoubleChecker(86.0),
            DoubleChecker(168.0),
            DoubleChecker(251.0),
            DoubleChecker(1.0),
        )
        val expectedTrafficLineExpressionContents = listOf(
            StringChecker("step"),
            StringChecker("[line-progress]"),
            StringChecker("[rgba, 255.0, 149.0, 0.0, 1.0]"),
            DoubleChecker(0.0),
            StringChecker("[rgba, 255.0, 149.0, 0.0, 1.0]"),
            DoubleChecker(0.057451),
            StringChecker("[rgba, 86.0, 168.0, 251.0, 1.0]"),
        )
        val expectedPrimaryRouteSourceGeometry = "LineString{type=LineString, bbox=null, " +
            "coordinates=[Point{type=Point, bbox=null, coordinates=[-122.523131, 37.975067]}," +
            " Point{type=Point, bbox=null, coordinates=[-122.523117, 37.975107]}, " +
            "Point{type=Point, bbox=null, coordinates=[-122.523579, 37.975173]}, " +
            "Point{type=Point, bbox=null, coordinates=[-122.523729, 37.975194]}, " +
            "Point{type=Point, bbox=null, coordinates=[-122.523671, 37.975379]}]}"
        val expectedWaypointFeature0 =
            "Point{type=Point, bbox=null, coordinates=[-122.523671, 37.975379]}"
        val expectedWaypointFeature1 =
            "Point{type=Point, bbox=null, coordinates=[-122.523131, 37.975067]}"
        val route = loadNavigationRoute(
            "short_route.json",
            waypoints = listOf(
                createWaypoint(
                    location = doubleArrayOf(-122.523671, 37.975379),
                ),
                createWaypoint(
                    location = doubleArrayOf(-122.523131, 37.975067),
                ),
            ),
        )
        val routes = listOf(route)

        var result: RouteSetValue? = null
        val consumer = MapboxNavigationConsumer<Expected<RouteLineError, RouteSetValue>> {
            result = it.value
        }
        api.setNavigationRoutes(routes, consumer)

        val viewOptions = MapboxRouteLineViewOptions.Builder(ctx).build().toData()
        checkExpression(
            expectedCasingExpressionContents,
            getAppliedExpression(
                result!!.primaryRouteLineData.dynamicData!!.casingExpressionCommandHolder,
                viewOptions,
                "line-gradient",
            ),
        )
        checkExpression(
            expectedRouteLineExpressionContents,
            getAppliedExpression(
                result!!.primaryRouteLineData.dynamicData!!.baseExpressionCommandHolder,
                viewOptions,
                "line-gradient",
            ),
        )
        checkExpression(
            expectedTrafficLineExpressionContents,
            getAppliedExpression(
                result!!.primaryRouteLineData.dynamicData!!.trafficExpressionCommandHolder!!,
                viewOptions,
                "line-gradient",
            ),
        )

        val primaryLineData = result!!.primaryRouteLineData.dynamicData!!
        assertTrue(
            primaryLineData.trafficExpressionCommandHolder!!.provider
            is HeavyRouteLineValueProvider,
        )
        assertTrue(
            primaryLineData.baseExpressionCommandHolder.provider is LightRouteLineValueProvider,
        )
        assertTrue(
            primaryLineData.casingExpressionCommandHolder.provider is LightRouteLineValueProvider,
        )

        assertEquals(
            expectedPrimaryRouteSourceGeometry,
            result!!.primaryRouteLineData.featureCollection.features()!![0].geometry().toString(),
        )
        assertTrue(
            result!!.alternativeRouteLinesData[0].featureCollection.features()!!.isEmpty(),
        )
        assertTrue(
            result!!.alternativeRouteLinesData[1].featureCollection.features()!!.isEmpty(),
        )
        assertEquals(
            expectedWaypointFeature0,
            result!!.waypointsSource.features()!![0].geometry().toString(),
        )
        assertEquals(
            expectedWaypointFeature1,
            result!!.waypointsSource.features()!![1].geometry().toString(),
        )
    }

    @Test
    fun `setRoutes with restrictions across legs`() = coroutineRule.runBlockingTest {
        val viewOptions = MapboxRouteLineViewOptions.Builder(ctx)
            .displayRestrictedRoadSections(true)
            .routeLineColorResources(
                RouteLineColorResources.Builder()
                    .restrictedRoadColor(Color.RED)
                    .build(),
            )
            .build()
            .toData()
        val apiOptions =
            MapboxRouteLineApiOptions.Builder().calculateRestrictedRoadSections(true).build()
        val api = MapboxRouteLineApi(apiOptions)
        val expectedRestrictedExpressionContents = listOf(
            StringChecker("step"),
            StringChecker("[line-progress]"),
            StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            DoubleChecker(0.0),
            StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            DoubleChecker(0.4459960518654729),
            StringChecker("[rgba, 255.0, 0.0, 0.0, 1.0]"),
            DoubleChecker(0.60435421),
            StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
        )
        val expectedMaskingRestrictedExpressionContents = listOf(
            StringChecker("step"),
            StringChecker("[line-progress]"),
            StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            DoubleChecker(0.0),
            StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            DoubleChecker(0.5102280025300375),
            StringChecker("[rgba, 255.0, 0.0, 0.0, 1.0]"),
            DoubleChecker(0.60435421),
            StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
        )
        val route = loadNavigationRoute("multileg_route_two_legs_with_restrictions.json")

        val result = api.setNavigationRoutes(listOf(route))

        checkExpression(
            expectedRestrictedExpressionContents,
            getAppliedExpression(
                result.value!!.primaryRouteLineData.dynamicData!!
                    .restrictedSectionExpressionCommandHolder!!,
                viewOptions,
                "line-gradient",
            ),
        )
        assertTrue(
            result.value!!.primaryRouteLineData.dynamicData!!
                .restrictedSectionExpressionCommandHolder?.provider
            is HeavyRouteLineValueProvider,
        )
        checkExpression(
            expectedMaskingRestrictedExpressionContents,
            getAppliedExpression(
                result.value!!.routeLineMaskingLayerDynamicData!!
                    .restrictedSectionExpressionCommandHolder!!,
                viewOptions,
                "line-gradient",
            ),
        )
        assertTrue(
            result.value!!.routeLineMaskingLayerDynamicData!!
                .restrictedSectionExpressionCommandHolder?.provider
            is LightRouteLineValueProvider,
        )
    }

    @Test
    fun `setRoutes with restrictions across legs when inactiveLegStyling and leg 0`() =
        coroutineRule.runBlockingTest {
            val viewOptions = MapboxRouteLineViewOptions.Builder(ctx)
                .displayRestrictedRoadSections(true)
                .routeLineColorResources(
                    RouteLineColorResources.Builder()
                        .restrictedRoadColor(Color.RED)
                        .inactiveRouteLegRestrictedRoadColor(Color.GREEN)
                        .build(),
                )
                .build()
                .toData()
            val apiOptions = MapboxRouteLineApiOptions.Builder()
                .calculateRestrictedRoadSections(true)
                .styleInactiveRouteLegsIndependently(true)
                .build()
            val api = MapboxRouteLineApi(apiOptions)
            val expectedRestrictedExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
                DoubleChecker(0.445996051865473),
                StringChecker("[rgba, 0.0, 255.0, 0.0, 1.0]"),
                DoubleChecker(0.510228002530038),
                StringChecker("[rgba, 255.0, 0.0, 0.0, 1.0]"),
                DoubleChecker(0.60435421),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            )
            val expectedMaskingRestrictedExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
                DoubleChecker(0.510228002530038),
                StringChecker("[rgba, 255.0, 0.0, 0.0, 1.0]"),
                DoubleChecker(0.60435421),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            )
            val route = loadNavigationRoute("multileg_route_two_legs_with_restrictions.json")

            val result = api.setNavigationRoutes(listOf(route), activeLegIndex = 0).value!!

            checkExpression(
                expectedRestrictedExpressionContents,
                getAppliedExpression(
                    result.primaryRouteLineData.dynamicData!!
                        .restrictedSectionExpressionCommandHolder!!,
                    viewOptions,
                    "line-gradient",
                ),
            )
            checkExpression(
                expectedMaskingRestrictedExpressionContents,
                getAppliedExpression(
                    result.routeLineMaskingLayerDynamicData!!
                        .restrictedSectionExpressionCommandHolder!!,
                    viewOptions,
                    "line-gradient",
                ),
            )

            assertTrue(
                result.primaryRouteLineData.dynamicData!!.restrictedSectionExpressionCommandHolder!!
                    .provider is HeavyRouteLineValueProvider,
            )
            assertTrue(
                result.routeLineMaskingLayerDynamicData.restrictedSectionExpressionCommandHolder!!
                    .provider is LightRouteLineValueProvider,
            )
        }

    @Test
    fun `setRoutes with restrictions across legs when inactiveLegStyling and leg 1`() =
        coroutineRule.runBlockingTest {
            val viewOptions = MapboxRouteLineViewOptions.Builder(ctx)
                .displayRestrictedRoadSections(true)
                .routeLineColorResources(
                    RouteLineColorResources.Builder()
                        .restrictedRoadColor(Color.RED)
                        .inactiveRouteLegRestrictedRoadColor(Color.GREEN)
                        .build(),
                )
                .build()
                .toData()
            val apiOptions = MapboxRouteLineApiOptions.Builder()
                .styleInactiveRouteLegsIndependently(true)
                .calculateRestrictedRoadSections(true)
                .build()
            val api = MapboxRouteLineApi(apiOptions)
            val expectedRestrictedExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
                DoubleChecker(0.445996051865473),
                StringChecker("[rgba, 255.0, 0.0, 0.0, 1.0]"),
                DoubleChecker(0.510228002530038),
                StringChecker("[rgba, 0.0, 255.0, 0.0, 1.0]"),
                DoubleChecker(0.60435421),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            )
            val expectedMaskingRestrictedExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
                DoubleChecker(0.445996051865473),
                StringChecker("[rgba, 255.0, 0.0, 0.0, 1.0]"),
                DoubleChecker(0.510228002530038),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            )
            val route = loadNavigationRoute("multileg_route_two_legs_with_restrictions.json")

            val result = api.setNavigationRoutes(listOf(route), activeLegIndex = 1).value!!

            checkExpression(
                expectedRestrictedExpressionContents,
                getAppliedExpression(
                    result.primaryRouteLineData.dynamicData!!
                        .restrictedSectionExpressionCommandHolder!!,
                    viewOptions,
                    "line-gradient",
                ),
            )
            checkExpression(
                expectedMaskingRestrictedExpressionContents,
                getAppliedExpression(
                    result.routeLineMaskingLayerDynamicData!!
                        .restrictedSectionExpressionCommandHolder!!,
                    viewOptions,
                    "line-gradient",
                ),
            )

            assertTrue(
                result.primaryRouteLineData.dynamicData.restrictedSectionExpressionCommandHolder!!
                    .provider is HeavyRouteLineValueProvider,
            )
            assertTrue(
                result.routeLineMaskingLayerDynamicData.restrictedSectionExpressionCommandHolder!!
                    .provider is LightRouteLineValueProvider,
            )
        }

    @Test
    fun updateWithRouteProgress_displaySoftGradientForTraffic() = coroutineRule.runBlockingTest {
        val colors = RouteLineColorResources.Builder()
            .routeLowCongestionColor(Color.GRAY)
            .routeUnknownCongestionColor(Color.LTGRAY)
            .routeDefaultColor(Color.BLUE)
            .routeLineTraveledColor(Color.RED)
            .routeLineTraveledCasingColor(Color.MAGENTA)
            .inActiveRouteLegsColor(Color.YELLOW)
            .inactiveRouteLegUnknownCongestionColor(Color.CYAN)
            .inactiveRouteLegLowCongestionColor(Color.rgb(22, 99, 66))
            .inactiveRouteLegModerateCongestionColor(Color.rgb(33, 99, 66))
            .inactiveRouteLegHeavyCongestionColor(Color.rgb(44, 99, 66))
            .inactiveRouteLegSevereCongestionColor(Color.rgb(55, 99, 66))
            .build()
        val viewOptions = MapboxRouteLineViewOptions.Builder(ctx)
            .routeLineColorResources(colors)
            .displaySoftGradientForTraffic(true)
            .softGradientTransition(30.0)
            .build()
            .toData()
        val apiOptions = MapboxRouteLineApiOptions.Builder()
            .styleInactiveRouteLegsIndependently(true)
            .vanishingRouteLineEnabled(true)
            .build()

        val api = MapboxRouteLineApi(apiOptions)
        api.setNavigationRoutes(listOf(multiLegRouteWithOverlap.navigationRoute), -1)
        val routeProgress = mockRouteProgress(multiLegRouteWithOverlap.navigationRoute)
        every { routeProgress.currentRouteGeometryIndex } returns 15

        val expectedTrafficExpressionContents = listOf(
            StringChecker("interpolate"),
            StringChecker("[linear]"),
            StringChecker("[line-progress]"),
            DoubleChecker(0.0),
            StringChecker("[rgba, 22.0, 99.0, 66.0, 1.0]"),
            DoubleChecker(0.485253290658251),
            StringChecker("[rgba, 22.0, 99.0, 66.0, 1.0]"),
            DoubleChecker(0.486808287578294),
            StringChecker("[rgba, 0.0, 255.0, 255.0, 1.0]"),
            DoubleChecker(0.486808287578294),
            StringChecker("[rgba, 0.0, 255.0, 255.0, 1.0]"),
            DoubleChecker(0.49440281360297),
            StringChecker("[rgba, 22.0, 99.0, 66.0, 1.0]"),
            DoubleChecker(0.49440281360297),
            StringChecker("[rgba, 22.0, 99.0, 66.0, 1.0]"),
            DoubleChecker(0.496059084362542),
            StringChecker("[rgba, 0.0, 255.0, 255.0, 1.0]"),
            DoubleChecker(0.496059084362542),
            StringChecker("[rgba, 0.0, 255.0, 255.0, 1.0]"),
            DoubleChecker(0.502197025505151),
            StringChecker("[rgba, 22.0, 99.0, 66.0, 1.0]"),
            DoubleChecker(0.502197025505151),
            StringChecker("[rgba, 22.0, 99.0, 66.0, 1.0]"),
            DoubleChecker(0.507776180581013),
            StringChecker("[rgba, 136.0, 136.0, 136.0, 1.0]"),
            DoubleChecker(0.507776180581013),
            StringChecker("[rgba, 136.0, 136.0, 136.0, 1.0]"),
            DoubleChecker(0.509590441827215),
            StringChecker("[rgba, 204.0, 204.0, 204.0, 1.0]"),
            DoubleChecker(0.509590441827215),
            StringChecker("[rgba, 204.0, 204.0, 204.0, 1.0]"),
            DoubleChecker(0.515189898985002),
            StringChecker("[rgba, 136.0, 136.0, 136.0, 1.0]"),
            DoubleChecker(0.515189898985002),
            StringChecker("[rgba, 136.0, 136.0, 136.0, 1.0]"),
            DoubleChecker(0.517799629815412),
            StringChecker("[rgba, 204.0, 204.0, 204.0, 1.0]"),
            DoubleChecker(0.517799629825412),
            StringChecker("[rgba, 204.0, 204.0, 204.0, 1.0]"),
            DoubleChecker(0.530695051821649),
            StringChecker("[rgba, 136.0, 136.0, 136.0, 1.0]"),
            DoubleChecker(1.0),
            StringChecker("[rgba, 136.0, 136.0, 136.0, 1.0]"),
        )

        val expectedMaskingTrafficExpressionContents = listOf(
            StringChecker("interpolate"),
            StringChecker("[linear]"),
            StringChecker("[line-progress]"),
            DoubleChecker(0.0),
            StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            DoubleChecker(0.502197025505151),
            StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            DoubleChecker(0.507776180581013),
            StringChecker("[rgba, 136.0, 136.0, 136.0, 1.0]"),
            DoubleChecker(0.507776180581013),
            StringChecker("[rgba, 136.0, 136.0, 136.0, 1.0]"),
            DoubleChecker(0.509590441827215),
            StringChecker("[rgba, 204.0, 204.0, 204.0, 1.0]"),
            DoubleChecker(0.509590441827215),
            StringChecker("[rgba, 204.0, 204.0, 204.0, 1.0]"),
            DoubleChecker(0.515189898985002),
            StringChecker("[rgba, 136.0, 136.0, 136.0, 1.0]"),
            DoubleChecker(0.515189898985002),
            StringChecker("[rgba, 136.0, 136.0, 136.0, 1.0]"),
            DoubleChecker(0.517799629815412),
            StringChecker("[rgba, 204.0, 204.0, 204.0, 1.0]"),
            DoubleChecker(0.517799629825412),
            StringChecker("[rgba, 204.0, 204.0, 204.0, 1.0]"),
            DoubleChecker(0.530695051821649),
            StringChecker("[rgba, 136.0, 136.0, 136.0, 1.0]"),
            DoubleChecker(1.0),
            StringChecker("[rgba, 136.0, 136.0, 136.0, 1.0]"),
        )

        var callbackCalled = false
        api.updateWithRouteProgress(routeProgress) { result ->
            runBlocking {
                checkExpression(
                    expectedTrafficExpressionContents,
                    getAppliedExpression(
                        result.value!!.primaryRouteLineDynamicData!!
                            .trafficExpressionCommandHolder!!,
                        viewOptions,
                        "line-gradient",
                    ),
                )
                assertTrue(
                    result.value!!.primaryRouteLineDynamicData!!
                        .trafficExpressionCommandHolder!!.provider
                    is HeavyRouteLineValueProvider,
                )
                checkExpression(
                    expectedMaskingTrafficExpressionContents,
                    getAppliedExpression(
                        result.value!!.routeLineMaskingLayerDynamicData!!
                            .trafficExpressionCommandHolder!!,
                        viewOptions,
                        "line-gradient",
                    ),
                )
                assertTrue(
                    result.value!!.routeLineMaskingLayerDynamicData!!
                        .trafficExpressionCommandHolder!!.provider
                    is LightRouteLineValueProvider,
                )

                callbackCalled = true
            }
        }
        assertTrue(callbackCalled)
    }

    @Test
    fun `updateWithRouteProgress with restrictions across legs when inactiveLegStyling and leg 0`() =
        coroutineRule.runBlockingTest {
            val viewOptions = MapboxRouteLineViewOptions.Builder(ctx)
                .displayRestrictedRoadSections(true)
                .routeLineColorResources(
                    RouteLineColorResources.Builder()
                        .restrictedRoadColor(Color.RED)
                        .inactiveRouteLegRestrictedRoadColor(Color.GREEN)
                        .build(),
                )
                .build()
                .toData()
            val apiOptions = MapboxRouteLineApiOptions.Builder()
                .styleInactiveRouteLegsIndependently(true)
                .calculateRestrictedRoadSections(true)
                .build()
            val api = MapboxRouteLineApi(apiOptions)
            val expectedRestrictedExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
                DoubleChecker(0.445996051865473),
                StringChecker("[rgba, 0.0, 255.0, 0.0, 1.0]"),
                DoubleChecker(0.510228002530038),
                StringChecker("[rgba, 255.0, 0.0, 0.0, 1.0]"),
                DoubleChecker(0.60435421),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            )
            val expectedMaskingRestrictedExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
                DoubleChecker(0.510228002530038),
                StringChecker("[rgba, 255.0, 0.0, 0.0, 1.0]"),
                DoubleChecker(0.60435421),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            )
            val route = loadNavigationRoute("multileg_route_two_legs_with_restrictions.json")

            api.setNavigationRoutes(listOf(route), activeLegIndex = -1)
            val routeProgress = mockRouteProgress(route)
            every { routeProgress.currentLegProgress!!.legIndex } returns 0

            var callbackCalled = false
            api.updateWithRouteProgress(routeProgress) {
                runBlocking {
                    val result = it.value!!
                    checkExpression(
                        expectedRestrictedExpressionContents,
                        getAppliedExpression(
                            result.primaryRouteLineDynamicData!!
                                .restrictedSectionExpressionCommandHolder!!,
                            viewOptions,
                            "line-gradient",
                        ),
                    )
                    checkExpression(
                        expectedMaskingRestrictedExpressionContents,
                        getAppliedExpression(
                            result.routeLineMaskingLayerDynamicData!!
                                .restrictedSectionExpressionCommandHolder!!,
                            viewOptions,
                            "line-gradient",
                        ),
                    )
                    assertTrue(
                        result.primaryRouteLineDynamicData
                            .restrictedSectionExpressionCommandHolder!!.provider
                        is HeavyRouteLineValueProvider,
                    )
                    assertTrue(
                        result.routeLineMaskingLayerDynamicData
                            .restrictedSectionExpressionCommandHolder!!.provider
                        is LightRouteLineValueProvider,
                    )
                    callbackCalled = true
                }
            }
            assertTrue(callbackCalled)
        }

    @Test
    fun `updateWithRouteProgress with restrictions across legs when inactiveLegStyling and leg 1`() =
        coroutineRule.runBlockingTest {
            val viewOptions = MapboxRouteLineViewOptions.Builder(ctx)
                .displayRestrictedRoadSections(true)
                .routeLineColorResources(
                    RouteLineColorResources.Builder()
                        .restrictedRoadColor(Color.RED)
                        .inactiveRouteLegRestrictedRoadColor(Color.GREEN)
                        .build(),
                )
                .build()
                .toData()
            val apiOptions = MapboxRouteLineApiOptions.Builder()
                .styleInactiveRouteLegsIndependently(true)
                .calculateRestrictedRoadSections(true)
                .build()
            val api = MapboxRouteLineApi(apiOptions)
            val expectedRestrictedExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
                DoubleChecker(0.445996051865473),
                StringChecker("[rgba, 255.0, 0.0, 0.0, 1.0]"),
                DoubleChecker(0.510228002530038),
                StringChecker("[rgba, 0.0, 255.0, 0.0, 1.0]"),
                DoubleChecker(0.60435421),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            )
            val expectedMaskingRestrictedExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
                DoubleChecker(0.445996051865473),
                StringChecker("[rgba, 255.0, 0.0, 0.0, 1.0]"),
                DoubleChecker(0.510228002530038),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            )
            val route = loadNavigationRoute("multileg_route_two_legs_with_restrictions.json")

            api.setNavigationRoutes(listOf(route))

            val routeProgress = mockRouteProgress(route)
            every { routeProgress.currentLegProgress!!.legIndex } returns 1
            var callbackCalled = false
            api.updateWithRouteProgress(routeProgress) {
                runBlocking {
                    val result = it.value!!
                    checkExpression(
                        expectedRestrictedExpressionContents,
                        getAppliedExpression(
                            result.primaryRouteLineDynamicData!!
                                .restrictedSectionExpressionCommandHolder!!,
                            viewOptions,
                            "line-gradient",
                        ),
                    )
                    checkExpression(
                        expectedMaskingRestrictedExpressionContents,
                        getAppliedExpression(
                            result.routeLineMaskingLayerDynamicData!!
                                .restrictedSectionExpressionCommandHolder!!,
                            viewOptions,
                            "line-gradient",
                        ),
                    )
                    val dynamicData = result.primaryRouteLineDynamicData
                    assertTrue(
                        dynamicData.restrictedSectionExpressionCommandHolder!!.provider
                        is HeavyRouteLineValueProvider,
                    )
                    val maskingData = result.routeLineMaskingLayerDynamicData
                    assertTrue(
                        maskingData.restrictedSectionExpressionCommandHolder!!.provider
                        is LightRouteLineValueProvider,
                    )
                    callbackCalled = true
                }
            }
            assertTrue(callbackCalled)
        }

    @Test
    fun `setVanishingOffset with restrictions across legs when inactiveLegStyling and leg 0`() =
        coroutineRule.runBlockingTest {
            val viewOptions = MapboxRouteLineViewOptions.Builder(ctx)
                .displayRestrictedRoadSections(true)
                .routeLineColorResources(
                    RouteLineColorResources.Builder()
                        .restrictedRoadColor(Color.RED)
                        .inactiveRouteLegRestrictedRoadColor(Color.GREEN)
                        .build(),
                )
                .build()
                .toData()
            val apiOptions = MapboxRouteLineApiOptions.Builder()
                .styleInactiveRouteLegsIndependently(true)
                .calculateRestrictedRoadSections(true)
                .vanishingRouteLineEnabled(true)
                .build()
            val api = MapboxRouteLineApi(apiOptions)
            val expectedRestrictedExpressionContents = Value.valueOf(1.0)
            val route = loadNavigationRoute("multileg_route_two_legs_with_restrictions.json")

            api.setNavigationRoutes(listOf(route), activeLegIndex = 0)
            val result = api.setVanishingOffset(0.0).value!!
            val actual = getAppliedValue(
                result
                    .primaryRouteLineDynamicData!!
                    .restrictedSectionExpressionCommandHolder!!,
                viewOptions,
                "line-trim-start",
            )

            assertEquals(
                expectedRestrictedExpressionContents,
                actual,
            )
            assertTrue(
                result.primaryRouteLineDynamicData.restrictedSectionExpressionCommandHolder!!
                    .provider is LightRouteLineValueProvider,
            )
        }

    @Test
    fun `setVanishingOffset with restrictions across legs when inactiveLegStyling and leg 1`() =
        coroutineRule.runBlockingTest {
            val viewOptions = MapboxRouteLineViewOptions.Builder(ctx)
                .displayRestrictedRoadSections(true)
                .routeLineColorResources(
                    RouteLineColorResources.Builder()
                        .restrictedRoadColor(Color.RED)
                        .inactiveRouteLegRestrictedRoadColor(Color.GREEN)
                        .build(),
                )
                .build()
                .toData()
            val apiOptions = MapboxRouteLineApiOptions.Builder()
                .styleInactiveRouteLegsIndependently(true)
                .calculateRestrictedRoadSections(true)
                .vanishingRouteLineEnabled(true)
                .build()
            val api = MapboxRouteLineApi(apiOptions)
            val expectedRestrictedExpressionContents = Value.valueOf(1.0)
            val route = loadNavigationRoute("multileg_route_two_legs_with_restrictions.json")

            api.setNavigationRoutes(listOf(route), activeLegIndex = 1)
            val result = api.setVanishingOffset(0.0).value!!
            val actual = getAppliedValue(
                result
                    .primaryRouteLineDynamicData!!
                    .restrictedSectionExpressionCommandHolder!!,
                viewOptions,
                "line-trim-start",
            )

            assertEquals(
                expectedRestrictedExpressionContents,
                actual,
            )
            assertTrue(
                result.primaryRouteLineDynamicData.restrictedSectionExpressionCommandHolder!!
                    .provider is LightRouteLineValueProvider,
            )
        }

    @Test
    fun setRoutesTrafficExpressionsWithAlternativeRoutes() = coroutineRule.runBlockingTest {
        val expectedPrimaryTrafficLineExpressionContents = listOf(
            StringChecker("step"),
            StringChecker("[line-progress]"),
            StringChecker("[rgba, 255.0, 149.0, 0.0, 1.0]"),
            DoubleChecker(0.0),
            StringChecker("[rgba, 255.0, 149.0, 0.0, 1.0]"),
            DoubleChecker(0.05745011),
            StringChecker("[rgba, 86.0, 168.0, 251.0, 1.0]"),
        )
        val expectedAlternative1TrafficLineExpressionContents = listOf(
            StringChecker("step"),
            StringChecker("[line-progress]"),
            StringChecker("[rgba, 134.0, 148.0, 165.0, 1.0]"),
            DoubleChecker(0.0),
            StringChecker("[rgba, 134.0, 148.0, 165.0, 1.0]"),
            DoubleChecker(0.5041359),
            StringChecker("[rgba, 190.0, 160.0, 135.0, 1.0]"),
            DoubleChecker(0.57213761),
            StringChecker("[rgba, 134.0, 148.0, 165.0, 1.0]"),
        )
        val expectedAlternative2TrafficLineExpressionContents = listOf(
            StringChecker("step"),
            StringChecker("[line-progress]"),
            StringChecker("[rgba, 134.0, 148.0, 165.0, 1.0]"),
            DoubleChecker(0.0),
            StringChecker("[rgba, 134.0, 148.0, 165.0, 1.0]"),
            DoubleChecker(0.07244878),
            StringChecker("[rgba, 181.0, 130.0, 129.0, 1.0]"),
            DoubleChecker(0.111921),
            StringChecker("[rgba, 134.0, 148.0, 165.0, 1.0]"),
            DoubleChecker(0.1338196),
            StringChecker("[rgba, 190.0, 160.0, 135.0, 1.0]"),
            DoubleChecker(0.1352459),
            StringChecker("[rgba, 134.0, 148.0, 165.0, 1.0]"),
            DoubleChecker(0.1827723),
            StringChecker("[rgba, 190.0, 160.0, 135.0, 1.0]"),
            DoubleChecker(0.2088926),
            StringChecker("[rgba, 134.0, 148.0, 165.0, 1.0]"),
            DoubleChecker(0.246663),
            StringChecker("[rgba, 190.0, 160.0, 135.0, 1.0]"),
            DoubleChecker(0.257095),
            StringChecker("[rgba, 134.0, 148.0, 165.0, 1.0]"),
            DoubleChecker(0.9003068),
            StringChecker("[rgba, 190.0, 160.0, 135.0, 1.0]"),
            DoubleChecker(0.90878266),
            StringChecker("[rgba, 134.0, 148.0, 165.0, 1.0]"),
        )
        val viewOptions = MapboxRouteLineViewOptions.Builder(ctx).build().toData()
        val apiOptions = MapboxRouteLineApiOptions.Builder().build()
        val api = MapboxRouteLineApi(apiOptions)
        val route = loadNavigationRoute("short_route.json")
        val altRoute1 = loadNavigationRoute("route-with-road-classes.txt")
        val altRoute2 = loadNavigationRoute("multileg_route.json")
        val routes = listOf(
            NavigationRouteLine(route, null),
            NavigationRouteLine(altRoute1, null),
            NavigationRouteLine(altRoute2, null),
        )

        val result = api.setNavigationRouteLines(routes)

        checkExpression(
            expectedPrimaryTrafficLineExpressionContents,
            getAppliedExpression(
                result.value!!.primaryRouteLineData.dynamicData!!.trafficExpressionCommandHolder!!,
                viewOptions,
                "line-gradient",
            ),
        )
        checkExpression(
            expectedAlternative1TrafficLineExpressionContents,
            getAppliedExpression(
                result.value!!.alternativeRouteLinesData[0].dynamicData!!
                    .trafficExpressionCommandHolder!!,
                viewOptions,
                "line-gradient",
            ),
        )
        assertTrue(
            result.value!!.alternativeRouteLinesData[0].dynamicData!!.trafficExpressionCommandHolder
                ?.provider is HeavyRouteLineValueProvider,
        )
        checkExpression(
            expectedAlternative2TrafficLineExpressionContents,
            getAppliedExpression(
                result.value!!.alternativeRouteLinesData[1].dynamicData!!
                    .trafficExpressionCommandHolder!!,
                viewOptions,
                "line-gradient",
            ),
        )
        assertTrue(
            result.value!!.alternativeRouteLinesData[1].dynamicData!!
                .trafficExpressionCommandHolder?.provider is HeavyRouteLineValueProvider,
        )
    }

    @Test
    fun `setNavigationRoutes takes into account inactiveLegStyling and legIndex`() =
        coroutineRule.runBlockingTest {
            val route = loadNavigationRoute("multileg_route.json")
            val altRoute = loadNavigationRoute("multileg_route_with_overlap.json")

            val apiWithIndependentInactiveStylingEnabled = MapboxRouteLineApi(
                MapboxRouteLineApiOptions.Builder()
                    .styleInactiveRouteLegsIndependently(true)
                    .build(),
            )
            val apiWithIndependentInactiveStylingDisabled = MapboxRouteLineApi(
                MapboxRouteLineApiOptions.Builder().build(),
            )
            val viewOptions = MapboxRouteLineViewOptions.Builder(ctx).build().toData()

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
                getAppliedExpression(
                    it.primaryRouteLineData.dynamicData!!.trafficExpressionCommandHolder!!,
                    viewOptions,
                    "line-gradient",
                )
            }
            checkEquals(equalValuesToCheck) {
                getAppliedExpression(
                    it.primaryRouteLineData.dynamicData!!.trafficExpressionCommandHolder!!,
                    viewOptions,
                    "line-gradient",
                )
            }
            checkNotEquals(differentValuesToCheck) {
                getAppliedExpression(
                    it.primaryRouteLineData.dynamicData!!.baseExpressionCommandHolder,
                    viewOptions,
                    "line-gradient",
                )
            }
            checkEquals(equalValuesToCheck) {
                getAppliedExpression(
                    it.primaryRouteLineData.dynamicData!!.baseExpressionCommandHolder,
                    viewOptions,
                    "line-gradient",
                )
            }
            checkNotEquals(differentValuesToCheck) {
                getAppliedExpression(
                    it.primaryRouteLineData.dynamicData!!.casingExpressionCommandHolder,
                    viewOptions,
                    "line-gradient",
                )
            }
            checkEquals(equalValuesToCheck) {
                getAppliedExpression(
                    it.primaryRouteLineData.dynamicData!!.casingExpressionCommandHolder,
                    viewOptions,
                    "line-gradient",
                )
            }

            // trail and trailCasing expressions also depend on inactiveLegStyling,
            // because for the case of inactive leg colours being transparent, we don't want
            // the trail layer to show up underneath them, so we substitute some parts for transparent colour.
            checkNotEquals(
                setOf(
                    independentStylingEnabledLegZeroResult,
                    independentStylingDisabledLegZeroResult,
                ),
            ) {
                getAppliedExpression(
                    it.primaryRouteLineData.dynamicData!!.trailExpressionCommandHolder!!,
                    viewOptions,
                    "line-gradient",
                )
            }
            checkNotEquals(
                setOf(
                    independentStylingEnabledLegOneResult,
                    independentStylingDisabledLegOneResult,
                ),
            ) {
                getAppliedExpression(
                    it.primaryRouteLineData.dynamicData!!.trailExpressionCommandHolder!!,
                    viewOptions,
                    "line-gradient",
                )
            }
            checkEquals(
                setOf(
                    independentStylingEnabledLegZeroResult,
                    independentStylingEnabledLegOneResult,
                ),
            ) {
                getAppliedExpression(
                    it.primaryRouteLineData.dynamicData!!.trailExpressionCommandHolder!!,
                    viewOptions,
                    "line-gradient",
                )
            }
            checkEquals(
                setOf(
                    independentStylingDisabledLegZeroResult,
                    independentStylingDisabledLegOneResult,
                ),
            ) {
                getAppliedExpression(
                    it.primaryRouteLineData.dynamicData!!.trailExpressionCommandHolder!!,
                    viewOptions,
                    "line-gradient",
                )
            }

            checkNotEquals(
                setOf(
                    independentStylingEnabledLegZeroResult,
                    independentStylingDisabledLegZeroResult,
                ),
            ) {
                getAppliedExpression(
                    it.primaryRouteLineData.dynamicData!!.trailCasingExpressionCommandHolder!!,
                    viewOptions,
                    "line-gradient",
                )
            }
            checkNotEquals(
                setOf(
                    independentStylingEnabledLegOneResult,
                    independentStylingDisabledLegOneResult,
                ),
            ) {
                getAppliedExpression(
                    it.primaryRouteLineData.dynamicData!!.trailCasingExpressionCommandHolder!!,
                    viewOptions,
                    "line-gradient",
                )
            }
            checkEquals(
                setOf(
                    independentStylingEnabledLegZeroResult,
                    independentStylingEnabledLegOneResult,
                ),
            ) {
                getAppliedExpression(
                    it.primaryRouteLineData.dynamicData!!.trailCasingExpressionCommandHolder!!,
                    viewOptions,
                    "line-gradient",
                )
            }
            checkEquals(
                setOf(
                    independentStylingDisabledLegZeroResult,
                    independentStylingDisabledLegOneResult,
                ),
            ) {
                getAppliedExpression(
                    it.primaryRouteLineData.dynamicData!!.trailCasingExpressionCommandHolder!!,
                    viewOptions,
                    "line-gradient",
                )
            }

            checkEquals(allValuesToCheck) {
                getAppliedExpression(
                    it.alternativeRouteLinesData.first().dynamicData!!
                        .trafficExpressionCommandHolder!!,
                    viewOptions,
                    "line-gradient",
                )
            }
            checkEquals(allValuesToCheck) {
                getAppliedExpression(
                    it.alternativeRouteLinesData.first().dynamicData!!.baseExpressionCommandHolder,
                    viewOptions,
                    "line-gradient",
                )
            }
            checkEquals(allValuesToCheck) {
                getAppliedExpression(
                    it.alternativeRouteLinesData.first().dynamicData!!
                        .casingExpressionCommandHolder,
                    viewOptions,
                    "line-gradient",
                )
            }
            checkEquals(allValuesToCheck) {
                getAppliedExpression(
                    it.alternativeRouteLinesData.first().dynamicData!!
                        .trailExpressionCommandHolder!!,
                    viewOptions,
                    "line-gradient",
                )
            }
            checkEquals(allValuesToCheck) {
                getAppliedExpression(
                    it.alternativeRouteLinesData.first().dynamicData!!
                        .trailCasingExpressionCommandHolder!!,
                    viewOptions,
                    "line-gradient",
                )
            }
        }

    private suspend fun checkNotEquals(
        values: Iterable<Expected<RouteLineError, RouteSetValue>>,
        expressionExtractor: suspend (RouteSetValue) -> Expression,
    ) {
        val expressions = values.map { expressionExtractor(it.value!!) }
        for (i in 0 until expressions.size) {
            for (j in i + 1 until expressions.size) {
                assertNotEquals(expressions[i], expressions[j])
            }
        }
    }

    private suspend fun checkEquals(
        values: Iterable<Expected<RouteLineError, RouteSetValue>>,
        expressionExtractor: suspend (RouteSetValue) -> Expression,
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
    fun getRouteDrawData() = coroutineRule.runBlockingTest {
        val viewOptions = MapboxRouteLineViewOptions.Builder(ctx).build().toData()
        val apiOptions = MapboxRouteLineApiOptions.Builder().build()
        val api = MapboxRouteLineApi(apiOptions)
        val expectedCasingExpressionContents = listOf(
            StringChecker("rgba"),
            DoubleChecker(47.0),
            DoubleChecker(122.0),
            DoubleChecker(198.0),
            DoubleChecker(1.0),
        )
        val expectedRouteLineExpressionContents = listOf(
            StringChecker("rgba"),
            DoubleChecker(86.0),
            DoubleChecker(168.0),
            DoubleChecker(251.0),
            DoubleChecker(1.0),
        )
        val expectedTrafficLineExpressionContents = listOf(
            StringChecker("step"),
            StringChecker("[line-progress]"),
            StringChecker("[rgba, 255.0, 149.0, 0.0, 1.0]"),
            DoubleChecker(0.0),
            StringChecker("[rgba, 255.0, 149.0, 0.0, 1.0]"),
            DoubleChecker(0.0574502),
            StringChecker("[rgba, 86.0, 168.0, 251.0, 1.0]"),
        )
        val expectedPrimaryRouteSourceGeometry = "LineString{type=LineString, bbox=null, " +
            "coordinates=[Point{type=Point, bbox=null, coordinates=[-122.523131, 37.975067]}," +
            " Point{type=Point, bbox=null, coordinates=[-122.523117, 37.975107]}, " +
            "Point{type=Point, bbox=null, coordinates=[-122.523579, 37.975173]}, " +
            "Point{type=Point, bbox=null, coordinates=[-122.523729, 37.975194]}, " +
            "Point{type=Point, bbox=null, coordinates=[-122.523671, 37.975379]}]}"
        val expectedWaypointFeature0 =
            "Point{type=Point, bbox=null, coordinates=[-122.523671, 37.975379]}"
        val expectedWaypointFeature1 =
            "Point{type=Point, bbox=null, coordinates=[-122.523131, 37.975067]}"
        val route = loadNavigationRoute(
            "short_route.json",
            waypoints = listOf(
                createWaypoint(location = doubleArrayOf(-122.523671, 37.975379)),
                createWaypoint(location = doubleArrayOf(-122.523131, 37.975067)),
            ),
        )
        val routes = listOf(route)
        api.setNavigationRoutes(routes)

        val result = api.getRouteDrawData()

        checkExpression(
            expectedCasingExpressionContents,
            getAppliedExpression(
                result.value!!.primaryRouteLineData.dynamicData!!.casingExpressionCommandHolder,
                viewOptions,
                "line-gradient",
            ),
        )
        checkExpression(
            expectedRouteLineExpressionContents,
            getAppliedExpression(
                result.value!!.primaryRouteLineData.dynamicData!!.baseExpressionCommandHolder,
                viewOptions,
                "line-gradient",
            ),
        )
        checkExpression(
            expectedTrafficLineExpressionContents,
            getAppliedExpression(
                result.value!!.primaryRouteLineData.dynamicData!!.trafficExpressionCommandHolder!!,
                viewOptions,
                "line-gradient",
            ),
        )

        assertTrue(
            result.value!!.primaryRouteLineData.dynamicData!!.casingExpressionCommandHolder
                .provider is LightRouteLineValueProvider,
        )
        assertTrue(
            result.value!!.primaryRouteLineData.dynamicData!!.baseExpressionCommandHolder
                .provider is LightRouteLineValueProvider,
        )
        assertTrue(
            result.value!!.primaryRouteLineData.dynamicData!!.trafficExpressionCommandHolder!!
                .provider is HeavyRouteLineValueProvider,
        )

        assertEquals(
            expectedPrimaryRouteSourceGeometry,
            result.value!!.primaryRouteLineData.featureCollection.features()!![0].geometry()
                .toString(),
        )
        assertTrue(
            result.value!!.alternativeRouteLinesData[0].featureCollection.features()!!.isEmpty(),
        )
        assertTrue(
            result.value!!.alternativeRouteLinesData[1].featureCollection.features()!!.isEmpty(),
        )
        assertEquals(
            expectedWaypointFeature0,
            result.value!!.waypointsSource.features()!![0].geometry().toString(),
        )
        assertEquals(
            expectedWaypointFeature1,
            result.value!!.waypointsSource.features()!![1].geometry().toString(),
        )
    }

    @Test
    fun updateTraveledRouteLine() = coroutineRule.runBlockingTest {
        val viewOptions = MapboxRouteLineViewOptions.Builder(ctx)
            .displayRestrictedRoadSections(false)
            .build()
            .toData()
        val apiOptions = MapboxRouteLineApiOptions.Builder()
            .vanishingRouteLineEnabled(true)
            .calculateRestrictedRoadSections(false)
            .vanishingRouteLineUpdateIntervalNano(0)
            .build()
        val api = MapboxRouteLineApi(apiOptions)
        val expectedTrimStart = 0.6759230551
        val route = shortRoute.navigationRoute
        val lineString = LineString.fromPolyline(
            route.directionsRoute.geometry() ?: "",
            Constants.PRECISION_6,
        )
        val routeProgress = shortRoute.mockRouteProgress(stepIndexValue = 2)

        api.updateVanishingPointState(RouteProgressState.TRACKING)
        api.setNavigationRoutes(listOf(route))
        api.updateUpcomingRoutePointIndex(routeProgress)

        val result = api.updateTraveledRouteLine(lineString.coordinates()[1])

        assertEquals(
            expectedTrimStart,
            getAppliedValue(
                result.value!!.primaryRouteLineDynamicData!!
                    .casingExpressionCommandHolder,
                viewOptions,
                "line-trim-start",
            ).contents as Double,
            0.0000000001,
        )
        assertEquals(
            expectedTrimStart,
            getAppliedValue(
                result.value!!.primaryRouteLineDynamicData!!
                    .baseExpressionCommandHolder,
                viewOptions,
                "line-trim-start",
            ).contents as Double,
            0.0000000001,
        )
        assertEquals(
            expectedTrimStart,
            getAppliedValue(
                result.value!!
                    .primaryRouteLineDynamicData!!.trafficExpressionCommandHolder!!,
                viewOptions,
                "line-trim-start",
            ).contents as Double,
            0.0000000001,
        )
        assertEquals(
            expectedTrimStart,
            getAppliedValue(
                result.value!!
                    .primaryRouteLineDynamicData!!.restrictedSectionExpressionCommandHolder!!,
                viewOptions,
                "line-trim-start",
            ).contents as Double,
            0.0000000001,
        )

        assertTrue(
            result.value!!.primaryRouteLineDynamicData!!.casingExpressionCommandHolder
                .provider is LightRouteLineValueProvider,
        )
        assertTrue(
            result.value!!.primaryRouteLineDynamicData!!.baseExpressionCommandHolder
                .provider is LightRouteLineValueProvider,
        )
        assertTrue(
            result.value!!.primaryRouteLineDynamicData!!.trafficExpressionCommandHolder!!
                .provider is LightRouteLineValueProvider,
        )
        assertTrue(
            result.value!!.primaryRouteLineDynamicData!!.restrictedSectionExpressionCommandHolder!!
                .provider is LightRouteLineValueProvider,
        )
    }

    @Test
    fun updateTraveledRouteLine_displaySoftGradientForTraffic() = coroutineRule.runBlockingTest {
        val colors = RouteLineColorResources.Builder()
            .routeLowCongestionColor(Color.GRAY)
            .routeUnknownCongestionColor(Color.LTGRAY)
            .routeDefaultColor(Color.BLUE)
            .routeLineTraveledColor(Color.RED)
            .routeLineTraveledCasingColor(Color.MAGENTA)
            .inActiveRouteLegsColor(Color.YELLOW)
            .inactiveRouteLegUnknownCongestionColor(Color.CYAN)
            .inactiveRouteLegLowCongestionColor(Color.rgb(22, 99, 66))
            .inactiveRouteLegModerateCongestionColor(Color.rgb(33, 99, 66))
            .inactiveRouteLegHeavyCongestionColor(Color.rgb(44, 99, 66))
            .inactiveRouteLegSevereCongestionColor(Color.rgb(55, 99, 66))
            .build()
        val viewOptions = MapboxRouteLineViewOptions.Builder(ctx)
            .routeLineColorResources(colors)
            .displaySoftGradientForTraffic(true)
            .softGradientTransition(30.0)
            .build()
            .toData()
        val apiOptions = MapboxRouteLineApiOptions.Builder()
            .styleInactiveRouteLegsIndependently(true)
            .vanishingRouteLineEnabled(true)
            .build()
        val api = MapboxRouteLineApi(apiOptions)
        val route = multiLegRouteWithOverlap.navigationRoute
        val lineString = LineString.fromPolyline(
            route.directionsRoute.geometry() ?: "",
            Constants.PRECISION_6,
        )
        api.setNavigationRoutes(listOf(route))
        val routeProgress = mockRouteProgress(route)
        every { routeProgress.currentRouteGeometryIndex } returns 15
        api.updateUpcomingRoutePointIndex(routeProgress)
        api.updateVanishingPointState(routeProgress.currentState)
        val result = api.updateTraveledRouteLine(lineString.coordinates()[16])

        val expectedTrafficExpressionContents = 0.6224151328
        val expectedMaskingTrafficExpressionContents = 0.6224151328

        assertEquals(
            expectedTrafficExpressionContents,
            getAppliedValue(
                result.value!!.primaryRouteLineDynamicData!!.trafficExpressionCommandHolder!!,
                viewOptions,
                "line-trim-start",
            ).contents as Double,
            0.0001,
        )
        assertEquals(
            expectedMaskingTrafficExpressionContents,
            getAppliedValue(
                result.value!!.routeLineMaskingLayerDynamicData!!.trafficExpressionCommandHolder!!,
                viewOptions,
                "line-trim-start",
            ).contents as Double,
            0.0001,
        )
        assertNull(result.value!!.routeLineMaskingLayerDynamicData!!.trailExpressionCommandHolder)
        assertNull(
            result.value!!.routeLineMaskingLayerDynamicData!!.trailCasingExpressionCommandHolder,
        )

        assertTrue(
            result.value!!.primaryRouteLineDynamicData!!.trafficExpressionCommandHolder!!
                .provider is LightRouteLineValueProvider,
        )
        assertTrue(
            result.value!!.routeLineMaskingLayerDynamicData!!.trafficExpressionCommandHolder!!
                .provider is LightRouteLineValueProvider,
        )
    }

    @Test
    fun updateTraveledRouteLine_pointUpdateIntervalRespected() =
        coroutineRule.runBlockingTest {
            val apiOptions = MapboxRouteLineApiOptions.Builder()
                .vanishingRouteLineEnabled(true)
                .calculateRestrictedRoadSections(false)
                .vanishingRouteLineUpdateIntervalNano(TimeUnit.MILLISECONDS.toNanos(1200))
                .build()
            val api = MapboxRouteLineApi(apiOptions)
            val route = shortRoute.navigationRoute
            val lineString = LineString.fromPolyline(
                route.directionsRoute.geometry() ?: "",
                Constants.PRECISION_6,
            )
            val routeProgress = shortRoute.mockRouteProgress(stepIndexValue = 2)

            api.updateVanishingPointState(RouteProgressState.TRACKING)
            api.setNavigationRoutes(listOf(route))
            api.updateUpcomingRoutePointIndex(routeProgress)

            pauseDispatcher {
                val result1 = api.updateTraveledRouteLine(lineString.coordinates()[1])
                assertTrue(result1.isValue)

                Thread.sleep(1000L)
                api.updateUpcomingRoutePointIndex(routeProgress) // only update the progress
                Thread.sleep(300L) // in summary we've waited for 1.3s since last point update
                val result2 = api.updateTraveledRouteLine(lineString.coordinates()[2])
                assertTrue(result2.isValue) // should succeed because threshold was 1.2s

                Thread.sleep(500L) // wait less than threshold
                val result3 = api.updateTraveledRouteLine(lineString.coordinates()[1])
                assertTrue(result3.isError)
            }
        }

    @Test
    fun updateTraveledRouteLine_noUpdateWhenPointDistanceTooSmall() =
        coroutineRule.runBlockingTest {
            val apiOptions = MapboxRouteLineApiOptions.Builder()
                .vanishingRouteLineEnabled(true)
                .vanishingRouteLineUpdateIntervalNano(TimeUnit.MILLISECONDS.toNanos(1200))
                .calculateRestrictedRoadSections(false)
                .build()

            val api = MapboxRouteLineApi(apiOptions)
            val route = shortRoute.navigationRoute
            val lineString = LineString.fromPolyline(
                route.directionsRoute.geometry() ?: "",
                Constants.PRECISION_6,
            )
            val routeProgress = shortRoute.mockRouteProgress(stepIndexValue = 2)

            api.updateVanishingPointState(RouteProgressState.TRACKING)
            api.setNavigationRoutes(listOf(route))
            api.updateUpcomingRoutePointIndex(routeProgress)

            pauseDispatcher {
                val result1 = api.updateTraveledRouteLine(lineString.coordinates()[1])
                assertTrue(result1.isValue)

                Thread.sleep(1000L)
                api.updateUpcomingRoutePointIndex(routeProgress)
                Thread.sleep(300L)
                val result2 = api.updateTraveledRouteLine(lineString.coordinates()[1])
                assertTrue(result2.isError)
            }
        }

    @Test
    fun `updateTraveledRouteLine when route has restrictions and legs not styled independently`() =
        coroutineRule.runBlockingTest {
            val apiOptions = MapboxRouteLineApiOptions.Builder()
                .vanishingRouteLineEnabled(true)
                .calculateRestrictedRoadSections(true)
                .vanishingRouteLineUpdateIntervalNano(0)
                .build()
            val viewOptions = MapboxRouteLineViewOptions.Builder(ctx)
                .displayRestrictedRoadSections(true)
                .build()
                .toData()
            val api = MapboxRouteLineApi(apiOptions)
            val expectedRestrictedExpressionContents = 0.9588858771
            val route = routeWithRestrictions.navigationRoute
            val lineString = LineString.fromPolyline(
                route.directionsRoute.geometry() ?: "",
                Constants.PRECISION_6,
            )
            val routeProgress = routeWithRestrictions.mockRouteProgress(stepIndexValue = 2)

            api.updateVanishingPointState(RouteProgressState.TRACKING)
            api.setNavigationRoutes(listOf(route))
            api.updateUpcomingRoutePointIndex(routeProgress)

            mockkObject(MapboxRouteLineUtils)
            val result = api.updateTraveledRouteLine(lineString.coordinates()[1])

            val data = result.value!!.primaryRouteLineDynamicData!!

            assertEquals(
                expectedRestrictedExpressionContents,
                getAppliedValue(
                    data.restrictedSectionExpressionCommandHolder!!,
                    viewOptions,
                    "line-trim-start",
                ).contents as Double,
                0.00000001,
            )

            assertTrue(
                data.restrictedSectionExpressionCommandHolder!!.provider
                is LightRouteLineValueProvider,
            )

            verify(exactly = 0) {
                // the cache key is based on the full hash of the Directions Route
                // and is not suited to be used as frequently as the vanishing route line needs it
                MapboxRouteLineUtils.extractRouteData(any(), any())
            }
            unmockkObject(MapboxRouteLineUtils)
        }

    @Test
    fun updateTraveledRouteLine_whenRouteRestrictionsEnabledButHasNone() =
        coroutineRule.runBlockingTest {
            val epsilon = 0.0000000001
            val expectedLineTrimStart = 0.6759230551
            val viewOptions = MapboxRouteLineViewOptions.Builder(ctx)
                .displayRestrictedRoadSections(true)
                .build()
                .toData()
            val apiOptions = MapboxRouteLineApiOptions.Builder()
                .vanishingRouteLineUpdateIntervalNano(0)
                .vanishingRouteLineEnabled(true)
                .calculateRestrictedRoadSections(true)
                .build()
            val api = MapboxRouteLineApi(apiOptions)
            val route = shortRoute.navigationRoute
            val lineString = LineString.fromPolyline(
                route.directionsRoute.geometry() ?: "",
                Constants.PRECISION_6,
            )
            val routeProgress = shortRoute.mockRouteProgress(stepIndexValue = 2)

            api.updateVanishingPointState(RouteProgressState.TRACKING)
            api.setNavigationRoutes(listOf(route))
            api.updateUpcomingRoutePointIndex(routeProgress)

            val result = api.updateTraveledRouteLine(lineString.coordinates()[1])

            val data = result.value!!.primaryRouteLineDynamicData!!

            assertEquals(
                expectedLineTrimStart,
                getAppliedValue(
                    data.casingExpressionCommandHolder,
                    viewOptions,
                    "line-trim-start",
                ).contents as Double,
                epsilon,
            )
            assertEquals(
                expectedLineTrimStart,
                getAppliedValue(
                    data.baseExpressionCommandHolder,
                    viewOptions,
                    "line-trim-start",
                ).contents as Double,
                epsilon,
            )
            assertEquals(
                expectedLineTrimStart,
                getAppliedValue(
                    data.trafficExpressionCommandHolder!!,
                    viewOptions,
                    "line-trim-start",
                ).contents as Double,
                epsilon,
            )
            assertEquals(
                expectedLineTrimStart,
                getAppliedValue(
                    data.restrictedSectionExpressionCommandHolder!!,
                    viewOptions,
                    "line-trim-start",
                ).contents as Double,
                epsilon,
            )

            assertTrue(
                data.trafficExpressionCommandHolder.provider is LightRouteLineValueProvider,
            )
            assertTrue(
                data.baseExpressionCommandHolder.provider is LightRouteLineValueProvider,
            )
            assertTrue(
                data.casingExpressionCommandHolder.provider is LightRouteLineValueProvider,
            )
            assertTrue(
                data.restrictedSectionExpressionCommandHolder.provider
                is LightRouteLineValueProvider,
            )
        }

    @Test
    fun updateWithRouteProgress_whenDeEmphasizeInactiveLegSegments() =
        coroutineRule.runBlockingTest {
            val expectedTrafficExpContents = Value.valueOf(1.0)
            val route = multiLegRouteTwoLegs.navigationRoute
            val apiOptions = MapboxRouteLineApiOptions.Builder()
                .styleInactiveRouteLegsIndependently(true)
                .calculateRestrictedRoadSections(true)
                .vanishingRouteLineEnabled(true)
                .build()
            val viewOptions = MapboxRouteLineViewOptions.Builder(ctx)
                .displaySoftGradientForTraffic(false)
                .displayRestrictedRoadSections(false)
                .softGradientTransition(30.0)
                .build()
                .toData()
            val api = MapboxRouteLineApi(apiOptions)
            val routeProgress = multiLegRouteTwoLegs.mockRouteProgress()
            api.updateVanishingPointState(RouteProgressState.TRACKING)
            api.setNavigationRoutes(listOf(route))
            api.updateWithRouteProgress(routeProgress) {}

            val result = api.setVanishingOffset(0.0).value!!

            assertEquals(
                expectedTrafficExpContents,
                getAppliedValue(
                    result.primaryRouteLineDynamicData!!
                        .trafficExpressionCommandHolder!!,
                    viewOptions,
                    "line-trim-start",
                ),
            )
            assertTrue(
                result.primaryRouteLineDynamicData!!.trafficExpressionCommandHolder!!
                    .provider is LightRouteLineValueProvider,
            )
        }

    @Test
    fun setVanishingOffset() = runBlocking {
        val apiOptions = MapboxRouteLineApiOptions.Builder()
            .vanishingRouteLineEnabled(true)
            .build()
        val viewOptions = MapboxRouteLineViewOptions.Builder(ctx).build().toData()
        val trafficExpressionContents = Value.valueOf(0.5)
        val routeLineExpressionContents = Value.valueOf(0.5)
        val casingExpressionContents = Value.valueOf(0.5)
        val api = MapboxRouteLineApi(apiOptions)

        api.setNavigationRoutes(listOf(shortRoute.navigationRoute))

        val result = api.setVanishingOffset(.5)

        assertEquals(
            trafficExpressionContents,
            getAppliedValue(
                result.value!!.primaryRouteLineDynamicData!!
                    .trafficExpressionCommandHolder!!,
                viewOptions,
                "line-trim-start",
            ),
        )
        assertEquals(
            routeLineExpressionContents,
            getAppliedValue(
                result.value!!.primaryRouteLineDynamicData!!
                    .baseExpressionCommandHolder,
                viewOptions,
                "line-trim-start",
            ),
        )
        assertEquals(
            casingExpressionContents,
            getAppliedValue(
                result.value!!.primaryRouteLineDynamicData!!
                    .casingExpressionCommandHolder,
                viewOptions,
                "line-trim-start",
            ),
        )
        assertTrue(
            result.value!!.primaryRouteLineDynamicData!!.trafficExpressionCommandHolder!!
                .provider is LightRouteLineValueProvider,
        )
        assertTrue(
            result.value!!.primaryRouteLineDynamicData!!.baseExpressionCommandHolder!!
                .provider is LightRouteLineValueProvider,
        )
        assertTrue(
            result.value!!.primaryRouteLineDynamicData!!.casingExpressionCommandHolder!!
                .provider is LightRouteLineValueProvider,
        )
    }

    @Test
    fun setVanishingOffset_displaySoftGradientForTraffic() = coroutineRule.runBlockingTest {
        val colors = RouteLineColorResources.Builder()
            .routeLowCongestionColor(Color.GRAY)
            .routeUnknownCongestionColor(Color.LTGRAY)
            .routeDefaultColor(Color.BLUE)
            .routeLineTraveledColor(Color.RED)
            .routeLineTraveledCasingColor(Color.MAGENTA)
            .inActiveRouteLegsColor(Color.YELLOW)
            .inactiveRouteLegUnknownCongestionColor(Color.CYAN)
            .inactiveRouteLegLowCongestionColor(Color.rgb(22, 99, 66))
            .inactiveRouteLegModerateCongestionColor(Color.rgb(33, 99, 66))
            .inactiveRouteLegHeavyCongestionColor(Color.rgb(44, 99, 66))
            .inactiveRouteLegSevereCongestionColor(Color.rgb(55, 99, 66))
            .build()
        val viewOptions = MapboxRouteLineViewOptions.Builder(ctx)
            .routeLineColorResources(colors)
            .displaySoftGradientForTraffic(true)
            .softGradientTransition(30.0)
            .build()
            .toData()
        val apiOptions = MapboxRouteLineApiOptions.Builder()
            .styleInactiveRouteLegsIndependently(true)
            .vanishingRouteLineEnabled(true)
            .build()
        val api = MapboxRouteLineApi(apiOptions)
        val route = multiLegRouteWithOverlap.navigationRoute
        api.setNavigationRoutes(listOf(route))
        val result = api.setVanishingOffset(.4)

        val expectedTrafficExpressionContents = Value.valueOf(0.6)
        val expectedMaskingTrafficExpressionContents = Value.valueOf(0.6)

        assertEquals(
            expectedTrafficExpressionContents,
            getAppliedValue(
                result.value!!.primaryRouteLineDynamicData!!.trafficExpressionCommandHolder!!,
                viewOptions,
                "line-trim-start",
            ),
        )
        assertEquals(
            expectedMaskingTrafficExpressionContents,
            getAppliedValue(
                result.value!!.routeLineMaskingLayerDynamicData!!.trafficExpressionCommandHolder!!,
                viewOptions,
                "line-trim-start",
            ),
        )
        assertTrue(
            result.value!!.primaryRouteLineDynamicData!!.trafficExpressionCommandHolder!!
                .provider is LightRouteLineValueProvider,
        )
        assertTrue(
            result.value!!.routeLineMaskingLayerDynamicData!!.trafficExpressionCommandHolder!!
                .provider is LightRouteLineValueProvider,
        )
        assertNull(result.value!!.routeLineMaskingLayerDynamicData!!.trailExpressionCommandHolder)
        assertNull(
            result.value!!.routeLineMaskingLayerDynamicData!!.trailCasingExpressionCommandHolder,
        )
    }

    @Test
    fun setVanishingOffset_withRestrictionsEnabledPrimaryRouteNull() = runBlocking {
        val apiOptions = MapboxRouteLineApiOptions.Builder()
            .vanishingRouteLineEnabled(true)
            .calculateRestrictedRoadSections(true)
            .build()
        val api = MapboxRouteLineApi(apiOptions)

        val result = api.setVanishingOffset(.5)

        assertTrue(result.isError)
    }

    @Test
    fun setVanishingOffset_withRestrictionsEnabled() = coroutineRule.runBlockingTest {
        val viewOptions = MapboxRouteLineViewOptions.Builder(ctx)
            .displayRestrictedRoadSections(true)
            .build()
            .toData()
        val apiOptions = MapboxRouteLineApiOptions.Builder()
            .vanishingRouteLineEnabled(true)
            .calculateRestrictedRoadSections(true)
            .build()
        val trafficExpressionContents = Value.valueOf(0.5)
        val routeLineExpressionContents = Value.valueOf(0.5)
        val casingExpressionContents = Value.valueOf(0.5)
        val restrictedExpressionContents = Value.valueOf(0.5)
        val route = loadNavigationRoute("route-with-restrictions.json")

        val api = MapboxRouteLineApi(apiOptions)
        api.setNavigationRouteLines(listOf(NavigationRouteLine(route, null)))

        val result = api.setVanishingOffset(.5)

        assertEquals(
            trafficExpressionContents,
            getAppliedValue(
                result.value!!.primaryRouteLineDynamicData!!
                    .trafficExpressionCommandHolder!!,
                viewOptions,
                "line-trim-start",
            ),
        )
        assertEquals(
            routeLineExpressionContents,
            getAppliedValue(
                result.value!!.primaryRouteLineDynamicData!!
                    .baseExpressionCommandHolder,
                viewOptions,
                "line-trim-start",
            ),
        )
        assertEquals(
            casingExpressionContents,
            getAppliedValue(
                result.value!!.primaryRouteLineDynamicData!!
                    .casingExpressionCommandHolder,
                viewOptions,
                "line-trim-start",
            ),
        )
        assertEquals(
            restrictedExpressionContents,
            getAppliedValue(
                result.value!!.primaryRouteLineDynamicData!!
                    .restrictedSectionExpressionCommandHolder!!,
                viewOptions,
                "line-trim-start",
            ),
        )

        assertTrue(
            result.value!!.primaryRouteLineDynamicData!!.trafficExpressionCommandHolder!!
                .provider is LightRouteLineValueProvider,
        )
        assertTrue(
            result.value!!.primaryRouteLineDynamicData!!.baseExpressionCommandHolder!!
                .provider is LightRouteLineValueProvider,
        )
        assertTrue(
            result.value!!.primaryRouteLineDynamicData!!.casingExpressionCommandHolder!!
                .provider is LightRouteLineValueProvider,
        )
        assertTrue(
            result.value!!.primaryRouteLineDynamicData!!.restrictedSectionExpressionCommandHolder!!
                .provider is LightRouteLineValueProvider,
        )
    }

    @Test
    fun setVanishingOffset_whenHasRestrictionsButDisabled() = coroutineRule.runBlockingTest {
        val viewOptions = MapboxRouteLineViewOptions.Builder(ctx)
            .displayRestrictedRoadSections(false)
            .build()
            .toData()
        val apiOptions = MapboxRouteLineApiOptions.Builder()
            .vanishingRouteLineEnabled(true)
            .calculateRestrictedRoadSections(false)
            .build()
        val trafficExpressionContents = Value.valueOf(0.5)
        val routeLineExpressionContents = Value.valueOf(0.5)
        val casingExpressionContents = Value.valueOf(0.5)
        val restrictedExpressionContents = Value.valueOf(0.5)
        val route = loadNavigationRoute("route-with-restrictions.json")

        val api = MapboxRouteLineApi(apiOptions)
        api.setNavigationRouteLines(listOf(NavigationRouteLine(route, null)))

        val result = api.setVanishingOffset(.5)

        assertEquals(
            trafficExpressionContents,
            getAppliedValue(
                result.value!!.primaryRouteLineDynamicData!!
                    .trafficExpressionCommandHolder!!,
                viewOptions,
                "line-trim-start",
            ),
        )
        assertEquals(
            routeLineExpressionContents,
            getAppliedValue(
                result.value!!.primaryRouteLineDynamicData!!
                    .baseExpressionCommandHolder,
                viewOptions,
                "line-trim-start",
            ),
        )
        assertEquals(
            casingExpressionContents,
            getAppliedValue(
                result.value!!.primaryRouteLineDynamicData!!
                    .casingExpressionCommandHolder,
                viewOptions,
                "line-trim-start",
            ),
        )
        assertEquals(
            restrictedExpressionContents,
            getAppliedValue(
                result.value!!.primaryRouteLineDynamicData!!
                    .restrictedSectionExpressionCommandHolder!!,
                viewOptions,
                "line-trim-start",
            ),
        )
    }

    @Test
    fun setRouteAsyncCallsReturnsCorrectRouteSuspend() = coroutineRule.runBlockingTest {
        val shortRoute = listOf(NavigationRouteLine(loadNavigationRoute("short_route.json"), null))
        val longRoute = listOf(
            NavigationRouteLine(loadNavigationRoute("cross-country-route.json"), null),
        )
        val viewOptions = MapboxRouteLineViewOptions.Builder(ctx).build().toData()
        val apiOptions = MapboxRouteLineApiOptions.Builder().build()
        val api = MapboxRouteLineApi(apiOptions)

        val longRouteDef = async {
            val result = api.setNavigationRouteLines(longRoute)
            (
                getAppliedExpression(
                    result
                        .value!!
                        .primaryRouteLineData
                        .dynamicData!!
                        .trafficExpressionCommandHolder!!,
                    viewOptions,
                    "line-gradient",
                )
                    .contents as ArrayList<*>
                ).size
        }
        delay(40)
        val shortRouteDef = async {
            val result = api.setNavigationRouteLines(shortRoute)
            (
                getAppliedExpression(
                    result
                        .value!!
                        .primaryRouteLineData
                        .dynamicData!!
                        .trafficExpressionCommandHolder!!,
                    viewOptions,
                    "line-gradient",
                )
                    .contents as ArrayList<*>
                ).size
        }

        assertEquals(7, shortRouteDef.await())
        assertEquals(625, longRouteDef.await())
    }

    @Test
    fun alternativelyStyleSegmentsNotInLeg() = coroutineRule.runBlockingTest {
        val apiOptions = MapboxRouteLineApiOptions.Builder().build()
        val route = loadNavigationRoute("multileg-route-two-legs.json")
        val segments = MapboxRouteLineUtils.calculateRouteLineSegments(
            route,
            listOf(),
            true,
            apiOptions,
        )
        val api = MapboxRouteLineApi(apiOptions)

        val result = api.alternativelyStyleSegmentsNotInLeg(
            1,
            segments,
            InactiveRouteColors(),
        )

        assertEquals(19, result.size)
        assertEquals(SegmentColorType.INACTIVE_LOW_CONGESTION, result.first().segmentColorType)
        assertEquals(0, result.first().legIndex)
        assertEquals(SegmentColorType.INACTIVE_MODERATE_CONGESTION, result[4].segmentColorType)
        assertEquals(0, result[6].legIndex)
        // this will be ignored, it's only here because the test route has an incorrect geometry
        // with a duplicate point
        assertEquals(0.4897719974699625, result[7].offset, 0.01)
        assertEquals(SegmentColorType.INACTIVE_UNKNOWN_CONGESTION, result[6].segmentColorType)
        assertEquals(1, result[8].legIndex)
        assertEquals(0.4897719974699625, result[8].offset, 0.01)
    }

    @Test
    fun `set routes - with alternative metadata - vanished until deviation point`() =
        coroutineRule.runBlockingTest {
            val viewOptions = MapboxRouteLineViewOptions.Builder(ctx).build().toData()
            val apiOptions = MapboxRouteLineApiOptions.Builder().build()
            val api = MapboxRouteLineApi(apiOptions)
            val response = DirectionsResponse.fromJson(
                FileUtils.loadJsonFixture(
                    "route_response_alternative_start.json",
                ),
            )
            val routeOptions = response.routes().first().routeOptions()!!
            val routes = createNavigationRoutes(
                response = DirectionsResponse.fromJson(
                    FileUtils.loadJsonFixture(
                        "route_response_alternative_start.json",
                    ),
                ),
                options = routeOptions,
                routerOrigin = RouterOrigin.ONLINE,
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
            val expectedPrimaryTrafficExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 86.0, 168.0, 251.0, 1.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 86.0, 168.0, 251.0, 1.0]"),
            )
            val expectedBaseExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 134.0, 148.0, 165.0, 1.0]"),
                DoubleChecker(0.96143871),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            )
            val expectedCasingExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 114.0, 126.0, 141.0, 1.0]"),
                DoubleChecker(0.96143871),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            )
            val expectedAlternativeTrafficExpressionContents = listOf(
                StringChecker("step"),
                StringChecker("[line-progress]"),
                StringChecker("[rgba, 134.0, 148.0, 165.0, 1.0]"),
                DoubleChecker(0.0),
                StringChecker("[rgba, 134.0, 148.0, 165.0, 1.0]"),
                DoubleChecker(0.26146675),
                StringChecker("[rgba, 190.0, 160.0, 135.0, 1.0]"),
                DoubleChecker(0.3436878),
                StringChecker("[rgba, 134.0, 148.0, 165.0, 1.0]"),
                DoubleChecker(0.9614388),
                StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            )

            checkExpression(
                expectedPrimaryTrafficExpressionContents,
                getAppliedExpression(
                    result
                        .primaryRouteLineData
                        .dynamicData!!
                        .trafficExpressionCommandHolder!!,
                    viewOptions,
                    "line-gradient",
                ),
            )
            assertTrue(
                result.primaryRouteLineData.dynamicData.trafficExpressionCommandHolder!!
                    .provider is HeavyRouteLineValueProvider,
            )
            checkExpression(
                expectedBaseExpressionContents,
                getAppliedExpression(
                    result
                        .alternativeRouteLinesData[0]
                        .dynamicData!!
                        .baseExpressionCommandHolder,
                    viewOptions,
                    "line-gradient",
                ),
            )
            assertTrue(
                result.alternativeRouteLinesData[0].dynamicData!!.baseExpressionCommandHolder
                    .provider is LightRouteLineValueProvider,
            )
            checkExpression(
                expectedCasingExpressionContents,
                getAppliedExpression(
                    result
                        .alternativeRouteLinesData[0]
                        .dynamicData!!
                        .casingExpressionCommandHolder,
                    viewOptions,
                    "line-gradient",
                ),
            )
            assertTrue(
                result.alternativeRouteLinesData[0].dynamicData!!.casingExpressionCommandHolder
                    .provider is LightRouteLineValueProvider,
            )
            checkExpression(
                expectedAlternativeTrafficExpressionContents,
                getAppliedExpression(
                    result
                        .alternativeRouteLinesData[0]
                        .dynamicData!!
                        .trafficExpressionCommandHolder!!,
                    viewOptions,
                    "line-gradient",
                ),
            )
            assertTrue(
                result.alternativeRouteLinesData[0].dynamicData!!.trafficExpressionCommandHolder!!
                    .provider is HeavyRouteLineValueProvider,
            )
        }

    @Test
    fun getAlternativeRoutesDeviationOffsetsTest() {
        val routeData = FileUtils.loadJsonFixture("route_response_alternative_start.json")
        val response = DirectionsResponse.fromJson(routeData)
        val routeOptions = response.routes().first().routeOptions()!!
        val routes = createNavigationRoutes(
            response = DirectionsResponse.fromJson(
                routeData,
            ),
            options = routeOptions,
            routerOrigin = RouterOrigin.ONLINE,
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

        val result = MapboxRouteLineUtils.getAlternativeRouteDeviationOffsets(
            alternativeRouteMetadata,
        )

        assertEquals(0.03856129838762756, result, 0.000000001)
    }

    @Test
    fun `getAlternativeRoutesDeviationOffsetsTest empty distances`() {
        val route = mockk<NavigationRoute> {
            every { id } returns "abc#0"
        }
        val alternativeRouteMetadata = mockk<AlternativeRouteMetadata> {
            every { navigationRoute } returns route
        }

        val result = MapboxRouteLineUtils.getAlternativeRouteDeviationOffsets(
            alternativeRouteMetadata,
            distancesProvider = {
                RouteLineGranularDistances(
                    1.0,
                    emptyArray(),
                    emptyArray(),
                    emptyArray(),
                )
            },
        )

        assertEquals(0.0, result, 0.000000001)
        verify {
            logger.logW(
                "Remaining distances array size is 0 " +
                    "and the full distance is 1.0 - " +
                    "unable to calculate the deviation point of the alternative with ID " +
                    "'abc#0' to hide the portion that overlaps " +
                    "with the primary route.",
                "MapboxRouteLineUtils",
            )
        }
    }

    @Test
    fun `getAlternativeRoutesDeviationOffsetsTest full distance is zero`() {
        val route = mockk<NavigationRoute> {
            every { id } returns "abc#0"
        }
        val alternativeRouteMetadata = mockk<AlternativeRouteMetadata> {
            every { navigationRoute } returns route
        }

        val result = MapboxRouteLineUtils.getAlternativeRouteDeviationOffsets(
            alternativeRouteMetadata,
            distancesProvider = {
                RouteLineGranularDistances(
                    0.0,
                    arrayOf(mockk(relaxed = true), mockk(relaxed = true)),
                    arrayOf(arrayOf(mockk(relaxed = true), mockk(relaxed = true))),
                    arrayOf(arrayOf(arrayOf(mockk(relaxed = true), mockk(relaxed = true)))),
                )
            },
        )

        assertEquals(0.0, result, 0.000000001)
        verify {
            logger.logW(
                "Remaining distances array size is 2 " +
                    "and the full distance is 0.0 - " +
                    "unable to calculate the deviation point of the alternative with ID " +
                    "'abc#0' to hide the portion that overlaps " +
                    "with the primary route.",
                "MapboxRouteLineUtils",
            )
        }
    }

    @Test
    fun `getAlternativeRoutesDeviationOffsetsTest remaining distance greater than full distance`() {
        val route = mockk<NavigationRoute> {
            every { id } returns "abc#0"
        }
        val alternativeRouteMetadata = mockk<AlternativeRouteMetadata> {
            every { navigationRoute } returns route
            every { forkIntersectionOfAlternativeRoute } returns mockk {
                // the remaining distance at this index (30)
                // is greater than overall distance (15)
                every { geometryIndexInRoute } returns 1
            }
        }

        val result = MapboxRouteLineUtils.getAlternativeRouteDeviationOffsets(
            alternativeRouteMetadata,
            distancesProvider = {
                RouteLineGranularDistances(
                    completeDistance = 15.0,
                    routeDistances = arrayOf(
                        RouteLineDistancesIndex(
                            point = mockk(),
                            distanceRemaining = 40.0,
                        ),
                        RouteLineDistancesIndex(
                            point = mockk(),
                            distanceRemaining = 30.0,
                        ),
                        RouteLineDistancesIndex(
                            point = mockk(),
                            distanceRemaining = 20.0,
                        ),
                    ),
                    legsDistances = emptyArray(),
                    stepsDistances = emptyArray(),
                )
            },
        )

        assertEquals(0.0, result, 0.000000001)
        verify {
            logger.logW(
                "distance remaining > full distance - " +
                    "unable to calculate the deviation point of the alternative with ID " +
                    "'abc#0' to hide the portion that overlaps " +
                    "with the primary route.",
                "MapboxRouteLineUtils",
            )
        }
    }

    @Test
    fun `getAlternativeRoutesDeviationOffsetsTest distance array out of bounds`() {
        val route = mockk<NavigationRoute> {
            every { id } returns "abc#0"
        }
        val alternativeRouteMetadata = mockk<AlternativeRouteMetadata> {
            every { navigationRoute } returns route
            every { forkIntersectionOfAlternativeRoute } returns mockk {
                every { geometryIndexInRoute } returns 3
            }
        }

        val result = MapboxRouteLineUtils.getAlternativeRouteDeviationOffsets(
            alternativeRouteMetadata,
            distancesProvider = {
                RouteLineGranularDistances(
                    completeDistance = 40.0,
                    routeDistances = arrayOf(
                        RouteLineDistancesIndex(
                            point = mockk(),
                            distanceRemaining = 40.0,
                        ),
                        RouteLineDistancesIndex(
                            point = mockk(),
                            distanceRemaining = 30.0,
                        ),
                        RouteLineDistancesIndex(
                            point = mockk(),
                            distanceRemaining = 20.0,
                        ),
                    ),
                    legsDistances = emptyArray(),
                    stepsDistances = emptyArray(),
                )
            },
        )

        assertEquals(0.0, result, 0.000000001)
        verify {
            logger.logW(
                "Remaining distance at index '3' requested but there are " +
                    "3 elements in the distances array - " +
                    "unable to calculate the deviation point of the alternative with ID " +
                    "'abc#0' to hide the portion that overlaps " +
                    "with the primary route.",
                "MapboxRouteLineUtils",
            )
        }
    }

    @Test
    fun updateTraveledRouteLinePushesEvent() = coroutineRule.runBlockingTest {
        val vanishingRouteLine = mockk<VanishingRouteLine>(relaxed = true) {
            every { vanishingPointState } returns VanishingPointState.ENABLED
        }
        val route = shortRoute.navigationRoute
        val sender = mockk<RouteLineHistoryRecordingApiSender>(relaxed = true)
        val point = Point.fromLngLat(3.0, 6.0)
        val options = MapboxRouteLineApiOptions.Builder().build()
        val api = MapboxRouteLineApi(
            options,
            coroutineRule.createTestScope(),
            vanishingRouteLine,
            sender,
            mockk(relaxed = true),
        )
        val routeProgress = shortRoute.mockRouteProgress(stepIndexValue = 2)

        api.updateVanishingPointState(RouteProgressState.TRACKING)
        api.setNavigationRoutes(listOf(route))
        api.updateUpcomingRoutePointIndex(routeProgress)
        api.updateTraveledRouteLine(point)

        verify { sender.sendUpdateTraveledRouteLineEvent(point) }
    }

    @Test
    fun updateTraveledRouteLineDoesNotPushEventIfSkipped() = coroutineRule.runBlockingTest {
        val vanishingRouteLine = mockk<VanishingRouteLine>(relaxed = true) {
            every { vanishingPointState } returns VanishingPointState.DISABLED
        }
        val route = shortRoute.navigationRoute
        val sender = mockk<RouteLineHistoryRecordingApiSender>(relaxed = true)
        val point = Point.fromLngLat(3.0, 6.0)
        val options = MapboxRouteLineApiOptions.Builder().build()
        val api = MapboxRouteLineApi(
            options,
            coroutineRule.createTestScope(),
            vanishingRouteLine,
            sender,
            mockk(relaxed = true),
        )
        val routeProgress = shortRoute.mockRouteProgress(stepIndexValue = 2)

        api.updateVanishingPointState(RouteProgressState.TRACKING)
        api.setNavigationRoutes(listOf(route))
        api.updateUpcomingRoutePointIndex(routeProgress)
        api.updateTraveledRouteLine(point)

        verify(exactly = 0) { sender.sendUpdateTraveledRouteLineEvent(any()) }
    }

    @Test
    fun clearRouteLineSendsEvent() = coroutineRule.runBlockingTest {
        val sender = mockk<RouteLineHistoryRecordingApiSender>(relaxed = true)

        val options = MapboxRouteLineApiOptions.Builder().build()
        val api = MapboxRouteLineApi(
            options,
            coroutineRule.createTestScope(),
            mockk(relaxed = true),
            sender,
            mockk(relaxed = true),
        )
        api.clearRouteLine()

        verify { sender.sendClearRouteLineEvent() }
        api.cancel()
    }

    @Test
    fun setVanishingOffsetSendsEvent() = coroutineRule.runBlockingTest {
        val sender = mockk<RouteLineHistoryRecordingApiSender>(relaxed = true)

        val options = MapboxRouteLineApiOptions.Builder().build()
        val api = MapboxRouteLineApi(
            options,
            coroutineRule.createTestScope(),
            mockk(relaxed = true),
            sender,
            mockk(relaxed = true),
        )
        api.setVanishingOffset(0.2)

        verify { sender.sendSetVanishingOffsetEvent(0.2) }
        api.cancel()
    }

    @Test
    fun updateWithRouteProgressSendsEvent() = coroutineRule.runBlockingTest {
        val sender = mockk<RouteLineHistoryRecordingApiSender>(relaxed = true)
        val apiOptions = MapboxRouteLineApiOptions.Builder().build()
        val api = MapboxRouteLineApi(
            apiOptions,
            coroutineRule.createTestScope(),
            null,
            sender,
            mockk(relaxed = true),
        )
        api.setNavigationRoutes(
            listOf(multilegRouteWithOverlapAndAllCongestionLevels.navigationRoute),
        )

        val routeProgress = mockRouteProgress(
            multilegRouteWithOverlapAndAllCongestionLevels.navigationRoute,
        )
        every { routeProgress.currentLegProgress!!.legIndex } returns 0
        every { routeProgress.currentRouteGeometryIndex } returns 0
        api.updateWithRouteProgress(routeProgress) {}

        verify { sender.sendUpdateWithRouteProgressEvent(routeProgress) }
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
}
