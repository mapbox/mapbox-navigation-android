package com.mapbox.navigation.ui.maps.internal.route.line

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.mapbox.api.directions.v5.models.DirectionsRoute
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
import com.mapbox.navigation.base.internal.NativeRouteParserWrapper
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.route.toNavigationRoute
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
import com.mapbox.navigation.ui.maps.route.line.api.VanishingRouteLine
import com.mapbox.navigation.ui.maps.route.line.model.ExtractedRouteData
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineColorResources
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineDistancesIndex
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineGranularDistances
import com.mapbox.navigation.ui.maps.route.line.model.RoutePoints
import com.mapbox.navigation.ui.maps.testing.TestingUtil
import com.mapbox.navigation.ui.maps.testing.TestingUtil.loadNavigationRoute
import com.mapbox.navigation.utils.internal.InternalJobControlFactory
import com.mapbox.navigator.RouteInterface
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMeasurement
import com.mapbox.turf.TurfMisc
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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class MapboxRouteLineUtilsRoboTest {

    private lateinit var ctx: Context

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
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
    fun tearDown() {
        unmockkObject(NativeRouteParserWrapper)
        MapboxRouteLineUtils.trimRouteDataCacheToSize(0)
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
                WAYPOINT_SOURCE_ID,
                capture(waypointSourceValueSlots),
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
            (primaryRouteSourceValueSlots.last().contents as HashMap<String, Value>)
            ["type"]!!.contents
        )
        assertEquals(
            16L,
            (primaryRouteSourceValueSlots.last().contents as HashMap<String, Value>)
            ["maxzoom"]!!.contents
        )
        assertEquals(
            true,
            (primaryRouteSourceValueSlots.last().contents as HashMap<String, Value>)
            ["lineMetrics"]!!.contents
        )
        assertEquals(
            "",
            (primaryRouteSourceValueSlots.last().contents as HashMap<String, Value>)
            ["data"]!!.contents
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
                (addStyleLayerSlots[20].contents as HashMap<String, Value>)
                ["icon-offset"]!!.contents as ArrayList<Value>
                ).first().contents
        )
        assertEquals(
            44.4,
            (
                (addStyleLayerSlots[20].contents as HashMap<String, Value>)
                ["icon-offset"]!!.contents as ArrayList<Value>
                ).component2().contents
        )
        assertEquals(
            "viewport",
            (addStyleLayerSlots[20].contents as HashMap<String, Value>)
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
        unmockkStatic("com.mapbox.maps.extension.style.layers.LayerUtils")
    }

    @Test
    fun `calculateRouteGranularDistances - flat steps list distances`() {
        val route = loadNavigationRoute("short_route.json")

        val result = MapboxRouteLineUtils.granularDistancesProvider(route)!!

        assertEquals(8, result.flatStepDistances.size)
        assertEquals(result.flatStepDistances[1], result.flatStepDistances[2])
        assertEquals(result.flatStepDistances[4], result.flatStepDistances[5])
        assertEquals(result.flatStepDistances[6], result.flatStepDistances[7])
        assertEquals(Point.fromLngLat(-122.523671, 37.975379), result.flatStepDistances[0].point)
        assertEquals(0.0000025451727518618744, result.flatStepDistances[0].distanceRemaining, 0.0)
        assertEquals(Point.fromLngLat(-122.523117, 37.975107), result.flatStepDistances[4].point)
        assertEquals(0.00000014622044645899132, result.flatStepDistances[4].distanceRemaining, 0.0)
        assertEquals(Point.fromLngLat(-122.523131, 37.975067), result.flatStepDistances[7].point)
        assertEquals(0.0, result.flatStepDistances[7].distanceRemaining, 0.0)
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
            12
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
        val expectedExpression = "[interpolate, [linear], [line-progress], " +
            "0.0, [rgba, 86.0, 168.0, 251.0, 1.0], " +
            "0.4522143415383129, [rgba, 86.0, 168.0, 251.0, 1.0], " +
            // notice this value (below) minus the stopGap value equals the previous value (above)
            "0.4677574367125704, [rgba, 255.0, 149.0, 0.0, 1.0], " +
            "0.48662124620419406, [rgba, 255.0, 149.0, 0.0, 1.0], " +
            "0.5021643413784516, [rgba, 86.0, 168.0, 251.0, 1.0], " +
            "0.8435770151055655, [rgba, 86.0, 168.0, 251.0, 1.0], " +
            "0.859120110279823, [rgba, 255.0, 149.0, 0.0, 1.0], " +
            "0.8746827711270166, [rgba, 255.0, 149.0, 0.0, 1.0], " +
            "0.8902258663012742, [rgba, 86.0, 168.0, 251.0, 1.0]]"
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

        assertEquals(expectedExpression, result.toString())
    }

    @Test
    fun getTrafficLineExpressionSoftGradient_offsetGreaterThanZero() {
        val expectedExpression = "[interpolate, [linear], [line-progress], 0.0, " +
            "[rgba, 0.0, 0.0, 0.0, 0.0], 0.46999999999, " +
            "[rgba, 0.0, 0.0, 0.0, 0.0], 0.47, " +
            "[rgba, 255.0, 149.0, 0.0, 1.0], 0.48662124620419406, " +
            "[rgba, 255.0, 149.0, 0.0, 1.0], 0.5021643413784516, " +
            "[rgba, 86.0, 168.0, 251.0, 1.0], 0.8435770151055655, " +
            "[rgba, 86.0, 168.0, 251.0, 1.0], 0.859120110279823, " +
            "[rgba, 255.0, 149.0, 0.0, 1.0], 0.8746827711270166, " +
            "[rgba, 255.0, 149.0, 0.0, 1.0], 0.8902258663012742, " +
            "[rgba, 86.0, 168.0, 251.0, 1.0]]"
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

        assertEquals(expectedExpression, result.toString())
    }

    @Test
    fun getTrafficLineExpressionSoftGradient_whenStopGapOffsetGreaterThanItemOffset() {
        val expectedExpression = "[interpolate, [linear], [line-progress], " +
            "0.0, [rgba, 0.0, 0.0, 0.0, 0.0], " +
            "0.8454666619937382, [rgba, 0.0, 0.0, 0.0, 0.0], " +
            "0.8454666620037382, [rgba, 86.0, 168.0, 251.0, 1.0], " + // this is the value to notice
            "0.8454666620137382, [rgba, 86.0, 168.0, 251.0, 1.0], " + // this is the value to notice
            "0.859120110279823, [rgba, 255.0, 149.0, 0.0, 1.0], " +
            "0.8746827711270166, [rgba, 255.0, 149.0, 0.0, 1.0], " +
            "0.8902258663012742, [rgba, 86.0, 168.0, 251.0, 1.0]]"
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

        assertEquals(expectedExpression, result.toString())
    }

    @Test
    fun getTrafficLineExpressionSoftGradient_withExtremelySmallDistanceOffset() {
        val expectedExpression = "[interpolate, [linear], [line-progress], 0.0, " +
            // notice no stop added before the vanishing point
            "[rgba, 0.0, 0.0, 0.0, 0.0], 1.0267342531733E-12, " +
            "[rgba, 86.0, 168.0, 251.0, 1.0], 0.4522143415383129, " +
            "[rgba, 86.0, 168.0, 251.0, 1.0], 0.4677574367125704, " +
            "[rgba, 255.0, 149.0, 0.0, 1.0], 0.48662124620419406, " +
            "[rgba, 255.0, 149.0, 0.0, 1.0], 0.5021643413784516, " +
            "[rgba, 86.0, 168.0, 251.0, 1.0], 0.8435770151055655, " +
            "[rgba, 86.0, 168.0, 251.0, 1.0], 0.859120110279823, " +
            "[rgba, 255.0, 149.0, 0.0, 1.0], 0.8746827711270166, " +
            "[rgba, 255.0, 149.0, 0.0, 1.0], 0.8902258663012742, " +
            "[rgba, 86.0, 168.0, 251.0, 1.0]]"
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

        assertEquals(expectedExpression, result.toString())
    }

    @Test
    fun getTrafficLineExpressionSoftGradient_withOffsetEqualToVanishPointStopGap() {
        val expectedExpression = "[interpolate, [linear], [line-progress], 0.0, " +
            // notice no stop added before the vanishing point
            "[rgba, 0.0, 0.0, 0.0, 0.0], 1.0E-11, " +
            "[rgba, 86.0, 168.0, 251.0, 1.0], 0.4522143415383129, " +
            "[rgba, 86.0, 168.0, 251.0, 1.0], 0.4677574367125704, " +
            "[rgba, 255.0, 149.0, 0.0, 1.0], 0.48662124620419406, " +
            "[rgba, 255.0, 149.0, 0.0, 1.0], 0.5021643413784516, " +
            "[rgba, 86.0, 168.0, 251.0, 1.0], 0.8435770151055655, " +
            "[rgba, 86.0, 168.0, 251.0, 1.0], 0.859120110279823, " +
            "[rgba, 255.0, 149.0, 0.0, 1.0], 0.8746827711270166, " +
            "[rgba, 255.0, 149.0, 0.0, 1.0], 0.8902258663012742, " +
            "[rgba, 86.0, 168.0, 251.0, 1.0]]"
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

        assertEquals(expectedExpression, result.toString())
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

    @Test
    fun `trim route points cache`() {
        val route1 = mockk<NavigationRoute>(relaxed = true) {
            every { id } returns "1"
        }
        val route2 = mockk<NavigationRoute>(relaxed = true) {
            every { id } returns "2"
        }
        MapboxRouteLineUtils.routePointsProvider(route1)
        MapboxRouteLineUtils.routePointsProvider(route1)
        MapboxRouteLineUtils.routePointsProvider(route2)
        MapboxRouteLineUtils.routePointsProvider(route2)
        verify(exactly = 1) { route1.directionsRoute }
        verify(exactly = 1) { route2.directionsRoute }

        MapboxRouteLineUtils.trimRouteDataCacheToSize(1) // removes route1
        MapboxRouteLineUtils.routePointsProvider(route1)
        MapboxRouteLineUtils.routePointsProvider(route2)
        verify(exactly = 2) { route1.directionsRoute }
        verify(exactly = 1) { route2.directionsRoute }

        MapboxRouteLineUtils.trimRouteDataCacheToSize(0) // removes both routes
        MapboxRouteLineUtils.routePointsProvider(route1)
        MapboxRouteLineUtils.routePointsProvider(route2)
        verify(exactly = 3) { route1.directionsRoute }
        verify(exactly = 2) { route2.directionsRoute }

        MapboxRouteLineUtils.trimRouteDataCacheToSize(2) // doesn't remove anything
        MapboxRouteLineUtils.routePointsProvider(route1)
        MapboxRouteLineUtils.routePointsProvider(route2)
        verify(exactly = 3) { route1.directionsRoute }
        verify(exactly = 2) { route2.directionsRoute }
    }

    @Test
    fun scratch() {
        val routeJson = "{\"routeIndex\":\"0\",\"distance\":728.195,\"duration\":178.495,\"geometry\":\"yxh`vAtyg~hFyXQiZO}SKsp@YEnd@I|_@|]HrTJxf@B~FgAPoj@UoK{@iJu@oIB_c@?uE@gM?QDoV?{@?gHB_V\",\"weight\":241.413,\"weight_name\":\"auto\",\"legs\":[{\"weight\":241.413,\"via_waypoints\":[],\"distance\":728.195,\"duration\":178.495,\"summary\":\"Main Street, West McLoughlin Boulevard\",\"admins\":[{\"iso_3166_1\":\"US\",\"iso_3166_1_alpha3\":\"USA\"}],\"steps\":[{\"distance\":220.325,\"duration\":29.108,\"geometry\":\"yxh`vAtyg~hFyXQiZO}SKsp@Y\",\"name\":\"Main Street\",\"mode\":\"driving\",\"maneuver\":{\"location\":[-122.671531,45.634461],\"bearing_before\":0.0,\"bearing_after\":1.0,\"instruction\":\"Drive north on Main Street.\",\"type\":\"depart\"},\"driving_side\":\"right\",\"weight\":35.461,\"intersections\":[{\"duration\":7.5,\"weight\":9,\"location\":[-122.671531,45.634461],\"bearings\":[1],\"entry\":[true],\"out\":0,\"geometry_index\":0,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"}},{\"weight\":9.34,\"turn_duration\":2.019,\"turn_weight\":1.5,\"duration\":8.552,\"location\":[-122.671522,45.634874],\"bearings\":[1,82,181,259],\"entry\":[true,true,false,true],\"in\":2,\"out\":0,\"geometry_index\":1,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"},\"traffic_signal\":true},{\"duration\":3.719,\"turn_weight\":0.5,\"turn_duration\":0.019,\"weight\":4.94,\"location\":[-122.671514,45.635311],\"bearings\":[1,90,181],\"entry\":[true,true,false],\"in\":2,\"out\":0,\"geometry_index\":2,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"}},{\"turn_weight\":1,\"turn_duration\":0.019,\"location\":[-122.671508,45.635646],\"bearings\":[1,92,181,249],\"entry\":[true,true,false,true],\"in\":2,\"out\":0,\"geometry_index\":3,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"}}]},{\"distance\":87.726,\"duration\":31.082,\"geometry\":\"otl`vAlwg~hFEnd@I|_@\",\"name\":\"West 20th Street\",\"mode\":\"driving\",\"maneuver\":{\"location\":[-122.671495,45.63644],\"bearing_before\":1.0,\"bearing_after\":270.0,\"instruction\":\"Turn left onto West 20th Street.\",\"type\":\"turn\",\"modifier\":\"left\"},\"driving_side\":\"right\",\"weight\":45.681,\"intersections\":[{\"duration\":19.722,\"turn_weight\":12.5,\"turn_duration\":5.622,\"weight\":29.773,\"location\":[-122.671495,45.63644],\"bearings\":[1,104,181,270],\"entry\":[true,true,false,true],\"in\":2,\"out\":3,\"geometry_index\":4,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"street\"}},{\"turn_weight\":2,\"turn_duration\":0.007,\"location\":[-122.672095,45.636443],\"bearings\":[1,90,182,271],\"entry\":[true,false,true,true],\"in\":1,\"out\":3,\"geometry_index\":5,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"street\"}}]},{\"distance\":179.057,\"duration\":68.349,\"geometry\":\"_ul`vAz}i~hF|]HrTJxf@B~FgA\",\"name\":\"Washington Street\",\"mode\":\"driving\",\"maneuver\":{\"location\":[-122.672622,45.636448],\"bearing_before\":271.0,\"bearing_after\":180.0,\"instruction\":\"Turn left onto Washington Street.\",\"type\":\"turn\",\"modifier\":\"left\"},\"driving_side\":\"right\",\"weight\":88.24,\"intersections\":[{\"duration\":30.372,\"turn_weight\":10,\"turn_duration\":5.622,\"weight\":39.7,\"location\":[-122.672622,45.636448],\"bearings\":[1,91,180,270],\"entry\":[true,false,true,true],\"in\":1,\"out\":2,\"geometry_index\":6,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"street\"}},{\"duration\":17.558,\"turn_weight\":1,\"turn_duration\":0.008,\"weight\":22.06,\"location\":[-122.672627,45.635953],\"bearings\":[0,92,181],\"entry\":[false,true,true],\"in\":0,\"out\":2,\"geometry_index\":7,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"street\"}},{\"turn_weight\":2,\"turn_duration\":0.019,\"location\":[-122.672633,45.635607],\"bearings\":[1,92,180,270],\"entry\":[false,true,true,true],\"in\":0,\"out\":2,\"geometry_index\":8,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"street\"}}]},{\"distance\":241.086,\"duration\":49.955,\"geometry\":\"spi`vAl|i~hFPoj@UoK{@iJu@oIB_c@?uE@gM?QDoV?{@?gHB_V\",\"name\":\"West McLoughlin Boulevard\",\"mode\":\"driving\",\"maneuver\":{\"location\":[-122.672599,45.634842],\"bearing_before\":169.0,\"bearing_after\":91.0,\"instruction\":\"Turn left onto West McLoughlin Boulevard.\",\"type\":\"turn\",\"modifier\":\"left\"},\"driving_side\":\"right\",\"weight\":72.031,\"intersections\":[{\"duration\":22.522,\"turn_weight\":12.5,\"turn_duration\":3.622,\"weight\":35.18,\"location\":[-122.672599,45.634842],\"bearings\":[91,182,270,349],\"entry\":[true,true,true,false],\"in\":3,\"out\":0,\"geometry_index\":10,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"tertiary\"}},{\"weight\":14.931,\"turn_duration\":2.008,\"turn_weight\":3,\"duration\":11.951,\"location\":[-122.671522,45.634874],\"bearings\":[1,82,182,259],\"entry\":[true,true,true,false],\"in\":3,\"out\":1,\"geometry_index\":13,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"tertiary\"},\"traffic_signal\":true},{\"duration\":1.371,\"turn_weight\":0.75,\"weight\":2.396,\"location\":[-122.670778,45.634899],\"bearings\":[90,270],\"entry\":[true,false],\"in\":1,\"out\":0,\"geometry_index\":15,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"tertiary\"}},{\"duration\":3.086,\"turn_weight\":0.75,\"weight\":4.453,\"location\":[-122.670671,45.634899],\"bearings\":[90,270],\"entry\":[true,false],\"in\":1,\"out\":0,\"geometry_index\":16,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"tertiary\"}},{\"weight\":6.469,\"turn_duration\":2.008,\"turn_weight\":2,\"duration\":5.732,\"location\":[-122.670443,45.634898],\"bearings\":[1,91,182,270],\"entry\":[true,true,true,false],\"in\":3,\"out\":1,\"lanes\":[{\"valid\":false,\"active\":false,\"indications\":[\"left\"]},{\"valid\":true,\"active\":true,\"valid_indication\":\"straight\",\"indications\":[\"straight\",\"right\"]}],\"geometry_index\":17,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"tertiary\"},\"traffic_signal\":true},{\"duration\":0.248,\"turn_weight\":0.75,\"weight\":1.048,\"location\":[-122.670058,45.634895],\"bearings\":[90,271],\"entry\":[true,false],\"in\":1,\"out\":0,\"geometry_index\":19,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"tertiary\"}},{\"duration\":1.49,\"turn_weight\":0.75,\"weight\":2.538,\"location\":[-122.670028,45.634895],\"bearings\":[90,270],\"entry\":[true,false],\"in\":1,\"out\":0,\"geometry_index\":20,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"tertiary\"}},{\"turn_weight\":0.75,\"location\":[-122.66988,45.634895],\"bearings\":[90,270],\"entry\":[true,false],\"in\":1,\"out\":0,\"geometry_index\":21,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"tertiary\"}}]},{\"distance\":0.0,\"duration\":0.0,\"geometry\":\"ysi`vAn{c~hF??\",\"name\":\"West McLoughlin Boulevard\",\"mode\":\"driving\",\"maneuver\":{\"location\":[-122.669512,45.634893],\"bearing_before\":90.0,\"bearing_after\":0.0,\"instruction\":\"You have arrived at your destination.\",\"type\":\"arrive\"},\"driving_side\":\"right\",\"weight\":0.0,\"intersections\":[{\"location\":[-122.669512,45.634893],\"bearings\":[270],\"entry\":[true],\"in\":0,\"geometry_index\":22,\"admin_index\":0}]}],\"annotation\":{\"distance\":[46.0,48.6,37.3,88.4,46.7,41.1,55.1,38.5,70.9,14.5,54.2,15.6,14.5,13.4,44.8,8.4,17.7,0.7,29.3,2.3,11.5,28.7],\"congestion_numeric\":[0,null,4,4,null,null,null,null,null,null,0,0,0,4,4,4,4,1,1,1,1,1]}}],\"routeOptions\":{\"baseUrl\":\"https://api.mapbox.com\",\"user\":\"mapbox\",\"profile\":\"driving-traffic\",\"coordinates\":\"-122.6715815,45.6344615;-122.6716338,45.6364896;-122.6726954,45.6364085;-122.672707,45.6348712;-122.6695105,45.6350132\",\"geometries\":\"polyline6\",\"overview\":\"full\",\"steps\":true,\"annotations\":\"congestion_numeric,distance\",\"voice_instructions\":false,\"banner_instructions\":false,\"waypoints\":\"0;4\"},\"requestUuid\":\"mapmatching\"}"
        //val route = DirectionsRoute.fromJson(routeJson).toNavigationRoute(RouterOrigin.Offboard)
        val route = TestingUtil.loadNavigationRoute("short_route.json", "xyz")
        val granularDistances = MapboxRouteLineUtils.granularDistancesProvider(route)
        val vanishingRouteLine = VanishingRouteLine().also {
            it.setScope(InternalJobControlFactory.createMainScopeJobControl().scope)
        }
        vanishingRouteLine.setGranularDistances(granularDistances!!)

        granularDistances.flatStepDistances.forEach {
            vanishingRouteLine.trimTree(it.point)
        }
    }
}
