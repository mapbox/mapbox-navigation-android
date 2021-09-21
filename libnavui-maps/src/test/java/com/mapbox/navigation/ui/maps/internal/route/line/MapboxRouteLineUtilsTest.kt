package com.mapbox.navigation.ui.maps.internal.route.line

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.HandlerThread
import androidx.test.core.app.ApplicationProvider
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.LegAnnotation
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.bindgen.Value
import com.mapbox.core.constants.Constants
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.LayerPosition
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.Style
import com.mapbox.maps.StyleObjectInfo
import com.mapbox.maps.extension.style.layers.Layer
import com.mapbox.maps.extension.style.layers.getLayer
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.plugin.locationcomponent.LocationComponentConstants
import com.mapbox.navigation.testing.FileUtils.loadJsonFixture
import com.mapbox.navigation.ui.base.internal.model.route.RouteConstants
import com.mapbox.navigation.ui.base.model.route.RouteLayerConstants
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineUtils.getRestrictedRouteLegRanges
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineColorResources
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineExpressionData
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineScaleValue
import com.mapbox.navigation.ui.maps.route.line.model.RouteStyleDescriptor
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MapboxRouteLineUtilsTest {

    lateinit var ctx: Context

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
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
        val route = loadRoute("route-with-restrictions.json")

        val result = MapboxRouteLineUtils.extractRouteDataWithTrafficAndRoadClassDeDuped(
            route,
            MapboxRouteLineUtils.getRouteLegTrafficCongestionProvider
        )

        assertEquals(14, result.size)
        assertTrue(result[4].isInRestrictedSection)
        assertFalse(result[5].isInRestrictedSection)
        assertTrue(result[6].isInRestrictedSection)
    }

    @Test
    fun getRestrictedLineExpression() {
        val expectedExpression = "[step, [line-progress], [rgba, 0.0, 0.0, 0.0, 0.0], " +
            "0.2, [rgba, 0.0, 0.0, 0.0, 0.0], 0.44865144220494346, " +
            "[rgba, 255.0, 255.0, 255.0, 1.0], 0.468779750455607, [rgba, 0.0, 0.0, 0.0, 0.0]," +
            " 0.5032854217424586, [rgba, 255.0, 255.0, 255.0, 1.0], 0.5207714038134984, " +
            "[rgba, 0.0, 0.0, 0.0, 0.0]]"

        val route = loadRoute("route-with-restrictions.json")
        val expData = MapboxRouteLineUtils.extractRouteData(
            route,
            MapboxRouteLineUtils.getRouteLegTrafficCongestionProvider
        )

        val expression = MapboxRouteLineUtils.getRestrictedLineExpression(
            0.2,
            0,
            -1,
            expData
        )

        assertEquals(expectedExpression, expression.toString())
    }

    @Test
    fun getRestrictedLineExpressionProducer() {
        val colorResources = RouteLineColorResources.Builder()
            .restrictedRoadColor(Color.CYAN)
            .build()
        val expectedExpression = "[step, [line-progress], [rgba, 0.0, 0.0, 0.0, 0.0], " +
            "0.2, [rgba, 0.0, 0.0, 0.0, 0.0], 0.44865144220494346, " +
            "[rgba, 0.0, 255.0, 255.0, 1.0], 0.468779750455607, [rgba, 0.0, 0.0, 0.0, 0.0]," +
            " 0.5032854217424586, [rgba, 0.0, 255.0, 255.0, 1.0], 0.5207714038134984," +
            " [rgba, 0.0, 0.0, 0.0, 0.0]]"
        val route = loadRoute("route-with-restrictions.json")

        val expression = MapboxRouteLineUtils.getRestrictedLineExpressionProducer(
            route,
            0.2,
            0,
            colorResources
        ).invoke()

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
        ).invoke()

        assertEquals(expectedExpression, expression.toString())
    }

    @Test
    fun getRestrictedLineExpression_whenNoRestrictionsInRoute() {
        val expectedExpression = "[step, [line-progress], [rgba, 0.0, 0.0, 0.0, 0.0], 0.2, " +
            "[rgba, 0.0, 0.0, 0.0, 0.0]]"
        val route = loadRoute("short_route.json")
        val expData = MapboxRouteLineUtils.extractRouteData(
            route,
            MapboxRouteLineUtils.getRouteLegTrafficCongestionProvider
        )

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
    fun getRouteLineColorExpressions() {
        val blueLineDescriptor = RouteStyleDescriptor("blueLine", 1, 3)
        val redLineDescriptor = RouteStyleDescriptor("redLine", 2, 4)

        val result = MapboxRouteLineUtils.getRouteLineColorExpressions(
            7,
            listOf(blueLineDescriptor, redLineDescriptor),
            RouteStyleDescriptor::lineColor
        )

        assertEquals(7, result.size)
        assertEquals("[==, [get, mapboxDescriptorPlaceHolderUnused], true]", result[0].toString())
        assertEquals("[rgba, 0.0, 0.0, 7.0, 0.0]", result[1].toString())
        assertEquals("[==, [get, blueLine], true]", result[2].toString())
        assertEquals("[rgba, 0.0, 0.0, 1.0, 0.0]", result[3].toString())
        assertEquals("[==, [get, redLine], true]", result[4].toString())
        assertEquals("[rgba, 0.0, 0.0, 2.0, 0.0]", result[5].toString())
        assertEquals("[rgba, 0.0, 0.0, 7.0, 0.0]", result[6].toString())
    }

    @Test
    fun layersAreInitialized() {
        val options = mockk<MapboxRouteLineOptions> {
            every { displayRestrictedRoadSections } returns true
        }
        val style = mockk<Style> {
            every { isStyleLoaded } returns true
            every { styleSourceExists(RouteConstants.PRIMARY_ROUTE_SOURCE_ID) } returns true
            every { styleSourceExists(RouteConstants.ALTERNATIVE_ROUTE1_SOURCE_ID) } returns true
            every { styleSourceExists(RouteConstants.ALTERNATIVE_ROUTE2_SOURCE_ID) } returns true
            every { styleLayerExists(RouteLayerConstants.PRIMARY_ROUTE_LAYER_ID) } returns true
            every {
                styleLayerExists(RouteLayerConstants.PRIMARY_ROUTE_TRAFFIC_LAYER_ID)
            } returns true
            every {
                styleLayerExists(RouteLayerConstants.PRIMARY_ROUTE_CASING_LAYER_ID)
            } returns true
            every { styleLayerExists(RouteLayerConstants.ALTERNATIVE_ROUTE1_LAYER_ID) } returns true
            every { styleLayerExists(RouteLayerConstants.ALTERNATIVE_ROUTE2_LAYER_ID) } returns true
            every {
                styleLayerExists(RouteLayerConstants.ALTERNATIVE_ROUTE1_CASING_LAYER_ID)
            } returns true
            every {
                styleLayerExists(RouteLayerConstants.ALTERNATIVE_ROUTE2_CASING_LAYER_ID)
            } returns true
            every {
                styleLayerExists(RouteLayerConstants.ALTERNATIVE_ROUTE1_TRAFFIC_LAYER_ID)
            } returns true
            every {
                styleLayerExists(RouteLayerConstants.ALTERNATIVE_ROUTE2_TRAFFIC_LAYER_ID)
            } returns true
            every {
                styleLayerExists(RouteLayerConstants.RESTRICTED_ROAD_LAYER_ID)
            } returns true
            every {
                styleLayerExists(RouteLayerConstants.TOP_LEVEL_ROUTE_LINE_LAYER_ID)
            } returns true
        }

        val result = MapboxRouteLineUtils.layersAreInitialized(style, options)

        assertTrue(result)
        verify { style.isStyleLoaded }
        verify { style.styleSourceExists(RouteConstants.PRIMARY_ROUTE_SOURCE_ID) }
        verify { style.styleSourceExists(RouteConstants.ALTERNATIVE_ROUTE1_SOURCE_ID) }
        verify { style.styleSourceExists(RouteConstants.ALTERNATIVE_ROUTE2_SOURCE_ID) }
        verify { style.styleLayerExists(RouteLayerConstants.PRIMARY_ROUTE_LAYER_ID) }
        verify { style.styleLayerExists(RouteLayerConstants.PRIMARY_ROUTE_TRAFFIC_LAYER_ID) }
        verify { style.styleLayerExists(RouteLayerConstants.PRIMARY_ROUTE_CASING_LAYER_ID) }
        verify { style.styleLayerExists(RouteLayerConstants.ALTERNATIVE_ROUTE1_LAYER_ID) }
        verify { style.styleLayerExists(RouteLayerConstants.ALTERNATIVE_ROUTE2_LAYER_ID) }
        verify { style.styleLayerExists(RouteLayerConstants.ALTERNATIVE_ROUTE1_CASING_LAYER_ID) }
        verify { style.styleLayerExists(RouteLayerConstants.ALTERNATIVE_ROUTE2_CASING_LAYER_ID) }
        verify { style.styleLayerExists(RouteLayerConstants.ALTERNATIVE_ROUTE1_TRAFFIC_LAYER_ID) }
        verify { style.styleLayerExists(RouteLayerConstants.ALTERNATIVE_ROUTE2_TRAFFIC_LAYER_ID) }
        verify { style.styleLayerExists(RouteLayerConstants.RESTRICTED_ROAD_LAYER_ID) }
    }

    @Test
    fun `layersAreInitialized without restricted roads`() {
        val options = mockk<MapboxRouteLineOptions> {
            every { displayRestrictedRoadSections } returns false
        }
        val style = mockk<Style> {
            every { isStyleLoaded } returns true
            every { styleSourceExists(RouteConstants.PRIMARY_ROUTE_SOURCE_ID) } returns true
            every { styleSourceExists(RouteConstants.ALTERNATIVE_ROUTE1_SOURCE_ID) } returns true
            every { styleSourceExists(RouteConstants.ALTERNATIVE_ROUTE2_SOURCE_ID) } returns true
            every { styleLayerExists(RouteLayerConstants.PRIMARY_ROUTE_LAYER_ID) } returns true
            every {
                styleLayerExists(RouteLayerConstants.PRIMARY_ROUTE_TRAFFIC_LAYER_ID)
            } returns true
            every {
                styleLayerExists(RouteLayerConstants.PRIMARY_ROUTE_CASING_LAYER_ID)
            } returns true
            every { styleLayerExists(RouteLayerConstants.ALTERNATIVE_ROUTE1_LAYER_ID) } returns true
            every { styleLayerExists(RouteLayerConstants.ALTERNATIVE_ROUTE2_LAYER_ID) } returns true
            every {
                styleLayerExists(RouteLayerConstants.ALTERNATIVE_ROUTE1_CASING_LAYER_ID)
            } returns true
            every {
                styleLayerExists(RouteLayerConstants.ALTERNATIVE_ROUTE2_CASING_LAYER_ID)
            } returns true
            every {
                styleLayerExists(RouteLayerConstants.ALTERNATIVE_ROUTE1_TRAFFIC_LAYER_ID)
            } returns true
            every {
                styleLayerExists(RouteLayerConstants.ALTERNATIVE_ROUTE2_TRAFFIC_LAYER_ID)
            } returns true
            every {
                styleLayerExists(RouteLayerConstants.RESTRICTED_ROAD_LAYER_ID)
            } returns false
            every {
                styleLayerExists(RouteLayerConstants.TOP_LEVEL_ROUTE_LINE_LAYER_ID)
            } returns true
        }

        val result = MapboxRouteLineUtils.layersAreInitialized(style, options)

        assertTrue(result)
        verify { style.isStyleLoaded }
        verify { style.styleSourceExists(RouteConstants.PRIMARY_ROUTE_SOURCE_ID) }
        verify { style.styleSourceExists(RouteConstants.ALTERNATIVE_ROUTE1_SOURCE_ID) }
        verify { style.styleSourceExists(RouteConstants.ALTERNATIVE_ROUTE2_SOURCE_ID) }
        verify { style.styleLayerExists(RouteLayerConstants.PRIMARY_ROUTE_LAYER_ID) }
        verify { style.styleLayerExists(RouteLayerConstants.PRIMARY_ROUTE_TRAFFIC_LAYER_ID) }
        verify { style.styleLayerExists(RouteLayerConstants.PRIMARY_ROUTE_CASING_LAYER_ID) }
        verify { style.styleLayerExists(RouteLayerConstants.ALTERNATIVE_ROUTE1_LAYER_ID) }
        verify { style.styleLayerExists(RouteLayerConstants.ALTERNATIVE_ROUTE2_LAYER_ID) }
        verify { style.styleLayerExists(RouteLayerConstants.ALTERNATIVE_ROUTE1_CASING_LAYER_ID) }
        verify { style.styleLayerExists(RouteLayerConstants.ALTERNATIVE_ROUTE2_CASING_LAYER_ID) }
        verify { style.styleLayerExists(RouteLayerConstants.ALTERNATIVE_ROUTE1_TRAFFIC_LAYER_ID) }
        verify { style.styleLayerExists(RouteLayerConstants.ALTERNATIVE_ROUTE2_TRAFFIC_LAYER_ID) }
        verify(exactly = 0) {
            style.styleLayerExists(RouteLayerConstants.RESTRICTED_ROAD_LAYER_ID)
        }
    }

    @Test
    fun initializeLayers_whenStyleNotLoaded() {
        val options = MapboxRouteLineOptions.Builder(ctx).build()
        val style = mockk<Style> {
            every { isStyleLoaded } returns false
        }

        MapboxRouteLineUtils.initializeLayers(style, options)

        verify(exactly = 0) { style.styleSourceExists(any()) }
    }

    @Test
    fun initializeLayers_whenLayersAreInitialized() {
        val options = MapboxRouteLineOptions.Builder(ctx).build()
        val style = mockk<Style> {
            every { styleLayers } returns listOf()
            every { isStyleLoaded } returns true
            every { styleSourceExists(RouteConstants.PRIMARY_ROUTE_SOURCE_ID) } returns true
            every { styleSourceExists(RouteConstants.ALTERNATIVE_ROUTE1_SOURCE_ID) } returns true
            every { styleSourceExists(RouteConstants.ALTERNATIVE_ROUTE2_SOURCE_ID) } returns true
            every { styleLayerExists(RouteLayerConstants.PRIMARY_ROUTE_LAYER_ID) } returns true
            every {
                styleLayerExists(RouteLayerConstants.PRIMARY_ROUTE_TRAFFIC_LAYER_ID)
            } returns true
            every {
                styleLayerExists(RouteLayerConstants.PRIMARY_ROUTE_CASING_LAYER_ID)
            } returns true
            every { styleLayerExists(RouteLayerConstants.ALTERNATIVE_ROUTE1_LAYER_ID) } returns true
            every { styleLayerExists(RouteLayerConstants.ALTERNATIVE_ROUTE2_LAYER_ID) } returns true
            every {
                styleLayerExists(RouteLayerConstants.ALTERNATIVE_ROUTE1_CASING_LAYER_ID)
            } returns true
            every {
                styleLayerExists(RouteLayerConstants.ALTERNATIVE_ROUTE2_CASING_LAYER_ID)
            } returns true
            every {
                styleLayerExists(RouteLayerConstants.ALTERNATIVE_ROUTE1_TRAFFIC_LAYER_ID)
            } returns true
            every {
                styleLayerExists(RouteLayerConstants.ALTERNATIVE_ROUTE2_TRAFFIC_LAYER_ID)
            } returns true
            every {
                styleLayerExists(RouteLayerConstants.RESTRICTED_ROAD_LAYER_ID)
            } returns true
            every {
                styleLayerExists(RouteLayerConstants.TOP_LEVEL_ROUTE_LINE_LAYER_ID)
            } returns true
            every { styleSourceExists(RouteConstants.WAYPOINT_SOURCE_ID) } returns false
        }

        MapboxRouteLineUtils.initializeLayers(style, options)

        verify(exactly = 0) { style.styleLayers }
        verify(exactly = 0) { style.addStyleSource(any(), any()) }
    }

    @OptIn(MapboxExperimental::class)
    @Test
    fun initializeLayers() {
        GeoJsonSource.workerThread =
            HandlerThread("STYLE_WORKER").apply {
                priority = Thread.MAX_PRIORITY
                start()
            }
        mockkStatic("com.mapbox.maps.extension.style.layers.LayerUtils")
        val options = MapboxRouteLineOptions.Builder(ctx)
            .withRouteLineBelowLayerId(LocationComponentConstants.MODEL_LAYER)
            .displayRestrictedRoadSections(true)
            .build()
        val waypointSourceValueSlot = slot<Value>()
        val primaryRouteSourceValueSlot = slot<Value>()
        val alternativeRoute1SourceValueSlot = slot<Value>()
        val alternativeRoute2SourceValueSlot = slot<Value>()
        val topLevelRouteSourceValueSlot = slot<Value>()
        val addStyleLayerSlots = mutableListOf<Value>()
        val addStyleLayerPositionSlots = mutableListOf<LayerPosition>()
        val mockLayer = mockk<StyleObjectInfo> {
            every { id } returns LocationComponentConstants.MODEL_LAYER
        }
        val style = mockk<Style> {
            every { isStyleLoaded } returns true
            every { styleLayers } returns listOf(mockLayer)
            every { styleSourceExists(RouteConstants.PRIMARY_ROUTE_SOURCE_ID) } returns false
            every { styleSourceExists(RouteConstants.ALTERNATIVE_ROUTE1_SOURCE_ID) } returns false
            every { styleSourceExists(RouteConstants.ALTERNATIVE_ROUTE2_SOURCE_ID) } returns false
            every { styleSourceExists(RouteConstants.WAYPOINT_SOURCE_ID) } returns false
            every {
                styleSourceExists(RouteConstants.TOP_LEVEL_ROUTE_LAYER_SOURCE_ID)
            } returns false
            every { styleLayerExists(RouteLayerConstants.PRIMARY_ROUTE_LAYER_ID) } returns false
            every {
                styleLayerExists(RouteLayerConstants.PRIMARY_ROUTE_TRAFFIC_LAYER_ID)
            } returns false
            every {
                styleLayerExists(RouteLayerConstants.PRIMARY_ROUTE_CASING_LAYER_ID)
            } returns false
            every {
                styleLayerExists(RouteLayerConstants.ALTERNATIVE_ROUTE1_LAYER_ID)
            } returns false
            every {
                styleLayerExists(RouteLayerConstants.ALTERNATIVE_ROUTE2_LAYER_ID)
            } returns false
            every { getStyleImage(RouteConstants.ARROW_HEAD_ICON) } returns null
            every { getStyleImage(RouteConstants.ARROW_HEAD_ICON_CASING) } returns null
            every { getStyleImage(RouteConstants.ORIGIN_MARKER_NAME) } returns null
            every { getStyleImage(RouteConstants.DESTINATION_MARKER_NAME) } returns null
            every {
                styleLayerExists(RouteLayerConstants.ALTERNATIVE_ROUTE1_CASING_LAYER_ID)
            } returns false
            every {
                styleLayerExists(RouteLayerConstants.ALTERNATIVE_ROUTE2_CASING_LAYER_ID)
            } returns false
            every {
                styleLayerExists(RouteLayerConstants.ALTERNATIVE_ROUTE1_TRAFFIC_LAYER_ID)
            } returns false
            every {
                styleLayerExists(RouteLayerConstants.ALTERNATIVE_ROUTE2_TRAFFIC_LAYER_ID)
            } returns false
            every { styleLayerExists(RouteLayerConstants.WAYPOINT_LAYER_ID) } returns false
            every { styleLayerExists(LocationComponentConstants.MODEL_LAYER) } returns true
            every {
                addStyleSource(RouteConstants.WAYPOINT_SOURCE_ID, any())
            } returns ExpectedFactory.createNone()
            every {
                addStyleSource(RouteConstants.PRIMARY_ROUTE_SOURCE_ID, any())
            } returns ExpectedFactory.createNone()
            every {
                addStyleSource(RouteConstants.ALTERNATIVE_ROUTE1_SOURCE_ID, any())
            } returns ExpectedFactory.createNone()
            every {
                addStyleSource(RouteConstants.ALTERNATIVE_ROUTE2_SOURCE_ID, any())
            } returns ExpectedFactory.createNone()
            every {
                addStyleSource(RouteConstants.TOP_LEVEL_ROUTE_LAYER_SOURCE_ID, any())
            } returns ExpectedFactory.createNone()
            every { addPersistentStyleLayer(any(), any()) } returns ExpectedFactory.createNone()
            every {
                addImage(RouteConstants.ORIGIN_MARKER_NAME, any<Bitmap>())
            } returns ExpectedFactory.createNone()
            every {
                addImage(RouteConstants.DESTINATION_MARKER_NAME, any<Bitmap>())
            } returns ExpectedFactory.createNone()
        }

        MapboxRouteLineUtils.initializeLayers(style, options)

        verify {
            style.addStyleSource(
                RouteConstants.TOP_LEVEL_ROUTE_LAYER_SOURCE_ID,
                capture(topLevelRouteSourceValueSlot)
            )
        }
        assertEquals(
            "geojson",
            (
                topLevelRouteSourceValueSlot.captured.contents as HashMap<String, Value>
                )["type"]!!.contents
        )
        assertEquals(
            "{\"type\":\"FeatureCollection\",\"features\":[]}",
            (
                topLevelRouteSourceValueSlot.captured.contents as HashMap<String, Value>
                )["data"]!!.contents
        )

        verify {
            style.addStyleSource(
                RouteConstants.WAYPOINT_SOURCE_ID, capture(waypointSourceValueSlot)
            )
        }
        assertEquals(
            "geojson",
            (waypointSourceValueSlot.captured.contents as HashMap<String, Value>)["type"]!!.contents
        )
        assertEquals(
            16L,
            (waypointSourceValueSlot.captured.contents as HashMap<String, Value>)["maxzoom"]!!
                .contents
        )
        assertEquals(
            "{\"type\":\"FeatureCollection\",\"features\":[]}",
            (waypointSourceValueSlot.captured.contents as HashMap<String, Value>)["data"]!!.contents
        )
        assertEquals(
            RouteConstants.DEFAULT_ROUTE_SOURCES_TOLERANCE,
            (waypointSourceValueSlot.captured.contents as HashMap<String, Value>)
            ["tolerance"]!!.contents
        )

        verify {
            style.addStyleSource(
                RouteConstants.PRIMARY_ROUTE_SOURCE_ID,
                capture(primaryRouteSourceValueSlot)
            )
        }
        assertEquals(
            "geojson",
            (primaryRouteSourceValueSlot.captured.contents as HashMap<String, Value>)["type"]
            !!.contents
        )
        assertEquals(
            16L,
            (primaryRouteSourceValueSlot.captured.contents as HashMap<String, Value>)["maxzoom"]
            !!.contents
        )
        assertEquals(
            true,
            (primaryRouteSourceValueSlot.captured.contents as HashMap<String, Value>)["lineMetrics"]
            !!.contents
        )
        assertEquals(
            "{\"type\":\"FeatureCollection\",\"features\":[]}",
            (primaryRouteSourceValueSlot.captured.contents as HashMap<String, Value>)["data"]
            !!.contents
        )
        assertEquals(
            RouteConstants.DEFAULT_ROUTE_SOURCES_TOLERANCE,
            (primaryRouteSourceValueSlot.captured.contents as HashMap<String, Value>)
            ["tolerance"]!!.contents
        )

        verify {
            style.addStyleSource(
                RouteConstants.ALTERNATIVE_ROUTE1_SOURCE_ID,
                capture(alternativeRoute1SourceValueSlot)
            )
        }
        assertEquals(
            "geojson",
            (alternativeRoute1SourceValueSlot.captured.contents as HashMap<String, Value>)
            ["type"]!!.contents
        )
        assertEquals(
            16L,
            (alternativeRoute1SourceValueSlot.captured.contents as HashMap<String, Value>)
            ["maxzoom"]!!.contents
        )
        assertEquals(
            true,
            (alternativeRoute1SourceValueSlot.captured.contents as HashMap<String, Value>)
            ["lineMetrics"]!!.contents
        )
        assertEquals(
            "{\"type\":\"FeatureCollection\",\"features\":[]}",
            (alternativeRoute1SourceValueSlot.captured.contents as HashMap<String, Value>)
            ["data"]!!.contents
        )
        assertEquals(
            RouteConstants.DEFAULT_ROUTE_SOURCES_TOLERANCE,
            (alternativeRoute1SourceValueSlot.captured.contents as HashMap<String, Value>)
            ["tolerance"]!!.contents
        )

        verify {
            style.addStyleSource(
                RouteConstants.ALTERNATIVE_ROUTE2_SOURCE_ID,
                capture(alternativeRoute2SourceValueSlot)
            )
        }
        assertEquals(
            "geojson",
            (alternativeRoute2SourceValueSlot.captured.contents as HashMap<String, Value>)
            ["type"]!!.contents
        )
        assertEquals(
            16L,
            (alternativeRoute2SourceValueSlot.captured.contents as HashMap<String, Value>)
            ["maxzoom"]!!.contents
        )
        assertEquals(
            true,
            (alternativeRoute2SourceValueSlot.captured.contents as HashMap<String, Value>)
            ["lineMetrics"]!!.contents
        )
        assertEquals(
            "{\"type\":\"FeatureCollection\",\"features\":[]}",
            (alternativeRoute2SourceValueSlot.captured.contents as HashMap<String, Value>)
            ["data"]!!.contents
        )
        assertEquals(
            RouteConstants.DEFAULT_ROUTE_SOURCES_TOLERANCE,
            (alternativeRoute2SourceValueSlot.captured.contents as HashMap<String, Value>)
            ["tolerance"]!!.contents
        )

        verify {
            style.addPersistentStyleLayer(
                capture(addStyleLayerSlots),
                capture(addStyleLayerPositionSlots)
            )
        }
        assertEquals(
            "mapbox-navigation-alt-route1-casing-layer",
            (addStyleLayerSlots[0].contents as HashMap<String, Value>)["id"]!!.contents
        )
        assertEquals(
            "mapbox-navigation-alt-route2-casing-layer",
            (addStyleLayerSlots[1].contents as HashMap<String, Value>)["id"]!!.contents
        )
        assertEquals(
            "mapbox-navigation-alt-route1-layer",
            (addStyleLayerSlots[2].contents as HashMap<String, Value>)["id"]!!.contents
        )
        assertEquals(
            "mapbox-navigation-alt-route2-layer",
            (addStyleLayerSlots[3].contents as HashMap<String, Value>)["id"]!!.contents
        )
        assertEquals(
            "mapbox-navigation-alt-route1-traffic-layer",
            (addStyleLayerSlots[4].contents as HashMap<String, Value>)["id"]!!.contents
        )
        assertEquals(
            "mapbox-navigation-alt-route2-traffic-layer",
            (addStyleLayerSlots[5].contents as HashMap<String, Value>)["id"]!!.contents
        )
        assertEquals(
            "mapbox-navigation-route-casing-layer",
            (addStyleLayerSlots[6].contents as HashMap<String, Value>)["id"]!!.contents
        )
        assertEquals(
            "mapbox-navigation-route-layer",
            (addStyleLayerSlots[7].contents as HashMap<String, Value>)["id"]!!.contents
        )
        assertEquals(
            "mapbox-navigation-route-traffic-layer",
            (addStyleLayerSlots[8].contents as HashMap<String, Value>)["id"]!!.contents
        )
        assertEquals(
            "mapbox-restricted-road-layer",
            (addStyleLayerSlots[9].contents as HashMap<String, Value>)["id"]!!.contents
        )
        assertEquals(
            "mapbox-top-level-route-layer",
            (addStyleLayerSlots[10].contents as HashMap<String, Value>)["id"]!!.contents
        )
        assertEquals(
            "mapbox-navigation-waypoint-layer",
            (addStyleLayerSlots[11].contents as HashMap<String, Value>)["id"]!!.contents
        )
        assertEquals(
            LocationComponentConstants.MODEL_LAYER,
            addStyleLayerPositionSlots[0].below
        )
        assertEquals(
            LocationComponentConstants.MODEL_LAYER,
            addStyleLayerPositionSlots[1].below
        )
        assertEquals(
            LocationComponentConstants.MODEL_LAYER,
            addStyleLayerPositionSlots[2].below
        )
        assertEquals(
            LocationComponentConstants.MODEL_LAYER,
            addStyleLayerPositionSlots[3].below
        )
        assertEquals(
            LocationComponentConstants.MODEL_LAYER,
            addStyleLayerPositionSlots[4].below
        )
        assertEquals(
            LocationComponentConstants.MODEL_LAYER,
            addStyleLayerPositionSlots[5].below
        )
        assertEquals(
            LocationComponentConstants.MODEL_LAYER,
            addStyleLayerPositionSlots[6].below
        )
        assertEquals(
            LocationComponentConstants.MODEL_LAYER,
            addStyleLayerPositionSlots[7].below
        )
        assertEquals(
            LocationComponentConstants.MODEL_LAYER,
            addStyleLayerPositionSlots[8].below
        )
        assertEquals(
            LocationComponentConstants.MODEL_LAYER,
            addStyleLayerPositionSlots[9].below
        )
        assertEquals(
            LocationComponentConstants.MODEL_LAYER,
            addStyleLayerPositionSlots[10].below
        )
        assertEquals(
            LocationComponentConstants.MODEL_LAYER,
            addStyleLayerPositionSlots[11].below
        )
        unmockkStatic("com.mapbox.maps.extension.style.layers.LayerUtils")
    }

    @Test
    fun calculateRouteGranularDistances() {
        val routeAsJsonJson = loadJsonFixture("short_route.json")
        val route = DirectionsRoute.fromJson(routeAsJsonJson)
        val lineString = LineString.fromPolyline(
            route.geometry() ?: "",
            Constants.PRECISION_6
        )

        val result = MapboxRouteLineUtils.calculateRouteGranularDistances(lineString.coordinates())

        assertEquals(0.0000025451727518618744, result!!.distance, 0.0)
        assertEquals(5, result.distancesArray.size())
        assertEquals(Point.fromLngLat(-122.523671, 37.975379), result.distancesArray[0].point)
        assertEquals(0.0000025451727518618744, result.distancesArray[0].distanceRemaining, 0.0)
        assertEquals(Point.fromLngLat(-122.523131, 37.975067), result.distancesArray[4].point)
        assertEquals(0.0, result.distancesArray[4].distanceRemaining, 0.0)
    }

    @Test
    fun calculateRouteGranularDistances_whenInputNull() {
        val result = MapboxRouteLineUtils.calculateRouteGranularDistances(listOf())

        assertNull(result)
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
    fun findDistanceToNearestPointOnCurrentLine() {
        val route = getMultilegRoute()
        val lineString = LineString.fromPolyline(route.geometry() ?: "", Constants.PRECISION_6)
        val distances = MapboxRouteLineUtils.calculateRouteGranularDistances(
            lineString.coordinates()
        )

        val result = MapboxRouteLineUtils.findDistanceToNearestPointOnCurrentLine(
            lineString.coordinates()[15],
            distances!!,
            12
        )

        assertEquals(141.6772603078415, result, 0.0)
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
        val routeAsJsonJson = loadJsonFixture("route-unique-road-classes.json")
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
        assertEquals(RouteConstants.LOW_CONGESTION_VALUE, result.last().trafficCongestionIdentifier)
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
        val routeAsJsonJson = loadJsonFixture("route-with-road-classes.txt")
        val route = DirectionsRoute.fromJson(routeAsJsonJson)

        val result = MapboxRouteLineUtils.extractRouteDataWithTrafficAndRoadClassDeDuped(
            route,
            MapboxRouteLineUtils.getRouteLegTrafficCongestionProvider
        )

        assertEquals(10, result.size)
        assertEquals(1300.0000000000002, result.last().distanceFromOrigin, 0.0)
        assertEquals(RouteConstants.LOW_CONGESTION_VALUE, result.last().trafficCongestionIdentifier)
        assertEquals("service", result.last().roadClass)
    }

    @Test
    fun getRouteLineTrafficExpressionDataWithSomeRoadClassesDuplicatesRemoved() {
        val routeAsJsonJson =
            loadJsonFixture("motorway-route-with-road-classes-mixed.json")
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

        val routeAsJsonJson = loadJsonFixture("motorway-route-with-road-classes.json")
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
            loadJsonFixture("motorway-route-with-road-classes-mixed.json")
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

        val routeAsJsonJson = loadJsonFixture("route-with-closure.json")
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
        val routeAsJsonJson = loadJsonFixture("route-with-traffic-no-street-classes.txt")
        val route = DirectionsRoute.fromJson(routeAsJsonJson)

        val result = MapboxRouteLineUtils.extractRouteDataWithTrafficAndRoadClassDeDuped(
            route,
            MapboxRouteLineUtils.getRouteLegTrafficCongestionProvider
        )

        assertEquals(5, result.size)
        assertEquals(1188.7000000000003, result.last().distanceFromOrigin, 0.0)
        assertEquals(RouteConstants.LOW_CONGESTION_VALUE, result.last().trafficCongestionIdentifier)
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

        val routeAsJsonJson = loadJsonFixture("route-with-road-classes.txt")
        val route = DirectionsRoute.fromJson(routeAsJsonJson)
        val trafficExpressionData =
            MapboxRouteLineUtils.extractRouteDataWithTrafficAndRoadClassDeDuped(
                route,
                MapboxRouteLineUtils.getRouteLegTrafficCongestionProvider
            )

        assertEquals("service", trafficExpressionData[0].roadClass)
        assertEquals("street", trafficExpressionData[1].roadClass)
        assertEquals(
            RouteConstants.UNKNOWN_CONGESTION_VALUE,
            trafficExpressionData[0].trafficCongestionIdentifier
        )
        assertEquals(
            RouteConstants.UNKNOWN_CONGESTION_VALUE,
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
        val routeAsJsonJson = loadJsonFixture("route-with-traffic-no-street-classes.txt")
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

        val routeAsJsonJson = loadJsonFixture(
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
        Assert.assertNotEquals(-9, result[1].segmentColor)
        assertEquals(-1, result[1].segmentColor)
        assertEquals(-2, result[2].segmentColor)
    }

    @Test
    fun getRouteLineTrafficExpressionDataMissingRoadClass() {
        val routeAsJsonJson = loadJsonFixture(
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
        val routeAsJsonJson = loadJsonFixture(
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
        val routeAsJsonJson = loadJsonFixture("route-with-closure.json")
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
        val route = loadRoute("route-with-restrictions.json")
        val expectedDistanceFromOriginIndex3 =
            route.legs()!!.first().annotation()!!.distance()!!.subList(0, 3).sum()
        val expectedDistanceFromOriginIndex17 =
            route.legs()!!.first().annotation()!!.distance()!!.subList(0, 17).sum()
        val expectedDistanceFromOriginIndex18 =
            route.legs()!!.first().annotation()!!.distance()!!.subList(0, 18).sum()
        val expectedDistanceFromOriginIndex19 =
            route.legs()!!.first().annotation()!!.distance()!!.subList(0, 19).sum()
        val expectedDistanceFromOriginIndex20 =
            route.legs()!!.first().annotation()!!.distance()!!.subList(0, 20).sum()

        val trafficExpressionData = MapboxRouteLineUtils.extractRouteData(
            route,
            MapboxRouteLineUtils.getRouteLegTrafficCongestionProvider
        )

        assertEquals(0.0, trafficExpressionData[0].offset, 0.0)
        assertEquals(true, trafficExpressionData[0].isLegOrigin)

        assertEquals(
            expectedDistanceFromOriginIndex3,
            trafficExpressionData[3].offset * route.distance(),
            0.0
        )
        assertEquals(0, trafficExpressionData[3].legIndex)
        assertEquals(false, trafficExpressionData[3].isInRestrictedSection)
        assertEquals(false, trafficExpressionData[3].isLegOrigin)

        assertEquals(
            expectedDistanceFromOriginIndex17,
            trafficExpressionData[17].offset * route.distance(),
            0.0
        )
        assertEquals(true, trafficExpressionData[17].isInRestrictedSection)

        assertEquals(
            expectedDistanceFromOriginIndex18,
            trafficExpressionData[18].offset * route.distance(),
            0.0
        )
        assertEquals(false, trafficExpressionData[18].isInRestrictedSection)

        assertEquals(
            expectedDistanceFromOriginIndex19,
            trafficExpressionData[19].offset * route.distance(),
            0.000000000001
        )
        assertEquals(true, trafficExpressionData[19].isInRestrictedSection)

        assertEquals(
            expectedDistanceFromOriginIndex20,
            trafficExpressionData[20].offset * route.distance(),
            0.0
        )
        assertEquals(false, trafficExpressionData[20].isInRestrictedSection)
    }

    @Test
    fun getRouteLineTrafficExpressionData_whenFirstDistanceInSecondLegIsZero() {
        val route = getMultilegWithTwoLegs()
        val routeGeometry = LineString.fromPolyline(
            route.geometry() ?: "",
            Constants.PRECISION_6
        )

        val result = MapboxRouteLineUtils.extractRouteDataWithTrafficAndRoadClassDeDuped(
            route,
            MapboxRouteLineUtils.getRouteLegTrafficCongestionProvider
        )

        assertEquals(19, result.size)
        assertEquals(478.70000000000005, result[7].distanceFromOrigin, 0.0)
        assertTrue(result[7].isLegOrigin)
        assertEquals(499.50000000000006, result[8].distanceFromOrigin, 0.0)
        assertFalse(result[8].isLegOrigin)
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
        val route = getMultilegRoute()

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
        val route = getMultilegRoute()

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
    fun calculateRouteLineSegments_when_styleInActiveRouteLegsIndependently() {
        val colors = RouteLineColorResources.Builder().build()
        val route = getMultilegWithTwoLegs()

        val result = MapboxRouteLineUtils.calculateRouteLineSegments(
            route,
            listOf(),
            true,
            colors
        )

        result.indexOfFirst { it.legIndex == 1 }

        assertEquals(12, result.size)
        assertEquals(5, result.indexOfFirst { it.legIndex == 1 })
        assertEquals(0.48807892461540975, result[5].offset, 0.0)
    }

    @Test
    fun getRouteColorForCongestionPrimaryRouteCongestionLow() {
        val resources = RouteLineColorResources.Builder().build()

        val result = MapboxRouteLineUtils.getRouteColorForCongestion(
            RouteConstants.LOW_CONGESTION_VALUE,
            true,
            resources
        )

        assertEquals(resources.routeLowCongestionColor, result)
    }

    @Test
    fun getRouteColorForCongestionPrimaryRouteCongestionModerate() {
        val resources = RouteLineColorResources.Builder().build()

        val result = MapboxRouteLineUtils.getRouteColorForCongestion(
            RouteConstants.MODERATE_CONGESTION_VALUE,
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
                RouteConstants.HEAVY_CONGESTION_VALUE,
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
                RouteConstants.SEVERE_CONGESTION_VALUE,
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
                RouteConstants.UNKNOWN_CONGESTION_VALUE,
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
            RouteConstants.CLOSURE_CONGESTION_VALUE,
            true,
            resources
        )

        assertEquals(resources.routeClosureColor, result)
    }

    @Test
    fun getRouteColorForCongestionPrimaryRouteCongestionRestricted() {
        val resources = RouteLineColorResources.Builder().build()

        val result = MapboxRouteLineUtils.getRouteColorForCongestion(
            RouteConstants.RESTRICTED_CONGESTION_VALUE,
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
                RouteConstants.LOW_CONGESTION_VALUE,
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
                RouteConstants.MODERATE_CONGESTION_VALUE,
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
                RouteConstants.HEAVY_CONGESTION_VALUE,
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
                RouteConstants.SEVERE_CONGESTION_VALUE,
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
                RouteConstants.UNKNOWN_CONGESTION_VALUE,
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
            RouteConstants.RESTRICTED_CONGESTION_VALUE,
            false,
            resources
        )

        assertEquals(resources.alternativeRouteRestrictedRoadColor, result)
    }

    @Test
    fun getRestrictedRouteLegRangesTest() {
        val route = loadRoute("route-with-restrictions.json")
        val coordinates = LineString.fromPolyline(
            route.geometry() ?: "",
            Constants.PRECISION_6
        )

        val result = getRestrictedRouteLegRanges(route.legs()!!.first())

        assertEquals(2, result.size)
        assertEquals(37.971947, coordinates.coordinates()[result[0].first].latitude(), 0.0)
        assertEquals(-122.526159, coordinates.coordinates()[result[0].first].longitude(), 0.0)
        assertEquals(37.971947, coordinates.coordinates()[result[0].last].latitude(), 0.0)
        assertEquals(-122.526159, coordinates.coordinates()[result[0].last].longitude(), 0.0)
        assertEquals(37.972037, coordinates.coordinates()[result[1].first].latitude(), 0.0)
        assertEquals(-122.526951, coordinates.coordinates()[result[1].first].longitude(), 0.0)
        assertEquals(37.972037, coordinates.coordinates()[result[1].last].latitude(), 0.0)
        assertEquals(-122.526951, coordinates.coordinates()[result[1].last].longitude(), 0.0)
    }

    @Test
    fun getRouteColorForCongestionNonPrimaryRouteCongestionClosure() {
        val expectedColor = Color.parseColor("#ffcc00")
        val resources = RouteLineColorResources.Builder()
            .alternativeRouteClosureColor(expectedColor)
            .build()

        val result = MapboxRouteLineUtils.getRouteColorForCongestion(
            RouteConstants.CLOSURE_CONGESTION_VALUE,
            false,
            resources
        )

        assertEquals(resources.alternativeRouteClosureColor, result)
    }

    @Test
    fun buildWayPointFeatureCollection() {
        val route = getMultilegRoute()

        val result = MapboxRouteLineUtils.buildWayPointFeatureCollection(route)

        assertEquals(4, result.features()!!.size)
        assertEquals(
            Point.fromLngLat(-77.157347, 38.783004),
            result.features()!![0].geometry() as Point
        )
        assertEquals(
            Point.fromLngLat(-77.167276, 38.775717),
            result.features()!![1].geometry() as Point
        )
        assertEquals(
            Point.fromLngLat(-77.167276, 38.775717),
            result.features()!![2].geometry() as Point
        )
        assertEquals(
            Point.fromLngLat(-77.153468, 38.77091),
            result.features()!![3].geometry() as Point
        )
    }

    @Test
    fun getLayerVisibility() {
        mockkStatic("com.mapbox.maps.extension.style.layers.LayerUtils")
        val layer = mockk<Layer>(relaxed = true) {
            every { visibility } returns Visibility.VISIBLE
        }

        val style = mockk<Style> {
            every { isStyleLoaded } returns true
            every { getLayer("foobar") } returns layer
        }

        val result = MapboxRouteLineUtils.getLayerVisibility(style, "foobar")

        assertEquals(Visibility.VISIBLE, result)
        unmockkStatic("com.mapbox.maps.extension.style.layers.LayerUtils")
    }

    @Test
    fun getLayerVisibility_whenStyleNotLoaded() {
        val style = mockk<Style> {
            every { isStyleLoaded } returns false
        }

        val result = MapboxRouteLineUtils.getLayerVisibility(style, "foobar")

        assertNull(result)
    }

    @Test
    fun getLayerVisibility_whenLayerNotFound() {
        mockkStatic("com.mapbox.maps.extension.style.layers.LayerUtils")
        val style = mockk<Style> {
            every { isStyleLoaded } returns true
            every { getLayer("foobar") } returns null
        }

        val result = MapboxRouteLineUtils.getLayerVisibility(style, "foobar")

        assertNull(result)
        unmockkStatic("com.mapbox.maps.extension.style.layers.LayerUtils")
    }

    @Test
    fun parseRoutePoints() {
        val route = getMultilegRoute()

        val result = MapboxRouteLineUtils.parseRoutePoints(route)!!

        assertEquals(128, result.flatList.size)
        assertEquals(15, result.nestedList.flatten().size)
        assertEquals(result.flatList[1].latitude(), result.flatList[2].latitude(), 0.0)
        assertEquals(result.flatList[1].longitude(), result.flatList[2].longitude(), 0.0)
        assertEquals(result.flatList[126].latitude(), result.flatList[127].latitude(), 0.0)
        assertEquals(result.flatList[126].longitude(), result.flatList[127].longitude(), 0.0)
    }

    @Test
    fun getTrafficLineExpressionProducer() {
        val routeLineColorResources = RouteLineColorResources.Builder().build()
        val expectedPrimaryTrafficLineExpression = "[step, [line-progress], " +
            "[rgba, 0.0, 0.0, 0.0, 0.0], 0.0, [rgba, 86.0, 168.0, 251.0, 1.0], " +
            "0.9429639111009005, [rgba, 255.0, 149.0, 0.0, 1.0]]"
        val route = loadRoute("short_route.json")

        val result = MapboxRouteLineUtils.getTrafficLineExpressionProducer(
            route,
            routeLineColorResources,
            listOf(),
            true,
            0.0,
            Color.TRANSPARENT,
            routeLineColorResources.routeUnknownCongestionColor,
            false,
            0.0
        ).invoke()

        assertEquals(
            expectedPrimaryTrafficLineExpression,
            result.toString()
        )
    }

    @Test
    fun getTrafficLineExpressionProducer_whenUseSoftGradient() {
        val routeLineColorResources = RouteLineColorResources.Builder().build()
        val expectedPrimaryTrafficLineExpression = "[interpolate, [linear], [line-progress], " +
            "0.0, [rgba, 86.0, 168.0, 251.0, 1.0], " +
            "0.6938979086102405, [rgba, 86.0, 168.0, 251.0, 1.0], " +
            "0.9429639111009005, [rgba, 255.0, 149.0, 0.0, 1.0]]"
        val route = loadRoute("short_route.json")

        val result = MapboxRouteLineUtils.getTrafficLineExpressionProducer(
            route,
            routeLineColorResources,
            listOf(),
            true,
            0.0,
            Color.TRANSPARENT,
            routeLineColorResources.routeUnknownCongestionColor,
            true,
            20.0
        ).invoke()

        assertEquals(
            expectedPrimaryTrafficLineExpression,
            result.toString()
        )
    }

    @Test
    fun getRouteLineExpression() {
        val expectedExpression = "[step, [line-progress], [rgba, 255.0, 0.0, 0.0, 1.0], 0.2," +
            " [rgba, 86.0, 168.0, 251.0, 1.0], 0.48807892461540975, " +
            "[rgba, 255.0, 255.0, 0.0, 1.0]]"
        val colorResources = RouteLineColorResources.Builder()
            .inActiveRouteLegsColor(Color.YELLOW)
            .build()
        val route = getMultilegWithTwoLegs()
        val segments = MapboxRouteLineUtils.calculateRouteLineSegments(
            route,
            listOf(),
            true,
            colorResources
        )

        val result = MapboxRouteLineUtils.getRouteLineExpression(
            .20,
            segments,
            Color.RED,
            colorResources.routeDefaultColor,
            colorResources.inActiveRouteLegsColor,
            0
        )

        assertEquals(expectedExpression, result.toString())
    }

    @Test
    fun getTrafficLineExpressionSoftGradient_multiLegRoute() {
        val expectedExpression = "[interpolate, [linear], [line-progress], 0.0, " +
            "[rgba, 86.0, 168.0, 251.0, 1.0], 0.0829948286268944, " +
            "[rgba, 86.0, 168.0, 251.0, 1.0], 0.10338667841237215, " +
            "[rgba, 255.0, 149.0, 0.0, 1.0], 0.10338667842237215, [rgba, 255.0, 149.0, 0.0, 1.0]," +
            " 0.1235746096999951, [rgba, 86.0, 168.0, 251.0, 1.0], 0.250513874614594, " +
            "[rgba, 86.0, 168.0, 251.0, 1.0], 0.27090572440007177, " +
            "[rgba, 255.0, 149.0, 0.0, 1.0], 0.3010856620825788, [rgba, 255.0, 149.0, 0.0, 1.0]," +
            " 0.32147751186805656, [rgba, 86.0, 168.0, 251.0, 1.0], 0.467687074829932, " +
            "[rgba, 86.0, 168.0, 251.0, 1.0], 0.48807892461540975, " +
            "[rgba, 143.0, 36.0, 71.0, 1.0], 0.4888945986068289, [rgba, 143.0, 36.0, 71.0, 1.0]," +
            " 0.5092864483923066, [rgba, 255.0, 149.0, 0.0, 1.0], 0.5317174831563323, " +
            "[rgba, 255.0, 149.0, 0.0, 1.0], 0.55210933294181, [rgba, 86.0, 168.0, 251.0, 1.0]," +
            " 0.5692384867616113, [rgba, 86.0, 168.0, 251.0, 1.0], 0.589630336547089, " +
            "[rgba, 255.0, 149.0, 0.0, 1.0], 0.5904460105385081, [rgba, 255.0, 149.0, 0.0, 1.0]," +
            " 0.6108378603239858, [rgba, 86.0, 168.0, 251.0, 1.0], 0.8839866882004601, " +
            "[rgba, 86.0, 168.0, 251.0, 1.0], 0.9043785379859378, [rgba, 255.0, 149.0, 0.0, 1.0]," +
            " 0.9329271276856067, [rgba, 255.0, 149.0, 0.0, 1.0], 0.9533189774710844, " +
            "[rgba, 86.0, 168.0, 251.0, 1.0]]"
        val colorResources = RouteLineColorResources.Builder().build()
        val route = getMultilegWithTwoLegs()
        val segments = MapboxRouteLineUtils.calculateRouteLineSegments(
            route,
            listOf(),
            true,
            colorResources
        )
        val stopGap = 20.0 / route.distance()

        val result = MapboxRouteLineUtils.getTrafficLineExpressionSoftGradient(
            0.0,
            Color.TRANSPARENT,
            colorResources.routeDefaultColor,
            stopGap,
            segments
        )

        assertEquals(expectedExpression, result.toString())
    }

    @Test
    fun getTrafficLineExpressionSoftGradient() {
        val expectedExpression = "[interpolate, [linear], [line-progress], " +
            "0.0, [rgba, 86.0, 168.0, 251.0, 1.0], " +
            "0.4532366552813495, [rgba, 86.0, 168.0, 251.0, 1.0], " +
            // notice this value (below) minus the stopGap value equals the previous value (above)
            "0.468779750455607, [rgba, 255.0, 149.0, 0.0, 1.0], " +
            "0.4877423265682011, [rgba, 255.0, 149.0, 0.0, 1.0], " +
            "0.5032854217424586, [rgba, 86.0, 168.0, 251.0, 1.0], " +
            "0.8454666620037381, [rgba, 86.0, 168.0, 251.0, 1.0], " +
            "0.8610097571779957, [rgba, 255.0, 149.0, 0.0, 1.0], " +
            "0.8766305678281243, [rgba, 255.0, 149.0, 0.0, 1.0], " +
            "0.8921736630023819, [rgba, 86.0, 168.0, 251.0, 1.0]]"
        val colorResources = RouteLineColorResources.Builder().build()
        val route = loadRoute("route-with-restrictions.json")
        val segments = MapboxRouteLineUtils.calculateRouteLineSegments(
            route,
            listOf(),
            true,
            colorResources
        )
        val stopGap = 20.0 / route.distance()

        val result = MapboxRouteLineUtils.getTrafficLineExpressionSoftGradient(
            0.0,
            Color.TRANSPARENT,
            colorResources.routeDefaultColor,
            stopGap,
            segments
        )

        assertEquals(expectedExpression, result.toString())
    }

    @Test
    fun getTrafficLineExpressionSoftGradient_offsetGreaterThanZero() {
        val expectedExpression = "[interpolate, [linear], [line-progress], 0.0, " +
            "[rgba, 0.0, 0.0, 0.0, 0.0], 0.46999999999, " +
            "[rgba, 0.0, 0.0, 0.0, 0.0], 0.47, " +
            "[rgba, 255.0, 149.0, 0.0, 1.0], 0.4877423265682011, " +
            "[rgba, 255.0, 149.0, 0.0, 1.0], 0.5032854217424586, " +
            "[rgba, 86.0, 168.0, 251.0, 1.0], 0.8454666620037381, " +
            "[rgba, 86.0, 168.0, 251.0, 1.0], 0.8610097571779957, " +
            "[rgba, 255.0, 149.0, 0.0, 1.0], 0.8766305678281243, " +
            "[rgba, 255.0, 149.0, 0.0, 1.0], 0.8921736630023819, " +
            "[rgba, 86.0, 168.0, 251.0, 1.0]]"
        val colorResources = RouteLineColorResources.Builder().build()
        val route = loadRoute("route-with-restrictions.json")
        val segments = MapboxRouteLineUtils.calculateRouteLineSegments(
            route,
            listOf(),
            true,
            colorResources
        )
        val stopGap = 20.0 / route.distance()

        val result = MapboxRouteLineUtils.getTrafficLineExpressionSoftGradient(
            0.47,
            Color.TRANSPARENT,
            colorResources.routeDefaultColor,
            stopGap,
            segments
        )

        assertEquals(expectedExpression, result.toString())
    }

    @Test
    fun getTrafficLineExpressionSoftGradient_whenStopGapOffsetGreaterThanItemOffset() {
        val expectedExpression = "[interpolate, [linear], [line-progress], " +
            "0.0, [rgba, 0.0, 0.0, 0.0, 0.0], " +
            "0.8454666619937382, [rgba, 0.0, 0.0, 0.0, 0.0], " +
            "0.8454666620037382, [rgba, 86.0, 168.0, 251.0, 1.0], " + // this is the value to notice
            "0.8454666620137382, [rgba, 86.0, 168.0, 251.0, 1.0], " + // this is the value to notice
            "0.8610097571779957, [rgba, 255.0, 149.0, 0.0, 1.0], " +
            "0.8766305678281243, [rgba, 255.0, 149.0, 0.0, 1.0], " +
            "0.8921736630023819, [rgba, 86.0, 168.0, 251.0, 1.0]]"
        val colorResources = RouteLineColorResources.Builder().build()
        val route = loadRoute("route-with-restrictions.json")
        val segments = MapboxRouteLineUtils.calculateRouteLineSegments(
            route,
            listOf(),
            true,
            colorResources
        )
        val stopGap = 20.0 / route.distance()

        val result = MapboxRouteLineUtils.getTrafficLineExpressionSoftGradient(
            0.8454666620037382,
            Color.TRANSPARENT,
            colorResources.routeDefaultColor,
            stopGap,
            segments
        )

        assertEquals(expectedExpression, result.toString())
    }

    @Test
    fun whenAnnotationIsCongestionNumericThenResolveLowCongestionNumeric() {
        val lowCongestionNumeric = 4
        val congestionResource = RouteLineColorResources.Builder().build()

        val result = MapboxRouteLineUtils.resolveNumericToValue(
            lowCongestionNumeric,
            congestionResource
        )

        assertEquals(RouteConstants.LOW_CONGESTION_VALUE, result)
    }

    @Test
    fun whenAnnotationIsCongestionNumericThenResolveModerateCongestionNumeric() {
        val moderateCongestionNumeric = 45
        val congestionResource = RouteLineColorResources.Builder().build()

        val result = MapboxRouteLineUtils.resolveNumericToValue(
            moderateCongestionNumeric,
            congestionResource
        )

        assertEquals(RouteConstants.MODERATE_CONGESTION_VALUE, result)
    }

    @Test
    fun whenAnnotationIsCongestionNumericThenResolveHeavyCongestionNumeric() {
        val heavyCongestionNumeric = 65
        val congestionResource = RouteLineColorResources.Builder().build()

        val result = MapboxRouteLineUtils.resolveNumericToValue(
            heavyCongestionNumeric,
            congestionResource
        )

        assertEquals(RouteConstants.HEAVY_CONGESTION_VALUE, result)
    }

    @Test
    fun whenAnnotatioIsCongestionNumericThenResolveSevereCongestionNumeric() {
        val severeCongestionNumeric = 85
        val congestionResource = RouteLineColorResources.Builder().build()

        val result = MapboxRouteLineUtils.resolveNumericToValue(
            severeCongestionNumeric,
            congestionResource
        )

        assertEquals(RouteConstants.SEVERE_CONGESTION_VALUE, result)
    }

    @Test
    fun whenAnnotationIsCongestionNumericThenResolveUnknownCongestionNumeric() {
        val unknownCongestionNumeric = null
        val congestionResource = RouteLineColorResources.Builder().build()

        val result = MapboxRouteLineUtils.resolveNumericToValue(
            unknownCongestionNumeric,
            congestionResource
        )

        assertEquals(RouteConstants.UNKNOWN_CONGESTION_VALUE, result)
    }

    @Test
    fun getRouteLineTrafficExpressionDataWithCongestionNumeric() {
        val colorResources = RouteLineColorResources.Builder().build()
        val routeAsJson = loadJsonFixture("route-with-congestion-numeric.json")
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
        val routeAsJson = loadJsonFixture(
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
        val routeAsJson = loadJsonFixture(
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
    fun getRouteLegTrafficCongestionProvider_cacheCheck() {
        val routeLeg = mockk<RouteLeg> {
            every { annotation() } returns mockk<LegAnnotation> {
                every { congestion() } returns listOf()
            }
        }

        MapboxRouteLineUtils.getRouteLegTrafficCongestionProvider(routeLeg)
        MapboxRouteLineUtils.getRouteLegTrafficCongestionProvider(routeLeg)
        MapboxRouteLineUtils.getRouteLegTrafficCongestionProvider(routeLeg)

        verify(exactly = 1) { routeLeg.annotation() }
    }

    @Test
    fun getRouteLegTrafficNumericCongestionProvider_cacheCheck() {
        val colorResources = RouteLineColorResources.Builder().build()

        val firstResult =
            MapboxRouteLineUtils.getRouteLegTrafficNumericCongestionProvider(colorResources)
        val secondResult =
            MapboxRouteLineUtils.getRouteLegTrafficNumericCongestionProvider(colorResources)
        val thirdResult =
            MapboxRouteLineUtils.getRouteLegTrafficNumericCongestionProvider(colorResources)

        assertEquals(firstResult, secondResult)
        assertEquals(secondResult, thirdResult)
    }

    @Test
    fun getAnnotationProvider_whenNumericTrafficSource() {
        val colorResources = RouteLineColorResources.Builder().build()
        val routeAsJson = loadJsonFixture(
            "route-with-congestion-numeric.json"
        )
        val route = DirectionsRoute.fromJson(routeAsJson)
        val expected =
            MapboxRouteLineUtils.getRouteLegTrafficNumericCongestionProvider(colorResources)

        val result =
            MapboxRouteLineUtils.getTrafficCongestionAnnotationProvider(route, colorResources)

        assertEquals(expected, result)
    }

    @Test
    fun getAnnotationProvider_whenNoRouteOptions() {
        val colorResources = RouteLineColorResources.Builder().build()
        val routeAsJson = loadJsonFixture(
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
    fun getRouteRestrictedSectionsExpressionData() {
        val route = loadRoute("route-with-restrictions.json")

        val result = MapboxRouteLineUtils.extractRouteData(
            route,
            MapboxRouteLineUtils.getRouteLegTrafficCongestionProvider
        )

        assertEquals(40, result.size)
        assertTrue(result.first().isLegOrigin)
        assertFalse(result[16].isInRestrictedSection)
        assertTrue(result[17].isInRestrictedSection)
        assertFalse(result[18].isInRestrictedSection)
        assertTrue(result[19].isInRestrictedSection)
        assertFalse(result[20].isInRestrictedSection)
    }

    @Test
    fun getRouteRestrictedSectionsExpressionData_multiLegRoute() {
        val route = loadRoute("two-leg-route-with-restrictions.json")

        val result = MapboxRouteLineUtils.extractRouteData(
            route,
            MapboxRouteLineUtils.getRouteLegTrafficCongestionProvider
        )

        assertEquals(45, result.size)
        assertTrue(result.first().isLegOrigin)
        assertFalse(result[1].isInRestrictedSection)
        assertTrue(result[2].isInRestrictedSection)
        assertTrue(result[3].isInRestrictedSection)
        assertTrue(result[4].isInRestrictedSection)
        assertFalse(result[5].isInRestrictedSection)
        assertTrue(result[17].isLegOrigin)
        assertFalse(result[37].isInRestrictedSection)
        assertTrue(result[38].isInRestrictedSection)
        assertFalse(result[39].isInRestrictedSection)
    }

    @Test
    fun extractRouteData_cacheCheck() {
        val route = mockk<DirectionsRoute> {
            every { legs() } returns null
        }
        val trafficCongestionProvider = MapboxRouteLineUtils.getRouteLegTrafficCongestionProvider
        MapboxRouteLineUtils.extractRouteData(route, trafficCongestionProvider)
        val result = MapboxRouteLineUtils.extractRouteData(route, trafficCongestionProvider)

        assertTrue(result.isEmpty())
        verify(exactly = 1) { route.legs() }
    }

    @Test
    fun `extractRouteData with null congestion provider`() {
        val route = loadRoute("short_route.json")
        for (data in MapboxRouteLineUtils.extractRouteData(route) { null }) {
            assertEquals(RouteConstants.UNKNOWN_CONGESTION_VALUE, data.trafficCongestionIdentifier)
        }
    }

    @Test
    fun `extractRouteData with empty congestion provider`() {
        val route = loadRoute("short_route.json")
        for (data in MapboxRouteLineUtils.extractRouteData(route) { emptyList() }) {
            assertEquals(RouteConstants.UNKNOWN_CONGESTION_VALUE, data.trafficCongestionIdentifier)
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
            RouteConstants.UNKNOWN_CONGESTION_VALUE,
            extractedData.last().trafficCongestionIdentifier,
        )
    }

    private fun getMultilegRoute(): DirectionsRoute {
        return loadRoute("multileg_route.json")
    }

    private fun getMultilegWithTwoLegs(): DirectionsRoute {
        val routeAsJson = loadJsonFixture("multileg-route-two-legs.json")
        return DirectionsRoute.fromJson(routeAsJson)
    }

    private fun loadRoute(routeFileName: String): DirectionsRoute {
        val routeAsJson = loadJsonFixture(routeFileName)
        return DirectionsRoute.fromJson(routeAsJson)
    }
}
