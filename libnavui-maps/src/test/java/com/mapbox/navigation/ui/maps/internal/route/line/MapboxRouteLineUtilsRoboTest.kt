package com.mapbox.navigation.ui.maps.internal.route.line

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.LegAnnotation
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.bindgen.Value
import com.mapbox.core.constants.Constants
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.LayerPosition
import com.mapbox.maps.Style
import com.mapbox.maps.StyleObjectInfo
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor
import com.mapbox.maps.extension.style.layers.properties.generated.IconPitchAlignment
import com.mapbox.maps.plugin.locationcomponent.LocationComponentConstants
import com.mapbox.navigation.testing.FileUtils.loadJsonFixture
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.ARROW_HEAD_ICON
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.ARROW_HEAD_ICON_CASING
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.BOTTOM_LEVEL_ROUTE_LINE_LAYER_ID
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.DEFAULT_ROUTE_SOURCES_TOLERANCE
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.DESTINATION_MARKER_NAME
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
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.ORIGIN_MARKER_NAME
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.TOP_LEVEL_ROUTE_LINE_LAYER_ID
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.WAYPOINT_LAYER_ID
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.WAYPOINT_SOURCE_ID
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineColorResources
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineExpressionData
import com.mapbox.navigation.ui.maps.testing.TestingUtil.loadRoute
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.ArrayList

@RunWith(RobolectricTestRunner::class)
class MapboxRouteLineUtilsRoboTest {

    private lateinit var ctx: Context

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun initializeLayers() {
        mockkStatic("com.mapbox.maps.extension.style.layers.LayerUtils")
        val options = MapboxRouteLineOptions.Builder(ctx)
            .withRouteLineBelowLayerId(LocationComponentConstants.MODEL_LAYER)
            .displayRestrictedRoadSections(true)
            .waypointLayerIconAnchor(IconAnchor.BOTTOM_RIGHT)
            .waypointLayerIconOffset(listOf(33.3, 44.4))
            .iconPitchAlignment(IconPitchAlignment.VIEWPORT)
            .build()
        val waypointSourceValueSlots = mutableListOf<Value>()
        val primaryRouteSourceValueSlots = mutableListOf<Value>()
        val alternativeRoute1SourceValueSlots = mutableListOf<Value>()
        val alternativeRoute2SourceValueSlots = mutableListOf<Value>()
        val addStyleLayerSlots = mutableListOf<Value>()
        val addStyleLayerPositionSlots = mutableListOf<LayerPosition>()
        val mockLayer = mockk<StyleObjectInfo> {
            every { id } returns LocationComponentConstants.MODEL_LAYER
        }
        val style = mockk<Style> {
            every { styleLayers } returns listOf(mockLayer)
            every { styleSourceExists(LAYER_GROUP_1_SOURCE_ID) } returns false
            every { styleSourceExists(LAYER_GROUP_2_SOURCE_ID) } returns false
            every { styleSourceExists(LAYER_GROUP_3_SOURCE_ID) } returns false
            every { styleSourceExists(WAYPOINT_SOURCE_ID) } returns false
            every { styleLayerExists(LAYER_GROUP_1_TRAIL_CASING) } returns false
            every {
                styleLayerExists(LAYER_GROUP_1_TRAIL)
            } returns false
            every {
                styleLayerExists(LAYER_GROUP_1_CASING)
            } returns false
            every {
                styleLayerExists(LAYER_GROUP_1_MAIN)
            } returns false
            every {
                styleLayerExists(LAYER_GROUP_1_TRAFFIC)
            } returns false
            every {
                styleLayerExists(LAYER_GROUP_1_RESTRICTED)
            } returns false
            every { styleLayerExists(LAYER_GROUP_2_TRAIL_CASING) } returns false
            every {
                styleLayerExists(LAYER_GROUP_2_TRAIL)
            } returns false
            every {
                styleLayerExists(LAYER_GROUP_2_CASING)
            } returns false
            every {
                styleLayerExists(LAYER_GROUP_2_MAIN)
            } returns false
            every {
                styleLayerExists(LAYER_GROUP_2_TRAFFIC)
            } returns false
            every {
                styleLayerExists(LAYER_GROUP_2_RESTRICTED)
            } returns false
            every { styleLayerExists(LAYER_GROUP_3_TRAIL_CASING) } returns false
            every {
                styleLayerExists(LAYER_GROUP_3_TRAIL)
            } returns false
            every {
                styleLayerExists(LAYER_GROUP_3_CASING)
            } returns false
            every {
                styleLayerExists(LAYER_GROUP_3_MAIN)
            } returns false
            every {
                styleLayerExists(LAYER_GROUP_3_TRAFFIC)
            } returns false
            every {
                styleLayerExists(LAYER_GROUP_3_RESTRICTED)
            } returns false
            every {
                styleLayerExists(BOTTOM_LEVEL_ROUTE_LINE_LAYER_ID)
            } returns false
            every {
                styleLayerExists(TOP_LEVEL_ROUTE_LINE_LAYER_ID)
            } returns false
            every { getStyleImage(ARROW_HEAD_ICON) } returns null
            every { getStyleImage(ARROW_HEAD_ICON_CASING) } returns null
            every { getStyleImage(ORIGIN_MARKER_NAME) } returns null
            every { getStyleImage(DESTINATION_MARKER_NAME) } returns null
            every { styleLayerExists(WAYPOINT_LAYER_ID) } returns false
            every { styleLayerExists(LocationComponentConstants.MODEL_LAYER) } returns true
            every {
                addStyleSource(WAYPOINT_SOURCE_ID, any())
            } returns ExpectedFactory.createNone()
            every {
                addStyleSource(LAYER_GROUP_1_SOURCE_ID, any())
            } returns ExpectedFactory.createNone()
            every {
                addStyleSource(LAYER_GROUP_2_SOURCE_ID, any())
            } returns ExpectedFactory.createNone()
            every {
                addStyleSource(LAYER_GROUP_3_SOURCE_ID, any())
            } returns ExpectedFactory.createNone()
            every { addPersistentStyleLayer(any(), any()) } returns ExpectedFactory.createNone()
            every {
                addImage(ORIGIN_MARKER_NAME, any<Bitmap>())
            } returns ExpectedFactory.createNone()
            every {
                addImage(DESTINATION_MARKER_NAME, any<Bitmap>())
            } returns ExpectedFactory.createNone()
        }

        MapboxRouteLineUtils.initializeLayers(style, options)

        verify {
            style.addStyleSource(
                WAYPOINT_SOURCE_ID, capture(waypointSourceValueSlots),
            )
        }
        assertEquals(
            "geojson",
            (waypointSourceValueSlots.last().contents as HashMap<String, Value>)["type"]!!.contents,
        )
        assertEquals(
            16L,
            (waypointSourceValueSlots.last().contents as HashMap<String, Value>)["maxzoom"]!!
                .contents
        )
        assertEquals(
            "",
            (waypointSourceValueSlots.last().contents as HashMap<String, Value>)["data"]!!.contents,
        )
        assertEquals(
            DEFAULT_ROUTE_SOURCES_TOLERANCE,
            (waypointSourceValueSlots.last().contents as HashMap<String, Value>)
            ["tolerance"]!!.contents
        )

        verify {
            style.addStyleSource(
                LAYER_GROUP_1_SOURCE_ID,
                capture(primaryRouteSourceValueSlots),
            )
        }
        assertEquals(
            "geojson",
            (primaryRouteSourceValueSlots.last().contents as HashMap<String, Value>)["type"]
            !!.contents
        )
        assertEquals(
            16L,
            (primaryRouteSourceValueSlots.last().contents as HashMap<String, Value>)["maxzoom"]
            !!.contents
        )
        assertEquals(
            true,
            (primaryRouteSourceValueSlots.last().contents as HashMap<String, Value>)["lineMetrics"]
            !!.contents
        )
        assertEquals(
            "",
            (primaryRouteSourceValueSlots.last().contents as HashMap<String, Value>)["data"]
            !!.contents
        )
        assertEquals(
            DEFAULT_ROUTE_SOURCES_TOLERANCE,
            (primaryRouteSourceValueSlots.last().contents as HashMap<String, Value>)
            ["tolerance"]!!.contents
        )

        verify {
            style.addStyleSource(
                LAYER_GROUP_2_SOURCE_ID,
                capture(alternativeRoute1SourceValueSlots),
            )
        }
        assertEquals(
            "geojson",
            (alternativeRoute1SourceValueSlots.last().contents as HashMap<String, Value>)
            ["type"]!!.contents
        )
        assertEquals(
            16L,
            (alternativeRoute1SourceValueSlots.last().contents as HashMap<String, Value>)
            ["maxzoom"]!!.contents
        )
        assertEquals(
            true,
            (alternativeRoute1SourceValueSlots.last().contents as HashMap<String, Value>)
            ["lineMetrics"]!!.contents
        )
        assertEquals(
            "",
            (alternativeRoute1SourceValueSlots.last().contents as HashMap<String, Value>)
            ["data"]!!.contents
        )
        assertEquals(
            DEFAULT_ROUTE_SOURCES_TOLERANCE,
            (alternativeRoute1SourceValueSlots.last().contents as HashMap<String, Value>)
            ["tolerance"]!!.contents
        )

        verify {
            style.addStyleSource(
                LAYER_GROUP_3_SOURCE_ID,
                capture(alternativeRoute2SourceValueSlots),
            )
        }
        assertEquals(
            "geojson",
            (alternativeRoute2SourceValueSlots.last().contents as HashMap<String, Value>)
            ["type"]!!.contents
        )
        assertEquals(
            16L,
            (alternativeRoute2SourceValueSlots.last().contents as HashMap<String, Value>)
            ["maxzoom"]!!.contents
        )
        assertEquals(
            true,
            (alternativeRoute2SourceValueSlots.last().contents as HashMap<String, Value>)
            ["lineMetrics"]!!.contents
        )
        assertEquals(
            "",
            (alternativeRoute2SourceValueSlots.last().contents as HashMap<String, Value>)
            ["data"]!!.contents
        )
        assertEquals(
            DEFAULT_ROUTE_SOURCES_TOLERANCE,
            (alternativeRoute2SourceValueSlots.last().contents as HashMap<String, Value>)
            ["tolerance"]!!.contents
        )

        verify {
            style.addPersistentStyleLayer(
                capture(addStyleLayerSlots),
                capture(addStyleLayerPositionSlots)
            )
        }
        assertEquals(
            "mapbox-bottom-level-route-layer",
            (addStyleLayerSlots[0].contents as HashMap<String, Value>)["id"]!!.contents
        )
        assertEquals(
            "mapbox-layerGroup-3-trailCasing",
            (addStyleLayerSlots[1].contents as HashMap<String, Value>)["id"]!!.contents
        )
        assertEquals(
            "mapbox-layerGroup-3-trail",
            (addStyleLayerSlots[2].contents as HashMap<String, Value>)["id"]!!.contents
        )
        assertEquals(
            "mapbox-layerGroup-3-casing",
            (addStyleLayerSlots[3].contents as HashMap<String, Value>)["id"]!!.contents
        )
        assertEquals(
            "mapbox-layerGroup-3-main",
            (addStyleLayerSlots[4].contents as HashMap<String, Value>)["id"]!!.contents
        )
        assertEquals(
            "mapbox-layerGroup-3-traffic",
            (addStyleLayerSlots[5].contents as HashMap<String, Value>)["id"]!!.contents
        )
        assertEquals(
            "mapbox-layerGroup-3-restricted",
            (addStyleLayerSlots[6].contents as HashMap<String, Value>)["id"]!!.contents
        )
        assertEquals(
            "mapbox-layerGroup-2-trailCasing",
            (addStyleLayerSlots[7].contents as HashMap<String, Value>)["id"]!!.contents
        )
        assertEquals(
            "mapbox-layerGroup-2-trail",
            (addStyleLayerSlots[8].contents as HashMap<String, Value>)["id"]!!.contents
        )
        assertEquals(
            "mapbox-layerGroup-2-casing",
            (addStyleLayerSlots[9].contents as HashMap<String, Value>)["id"]!!.contents
        )
        assertEquals(
            "mapbox-layerGroup-2-main",
            (addStyleLayerSlots[10].contents as HashMap<String, Value>)["id"]!!.contents
        )
        assertEquals(
            "mapbox-layerGroup-2-traffic",
            (addStyleLayerSlots[11].contents as HashMap<String, Value>)["id"]!!.contents
        )
        assertEquals(
            "mapbox-layerGroup-2-restricted",
            (addStyleLayerSlots[12].contents as HashMap<String, Value>)["id"]!!.contents
        )
        assertEquals(
            "mapbox-layerGroup-1-trailCasing",
            (addStyleLayerSlots[13].contents as HashMap<String, Value>)["id"]!!.contents
        )
        assertEquals(
            "mapbox-layerGroup-1-trail",
            (addStyleLayerSlots[14].contents as HashMap<String, Value>)["id"]!!.contents
        )
        assertEquals(
            "mapbox-layerGroup-1-casing",
            (addStyleLayerSlots[15].contents as HashMap<String, Value>)["id"]!!.contents
        )
        assertEquals(
            "mapbox-layerGroup-1-main",
            (addStyleLayerSlots[16].contents as HashMap<String, Value>)["id"]!!.contents
        )
        assertEquals(
            "mapbox-layerGroup-1-traffic",
            (addStyleLayerSlots[17].contents as HashMap<String, Value>)["id"]!!.contents
        )
        assertEquals(
            "mapbox-layerGroup-1-restricted",
            (addStyleLayerSlots[18].contents as HashMap<String, Value>)["id"]!!.contents
        )
        assertEquals(
            "mapbox-top-level-route-layer",
            (addStyleLayerSlots[19].contents as HashMap<String, Value>)["id"]!!.contents
        )
        assertEquals(
            "mapbox-navigation-waypoint-layer",
            (addStyleLayerSlots[20].contents as HashMap<String, Value>)["id"]!!.contents
        )
        assertEquals(
            "bottom-right",
            (addStyleLayerSlots[20].contents as HashMap<String, Value>)["icon-anchor"]!!.contents
        )
        assertEquals(
            33.3,
            (
                (addStyleLayerSlots[20].contents as HashMap<String, Value>)["icon-offset"]
                !!.contents as ArrayList<Value>
                ).first().contents
        )
        assertEquals(
            44.4,
            (
                (addStyleLayerSlots[20].contents as HashMap<String, Value>)["icon-offset"]
                !!.contents as ArrayList<Value>
                ).component2().contents
        )
        assertEquals(
            "viewport",
            (addStyleLayerSlots[20].contents as HashMap<String, Value>)["icon-pitch-alignment"]
            !!.contents
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
        assertEquals(
            LocationComponentConstants.MODEL_LAYER,
            addStyleLayerPositionSlots[12].below
        )
        assertEquals(
            LocationComponentConstants.MODEL_LAYER,
            addStyleLayerPositionSlots[13].below
        )
        assertEquals(
            LocationComponentConstants.MODEL_LAYER,
            addStyleLayerPositionSlots[14].below
        )
        assertEquals(
            LocationComponentConstants.MODEL_LAYER,
            addStyleLayerPositionSlots[15].below
        )
        assertEquals(
            LocationComponentConstants.MODEL_LAYER,
            addStyleLayerPositionSlots[16].below
        )
        assertEquals(
            LocationComponentConstants.MODEL_LAYER,
            addStyleLayerPositionSlots[17].below
        )
        assertEquals(
            LocationComponentConstants.MODEL_LAYER,
            addStyleLayerPositionSlots[18].below
        )
        assertEquals(
            LocationComponentConstants.MODEL_LAYER,
            addStyleLayerPositionSlots[19].below
        )
        assertEquals(
            LocationComponentConstants.MODEL_LAYER,
            addStyleLayerPositionSlots[20].below
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
    fun findDistanceToNearestPointOnCurrentLine() {
        val route = loadRoute("multileg_route.json")
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
    fun calculateRouteLineSegments_when_styleInActiveRouteLegsIndependently() {
        val colors = RouteLineColorResources.Builder().build()
        val route = loadRoute("multileg-route-two-legs.json")

        val result = MapboxRouteLineUtils.calculateRouteLineSegments(
            route,
            listOf(),
            true,
            colors
        )

        result.indexOfFirst { it.legIndex == 1 }

        assertEquals(11, result.size)
        assertEquals(5, result.indexOfFirst { it.legIndex == 1 })
        assertEquals(0.48807892461540975, result[5].offset, 0.0)
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
        ).generateExpression()

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
        ).generateExpression()

        assertEquals(
            expectedPrimaryTrafficLineExpression,
            result.toString()
        )
    }

    @Test
    fun getTrafficLineExpression() {
        val routeLineColorResources = RouteLineColorResources.Builder().build()
        val expectedPrimaryTrafficLineExpression = "[step, [line-progress], " +
            "[rgba, 0.0, 0.0, 0.0, 0.0], 0.0, [rgba, 86.0, 168.0, 251.0, 1.0], " +
            "0.9425498931842539, [rgba, 255.0, 149.0, 0.0, 1.0], 1.0, " +
            "[rgba, 86.0, 168.0, 251.0, 1.0]]"

        val result = MapboxRouteLineUtils.getTrafficLineExpression(
            0.0,
            Color.TRANSPARENT,
            routeLineColorResources.routeUnknownCongestionColor,
            0.0,
            false,
            listOf(
                RouteLineExpressionData(0.0, routeLineColorResources.routeLowCongestionColor, 0),
                RouteLineExpressionData(
                    offset = 0.9425498931842539,
                    segmentColor = routeLineColorResources.routeModerateCongestionColor,
                    legIndex = 0
                ),
                RouteLineExpressionData(1.0, routeLineColorResources.routeLowCongestionColor, 0),
            )
        )

        assertEquals(
            expectedPrimaryTrafficLineExpression,
            result.toString()
        )
    }

    @Test
    fun `getTrafficLineExpression when duplicate point`() {
        val routeLineColorResources = RouteLineColorResources.Builder().build()
        val expectedPrimaryTrafficLineExpression = "[step, [line-progress], " +
            "[rgba, 0.0, 0.0, 0.0, 0.0], 0.0, " +
            "[rgba, 86.0, 168.0, 251.0, 1.0], 0.10373821458415478, " +
            "[rgba, 255.0, 149.0, 0.0, 1.0], 0.1240124365711821, " +
            "[rgba, 86.0, 168.0, 251.0, 1.0], 0.2718982903427929, " +
            "[rgba, 255.0, 149.0, 0.0, 1.0], 0.32264099467350016, " +
            "[rgba, 86.0, 168.0, 251.0, 1.0], 0.4897719974699625, " +
            // this is the moderate color at the start of the second leg
            // even though it's preceded by a duplicate 'severe' point which is ignored
            "[rgba, 255.0, 149.0, 0.0, 1.0], 0.5421388243827154, " +
            "[rgba, 86.0, 168.0, 251.0, 1.0], 0.5710651139490561, " +
            "[rgba, 255.0, 149.0, 0.0, 1.0], 0.5916095976376619, " +
            "[rgba, 86.0, 168.0, 251.0, 1.0], 0.88674421638117, " +
            "[rgba, 255.0, 149.0, 0.0, 1.0], 0.9423002251348892, " +
            "[rgba, 86.0, 168.0, 251.0, 1.0]]"

        val result = MapboxRouteLineUtils.getTrafficLineExpression(
            vanishingPointOffset = 0.0,
            lineStartColor = Color.TRANSPARENT,
            lineColor = routeLineColorResources.routeUnknownCongestionColor,
            stopGap = 0.0,
            useSoftGradient = false,
            segments = listOf(
                RouteLineExpressionData(0.0, routeLineColorResources.routeLowCongestionColor, 0),
                RouteLineExpressionData(
                    0.10373821458415478,
                    routeLineColorResources.routeModerateCongestionColor,
                    0
                ),
                RouteLineExpressionData(
                    0.1240124365711821,
                    routeLineColorResources.routeLowCongestionColor,
                    0
                ),
                RouteLineExpressionData(
                    0.2718982903427929,
                    routeLineColorResources.routeModerateCongestionColor,
                    0
                ),
                RouteLineExpressionData(
                    0.32264099467350016,
                    routeLineColorResources.routeLowCongestionColor,
                    0
                ),

                RouteLineExpressionData(
                    0.4897719974699625,
                    routeLineColorResources.routeModerateCongestionColor,
                    1
                ),
                RouteLineExpressionData(
                    0.5421388243827154,
                    routeLineColorResources.routeLowCongestionColor,
                    1
                ),
                RouteLineExpressionData(
                    0.5710651139490561,
                    routeLineColorResources.routeModerateCongestionColor,
                    1
                ),
                RouteLineExpressionData(
                    0.5916095976376619,
                    routeLineColorResources.routeLowCongestionColor,
                    1
                ),
                RouteLineExpressionData(
                    0.88674421638117,
                    routeLineColorResources.routeModerateCongestionColor,
                    1
                ),
                RouteLineExpressionData(
                    0.9423002251348892,
                    routeLineColorResources.routeLowCongestionColor,
                    1
                ),
            )
        )

        assertEquals(
            expectedPrimaryTrafficLineExpression,
            result.toString()
        )
    }

    @Test
    fun `getTrafficLineExpression with classes override when duplicate point`() {
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
        val expectedPrimaryTrafficLineExpression = "[step, [line-progress], " +
            "[rgba, 0.0, 0.0, 0.0, 0.0], 0.0, " +
            "[rgba, 255.0, 255.0, 247.0, 1.0], 0.5688813850361385, " +
            "[rgba, 255.0, 255.0, 255.0, 1.0], 1.0, " +
            "[rgba, 255.0, 255.0, 247.0, 1.0]]"

        val result = MapboxRouteLineUtils.getTrafficLineExpression(
            vanishingPointOffset = 0.0,
            lineStartColor = Color.TRANSPARENT,
            lineColor = colorResources.routeUnknownCongestionColor,
            stopGap = 0.0,
            useSoftGradient = false,
            segments = listOf(
                RouteLineExpressionData(0.0, colorResources.routeLowCongestionColor, 0),
                RouteLineExpressionData(
                    offset = 0.5688813850361385,
                    segmentColor = colorResources.routeUnknownCongestionColor,
                    legIndex = 0
                ),
                RouteLineExpressionData(
                    offset = 0.5688813850361385,
                    segmentColor = colorResources.routeLowCongestionColor,
                    legIndex = 0
                ),
                RouteLineExpressionData(1.0, colorResources.routeUnknownCongestionColor, 0),
            )
        )

        assertEquals(
            expectedPrimaryTrafficLineExpression,
            result.toString()
        )
    }

    @Test
    fun getTrafficLineExpression_whenUseSoftGradient() {
        val routeLineColorResources = RouteLineColorResources.Builder().build()
        val expectedPrimaryTrafficLineExpression = "[interpolate, [linear], [line-progress], " +
            "0.0, [rgba, 86.0, 168.0, 251.0, 1.0], " +
            "0.6934838906942539, [rgba, 86.0, 168.0, 251.0, 1.0], " +
            "0.9425498931842539, [rgba, 255.0, 149.0, 0.0, 1.0], " +
            "0.9425498931942539, [rgba, 255.0, 149.0, 0.0, 1.0], " +
            "1.0, [rgba, 86.0, 168.0, 251.0, 1.0]]"

        val result = MapboxRouteLineUtils.getTrafficLineExpression(
            0.0,
            Color.TRANSPARENT,
            routeLineColorResources.routeUnknownCongestionColor,
            0.24906600249,
            true,
            listOf(
                RouteLineExpressionData(0.0, routeLineColorResources.routeLowCongestionColor, 0),
                RouteLineExpressionData(
                    0.9425498931842539,
                    routeLineColorResources.routeModerateCongestionColor,
                    0
                ),
                RouteLineExpressionData(
                    1.0,
                    routeLineColorResources.routeLowCongestionColor,
                    0
                ),
            )
        )

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
        val route = loadRoute("multileg-route-two-legs.json")
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
            " 0.32147751186805656, [rgba, 86.0, 168.0, 251.0, 1.0], 0.467687074829932," +
            " [rgba, 86.0, 168.0, 251.0, 1.0], 0.48807892461540975, " +
            "[rgba, 255.0, 149.0, 0.0, 1.0], 0.5198902102807551, [rgba, 255.0, 149.0, 0.0, 1.0]," +
            " 0.5402820600662328, [rgba, 86.0, 168.0, 251.0, 1.0], 0.548744677727206, " +
            "[rgba, 86.0, 168.0, 251.0, 1.0], 0.5691365275126837, [rgba, 255.0, 149.0, 0.0, 1.0]," +
            " 0.5692384867616113, [rgba, 255.0, 149.0, 0.0, 1.0], 0.589630336547089, " +
            "[rgba, 86.0, 168.0, 251.0, 1.0], 0.8632889606682003, " +
            "[rgba, 86.0, 168.0, 251.0, 1.0], 0.883680810453678, [rgba, 255.0, 149.0, 0.0, 1.0]," +
            " 0.9186528328357723, [rgba, 255.0, 149.0, 0.0, 1.0], 0.93904468262125, " +
            "[rgba, 86.0, 168.0, 251.0, 1.0]]"
        val colorResources = RouteLineColorResources.Builder().build()
        val route = loadRoute("multileg-route-two-legs.json")
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
    fun getTrafficLineExpressionSoftGradient_withExtremelySmallDistanceOffset() {
        val expectedExpression = "[interpolate, [linear], [line-progress], 0.0, " +
            // notice no stop added before the vanishing point
            "[rgba, 0.0, 0.0, 0.0, 0.0], 1.0267342531733E-12, " +
            "[rgba, 86.0, 168.0, 251.0, 1.0], 0.4532366552813495, " +
            "[rgba, 86.0, 168.0, 251.0, 1.0], 0.468779750455607, " +
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
            0.0000000000010267342531733,
            Color.TRANSPARENT,
            colorResources.routeDefaultColor,
            stopGap,
            segments
        )

        assertEquals(expectedExpression, result.toString())
    }

    @Test
    fun getTrafficLineExpressionSoftGradient_withOffsetEqualToVanishPointStopGap() {
        val expectedExpression = "[interpolate, [linear], [line-progress], 0.0, " +
            // notice no stop added before the vanishing point
            "[rgba, 0.0, 0.0, 0.0, 0.0], 1.0E-11, " +
            "[rgba, 86.0, 168.0, 251.0, 1.0], 0.4532366552813495, " +
            "[rgba, 86.0, 168.0, 251.0, 1.0], 0.468779750455607, " +
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
            MapboxRouteLineUtils.VANISH_POINT_STOP_GAP,
            Color.TRANSPARENT,
            colorResources.routeDefaultColor,
            stopGap,
            segments
        )

        assertEquals(expectedExpression, result.toString())
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
    fun resetExtractRouteDataCache() {
        val route1 = mockk<DirectionsRoute> {
            every { legs() } returns null
        }
        val trafficCongestionProvider = MapboxRouteLineUtils.getRouteLegTrafficCongestionProvider
        MapboxRouteLineUtils.extractRouteData(route1, trafficCongestionProvider)
        MapboxRouteLineUtils.extractRouteData(route1, trafficCongestionProvider)
        verify(exactly = 1) { route1.legs() }

        MapboxRouteLineUtils.resetCache()
        MapboxRouteLineUtils.extractRouteData(route1, trafficCongestionProvider)

        verify(exactly = 2) { route1.legs() }
    }
}
