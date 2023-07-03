package com.mapbox.navigation.ui.maps.internal.route.line

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.bindgen.Value
import com.mapbox.core.constants.Constants
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.geojson.utils.PolylineUtils
import com.mapbox.maps.LayerPosition
import com.mapbox.maps.Style
import com.mapbox.maps.StyleObjectInfo
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor
import com.mapbox.maps.extension.style.layers.properties.generated.IconPitchAlignment
import com.mapbox.maps.plugin.locationcomponent.LocationComponentConstants
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.testing.FileUtils.loadJsonFixture
import com.mapbox.navigation.testing.NativeRouteParserRule
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants
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
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.MASKING_LAYER_CASING
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.MASKING_LAYER_MAIN
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.MASKING_LAYER_RESTRICTED
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.MASKING_LAYER_TRAFFIC
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.MASKING_LAYER_TRAIL
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.MASKING_LAYER_TRAIL_CASING
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.ORIGIN_MARKER_NAME
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.TOP_LEVEL_ROUTE_LINE_LAYER_ID
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.WAYPOINT_LAYER_ID
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.WAYPOINT_SOURCE_ID
import com.mapbox.navigation.ui.maps.route.line.api.DoubleChecker
import com.mapbox.navigation.ui.maps.route.line.api.StringChecker
import com.mapbox.navigation.ui.maps.route.line.api.checkExpression
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineColorResources
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineExpressionData
import com.mapbox.navigation.ui.maps.testing.TestingUtil.loadNavigationRoute
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MapboxRouteLineUtilsRoboTest {

    private lateinit var ctx: Context

    @get:Rule
    val nativeRouteParserRule = NativeRouteParserRule()

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
    }

    @After
    fun tearDown() {
        unmockkAll()
        MapboxRouteLineUtils.trimRouteDataCacheToSize(0)
    }

    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    @Test
    fun initializeLayers() {
        mockkStatic("com.mapbox.maps.extension.style.layers.LayerUtils")
        val options = MapboxRouteLineOptions.Builder(ctx)
            .withRouteLineBelowLayerId(LocationComponentConstants.MODEL_LAYER)
            .displayRestrictedRoadSections(true)
            .waypointLayerIconAnchor(IconAnchor.BOTTOM_RIGHT)
            .waypointLayerIconOffset(listOf(33.3, 44.4))
            .iconPitchAlignment(IconPitchAlignment.VIEWPORT)
            .shareLineGeometrySources(true)
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
        val style = mockk<Style>(relaxed = true) {
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
            every {
                styleLayerExists(MASKING_LAYER_TRAIL_CASING)
            } returns false
            every {
                styleLayerExists(MASKING_LAYER_TRAIL)
            } returns false
            every {
                styleLayerExists(MASKING_LAYER_CASING)
            } returns false
            every {
                styleLayerExists(MASKING_LAYER_MAIN)
            } returns false
            every {
                styleLayerExists(MASKING_LAYER_TRAFFIC)
            } returns false
            every {
                styleLayerExists(MASKING_LAYER_RESTRICTED)
            } returns false
            every { getStyleImage(ARROW_HEAD_ICON) } returns null
            every { getStyleImage(ARROW_HEAD_ICON_CASING) } returns null
            every { getStyleImage(ORIGIN_MARKER_NAME) } returns null
            every { getStyleImage(DESTINATION_MARKER_NAME) } returns null
            every { styleLayerExists(WAYPOINT_LAYER_ID) } returns false
            every { styleLayerExists(LocationComponentConstants.MODEL_LAYER) } returns true
            every {
                addStyleSource(WAYPOINT_SOURCE_ID, capture(waypointSourceValueSlot))
            } returns ExpectedFactory.createNone()
            every {
                addStyleSource(LAYER_GROUP_1_SOURCE_ID, capture(primaryRouteSourceValueSlot))
            } returns ExpectedFactory.createNone()
            every {
                addStyleSource(LAYER_GROUP_2_SOURCE_ID, capture(alternativeRoute1SourceValueSlot))
            } returns ExpectedFactory.createNone()
            every {
                addStyleSource(LAYER_GROUP_3_SOURCE_ID, capture(alternativeRoute2SourceValueSlot))
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

        assertEquals(
            "geojson",
            (waypointSourceValueSlot.captured.contents as HashMap<String, Value>)
            ["type"]!!.contents,
        )
        assertEquals(
            16L,
            (waypointSourceValueSlot.captured.contents as HashMap<String, Value>)["maxzoom"]!!
                .contents
        )
        assertEquals(
            "",
            (waypointSourceValueSlot.captured.contents as HashMap<String, Value>)
            ["data"]!!.contents,
        )
        assertEquals(
            DEFAULT_ROUTE_SOURCES_TOLERANCE,
            (waypointSourceValueSlot.captured.contents as HashMap<String, Value>)
            ["tolerance"]!!.contents
        )
        assertEquals(
            false,
            (waypointSourceValueSlot.captured.contents as HashMap<String, Value>)
            ["sharedCache"]!!.contents
        )

        assertEquals(
            "geojson",
            (primaryRouteSourceValueSlot.captured.contents as HashMap<String, Value>)
            ["type"]!!.contents
        )
        assertEquals(
            16L,
            (primaryRouteSourceValueSlot.captured.contents as HashMap<String, Value>)
            ["maxzoom"]!!.contents
        )
        assertEquals(
            true,
            (primaryRouteSourceValueSlot.captured.contents as HashMap<String, Value>)
            ["lineMetrics"]!!.contents
        )
        assertEquals(
            "",
            (primaryRouteSourceValueSlot.captured.contents as HashMap<String, Value>)
            ["data"]!!.contents
        )
        assertEquals(
            DEFAULT_ROUTE_SOURCES_TOLERANCE,
            (primaryRouteSourceValueSlot.captured.contents as HashMap<String, Value>)
            ["tolerance"]!!.contents
        )
        assertEquals(
            true,
            (primaryRouteSourceValueSlot.captured.contents as HashMap<String, Value>)
            ["sharedCache"]!!.contents
        )

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
            "",
            (alternativeRoute1SourceValueSlot.captured.contents as HashMap<String, Value>)
            ["data"]!!.contents
        )
        assertEquals(
            DEFAULT_ROUTE_SOURCES_TOLERANCE,
            (alternativeRoute1SourceValueSlot.captured.contents as HashMap<String, Value>)
            ["tolerance"]!!.contents
        )
        assertEquals(
            true,
            (alternativeRoute1SourceValueSlot.captured.contents as HashMap<String, Value>)
            ["sharedCache"]!!.contents
        )

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
            "",
            (alternativeRoute2SourceValueSlot.captured.contents as HashMap<String, Value>)
            ["data"]!!.contents
        )
        assertEquals(
            DEFAULT_ROUTE_SOURCES_TOLERANCE,
            (alternativeRoute2SourceValueSlot.captured.contents as HashMap<String, Value>)
            ["tolerance"]!!.contents
        )
        assertEquals(
            true,
            (alternativeRoute2SourceValueSlot.captured.contents as HashMap<String, Value>)
            ["sharedCache"]!!.contents
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
            "mapbox-masking-layer-trailCasing",
            (addStyleLayerSlots[19].contents as HashMap<String, Value>)["id"]!!.contents
        )
        assertEquals(
            "mapbox-masking-layer-trail",
            (addStyleLayerSlots[20].contents as HashMap<String, Value>)["id"]!!.contents
        )
        assertEquals(
            "mapbox-masking-layer-casing",
            (addStyleLayerSlots[21].contents as HashMap<String, Value>)["id"]!!.contents
        )
        assertEquals(
            "mapbox-masking-layer-main",
            (addStyleLayerSlots[22].contents as HashMap<String, Value>)["id"]!!.contents
        )
        assertEquals(
            "mapbox-masking-layer-traffic",
            (addStyleLayerSlots[23].contents as HashMap<String, Value>)["id"]!!.contents
        )
        assertEquals(
            "mapbox-masking-layer-restricted",
            (addStyleLayerSlots[24].contents as HashMap<String, Value>)["id"]!!.contents
        )

        assertEquals(
            "mapbox-top-level-route-layer",
            (addStyleLayerSlots[25].contents as HashMap<String, Value>)["id"]!!.contents
        )
        assertEquals(
            "mapbox-navigation-waypoint-layer",
            (addStyleLayerSlots[26].contents as HashMap<String, Value>)["id"]!!.contents
        )
        assertEquals(
            "bottom-right",
            (addStyleLayerSlots[26].contents as HashMap<String, Value>)["icon-anchor"]!!.contents
        )
        assertEquals(
            33.3,
            (
                (addStyleLayerSlots[26].contents as HashMap<String, Value>)
                ["icon-offset"]!!.contents as ArrayList<Value>
                ).first().contents
        )
        assertEquals(
            44.4,
            (
                (addStyleLayerSlots[26].contents as HashMap<String, Value>)
                ["icon-offset"]!!.contents as ArrayList<Value>
                ).component2().contents
        )
        assertEquals(
            "viewport",
            (addStyleLayerSlots[26].contents as HashMap<String, Value>)
            ["icon-pitch-alignment"]!!.contents
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
        assertEquals(
            LocationComponentConstants.MODEL_LAYER,
            addStyleLayerPositionSlots[21].below
        )
        assertEquals(
            LocationComponentConstants.MODEL_LAYER,
            addStyleLayerPositionSlots[22].below
        )
        assertEquals(
            LocationComponentConstants.MODEL_LAYER,
            addStyleLayerPositionSlots[23].below
        )
        assertEquals(
            LocationComponentConstants.MODEL_LAYER,
            addStyleLayerPositionSlots[24].below
        )
        assertEquals(
            LocationComponentConstants.MODEL_LAYER,
            addStyleLayerPositionSlots[25].below
        )
    }

    @Test
    fun `calculateRouteGranularDistances - route distances`() {
        val route = loadNavigationRoute("short_route.json")

        val result = MapboxRouteLineUtils.granularDistancesProvider(route)!!

        assertEquals(
            PolylineUtils.decode(route.directionsRoute.geometry()!!, 6).size,
            result.routeDistances.size
        )
        assertEquals(5, result.routeDistances.size)
        assertEquals(Point.fromLngLat(-122.523671, 37.975379), result.routeDistances[0].point)
        assertEquals(0.0000025451727518618744, result.routeDistances[0].distanceRemaining, 0.0)
        assertEquals(Point.fromLngLat(-122.523117, 37.975107), result.routeDistances[3].point)
        assertEquals(0.00000014622044645899132, result.routeDistances[3].distanceRemaining, 0.0)
        assertEquals(Point.fromLngLat(-122.523131, 37.975067), result.routeDistances[4].point)
        assertEquals(0.0, result.routeDistances[4].distanceRemaining, 0.0)
    }

    @Test
    fun `calculateRouteGranularDistances - leg distances`() {
        val route = loadNavigationRoute("two-leg-route-with-restrictions.json")

        val result = MapboxRouteLineUtils.granularDistancesProvider(route)!!

        assertEquals(
            PolylineUtils.decode(route.directionsRoute.geometry()!!, 6).size,
            result.routeDistances.size
        )
        assertEquals(18, result.legsDistances[0].size)
        assertEquals(30, result.legsDistances[1].size)
        assertEquals(Point.fromLngLat(-122.523163, 37.974969), result.legsDistances[0][0].point)
        assertEquals(Point.fromLngLat(-122.524298, 37.970763), result.legsDistances[1][0].point)
        assertEquals(0.00003094931666768714, result.legsDistances[0][0].distanceRemaining, 0.0)
        assertEquals(0.000015791208023023606, result.legsDistances[1][0].distanceRemaining, 0.0)
        assertEquals(Point.fromLngLat(-122.523452, 37.974087), result.legsDistances[0][3].point)
        assertEquals(Point.fromLngLat(-122.523718, 37.970713), result.legsDistances[1][3].point)
        assertEquals(0.000027738689813981653, result.legsDistances[0][3].distanceRemaining, 0.0)
        assertEquals(0.000014170490514018856, result.legsDistances[1][3].distanceRemaining, 0.0)
        assertEquals(Point.fromLngLat(-122.524298, 37.970763), result.legsDistances[0][17].point)
        assertEquals(Point.fromLngLat(-122.518908, 37.970549), result.legsDistances[1][29].point)
        assertEquals(0.000015791208023023606, result.legsDistances[0][17].distanceRemaining, 0.0)
        assertEquals(0.0, result.legsDistances[1][29].distanceRemaining, 0.0)
    }

    @Test
    fun `calculateRouteGranularDistances with duplicate point`() {
        /**
         * this route contains a duplicate point somewhere in the middle of the first step.
         * Inspecting a decoded portion of the `LineString` presents this:
         * ```
         * ...
         *     [
         *       140.9184,
         *       37.718443
         *     ],
         *     [
         *       140.918069,
         *       37.719383
         *     ],
         *     [
         *       140.918069,
         *       37.719383
         *     ],
         *     [
         *       140.917924,
         *       37.719839
         *     ],
         * ...
         * ```
         * This point should not be filtered out.
         */
        val route = NavigationRoute.create(
            directionsResponseJson = loadJsonFixture(
                "route_response_duplicate_geometry_point.json"
            ),
            routeRequestUrl = RouteOptions.fromJson(
                loadJsonFixture("route_response_duplicate_geometry_point_url.json")
            ).toUrl("xyz").toString(),
            routerOrigin = RouterOrigin.Offboard
        )

        val result = MapboxRouteLineUtils.granularDistancesProvider(route.first())!!

        val routeGeometrySize =
            PolylineUtils.decode(route.first().directionsRoute.geometry()!!, 6).size
        assertEquals(
            routeGeometrySize,
            result.routeDistances.size
        )
        assertEquals(
            routeGeometrySize,
            result.legsDistances[0].size
        )
        assertEquals(1832, result.routeDistances.lastIndex)
        assertEquals(1832, result.legsDistances[0].lastIndex)
    }

    @Test
    fun findDistanceToNearestPointOnCurrentLine() {
        val route = loadNavigationRoute("multileg_route.json")
        val lineString = LineString.fromPolyline(
            route.directionsRoute.geometry()!!,
            Constants.PRECISION_6
        )
        val distances = MapboxRouteLineUtils.granularDistancesProvider(route)

        val result = MapboxRouteLineUtils.findDistanceToNearestPointOnCurrentLine(
            lineString.coordinates()[15],
            distances!!,
            upcomingIndex = 10
        )

        assertEquals(296.6434687878863, result, 0.0)
    }

    @Test
    fun calculateRouteLineSegments_when_styleInActiveRouteLegsIndependently() {
        val colors = RouteLineColorResources.Builder().build()
        val route = loadNavigationRoute("multileg-route-two-legs.json")

        val result = MapboxRouteLineUtils.calculateRouteLineSegments(
            route,
            listOf(),
            true,
            colors
        )

        assertEquals(12, result.size)
        assertEquals(5, result.indexOfFirst { it.legIndex == 1 })
        assertEquals(0.4897719974699625, result[5].offset, 0.0)
    }

    @Test
    fun getTrafficLineExpressionProducer() {
        val routeLineColorResources = RouteLineColorResources.Builder().build()
        val expectedPrimaryTrafficLineExpression = "[step, [line-progress], " +
            "[rgba, 0.0, 0.0, 0.0, 0.0], 0.0, [rgba, 86.0, 168.0, 251.0, 1.0], " +
            "0.9425498931842539, [rgba, 255.0, 149.0, 0.0, 1.0], 1.0, " +
            "[rgba, 86.0, 168.0, 251.0, 1.0]]"
        val route = loadNavigationRoute("short_route.json")

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

    /**
     * The route used here for testing produced an erroneous duplicate edge (a point in the geometry is duplicate).
     * This could cause an error when creating the
     * traffic expression because the values need to be in ascending order to create a
     * valid line gradient expression. This error won't occur in single leg routes and will
     * only occur in multileg routes when there is a traffic congestion change at the first point in
     * the leg. This is because duplicate traffic congestion values are dropped. The route
     * used in the test below has a traffic change at the first point in the second leg and
     * the distance annotation is 0.0 which would have caused an error prior to the fix this
     * test is checking for.
     * The duplicate point in this test has a 'severe' congestion set but it's expected to be overridden
     * by the value coming out of the second duplicate point to the next distinct point,
     * as a segment that has '0' distance cannot and should not be visualized.
     */
    @Test
    fun `getTrafficLineExpressionProducer when duplicate point`() {
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
        val route = loadNavigationRoute("multileg-route-two-legs.json")

        val result = MapboxRouteLineUtils.getTrafficLineExpressionProducer(
            route,
            routeLineColorResources,
            trafficBackfillRoadClasses = listOf(),
            isPrimaryRoute = true,
            vanishingPointOffset = 0.0,
            lineStartColor = Color.TRANSPARENT,
            lineColor = routeLineColorResources.routeUnknownCongestionColor,
            useSoftGradient = false,
            softGradientTransitionDistance = 0.0
        ).generateExpression()

        assertEquals(
            expectedPrimaryTrafficLineExpression,
            result.toString()
        )
    }

    /**
     * The route used here for testing produced an erroneous duplicate edge (a point in the geometry is duplicate).
     * This could cause an error when creating the
     * traffic expression because the values need to be in ascending order to create a
     * valid line gradient expression. This error won't occur in single leg routes and will
     * only occur in multileg routes when there is a traffic congestion change at the first point in
     * the leg. This is because duplicate traffic congestion values are dropped. The route
     * used in the test below has a traffic change at the first point in the second leg and
     * the distance annotation is 0.0 which would have caused an error prior to the fix this
     * test is checking for.
     * The duplicate point in this test is caused by a multi leg route on a motorway with full class override.
     */
    @Test
    fun `getTrafficLineExpressionProducer with classes override when duplicate point`() {
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
        val route = loadNavigationRoute("motorway-with-road-classes-multi-leg.json")

        val result = MapboxRouteLineUtils.getTrafficLineExpressionProducer(
            route,
            colorResources,
            trafficBackfillRoadClasses = listOf("motorway"),
            isPrimaryRoute = true,
            vanishingPointOffset = 0.0,
            lineStartColor = Color.TRANSPARENT,
            lineColor = colorResources.routeUnknownCongestionColor,
            useSoftGradient = false,
            softGradientTransitionDistance = 0.0
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
            "0.6934838906935938, [rgba, 86.0, 168.0, 251.0, 1.0], " +
            "0.9425498931842539, [rgba, 255.0, 149.0, 0.0, 1.0]]"
        val route = loadNavigationRoute("short_route.json")

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
                RouteLineExpressionData(
                    offset = 0.0,
                    congestionValue = RouteLayerConstants.LOW_CONGESTION_VALUE,
                    segmentColor = routeLineColorResources.routeLowCongestionColor,
                    legIndex = 0
                ),
                RouteLineExpressionData(
                    offset = 0.9425498931842539,
                    congestionValue = RouteLayerConstants.MODERATE_CONGESTION_VALUE,
                    segmentColor = routeLineColorResources.routeModerateCongestionColor,
                    legIndex = 0
                ),
                RouteLineExpressionData(
                    offset = 1.0,
                    congestionValue = RouteLayerConstants.LOW_CONGESTION_VALUE,
                    segmentColor = routeLineColorResources.routeLowCongestionColor,
                    legIndex = 0
                ),
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
                RouteLineExpressionData(
                    0.0,
                    congestionValue = RouteLayerConstants.LOW_CONGESTION_VALUE,
                    routeLineColorResources.routeLowCongestionColor,
                    0
                ),
                RouteLineExpressionData(
                    0.10373821458415478,
                    congestionValue = RouteLayerConstants.MODERATE_CONGESTION_VALUE,
                    routeLineColorResources.routeModerateCongestionColor,
                    0
                ),
                RouteLineExpressionData(
                    0.1240124365711821,
                    congestionValue = RouteLayerConstants.LOW_CONGESTION_VALUE,
                    routeLineColorResources.routeLowCongestionColor,
                    0
                ),
                RouteLineExpressionData(
                    0.2718982903427929,
                    congestionValue = RouteLayerConstants.MODERATE_CONGESTION_VALUE,
                    routeLineColorResources.routeModerateCongestionColor,
                    0
                ),
                RouteLineExpressionData(
                    0.32264099467350016,
                    congestionValue = RouteLayerConstants.LOW_CONGESTION_VALUE,
                    routeLineColorResources.routeLowCongestionColor,
                    0
                ),

                RouteLineExpressionData(
                    0.4897719974699625,
                    congestionValue = RouteLayerConstants.MODERATE_CONGESTION_VALUE,
                    routeLineColorResources.routeModerateCongestionColor,
                    1
                ),
                RouteLineExpressionData(
                    0.5421388243827154,
                    congestionValue = RouteLayerConstants.LOW_CONGESTION_VALUE,
                    routeLineColorResources.routeLowCongestionColor,
                    1
                ),
                RouteLineExpressionData(
                    0.5710651139490561,
                    congestionValue = RouteLayerConstants.MODERATE_CONGESTION_VALUE,
                    routeLineColorResources.routeModerateCongestionColor,
                    1
                ),
                RouteLineExpressionData(
                    0.5916095976376619,
                    congestionValue = RouteLayerConstants.LOW_CONGESTION_VALUE,
                    routeLineColorResources.routeLowCongestionColor,
                    1
                ),
                RouteLineExpressionData(
                    0.88674421638117,
                    congestionValue = RouteLayerConstants.MODERATE_CONGESTION_VALUE,
                    routeLineColorResources.routeModerateCongestionColor,
                    1
                ),
                RouteLineExpressionData(
                    0.9423002251348892,
                    congestionValue = RouteLayerConstants.LOW_CONGESTION_VALUE,
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
                RouteLineExpressionData(
                    0.0,
                    congestionValue = RouteLayerConstants.LOW_CONGESTION_VALUE,
                    colorResources.routeLowCongestionColor,
                    0
                ),
                RouteLineExpressionData(
                    offset = 0.5688813850361385,
                    congestionValue = RouteLayerConstants.UNKNOWN_CONGESTION_VALUE,
                    segmentColor = colorResources.routeUnknownCongestionColor,
                    legIndex = 0
                ),
                RouteLineExpressionData(
                    offset = 0.5688813850361385,
                    congestionValue = RouteLayerConstants.LOW_CONGESTION_VALUE,
                    segmentColor = colorResources.routeLowCongestionColor,
                    legIndex = 0
                ),
                RouteLineExpressionData(
                    1.0,
                    congestionValue = RouteLayerConstants.UNKNOWN_CONGESTION_VALUE,
                    colorResources.routeUnknownCongestionColor,
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
    fun getTrafficLineExpression_whenUseSoftGradient() {
        val routeLineColorResources = RouteLineColorResources.Builder().build()
        val expectedPrimaryTrafficLineExpression = "[interpolate, [linear], [line-progress], " +
            "0.0, [rgba, 86.0, 168.0, 251.0, 1.0], " +
            "0.6934838906942539, [rgba, 86.0, 168.0, 251.0, 1.0], " +
            "0.9425498931842539, [rgba, 255.0, 149.0, 0.0, 1.0]]"

        val result = MapboxRouteLineUtils.getTrafficLineExpression(
            0.0,
            Color.TRANSPARENT,
            routeLineColorResources.routeUnknownCongestionColor,
            0.24906600249,
            true,
            listOf(
                RouteLineExpressionData(
                    0.0,
                    congestionValue = RouteLayerConstants.LOW_CONGESTION_VALUE,
                    routeLineColorResources.routeLowCongestionColor,
                    0
                ),
                RouteLineExpressionData(
                    0.9425498931842539,
                    congestionValue = RouteLayerConstants.MODERATE_CONGESTION_VALUE,
                    routeLineColorResources.routeModerateCongestionColor,
                    0
                ),
                RouteLineExpressionData(
                    1.0,
                    congestionValue = RouteLayerConstants.LOW_CONGESTION_VALUE,
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
        val expectedExpression = "[step, [line-progress], [rgba, 255.0, 0.0, 0.0, 1.0], 0.2, " +
            "[rgba, 86.0, 168.0, 251.0, 1.0], 0.4897719974699625, " +
            "[rgba, 255.0, 255.0, 0.0, 1.0]]"
        val colorResources = RouteLineColorResources.Builder()
            .inActiveRouteLegsColor(Color.YELLOW)
            .build()
        val route = loadNavigationRoute("multileg-route-two-legs.json")
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
            "[rgba, 86.0, 168.0, 251.0, 1.0], 0.08334636479867703, " +
            "[rgba, 86.0, 168.0, 251.0, 1.0], 0.10373821458415478, " +
            "[rgba, 255.0, 149.0, 0.0, 1.0], 0.10373821459415478, [rgba, 255.0, 149.0, 0.0, 1.0]," +
            " 0.1240124365711821, [rgba, 86.0, 168.0, 251.0, 1.0], 0.25150644055731514, " +
            "[rgba, 86.0, 168.0, 251.0, 1.0], 0.2718982903427929, " +
            "[rgba, 255.0, 149.0, 0.0, 1.0], 0.3022491448880224, [rgba, 255.0, 149.0, 0.0, 1.0]," +
            " 0.32264099467350016, [rgba, 86.0, 168.0, 251.0, 1.0], 0.4693801476844847," +
            " [rgba, 86.0, 168.0, 251.0, 1.0], 0.4897719974699625, " +
            "[rgba, 255.0, 149.0, 0.0, 1.0], 0.5217469745972377, [rgba, 255.0, 149.0, 0.0, 1.0]," +
            " 0.5421388243827154, [rgba, 86.0, 168.0, 251.0, 1.0], 0.5506732641635784, " +
            "[rgba, 86.0, 168.0, 251.0, 1.0], 0.5710651139490561, [rgba, 255.0, 149.0, 0.0, 1.0]," +
            " 0.5712177478521842, [rgba, 255.0, 149.0, 0.0, 1.0], 0.5916095976376619, " +
            "[rgba, 86.0, 168.0, 251.0, 1.0], 0.8663523665956923, " +
            "[rgba, 86.0, 168.0, 251.0, 1.0], 0.88674421638117, [rgba, 255.0, 149.0, 0.0, 1.0]," +
            " 0.9219083753494115, [rgba, 255.0, 149.0, 0.0, 1.0], 0.9423002251348892, " +
            "[rgba, 86.0, 168.0, 251.0, 1.0]]"
        val colorResources = RouteLineColorResources.Builder().build()
        val route = loadNavigationRoute("multileg-route-two-legs.json")
        val segments = MapboxRouteLineUtils.calculateRouteLineSegments(
            route,
            listOf(),
            true,
            colorResources
        )
        val stopGap = 20.0 / route.directionsRoute.distance()

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
        val expectedExpressionContents = listOf(
            StringChecker("interpolate"),
            StringChecker("[linear]"),
            StringChecker("[line-progress]"),
            DoubleChecker(0.0),
            StringChecker("[rgba, 86.0, 168.0, 251.0, 1.0]"),
            DoubleChecker(0.4522143415383129),
            StringChecker("[rgba, 86.0, 168.0, 251.0, 1.0]"),
            // notice this value (below) minus the stopGap value equals the previous value (above)
            DoubleChecker(0.4677574367125704),
            StringChecker("[rgba, 255.0, 149.0, 0.0, 1.0]"),
            DoubleChecker(0.48662124620419406),
            StringChecker("[rgba, 255.0, 149.0, 0.0, 1.0]"),
            DoubleChecker(0.5021643413784516),
            StringChecker("[rgba, 86.0, 168.0, 251.0, 1.0]"),
            DoubleChecker(0.8435770151055655),
            StringChecker("[rgba, 86.0, 168.0, 251.0, 1.0]"),
            DoubleChecker(0.859120110279823),
            StringChecker("[rgba, 255.0, 149.0, 0.0, 1.0]"),
            DoubleChecker(0.8746827711270166),
            StringChecker("[rgba, 255.0, 149.0, 0.0, 1.0]"),
            DoubleChecker(0.8902258663012742),
            StringChecker("[rgba, 86.0, 168.0, 251.0, 1.0]"),
        )
        val colorResources = RouteLineColorResources.Builder().build()
        val route = loadNavigationRoute("route-with-restrictions.json")
        val segments = MapboxRouteLineUtils.calculateRouteLineSegments(
            route,
            listOf(),
            true,
            colorResources
        )
        val stopGap = 20.0 / route.directionsRoute.distance()

        val result = MapboxRouteLineUtils.getTrafficLineExpressionSoftGradient(
            0.0,
            Color.TRANSPARENT,
            colorResources.routeDefaultColor,
            stopGap,
            segments
        )

        checkExpression(expectedExpressionContents, result)
    }

    @Test
    fun getTrafficLineExpressionSoftGradient_offsetGreaterThanZero() {
        val expectedExpressionContents = listOf(
            StringChecker("interpolate"),
            StringChecker("[linear]"),
            StringChecker("[line-progress]"),
            DoubleChecker(0.0),
            StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            DoubleChecker(0.46999999999),
            StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            DoubleChecker(0.47),
            StringChecker("[rgba, 255.0, 149.0, 0.0, 1.0]"),
            DoubleChecker(0.48662124620419406),
            StringChecker("[rgba, 255.0, 149.0, 0.0, 1.0]"),
            DoubleChecker(0.5021643413784516),
            StringChecker("[rgba, 86.0, 168.0, 251.0, 1.0]"),
            DoubleChecker(0.8435770151055655),
            StringChecker("[rgba, 86.0, 168.0, 251.0, 1.0]"),
            DoubleChecker(0.859120110279823),
            StringChecker("[rgba, 255.0, 149.0, 0.0, 1.0]"),
            DoubleChecker(0.8746827711270166),
            StringChecker("[rgba, 255.0, 149.0, 0.0, 1.0]"),
            DoubleChecker(0.8902258663012742),
            StringChecker("[rgba, 86.0, 168.0, 251.0, 1.0]"),
        )
        val colorResources = RouteLineColorResources.Builder().build()
        val route = loadNavigationRoute("route-with-restrictions.json")
        val segments = MapboxRouteLineUtils.calculateRouteLineSegments(
            route,
            listOf(),
            true,
            colorResources
        )
        val stopGap = 20.0 / route.directionsRoute.distance()

        val result = MapboxRouteLineUtils.getTrafficLineExpressionSoftGradient(
            0.47,
            Color.TRANSPARENT,
            colorResources.routeDefaultColor,
            stopGap,
            segments
        )

        checkExpression(expectedExpressionContents, result)
    }

    @Test
    fun getTrafficLineExpressionSoftGradient_whenStopGapOffsetGreaterThanItemOffset() {
        val expectedExpressionContents = listOf(
            StringChecker("interpolate"),
            StringChecker("[linear]"),
            StringChecker("[line-progress]"),
            DoubleChecker(0.0),
            StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            DoubleChecker(0.8454666619937382),
            StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            // this is the value to notice
            DoubleChecker(0.8454666620037382),
            StringChecker("[rgba, 86.0, 168.0, 251.0, 1.0]"),
            // this is the value to notice
            DoubleChecker(0.8454666620137382),
            StringChecker("[rgba, 86.0, 168.0, 251.0, 1.0]"),
            DoubleChecker(0.859120110279823),
            StringChecker("[rgba, 255.0, 149.0, 0.0, 1.0]"),
            DoubleChecker(0.8746827711270166),
            StringChecker("[rgba, 255.0, 149.0, 0.0, 1.0]"),
            DoubleChecker(0.8902258663012742),
            StringChecker("[rgba, 86.0, 168.0, 251.0, 1.0]"),
        )
        val colorResources = RouteLineColorResources.Builder().build()
        val route = loadNavigationRoute("route-with-restrictions.json")
        val segments = MapboxRouteLineUtils.calculateRouteLineSegments(
            route,
            listOf(),
            true,
            colorResources
        )
        val stopGap = 20.0 / route.directionsRoute.distance()

        val result = MapboxRouteLineUtils.getTrafficLineExpressionSoftGradient(
            0.8454666620037382,
            Color.TRANSPARENT,
            colorResources.routeDefaultColor,
            stopGap,
            segments
        )

        checkExpression(expectedExpressionContents, result)
    }

    @Test
    fun getTrafficLineExpressionSoftGradient_withExtremelySmallDistanceOffset() {
        val expectedExpressionContents = listOf(
            StringChecker("interpolate"),
            StringChecker("[linear]"),
            StringChecker("[line-progress]"),
            DoubleChecker(0.0),
            // notice no stop added before the vanishing point
            StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            DoubleChecker(1.0267342531733E-12),
            StringChecker("[rgba, 86.0, 168.0, 251.0, 1.0]"),
            DoubleChecker(0.4522143415383129),
            StringChecker("[rgba, 86.0, 168.0, 251.0, 1.0]"),
            DoubleChecker(0.4677574367125704),
            StringChecker("[rgba, 255.0, 149.0, 0.0, 1.0]"),
            DoubleChecker(0.48662124620419406),
            StringChecker("[rgba, 255.0, 149.0, 0.0, 1.0]"),
            DoubleChecker(0.5021643413784516),
            StringChecker("[rgba, 86.0, 168.0, 251.0, 1.0]"),
            DoubleChecker(0.8435770151055655),
            StringChecker("[rgba, 86.0, 168.0, 251.0, 1.0]"),
            DoubleChecker(0.859120110279823),
            StringChecker("[rgba, 255.0, 149.0, 0.0, 1.0]"),
            DoubleChecker(0.8746827711270166),
            StringChecker("[rgba, 255.0, 149.0, 0.0, 1.0]"),
            DoubleChecker(0.8902258663012742),
            StringChecker("[rgba, 86.0, 168.0, 251.0, 1.0]"),
        )
        val colorResources = RouteLineColorResources.Builder().build()
        val route = loadNavigationRoute("route-with-restrictions.json")
        val segments = MapboxRouteLineUtils.calculateRouteLineSegments(
            route,
            listOf(),
            true,
            colorResources
        )
        val stopGap = 20.0 / route.directionsRoute.distance()

        val result = MapboxRouteLineUtils.getTrafficLineExpressionSoftGradient(
            0.0000000000010267342531733,
            Color.TRANSPARENT,
            colorResources.routeDefaultColor,
            stopGap,
            segments
        )

        checkExpression(expectedExpressionContents, result)
    }

    @Test
    fun getTrafficLineExpressionSoftGradient_withOffsetEqualToVanishPointStopGap() {
        val expectedExpressionContents = listOf(
            StringChecker("interpolate"),
            StringChecker("[linear]"),
            StringChecker("[line-progress]"),
            DoubleChecker(0.0),
            // notice no stop added before the vanishing point
            StringChecker("[rgba, 0.0, 0.0, 0.0, 0.0]"),
            DoubleChecker(1.0E-11),
            StringChecker("[rgba, 86.0, 168.0, 251.0, 1.0]"),
            DoubleChecker(0.4522143415383129),
            StringChecker("[rgba, 86.0, 168.0, 251.0, 1.0]"),
            DoubleChecker(0.4677574367125704),
            StringChecker("[rgba, 255.0, 149.0, 0.0, 1.0]"),
            DoubleChecker(0.48662124620419406),
            StringChecker("[rgba, 255.0, 149.0, 0.0, 1.0]"),
            DoubleChecker(0.5021643413784516),
            StringChecker("[rgba, 86.0, 168.0, 251.0, 1.0]"),
            DoubleChecker(0.8435770151055655),
            StringChecker("[rgba, 86.0, 168.0, 251.0, 1.0]"),
            DoubleChecker(0.859120110279823),
            StringChecker("[rgba, 255.0, 149.0, 0.0, 1.0]"),
            DoubleChecker(0.8746827711270166),
            StringChecker("[rgba, 255.0, 149.0, 0.0, 1.0]"),
            DoubleChecker(0.8902258663012742),
            StringChecker("[rgba, 86.0, 168.0, 251.0, 1.0]"),
        )
        val colorResources = RouteLineColorResources.Builder().build()
        val route = loadNavigationRoute("route-with-restrictions.json")
        val segments = MapboxRouteLineUtils.calculateRouteLineSegments(
            route,
            listOf(),
            true,
            colorResources
        )
        val stopGap = 20.0 / route.directionsRoute.distance()

        val result = MapboxRouteLineUtils.getTrafficLineExpressionSoftGradient(
            MapboxRouteLineUtils.VANISH_POINT_STOP_GAP,
            Color.TRANSPARENT,
            colorResources.routeDefaultColor,
            stopGap,
            segments
        )

        checkExpression(expectedExpressionContents, result)
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
        val route = loadNavigationRoute(
            "route-with-congestion-numeric.json"
        )
        val expected =
            MapboxRouteLineUtils.getRouteLegTrafficNumericCongestionProvider(colorResources)

        val result =
            MapboxRouteLineUtils.getTrafficCongestionAnnotationProvider(route, colorResources)

        assertEquals(expected, result)
    }

    @Test
    fun extractRouteData_cacheCheck() {
        MapboxRouteLineUtils.extractRouteDataCache.evictAll()
        val route = loadNavigationRoute("short_route.json", "xyz")
        val trafficCongestionProvider = MapboxRouteLineUtils.getRouteLegTrafficCongestionProvider

        MapboxRouteLineUtils.extractRouteData(route, trafficCongestionProvider)
        val putCount = MapboxRouteLineUtils.extractRouteDataCache.putCount()
        val hitCount = MapboxRouteLineUtils.extractRouteDataCache.hitCount()
        MapboxRouteLineUtils.extractRouteData(route, trafficCongestionProvider)

        assertEquals(putCount, MapboxRouteLineUtils.extractRouteDataCache.putCount())
        assertEquals(hitCount + 1, MapboxRouteLineUtils.extractRouteDataCache.hitCount())
    }

    @Test
    fun `trim route data cache`() {
        val putCount = MapboxRouteLineUtils.extractRouteDataCache.putCount()
        val hitCount = MapboxRouteLineUtils.extractRouteDataCache.hitCount()
        val route1 = loadNavigationRoute("short_route.json", "xyz1")
        val route2 = loadNavigationRoute("multileg-route-two-legs.json", "xyz2")
        val trafficCongestionProvider = MapboxRouteLineUtils.getRouteLegTrafficCongestionProvider

        MapboxRouteLineUtils.extractRouteData(route1, trafficCongestionProvider)
        MapboxRouteLineUtils.extractRouteData(route1, trafficCongestionProvider)
        MapboxRouteLineUtils.extractRouteData(route2, trafficCongestionProvider)
        MapboxRouteLineUtils.extractRouteData(route2, trafficCongestionProvider)
        assertEquals(putCount + 2, MapboxRouteLineUtils.extractRouteDataCache.putCount())
        assertEquals(hitCount + 2, MapboxRouteLineUtils.extractRouteDataCache.hitCount())

        MapboxRouteLineUtils.trimRouteDataCacheToSize(1) // removes route1
        MapboxRouteLineUtils.extractRouteData(route1, trafficCongestionProvider)
        MapboxRouteLineUtils.extractRouteData(route2, trafficCongestionProvider)
        assertEquals(putCount + 3, MapboxRouteLineUtils.extractRouteDataCache.putCount())
        assertEquals(hitCount + 3, MapboxRouteLineUtils.extractRouteDataCache.hitCount())

        MapboxRouteLineUtils.trimRouteDataCacheToSize(0) // removes both routes
        MapboxRouteLineUtils.extractRouteData(route1, trafficCongestionProvider)
        MapboxRouteLineUtils.extractRouteData(route2, trafficCongestionProvider)
        assertEquals(putCount + 5, MapboxRouteLineUtils.extractRouteDataCache.putCount())
        assertEquals(hitCount + 3, MapboxRouteLineUtils.extractRouteDataCache.hitCount())

        MapboxRouteLineUtils.trimRouteDataCacheToSize(2) // doesn't remove anything
        MapboxRouteLineUtils.extractRouteData(route1, trafficCongestionProvider)
        MapboxRouteLineUtils.extractRouteData(route2, trafficCongestionProvider)
        assertEquals(putCount + 5, MapboxRouteLineUtils.extractRouteDataCache.putCount())
        assertEquals(hitCount + 5, MapboxRouteLineUtils.extractRouteDataCache.hitCount())
    }
}
