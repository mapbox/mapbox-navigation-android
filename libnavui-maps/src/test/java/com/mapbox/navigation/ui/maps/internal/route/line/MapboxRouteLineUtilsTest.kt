package com.mapbox.navigation.ui.maps.internal.route.line

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.HandlerThread
import androidx.test.core.app.ApplicationProvider
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.bindgen.Value
import com.mapbox.core.constants.Constants
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.LayerPosition
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
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineTrafficExpressionData
import com.mapbox.navigation.ui.maps.route.line.model.RouteStyleDescriptor
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.Assert
import org.junit.Assert.assertEquals
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
    fun getTrafficLineExpressionWithRouteRestrictions() {
        val expectedExpression = "[step, [line-progress], [rgba, 0.0, 0.0, 0.0, 0.0], 0.0, " +
            "[rgba, 255.0, 255.0, 247.0, 1.0], 0.2940753606969524, " +
            "[rgba, 255.0, 255.0, 255.0, 1.0], 0.39813638288860653, " +
            "[rgba, 255.0, 255.0, 247.0, 1.0], 0.44865144220494346, " +
            "[rgba, 0.0, 8.0, 73.0, 0.0], 0.45642298979207224, [rgba, 0.0, 0.0, 0.0, 0.0], " +
            "0.464194537379201, [rgba, 0.0, 8.0, 73.0, 0.0], 0.4765512980427357, " +
            "[rgba, 0.0, 0.0, 0.0, 0.0], 0.4843228456298645, [rgba, 0.0, 8.0, 73.0, 0.0], " +
            "0.4920943932169933, [rgba, 0.0, 0.0, 0.0, 0.0], 0.499865940804122, " +
            "[rgba, 0.0, 8.0, 73.0, 0.0], 0.5110569693295874, [rgba, 0.0, 0.0, 0.0, 0.0], " +
            "0.5188285169167162, [rgba, 0.0, 8.0, 73.0, 0.0], 0.526600064503845, " +
            "[rgba, 0.0, 0.0, 0.0, 0.0], 0.5343716120909737, [rgba, 0.0, 8.0, 73.0, 0.0], " +
            "0.5421431596781024, [rgba, 0.0, 0.0, 0.0, 0.0], 0.5429203144368153, " +
            "[rgba, 255.0, 255.0, 247.0, 1.0], 0.5502255691687163, " +
            "[rgba, 255.0, 255.0, 255.0, 1.0], 0.8240949061391339, " +
            "[rgba, 255.0, 255.0, 247.0, 1.0], 0.8610097571779957, [rgba, 0.0, 0.0, 33.0, 0.0]," +
            " 0.8921736630023819, [rgba, 255.0, 255.0, 255.0, 1.0], 0.9126128331565305, " +
            "[rgba, 255.0, 255.0, 247.0, 1.0], 0.9451756175466001, " +
            "[rgba, 255.0, 255.0, 255.0, 1.0]]"
        val colorResources = RouteLineColorResources.Builder()
            .routeUnknownTrafficColor(-9)
            .routeLowCongestionColor(-1)
            .routeCasingColor(33)
            .routeDefaultColor(33)
            .routeHeavyColor(33)
            .routeLineTraveledCasingColor(33)
            .routeLineTraveledColor(33)
            .routeModerateColor(33)
            .routeSevereColor(33)
            .restrictedRoadColor(2121)
            .build()
        val route = loadRoute("route-with-restrictions.json")
        val segments = MapboxRouteLineUtils.calculateRouteLineSegments(
            route,
            listOf(),
            true,
            colorResources,
            RouteConstants.RESTRICTED_ROAD_SECTION_SCALE,
            true
        )

        val result = MapboxRouteLineUtils.getTrafficLineExpression(
            0.0,
            Color.TRANSPARENT,
            -11097861,
            segments
        )

        assertEquals(expectedExpression, result.toString())
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
        }

        val result = MapboxRouteLineUtils.layersAreInitialized(style)

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
            every { styleSourceExists(RouteConstants.WAYPOINT_SOURCE_ID) } returns false
        }

        MapboxRouteLineUtils.initializeLayers(style, options)

        verify(exactly = 0) { style.styleLayers }
        verify(exactly = 0) { style.addStyleSource(any(), any()) }
    }

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
            .build()
        val waypointSourceValueSlot = slot<Value>()
        val primaryRouteSourceValueSlot = slot<Value>()
        val alternativeRoute1SourceValueSlot = slot<Value>()
        val alternativeRoute2SourceValueSlot = slot<Value>()
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
            every { addStyleLayer(any(), any()) } returns ExpectedFactory.createNone()
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
            style.addStyleLayer(capture(addStyleLayerSlots), capture(addStyleLayerPositionSlots))
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
            "mapbox-navigation-waypoint-layer",
            (addStyleLayerSlots[9].contents as HashMap<String, Value>)["id"]!!.contents
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

        val result = MapboxRouteLineUtils.getRouteLineTrafficExpressionData(route)

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

        val trafficExpressionData = MapboxRouteLineUtils.getRouteLineTrafficExpressionData(route)

        assertEquals(1, trafficExpressionData.count { it.distanceFromOrigin == 0.0 })
        assertEquals(0.0, trafficExpressionData[0].distanceFromOrigin, 0.0)
    }

    @Test
    fun getRouteLineTrafficExpressionWithRoadClassesDuplicatesRemoved() {
        val routeAsJsonJson = loadJsonFixture("route-with-road-classes.txt")
        val route = DirectionsRoute.fromJson(routeAsJsonJson)

        val result = MapboxRouteLineUtils.getRouteLineTrafficExpressionData(route)

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

        val result = MapboxRouteLineUtils.getRouteLineTrafficExpressionData(route)

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
            .routeUnknownTrafficColor(-9)
            .routeLowCongestionColor(-1)
            .routeCasingColor(33)
            .routeDefaultColor(33)
            .routeHeavyColor(33)
            .routeLineTraveledCasingColor(33)
            .routeLineTraveledColor(33)
            .routeModerateColor(33)
            .routeSevereColor(33)
            .build()

        val routeAsJsonJson = loadJsonFixture("motorway-route-with-road-classes.json")
        val route = DirectionsRoute.fromJson(routeAsJsonJson)

        val trafficExpressionData = MapboxRouteLineUtils.getRouteLineTrafficExpressionData(route)
        val result = MapboxRouteLineUtils.getRouteLineExpressionDataWithStreetClassOverride(
            trafficExpressionData,
            route.distance(),
            colorResources,
            true,
            listOf("motorway"),
            RouteConstants.RESTRICTED_ROAD_SECTION_SCALE,
            false
        )

        assertTrue(result.all { it.segmentColor == -1 })
        assertEquals(1, result.size)
    }

    @Test
    fun getRouteLineExpressionDataWithSomeRoadClassesDuplicatesRemoved() {
        val colorResources = RouteLineColorResources.Builder()
            .routeUnknownTrafficColor(-1)
            .routeLowCongestionColor(-1)
            .routeCasingColor(33)
            .routeDefaultColor(33)
            .routeHeavyColor(33)
            .routeLineTraveledCasingColor(33)
            .routeLineTraveledColor(33)
            .routeModerateColor(33)
            .routeSevereColor(33)
            .build()

        val routeAsJsonJson =
            loadJsonFixture("motorway-route-with-road-classes-mixed.json")
        val route = DirectionsRoute.fromJson(routeAsJsonJson)

        val trafficExpressionData = MapboxRouteLineUtils.getRouteLineTrafficExpressionData(route)
        val result = MapboxRouteLineUtils.getRouteLineExpressionDataWithStreetClassOverride(
            trafficExpressionData,
            route.distance(),
            colorResources,
            true,
            listOf("motorway"),
            RouteConstants.RESTRICTED_ROAD_SECTION_SCALE,
            false
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
            .routeUnknownTrafficColor(-9)
            .routeLowCongestionColor(-1)
            .routeCasingColor(33)
            .routeDefaultColor(33)
            .routeHeavyColor(33)
            .routeLineTraveledCasingColor(33)
            .routeLineTraveledColor(33)
            .routeModerateColor(33)
            .routeSevereColor(99)
            .routeClosureColor(-21)
            .build()

        val routeAsJsonJson = loadJsonFixture("route-with-closure.json")
        val route = DirectionsRoute.fromJson(routeAsJsonJson)

        val trafficExpressionData = MapboxRouteLineUtils.getRouteLineTrafficExpressionData(route)
        val result = MapboxRouteLineUtils.getRouteLineExpressionDataWithStreetClassOverride(
            trafficExpressionData,
            route.distance(),
            colorResources,
            true,
            listOf("tertiary"),
            RouteConstants.RESTRICTED_ROAD_SECTION_SCALE,
            true
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

        val result = MapboxRouteLineUtils.getRouteLineTrafficExpressionData(route)

        assertEquals(5, result.size)
        assertEquals(1188.7000000000003, result.last().distanceFromOrigin, 0.0)
        assertEquals(RouteConstants.LOW_CONGESTION_VALUE, result.last().trafficCongestionIdentifier)
        assertNull(result.last().roadClass)
    }

    @Test
    fun getRouteLineTrafficExpressionDataWithStreetClassesDuplicatesRemoved() {
        val colorResources = RouteLineColorResources.Builder()
            .routeUnknownTrafficColor(-9)
            .routeLowCongestionColor(-1)
            .routeCasingColor(33)
            .routeDefaultColor(33)
            .routeHeavyColor(33)
            .routeLineTraveledCasingColor(33)
            .routeLineTraveledColor(33)
            .routeModerateColor(33)
            .routeSevereColor(33)
            .build()

        val routeAsJsonJson = loadJsonFixture("route-with-road-classes.txt")
        val route = DirectionsRoute.fromJson(routeAsJsonJson)
        val trafficExpressionData = MapboxRouteLineUtils.getRouteLineTrafficExpressionData(route)
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
            listOf("street"),
            RouteConstants.RESTRICTED_ROAD_SECTION_SCALE,
            false
        )

        assertEquals(-9, result[0].segmentColor)
        assertEquals(7, result.size)
        assertEquals(0.016404052025563352, result[1].offset, 0.0)
        assertEquals(-1, result[1].segmentColor)
    }

    @Test
    fun getRouteLineExpressionDataWithStreetClassOverrideWhenDoesNotHaveStreetClasses() {
        val colorResources = RouteLineColorResources.Builder()
            .routeUnknownTrafficColor(-9)
            .routeLowCongestionColor(-1)
            .routeCasingColor(33)
            .routeDefaultColor(33)
            .routeHeavyColor(33)
            .routeLineTraveledCasingColor(33)
            .routeLineTraveledColor(33)
            .routeModerateColor(33)
            .routeSevereColor(33)
            .build()

        val routeAsJsonJson = loadJsonFixture("route-with-traffic-no-street-classes.txt")
        val route = DirectionsRoute.fromJson(routeAsJsonJson)
        val trafficExpressionData = MapboxRouteLineUtils.getRouteLineTrafficExpressionData(route)

        val result = MapboxRouteLineUtils.getRouteLineExpressionDataWithStreetClassOverride(
            trafficExpressionData,
            route.distance(),
            colorResources,
            true,
            listOf(),
            RouteConstants.RESTRICTED_ROAD_SECTION_SCALE,
            false
        )

        assertEquals(5, result.size)
        assertEquals(0.23460041526970057, result[1].offset, 0.0)
        assertEquals(-1, result[1].segmentColor)
    }

    @Test
    fun getTrafficExpressionWithStreetClassOverrideOnMotorwayWhenChangeOutsideOfIntersections() {
        val colorResources = RouteLineColorResources.Builder()
            .routeUnknownTrafficColor(-9)
            .routeLowCongestionColor(-1)
            .routeCasingColor(33)
            .routeDefaultColor(33)
            .routeHeavyColor(33)
            .routeLineTraveledCasingColor(33)
            .routeLineTraveledColor(33)
            .routeModerateColor(33)
            .routeSevereColor(-2)
            .build()

        val routeAsJsonJson = loadJsonFixture(
            "motorway-route-with-road-classes-unknown-not-on-intersection.json"
        )
        val route = DirectionsRoute.fromJson(routeAsJsonJson)

        val trafficExpressionData = MapboxRouteLineUtils.getRouteLineTrafficExpressionData(route)
        val result = MapboxRouteLineUtils.getRouteLineExpressionDataWithStreetClassOverride(
            trafficExpressionData,
            route.distance(),
            colorResources,
            true,
            listOf("motorway"),
            RouteConstants.RESTRICTED_ROAD_SECTION_SCALE,
            false
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

        val result = MapboxRouteLineUtils.getRouteLineTrafficExpressionData(route)

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
            .routeUnknownTrafficColor(-9)
            .routeLowCongestionColor(-1)
            .routeCasingColor(33)
            .routeDefaultColor(33)
            .routeHeavyColor(33)
            .routeLineTraveledCasingColor(33)
            .routeLineTraveledColor(33)
            .routeModerateColor(33)
            .routeSevereColor(33)
            .build()
        val routeAsJsonJson = loadJsonFixture(
            "motorway-with-road-classes-multi-leg.json"
        )
        val route = DirectionsRoute.fromJson(routeAsJsonJson)

        val trafficExpressionData = MapboxRouteLineUtils.getRouteLineTrafficExpressionData(route)
        val result = MapboxRouteLineUtils.getRouteLineExpressionDataWithStreetClassOverride(
            trafficExpressionData,
            route.distance(),
            colorResources,
            true,
            listOf("motorway"),
            RouteConstants.RESTRICTED_ROAD_SECTION_SCALE,
            false
        )

        assertTrue(result.all { it.segmentColor == -1 })
        assertEquals(2, result.size)
    }

    @Test
    fun getRouteLineExpressionDataWithStreetClassOverrideWhenHasRoadRestrictions() {
        val colorResources = RouteLineColorResources.Builder()
            .routeUnknownTrafficColor(-9)
            .routeLowCongestionColor(-1)
            .routeCasingColor(33)
            .routeDefaultColor(33)
            .routeHeavyColor(33)
            .routeLineTraveledCasingColor(33)
            .routeLineTraveledColor(33)
            .routeModerateColor(33)
            .routeSevereColor(33)
            .restrictedRoadColor(2121)
            .build()
        val route = loadRoute("route-with-restrictions.json")
        val trafficExpressionData = MapboxRouteLineUtils.getRouteLineTrafficExpressionData(route)

        val result = MapboxRouteLineUtils.getRouteLineExpressionDataWithStreetClassOverride(
            trafficExpressionData,
            route.distance(),
            colorResources,
            true,
            listOf(),
            RouteConstants.RESTRICTED_ROAD_SECTION_SCALE,
            true
        )

        assertEquals(result[2].segmentColor, -9)
        assertEquals(0.39813638288860653, result[2].offset, 0.0)
        assertEquals(2121, result[3].segmentColor)
        assertEquals(0.44865144220494346, result[3].offset, 0.0)
        assertEquals(0, result[4].segmentColor)
        assertEquals(0.45642298979207224, result[4].offset, 0.0)
        assertEquals(2121, result[5].segmentColor)
        assertEquals(0.464194537379201, result[5].offset, 0.0)
        assertEquals(0, result[6].segmentColor)
        assertEquals(0.4765512980427357, result[6].offset, 0.0)
        assertEquals(2121, result[7].segmentColor)
        assertEquals(0.4843228456298645, result[7].offset, 0.0)
        assertEquals(0, result[8].segmentColor)
        assertEquals(0.4920943932169933, result[8].offset, 0.0)
        assertEquals(2121, result[9].segmentColor)
        assertEquals(0.499865940804122, result[9].offset, 0.0)
        assertEquals(0, result[10].segmentColor)
        assertEquals(0.5110569693295874, result[10].offset, 0.0)
        assertEquals(2121, result[11].segmentColor)
        assertEquals(0.5188285169167162, result[11].offset, 0.0)
        assertEquals(0, result[12].segmentColor)
        assertEquals(0.526600064503845, result[12].offset, 0.0)
        assertEquals(2121, result[13].segmentColor)
        assertEquals(0.5343716120909737, result[13].offset, 0.0)
        assertEquals(0, result[14].segmentColor)
        assertEquals(0.5421431596781024, result[14].offset, 0.0)
        assertEquals(-9, result[15].segmentColor)
        assertEquals(0.5429203144368153, result[15].offset, 0.0)
        assertEquals(-1, result[16].segmentColor)
        assertEquals(0.5502255691687163, result[16].offset, 0.0)
    }

    @Test
    fun getRouteLineExpressionDataWithStreetClassOverrideWhenHasRoadRestrictionsAtEnd() {
        val colorResources = RouteLineColorResources.Builder()
            .routeLowCongestionColor(-1)
            .routeHeavyColor(44)
            .routeModerateColor(33)
            .restrictedRoadColor(2121)
            .build()

        val trafficData = listOf(
            RouteLineTrafficExpressionData(
                0.0,
                RouteConstants.LOW_CONGESTION_VALUE,
                null,
                false
            ),
            RouteLineTrafficExpressionData(
                500.0,
                RouteConstants.MODERATE_CONGESTION_VALUE,
                null,
                false
            ),
            RouteLineTrafficExpressionData(
                1000.0,
                RouteConstants.HEAVY_CONGESTION_VALUE,
                null,
                false
            ),
            RouteLineTrafficExpressionData(
                1500.0,
                RouteConstants.HEAVY_CONGESTION_VALUE,
                null,
                true
            )
        )
        val result = MapboxRouteLineUtils.getRouteLineExpressionDataWithStreetClassOverride(
            trafficData,
            2000.0,
            colorResources,
            true,
            listOf(),
            RouteConstants.RESTRICTED_ROAD_SECTION_SCALE,
            true
        )

        assertEquals(53, result.size)
        assertEquals(.25, result[1].offset, 0.0)
        assertEquals(33, result[1].segmentColor)
        assertEquals(.50, result[2].offset, 0.0)
        assertEquals(44, result[2].segmentColor)
        assertEquals(.75, result[3].offset, 0.0)
        assertEquals(2121, result[3].segmentColor)
        assertEquals(.995, result.last().offset, 0.0)
        assertEquals(0, result.last().segmentColor)
    }

    @Test
    fun getRouteLineTrafficExpressionDataWithClosures() {
        val routeAsJsonJson = loadJsonFixture("route-with-closure.json")
        val route = DirectionsRoute.fromJson(routeAsJsonJson)

        val trafficExpressionData = MapboxRouteLineUtils.getRouteLineTrafficExpressionData(route)

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

        val trafficExpressionData = MapboxRouteLineUtils.getRouteLineTrafficExpressionData(route)

        assertEquals(0.0, trafficExpressionData[0].distanceFromOrigin, 0.0)
        assertEquals(512.3, trafficExpressionData[3].distanceFromOrigin, 0.0)
        assertEquals("unknown", trafficExpressionData[3].trafficCongestionIdentifier)
        assertEquals(false, trafficExpressionData[3].isInRestrictedSection)
        assertEquals(577.3, trafficExpressionData[4].distanceFromOrigin, 0.0)
        assertEquals("low", trafficExpressionData[4].trafficCongestionIdentifier)
        assertEquals(true, trafficExpressionData[4].isInRestrictedSection)
        assertEquals(true, trafficExpressionData[5].isInRestrictedSection)
        assertEquals(true, trafficExpressionData[6].isInRestrictedSection)
        assertEquals(698.5999999999999, trafficExpressionData[7].distanceFromOrigin, 0.0)
        assertEquals("unknown", trafficExpressionData[7].trafficCongestionIdentifier)
        assertEquals(false, trafficExpressionData[7].isInRestrictedSection)
    }

    @Test
    fun getRouteLineTrafficExpressionData_whenFirstDistanceInSecondLegIsZero() {
        val route = getMultilegWithTwoLegs()
        val result = MapboxRouteLineUtils.getRouteLineTrafficExpressionData(route)

        assertEquals(20, result.size)
        assertEquals(478.70000000000005, result[7].distanceFromOrigin, 0.0)
        assertEquals(478.73487833756155, result[8].distanceFromOrigin, 0.0)
    }

    @Test
    fun calculateRouteLineSegmentsWithRouteRestrictions() {
        val colorResources = RouteLineColorResources.Builder()
            .routeUnknownTrafficColor(-9)
            .routeLowCongestionColor(-1)
            .routeCasingColor(33)
            .routeDefaultColor(33)
            .routeHeavyColor(33)
            .routeLineTraveledCasingColor(33)
            .routeLineTraveledColor(33)
            .routeModerateColor(33)
            .routeSevereColor(33)
            .restrictedRoadColor(2121)
            .build()
        val route = loadRoute("route-with-restrictions.json")

        val result = MapboxRouteLineUtils.calculateRouteLineSegments(
            route,
            listOf(),
            true,
            colorResources,
            RouteConstants.RESTRICTED_ROAD_SECTION_SCALE,
            true
        )

        assertEquals(2121, result[3].segmentColor)
        assertEquals(0.44865144220494346, result[3].offset, 0.0)
    }

    @Test
    fun calculateRouteLineSegmentsMultilegRoute() {
        val colorResources = RouteLineColorResources.Builder()
            .routeUnknownTrafficColor(-9)
            .routeLowCongestionColor(-1)
            .routeCasingColor(33)
            .routeDefaultColor(33)
            .routeHeavyColor(33)
            .routeLineTraveledCasingColor(33)
            .routeLineTraveledColor(33)
            .routeModerateColor(33)
            .routeSevereColor(33)
            .build()
        val route = getMultilegRoute()

        val result = MapboxRouteLineUtils.calculateRouteLineSegments(
            route,
            listOf(),
            true,
            colorResources,
            RouteConstants.RESTRICTED_ROAD_SECTION_SCALE,
            false
        )

        assertEquals(20, result.size)
        assertEquals(0.039793906743275334, result[1].offset, 0.0)
        assertEquals(0.9924011283895643, result.last().offset, 0.0)
    }

    @Test
    fun calculateRouteLineSegmentsMultilegRouteFirstDistanceValueAboveMinimumOffset() {
        val colorResources = RouteLineColorResources.Builder()
            .routeUnknownTrafficColor(-9)
            .routeLowCongestionColor(-1)
            .routeCasingColor(33)
            .routeDefaultColor(33)
            .routeHeavyColor(33)
            .routeLineTraveledCasingColor(33)
            .routeLineTraveledColor(33)
            .routeModerateColor(33)
            .routeSevereColor(33)
            .build()
        val route = getMultilegRoute()

        val result = MapboxRouteLineUtils.calculateRouteLineSegments(
            route,
            listOf(),
            true,
            colorResources,
            RouteConstants.RESTRICTED_ROAD_SECTION_SCALE,
            false
        )

        assertTrue(result[1].offset > .001f)
    }

    @Test
    fun calculateRouteLineSegments_whenNoTrafficExpressionData() {
        val colorResources = RouteLineColorResources.Builder().build()
        val route = mockk<DirectionsRoute> {
            every { legs() } returns listOf()
        }

        val result = MapboxRouteLineUtils.calculateRouteLineSegments(
            route,
            listOf(),
            true,
            colorResources,
            RouteConstants.RESTRICTED_ROAD_SECTION_SCALE,
            false
        )

        assertEquals(1, result.size)
        assertEquals(0.0, result[0].offset, 0.0)
        assertEquals(colorResources.routeDefaultColor, result[0].segmentColor)
    }

    @Test
    fun calculateRouteLineSegments_when_styleINactiveRouteLegsIndependently() {
        val colors = RouteLineColorResources.Builder().build()
        val route = getMultilegWithTwoLegs()

        val result = MapboxRouteLineUtils.calculateRouteLineSegments(
            route,
            listOf(),
            true,
            colors,
            1.0,
            true
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

        assertEquals(resources.routeModerateColor, result)
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

        assertEquals(resources.routeHeavyColor, result)
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

        assertEquals(resources.routeSevereColor, result)
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

        assertEquals(resources.routeUnknownTrafficColor, result)
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

        assertEquals(resources.alternativeRouteLowColor, result)
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

        assertEquals(resources.alternativeRouteModerateColor, result)
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

        assertEquals(resources.alternativeRouteHeavyColor, result)
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

        assertEquals(resources.alternativeRouteSevereColor, result)
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

        assertEquals(resources.alternativeRouteUnknownTrafficColor, result)
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
        assertEquals(37.971984, coordinates.coordinates()[result[0].last].latitude(), 0.0)
        assertEquals(-122.52645, coordinates.coordinates()[result[0].last].longitude(), 0.0)
        assertEquals(37.972037, coordinates.coordinates()[result[1].first].latitude(), 0.0)
        assertEquals(-122.526951, coordinates.coordinates()[result[1].first].longitude(), 0.0)
        assertEquals(37.972061, coordinates.coordinates()[result[1].last].latitude(), 0.0)
        assertEquals(-122.527206, coordinates.coordinates()[result[1].last].longitude(), 0.0)
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
            listOf(),
            routeLineColorResources,
            true,
            0.0,
            Color.TRANSPARENT,
            routeLineColorResources.routeUnknownTrafficColor,
            0.0
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
            colorResources,
            RouteConstants.RESTRICTED_ROAD_SECTION_SCALE,
            false
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
