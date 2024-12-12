package com.mapbox.navigation.ui.maps.internal.route.line

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.appcompat.content.res.AppCompatResources
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.bindgen.Value
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.Style
import com.mapbox.maps.StyleObjectInfo
import com.mapbox.maps.StylePropertyValue
import com.mapbox.maps.StylePropertyValueKind
import com.mapbox.maps.extension.style.layers.Layer
import com.mapbox.maps.extension.style.layers.addPersistentLayer
import com.mapbox.maps.extension.style.layers.getLayer
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility
import com.mapbox.navigation.base.internal.route.Waypoint
import com.mapbox.navigation.base.internal.utils.WaypointFactory
import com.mapbox.navigation.base.internal.utils.internalWaypoints
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.utils.DecodeUtils.completeGeometryToPoints
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.NativeRouteParserRule
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.BOTTOM_LEVEL_ROUTE_LINE_LAYER_ID
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.CLOSURE_CONGESTION_VALUE
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.DESTINATION_MARKER_NAME
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.HEAVY_CONGESTION_VALUE
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_1_BLUR
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_1_CASING
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_1_MAIN
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_1_RESTRICTED
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_1_SOURCE_ID
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_1_TRAFFIC
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_1_TRAIL
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_1_TRAIL_CASING
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_2_BLUR
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_2_CASING
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_2_MAIN
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_2_RESTRICTED
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_2_SOURCE_ID
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_2_TRAFFIC
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_2_TRAIL
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_2_TRAIL_CASING
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_3_BLUR
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_3_CASING
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_3_MAIN
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_3_RESTRICTED
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_3_SOURCE_ID
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_3_TRAFFIC
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_3_TRAIL
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_3_TRAIL_CASING
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LOW_CONGESTION_VALUE
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.MASKING_LAYER_CASING
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.MASKING_LAYER_MAIN
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.MASKING_LAYER_RESTRICTED
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.MASKING_LAYER_TRAFFIC
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.MASKING_LAYER_TRAIL
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.MASKING_LAYER_TRAIL_CASING
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.MODERATE_CONGESTION_VALUE
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.ORIGIN_MARKER_NAME
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.RESTRICTED_CONGESTION_VALUE
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.SEVERE_CONGESTION_VALUE
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.TOP_LEVEL_ROUTE_LINE_LAYER_ID
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.UNKNOWN_CONGESTION_VALUE
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.WAYPOINT_LAYER_ID
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.WAYPOINT_SOURCE_ID
import com.mapbox.navigation.ui.maps.route.line.api.DoubleChecker
import com.mapbox.navigation.ui.maps.route.line.api.StringChecker
import com.mapbox.navigation.ui.maps.route.line.api.checkExpression
import com.mapbox.navigation.ui.maps.route.line.model.InactiveRouteColors
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineApiOptions
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineViewOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineColorResources
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineExpressionData
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineGranularDistances
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineScaleValue
import com.mapbox.navigation.ui.maps.route.line.model.SegmentColorType
import com.mapbox.navigation.ui.maps.testing.TestingUtil.loadNavigationRoute
import com.mapbox.navigation.ui.maps.util.CacheResultUtils
import com.mapbox.navigation.ui.maps.util.StyleManager
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMeasurement
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(MapboxExperimental::class)
class MapboxRouteLineUtilsTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    @get:Rule
    val routeParserRule = NativeRouteParserRule()

    private val ctx: Context = mockk()
    private val tolerance = 0.000001

    @Before
    fun setUp() {
        mockkStatic(AppCompatResources::class)
        every { AppCompatResources.getDrawable(any(), any()) } returns mockk(relaxed = true) {
            every { intrinsicWidth } returns 24
            every { intrinsicHeight } returns 24
        }
    }

    @After
    fun cleanUp() {
        unmockkStatic(AppCompatResources::class)
    }

    @Test
    fun getTrafficLineExpressionTest() {
        val dynamicOptions = MapboxRouteLineViewOptions.Builder(ctx)
            .routeLineColorResources(
                RouteLineColorResources.Builder()
                    .routeDefaultColor(-11097861)
                    .build(),
            )
            .build()
            .toData()
        val expectedExpression = "[step, [line-progress], [rgba, 0.0, 0.0, 0.0, 0.0], 0.0, " +
            "[rgba, 86.0, 168.0, 251.0, 1.0]]"

        val expressionDatas = listOf(
            RouteLineExpressionData(0.0, "", SegmentColorType.PRIMARY_DEFAULT, 0),
            RouteLineExpressionData(0.015670907645820537, "", SegmentColorType.PRIMARY_DEFAULT, 0),
            RouteLineExpressionData(0.11898525632162987, "", SegmentColorType.PRIMARY_DEFAULT, 0),
        )

        val result = MapboxRouteLineUtils.getTrafficLineExpression(
            dynamicOptions,
            0.0,
            Color.TRANSPARENT,
            SegmentColorType.PRIMARY_DEFAULT,
            expressionDatas,
            1.0,
        )

        assertEquals(expectedExpression, result.toString())
    }

    /**
     * If there a duplicate point in the route geometry (a duplicate offset), then we should pick
     * the last occurrence of a duplicate to provide the correct continuation of color changes.
     */
    @Test
    fun getTrafficLineExpressionDuplicateOffsetsRemoved() {
        val expectedExpression = "[step, [line-progress], [rgba, 0.0, 0.0, 0.0, 0.0], 0.0, " +
            "[rgba, 86.0, 168.0, 251.0, 1.0], 0.7932530928525063, " +
            "[rgba, 0.0, 255.0, 0.0, 1.0], 0.7964017663976524, " +
            "[rgba, 0.0, 0.0, 255.0, 1.0]]"
        val dynamicData = MapboxRouteLineViewOptions.Builder(ctx)
            .routeLineColorResources(
                RouteLineColorResources.Builder()
                    .routeDefaultColor(-11097861)
                    .routeLowCongestionColor(Color.RED)
                    .routeModerateCongestionColor(Color.GREEN)
                    .routeLineTraveledColor(Color.BLUE)
                    .build(),
            )
            .build()
            .toData()

        val expressionDatas = listOf(
            RouteLineExpressionData(0.7868200761181402, "", SegmentColorType.PRIMARY_DEFAULT, 0),
            RouteLineExpressionData(0.7930120224665551, "", SegmentColorType.PRIMARY_DEFAULT, 0),
            // this should not be used
            RouteLineExpressionData(
                0.7932530928525063,
                "",
                SegmentColorType.PRIMARY_LOW_CONGESTION,
                0,
            ),
            // this should be used
            RouteLineExpressionData(
                0.7932530928525063,
                "",
                SegmentColorType.PRIMARY_MODERATE_CONGESTION,
                0,
            ),
            RouteLineExpressionData(0.7964017663976524, "", SegmentColorType.TRAVELED, 0),
        )

        val result = MapboxRouteLineUtils.getTrafficLineExpression(
            dynamicData,
            0.0,
            Color.TRANSPARENT,
            SegmentColorType.PRIMARY_DEFAULT,
            expressionDatas,
            1.0,
        )

        assertEquals(expectedExpression, result.toString())
    }

    @Test
    fun getFilteredRouteLineExpressionDataDuplicateOffsetsRemoved() {
        val expressionDatas = listOf(
            RouteLineExpressionData(0.7868200761181402, "", SegmentColorType.PRIMARY_DEFAULT, 0),
            RouteLineExpressionData(0.7930120224665551, "", SegmentColorType.PRIMARY_DEFAULT, 0),
            RouteLineExpressionData(0.7932530928525063, "", SegmentColorType.PRIMARY_DEFAULT, 0),
            RouteLineExpressionData(0.7932530928525063, "", SegmentColorType.PRIMARY_DEFAULT, 0),
            RouteLineExpressionData(0.7964017663976524, "", SegmentColorType.PRIMARY_DEFAULT, 0),
        )

        val result = MapboxRouteLineUtils.getFilteredRouteLineExpressionData(
            0.0,
            expressionDatas,
            defaultObjectCreator = {
                RouteLineExpressionData(0.0, "", SegmentColorType.PRIMARY_DEFAULT, 0)
            },
        )

        assertEquals(2, expressionDatas.count { it.offset == 0.7932530928525063 })
        assertEquals(1, result.count { it.offset == 0.7932530928525063 })
    }

    @Test
    fun getRestrictedSectionExpressionData() {
        val route = loadNavigationRoute("route-with-restrictions.json")

        val result = MapboxRouteLineUtils.extractRouteRestrictionData(
            route,
            MapboxRouteLineUtils.granularDistancesProvider,
        )

        assertEquals(5, result.size)
        assertTrue(result[1].isInRestrictedSection)
        assertFalse(result[2].isInRestrictedSection)
        assertTrue(result[3].isInRestrictedSection)
        assertFalse(result[4].isInRestrictedSection)
    }

    @Test
    fun getRestrictedLineExpression() {
        val expectedExpressionContents = listOf(
            StringChecker("step"),
            StringChecker("[line-progress]"),
            StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            DoubleChecker(0.2),
            StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            DoubleChecker(0.4476941554901612),
            StringChecker("[rgba, 0.0, 255.0, 255.0, 1.0]"),
            DoubleChecker(0.4677574367125704),
            StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            DoubleChecker(0.5021643413784516),
            StringChecker("[rgba, 0.0, 255.0, 255.0, 1.0]"),
            DoubleChecker(0.5196445159361185),
            StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
        )
        val route = loadNavigationRoute("route-with-restrictions.json")
        val expData = MapboxRouteLineUtils.extractRouteRestrictionData(
            route,
            MapboxRouteLineUtils.granularDistancesProvider,
        )

        val expression = MapboxRouteLineUtils.getRestrictedLineExpression(
            0.2,
            0,
            Color.CYAN,
            Color.GRAY,
            expData,
        )

        checkExpression(expectedExpressionContents, expression)
    }

    @Test
    fun getRestrictedLineExpressionProducerRestrictionsEnabled() = runBlocking {
        val expectedExpressionContents = listOf(
            StringChecker("step"),
            StringChecker("[line-progress]"),
            StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            DoubleChecker(0.2),
            StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            DoubleChecker(0.4476941554901612),
            StringChecker("[rgba, 0.0, 255.0, 255.0, 1.0]"),
            DoubleChecker(0.4677574367125704),
            StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            DoubleChecker(0.5021643413784516),
            StringChecker("[rgba, 0.0, 255.0, 255.0, 1.0]"),
            DoubleChecker(0.5196445159361185),
            StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
        )
        val route = loadNavigationRoute("route-with-restrictions.json")
        val expData = MapboxRouteLineUtils.extractRouteRestrictionData(
            route,
            MapboxRouteLineUtils.granularDistancesProvider,
        )

        val dynamicData = MapboxRouteLineViewOptions.Builder(ctx)
            .routeLineColorResources(
                RouteLineColorResources.Builder()
                    .routeLowCongestionColor(Color.GRAY)
                    .restrictedRoadColor(Color.CYAN)
                    .build(),
            )
            .build()
            .toData()
        val expression = MapboxRouteLineUtils.getRestrictedLineExpressionProducer(
            MapboxRouteLineApiOptions.Builder()
                .calculateRestrictedRoadSections(true)
                .build(),
            expData,
            0.2,
            0,
            inactiveColorType = SegmentColorType.PRIMARY_LOW_CONGESTION,
        )(dynamicData)

        checkExpression(expectedExpressionContents, expression)
    }

    @Test
    fun getRestrictedLineExpressionProducerRestrictionsDisabled() = runBlocking {
        val expectedExpression = "[rgba, 0.0, 0.0, 0.0, 0.0]"

        val route = loadNavigationRoute("route-with-restrictions.json")
        val expData = MapboxRouteLineUtils.extractRouteRestrictionData(
            route,
            MapboxRouteLineUtils.granularDistancesProvider,
        )

        val expression = MapboxRouteLineUtils.getRestrictedLineExpressionProducer(
            MapboxRouteLineApiOptions.Builder().calculateRestrictedRoadSections(false).build(),
            expData,
            0.2,
            0,
            SegmentColorType.PRIMARY_UNKNOWN_CONGESTION,
        )(
            MapboxRouteLineViewOptions.Builder(ctx)
                .routeLineColorResources(
                    RouteLineColorResources.Builder()
                        .restrictedRoadColor(Color.CYAN)
                        .inactiveRouteLegUnknownCongestionColor(Color.CYAN)
                        .build(),
                )
                .build()
                .toData(),
        )

        assertEquals(expectedExpression, expression.toString())
    }

    @Test
    fun getRestrictedLineExpression_whenNoRestrictionsInRoute() {
        val expectedExpression = "[step, [line-progress], [rgba, 0.0, 0.0, 0.0, 0.0], 0.2, " +
            "[rgba, 0.0, 0.0, 0.0, 0.0]]"
        val route = loadNavigationRoute("short_route.json")
        val expData = MapboxRouteLineUtils.extractRouteRestrictionData(
            route,
            MapboxRouteLineUtils.granularDistancesProvider,
        )

        val expression = MapboxRouteLineUtils.getRestrictedLineExpression(
            0.2,
            0,
            -1,
            -1,
            expData,
        )

        assertEquals(expectedExpression, expression.toString())
    }

    @Test
    fun getVanishingRouteLineExpressionTest() {
        val expectedExpression = "[step, [line-progress], [rgba, 255.0, 77.0, 77.0, 1.0]" +
            ", 3.0, [rgba, 86.0, 168.0, 251.0, 1.0]]"

        val result = MapboxRouteLineUtils.getRouteLineExpression(3.0, -45747, -11097861)

        assertEquals(expectedExpression, result.toString())
    }

    @Test
    fun calculateDistance() {
        val result = MapboxRouteLineUtils.calculateDistance(
            Point.fromLngLat(-122.525212, 37.974092),
            Point.fromLngLat(-122.52509389295653, 37.974569579999944),
        )

        assertEquals(0.0000017145850113848236, result, 0.0)
    }

    @Test
    fun layersAreInitialized() {
        val style = mockk<Style> {
            every { styleSourceExists(LAYER_GROUP_1_SOURCE_ID) } returns true
            every { styleSourceExists(LAYER_GROUP_2_SOURCE_ID) } returns true
            every { styleSourceExists(LAYER_GROUP_3_SOURCE_ID) } returns true
            every { styleLayerExists(LAYER_GROUP_1_TRAIL_CASING) } returns true
            every { styleLayerExists(LAYER_GROUP_1_TRAIL) } returns true
            every { styleLayerExists(LAYER_GROUP_1_CASING) } returns true
            every { styleLayerExists(LAYER_GROUP_1_MAIN) } returns true
            every { styleLayerExists(LAYER_GROUP_1_TRAFFIC) } returns true
            every { styleLayerExists(LAYER_GROUP_1_RESTRICTED) } returns true
            every { styleLayerExists(LAYER_GROUP_2_TRAIL_CASING) } returns true
            every { styleLayerExists(LAYER_GROUP_2_TRAIL) } returns true
            every { styleLayerExists(LAYER_GROUP_2_CASING) } returns true
            every { styleLayerExists(LAYER_GROUP_2_MAIN) } returns true
            every { styleLayerExists(LAYER_GROUP_2_TRAFFIC) } returns true
            every { styleLayerExists(LAYER_GROUP_2_RESTRICTED) } returns true
            every { styleLayerExists(LAYER_GROUP_3_TRAIL_CASING) } returns true
            every { styleLayerExists(LAYER_GROUP_3_TRAIL) } returns true
            every { styleLayerExists(LAYER_GROUP_3_CASING) } returns true
            every { styleLayerExists(LAYER_GROUP_3_MAIN) } returns true
            every { styleLayerExists(LAYER_GROUP_3_TRAFFIC) } returns true
            every { styleLayerExists(LAYER_GROUP_3_RESTRICTED) } returns true
            every { styleLayerExists(TOP_LEVEL_ROUTE_LINE_LAYER_ID) } returns true
            every { styleLayerExists(BOTTOM_LEVEL_ROUTE_LINE_LAYER_ID) } returns true
            every { styleLayerExists(MASKING_LAYER_TRAIL_CASING) } returns true
            every { styleLayerExists(MASKING_LAYER_TRAIL) } returns true
            every { styleLayerExists(MASKING_LAYER_CASING) } returns true
            every { styleLayerExists(MASKING_LAYER_MAIN) } returns true
            every { styleLayerExists(MASKING_LAYER_TRAFFIC) } returns true
            every { styleLayerExists(MASKING_LAYER_RESTRICTED) } returns true
            every { styleLayerExists(LAYER_GROUP_1_BLUR) } returns true
            every { styleLayerExists(LAYER_GROUP_2_BLUR) } returns true
            every { styleLayerExists(LAYER_GROUP_3_BLUR) } returns true

            every {
                styleLayerExists(TOP_LEVEL_ROUTE_LINE_LAYER_ID)
            } returns true
        }

        val result = MapboxRouteLineUtils.layersAreInitialized(
            style,
            MapboxRouteLineViewOptions.Builder(ctx)
                .displayRestrictedRoadSections(true)
                .routeLineBlurEnabled(true)
                .build(),
        )

        assertTrue(result)
        verify { style.styleSourceExists(LAYER_GROUP_1_SOURCE_ID) }
        verify { style.styleSourceExists(LAYER_GROUP_2_SOURCE_ID) }
        verify { style.styleSourceExists(LAYER_GROUP_3_SOURCE_ID) }
        verify { style.styleLayerExists(LAYER_GROUP_1_TRAIL_CASING) }
        verify { style.styleLayerExists(LAYER_GROUP_1_TRAIL) }
        verify { style.styleLayerExists(LAYER_GROUP_1_CASING) }
        verify { style.styleLayerExists(LAYER_GROUP_1_MAIN) }
        verify { style.styleLayerExists(LAYER_GROUP_1_TRAFFIC) }
        verify { style.styleLayerExists(LAYER_GROUP_1_RESTRICTED) }
        verify { style.styleLayerExists(LAYER_GROUP_2_TRAIL_CASING) }
        verify { style.styleLayerExists(LAYER_GROUP_2_TRAIL) }
        verify { style.styleLayerExists(LAYER_GROUP_2_CASING) }
        verify { style.styleLayerExists(LAYER_GROUP_2_MAIN) }
        verify { style.styleLayerExists(LAYER_GROUP_2_TRAFFIC) }
        verify { style.styleLayerExists(LAYER_GROUP_2_RESTRICTED) }
        verify { style.styleLayerExists(LAYER_GROUP_3_TRAIL_CASING) }
        verify { style.styleLayerExists(LAYER_GROUP_3_TRAIL) }
        verify { style.styleLayerExists(LAYER_GROUP_3_CASING) }
        verify { style.styleLayerExists(LAYER_GROUP_3_MAIN) }
        verify { style.styleLayerExists(LAYER_GROUP_3_TRAFFIC) }
        verify { style.styleLayerExists(LAYER_GROUP_3_RESTRICTED) }
        verify { style.styleLayerExists(TOP_LEVEL_ROUTE_LINE_LAYER_ID) }
        verify { style.styleLayerExists(BOTTOM_LEVEL_ROUTE_LINE_LAYER_ID) }
        verify { style.styleLayerExists(MASKING_LAYER_TRAIL_CASING) }
        verify { style.styleLayerExists(MASKING_LAYER_TRAIL) }
        verify { style.styleLayerExists(MASKING_LAYER_CASING) }
        verify { style.styleLayerExists(MASKING_LAYER_MAIN) }
        verify { style.styleLayerExists(MASKING_LAYER_TRAFFIC) }
        verify { style.styleLayerExists(LAYER_GROUP_1_BLUR) }
        verify { style.styleLayerExists(LAYER_GROUP_2_BLUR) }
        verify { style.styleLayerExists(LAYER_GROUP_3_BLUR) }
    }

    @Test
    fun `layersAreInitialized without restricted roads`() {
        val options = MapboxRouteLineViewOptions.Builder(ctx)
            .displayRestrictedRoadSections(false)
            .build()
        val style = mockk<Style> {
            every { styleSourceExists(any()) } returns true
            every { styleLayerExists(any()) } returns true
            every { styleLayerExists(LAYER_GROUP_1_RESTRICTED) } returns false
            every { styleLayerExists(LAYER_GROUP_2_RESTRICTED) } returns false
            every { styleLayerExists(LAYER_GROUP_3_RESTRICTED) } returns false
        }

        val result = MapboxRouteLineUtils.layersAreInitialized(style, options)

        assertTrue(result)
        verify { style.styleSourceExists(LAYER_GROUP_1_SOURCE_ID) }
        verify { style.styleSourceExists(LAYER_GROUP_2_SOURCE_ID) }
        verify { style.styleSourceExists(LAYER_GROUP_3_SOURCE_ID) }
        verify { style.styleLayerExists(LAYER_GROUP_1_TRAIL_CASING) }
        verify { style.styleLayerExists(LAYER_GROUP_1_TRAIL) }
        verify { style.styleLayerExists(LAYER_GROUP_1_CASING) }
        verify { style.styleLayerExists(LAYER_GROUP_1_MAIN) }
        verify { style.styleLayerExists(LAYER_GROUP_1_TRAFFIC) }
        verify { style.styleLayerExists(LAYER_GROUP_2_TRAIL_CASING) }
        verify { style.styleLayerExists(LAYER_GROUP_2_TRAIL) }
        verify { style.styleLayerExists(LAYER_GROUP_2_CASING) }
        verify { style.styleLayerExists(LAYER_GROUP_2_MAIN) }
        verify { style.styleLayerExists(LAYER_GROUP_2_TRAFFIC) }
        verify { style.styleLayerExists(LAYER_GROUP_3_TRAIL_CASING) }
        verify { style.styleLayerExists(LAYER_GROUP_3_TRAIL) }
        verify { style.styleLayerExists(LAYER_GROUP_3_CASING) }
        verify { style.styleLayerExists(LAYER_GROUP_3_MAIN) }
        verify { style.styleLayerExists(LAYER_GROUP_3_TRAFFIC) }
        verify { style.styleLayerExists(MASKING_LAYER_TRAIL_CASING) }
        verify { style.styleLayerExists(MASKING_LAYER_TRAIL) }
        verify { style.styleLayerExists(MASKING_LAYER_CASING) }
        verify { style.styleLayerExists(MASKING_LAYER_MAIN) }
        verify { style.styleLayerExists(MASKING_LAYER_TRAFFIC) }
        verify(exactly = 0) {
            style.styleLayerExists(LAYER_GROUP_1_RESTRICTED)
        }
        verify(exactly = 0) {
            style.styleLayerExists(LAYER_GROUP_2_RESTRICTED)
        }
        verify(exactly = 0) {
            style.styleLayerExists(LAYER_GROUP_3_RESTRICTED)
        }
    }

    @Test
    fun initializeLayers_whenLayersAreInitialized_whenSourcesAreCompatible() {
        mockkStatic(Style::addPersistentLayer)
        val options = MapboxRouteLineViewOptions.Builder(ctx).build()
        val expectedLineSourceProperties = hashMapOf(
            "type" to Value("geojson"),
            "sharedCache" to Value(options.shareLineGeometrySources),
            "maxzoom" to Value(16),
            "lineMetrics" to Value(true),
            "tolerance" to Value(options.tolerance),
        )
        val expectedWaypointSourceProperties = hashMapOf(
            "type" to Value("geojson"),
            "sharedCache" to Value(false),
            "maxzoom" to Value(16),
            "lineMetrics" to Value(false),
            "tolerance" to Value(options.tolerance),
        )
        val style = mockk<Style> {
            every { styleSourceExists(any()) } returns true
            every { styleLayerExists(any()) } returns true
            every { hasStyleImage(any()) } returns true
            mockSourceProperties(LAYER_GROUP_1_SOURCE_ID, expectedLineSourceProperties)
            mockSourceProperties(LAYER_GROUP_2_SOURCE_ID, expectedLineSourceProperties)
            mockSourceProperties(LAYER_GROUP_3_SOURCE_ID, expectedLineSourceProperties)
            mockSourceProperties(WAYPOINT_SOURCE_ID, expectedWaypointSourceProperties)
            every { removeStyleSource(any()) } returns ExpectedFactory.createNone()
            every { addStyleSource(any(), any()) } returns ExpectedFactory.createNone()
            every { styleSlots } returns listOf()
        }

        MapboxRouteLineUtils.initializeLayers(style, options)

        verify(exactly = 0) { style.addPersistentLayer(any(), any()) }
        verify(exactly = 0) { style.addStyleSource(any(), any()) }
        verify(exactly = 0) { style.addImage(any(), any<Bitmap>()) }

        unmockkStatic(Style::addPersistentLayer)
    }

    @Test
    fun initializeLayers_whenLayersAreInitialized_whenLineSourcesAreIncompatible() {
        mockkStatic(Style::addPersistentLayer) {
            val options = MapboxRouteLineViewOptions.Builder(ctx).build()
            val waypointSourceProperties = hashMapOf(
                "type" to Value("geojson"),
                "sharedCache" to Value(false),
                "maxzoom" to Value(16),
                "lineMetrics" to Value(false),
                "tolerance" to Value(options.tolerance),
                "data" to Value("{}"),
            )
            val expectedLineSourceProperties = hashMapOf(
                "type" to Value("geojson"),
                "sharedCache" to Value(options.shareLineGeometrySources),
                "maxzoom" to Value(16),
                "lineMetrics" to Value(true),
                "tolerance" to Value(options.tolerance),
                "data" to Value("{}"),
            )
            listOf(
                hashMapOf(
                    "type" to Value("geojson"),
                    "sharedCache" to Value(!options.shareLineGeometrySources),
                    "maxzoom" to Value(16),
                    "lineMetrics" to Value(true),
                    "tolerance" to Value(options.tolerance),
                    "data" to Value("{}"),
                ),
                hashMapOf(
                    "type" to Value("geojson"),
                    "sharedCache" to Value(options.shareLineGeometrySources),
                    "maxzoom" to Value(17),
                    "lineMetrics" to Value(true),
                    "tolerance" to Value(options.tolerance),
                    "data" to Value("{}"),
                ),
                hashMapOf(
                    "type" to Value("geojson"),
                    "sharedCache" to Value(options.shareLineGeometrySources),
                    "maxzoom" to Value(16),
                    "lineMetrics" to Value(false),
                    "tolerance" to Value(options.tolerance),
                    "data" to Value("{}"),
                ),
                hashMapOf(
                    "type" to Value("geojson"),
                    "sharedCache" to Value(options.shareLineGeometrySources),
                    "maxzoom" to Value(16),
                    "lineMetrics" to Value(true),
                    "tolerance" to Value(options.tolerance + 0.5),
                    "data" to Value("{}"),
                ),
            ).forEach { existingLineSourceProperties ->
                val style = mockk<Style> {
                    every { styleSourceExists(any()) } returns true
                    every { styleLayerExists(any()) } returns true
                    every { hasStyleImage(any()) } returns true
                    mockSourceProperties(LAYER_GROUP_1_SOURCE_ID, existingLineSourceProperties)
                    mockSourceProperties(LAYER_GROUP_2_SOURCE_ID, existingLineSourceProperties)
                    mockSourceProperties(LAYER_GROUP_3_SOURCE_ID, existingLineSourceProperties)
                    mockSourceProperties(WAYPOINT_SOURCE_ID, waypointSourceProperties)
                    every { removeStyleSource(any()) } returns ExpectedFactory.createNone()
                    every { addStyleSource(any(), any()) } returns ExpectedFactory.createNone()
                    every { styleSlots } returns listOf()
                }

                MapboxRouteLineUtils.initializeLayers(style, options)

                verify(exactly = 0) { style.addPersistentLayer(any(), any()) }
                verify(exactly = 1) {
                    style.addStyleSource(
                        LAYER_GROUP_1_SOURCE_ID,
                        Value(expectedLineSourceProperties),
                    )
                }
                verify(exactly = 1) {
                    style.addStyleSource(
                        LAYER_GROUP_2_SOURCE_ID,
                        Value(expectedLineSourceProperties),
                    )
                }
                verify(exactly = 1) {
                    style.addStyleSource(
                        LAYER_GROUP_3_SOURCE_ID,
                        Value(expectedLineSourceProperties),
                    )
                }
                verify(exactly = 0) { style.addImage(any(), any<Bitmap>()) }
            }
        }
    }

    @Test
    fun initializeLayers_whenLayersAreInitialized_whenWaypointSourcesAreIncompatible() {
        mockkStatic(Style::addPersistentLayer) {
            val options = MapboxRouteLineViewOptions.Builder(ctx).build()
            val lineSourceProperties = hashMapOf(
                "type" to Value("geojson"),
                "sharedCache" to Value(options.shareLineGeometrySources),
                "maxzoom" to Value(16),
                "lineMetrics" to Value(true),
                "tolerance" to Value(options.tolerance),
                "data" to Value("{}"),
            )
            val expectedWaypointSourceProperties = hashMapOf(
                "type" to Value("geojson"),
                "sharedCache" to Value(false),
                "maxzoom" to Value(16),
                "lineMetrics" to Value(false),
                "tolerance" to Value(options.tolerance),
                "data" to Value("{}"),
            )
            listOf(
                hashMapOf(
                    "type" to Value("geojson"),
                    "sharedCache" to Value(false),
                    "maxzoom" to Value(16),
                    "lineMetrics" to Value(false),
                    "tolerance" to Value(options.tolerance + 0.5),
                    "data" to Value("{}"),
                ),
            ).forEach { existingWaypointSourceProperties ->
                val style = mockk<Style> {
                    every { styleSourceExists(any()) } returns true
                    every { styleLayerExists(any()) } returns true
                    every { hasStyleImage(any()) } returns true
                    mockSourceProperties(LAYER_GROUP_1_SOURCE_ID, lineSourceProperties)
                    mockSourceProperties(LAYER_GROUP_2_SOURCE_ID, lineSourceProperties)
                    mockSourceProperties(LAYER_GROUP_3_SOURCE_ID, lineSourceProperties)
                    mockSourceProperties(WAYPOINT_SOURCE_ID, existingWaypointSourceProperties)
                    every { removeStyleSource(any()) } returns ExpectedFactory.createNone()
                    every { addStyleSource(any(), any()) } returns ExpectedFactory.createNone()
                    every { styleSlots } returns listOf()
                }

                MapboxRouteLineUtils.initializeLayers(style, options)

                verify(exactly = 0) { style.addPersistentLayer(any(), any()) }
                verify(exactly = 1) {
                    style.addStyleSource(
                        WAYPOINT_SOURCE_ID,
                        Value(expectedWaypointSourceProperties),
                    )
                }
                verify(exactly = 0) { style.addImage(any(), any<Bitmap>()) }
            }
        }
    }

    private fun Style.mockSourceProperties(sourceId: String, properties: Map<String, Value>) {
        properties.forEach {
            every { getStyleSourceProperty(sourceId, it.key) } returns StylePropertyValue(
                it.value,
                StylePropertyValueKind.CONSTANT,
            )
        }
    }

    @Test
    fun getBelowLayerIdToUse() {
        val style = mockk<Style> {
            every { styleLayerExists("foobar") } returns true
        }

        val result = MapboxRouteLineUtils.getBelowLayerIdToUse("foobar", style)

        assertEquals("foobar", result)
    }

    @Test
    fun getBelowLayerIdToUse_whenLayerIdNotFoundReturnsNull() {
        val style = mockk<Style> {
            every { styleLayerExists("foobar") } returns false
        }

        val result = MapboxRouteLineUtils.getBelowLayerIdToUse("foobar", style)

        assertNull(result)
    }

    @Test
    fun getBelowLayerIdToUse_whenLayerIdNotSpecified() {
        val style = mockk<Style>()

        val result = MapboxRouteLineUtils.getBelowLayerIdToUse(null, style)

        assertNull(result)
    }

    @Test
    fun buildScalingExpression() {
        val expectedExpression = "[interpolate, [exponential, 1.5], [zoom], 4.0, [*, 3.0, 1.0]," +
            " 10.0, [*, 4.0, 1.0], 13.0, [*, 6.0, 1.0], 16.0, [*, 10.0, 1.0], 19.0, " +
            "[*, 14.0, 1.0], 22.0, [*, 18.0, 1.0]]"
        val values = listOf(
            RouteLineScaleValue(4f, 3f, 1f),
            RouteLineScaleValue(10f, 4f, 1f),
            RouteLineScaleValue(13f, 6f, 1f),
            RouteLineScaleValue(16f, 10f, 1f),
            RouteLineScaleValue(19f, 14f, 1f),
            RouteLineScaleValue(22f, 18f, 1f),
        )

        val result = MapboxRouteLineUtils.buildScalingExpression(values)

        assertEquals(expectedExpression, result.toString())
    }

    @Test
    fun getRouteLineTrafficExpressionDataWhenUniqueStreetClassDataExists() {
        val route = loadNavigationRoute("route-unique-road-classes.json")
        val roadClasses = route.directionsRoute.legs()?.asSequence()
            ?.mapNotNull { it.steps() }
            ?.flatten()
            ?.mapNotNull { it.intersections() }
            ?.flatten()
            ?.filter {
                it.geometryIndex() != null && it.mapboxStreetsV8()?.roadClass() != null
            }
            ?.map { it.mapboxStreetsV8()!!.roadClass() }
            ?.toList()

        val result = MapboxRouteLineUtils.extractRouteData(
            route,
            MapboxRouteLineUtils.getRouteLegTrafficCongestionProvider,
        )

        assertEquals(route.directionsRoute.completeGeometryToPoints().size, result.size)
        assertEquals(0.016357747710023185, result[1].offset, 0.001)
        assertEquals(result[0].roadClass, roadClasses!!.first())
        assertEquals(0.02608868855363078, result[2].offset, 0.001)
        assertEquals(0.9889249500426456, result[result.lastIndex - 1].offset, 0.001)
        assertEquals(LOW_CONGESTION_VALUE, result[result.lastIndex - 1].trafficCongestionIdentifier)
        assertEquals("service", result[result.lastIndex - 1].roadClass)
        assertEquals(1.0, result.last().offset, 0.0)
    }

    @Test
    fun getRouteLineTrafficExpressionWithRoadClassesDuplicatesRemoved() {
        val route = loadNavigationRoute("route-with-road-classes.txt")

        val result = MapboxRouteLineUtils.extractRouteDataWithTrafficAndRoadClassDeDuped(
            route,
            MapboxRouteLineUtils.getRouteLegTrafficCongestionProvider,
        )

        assertEquals(11, result.size)
        assertEquals(1.0, result.last().offset, 0.0)
        assertEquals(LOW_CONGESTION_VALUE, result[9].trafficCongestionIdentifier)
        assertEquals("service", result[9].roadClass)
    }

    @Test
    fun getRouteLineTrafficExpressionDataWithSomeRoadClassesDuplicatesRemoved() {
        val route =
            loadNavigationRoute("motorway-route-with-road-classes-mixed.json")

        val result = MapboxRouteLineUtils.extractRouteDataWithTrafficAndRoadClassDeDuped(
            route,
            MapboxRouteLineUtils.getRouteLegTrafficCongestionProvider,
        )

        assertEquals(6, result.size)
        assertEquals(0.0, result[0].offset, 0.0)
        assertEquals("unknown", result[0].trafficCongestionIdentifier)
        assertEquals("motorway", result[0].roadClass)
        assertEquals(0.00236427570458575, result[1].offset, tolerance)
        assertEquals("severe", result[1].trafficCongestionIdentifier)
        assertEquals("motorway", result[1].roadClass)
        assertEquals(0.01736459676474489, result[2].offset, tolerance)
        assertEquals("unknown", result[2].trafficCongestionIdentifier)
        assertEquals("motorway", result[2].roadClass)
        assertEquals(0.02522030104681694, result[3].offset, tolerance)
        assertEquals("severe", result[3].trafficCongestionIdentifier)
        assertEquals("motorway", result[3].roadClass)
        assertEquals(0.0629029108487491, result[4].offset, tolerance)
        assertEquals("unknown", result[4].trafficCongestionIdentifier)
        assertEquals("motorway", result[4].roadClass)
    }

    @Test
    fun getRouteLineExpressionDataWithStreetClassOverrideWhenHasStreetClassesOnMotorway() {
        val route = loadNavigationRoute("motorway-route-with-road-classes.json")
        val trafficExpressionData =
            MapboxRouteLineUtils.extractRouteDataWithTrafficAndRoadClassDeDuped(
                route,
                MapboxRouteLineUtils.getRouteLegTrafficCongestionProvider,
            )

        val result = MapboxRouteLineUtils.getRouteLineExpressionDataWithStreetClassOverride(
            trafficExpressionData,
            true,
            listOf("motorway"),
        )

        assertTrue(
            result.dropLast(1).all {
                it.segmentColorType == SegmentColorType.PRIMARY_LOW_CONGESTION
            },
        )
        assertEquals(SegmentColorType.PRIMARY_UNKNOWN_CONGESTION, result.last().segmentColorType)
        assertEquals(1.0, result.last().offset, 0.0)
        assertEquals(2, result.size)
    }

    @Test
    fun getRouteLineExpressionDataWithSomeRoadClassesDuplicatesRemoved() {
        val route = loadNavigationRoute("motorway-route-with-road-classes-mixed.json")
        val trafficExpressionData =
            MapboxRouteLineUtils.extractRouteDataWithTrafficAndRoadClassDeDuped(
                route,
                MapboxRouteLineUtils.getRouteLegTrafficCongestionProvider,
            )

        val result = MapboxRouteLineUtils.getRouteLineExpressionDataWithStreetClassOverride(
            trafficExpressionData,
            true,
            listOf("motorway"),
        )

        assertEquals(6, result.size)
        assertEquals(0.0, result[0].offset, 0.0)
        assertEquals(SegmentColorType.PRIMARY_LOW_CONGESTION, result[0].segmentColorType)
        assertEquals(0.00236427570458575, result[1].offset, tolerance)
        assertEquals(SegmentColorType.PRIMARY_SEVERE_CONGESTION, result[1].segmentColorType)
        assertEquals(0.01736459676474489, result[2].offset, tolerance)
        assertEquals(SegmentColorType.PRIMARY_LOW_CONGESTION, result[2].segmentColorType)
        assertEquals(0.02522030104681694, result[3].offset, tolerance)
        assertEquals(SegmentColorType.PRIMARY_SEVERE_CONGESTION, result[3].segmentColorType)
        assertEquals(0.0629029108487491, result[4].offset, tolerance)
        assertEquals(SegmentColorType.PRIMARY_LOW_CONGESTION, result[4].segmentColorType)
        assertEquals(1.0, result[5].offset, tolerance)
        assertEquals(SegmentColorType.PRIMARY_UNKNOWN_CONGESTION, result[5].segmentColorType)
    }

    @Test
    fun getRouteLineExpressionDataWithStreetClassOverrideWhenHasStreetClassesAndClosures() {
        val route = loadNavigationRoute("route-with-closure.json")
        val trafficExpressionData =
            MapboxRouteLineUtils.extractRouteDataWithTrafficAndRoadClassDeDuped(
                route,
                MapboxRouteLineUtils.getRouteLegTrafficCongestionProvider,
            )

        val result = MapboxRouteLineUtils.getRouteLineExpressionDataWithStreetClassOverride(
            trafficExpressionData,
            true,
            listOf("tertiary"),
        )

        assertEquals(0.0, result[0].offset, 0.0)
        assertEquals(SegmentColorType.PRIMARY_LOW_CONGESTION, result[0].segmentColorType)
        assertEquals(0.5463467450710624, result[1].offset, 0.0)
        assertEquals(SegmentColorType.PRIMARY_CLOSURE, result[1].segmentColorType)
        assertEquals(0.8686140993140916, result[2].offset, 0.0)
        assertEquals(SegmentColorType.PRIMARY_SEVERE_CONGESTION, result[2].segmentColorType)
    }

    @Test
    fun getRouteLineTrafficExpressionDataWithOutStreetClassesDuplicatesRemoved() {
        val route = loadNavigationRoute("route-with-traffic-no-street-classes.txt")

        val result = MapboxRouteLineUtils.extractRouteDataWithTrafficAndRoadClassDeDuped(
            route,
            MapboxRouteLineUtils.getRouteLegTrafficCongestionProvider,
        )

        assertEquals(6, result.size)
        assertEquals(1.0, result.last().offset, 0.0)
        assertEquals(0.8911757464617965, result[result.lastIndex - 1].offset, 0.001)
        assertEquals(LOW_CONGESTION_VALUE, result[result.lastIndex - 1].trafficCongestionIdentifier)
        assertNull(result[result.lastIndex - 1].roadClass)
    }

    @Test
    fun getRouteLineTrafficExpressionDataWithStreetClassesDuplicatesRemoved() {
        val route = loadNavigationRoute("route-with-road-classes.txt")
        val trafficExpressionData =
            MapboxRouteLineUtils.extractRouteDataWithTrafficAndRoadClassDeDuped(
                route,
                MapboxRouteLineUtils.getRouteLegTrafficCongestionProvider,
            )

        assertEquals("service", trafficExpressionData[0].roadClass)
        assertEquals("street", trafficExpressionData[1].roadClass)
        assertEquals(
            UNKNOWN_CONGESTION_VALUE,
            trafficExpressionData[0].trafficCongestionIdentifier,
        )
        assertEquals(
            UNKNOWN_CONGESTION_VALUE,
            trafficExpressionData[1].trafficCongestionIdentifier,
        )

        val result = MapboxRouteLineUtils.getRouteLineExpressionDataWithStreetClassOverride(
            trafficExpressionData,
            true,
            listOf("street"),
        )

        assertEquals(SegmentColorType.PRIMARY_UNKNOWN_CONGESTION, result[0].segmentColorType)
        assertEquals(8, result.size)
        assertEquals(0.016357747710023185, result[1].offset, 0.001)
        assertEquals(SegmentColorType.PRIMARY_LOW_CONGESTION, result[1].segmentColorType)
        assertEquals(1.0, result.last().offset, 0.0)
    }

    @Test
    fun getRouteLineExpressionDataWithStreetClassOverrideWhenDoesNotHaveStreetClasses() {
        val route = loadNavigationRoute("route-with-traffic-no-street-classes.txt")
        val trafficExpressionData =
            MapboxRouteLineUtils.extractRouteDataWithTrafficAndRoadClassDeDuped(
                route,
                MapboxRouteLineUtils.getRouteLegTrafficCongestionProvider,
            )

        val result = MapboxRouteLineUtils.getRouteLineExpressionDataWithStreetClassOverride(
            trafficExpressionData,
            true,
            listOf(),
        )

        assertEquals(6, result.size)
        assertEquals(0.2347789510574998, result[1].offset, 0.001)
        assertEquals(SegmentColorType.PRIMARY_LOW_CONGESTION, result[1].segmentColorType)
        assertEquals(1.0, result.last().offset, 0.0)
    }

    @Test
    fun getTrafficExpressionWithStreetClassOverrideOnMotorwayWhenChangeOutsideOfIntersections() {
        val colorResources = RouteLineColorResources.Builder()
            .routeUnknownCongestionColor(-9)
            .routeLowCongestionColor(-1)
            .routeCasingColor(33)
            .routeDefaultColor(33)
            .routeHeavyCongestionColor(33)
            .routeLineTraveledCasingColor(33)
            .routeLineTraveledColor(33)
            .routeModerateCongestionColor(33)
            .routeSevereCongestionColor(-2)
            .build()

        val route = loadNavigationRoute(
            "motorway-route-with-road-classes-unknown-not-on-intersection.json",
        )

        val trafficExpressionData =
            MapboxRouteLineUtils.extractRouteDataWithTrafficAndRoadClassDeDuped(
                route,
                MapboxRouteLineUtils.getRouteLegTrafficCongestionProvider,
            )

        val result = MapboxRouteLineUtils.getRouteLineExpressionDataWithStreetClassOverride(
            trafficExpressionData,
            true,
            listOf("motorway"),
        )

        assertEquals(SegmentColorType.PRIMARY_SEVERE_CONGESTION, result[0].segmentColorType)
        assertNotEquals(SegmentColorType.PRIMARY_UNKNOWN_CONGESTION, result[1].segmentColorType)
        assertEquals(SegmentColorType.PRIMARY_LOW_CONGESTION, result[1].segmentColorType)
        assertEquals(SegmentColorType.PRIMARY_SEVERE_CONGESTION, result[2].segmentColorType)
    }

    @Test
    fun getRouteLineTrafficExpressionDataMissingRoadClass() {
        val route = loadNavigationRoute(
            "route-with-missing-road-classes.json",
        )

        val result = MapboxRouteLineUtils.extractRouteDataWithTrafficAndRoadClassDeDuped(
            route,
            MapboxRouteLineUtils.getRouteLegTrafficCongestionProvider,
        )

        assertEquals(8, result.size)
        assertEquals(0.0, result[0].offset, 0.0)
        assertEquals("severe", result[0].trafficCongestionIdentifier)
        assertEquals("motorway", result[0].roadClass)
        assertEquals(0.00236427570458575, result[1].offset, tolerance)
        assertEquals("unknown", result[1].trafficCongestionIdentifier)
        assertEquals("motorway", result[1].roadClass)
        assertEquals(0.01736459676474489, result[2].offset, tolerance)
        assertEquals("severe", result[2].trafficCongestionIdentifier)
        assertEquals("motorway", result[2].roadClass)
        assertEquals(0.1714843563045706, result[3].offset, tolerance)
        assertEquals("severe", result[3].trafficCongestionIdentifier)
        assertEquals("intersection_without_class_fallback", result[3].roadClass)
        assertEquals(0.19250306326360345, result[4].offset, tolerance)
        assertEquals("severe", result[4].trafficCongestionIdentifier)
        assertEquals("motorway", result[4].roadClass)
        assertEquals(0.34420704878106756, result[5].offset, tolerance)
        assertEquals("severe", result[5].trafficCongestionIdentifier)
        assertEquals("intersection_without_class_fallback", result[5].roadClass)
        assertEquals(0.7375706868313839, result[6].offset, tolerance)
        assertEquals("severe", result[6].trafficCongestionIdentifier)
        assertEquals("motorway", result[6].roadClass)
        assertEquals(1.0, result.last().offset, 0.0)
    }

    @Test
    fun getRouteLineTrafficExpressionDataWithClosures() {
        val route = loadNavigationRoute("route-with-closure.json")

        val trafficExpressionData =
            MapboxRouteLineUtils.extractRouteDataWithTrafficAndRoadClassDeDuped(
                route,
                MapboxRouteLineUtils.getRouteLegTrafficCongestionProvider,
            )

        assertEquals(0.0, trafficExpressionData[0].offset, 0.0)
        assertEquals("low", trafficExpressionData[0].trafficCongestionIdentifier)
        assertEquals(0.5463467450710624, trafficExpressionData[1].offset, 0.0)
        assertEquals("closed", trafficExpressionData[1].trafficCongestionIdentifier)
        assertEquals(0.8686140993140916, trafficExpressionData[2].offset, 0.0)
        assertEquals("severe", trafficExpressionData[2].trafficCongestionIdentifier)
    }

    @Test
    fun getRouteLineTrafficExpressionDataWithRestrictedSections() {
        val route = loadNavigationRoute("route-with-restrictions.json")

        val trafficExpressionData = MapboxRouteLineUtils.extractRouteRestrictionData(
            route,
            MapboxRouteLineUtils.granularDistancesProvider,
        )

        assertEquals(0.0, trafficExpressionData[0].offset, 0.0)
        assertFalse(trafficExpressionData[0].isInRestrictedSection)
        assertEquals(0, trafficExpressionData[0].legIndex)

        assertEquals(0, trafficExpressionData[1].legIndex)
        assertTrue(trafficExpressionData[1].isInRestrictedSection)
        assertEquals(0.4476941554901612, trafficExpressionData[1].offset, tolerance)

        assertEquals(0, trafficExpressionData[2].legIndex)
        assertFalse(trafficExpressionData[2].isInRestrictedSection)
        assertEquals(0.4677574367125704, trafficExpressionData[2].offset, tolerance)

        assertEquals(0, trafficExpressionData[3].legIndex)
        assertTrue(trafficExpressionData[3].isInRestrictedSection)
        assertEquals(0.5021643413784516, trafficExpressionData[3].offset, tolerance)

        assertEquals(0, trafficExpressionData[4].legIndex)
        assertFalse(trafficExpressionData[4].isInRestrictedSection)
        assertEquals(0.5196445159361185, trafficExpressionData[4].offset, tolerance)

        assertEquals(5, trafficExpressionData.size)
    }

    @Test
    fun getRouteLineTrafficExpressionData_whenFirstDistanceInSecondLegIsZero() {
        val route = loadNavigationRoute("multileg-route-two-legs.json")

        val result = MapboxRouteLineUtils.extractRouteDataWithTrafficAndRoadClassDeDuped(
            route,
            MapboxRouteLineUtils.getRouteLegTrafficCongestionProvider,
        )

        assertEquals(22, result.size)
        assertFalse(result[7].isLegOrigin)
        assertEquals(0.4897719974699625, result[7].offset, tolerance)
        assertTrue(result[8].isLegOrigin)
        assertEquals(0.4897719974699625, result[8].offset, tolerance)
        assertFalse(result[9].isLegOrigin)
    }

    @Test
    fun calculateRouteLineSegmentsMultilegRoute() {
        val route = loadNavigationRoute("multileg_route.json")

        val result = MapboxRouteLineUtils.calculateRouteLineSegments(
            route,
            listOf(),
            true,
            MapboxRouteLineApiOptions.Builder().build(),
        )

        assertEquals(22, result.size)
        assertEquals(0.039796278954241426, result[1].offset, tolerance)
        assertEquals(0.9898280890800685, result.last().offset, tolerance)
    }

    @Test
    fun calculateRouteLineSegmentsMultilegRouteFirstDistanceValueAboveMinimumOffset() {
        val route = loadNavigationRoute("multileg_route.json")

        val result = MapboxRouteLineUtils.calculateRouteLineSegments(
            route,
            listOf(),
            true,
            MapboxRouteLineApiOptions.Builder().build(),
        )

        assertTrue(result[1].offset > .001f)
    }

    @Test
    fun calculateRouteLineSegments_whenNoTrafficExpressionData() {
        val route = loadNavigationRoute("short_route_no_congestion.json")

        val result = MapboxRouteLineUtils.calculateRouteLineSegments(
            route,
            listOf(),
            true,
            MapboxRouteLineApiOptions.Builder().build(),
        )

        assertEquals(1, result.size)
        assertEquals(0.0, result[0].offset, 0.0)
        assertEquals(SegmentColorType.PRIMARY_UNKNOWN_CONGESTION, result[0].segmentColorType)
    }

    @Test
    fun getRouteColorForCongestionPrimaryRouteCongestionLow() {
        val result = MapboxRouteLineUtils.getRouteColorTypeForCongestion(
            LOW_CONGESTION_VALUE,
            true,
        )

        assertEquals(SegmentColorType.PRIMARY_LOW_CONGESTION, result)
    }

    @Test
    fun getRouteColorForCongestionPrimaryRouteCongestionModerate() {
        val result = MapboxRouteLineUtils.getRouteColorTypeForCongestion(
            MODERATE_CONGESTION_VALUE,
            true,
        )

        assertEquals(SegmentColorType.PRIMARY_MODERATE_CONGESTION, result)
    }

    @Test
    fun getRouteColorForCongestionPrimaryRouteCongestionHeavy() {
        val result =
            MapboxRouteLineUtils.getRouteColorTypeForCongestion(
                HEAVY_CONGESTION_VALUE,
                true,
            )

        assertEquals(SegmentColorType.PRIMARY_HEAVY_CONGESTION, result)
    }

    @Test
    fun getRouteColorForCongestionPrimaryRouteCongestionSevere() {
        val result =
            MapboxRouteLineUtils.getRouteColorTypeForCongestion(
                SEVERE_CONGESTION_VALUE,
                true,
            )

        assertEquals(SegmentColorType.PRIMARY_SEVERE_CONGESTION, result)
    }

    @Test
    fun getRouteColorForCongestionPrimaryRouteCongestionUnknown() {
        val result =
            MapboxRouteLineUtils.getRouteColorTypeForCongestion(
                UNKNOWN_CONGESTION_VALUE,
                true,
            )

        assertEquals(SegmentColorType.PRIMARY_UNKNOWN_CONGESTION, result)
    }

    @Test
    fun getRouteColorForCongestionPrimaryRouteCongestionDefault() {
        val result = MapboxRouteLineUtils.getRouteColorTypeForCongestion(
            "foobar",
            true,
        )

        assertEquals(SegmentColorType.PRIMARY_DEFAULT, result)
    }

    @Test
    fun getRouteColorForCongestionPrimaryRouteCongestionClosure() {
        val result = MapboxRouteLineUtils.getRouteColorTypeForCongestion(
            CLOSURE_CONGESTION_VALUE,
            true,
        )

        assertEquals(SegmentColorType.PRIMARY_CLOSURE, result)
    }

    @Test
    fun getRouteColorForCongestionPrimaryRouteCongestionRestricted() {
        val result = MapboxRouteLineUtils.getRouteColorTypeForCongestion(
            RESTRICTED_CONGESTION_VALUE,
            true,
        )

        assertEquals(SegmentColorType.PRIMARY_RESTRICTED, result)
    }

    @Test
    fun getRouteColorForCongestionNonPrimaryRouteCongestionLow() {
        val result =
            MapboxRouteLineUtils.getRouteColorTypeForCongestion(
                LOW_CONGESTION_VALUE,
                false,
            )

        assertEquals(SegmentColorType.ALTERNATIVE_LOW_CONGESTION, result)
    }

    @Test
    fun getRouteColorForCongestionNonPrimaryRouteCongestionModerate() {
        val result =
            MapboxRouteLineUtils.getRouteColorTypeForCongestion(
                MODERATE_CONGESTION_VALUE,
                false,
            )

        assertEquals(SegmentColorType.ALTERNATIVE_MODERATE_CONGESTION, result)
    }

    @Test
    fun getRouteColorForCongestionNonPrimaryRouteCongestionHeavy() {
        val result =
            MapboxRouteLineUtils.getRouteColorTypeForCongestion(
                HEAVY_CONGESTION_VALUE,
                false,
            )

        assertEquals(SegmentColorType.ALTERNATIVE_HEAVY_CONGESTION, result)
    }

    @Test
    fun getRouteColorForCongestionNonPrimaryRouteCongestionSevere() {
        val result =
            MapboxRouteLineUtils.getRouteColorTypeForCongestion(
                SEVERE_CONGESTION_VALUE,
                false,
            )

        assertEquals(SegmentColorType.ALTERNATIVE_SEVERE_CONGESTION, result)
    }

    @Test
    fun getRouteColorForCongestionNonPrimaryRouteCongestionUnknown() {
        val result =
            MapboxRouteLineUtils.getRouteColorTypeForCongestion(
                UNKNOWN_CONGESTION_VALUE,
                false,
            )

        assertEquals(SegmentColorType.ALTERNATIVE_UNKNOWN_CONGESTION, result)
    }

    @Test
    fun getRouteColorForCongestionNonPrimaryRouteCongestionDefault() {
        val result = MapboxRouteLineUtils.getRouteColorTypeForCongestion(
            "foobar",
            false,
        )

        assertEquals(SegmentColorType.ALTERNATIVE_DEFAULT, result)
    }

    @Test
    fun getRouteColorForCongestionNonPrimaryRouteCongestionRestricted() {
        val result = MapboxRouteLineUtils.getRouteColorTypeForCongestion(
            RESTRICTED_CONGESTION_VALUE,
            false,
        )

        assertEquals(SegmentColorType.ALTERNATIVE_RESTRICTED, result)
    }

    @Test
    fun getRouteColorForCongestionNonPrimaryRouteCongestionClosure() {
        val result = MapboxRouteLineUtils.getRouteColorTypeForCongestion(
            CLOSURE_CONGESTION_VALUE,
            false,
        )

        assertEquals(SegmentColorType.ALTERNATIVE_CLOSURE, result)
    }

    @Test
    fun buildWayPointFeatureCollection() {
        val route = mockk<NavigationRoute>()
        mockkStatic(route::internalWaypoints) {
            every { route.internalWaypoints() } returns listOf(
                WaypointFactory.provideWaypoint(
                    name = "w1",
                    location = Point.fromLngLat(-77.157347, 38.783004),
                    type = Waypoint.REGULAR,
                    target = null,
                    metadata = emptyMap(),
                ),
                WaypointFactory.provideWaypoint(
                    name = "w2",
                    location = Point.fromLngLat(-77.167276, 38.775717),
                    type = Waypoint.REGULAR,
                    target = null,
                    metadata = emptyMap(),
                ),
                WaypointFactory.provideWaypoint(
                    name = "w3",
                    location = Point.fromLngLat(-77.153468, 38.77091),
                    type = Waypoint.REGULAR,
                    target = null,
                    metadata = emptyMap(),
                ),
            )
            val result = MapboxRouteLineUtils.buildWayPointFeatureCollection(route)

            assertEquals(3, result.features()!!.size)
            assertEquals(
                Point.fromLngLat(-77.157347, 38.783004),
                result.features()!![0].geometry() as Point,
            )
            assertEquals(
                Point.fromLngLat(-77.167276, 38.775717),
                result.features()!![1].geometry() as Point,
            )
            assertEquals(
                Point.fromLngLat(-77.153468, 38.77091),
                result.features()!![2].geometry() as Point,
            )
        }
    }

    @Test
    fun `build waypoint FeatureCollection with silent waypoints`() {
        val route = mockk<NavigationRoute>()
        mockkStatic(route::internalWaypoints) {
            every { route.internalWaypoints() } returns listOf(
                WaypointFactory.provideWaypoint(
                    name = "w1",
                    location = Point.fromLngLat(-77.157347, 38.783004),
                    type = Waypoint.REGULAR,
                    target = null,
                    metadata = emptyMap(),
                ),
                WaypointFactory.provideWaypoint(
                    name = "w2",
                    location = Point.fromLngLat(-77.167276, 38.775717),
                    type = Waypoint.SILENT,
                    target = null,
                    metadata = emptyMap(),
                ),
                WaypointFactory.provideWaypoint(
                    name = "w3",
                    location = Point.fromLngLat(-77.153468, 38.77091),
                    type = Waypoint.REGULAR,
                    target = null,
                    metadata = emptyMap(),
                ),
            )
            val result = MapboxRouteLineUtils.buildWayPointFeatureCollection(route)

            assertEquals(2, result.features()!!.size)
            assertEquals(
                Point.fromLngLat(-77.157347, 38.783004),
                result.features()!![0].geometry() as Point,
            )
            assertEquals(
                Point.fromLngLat(-77.153468, 38.77091),
                result.features()!![1].geometry() as Point,
            )
        }
    }

    @Test
    fun `build waypoint FeatureCollection with EV waypoints`() {
        val route = mockk<NavigationRoute>()
        mockkStatic(route::internalWaypoints) {
            every { route.internalWaypoints() } returns listOf(
                WaypointFactory.provideWaypoint(
                    name = "w1",
                    location = Point.fromLngLat(-77.157347, 38.783004),
                    type = Waypoint.EV_CHARGING_SERVER,
                    target = null,
                    metadata = emptyMap(),
                ),
                WaypointFactory.provideWaypoint(
                    name = "w2",
                    location = Point.fromLngLat(-77.167276, 38.775717),
                    type = Waypoint.SILENT,
                    target = null,
                    metadata = emptyMap(),
                ),
                WaypointFactory.provideWaypoint(
                    name = "w3",
                    location = Point.fromLngLat(-77.153468, 38.77091),
                    type = Waypoint.REGULAR,
                    target = null,
                    metadata = emptyMap(),
                ),
            )
            val result = MapboxRouteLineUtils.buildWayPointFeatureCollection(route)

            assertEquals(2, result.features()!!.size)
            assertEquals(
                Point.fromLngLat(-77.157347, 38.783004),
                result.features()!![0].geometry() as Point,
            )
            assertEquals(
                Point.fromLngLat(-77.153468, 38.77091),
                result.features()!![1].geometry() as Point,
            )
        }
    }

    @Test
    fun getLayerVisibility() {
        mockkStatic("com.mapbox.maps.extension.style.layers.LayerUtils")
        val layer = mockk<Layer>(relaxed = true) {
            every { visibility } returns Visibility.VISIBLE
        }

        val style = mockk<Style> {
            every { getLayer("foobar") } returns layer
        }

        val result = MapboxRouteLineUtils.getLayerVisibility(style, "foobar")

        assertEquals(Visibility.VISIBLE, result)
        unmockkStatic("com.mapbox.maps.extension.style.layers.LayerUtils")
    }

    @Test
    fun getLayerVisibility_whenLayerNotFound() {
        mockkStatic("com.mapbox.maps.extension.style.layers.LayerUtils")
        val style = mockk<Style> {
            every { getLayer("foobar") } returns null
        }

        val result = MapboxRouteLineUtils.getLayerVisibility(style, "foobar")

        assertNull(result)
        unmockkStatic("com.mapbox.maps.extension.style.layers.LayerUtils")
    }

    @Test
    fun whenAnnotationIsCongestionNumericThenResolveLowCongestionNumeric() {
        val lowCongestionNumeric = 4

        val result = MapboxRouteLineUtils.resolveNumericToValue(
            lowCongestionNumeric,
            MapboxRouteLineApiOptions.Builder().build(),
        )

        assertEquals(LOW_CONGESTION_VALUE, result)
    }

    @Test
    fun whenAnnotationIsCongestionNumericThenResolveModerateCongestionNumeric() {
        val moderateCongestionNumeric = 45

        val result = MapboxRouteLineUtils.resolveNumericToValue(
            moderateCongestionNumeric,
            MapboxRouteLineApiOptions.Builder().build(),
        )

        assertEquals(MODERATE_CONGESTION_VALUE, result)
    }

    @Test
    fun whenAnnotationIsCongestionNumericThenResolveHeavyCongestionNumeric() {
        val heavyCongestionNumeric = 65

        val result = MapboxRouteLineUtils.resolveNumericToValue(
            heavyCongestionNumeric,
            MapboxRouteLineApiOptions.Builder().build(),
        )

        assertEquals(HEAVY_CONGESTION_VALUE, result)
    }

    @Test
    fun whenAnnotationIsCongestionNumericThenResolveSevereCongestionNumeric() {
        val severeCongestionNumeric = 85

        val result = MapboxRouteLineUtils.resolveNumericToValue(
            severeCongestionNumeric,
            MapboxRouteLineApiOptions.Builder().build(),
        )

        assertEquals(SEVERE_CONGESTION_VALUE, result)
    }

    @Test
    fun whenAnnotationIsCongestionNumericThenResolveUnknownCongestionNumeric() {
        val unknownCongestionNumeric = null

        val result = MapboxRouteLineUtils.resolveNumericToValue(
            unknownCongestionNumeric,
            MapboxRouteLineApiOptions.Builder().build(),
        )

        assertEquals(UNKNOWN_CONGESTION_VALUE, result)
    }

    @Test
    fun getRouteLineTrafficExpressionDataWithCongestionNumeric() {
        val route = loadNavigationRoute("route-with-congestion-numeric.json")
        val annotationProvider =
            MapboxRouteLineUtils.getTrafficCongestionAnnotationProvider(
                route,
                MapboxRouteLineApiOptions.Builder().build(),
            )

        val trafficExpressionData =
            MapboxRouteLineUtils.extractRouteDataWithTrafficAndRoadClassDeDuped(
                route,
                annotationProvider,
            )

        MapboxRouteLineUtils.extractRouteDataWithTrafficAndRoadClassDeDuped(
            route,
            MapboxRouteLineUtils.getTrafficCongestionAnnotationProvider(
                route,
                MapboxRouteLineApiOptions.Builder().build(),
            ),
        )
        MapboxRouteLineUtils.extractRouteDataWithTrafficAndRoadClassDeDuped(
            route,
            MapboxRouteLineUtils.getTrafficCongestionAnnotationProvider(
                route,
                MapboxRouteLineApiOptions.Builder().build(),
            ),
        )

        assertEquals("low", trafficExpressionData[0].trafficCongestionIdentifier)
        assertEquals("moderate", trafficExpressionData[1].trafficCongestionIdentifier)
        assertEquals("severe", trafficExpressionData[2].trafficCongestionIdentifier)
    }

    @Test
    fun getRouteLineTrafficExpressionDataWithNoCongestionOrCongestionNumeric() {
        val route = loadNavigationRoute("route-with-no-congestion-annotation.json")
        val annotationProvider =
            MapboxRouteLineUtils.getTrafficCongestionAnnotationProvider(
                route,
                MapboxRouteLineApiOptions.Builder().build(),
            )

        val trafficExpressionData =
            MapboxRouteLineUtils.extractRouteData(
                route,
                annotationProvider,
            )

        assertEquals("unknown", trafficExpressionData[0].trafficCongestionIdentifier)
        assertEquals(22, trafficExpressionData.size)
        assertEquals(1.0, trafficExpressionData.last().offset, 0.0)
    }

    @Test
    fun getAnnotationProvider_whenNumericTrafficSource_matchesDistances() {
        val route = loadNavigationRoute("route-with-congestion-numeric.json")

        val result =
            MapboxRouteLineUtils.getRouteLegTrafficNumericCongestionProvider(
                MapboxRouteLineApiOptions.Builder().build(),
            )

        assertEquals(
            route.directionsRoute.legs()!!.first().annotation()!!.distance()!!.size,
            result(route.directionsRoute.legs()!!.first()).size,
        )
    }

    @Test
    fun getRouteRestrictedSectionsExpressionData_multiLegRoute() {
        val route = loadNavigationRoute("two-leg-route-with-restrictions.json")

        val result = MapboxRouteLineUtils.extractRouteRestrictionData(
            route,
            MapboxRouteLineUtils.granularDistancesProvider,
        )

        assertEquals(7, result.size)
        assertTrue(result[1].isInRestrictedSection)
        assertTrue(result[2].isInRestrictedSection)
        assertFalse(result[3].isInRestrictedSection)
        assertFalse(result[4].isInRestrictedSection)
        assertEquals(1, result[4].legIndex)
        assertEquals(1, result[5].legIndex)
        assertTrue(result[5].isInRestrictedSection)
        assertEquals(1, result[6].legIndex)
        assertFalse(result[6].isInRestrictedSection)
    }

    @Test
    fun `extractRouteData with null congestion provider`() {
        val route = loadNavigationRoute("short_route.json")
        for (data in MapboxRouteLineUtils.extractRouteData(route) { null }) {
            assertEquals(UNKNOWN_CONGESTION_VALUE, data.trafficCongestionIdentifier)
        }
    }

    @Test
    fun `extractRouteData with empty congestion provider`() {
        val route = loadNavigationRoute("short_route.json")
        for (data in MapboxRouteLineUtils.extractRouteData(route) { emptyList() }) {
            assertEquals(UNKNOWN_CONGESTION_VALUE, data.trafficCongestionIdentifier)
        }
    }

    @Test
    fun `extractRouteData with short congestion provider`() {
        val route = loadNavigationRoute("short_route.json")
        val geometrySize = route.directionsRoute.completeGeometryToPoints().size
        // this has one less congestion annotation than required
        // by the number of geometry points in the route
        val fakeSize = geometrySize - 2
        val extractedData = MapboxRouteLineUtils.extractRouteData(route) {
            List(fakeSize) { "low" }
        }
        for (index in 0 until fakeSize) {
            assertEquals("low", extractedData[index].trafficCongestionIdentifier)
        }
        for (index in fakeSize until geometrySize) {
            assertEquals(
                UNKNOWN_CONGESTION_VALUE,
                extractedData[index].trafficCongestionIdentifier,
            )
        }
    }

    @Test
    fun `getRoadClassArray when route has step intersections with incorrect geometry indexes`() {
        val route = loadNavigationRoute("route-with-incorrect-geometry-indexes.json")

        val result = MapboxRouteLineUtils.extractRouteDataWithTrafficAndRoadClassDeDuped(
            route,
            MapboxRouteLineUtils.getRouteLegTrafficCongestionProvider,
        )

        assertEquals(1, result.size)
        assertEquals("unknown", result.first().trafficCongestionIdentifier)
        assertEquals(0.0, result.first().offset, 0.0)
    }

    @Test
    fun `featureCollectionHasProperty when FeatureCollection is null`() {
        val result = MapboxRouteLineUtils.featureCollectionHasProperty(
            null,
            0,
            "",
        )

        assertFalse(result)
    }

    @Test
    fun `featureCollectionHasProperty when features null`() {
        val mockFeatureCollection = mockk<FeatureCollection> {
            every { features() } returns null
        }
        val result = MapboxRouteLineUtils.featureCollectionHasProperty(
            mockFeatureCollection,
            0,
            "",
        )

        assertFalse(result)
    }

    @Test
    fun `featureCollectionHasProperty when features empty`() {
        val mockFeatureCollection = mockk<FeatureCollection> {
            every { features() } returns listOf()
        }
        val result = MapboxRouteLineUtils.featureCollectionHasProperty(
            mockFeatureCollection,
            0,
            "",
        )

        assertFalse(result)
    }

    @Test
    fun `featureCollectionHasProperty when index equal to features size`() {
        val mockFeatureCollection = mockk<FeatureCollection> {
            every { features() } returns listOf(mockk())
        }
        val result = MapboxRouteLineUtils.featureCollectionHasProperty(
            mockFeatureCollection,
            1,
            "",
        )

        assertFalse(result)
    }

    @Test
    fun `featureCollectionHasProperty when index greater than features size`() {
        val mockFeatureCollection = mockk<FeatureCollection> {
            every { features() } returns listOf(mockk())
        }
        val result = MapboxRouteLineUtils.featureCollectionHasProperty(
            mockFeatureCollection,
            5,
            "",
        )

        assertFalse(result)
    }

    @Test
    fun `featureCollectionHasProperty when feature has property`() {
        val mockFeature1 = mockk<Feature> {
            every { hasNonNullValueForProperty("someProperty") } returns false
        }
        val mockFeature2 = mockk<Feature> {
            every { hasNonNullValueForProperty("someProperty") } returns true
        }
        val mockFeatureCollection = mockk<FeatureCollection> {
            every { features() } returns listOf(mockFeature1, mockFeature2)
        }
        val result = MapboxRouteLineUtils.featureCollectionHasProperty(
            mockFeatureCollection,
            0,
            "someProperty",
        )

        assertTrue(result)
    }

    @Test
    fun layerGroup1SourceKey() {
        assertEquals(LAYER_GROUP_1_SOURCE_ID, MapboxRouteLineUtils.layerGroup1SourceKey.sourceId)
    }

    @Test
    fun layerGroup2SourceKey() {
        assertEquals(LAYER_GROUP_2_SOURCE_ID, MapboxRouteLineUtils.layerGroup2SourceKey.sourceId)
    }

    @Test
    fun layerGroup3SourceKey() {
        assertEquals(LAYER_GROUP_3_SOURCE_ID, MapboxRouteLineUtils.layerGroup3SourceKey.sourceId)
    }

    @Test
    fun layerGroup1SourceLayerIds() {
        assertEquals(7, MapboxRouteLineUtils.layerGroup1SourceLayerIds.size)
        assertTrue(
            MapboxRouteLineUtils.layerGroup1SourceLayerIds.contains(
                RouteLayerConstants.LAYER_GROUP_1_TRAIL_CASING,
            ),
        )
        assertTrue(
            MapboxRouteLineUtils.layerGroup1SourceLayerIds.contains(
                RouteLayerConstants.LAYER_GROUP_1_TRAIL,
            ),
        )
        assertTrue(
            MapboxRouteLineUtils.layerGroup1SourceLayerIds.contains(
                RouteLayerConstants.LAYER_GROUP_1_CASING,
            ),
        )
        assertTrue(
            MapboxRouteLineUtils.layerGroup1SourceLayerIds.contains(
                RouteLayerConstants.LAYER_GROUP_1_MAIN,
            ),
        )
        assertTrue(
            MapboxRouteLineUtils.layerGroup1SourceLayerIds.contains(
                RouteLayerConstants.LAYER_GROUP_1_TRAFFIC,
            ),
        )
        assertTrue(
            MapboxRouteLineUtils.layerGroup1SourceLayerIds.contains(
                RouteLayerConstants.LAYER_GROUP_1_RESTRICTED,
            ),
        )
        assertTrue(
            MapboxRouteLineUtils.layerGroup1SourceLayerIds.contains(
                RouteLayerConstants.LAYER_GROUP_1_BLUR,
            ),
        )
    }

    @Test
    fun layerGroup2SourceLayerIds() {
        assertEquals(7, MapboxRouteLineUtils.layerGroup2SourceLayerIds.size)
        assertTrue(
            MapboxRouteLineUtils.layerGroup2SourceLayerIds.contains(
                RouteLayerConstants.LAYER_GROUP_2_TRAIL_CASING,
            ),
        )
        assertTrue(
            MapboxRouteLineUtils.layerGroup2SourceLayerIds.contains(
                RouteLayerConstants.LAYER_GROUP_2_TRAIL,
            ),
        )
        assertTrue(
            MapboxRouteLineUtils.layerGroup2SourceLayerIds.contains(
                RouteLayerConstants.LAYER_GROUP_2_CASING,
            ),
        )
        assertTrue(
            MapboxRouteLineUtils.layerGroup2SourceLayerIds.contains(
                RouteLayerConstants.LAYER_GROUP_2_MAIN,
            ),
        )
        assertTrue(
            MapboxRouteLineUtils.layerGroup2SourceLayerIds.contains(
                RouteLayerConstants.LAYER_GROUP_2_TRAFFIC,
            ),
        )
        assertTrue(
            MapboxRouteLineUtils.layerGroup2SourceLayerIds.contains(
                RouteLayerConstants.LAYER_GROUP_2_RESTRICTED,
            ),
        )
        assertTrue(
            MapboxRouteLineUtils.layerGroup2SourceLayerIds.contains(
                RouteLayerConstants.LAYER_GROUP_2_BLUR,
            ),
        )
    }

    @Test
    fun layerGroup3SourceLayerIds() {
        assertEquals(7, MapboxRouteLineUtils.layerGroup3SourceLayerIds.size)
        assertTrue(
            MapboxRouteLineUtils.layerGroup3SourceLayerIds.contains(
                RouteLayerConstants.LAYER_GROUP_3_TRAIL_CASING,
            ),
        )
        assertTrue(
            MapboxRouteLineUtils.layerGroup3SourceLayerIds.contains(
                RouteLayerConstants.LAYER_GROUP_3_TRAIL,
            ),
        )
        assertTrue(
            MapboxRouteLineUtils.layerGroup3SourceLayerIds.contains(
                RouteLayerConstants.LAYER_GROUP_3_CASING,
            ),
        )
        assertTrue(
            MapboxRouteLineUtils.layerGroup3SourceLayerIds.contains(
                RouteLayerConstants.LAYER_GROUP_3_MAIN,
            ),
        )
        assertTrue(
            MapboxRouteLineUtils.layerGroup3SourceLayerIds.contains(
                RouteLayerConstants.LAYER_GROUP_3_TRAFFIC,
            ),
        )
        assertTrue(
            MapboxRouteLineUtils.layerGroup3SourceLayerIds.contains(
                RouteLayerConstants.LAYER_GROUP_3_RESTRICTED,
            ),
        )
        assertTrue(
            MapboxRouteLineUtils.layerGroup3SourceLayerIds.contains(
                RouteLayerConstants.LAYER_GROUP_3_BLUR,
            ),
        )
    }

    @Test
    fun sourceLayerMap() {
        assertEquals(3, MapboxRouteLineUtils.sourceLayerMap.size)
        assertEquals(
            MapboxRouteLineUtils.layerGroup1SourceKey,
            MapboxRouteLineUtils.sourceLayerMap.keys.toList()[0],
        )
        assertEquals(
            MapboxRouteLineUtils.layerGroup1SourceLayerIds,
            MapboxRouteLineUtils.sourceLayerMap.values.toList()[0],
        )
        assertEquals(
            MapboxRouteLineUtils.layerGroup2SourceKey,
            MapboxRouteLineUtils.sourceLayerMap.keys.toList()[1],
        )
        assertEquals(
            MapboxRouteLineUtils.layerGroup2SourceLayerIds,
            MapboxRouteLineUtils.sourceLayerMap.values.toList()[1],
        )
        assertEquals(
            MapboxRouteLineUtils.layerGroup3SourceKey,
            MapboxRouteLineUtils.sourceLayerMap.keys.toList()[2],
        )
        assertEquals(
            MapboxRouteLineUtils.layerGroup3SourceLayerIds,
            MapboxRouteLineUtils.sourceLayerMap.values.toList()[2],
        )
    }

    @Test
    fun getLayerIdsForPrimaryRoute() {
        val topLevelRouteLayer = StyleObjectInfo(
            TOP_LEVEL_ROUTE_LINE_LAYER_ID,
            "background",
        )
        val bottomLevelRouteLayer = StyleObjectInfo(
            BOTTOM_LEVEL_ROUTE_LINE_LAYER_ID,
            "background",
        )
        val layerGroup1 = StyleObjectInfo(
            LAYER_GROUP_1_MAIN,
            "line",
        )
        val layerGroup2 = StyleObjectInfo(
            LAYER_GROUP_2_MAIN,
            "line",
        )
        val style = mockk<Style> {
            every { styleLayers } returns listOf(
                bottomLevelRouteLayer,
                layerGroup2,
                layerGroup1,
                topLevelRouteLayer,
            )
        }

        val result = MapboxRouteLineUtils.getLayerIdsForPrimaryRoute(
            style,
            MapboxRouteLineUtils.sourceLayerMap,
        )

        assertEquals(MapboxRouteLineUtils.layerGroup1SourceLayerIds, result)
    }

    @Test
    fun `extractRouteRestrictionData with restriction at end of route`() {
        val route = loadNavigationRoute("route-with-restrictions-at-end.json")

        val result = MapboxRouteLineUtils.extractRouteRestrictionData(
            route,
            MapboxRouteLineUtils.granularDistancesProvider,
        )

        assertEquals(3, result.size)
        assertEquals(0.0, result[0].offset, 0.0)
        assertEquals(0, result[0].legIndex)
        assertFalse(result[0].isInRestrictedSection)
        assertEquals(0.9963424099457971, result[1].offset, tolerance)
        assertEquals(0, result[1].legIndex)
        assertTrue(result[1].isInRestrictedSection)
    }

    @Test
    fun `getRestrictedLineExpression with restriction at end of route`() {
        val expectedExpressionContents = listOf(
            StringChecker("step"),
            StringChecker("[line-progress]"),
            StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            DoubleChecker(0.0),
            StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            DoubleChecker(0.9963424099457971),
            StringChecker("[rgba, 0.0, 255.0, 255.0, 1.0]"),
            DoubleChecker(1.0),
            StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
        )
        val route = loadNavigationRoute("route-with-restrictions-at-end.json")
        val expressionData = MapboxRouteLineUtils.extractRouteRestrictionData(
            route,
            MapboxRouteLineUtils.granularDistancesProvider,
        )

        val result = MapboxRouteLineUtils.getRestrictedLineExpression(
            0.0,
            0,
            Color.CYAN,
            Color.GRAY,
            expressionData,
        )

        checkExpression(expectedExpressionContents, result)
    }

    @Test
    fun `extractRouteRestrictionData with restriction at start of route`() {
        val route = loadNavigationRoute("route-with-restrictions-at-start.json")

        val result = MapboxRouteLineUtils.extractRouteRestrictionData(
            route,
            MapboxRouteLineUtils.granularDistancesProvider,
        )

        assertEquals(2, result.size)
        assertEquals(0.0, result[0].offset, 0.0)
        assertEquals(0, result[0].legIndex)
        assertTrue(result[0].isInRestrictedSection)
        assertEquals(0.0036660165533364264, result[1].offset, tolerance)
        assertEquals(0, result[1].legIndex)
        assertFalse(result[1].isInRestrictedSection)
    }

    @Test
    fun `extractRouteRestrictionData when RouteLineGranularDistances null`() {
        val route = loadNavigationRoute("route-with-restrictions-at-start.json")

        val result = MapboxRouteLineUtils.extractRouteRestrictionData(
            route,
        ) { null }

        assertTrue(result.isEmpty())
    }

    @Test
    fun `extractRouteRestrictionData when leg distances array size less than route leg size`() {
        val route = loadNavigationRoute("multileg_route_two_legs_with_restrictions.json")
        val granularDistances = MapboxRouteLineUtils.granularDistancesProvider(route)!!
        val updatedDistances = RouteLineGranularDistances(
            granularDistances.completeDistance,
            granularDistances.routeDistances,
            arrayOf(granularDistances.legsDistances.first()),
            granularDistances.stepsDistances,
        )

        val result = MapboxRouteLineUtils.extractRouteRestrictionData(
            route,
        ) { updatedDistances }

        assertEquals(5, result.size)
    }

    @Test
    fun `extractRouteRestrictionData when step intersection geometry index not found in leg distances array`() {
        val route = loadNavigationRoute("multileg_route_two_legs_with_restrictions.json")
        val granularDistances = MapboxRouteLineUtils.granularDistancesProvider(route)!!
        val updatedLegDistances = granularDistances.legsDistances[1]
            .drop(granularDistances.legsDistances[1].size - 1)
            .toTypedArray()

        val updatedDistances = RouteLineGranularDistances(
            granularDistances.completeDistance,
            granularDistances.routeDistances,
            arrayOf(granularDistances.legsDistances.first(), updatedLegDistances),
            granularDistances.stepsDistances,
        )

        val result = MapboxRouteLineUtils.extractRouteRestrictionData(
            route,
        ) { updatedDistances }

        assertEquals(6, result.size)
    }

    @Test
    fun `getRestrictedLineExpression with restriction across two legs`() {
        val exp = listOf(
            StringChecker("step"),
            StringChecker("[line-progress]"),
            StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            DoubleChecker(0.0),
            StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            DoubleChecker(0.3956457979751531),
            StringChecker("[rgba, 0.0, 255.0, 255.0, 1.0]"),
            DoubleChecker(0.5540039481345271),
            StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
        )
        val route = loadNavigationRoute("multileg_route_two_legs_with_restrictions.json")
        val expressionData = MapboxRouteLineUtils.extractRouteRestrictionData(
            route,
            MapboxRouteLineUtils.granularDistancesProvider,
        )

        val result = MapboxRouteLineUtils.getRestrictedLineExpression(
            vanishingPointOffset = 0.0,
            activeLegIndex = -1,
            Color.CYAN,
            Color.GRAY,
            expressionData,
        )

        checkExpression(exp, result)
    }

    @Test
    fun `getRestrictedLineExpression with restriction across two legs, active index = 0`() {
        val expExpression = listOf(
            StringChecker("step"),
            StringChecker("[line-progress]"),
            StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            DoubleChecker(0.0),
            StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            DoubleChecker(0.3956457979751531),
            StringChecker("[rgba, 0.0, 255.0, 255.0, 1.0]"),
            DoubleChecker(0.4897719974699625),
            StringChecker("[rgba, 136.0, 136.0, 136.0, 1.0]"),
            DoubleChecker(0.5540039481345271),
            StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
        )
        val route = loadNavigationRoute("multileg_route_two_legs_with_restrictions.json")
        val expressionData = MapboxRouteLineUtils.extractRouteRestrictionData(
            route,
            MapboxRouteLineUtils.granularDistancesProvider,
        )

        val result = MapboxRouteLineUtils.getRestrictedLineExpression(
            vanishingPointOffset = 0.0,
            activeLegIndex = 0,
            Color.CYAN,
            Color.GRAY,
            expressionData,
        )

        checkExpression(expExpression, result)
    }

    @Test
    fun `getRestrictedLineExpression with restriction across two legs, active index = 1`() {
        val expectedExpression = listOf(
            StringChecker("step"),
            StringChecker("[line-progress]"),
            StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            DoubleChecker(0.0),
            StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            DoubleChecker(0.3956457979751531),
            StringChecker("[rgba, 136.0, 136.0, 136.0, 1.0]"),
            DoubleChecker(0.4897719974699625),
            StringChecker("[rgba, 0.0, 255.0, 255.0, 1.0]"),
            DoubleChecker(0.5540039481345271),
            StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
        )
        val route = loadNavigationRoute("multileg_route_two_legs_with_restrictions.json")
        val expressionData = MapboxRouteLineUtils.extractRouteRestrictionData(
            route,
            MapboxRouteLineUtils.granularDistancesProvider,
        )

        val result = MapboxRouteLineUtils.getRestrictedLineExpression(
            vanishingPointOffset = 0.0,
            activeLegIndex = 1,
            Color.CYAN,
            Color.GRAY,
            expressionData,
        )

        checkExpression(expectedExpression, result)
    }

    @Test
    fun `getNonMaskingRestrictedLineExpression with restriction across two legs, enabled`() = runBlocking {
        val viewData = MapboxRouteLineViewOptions.Builder(ctx)
            .routeLineColorResources(
                RouteLineColorResources.Builder()
                    .restrictedRoadColor(Color.CYAN)
                    .inactiveRouteLegRestrictedRoadColor(Color.GRAY)
                    .build(),
            )
            .displayRestrictedRoadSections(true)
            .build()
            .toData()
        val apiOptions = MapboxRouteLineApiOptions.Builder()
            .styleInactiveRouteLegsIndependently(true)
            .calculateRestrictedRoadSections(true)
            .build()
        val expectedExpression = listOf(
            StringChecker("step"),
            StringChecker("[line-progress]"),
            StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            DoubleChecker(0.0),
            StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            DoubleChecker(0.3956457979751531),
            StringChecker("[rgba, 136.0, 136.0, 136.0, 1.0]"),
            DoubleChecker(0.4897719974699625),
            StringChecker("[rgba, 0.0, 255.0, 255.0, 1.0]"),
            DoubleChecker(0.5540039481345271),
            StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
        )
        val route = loadNavigationRoute("multileg_route_two_legs_with_restrictions.json")
        val expressionData = MapboxRouteLineUtils.extractRouteRestrictionData(
            route,
            MapboxRouteLineUtils.granularDistancesProvider,
        )

        val result = MapboxRouteLineUtils.getNonMaskingRestrictedLineExpressionProducer(
            expressionData,
            vanishingPointOffset = 0.0,
            activeLegIndex = 1,
            apiOptions,
        )

        checkExpression(expectedExpression, result(viewData))
    }

    @Test
    fun `getNonMaskingRestrictedLineExpression with restriction across two legs, disabled`() = runBlocking {
        val viewData = MapboxRouteLineViewOptions.Builder(ctx)
            .routeLineColorResources(
                RouteLineColorResources.Builder()
                    .restrictedRoadColor(Color.CYAN)
                    .inactiveRouteLegRestrictedRoadColor(Color.GRAY)
                    .build(),
            )
            .displayRestrictedRoadSections(true)
            .build()
            .toData()
        val apiOptions = MapboxRouteLineApiOptions.Builder()
            .styleInactiveRouteLegsIndependently(false)
            .calculateRestrictedRoadSections(true)
            .build()
        val expectedExpression = listOf(
            StringChecker("step"),
            StringChecker("[line-progress]"),
            StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            DoubleChecker(0.0),
            StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            DoubleChecker(0.3956457979751531),
            StringChecker("[rgba, 0.0, 255.0, 255.0, 1.0]"),
            DoubleChecker(0.5540039481345271),
            StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
        )
        val route = loadNavigationRoute("multileg_route_two_legs_with_restrictions.json")
        val expressionData = MapboxRouteLineUtils.extractRouteRestrictionData(
            route,
            MapboxRouteLineUtils.granularDistancesProvider,
        )

        val result = MapboxRouteLineUtils.getNonMaskingRestrictedLineExpressionProducer(
            expressionData,
            vanishingPointOffset = 0.0,
            activeLegIndex = 1,
            apiOptions,
        )

        checkExpression(expectedExpression, result(viewData))
    }

    @Test
    fun cacheResultKeyRouteTraffic_hashCode_numericTraffic() {
        val route = loadNavigationRoute("route-with-congestion-numeric.json")
        val trafficProvider =
            MapboxRouteLineUtils.getRouteLegTrafficNumericCongestionProvider(
                MapboxRouteLineApiOptions.Builder().build(),
            )
        var expectedHashCode = route.id.hashCode()
        expectedHashCode = 31 * expectedHashCode + trafficProvider.hashCode()
        route.directionsRoute.legs()?.forEach { routeLeg ->
            expectedHashCode =
                31 * expectedHashCode + routeLeg.annotation()?.congestion().hashCode()
            expectedHashCode =
                31 * expectedHashCode + routeLeg.annotation()?.congestionNumeric().hashCode()
            expectedHashCode = 31 * expectedHashCode + routeLeg.closures().hashCode()
            MapboxRouteLineUtils.getRoadClassArray(routeLeg.steps()).forEach {
                expectedHashCode = 31 * expectedHashCode + it.hashCode()
            }
        }

        val hashCode = CacheResultUtils.CacheResultKeyRouteTraffic<Any>(
            route,
            trafficProvider,
        ).hashCode()

        assertEquals(expectedHashCode, hashCode)
    }

    @Test
    fun removeLayers() {
        val style = mockk<StyleManager> {
            every { removeStyleLayer(any()) } returns ExpectedFactory.createNone()
            every { removeStyleImage(any()) } returns ExpectedFactory.createNone()
        }

        MapboxRouteLineUtils.removeLayers(style)

        verify { style.removeStyleLayer(TOP_LEVEL_ROUTE_LINE_LAYER_ID) }
        verify { style.removeStyleLayer(BOTTOM_LEVEL_ROUTE_LINE_LAYER_ID) }
        verify { style.removeStyleLayer(LAYER_GROUP_1_TRAIL_CASING) }
        verify { style.removeStyleLayer(LAYER_GROUP_1_TRAIL) }
        verify { style.removeStyleLayer(LAYER_GROUP_1_CASING) }
        verify { style.removeStyleLayer(LAYER_GROUP_1_MAIN) }
        verify { style.removeStyleLayer(LAYER_GROUP_1_TRAFFIC) }
        verify { style.removeStyleLayer(LAYER_GROUP_1_RESTRICTED) }
        verify { style.removeStyleLayer(LAYER_GROUP_2_TRAIL_CASING) }
        verify { style.removeStyleLayer(LAYER_GROUP_2_TRAIL) }
        verify { style.removeStyleLayer(LAYER_GROUP_2_CASING) }
        verify { style.removeStyleLayer(LAYER_GROUP_2_MAIN) }
        verify { style.removeStyleLayer(LAYER_GROUP_2_TRAFFIC) }
        verify { style.removeStyleLayer(LAYER_GROUP_2_RESTRICTED) }
        verify { style.removeStyleLayer(LAYER_GROUP_3_TRAIL_CASING) }
        verify { style.removeStyleLayer(LAYER_GROUP_3_TRAIL) }
        verify { style.removeStyleLayer(LAYER_GROUP_3_CASING) }
        verify { style.removeStyleLayer(LAYER_GROUP_3_MAIN) }
        verify { style.removeStyleLayer(LAYER_GROUP_3_TRAFFIC) }
        verify { style.removeStyleLayer(LAYER_GROUP_3_RESTRICTED) }
        verify { style.removeStyleLayer(WAYPOINT_LAYER_ID) }
        verify { style.removeStyleImage(ORIGIN_MARKER_NAME) }
        verify { style.removeStyleImage(DESTINATION_MARKER_NAME) }
        verify { style.removeStyleLayer(LAYER_GROUP_1_BLUR) }
        verify { style.removeStyleLayer(LAYER_GROUP_2_BLUR) }
        verify { style.removeStyleLayer(LAYER_GROUP_3_BLUR) }
    }

    @Test
    fun cacheResultKeyRouteTraffic_hashCode_legacyTraffic() {
        val route = loadNavigationRoute("short_route.json")
        val trafficProvider = MapboxRouteLineUtils.getRouteLegTrafficCongestionProvider
        var expectedHashCode = route.id.hashCode()
        expectedHashCode = 31 * expectedHashCode + trafficProvider.hashCode()
        route.directionsRoute.legs()?.forEach { routeLeg ->
            expectedHashCode =
                31 * expectedHashCode + routeLeg.annotation()?.congestion().hashCode()
            expectedHashCode =
                31 * expectedHashCode + routeLeg.annotation()?.congestionNumeric().hashCode()
            expectedHashCode = 31 * expectedHashCode + routeLeg.closures().hashCode()
        }

        val hashCode = CacheResultUtils.CacheResultKeyRouteTraffic<Any>(
            route,
            trafficProvider,
        ).hashCode()

        assertEquals(expectedHashCode, hashCode)
    }

    @Test
    fun cacheResultKeyRouteTraffic_hashCode_multiLegRoute() {
        val route = loadNavigationRoute("multileg-route-two-legs.json")
        val trafficProvider =
            MapboxRouteLineUtils.getRouteLegTrafficCongestionProvider
        var expectedHashCode = route.id.hashCode()
        expectedHashCode = 31 * expectedHashCode + trafficProvider.hashCode()
        route.directionsRoute.legs()?.forEach { routeLeg ->
            expectedHashCode =
                31 * expectedHashCode + routeLeg.annotation()?.congestion().hashCode()
            expectedHashCode =
                31 * expectedHashCode + routeLeg.annotation()?.congestionNumeric().hashCode()
            expectedHashCode = 31 * expectedHashCode + routeLeg.closures().hashCode()
            MapboxRouteLineUtils.getRoadClassArray(routeLeg.steps()).forEach {
                expectedHashCode = 31 * expectedHashCode + it.hashCode()
            }
        }

        val hashCode = CacheResultUtils.CacheResultKeyRouteTraffic<Any>(
            route,
            trafficProvider,
        ).hashCode()

        assertEquals(expectedHashCode, hashCode)
    }

    @Test
    fun cacheResultKeyRouteTraffic_equals() {
        val route = loadNavigationRoute("multileg-route-two-legs.json")
        val trafficProvider =
            MapboxRouteLineUtils.getRouteLegTrafficCongestionProvider
        val eqKey = CacheResultUtils.CacheResultKeyRouteTraffic<Any>(route, trafficProvider)
        val equals =
            CacheResultUtils.CacheResultKeyRouteTraffic<Any>(route, trafficProvider).equals(eqKey)

        assertTrue(equals)
    }

    @Test
    fun cacheResultKeyRouteTraffic_notEquals() {
        val route = loadNavigationRoute("short_route.json")
        val routeId = route.id
        val directionsRoute2 = route.directionsRoute.toBuilder()
            .legs(
                route.directionsRoute.legs()?.mapIndexed { index, leg ->
                    if (index == 0) {
                        leg.toBuilder()
                            .annotation(
                                leg.annotation()?.toBuilder()
                                    ?.congestion(leg.annotation()?.congestion()?.reversed())
                                    ?.build(),
                            )
                            .build()
                    } else {
                        leg
                    }
                },
            )
            .build()
        val route2 = mockk<NavigationRoute> {
            every { id } returns routeId
            every { directionsRoute } returns directionsRoute2
        }
        val trafficProvider = MapboxRouteLineUtils.getRouteLegTrafficCongestionProvider
        val eqKey = CacheResultUtils.CacheResultKeyRouteTraffic<Any>(route2, trafficProvider)

        val equals =
            CacheResultUtils.CacheResultKeyRouteTraffic<Any>(route, trafficProvider).equals(eqKey)

        assertFalse(equals)
    }

    @Test
    fun cacheResultKeyRouteTraffic_numericTrafficNotEquals() {
        val route = loadNavigationRoute("route-with-congestion-numeric.json")
        val routeId = route.id
        val directionsRoute2 = route.directionsRoute.toBuilder()
            .legs(
                route.directionsRoute.legs()?.mapIndexed { index, leg ->
                    if (index == 0) {
                        leg.toBuilder()
                            .annotation(
                                leg.annotation()?.toBuilder()
                                    ?.congestionNumeric(
                                        leg.annotation()?.congestionNumeric()?.reversed(),
                                    )
                                    ?.build(),
                            )
                            .build()
                    } else {
                        leg
                    }
                },
            )
            .build()
        val route2 = mockk<NavigationRoute> {
            every { id } returns routeId
            every { directionsRoute } returns directionsRoute2
        }
        val trafficProvider =
            MapboxRouteLineUtils.getRouteLegTrafficNumericCongestionProvider(
                MapboxRouteLineApiOptions.Builder().build(),
            )
        val eqKey = CacheResultUtils.CacheResultKeyRouteTraffic<Any>(route2, trafficProvider)

        val equals =
            CacheResultUtils.CacheResultKeyRouteTraffic<Any>(route, trafficProvider).equals(eqKey)

        assertFalse(equals)
    }

    @Test
    fun cacheResultKeyRouteTraffic_differentTrafficType() {
        val routeWithLegacyTraffic = loadNavigationRoute("short_route.json")
        val route = loadNavigationRoute("route-with-congestion-numeric.json")
        val routeId = route.id
        val directionsRoute2 = route.directionsRoute.toBuilder()
            .routeOptions(routeWithLegacyTraffic.directionsRoute.routeOptions())
            .build()
        val route2 = mockk<NavigationRoute> {
            every { id } returns routeId
            every { directionsRoute } returns directionsRoute2
        }
        val trafficProvider =
            MapboxRouteLineUtils.getRouteLegTrafficNumericCongestionProvider(
                MapboxRouteLineApiOptions.Builder().build(),
            )
        val eqKey = CacheResultUtils.CacheResultKeyRouteTraffic<Any>(route2, trafficProvider)

        val equals =
            CacheResultUtils.CacheResultKeyRouteTraffic<Any>(route, trafficProvider).equals(eqKey)

        assertFalse(equals)
    }

    @Test
    fun cacheResultKeyRouteTraffic_closuresNotEqual() {
        val trafficProvider = MapboxRouteLineUtils.getRouteLegTrafficCongestionProvider
        val route = loadNavigationRoute("route-with-closure.json")
        val routeId = route.id
        val directionsRoute2 = route.directionsRoute.toBuilder()
            .legs(
                route.directionsRoute.legs()?.mapIndexed { index, leg ->
                    if (index == 0) {
                        leg.toBuilder()
                            .closures(
                                leg.closures()!!.mapIndexed { index, closure ->
                                    if (index == 0) {
                                        closure.toBuilder().geometryIndexStart(5).build()
                                    } else {
                                        closure
                                    }
                                },
                            )
                            .build()
                    } else {
                        leg
                    }
                },
            )
            .build()
        val route2 = mockk<NavigationRoute> {
            every { id } returns routeId
            every { directionsRoute } returns directionsRoute2
        }
        val eqKey = CacheResultUtils.CacheResultKeyRouteTraffic<Any>(route2, trafficProvider)

        val equals =
            CacheResultUtils.CacheResultKeyRouteTraffic<Any>(route, trafficProvider).equals(eqKey)

        assertFalse(equals)
    }

    @Test
    fun cacheResultKeyRouteTraffic_closuresEqual() {
        val trafficProvider = MapboxRouteLineUtils.getRouteLegTrafficCongestionProvider
        val route = loadNavigationRoute("route-with-closure.json")
        val eqKey = CacheResultUtils.CacheResultKeyRouteTraffic<Any>(route, trafficProvider)

        val equals =
            CacheResultUtils.CacheResultKeyRouteTraffic<Any>(route, trafficProvider).equals(eqKey)

        assertTrue(equals)
    }

    @Test
    fun cacheResultKeyRouteTraffic_roadClassesNotEqual() {
        val trafficProvider = MapboxRouteLineUtils.getRouteLegTrafficCongestionProvider
        val route = loadNavigationRoute("route-with-closure.json")
        val routeId = route.id
        val mapboxStreetsV8 = route.directionsRoute
            .legs()!![0]
            .steps()!![0]
            .intersections()!![0]
            .mapboxStreetsV8()!!
            .toBuilder()
            .roadClass("foobar")
            .build()
        val stepIntersection = route.directionsRoute
            .legs()!![0]
            .steps()!![0]
            .intersections()!![0]
            .toBuilder()
            .mapboxStreetsV8(mapboxStreetsV8)
            .build()
        val intersections = route.directionsRoute
            .legs()!![0]
            .steps()!![0]
            .intersections()!!
            .drop(1)
            .toMutableList()
            .also { it.add(0, stepIntersection) }
        val legStep = route.directionsRoute
            .legs()!![0]
            .steps()!![0]
            .toBuilder()
            .intersections(intersections)
            .build()
        val legSteps = route.directionsRoute
            .legs()!![0]
            .steps()!!
            .drop(1)
            .toMutableList()
            .also { it.add(0, legStep) }
        val leg = route.directionsRoute.legs()!![0].toBuilder().steps(legSteps).build()
        val updatedRoute = route.directionsRoute.toBuilder().legs(listOf(leg)).build()
        val route2 = mockk<NavigationRoute> {
            every { id } returns routeId
            every { directionsRoute } returns updatedRoute
        }
        val eqKey = CacheResultUtils.CacheResultKeyRouteTraffic<Any>(route2, trafficProvider)

        val equals =
            CacheResultUtils.CacheResultKeyRouteTraffic<Any>(route, trafficProvider).equals(eqKey)

        assertFalse(equals)
    }

    @Test
    fun cacheResultKeyRouteTraffic_routeLegsNotEqualSize() {
        val trafficProvider = MapboxRouteLineUtils.getRouteLegTrafficCongestionProvider
        val route = loadNavigationRoute("route-with-closure.json")
        val routeId = route.id
        val directionsRoute2 = route.directionsRoute.toBuilder()
            .legs(route.directionsRoute.legs()!! + mockk<RouteLeg>(relaxed = true))
            .build()
        val route2 = mockk<NavigationRoute> {
            every { id } returns routeId
            every { directionsRoute } returns directionsRoute2
        }
        val eqKey = CacheResultUtils.CacheResultKeyRouteTraffic<Any>(route, trafficProvider)

        val equals =
            CacheResultUtils.CacheResultKeyRouteTraffic<Any>(route2, trafficProvider).equals(eqKey)

        assertFalse(equals)
    }

    @Test
    fun cacheResultKeyRouteTraffic_routeIdNotEqual() {
        val trafficProvider = MapboxRouteLineUtils.getRouteLegTrafficCongestionProvider
        val route = loadNavigationRoute("route-with-closure.json")
        val route2 = mockk<NavigationRoute> {
            every { id } returns "foobar"
        }
        val eqKey = CacheResultUtils.CacheResultKeyRouteTraffic<Any>(route, trafficProvider)

        val equals =
            CacheResultUtils.CacheResultKeyRouteTraffic<Any>(route2, trafficProvider).equals(eqKey)

        assertFalse(equals)
    }

    @Test
    fun getCongestionColorTypeForInactiveRouteLegs() {
        val inactiveRouteColors = InactiveRouteColors()

        assertEquals(
            SegmentColorType.INACTIVE_LOW_CONGESTION,
            MapboxRouteLineUtils.getCongestionColorTypeForInactiveRouteLegs(
                "low",
                inactiveRouteColors,
            ),
        )
        assertEquals(
            SegmentColorType.INACTIVE_MODERATE_CONGESTION,
            MapboxRouteLineUtils.getCongestionColorTypeForInactiveRouteLegs(
                "moderate",
                inactiveRouteColors,
            ),
        )
        assertEquals(
            SegmentColorType.INACTIVE_HEAVY_CONGESTION,
            MapboxRouteLineUtils.getCongestionColorTypeForInactiveRouteLegs(
                "heavy",
                inactiveRouteColors,
            ),
        )
        assertEquals(
            SegmentColorType.INACTIVE_SEVERE_CONGESTION,
            MapboxRouteLineUtils.getCongestionColorTypeForInactiveRouteLegs(
                "severe",
                inactiveRouteColors,
            ),
        )
        assertEquals(
            SegmentColorType.INACTIVE_UNKNOWN_CONGESTION,
            MapboxRouteLineUtils.getCongestionColorTypeForInactiveRouteLegs(
                "unknown",
                inactiveRouteColors,
            ),
        )
        assertEquals(
            SegmentColorType.INACTIVE_CLOSURE,
            MapboxRouteLineUtils.getCongestionColorTypeForInactiveRouteLegs(
                "closed",
                inactiveRouteColors,
            ),
        )
        assertEquals(
            SegmentColorType.INACTIVE_RESTRICTED,
            MapboxRouteLineUtils.getCongestionColorTypeForInactiveRouteLegs(
                "restricted",
                inactiveRouteColors,
            ),
        )
        assertEquals(
            SegmentColorType.INACTIVE_UNKNOWN_CONGESTION,
            MapboxRouteLineUtils.getCongestionColorTypeForInactiveRouteLegs(
                "aaa",
                inactiveRouteColors,
            ),
        )
    }

    @Test
    fun getRouteColorTypeForCongestion_primaryRoute() {
        assertEquals(
            SegmentColorType.PRIMARY_LOW_CONGESTION,
            MapboxRouteLineUtils.getRouteColorTypeForCongestion("low", true),
        )
        assertEquals(
            SegmentColorType.PRIMARY_MODERATE_CONGESTION,
            MapboxRouteLineUtils.getRouteColorTypeForCongestion("moderate", true),
        )
        assertEquals(
            SegmentColorType.PRIMARY_HEAVY_CONGESTION,
            MapboxRouteLineUtils.getRouteColorTypeForCongestion("heavy", true),
        )
        assertEquals(
            SegmentColorType.PRIMARY_SEVERE_CONGESTION,
            MapboxRouteLineUtils.getRouteColorTypeForCongestion("severe", true),
        )
        assertEquals(
            SegmentColorType.PRIMARY_UNKNOWN_CONGESTION,
            MapboxRouteLineUtils.getRouteColorTypeForCongestion("unknown", true),
        )
        assertEquals(
            SegmentColorType.PRIMARY_CLOSURE,
            MapboxRouteLineUtils.getRouteColorTypeForCongestion("closed", true),
        )
        assertEquals(
            SegmentColorType.PRIMARY_RESTRICTED,
            MapboxRouteLineUtils.getRouteColorTypeForCongestion("restricted", true),
        )
        assertEquals(
            SegmentColorType.PRIMARY_DEFAULT,
            MapboxRouteLineUtils.getRouteColorTypeForCongestion("aaa", true),
        )
    }

    @Test
    fun getRouteColorTypeForCongestion_nonPrimaryRoute() {
        assertEquals(
            SegmentColorType.ALTERNATIVE_LOW_CONGESTION,
            MapboxRouteLineUtils.getRouteColorTypeForCongestion("low", false),
        )
        assertEquals(
            SegmentColorType.ALTERNATIVE_MODERATE_CONGESTION,
            MapboxRouteLineUtils.getRouteColorTypeForCongestion("moderate", false),
        )
        assertEquals(
            SegmentColorType.ALTERNATIVE_HEAVY_CONGESTION,
            MapboxRouteLineUtils.getRouteColorTypeForCongestion("heavy", false),
        )
        assertEquals(
            SegmentColorType.ALTERNATIVE_SEVERE_CONGESTION,
            MapboxRouteLineUtils.getRouteColorTypeForCongestion("severe", false),
        )
        assertEquals(
            SegmentColorType.ALTERNATIVE_UNKNOWN_CONGESTION,
            MapboxRouteLineUtils.getRouteColorTypeForCongestion("unknown", false),
        )
        assertEquals(
            SegmentColorType.ALTERNATIVE_CLOSURE,
            MapboxRouteLineUtils.getRouteColorTypeForCongestion("closed", false),
        )
        assertEquals(
            SegmentColorType.ALTERNATIVE_RESTRICTED,
            MapboxRouteLineUtils.getRouteColorTypeForCongestion("restricted", false),
        )
        assertEquals(
            SegmentColorType.ALTERNATIVE_DEFAULT,
            MapboxRouteLineUtils.getRouteColorTypeForCongestion("aaa", false),
        )
    }

    @Test
    fun findClosestRouteLineDistanceIndexToPoint() {
        val route = loadNavigationRoute("customer_test_route.json")
        val granularDistances = MapboxRouteLineUtils.granularDistancesProvider(route)!!
        val point = Point.fromLngLat(11.590394230334425, 48.166546952177086)
        val index1 = MapboxRouteLineUtils.findClosestRouteLineDistanceIndexToPoint(
            point,
            granularDistances,
            8,
        )
        val index2 = MapboxRouteLineUtils.findClosestRouteLineDistanceIndexToPoint(
            point,
            granularDistances,
            9,
        )

        assertEquals(index1, index2)
    }

    @Test
    fun findClosestRouteLineDistanceIndexToPoint_whenPointBeyondLastGranularDistancePoint() {
        val route = loadNavigationRoute("short_route.json")
        val granularDistances = MapboxRouteLineUtils.granularDistancesProvider(route)!!
        val bearing = TurfMeasurement.bearing(
            granularDistances.routeDistances[granularDistances.routeDistances.lastIndex - 1].point,
            granularDistances.routeDistances[granularDistances.routeDistances.lastIndex].point,
        )
        val point = TurfMeasurement.destination(
            granularDistances.routeDistances[granularDistances.routeDistances.lastIndex].point,
            1.0,
            bearing,
            TurfConstants.UNIT_METERS,
        )
        val index = MapboxRouteLineUtils.findClosestRouteLineDistanceIndexToPoint(
            point,
            granularDistances,
            granularDistances.routeDistances.lastIndex,
        )

        assertEquals(index, 4)
    }
}
