package com.mapbox.navigation.ui.maps.internal.route.line

import android.content.Context
import android.graphics.Color
import androidx.appcompat.content.res.AppCompatResources
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.maps.Style
import com.mapbox.maps.StyleObjectInfo
import com.mapbox.maps.extension.style.layers.Layer
import com.mapbox.maps.extension.style.layers.getLayer
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility
import com.mapbox.navigation.base.internal.NativeRouteParserWrapper
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.route.toNavigationRoute
import com.mapbox.navigation.testing.FileUtils
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.BOTTOM_LEVEL_ROUTE_LINE_LAYER_ID
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.CLOSURE_CONGESTION_VALUE
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.HEAVY_CONGESTION_VALUE
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_1_CASING
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_1_MAIN
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_1_RESTRICTED
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_1_SOURCE_ID
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_1_TRAFFIC
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_1_TRAIL
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_1_TRAIL_CASING
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_2_CASING
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_2_MAIN
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_2_RESTRICTED
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_2_SOURCE_ID
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_2_TRAFFIC
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_2_TRAIL
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_2_TRAIL_CASING
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_3_CASING
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_3_MAIN
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_3_RESTRICTED
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_3_SOURCE_ID
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_3_TRAFFIC
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_3_TRAIL
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_3_TRAIL_CASING
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LOW_CONGESTION_VALUE
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.MODERATE_CONGESTION_VALUE
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.RESTRICTED_CONGESTION_VALUE
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.SEVERE_CONGESTION_VALUE
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.TOP_LEVEL_ROUTE_LINE_LAYER_ID
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.UNKNOWN_CONGESTION_VALUE
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.WAYPOINT_SOURCE_ID
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineColorResources
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineExpressionData
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineScaleValue
import com.mapbox.navigation.ui.maps.route.line.model.RouteStyleDescriptor
import com.mapbox.navigation.ui.maps.testing.TestingUtil.loadNavigationRoute
import com.mapbox.navigation.ui.maps.testing.TestingUtil.loadRoute
import com.mapbox.navigator.RouteInterface
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.UUID

class MapboxRouteLineUtilsTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    private val ctx: Context = mockk()

    @Before
    fun setUp() {
        mockkStatic(AppCompatResources::class)
        every { AppCompatResources.getDrawable(any(), any()) } returns mockk(relaxed = true) {
            every { intrinsicWidth } returns 24
            every { intrinsicHeight } returns 24
        }
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
        unmockkObject(NativeRouteParserWrapper)
    }

    @Test
    fun getTrafficLineExpressionTest() {
        val expectedExpression = "[step, [line-progress], [rgba, 0.0, 0.0, 0.0, 0.0], 0.0, " +
            "[rgba, 86.0, 168.0, 251.0, 1.0]]"

        val expressionDatas = listOf(
            RouteLineExpressionData(0.0, -11097861, 0),
            RouteLineExpressionData(0.015670907645820537, -11097861, 0),
            RouteLineExpressionData(0.11898525632162987, -11097861, 0)
        )

        val result = MapboxRouteLineUtils.getTrafficLineExpression(
            0.0,
            Color.TRANSPARENT,
            -11097861,
            expressionDatas
        )

        assertEquals(expectedExpression, result.toString())
    }

    @Test
    fun getTrafficLineExpressionDuplicateOffsetsRemoved() {
        val expectedExpression = "[step, [line-progress], [rgba, 0.0, 0.0, 0.0, 0.0], 0.0, " +
            "[rgba, 86.0, 168.0, 251.0, 1.0], 0.7964017663976524, [rgba, 255.0, 0.0, 0.0, 1.0]]"
        val expressionDatas = listOf(
            RouteLineExpressionData(0.7868200761181402, -11097861, 0),
            RouteLineExpressionData(0.7930120224665551, -11097861, 0),
            RouteLineExpressionData(0.7932530928525063, -11097861, 0),
            RouteLineExpressionData(0.7932530928525063, -11097861, 0),
            RouteLineExpressionData(0.7964017663976524, Color.RED, 0)
        )

        val result = MapboxRouteLineUtils.getTrafficLineExpression(
            0.0,
            Color.TRANSPARENT,
            -11097861,
            expressionDatas
        )

        assertEquals(result.toString(), expectedExpression)
    }

    @Test
    fun getFilteredRouteLineExpressionDataDuplicateOffsetsRemoved() {
        val expressionDatas = listOf(
            RouteLineExpressionData(0.7868200761181402, -11097861, 0),
            RouteLineExpressionData(0.7930120224665551, -11097861, 0),
            RouteLineExpressionData(0.7932530928525063, -11097861, 0),
            RouteLineExpressionData(0.7932530928525063, -11097861, 0),
            RouteLineExpressionData(0.7964017663976524, -11097861, 0)
        )

        val result = MapboxRouteLineUtils.getFilteredRouteLineExpressionData(
            0.0,
            expressionDatas,
            -11097861
        )

        assertEquals(2, expressionDatas.count { it.offset == 0.7932530928525063 })
        assertEquals(1, result.count { it.offset == 0.7932530928525063 })
    }

    @Test
    fun getRestrictedSectionExpressionData() {
        val route = loadRoute("route-with-restrictions.json").toNavigationRoute(
            RouterOrigin.Offboard
        )

        val result = MapboxRouteLineUtils.extractRouteRestrictionData(route)

        assertEquals(5, result.size)
        assertTrue(result[1].isInRestrictedSection)
        assertFalse(result[2].isInRestrictedSection)
        assertTrue(result[3].isInRestrictedSection)
        assertFalse(result[4].isInRestrictedSection)
    }

    @Test
    fun getRestrictedLineExpression() {
        val expectedExpression = "[step, [line-progress], [rgba, 0.0, 0.0, 0.0, 0.0], 0.2, " +
            "[rgba, 0.0, 0.0, 0.0, 0.0], 0.4476941554901612, [rgba, 0.0, 255.0, 255.0, 1.0], " +
            "0.4677574367125704, [rgba, 0.0, 0.0, 0.0, 0.0], 0.5021643413784516, " +
            "[rgba, 0.0, 255.0, 255.0, 1.0], 0.5196445159361185, [rgba, 0.0, 0.0, 0.0, 0.0]]"
        val route = loadRoute("route-with-restrictions.json").toNavigationRoute(
            RouterOrigin.Offboard
        )
        val expData = MapboxRouteLineUtils.extractRouteRestrictionData(route)

        val expression = MapboxRouteLineUtils.getRestrictedLineExpression(
            0.2,
            0,
            Color.CYAN,
            expData
        )

        assertEquals(expectedExpression, expression.toString())
    }

    @Test
    fun getRestrictedLineExpressionProducer() {
        val colorResources = RouteLineColorResources.Builder()
            .restrictedRoadColor(Color.CYAN)
            .build()
        val expectedExpression = "[step, [line-progress], [rgba, 0.0, 0.0, 0.0, 0.0], 0.2, " +
            "[rgba, 0.0, 0.0, 0.0, 0.0], 0.4476941554901612, [rgba, 0.0, 255.0, 255.0, 1.0], " +
            "0.4677574367125704, [rgba, 0.0, 0.0, 0.0, 0.0], 0.5021643413784516, " +
            "[rgba, 0.0, 255.0, 255.0, 1.0], 0.5196445159361185, [rgba, 0.0, 0.0, 0.0, 0.0]]"
        val route = loadRoute("route-with-restrictions.json").toNavigationRoute(
            RouterOrigin.Offboard
        )
        val expData = MapboxRouteLineUtils.extractRouteRestrictionData(route)

        val expression = MapboxRouteLineUtils.getRestrictedLineExpressionProducer(
            expData,
            0.2,
            0,
            colorResources
        ).generateExpression()

        assertEquals(expectedExpression, expression.toString())
    }

    @Test
    fun getDisabledRestrictedLineExpressionProducer() {
        val expectedExpression = "[step, [line-progress], [rgba, 0.0, 0.0, 0.0, 0.0], " +
            "0.0, [rgba, 0.0, 0.0, 0.0, 0.0]]"

        val expression = MapboxRouteLineUtils.getDisabledRestrictedLineExpressionProducer(
            0.0,
            0,
            1
        ).generateExpression()

        assertEquals(expectedExpression, expression.toString())
    }

    @Test
    fun getRestrictedLineExpression_whenNoRestrictionsInRoute() {
        val expectedExpression = "[step, [line-progress], [rgba, 0.0, 0.0, 0.0, 0.0], 0.2, " +
            "[rgba, 0.0, 0.0, 0.0, 0.0]]"
        val route = loadRoute("short_route.json").toNavigationRoute(
            RouterOrigin.Offboard
        )
        val expData = MapboxRouteLineUtils.extractRouteRestrictionData(route)

        val expression = MapboxRouteLineUtils.getRestrictedLineExpression(
            0.2,
            0,
            -1,
            expData
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
            Point.fromLngLat(-122.52509389295653, 37.974569579999944)
        )

        assertEquals(0.0000017145850113848236, result, 0.0)
    }

    @Test
    fun layersAreInitialized() {
        val options = mockk<MapboxRouteLineOptions> {
            every { displayRestrictedRoadSections } returns true
        }
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

            every {
                styleLayerExists(TOP_LEVEL_ROUTE_LINE_LAYER_ID)
            } returns true
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
    }

    @Test
    fun `layersAreInitialized without restricted roads`() {
        val options = mockk<MapboxRouteLineOptions> {
            every { displayRestrictedRoadSections } returns false
        }
        val style = mockk<Style> {
            every { styleSourceExists(LAYER_GROUP_1_SOURCE_ID) } returns true
            every { styleSourceExists(LAYER_GROUP_2_SOURCE_ID) } returns true
            every { styleSourceExists(LAYER_GROUP_3_SOURCE_ID) } returns true
            every { styleLayerExists(LAYER_GROUP_1_TRAIL_CASING) } returns true
            every { styleLayerExists(LAYER_GROUP_1_TRAIL) } returns true
            every { styleLayerExists(LAYER_GROUP_1_CASING) } returns true
            every { styleLayerExists(LAYER_GROUP_1_MAIN) } returns true
            every { styleLayerExists(LAYER_GROUP_1_TRAFFIC) } returns true
            every { styleLayerExists(LAYER_GROUP_1_RESTRICTED) } returns false
            every { styleLayerExists(LAYER_GROUP_2_TRAIL_CASING) } returns true
            every { styleLayerExists(LAYER_GROUP_2_TRAIL) } returns true
            every { styleLayerExists(LAYER_GROUP_2_CASING) } returns true
            every { styleLayerExists(LAYER_GROUP_2_MAIN) } returns true
            every { styleLayerExists(LAYER_GROUP_2_TRAFFIC) } returns true
            every { styleLayerExists(LAYER_GROUP_2_RESTRICTED) } returns false
            every { styleLayerExists(LAYER_GROUP_3_TRAIL_CASING) } returns true
            every { styleLayerExists(LAYER_GROUP_3_TRAIL) } returns true
            every { styleLayerExists(LAYER_GROUP_3_CASING) } returns true
            every { styleLayerExists(LAYER_GROUP_3_MAIN) } returns true
            every { styleLayerExists(LAYER_GROUP_3_TRAFFIC) } returns true
            every { styleLayerExists(LAYER_GROUP_3_RESTRICTED) } returns false
            every { styleLayerExists(TOP_LEVEL_ROUTE_LINE_LAYER_ID) } returns true
            every { styleLayerExists(BOTTOM_LEVEL_ROUTE_LINE_LAYER_ID) } returns true
            every { styleSourceExists(WAYPOINT_SOURCE_ID) } returns true
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
    fun initializeLayers_whenLayersAreInitialized() {
        val options = MapboxRouteLineOptions.Builder(ctx).build()
        val style = mockk<Style> {
            every { styleLayers } returns listOf()
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
            every { styleLayerExists(BOTTOM_LEVEL_ROUTE_LINE_LAYER_ID) } returns true
            every {
                styleLayerExists(TOP_LEVEL_ROUTE_LINE_LAYER_ID)
            } returns true
            every { styleSourceExists(WAYPOINT_SOURCE_ID) } returns false
        }

        MapboxRouteLineUtils.initializeLayers(style, options)

        verify(exactly = 0) { style.styleLayers }
        verify(exactly = 0) { style.addStyleSource(any(), any()) }
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
            RouteLineScaleValue(22f, 18f, 1f)
        )

        val result = MapboxRouteLineUtils.buildScalingExpression(values)

        assertEquals(expectedExpression, result.toString())
    }

    @Test
    fun getRouteLineTrafficExpressionDataWhenUniqueStreetClassDataExists() {
        val routeAsJsonJson = FileUtils.loadJsonFixture("route-unique-road-classes.json")
        val route = DirectionsRoute.fromJson(routeAsJsonJson)
        val distances = route.legs()!!.mapNotNull { it.annotation()!!.distance() }.flatten()
        val distancesSum = distances.subList(0, distances.lastIndex).sum()
        val roadClasses = route.legs()?.asSequence()
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
            MapboxRouteLineUtils.getRouteLegTrafficCongestionProvider
        )

        assertEquals(distances.size, result.size)
        assertEquals(distances.first(), result[1].distanceFromOrigin, 0.0)
        assertEquals(result[0].roadClass, roadClasses!!.first())
        assertEquals(result[2].distanceFromOrigin, distances.subList(0, 2).sum(), 0.0)
        assertEquals(distancesSum, result.last().distanceFromOrigin, 0.0)
        assertEquals(LOW_CONGESTION_VALUE, result.last().trafficCongestionIdentifier)
        assertEquals("service", result.last().roadClass)
    }

    // The route used here for testing produced an erroneous edge case. The
    // getRouteLineTrafficExpressionData method was always producing a distanceFromOrigin value
    // of 0.0 for the beginning of each route leg. This could cause an error when creating the
    // traffic expression because the distanceFromOrigin value is used to determine the
    // percentage of distance traveled. These values need to be in ascending order to create a
    // valid line gradient expression. This error won't occur in single leg routes and will
    // only occur in multileg routes when there is a traffic congestion change at the first point in
    // the leg. This is because duplicate traffic congestion values are dropped. The route
    // used in the test below has a traffic change at the first point in the second leg and
    // the distance annotation is 0.0 which would have caused an error prior to the fix this
    // test is checking for.
    @Test
    fun getRouteLineTrafficExpressionDataMultiLegRouteWithTrafficChangeAtWaypoint() {
        val route = loadRoute("multileg-route-two-legs.json")

        val trafficExpressionData = MapboxRouteLineUtils.extractRouteData(
            route,
            MapboxRouteLineUtils.getRouteLegTrafficCongestionProvider
        )

        assertEquals(1, trafficExpressionData.count { it.distanceFromOrigin == 0.0 })
        assertEquals(0.0, trafficExpressionData[0].distanceFromOrigin, 0.0)
    }

    @Test
    fun getRouteLineTrafficExpressionWithRoadClassesDuplicatesRemoved() {
        val routeAsJsonJson = FileUtils.loadJsonFixture("route-with-road-classes.txt")
        val route = DirectionsRoute.fromJson(routeAsJsonJson)

        val result = MapboxRouteLineUtils.extractRouteDataWithTrafficAndRoadClassDeDuped(
            route,
            MapboxRouteLineUtils.getRouteLegTrafficCongestionProvider
        )

        assertEquals(10, result.size)
        assertEquals(1300.0000000000002, result.last().distanceFromOrigin, 0.0)
        assertEquals(LOW_CONGESTION_VALUE, result.last().trafficCongestionIdentifier)
        assertEquals("service", result.last().roadClass)
    }

    @Test
    fun getRouteLineTrafficExpressionDataWithSomeRoadClassesDuplicatesRemoved() {
        val routeAsJsonJson =
            FileUtils.loadJsonFixture("motorway-route-with-road-classes-mixed.json")
        val route = DirectionsRoute.fromJson(routeAsJsonJson)

        val result = MapboxRouteLineUtils.extractRouteDataWithTrafficAndRoadClassDeDuped(
            route,
            MapboxRouteLineUtils.getRouteLegTrafficCongestionProvider
        )

        assertEquals(5, result.size)
        assertEquals(0.0, result[0].distanceFromOrigin, 0.0)
        assertEquals("unknown", result[0].trafficCongestionIdentifier)
        assertEquals("motorway", result[0].roadClass)
        assertEquals(3.7, result[1].distanceFromOrigin, 0.0)
        assertEquals("severe", result[1].trafficCongestionIdentifier)
        assertEquals("motorway", result[1].roadClass)
        assertEquals(27.5, result[2].distanceFromOrigin, 0.0)
        assertEquals("unknown", result[2].trafficCongestionIdentifier)
        assertEquals("motorway", result[2].roadClass)
        assertEquals(39.9, result[3].distanceFromOrigin, 0.0)
        assertEquals("severe", result[3].trafficCongestionIdentifier)
        assertEquals("motorway", result[3].roadClass)
        assertEquals(99.6, result[4].distanceFromOrigin, 0.0)
        assertEquals("unknown", result[4].trafficCongestionIdentifier)
        assertEquals("motorway", result[4].roadClass)
    }

    @Test
    fun getRouteLineExpressionDataWithStreetClassOverrideWhenHasStreetClassesOnMotorway() {
        val colorResources = RouteLineColorResources.Builder()
            .routeUnknownCongestionColor(-9)
            .routeLowCongestionColor(-1)
            .routeCasingColor(33)
            .routeDefaultColor(33)
            .routeHeavyCongestionColor(33)
            .routeLineTraveledCasingColor(33)
            .routeLineTraveledColor(33)
            .routeModerateCongestionColor(33)
            .routeSevereCongestionColor(33)
            .build()

        val routeAsJsonJson = FileUtils.loadJsonFixture("motorway-route-with-road-classes.json")
        val route = DirectionsRoute.fromJson(routeAsJsonJson)
        val trafficExpressionData =
            MapboxRouteLineUtils.extractRouteDataWithTrafficAndRoadClassDeDuped(
                route,
                MapboxRouteLineUtils.getRouteLegTrafficCongestionProvider
            )

        val result = MapboxRouteLineUtils.getRouteLineExpressionDataWithStreetClassOverride(
            trafficExpressionData,
            route.distance(),
            colorResources,
            true,
            listOf("motorway")
        )

        assertTrue(result.all { it.segmentColor == -1 })
        assertEquals(1, result.size)
    }

    @Test
    fun getRouteLineExpressionDataWithSomeRoadClassesDuplicatesRemoved() {
        val colorResources = RouteLineColorResources.Builder()
            .routeUnknownCongestionColor(-1)
            .routeLowCongestionColor(-1)
            .routeCasingColor(33)
            .routeDefaultColor(33)
            .routeHeavyCongestionColor(33)
            .routeLineTraveledCasingColor(33)
            .routeLineTraveledColor(33)
            .routeModerateCongestionColor(33)
            .routeSevereCongestionColor(33)
            .build()

        val routeAsJsonJson =
            FileUtils.loadJsonFixture("motorway-route-with-road-classes-mixed.json")
        val route = DirectionsRoute.fromJson(routeAsJsonJson)
        val trafficExpressionData =
            MapboxRouteLineUtils.extractRouteDataWithTrafficAndRoadClassDeDuped(
                route,
                MapboxRouteLineUtils.getRouteLegTrafficCongestionProvider
            )

        val result = MapboxRouteLineUtils.getRouteLineExpressionDataWithStreetClassOverride(
            trafficExpressionData,
            route.distance(),
            colorResources,
            true,
            listOf("motorway")
        )

        assertEquals(5, result.size)
        assertEquals(0.0, result[0].offset, 0.0)
        assertEquals(-1, result[0].segmentColor)
        assertEquals(0.002337691548550063, result[1].offset, 0.0)
        assertEquals(33, result[1].segmentColor)
        assertEquals(0.01737473448246668, result[2].offset, 0.0)
        assertEquals(-1, result[2].segmentColor)
        assertEquals(0.025209160212742564, result[3].offset, 0.0)
        assertEquals(33, result[3].segmentColor)
        assertEquals(0.06292812925286113, result[4].offset, 0.0)
        assertEquals(-1, result[4].segmentColor)
    }

    @Test
    fun getRouteLineExpressionDataWithStreetClassOverrideWhenHasStreetClassesAndClosures() {
        val colorResources = RouteLineColorResources.Builder()
            .routeUnknownCongestionColor(-9)
            .routeLowCongestionColor(-1)
            .routeCasingColor(33)
            .routeDefaultColor(33)
            .routeHeavyCongestionColor(33)
            .routeLineTraveledCasingColor(33)
            .routeLineTraveledColor(33)
            .routeModerateCongestionColor(33)
            .routeSevereCongestionColor(99)
            .routeClosureColor(-21)
            .build()

        val routeAsJsonJson = FileUtils.loadJsonFixture("route-with-closure.json")
        val route = DirectionsRoute.fromJson(routeAsJsonJson)
        val trafficExpressionData =
            MapboxRouteLineUtils.extractRouteDataWithTrafficAndRoadClassDeDuped(
                route,
                MapboxRouteLineUtils.getRouteLegTrafficCongestionProvider
            )

        val result = MapboxRouteLineUtils.getRouteLineExpressionDataWithStreetClassOverride(
            trafficExpressionData,
            route.distance(),
            colorResources,
            true,
            listOf("tertiary")
        )

        assertEquals(0.0, result[0].offset, 0.0)
        assertEquals(-1, result[0].segmentColor)
        assertEquals(0.5467690917306824, result[1].offset, 0.0)
        assertEquals(-21, result[1].segmentColor)
        assertEquals(0.8698599186624492, result[2].offset, 0.0)
        assertEquals(99, result[2].segmentColor)
    }

    @Test
    fun getRouteLineTrafficExpressionDataWithOutStreetClassesDuplicatesRemoved() {
        val routeAsJsonJson = FileUtils.loadJsonFixture("route-with-traffic-no-street-classes.txt")
        val route = DirectionsRoute.fromJson(routeAsJsonJson)

        val result = MapboxRouteLineUtils.extractRouteDataWithTrafficAndRoadClassDeDuped(
            route,
            MapboxRouteLineUtils.getRouteLegTrafficCongestionProvider
        )

        assertEquals(5, result.size)
        assertEquals(1188.7000000000003, result.last().distanceFromOrigin, 0.0)
        assertEquals(LOW_CONGESTION_VALUE, result.last().trafficCongestionIdentifier)
        assertNull(result.last().roadClass)
    }

    @Test
    fun getRouteLineTrafficExpressionDataWithStreetClassesDuplicatesRemoved() {
        val colorResources = RouteLineColorResources.Builder()
            .routeUnknownCongestionColor(-9)
            .routeLowCongestionColor(-1)
            .routeCasingColor(33)
            .routeDefaultColor(33)
            .routeHeavyCongestionColor(33)
            .routeLineTraveledCasingColor(33)
            .routeLineTraveledColor(33)
            .routeModerateCongestionColor(33)
            .routeSevereCongestionColor(33)
            .build()

        val routeAsJsonJson = FileUtils.loadJsonFixture("route-with-road-classes.txt")
        val route = DirectionsRoute.fromJson(routeAsJsonJson)
        val trafficExpressionData =
            MapboxRouteLineUtils.extractRouteDataWithTrafficAndRoadClassDeDuped(
                route,
                MapboxRouteLineUtils.getRouteLegTrafficCongestionProvider
            )

        assertEquals("service", trafficExpressionData[0].roadClass)
        assertEquals("street", trafficExpressionData[1].roadClass)
        assertEquals(
            UNKNOWN_CONGESTION_VALUE,
            trafficExpressionData[0].trafficCongestionIdentifier
        )
        assertEquals(
            UNKNOWN_CONGESTION_VALUE,
            trafficExpressionData[1].trafficCongestionIdentifier
        )

        val result = MapboxRouteLineUtils.getRouteLineExpressionDataWithStreetClassOverride(
            trafficExpressionData,
            route.distance(),
            colorResources,
            true,
            listOf("street")
        )

        assertEquals(-9, result[0].segmentColor)
        assertEquals(7, result.size)
        assertEquals(0.016404052025563352, result[1].offset, 0.0)
        assertEquals(-1, result[1].segmentColor)
    }

    @Test
    fun getRouteLineExpressionDataWithStreetClassOverrideWhenDoesNotHaveStreetClasses() {
        val colorResources = RouteLineColorResources.Builder()
            .routeUnknownCongestionColor(-9)
            .routeLowCongestionColor(-1)
            .routeCasingColor(33)
            .routeDefaultColor(33)
            .routeHeavyCongestionColor(33)
            .routeLineTraveledCasingColor(33)
            .routeLineTraveledColor(33)
            .routeModerateCongestionColor(33)
            .routeSevereCongestionColor(33)
            .build()
        val routeAsJsonJson = FileUtils.loadJsonFixture("route-with-traffic-no-street-classes.txt")
        val route = DirectionsRoute.fromJson(routeAsJsonJson)
        val trafficExpressionData =
            MapboxRouteLineUtils.extractRouteDataWithTrafficAndRoadClassDeDuped(
                route,
                MapboxRouteLineUtils.getRouteLegTrafficCongestionProvider
            )

        val result = MapboxRouteLineUtils.getRouteLineExpressionDataWithStreetClassOverride(
            trafficExpressionData,
            route.distance(),
            colorResources,
            true,
            listOf()
        )

        assertEquals(5, result.size)
        assertEquals(0.23460041526970057, result[1].offset, 0.0)
        assertEquals(-1, result[1].segmentColor)
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

        val routeAsJsonJson = FileUtils.loadJsonFixture(
            "motorway-route-with-road-classes-unknown-not-on-intersection.json"
        )
        val route = DirectionsRoute.fromJson(routeAsJsonJson)

        val trafficExpressionData =
            MapboxRouteLineUtils.extractRouteDataWithTrafficAndRoadClassDeDuped(
                route,
                MapboxRouteLineUtils.getRouteLegTrafficCongestionProvider
            )

        val result = MapboxRouteLineUtils.getRouteLineExpressionDataWithStreetClassOverride(
            trafficExpressionData,
            route.distance(),
            colorResources,
            true,
            listOf("motorway")
        )

        assertEquals(-2, result[0].segmentColor)
        assertNotEquals(-9, result[1].segmentColor)
        assertEquals(-1, result[1].segmentColor)
        assertEquals(-2, result[2].segmentColor)
    }

    @Test
    fun getRouteLineTrafficExpressionDataMissingRoadClass() {
        val routeAsJsonJson = FileUtils.loadJsonFixture(
            "route-with-missing-road-classes.json"
        )
        val route = DirectionsRoute.fromJson(routeAsJsonJson)

        val result = MapboxRouteLineUtils.extractRouteDataWithTrafficAndRoadClassDeDuped(
            route,
            MapboxRouteLineUtils.getRouteLegTrafficCongestionProvider
        )

        assertEquals(7, result.size)
        assertEquals(0.0, result[0].distanceFromOrigin, 0.0)
        assertEquals("severe", result[0].trafficCongestionIdentifier)
        assertEquals("motorway", result[0].roadClass)
        assertEquals(3.7, result[1].distanceFromOrigin, 0.0)
        assertEquals("unknown", result[1].trafficCongestionIdentifier)
        assertEquals("motorway", result[1].roadClass)
        assertEquals(27.5, result[2].distanceFromOrigin, 0.0)
        assertEquals("severe", result[2].trafficCongestionIdentifier)
        assertEquals("motorway", result[2].roadClass)
        assertEquals(271.8, result[3].distanceFromOrigin, 0.0)
        assertEquals("severe", result[3].trafficCongestionIdentifier)
        assertEquals("intersection_without_class_fallback", result[3].roadClass)
        assertEquals(305.2, result[4].distanceFromOrigin, 0.0)
        assertEquals("severe", result[4].trafficCongestionIdentifier)
        assertEquals("motorway", result[4].roadClass)
        assertEquals(545.6, result[5].distanceFromOrigin, 0.0)
        assertEquals("severe", result[5].trafficCongestionIdentifier)
        assertEquals("intersection_without_class_fallback", result[5].roadClass)
        assertEquals(1168.3000000000002, result[6].distanceFromOrigin, 0.0)
        assertEquals("severe", result[6].trafficCongestionIdentifier)
        assertEquals("motorway", result[6].roadClass)
    }

    @Test
    fun getRouteLineExpressionDataWithStreetClassOverrideWhenHasStreetClassesOnMotorwayMultiLeg() {
        // test case for overlapping geometry indices across multiple legs
        val colorResources = RouteLineColorResources.Builder()
            .routeUnknownCongestionColor(-9)
            .routeLowCongestionColor(-1)
            .routeCasingColor(33)
            .routeDefaultColor(33)
            .routeHeavyCongestionColor(33)
            .routeLineTraveledCasingColor(33)
            .routeLineTraveledColor(33)
            .routeModerateCongestionColor(33)
            .routeSevereCongestionColor(33)
            .build()
        val routeAsJsonJson = FileUtils.loadJsonFixture(
            "motorway-with-road-classes-multi-leg.json"
        )
        val route = DirectionsRoute.fromJson(routeAsJsonJson)

        val trafficExpressionData =
            MapboxRouteLineUtils.extractRouteDataWithTrafficAndRoadClassDeDuped(
                route,
                MapboxRouteLineUtils.getRouteLegTrafficCongestionProvider
            )

        val result = MapboxRouteLineUtils.getRouteLineExpressionDataWithStreetClassOverride(
            trafficExpressionData,
            route.distance(),
            colorResources,
            true,
            listOf("motorway")
        )

        assertTrue(result.all { it.segmentColor == -1 })
        assertEquals(2, result.size)
    }

    @Test
    fun getRouteLineTrafficExpressionDataWithClosures() {
        val routeAsJsonJson = FileUtils.loadJsonFixture("route-with-closure.json")
        val route = DirectionsRoute.fromJson(routeAsJsonJson)

        val trafficExpressionData =
            MapboxRouteLineUtils.extractRouteDataWithTrafficAndRoadClassDeDuped(
                route,
                MapboxRouteLineUtils.getRouteLegTrafficCongestionProvider
            )

        assertEquals(0.0, trafficExpressionData[0].distanceFromOrigin, 0.0)
        assertEquals("low", trafficExpressionData[0].trafficCongestionIdentifier)
        assertEquals(145.20000000000002, trafficExpressionData[1].distanceFromOrigin, 0.0)
        assertEquals("closed", trafficExpressionData[1].trafficCongestionIdentifier)
        assertEquals(231.0, trafficExpressionData[2].distanceFromOrigin, 0.0)
        assertEquals("severe", trafficExpressionData[2].trafficCongestionIdentifier)
    }

    @Test
    fun getRouteLineTrafficExpressionDataWithRestrictedSections() {
        val route = loadRoute("route-with-restrictions.json").toNavigationRoute(
            RouterOrigin.Offboard
        )

        val trafficExpressionData = MapboxRouteLineUtils.extractRouteRestrictionData(route)

        assertEquals(0.0, trafficExpressionData[0].offset, 0.0)
        assertFalse(trafficExpressionData[0].isInRestrictedSection)
        assertEquals(0, trafficExpressionData[0].legIndex)

        assertEquals(0, trafficExpressionData[1].legIndex)
        assertTrue(trafficExpressionData[1].isInRestrictedSection)
        assertEquals(0.4476941554901612, trafficExpressionData[1].offset, 0.0)

        assertEquals(0, trafficExpressionData[2].legIndex)
        assertFalse(trafficExpressionData[2].isInRestrictedSection)
        assertEquals(0.4677574367125704, trafficExpressionData[2].offset, 0.0)

        assertEquals(0, trafficExpressionData[3].legIndex)
        assertTrue(trafficExpressionData[3].isInRestrictedSection)
        assertEquals(0.5021643413784516, trafficExpressionData[3].offset, 0.0)

        assertEquals(0, trafficExpressionData[4].legIndex)
        assertFalse(trafficExpressionData[4].isInRestrictedSection)
        assertEquals(0.5196445159361185, trafficExpressionData[4].offset, 0.0)

        assertEquals(5, trafficExpressionData.size)
    }

    @Test
    fun getRouteLineTrafficExpressionData_whenFirstDistanceInSecondLegIsZero() {
        val route = loadRoute("multileg-route-two-legs.json")

        val result = MapboxRouteLineUtils.extractRouteDataWithTrafficAndRoadClassDeDuped(
            route,
            MapboxRouteLineUtils.getRouteLegTrafficCongestionProvider
        )

        assertEquals(19, result.size)
        assertEquals(478.70000000000005, result[7].distanceFromOrigin, 0.0)
        assertTrue(result[7].isLegOrigin)
        assertEquals(529.9000000000001, result[8].distanceFromOrigin, 0.0)
        assertFalse(result[8].isLegOrigin)
    }

    @Test
    fun `extractRouteData with multiple distance entries of zero`() {
        val route = loadRoute("artificially_wrong_two_leg_route.json")
        val comparisonRoute = loadRoute("multileg-route-two-legs.json")
        val expectedResult = MapboxRouteLineUtils.extractRouteData(
            comparisonRoute,
            MapboxRouteLineUtils.getRouteLegTrafficCongestionProvider
        )

        val result = MapboxRouteLineUtils.extractRouteData(
            route,
            MapboxRouteLineUtils.getRouteLegTrafficCongestionProvider
        )

        // In this case the two routes being used are the same except for an additional
        // distance value of 0 being added to test the implementation. The result of the
        // calls should be the same to prove that distance values of 0 in the route
        // are ignored.
        val listItemsAreEqual = listElementsAreEqual(expectedResult, result) { item1, item2 ->
            compareValuesBy(
                item1,
                item2,
                { it.distanceFromOrigin },
                { it.isLegOrigin },
                { it.offset },
                { it.trafficCongestionIdentifier }
            ) == 0
        }
        assertTrue(listItemsAreEqual)
        assertFalse(result[16].isLegOrigin)
        assertTrue(result[17].isLegOrigin)
        assertFalse(result[18].isLegOrigin)
        assertEquals(478.70000000000005, result[17].distanceFromOrigin, 0.0)
    }

    @Test
    fun calculateRouteLineSegmentsMultilegRoute() {
        val colorResources = RouteLineColorResources.Builder()
            .routeUnknownCongestionColor(-9)
            .routeLowCongestionColor(-1)
            .routeCasingColor(33)
            .routeDefaultColor(33)
            .routeHeavyCongestionColor(33)
            .routeLineTraveledCasingColor(33)
            .routeLineTraveledColor(33)
            .routeModerateCongestionColor(33)
            .routeSevereCongestionColor(33)
            .build()
        val route = loadRoute("multileg_route.json")

        val result = MapboxRouteLineUtils.calculateRouteLineSegments(
            route,
            listOf(),
            true,
            colorResources
        )

        assertEquals(20, result.size)
        assertEquals(0.039793906743275334, result[1].offset, 0.0)
        assertEquals(0.989831291992653, result.last().offset, 0.0)
    }

    @Test
    fun calculateRouteLineSegmentsMultilegRouteFirstDistanceValueAboveMinimumOffset() {
        val colorResources = RouteLineColorResources.Builder()
            .routeUnknownCongestionColor(-9)
            .routeLowCongestionColor(-1)
            .routeCasingColor(33)
            .routeDefaultColor(33)
            .routeHeavyCongestionColor(33)
            .routeLineTraveledCasingColor(33)
            .routeLineTraveledColor(33)
            .routeModerateCongestionColor(33)
            .routeSevereCongestionColor(33)
            .build()
        val route = loadRoute("multileg_route.json")

        val result = MapboxRouteLineUtils.calculateRouteLineSegments(
            route,
            listOf(),
            true,
            colorResources
        )

        assertTrue(result[1].offset > .001f)
    }

    @Test
    fun calculateRouteLineSegments_whenNoTrafficExpressionData() {
        val colorResources = RouteLineColorResources.Builder().build()
        val routeOptions = mockk<RouteOptions>(relaxed = true) {
            every {
                annotationsList()
            } returns listOf(DirectionsCriteria.ANNOTATION_CONGESTION_NUMERIC)
        }
        val route = mockk<DirectionsRoute> {
            every { legs() } returns listOf()
            every { routeOptions() } returns routeOptions
        }

        val result = MapboxRouteLineUtils.calculateRouteLineSegments(
            route,
            listOf(),
            true,
            colorResources
        )

        assertEquals(1, result.size)
        assertEquals(0.0, result[0].offset, 0.0)
        assertEquals(colorResources.routeDefaultColor, result[0].segmentColor)
    }

    @Test
    fun getRouteColorForCongestionPrimaryRouteCongestionLow() {
        val resources = RouteLineColorResources.Builder().build()

        val result = MapboxRouteLineUtils.getRouteColorForCongestion(
            LOW_CONGESTION_VALUE,
            true,
            resources
        )

        assertEquals(resources.routeLowCongestionColor, result)
    }

    @Test
    fun getRouteColorForCongestionPrimaryRouteCongestionModerate() {
        val resources = RouteLineColorResources.Builder().build()

        val result = MapboxRouteLineUtils.getRouteColorForCongestion(
            MODERATE_CONGESTION_VALUE,
            true,
            resources
        )

        assertEquals(resources.routeModerateCongestionColor, result)
    }

    @Test
    fun getRouteColorForCongestionPrimaryRouteCongestionHeavy() {
        val resources = RouteLineColorResources.Builder().build()

        val result =
            MapboxRouteLineUtils.getRouteColorForCongestion(
                HEAVY_CONGESTION_VALUE,
                true,
                resources
            )

        assertEquals(resources.routeHeavyCongestionColor, result)
    }

    @Test
    fun getRouteColorForCongestionPrimaryRouteCongestionSevere() {
        val resources = RouteLineColorResources.Builder().build()

        val result =
            MapboxRouteLineUtils.getRouteColorForCongestion(
                SEVERE_CONGESTION_VALUE,
                true,
                resources
            )

        assertEquals(resources.routeSevereCongestionColor, result)
    }

    @Test
    fun getRouteColorForCongestionPrimaryRouteCongestionUnknown() {
        val resources = RouteLineColorResources.Builder().build()

        val result =
            MapboxRouteLineUtils.getRouteColorForCongestion(
                UNKNOWN_CONGESTION_VALUE,
                true,
                resources
            )

        assertEquals(resources.routeUnknownCongestionColor, result)
    }

    @Test
    fun getRouteColorForCongestionPrimaryRouteCongestionDefault() {
        val resources = RouteLineColorResources.Builder().build()

        val result = MapboxRouteLineUtils.getRouteColorForCongestion(
            "foobar",
            true,
            resources
        )

        assertEquals(resources.routeDefaultColor, result)
    }

    @Test
    fun getRouteColorForCongestionPrimaryRouteCongestionClosure() {
        val resources = RouteLineColorResources.Builder().build()

        val result = MapboxRouteLineUtils.getRouteColorForCongestion(
            CLOSURE_CONGESTION_VALUE,
            true,
            resources
        )

        assertEquals(resources.routeClosureColor, result)
    }

    @Test
    fun getRouteColorForCongestionPrimaryRouteCongestionRestricted() {
        val resources = RouteLineColorResources.Builder().build()

        val result = MapboxRouteLineUtils.getRouteColorForCongestion(
            RESTRICTED_CONGESTION_VALUE,
            true,
            resources
        )

        assertEquals(resources.restrictedRoadColor, result)
    }

    @Test
    fun getRouteColorForCongestionNonPrimaryRouteCongestionLow() {
        val resources = RouteLineColorResources.Builder().build()

        val result =
            MapboxRouteLineUtils.getRouteColorForCongestion(
                LOW_CONGESTION_VALUE,
                false,
                resources
            )

        assertEquals(resources.alternativeRouteLowCongestionColor, result)
    }

    @Test
    fun getRouteColorForCongestionNonPrimaryRouteCongestionModerate() {
        val resources = RouteLineColorResources.Builder().build()

        val result =
            MapboxRouteLineUtils.getRouteColorForCongestion(
                MODERATE_CONGESTION_VALUE,
                false,
                resources
            )

        assertEquals(resources.alternativeRouteModerateCongestionColor, result)
    }

    @Test
    fun getRouteColorForCongestionNonPrimaryRouteCongestionHeavy() {
        val resources = RouteLineColorResources.Builder().build()

        val result =
            MapboxRouteLineUtils.getRouteColorForCongestion(
                HEAVY_CONGESTION_VALUE,
                false,
                resources
            )

        assertEquals(resources.alternativeRouteHeavyCongestionColor, result)
    }

    @Test
    fun getRouteColorForCongestionNonPrimaryRouteCongestionSevere() {
        val resources = RouteLineColorResources.Builder().build()

        val result =
            MapboxRouteLineUtils.getRouteColorForCongestion(
                SEVERE_CONGESTION_VALUE,
                false,
                resources
            )

        assertEquals(resources.alternativeRouteSevereCongestionColor, result)
    }

    @Test
    fun getRouteColorForCongestionNonPrimaryRouteCongestionUnknown() {
        val resources = RouteLineColorResources.Builder().build()

        val result =
            MapboxRouteLineUtils.getRouteColorForCongestion(
                UNKNOWN_CONGESTION_VALUE,
                false,
                resources
            )

        assertEquals(resources.alternativeRouteUnknownCongestionColor, result)
    }

    @Test
    fun getRouteColorForCongestionNonPrimaryRouteCongestionDefault() {
        val resources = RouteLineColorResources.Builder().build()

        val result = MapboxRouteLineUtils.getRouteColorForCongestion(
            "foobar",
            false,
            resources
        )

        assertEquals(resources.alternativeRouteDefaultColor, result)
    }

    @Test
    fun getRouteColorForCongestionNonPrimaryRouteCongestionRestricted() {
        val resources = RouteLineColorResources.Builder().build()

        val result = MapboxRouteLineUtils.getRouteColorForCongestion(
            RESTRICTED_CONGESTION_VALUE,
            false,
            resources
        )

        assertEquals(resources.alternativeRouteRestrictedRoadColor, result)
    }

    @Test
    fun getRouteColorForCongestionNonPrimaryRouteCongestionClosure() {
        val expectedColor = Color.parseColor("#ffcc00")
        val resources = RouteLineColorResources.Builder()
            .alternativeRouteClosureColor(expectedColor)
            .build()

        val result = MapboxRouteLineUtils.getRouteColorForCongestion(
            CLOSURE_CONGESTION_VALUE,
            false,
            resources
        )

        assertEquals(resources.alternativeRouteClosureColor, result)
    }

    @Test
    fun buildWayPointFeatureCollection() {
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

        val route = loadRoute("multileg_route.json").toNavigationRoute()

        val result = MapboxRouteLineUtils.buildWayPointFeatureCollection(route)

        assertEquals(3, result.features()!!.size)
        assertEquals(
            Point.fromLngLat(-77.157347, 38.783004),
            result.features()!![0].geometry() as Point
        )
        assertEquals(
            Point.fromLngLat(-77.167276, 38.775717),
            result.features()!![1].geometry() as Point
        )
        assertEquals(
            Point.fromLngLat(-77.153468, 38.77091),
            result.features()!![2].geometry() as Point
        )
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
    fun parseRoutePoints() {
        val route = loadNavigationRoute("multileg_route.json")

        val result = MapboxRouteLineUtils.routePointsProvider(route)!!

        assertEquals(128, result.flatList.size)
        assertEquals(15, result.stepPoints.flatten().size)
        assertEquals(result.flatList[1].latitude(), result.flatList[2].latitude(), 0.0)
        assertEquals(result.flatList[1].longitude(), result.flatList[2].longitude(), 0.0)
        assertEquals(result.flatList[126].latitude(), result.flatList[127].latitude(), 0.0)
        assertEquals(result.flatList[126].longitude(), result.flatList[127].longitude(), 0.0)
    }

    @Test
    fun whenAnnotationIsCongestionNumericThenResolveLowCongestionNumeric() {
        val lowCongestionNumeric = 4
        val congestionResource = RouteLineColorResources.Builder().build()

        val result = MapboxRouteLineUtils.resolveNumericToValue(
            lowCongestionNumeric,
            congestionResource
        )

        assertEquals(LOW_CONGESTION_VALUE, result)
    }

    @Test
    fun whenAnnotationIsCongestionNumericThenResolveModerateCongestionNumeric() {
        val moderateCongestionNumeric = 45
        val congestionResource = RouteLineColorResources.Builder().build()

        val result = MapboxRouteLineUtils.resolveNumericToValue(
            moderateCongestionNumeric,
            congestionResource
        )

        assertEquals(MODERATE_CONGESTION_VALUE, result)
    }

    @Test
    fun whenAnnotationIsCongestionNumericThenResolveHeavyCongestionNumeric() {
        val heavyCongestionNumeric = 65
        val congestionResource = RouteLineColorResources.Builder().build()

        val result = MapboxRouteLineUtils.resolveNumericToValue(
            heavyCongestionNumeric,
            congestionResource
        )

        assertEquals(HEAVY_CONGESTION_VALUE, result)
    }

    @Test
    fun whenAnnotationIsCongestionNumericThenResolveSevereCongestionNumeric() {
        val severeCongestionNumeric = 85
        val congestionResource = RouteLineColorResources.Builder().build()

        val result = MapboxRouteLineUtils.resolveNumericToValue(
            severeCongestionNumeric,
            congestionResource
        )

        assertEquals(SEVERE_CONGESTION_VALUE, result)
    }

    @Test
    fun whenAnnotationIsCongestionNumericThenResolveUnknownCongestionNumeric() {
        val unknownCongestionNumeric = null
        val congestionResource = RouteLineColorResources.Builder().build()

        val result = MapboxRouteLineUtils.resolveNumericToValue(
            unknownCongestionNumeric,
            congestionResource
        )

        assertEquals(UNKNOWN_CONGESTION_VALUE, result)
    }

    @Test
    fun getRouteLineTrafficExpressionDataWithCongestionNumeric() {
        val colorResources = RouteLineColorResources.Builder().build()
        val routeAsJson = FileUtils.loadJsonFixture("route-with-congestion-numeric.json")
        val route = DirectionsRoute.fromJson(routeAsJson)
        val annotationProvider =
            MapboxRouteLineUtils.getTrafficCongestionAnnotationProvider(route, colorResources)

        val trafficExpressionData =
            MapboxRouteLineUtils.extractRouteDataWithTrafficAndRoadClassDeDuped(
                route,
                annotationProvider
            )

        MapboxRouteLineUtils.extractRouteDataWithTrafficAndRoadClassDeDuped(
            route,
            MapboxRouteLineUtils.getTrafficCongestionAnnotationProvider(route, colorResources)
        )
        MapboxRouteLineUtils.extractRouteDataWithTrafficAndRoadClassDeDuped(
            route,
            MapboxRouteLineUtils.getTrafficCongestionAnnotationProvider(route, colorResources)
        )

        assertEquals("low", trafficExpressionData[0].trafficCongestionIdentifier)
        assertEquals("moderate", trafficExpressionData[1].trafficCongestionIdentifier)
        assertEquals("severe", trafficExpressionData[2].trafficCongestionIdentifier)
    }

    @Test
    fun getRouteLineTrafficExpressionDataWithNoRouteOptions() {
        val colorResources = RouteLineColorResources.Builder().build()
        val routeAsJson = FileUtils.loadJsonFixture(
            "route-with-congestion-numeric-no-route-options.json"
        )
        val route = DirectionsRoute.fromJson(routeAsJson)
        val annotationProvider =
            MapboxRouteLineUtils.getTrafficCongestionAnnotationProvider(route, colorResources)

        val trafficExpressionData =
            MapboxRouteLineUtils.extractRouteData(
                route,
                annotationProvider
            )

        assertEquals("unknown", trafficExpressionData[0].trafficCongestionIdentifier)
        assertEquals(21, trafficExpressionData.size)
    }

    @Test
    fun getRouteLineTrafficExpressionDataWithNoCongestionOrCongestionNumeric() {
        val colorResources = RouteLineColorResources.Builder().build()
        val routeAsJson = FileUtils.loadJsonFixture(
            "route-with-no-congestion-annotation.json"
        )
        val route = DirectionsRoute.fromJson(routeAsJson)
        val annotationProvider =
            MapboxRouteLineUtils.getTrafficCongestionAnnotationProvider(route, colorResources)

        val trafficExpressionData =
            MapboxRouteLineUtils.extractRouteData(
                route,
                annotationProvider
            )

        assertEquals("unknown", trafficExpressionData[0].trafficCongestionIdentifier)
        assertEquals(21, trafficExpressionData.size)
    }

    @Test
    fun getAnnotationProvider_whenNumericTrafficSource_matchesDistances() {
        val colorResources = RouteLineColorResources.Builder().build()
        val routeAsJson = FileUtils.loadJsonFixture(
            "route-with-congestion-numeric.json"
        )
        val route = DirectionsRoute.fromJson(routeAsJson)

        val result =
            MapboxRouteLineUtils.getRouteLegTrafficNumericCongestionProvider(colorResources)

        assertEquals(
            route.legs()!!.first().annotation()!!.distance()!!.size,
            result(route.legs()!!.first()).size
        )
    }

    @Test
    fun getAnnotationProvider_whenNoRouteOptions() {
        val colorResources = RouteLineColorResources.Builder().build()
        val routeAsJson = FileUtils.loadJsonFixture(
            "route-with-congestion-numeric-no-route-options.json"
        )
        val route = DirectionsRoute.fromJson(routeAsJson)
        val expected = MapboxRouteLineUtils.getRouteLegTrafficCongestionProvider

        val result =
            MapboxRouteLineUtils.getTrafficCongestionAnnotationProvider(route, colorResources)

        assertEquals(expected, result)
    }

    @Test
    fun routeHasRestrictions_whenHasRestrictions() {
        val route = loadRoute("route-with-restrictions.json")

        val result = MapboxRouteLineUtils.routeHasRestrictions(route)

        assertTrue(result)
    }

    @Test
    fun routeHasRestrictions_whenNotHasRestrictions() {
        val route = loadRoute("motorway-with-road-classes-multi-leg.json")

        val result = MapboxRouteLineUtils.routeHasRestrictions(route)

        assertFalse(result)
    }

    @Test
    fun getRouteRestrictedSectionsExpressionData_multiLegRoute() {
        val route = loadRoute("two-leg-route-with-restrictions.json").toNavigationRoute(
            RouterOrigin.Offboard
        )

        val result = MapboxRouteLineUtils.extractRouteRestrictionData(route)

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
        val route = loadRoute("short_route.json")
        for (data in MapboxRouteLineUtils.extractRouteData(route) { null }) {
            assertEquals(UNKNOWN_CONGESTION_VALUE, data.trafficCongestionIdentifier)
        }
    }

    @Test
    fun `extractRouteData with empty congestion provider`() {
        val route = loadRoute("short_route.json")
        for (data in MapboxRouteLineUtils.extractRouteData(route) { emptyList() }) {
            assertEquals(UNKNOWN_CONGESTION_VALUE, data.trafficCongestionIdentifier)
        }
    }

    @Test
    fun `extractRouteData with short congestion provider`() {
        val route = loadRoute("short_route.json")
        val extractedData = MapboxRouteLineUtils.extractRouteData(route) { leg ->
            val distance = requireNotNull(leg.annotation()?.distance()?.takeIf { it.size > 1 })
            List(distance.size - 1) { "low" }
        }
        for (index in 0 until extractedData.lastIndex) {
            assertEquals("low", extractedData[index].trafficCongestionIdentifier)
        }
        assertEquals(
            UNKNOWN_CONGESTION_VALUE,
            extractedData.last().trafficCongestionIdentifier,
        )
    }

    @Test
    fun `getRoadClassArray when route has step intersections with incorrect geometry indexes`() {
        val route = loadRoute("route-with-incorrect-geometry-indexes.json")

        val result = MapboxRouteLineUtils.extractRouteDataWithTrafficAndRoadClassDeDuped(
            route,
            MapboxRouteLineUtils.getRouteLegTrafficCongestionProvider
        )

        assertEquals(0, result.size)
    }

    @Test
    fun `featureCollectionHasProperty when FeatureCollection is null`() {
        val result = MapboxRouteLineUtils.featureCollectionHasProperty(
            null,
            0,
            ""
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
            ""
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
            ""
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
            ""
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
            ""
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
            "someProperty"
        )

        assertTrue(result)
    }

    @Test
    fun getMatchingColors() {
        val mockFeature1 = mockk<Feature> {
            every { hasNonNullValueForProperty("someProperty") } returns false
        }
        val mockFeature2 = mockk<Feature> {
            every { hasNonNullValueForProperty("someProperty") } returns true
        }
        val mockFeatureCollection = mockk<FeatureCollection> {
            every { features() } returns listOf(mockFeature1, mockFeature2)
        }
        val styleDescriptors = listOf(RouteStyleDescriptor("someProperty", 1, 2))

        val result = MapboxRouteLineUtils.getMatchingColors(
            mockFeatureCollection,
            styleDescriptors,
            4,
            5
        )

        assertEquals(1, result.first)
        assertEquals(2, result.second)
    }

    @Test
    fun `getMatchingColors when no match`() {
        val mockFeature1 = mockk<Feature> {
            every { hasNonNullValueForProperty("someProperty") } returns false
        }
        val mockFeature2 = mockk<Feature> {
            every { hasNonNullValueForProperty("someProperty") } returns false
        }
        val mockFeatureCollection = mockk<FeatureCollection> {
            every { features() } returns listOf(mockFeature1, mockFeature2)
        }
        val styleDescriptors = listOf(RouteStyleDescriptor("someProperty", 1, 2))

        val result = MapboxRouteLineUtils.getMatchingColors(
            mockFeatureCollection,
            styleDescriptors,
            4,
            5
        )

        assertEquals(4, result.first)
        assertEquals(5, result.second)
    }

    @Test
    fun `getMatchingColors when feature collection is null`() {
        val styleDescriptors = listOf(RouteStyleDescriptor("someProperty", 1, 2))

        val result = MapboxRouteLineUtils.getMatchingColors(
            null,
            styleDescriptors,
            4,
            5
        )

        assertEquals(4, result.first)
        assertEquals(5, result.second)
    }

    @Test
    fun `getMatchingColors when route descriptors empty`() {
        val mockFeature1 = mockk<Feature> {
            every { hasNonNullValueForProperty("someProperty") } returns false
        }
        val mockFeature2 = mockk<Feature> {
            every { hasNonNullValueForProperty("someProperty") } returns true
        }
        val mockFeatureCollection = mockk<FeatureCollection> {
            every { features() } returns listOf(mockFeature1, mockFeature2)
        }

        val result = MapboxRouteLineUtils.getMatchingColors(
            mockFeatureCollection,
            listOf(),
            4,
            5
        )

        assertEquals(4, result.first)
        assertEquals(5, result.second)
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
        assertEquals(6, MapboxRouteLineUtils.layerGroup1SourceLayerIds.size)
        assertTrue(
            MapboxRouteLineUtils.layerGroup1SourceLayerIds.contains(
                RouteLayerConstants.LAYER_GROUP_1_TRAIL_CASING
            )
        )
        assertTrue(
            MapboxRouteLineUtils.layerGroup1SourceLayerIds.contains(
                RouteLayerConstants.LAYER_GROUP_1_TRAIL
            )
        )
        assertTrue(
            MapboxRouteLineUtils.layerGroup1SourceLayerIds.contains(
                RouteLayerConstants.LAYER_GROUP_1_CASING
            )
        )
        assertTrue(
            MapboxRouteLineUtils.layerGroup1SourceLayerIds.contains(
                RouteLayerConstants.LAYER_GROUP_1_MAIN
            )
        )
        assertTrue(
            MapboxRouteLineUtils.layerGroup1SourceLayerIds.contains(
                RouteLayerConstants.LAYER_GROUP_1_TRAFFIC
            )
        )
        assertTrue(
            MapboxRouteLineUtils.layerGroup1SourceLayerIds.contains(
                RouteLayerConstants.LAYER_GROUP_1_RESTRICTED
            )
        )
    }

    @Test
    fun layerGroup2SourceLayerIds() {
        assertEquals(6, MapboxRouteLineUtils.layerGroup2SourceLayerIds.size)
        assertTrue(
            MapboxRouteLineUtils.layerGroup2SourceLayerIds.contains(
                RouteLayerConstants.LAYER_GROUP_2_TRAIL_CASING
            )
        )
        assertTrue(
            MapboxRouteLineUtils.layerGroup2SourceLayerIds.contains(
                RouteLayerConstants.LAYER_GROUP_2_TRAIL
            )
        )
        assertTrue(
            MapboxRouteLineUtils.layerGroup2SourceLayerIds.contains(
                RouteLayerConstants.LAYER_GROUP_2_CASING
            )
        )
        assertTrue(
            MapboxRouteLineUtils.layerGroup2SourceLayerIds.contains(
                RouteLayerConstants.LAYER_GROUP_2_MAIN
            )
        )
        assertTrue(
            MapboxRouteLineUtils.layerGroup2SourceLayerIds.contains(
                RouteLayerConstants.LAYER_GROUP_2_TRAFFIC
            )
        )
        assertTrue(
            MapboxRouteLineUtils.layerGroup2SourceLayerIds.contains(
                RouteLayerConstants.LAYER_GROUP_2_RESTRICTED
            )
        )
    }

    @Test
    fun layerGroup3SourceLayerIds() {
        assertEquals(6, MapboxRouteLineUtils.layerGroup3SourceLayerIds.size)
        assertTrue(
            MapboxRouteLineUtils.layerGroup3SourceLayerIds.contains(
                RouteLayerConstants.LAYER_GROUP_3_TRAIL_CASING
            )
        )
        assertTrue(
            MapboxRouteLineUtils.layerGroup3SourceLayerIds.contains(
                RouteLayerConstants.LAYER_GROUP_3_TRAIL
            )
        )
        assertTrue(
            MapboxRouteLineUtils.layerGroup3SourceLayerIds.contains(
                RouteLayerConstants.LAYER_GROUP_3_CASING
            )
        )
        assertTrue(
            MapboxRouteLineUtils.layerGroup3SourceLayerIds.contains(
                RouteLayerConstants.LAYER_GROUP_3_MAIN
            )
        )
        assertTrue(
            MapboxRouteLineUtils.layerGroup3SourceLayerIds.contains(
                RouteLayerConstants.LAYER_GROUP_3_TRAFFIC
            )
        )
        assertTrue(
            MapboxRouteLineUtils.layerGroup3SourceLayerIds.contains(
                RouteLayerConstants.LAYER_GROUP_3_RESTRICTED
            )
        )
    }

    @Test
    fun sourceLayerMap() {
        assertEquals(3, MapboxRouteLineUtils.sourceLayerMap.size)
        assertEquals(
            MapboxRouteLineUtils.layerGroup1SourceKey,
            MapboxRouteLineUtils.sourceLayerMap.keys.toList()[0]
        )
        assertEquals(
            MapboxRouteLineUtils.layerGroup1SourceLayerIds,
            MapboxRouteLineUtils.sourceLayerMap.values.toList()[0]
        )
        assertEquals(
            MapboxRouteLineUtils.layerGroup2SourceKey,
            MapboxRouteLineUtils.sourceLayerMap.keys.toList()[1]
        )
        assertEquals(
            MapboxRouteLineUtils.layerGroup2SourceLayerIds,
            MapboxRouteLineUtils.sourceLayerMap.values.toList()[1]
        )
        assertEquals(
            MapboxRouteLineUtils.layerGroup3SourceKey,
            MapboxRouteLineUtils.sourceLayerMap.keys.toList()[2]
        )
        assertEquals(
            MapboxRouteLineUtils.layerGroup3SourceLayerIds,
            MapboxRouteLineUtils.sourceLayerMap.values.toList()[2]
        )
    }

    @Test
    fun getLayerIdsForPrimaryRoute() {
        val topLevelRouteLayer = StyleObjectInfo(
            TOP_LEVEL_ROUTE_LINE_LAYER_ID,
            "background"
        )
        val bottomLevelRouteLayer = StyleObjectInfo(
            BOTTOM_LEVEL_ROUTE_LINE_LAYER_ID,
            "background"
        )
        val layerGroup1 = StyleObjectInfo(
            LAYER_GROUP_1_MAIN,
            "line"
        )
        val layerGroup2 = StyleObjectInfo(
            LAYER_GROUP_2_MAIN,
            "line"
        )
        val style = mockk<Style> {
            every { styleLayers } returns listOf(
                bottomLevelRouteLayer,
                layerGroup2,
                layerGroup1,
                topLevelRouteLayer
            )
        }

        val result = MapboxRouteLineUtils.getLayerIdsForPrimaryRoute(
            style,
            MapboxRouteLineUtils.sourceLayerMap
        )

        assertEquals(MapboxRouteLineUtils.layerGroup1SourceLayerIds, result)
    }

    @Test
    fun `extractRouteRestrictionData with restriction at end of route`() {
        val route = loadRoute("route-with-restrictions-at-end.json").toNavigationRoute(
            RouterOrigin.Offboard
        )

        val result = MapboxRouteLineUtils.extractRouteRestrictionData(route)

        assertEquals(3, result.size)
        assertEquals(0.0, result[0].offset, 0.0)
        assertEquals(0, result[0].legIndex)
        assertFalse(result[0].isInRestrictedSection)
        assertEquals(0.9963424099457971, result[1].offset, 0.0)
        assertEquals(0, result[1].legIndex)
        assertTrue(result[1].isInRestrictedSection)
    }

    @Test
    fun `getRestrictedLineExpression with restriction at end of route`() {
        val expectedExpression = "[step, [line-progress], [rgba, 0.0, 0.0, 0.0, 0.0], 0.0, " +
            "[rgba, 0.0, 0.0, 0.0, 0.0], 0.9963424099457971, " +
            "[rgba, 0.0, 255.0, 255.0, 1.0], 1.0, [rgba, 0.0, 0.0, 0.0, 0.0]]"
        val route = loadRoute("route-with-restrictions-at-end.json").toNavigationRoute(
            RouterOrigin.Offboard
        )
        val expressionData = MapboxRouteLineUtils.extractRouteRestrictionData(route)

        val result = MapboxRouteLineUtils.getRestrictedLineExpression(
            0.0,
            0,
            Color.CYAN,
            expressionData
        )

        assertEquals(expectedExpression, result.toString())
    }

    @Test
    fun `extractRouteRestrictionData with restriction at start of route`() {
        val route =
            loadRoute("route-with-restrictions-at-start.json").toNavigationRoute(
                RouterOrigin.Offboard
            )

        val result = MapboxRouteLineUtils.extractRouteRestrictionData(route)

        assertEquals(2, result.size)
        assertEquals(0.0, result[0].offset, 0.0)
        assertEquals(0, result[0].legIndex)
        assertTrue(result[0].isInRestrictedSection)
        assertEquals(0.0036660165533364264, result[1].offset, 0.0)
        assertEquals(0, result[1].legIndex)
        assertFalse(result[1].isInRestrictedSection)
    }

    @Test
    fun routeHasRestrictions_when_routeNull() {
        val result = MapboxRouteLineUtils.routeHasRestrictions(null)

        assertFalse(result)
    }

    @Test
    fun routeHasRestrictions() {
        val route =
            loadRoute("route-with-restrictions-at-start.json").toNavigationRoute(
                RouterOrigin.Offboard
            )

        val result = MapboxRouteLineUtils.routeHasRestrictions(route)

        assertTrue(result)
    }

    @Test
    fun routeHasRestrictions_whenNoRestrictions() {
        val route =
            loadRoute("short_route.json").toNavigationRoute(
                RouterOrigin.Offboard
            )

        val result = MapboxRouteLineUtils.routeHasRestrictions(route)

        assertFalse(result)
    }

    private fun <T> listElementsAreEqual(
        first: List<T>,
        second: List<T>,
        equalityFun: (T, T) -> Boolean
    ): Boolean {
        if (first.size != second.size) {
            return false
        }

        return first.zip(second).all { (x, y) ->
            equalityFun(x, y)
        }
    }
}
