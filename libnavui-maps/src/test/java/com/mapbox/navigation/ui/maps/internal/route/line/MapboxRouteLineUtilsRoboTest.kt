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
        val routeJson = "{\"country_crossed\":false,\"weight_typical\":305.822,\"routeIndex\":\"1\",\"distance\":1752.465,\"duration\":225.213,\"duration_typical\":225.213,\"geometry\":\"axh`vAtyg~hFqYQiZO}SKsp@Y{TOgMGaRKaLGmDAmCCkCA_DA}KGgPIu[QaEAic@UcKEmZM_DAcEq@mIwD_Bs@oCoA}FaCkCiAuD{AsB{@yCmAoQiHsGiCcFsBmDuAkBu@yVaJ}EaBwOiFiCgAkFyBuJ_E}ViKiWgKwUoJ}UqJuUmJoHgC{OsFiDmACA{CgA_EuAmC}@_@fFEfN?j@Oz|@CdLCfN?tA?zwAIp^Opq@?tDCrDg@zoAC`R\",\"weight\":305.822,\"weight_name\":\"auto\",\"legs\":[{\"weight_typical\":305.822,\"weight\":305.822,\"via_waypoints\":[],\"distance\":1752.465,\"duration\":225.213,\"duration_typical\":225.213,\"summary\":\"Main Street, East 33rd Street\",\"admins\":[{\"iso_3166_1\":\"US\",\"iso_3166_1_alpha3\":\"USA\"}],\"steps\":[{\"weight_typical\":212.389,\"distance\":1255.235,\"duration\":157.679,\"duration_typical\":157.679,\"speedLimitUnit\":\"mph\",\"speedLimitSign\":\"mutcd\",\"geometry\":\"axh`vAtyg~hFqYQiZO}SKsp@Y{TOgMGaRKaLGmDAmCCkCA_DA}KGgPIu[QaEAic@UcKEmZM_DAcEq@mIwD_Bs@oCoA}FaCkCiAuD{AsB{@yCmAoQiHsGiCcFsBmDuAkBu@yVaJ}EaBwOiFiCgAkFyBuJ_E}ViKiWgKwUoJ}UqJuUmJoHgC{OsFiDmACA{CgA_EuAmC}@\",\"name\":\"Main Street\",\"mode\":\"driving\",\"maneuver\":{\"location\":[-122.671531,45.634449],\"bearing_before\":0.0,\"bearing_after\":1.0,\"instruction\":\"Drive north on Main Street.\",\"type\":\"depart\"},\"voiceInstructions\":[{\"distanceAlongGeometry\":1255.235,\"announcement\":\"Drive north on Main Street for 1 mile.\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eDrive north on \\u003csay-as interpret-as\\u003d\\\"address\\\"\\u003eMain Street\\u003c/say-as\\u003e for 1 mile.\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"},{\"distanceAlongGeometry\":402.336,\"announcement\":\"In a quarter mile, Turn left onto East 33rd Street.\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eIn a quarter mile, Turn left onto \\u003csay-as interpret-as\\u003d\\\"address\\\"\\u003eEast 33rd Street\\u003c/say-as\\u003e.\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"},{\"distanceAlongGeometry\":85.333,\"announcement\":\"Turn left onto East 33rd Street.\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eTurn left onto \\u003csay-as interpret-as\\u003d\\\"address\\\"\\u003eEast 33rd Street\\u003c/say-as\\u003e.\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"}],\"bannerInstructions\":[{\"distanceAlongGeometry\":1255.235,\"primary\":{\"text\":\"East 33rd Street\",\"components\":[{\"text\":\"East 33rd Street\",\"type\":\"text\"}],\"type\":\"turn\",\"modifier\":\"left\"}},{\"distanceAlongGeometry\":402.336,\"primary\":{\"text\":\"East 33rd Street\",\"components\":[{\"text\":\"East 33rd Street\",\"type\":\"text\"}],\"type\":\"turn\",\"modifier\":\"left\"},\"sub\":{\"text\":\"\",\"components\":[{\"text\":\"\",\"type\":\"lane\",\"directions\":[\"left\"],\"active\":true,\"active_direction\":\"left\"},{\"text\":\"\",\"type\":\"lane\",\"directions\":[\"straight\"],\"active\":false},{\"text\":\"\",\"type\":\"lane\",\"directions\":[\"straight\",\"right\"],\"active\":false}]}}],\"driving_side\":\"right\",\"weight\":212.389,\"intersections\":[{\"duration\":7.364,\"weight\":8.836,\"location\":[-122.671531,45.634449],\"bearings\":[1],\"entry\":[true],\"out\":0,\"geometry_index\":0,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"}},{\"weight\":9.34,\"turn_duration\":2.019,\"turn_weight\":1.5,\"duration\":8.552,\"location\":[-122.671522,45.634874],\"bearings\":[1,82,181,259],\"entry\":[true,true,false,true],\"in\":2,\"out\":0,\"geometry_index\":1,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"},\"traffic_signal\":true},{\"duration\":3.937,\"turn_weight\":0.5,\"turn_duration\":0.019,\"weight\":5.201,\"location\":[-122.671514,45.635311],\"bearings\":[1,90,181],\"entry\":[true,true,false],\"in\":2,\"out\":0,\"geometry_index\":2,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"}},{\"duration\":9.337,\"turn_weight\":1,\"turn_duration\":0.019,\"weight\":12.181,\"location\":[-122.671508,45.635646],\"bearings\":[1,92,181,249],\"entry\":[true,true,false,true],\"in\":2,\"out\":0,\"geometry_index\":3,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"}},{\"duration\":4.274,\"turn_weight\":1,\"turn_duration\":0.019,\"weight\":6.212,\"location\":[-122.671495,45.63644],\"bearings\":[1,104,181,270],\"entry\":[true,true,false,true],\"in\":2,\"out\":0,\"geometry_index\":4,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"}},{\"duration\":2.746,\"turn_weight\":0.5,\"turn_duration\":0.019,\"weight\":3.841,\"location\":[-122.671487,45.63679],\"bearings\":[1,92,181],\"entry\":[true,true,false],\"in\":2,\"out\":0,\"geometry_index\":5,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"}},{\"duration\":3.728,\"turn_weight\":0.5,\"turn_duration\":0.019,\"weight\":5.044,\"location\":[-122.671483,45.637018],\"bearings\":[1,90,181],\"entry\":[true,true,false],\"in\":2,\"out\":0,\"geometry_index\":6,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"}},{\"duration\":2.874,\"turn_weight\":0.5,\"turn_duration\":0.019,\"weight\":3.998,\"location\":[-122.671477,45.637323],\"bearings\":[1,181,270],\"entry\":[true,false,true],\"in\":1,\"out\":0,\"geometry_index\":7,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"}},{\"duration\":2.965,\"turn_weight\":0.5,\"turn_duration\":0.019,\"weight\":4.108,\"location\":[-122.671473,45.637532],\"bearings\":[1,90,181],\"entry\":[true,true,false],\"in\":2,\"out\":0,\"geometry_index\":8,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"}},{\"weight\":2.999,\"turn_duration\":2.019,\"turn_weight\":0.5,\"duration\":4.059,\"location\":[-122.67147,45.63769],\"bearings\":[1,92,181],\"entry\":[true,true,false],\"in\":2,\"out\":0,\"geometry_index\":10,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"},\"traffic_signal\":true},{\"duration\":2.779,\"turn_weight\":0.5,\"turn_duration\":0.019,\"weight\":3.881,\"location\":[-122.671468,45.63784],\"bearings\":[1,181,267],\"entry\":[true,false,true],\"in\":1,\"out\":0,\"geometry_index\":12,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"}},{\"duration\":4.005,\"turn_weight\":1,\"turn_duration\":0.019,\"weight\":5.882,\"location\":[-122.671464,45.638047],\"bearings\":[1,92,181,270],\"entry\":[true,true,false,true],\"in\":2,\"out\":0,\"geometry_index\":13,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"}},{\"duration\":6.819,\"turn_weight\":0.5,\"turn_duration\":0.019,\"weight\":8.83,\"location\":[-122.671459,45.638323],\"bearings\":[1,181,270],\"entry\":[true,false,true],\"in\":1,\"out\":0,\"geometry_index\":14,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"}},{\"duration\":9.662,\"turn_weight\":1,\"turn_duration\":0.019,\"weight\":12.812,\"location\":[-122.67145,45.638782],\"bearings\":[1,75,181,270],\"entry\":[true,true,false,true],\"in\":2,\"out\":0,\"geometry_index\":15,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"}},{\"duration\":2.659,\"turn_weight\":1,\"turn_duration\":0.019,\"weight\":4.234,\"location\":[-122.671438,45.63946],\"bearings\":[1,92,181,270],\"entry\":[true,true,false,true],\"in\":2,\"out\":0,\"geometry_index\":17,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"}},{\"duration\":11.009,\"turn_weight\":1,\"turn_duration\":0.019,\"weight\":14.462,\"location\":[-122.671435,45.639654],\"bearings\":[1,90,181,270],\"entry\":[true,true,false,true],\"in\":2,\"out\":0,\"geometry_index\":18,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"}},{\"weight\":17.19,\"turn_duration\":2.017,\"turn_weight\":2,\"duration\":14.417,\"location\":[-122.671427,45.640173],\"bearings\":[15,90,181,270],\"entry\":[true,true,false,true],\"in\":2,\"out\":0,\"lanes\":[{\"valid\":false,\"active\":false,\"indications\":[\"left\"]},{\"valid\":true,\"active\":true,\"valid_indication\":\"straight\",\"indications\":[\"straight\"]},{\"valid\":false,\"active\":false,\"indications\":[\"right\"]}],\"geometry_index\":20,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"},\"traffic_signal\":true},{\"duration\":2.4,\"turn_weight\":0.5,\"weight\":3.44,\"location\":[-122.67131,45.640438],\"bearings\":[21,201],\"entry\":[true,false],\"in\":1,\"out\":0,\"geometry_index\":22,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"}},{\"duration\":3.607,\"turn_weight\":0.5,\"turn_duration\":0.007,\"weight\":4.91,\"location\":[-122.671284,45.640486],\"bearings\":[21,201,258],\"entry\":[true,false,true],\"in\":1,\"out\":0,\"geometry_index\":23,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"}},{\"duration\":2.019,\"turn_weight\":0.5,\"turn_duration\":0.019,\"weight\":2.95,\"location\":[-122.671244,45.640558],\"bearings\":[20,201,270],\"entry\":[true,false,true],\"in\":1,\"out\":0,\"geometry_index\":24,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"}},{\"duration\":1.067,\"turn_weight\":0.5,\"weight\":1.807,\"location\":[-122.671179,45.640685],\"bearings\":[20,200],\"entry\":[true,false],\"in\":1,\"out\":0,\"geometry_index\":25,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"}},{\"weight\":2.835,\"turn_duration\":0.007,\"turn_weight\":0.5,\"duration\":1.913,\"location\":[-122.671142,45.640755],\"bearings\":[20,111,200],\"entry\":[true,true,false],\"in\":2,\"out\":0,\"lanes\":[{\"valid\":false,\"active\":false,\"indications\":[\"left\"]},{\"valid\":true,\"active\":true,\"valid_indication\":\"straight\",\"indications\":[\"straight\",\"right\"]}],\"geometry_index\":26,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"}},{\"weight\":6.106,\"turn_duration\":0.021,\"turn_weight\":1,\"duration\":4.189,\"location\":[-122.671066,45.640904],\"bearings\":[19,101,200,283],\"entry\":[true,true,false,true],\"in\":2,\"out\":0,\"lanes\":[{\"valid\":false,\"active\":false,\"indications\":[\"left\"]},{\"valid\":true,\"active\":true,\"valid_indication\":\"straight\",\"indications\":[\"straight\",\"right\"]}],\"geometry_index\":28,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"}},{\"duration\":1.535,\"turn_weight\":0.5,\"turn_duration\":0.019,\"weight\":2.357,\"location\":[-122.670878,45.641277],\"bearings\":[19,108,199],\"entry\":[true,true,false],\"in\":2,\"out\":0,\"geometry_index\":30,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"}},{\"duration\":1.177,\"turn_weight\":0.5,\"turn_duration\":0.007,\"weight\":1.933,\"location\":[-122.670809,45.641415],\"bearings\":[20,108,199],\"entry\":[true,true,false],\"in\":2,\"out\":0,\"geometry_index\":31,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"}},{\"weight\":1.602,\"turn_duration\":0.021,\"turn_weight\":0.5,\"duration\":0.921,\"location\":[-122.670751,45.641529],\"bearings\":[19,90,200],\"entry\":[true,false,false],\"in\":2,\"out\":0,\"lanes\":[{\"valid\":false,\"active\":false,\"indications\":[\"left\"]},{\"valid\":true,\"active\":true,\"valid_indication\":\"straight\",\"indications\":[\"straight\",\"right\"]}],\"geometry_index\":32,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"}},{\"weight\":1.101,\"turn_duration\":0.019,\"turn_weight\":0.5,\"duration\":0.51,\"location\":[-122.670708,45.641616],\"bearings\":[19,76,199],\"entry\":[true,true,false],\"in\":2,\"out\":0,\"lanes\":[{\"valid\":false,\"active\":false,\"indications\":[\"left\"]},{\"valid\":true,\"active\":true,\"valid_indication\":\"straight\",\"indications\":[\"straight\",\"right\"]}],\"geometry_index\":33,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"}},{\"weight\":4.722,\"turn_duration\":0.019,\"turn_weight\":0.5,\"duration\":3.466,\"location\":[-122.670681,45.64167],\"bearings\":[18,199,270],\"entry\":[true,false,true],\"in\":1,\"out\":0,\"lanes\":[{\"valid\":false,\"active\":false,\"indications\":[\"left\"]},{\"valid\":true,\"active\":true,\"valid_indication\":\"straight\",\"indications\":[\"straight\"]}],\"geometry_index\":34,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"}},{\"duration\":1.084,\"turn_weight\":0.5,\"turn_duration\":0.021,\"weight\":1.803,\"location\":[-122.670504,45.642051],\"bearings\":[17,198,270],\"entry\":[true,false,true],\"in\":1,\"out\":0,\"geometry_index\":35,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"}},{\"duration\":2.536,\"turn_weight\":0.5,\"weight\":3.607,\"location\":[-122.670455,45.642162],\"bearings\":[17,197],\"entry\":[true,false],\"in\":1,\"out\":0,\"lanes\":[{\"valid\":false,\"active\":false,\"indications\":[\"left\"]},{\"valid\":true,\"active\":true,\"valid_indication\":\"straight\",\"indications\":[\"straight\"]}],\"geometry_index\":36,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"}},{\"weight\":3.609,\"turn_duration\":0.008,\"turn_weight\":1.5,\"duration\":1.73,\"location\":[-122.670338,45.64243],\"bearings\":[20,92,173,197,270],\"entry\":[true,true,false,false,true],\"in\":3,\"out\":0,\"lanes\":[{\"valid\":false,\"active\":false,\"indications\":[\"left\"]},{\"valid\":true,\"active\":true,\"valid_indication\":\"straight\",\"indications\":[\"straight\"]}],\"geometry_index\":37,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"}},{\"duration\":1.722,\"turn_weight\":0.5,\"weight\":2.609,\"location\":[-122.670241,45.642617],\"bearings\":[20,200],\"entry\":[true,false],\"in\":1,\"out\":0,\"geometry_index\":39,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"}},{\"duration\":3.247,\"turn_weight\":0.5,\"turn_duration\":0.007,\"weight\":4.469,\"location\":[-122.670145,45.642804],\"bearings\":[20,200,270],\"entry\":[true,false,true],\"in\":1,\"out\":0,\"geometry_index\":40,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"}},{\"duration\":3.268,\"turn_weight\":0.5,\"turn_duration\":0.021,\"weight\":4.478,\"location\":[-122.669948,45.643187],\"bearings\":[19,200,270],\"entry\":[true,false,true],\"in\":1,\"out\":0,\"geometry_index\":41,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"}},{\"duration\":2.996,\"turn_weight\":0.5,\"turn_duration\":0.019,\"weight\":4.147,\"location\":[-122.669752,45.643576],\"bearings\":[19,199,270],\"entry\":[true,false,true],\"in\":1,\"out\":0,\"geometry_index\":42,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"}},{\"duration\":3.115,\"turn_weight\":0.5,\"turn_duration\":0.019,\"weight\":4.293,\"location\":[-122.669568,45.64394],\"bearings\":[19,199,270],\"entry\":[true,false,true],\"in\":1,\"out\":0,\"geometry_index\":43,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"}},{\"duration\":3.244,\"turn_weight\":0.5,\"turn_duration\":0.019,\"weight\":4.451,\"location\":[-122.669383,45.644307],\"bearings\":[19,199,270],\"entry\":[true,false,true],\"in\":1,\"out\":0,\"geometry_index\":44,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"}},{\"duration\":1.528,\"turn_weight\":1,\"turn_duration\":0.021,\"weight\":2.846,\"location\":[-122.6692,45.64467],\"bearings\":[17,99,199,277],\"entry\":[true,true,false,true],\"in\":2,\"out\":0,\"geometry_index\":45,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"}},{\"duration\":2.686,\"turn_weight\":0.5,\"turn_duration\":0.007,\"weight\":3.782,\"location\":[-122.669132,45.644822],\"bearings\":[18,197,270],\"entry\":[true,false,true],\"in\":1,\"out\":0,\"geometry_index\":46,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"}},{\"duration\":1.733,\"turn_weight\":0.5,\"turn_duration\":0.019,\"weight\":2.6,\"location\":[-122.66901,45.645092],\"bearings\":[18,198,270],\"entry\":[true,false,true],\"in\":1,\"out\":0,\"geometry_index\":47,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"}},{\"duration\":1.543,\"turn_weight\":0.5,\"weight\":2.39,\"location\":[-122.668971,45.645177],\"bearings\":[18,198],\"entry\":[true,false],\"in\":1,\"out\":0,\"lanes\":[{\"valid\":true,\"active\":true,\"valid_indication\":\"left\",\"indications\":[\"left\"]},{\"valid\":false,\"active\":false,\"indications\":[\"straight\"]},{\"valid\":false,\"active\":false,\"indications\":[\"straight\",\"right\"]}],\"geometry_index\":48,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"}},{\"turn_weight\":0.5,\"location\":[-122.668934,45.645257],\"bearings\":[17,198],\"entry\":[true,false],\"in\":1,\"out\":0,\"lanes\":[{\"valid\":true,\"active\":true,\"valid_indication\":\"left\",\"indications\":[\"left\"]},{\"valid\":false,\"active\":false,\"indications\":[\"straight\"]},{\"valid\":false,\"active\":false,\"indications\":[\"straight\",\"right\"]}],\"geometry_index\":50,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"}}]},{\"weight_typical\":93.433,\"distance\":497.229,\"duration\":67.534,\"duration_typical\":67.534,\"speedLimitUnit\":\"mph\",\"speedLimitSign\":\"mutcd\",\"geometry\":\"_f~`vAvrb~hF_@fFEfN?j@Oz|@CdLCfN?tA?zwAIp^Opq@?tDCrDg@zoAC`R\",\"name\":\"East 33rd Street\",\"mode\":\"driving\",\"maneuver\":{\"location\":[-122.66886,45.645424],\"bearing_before\":17.0,\"bearing_after\":275.0,\"instruction\":\"Turn left onto East 33rd Street.\",\"type\":\"turn\",\"modifier\":\"left\"},\"voiceInstructions\":[{\"distanceAlongGeometry\":483.896,\"announcement\":\"In a quarter mile, Your destination will be on the right.\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eIn a quarter mile, Your destination will be on the right.\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"},{\"distanceAlongGeometry\":55.556,\"announcement\":\"Your destination is on the right.\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eYour destination is on the right.\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"}],\"bannerInstructions\":[{\"distanceAlongGeometry\":497.229,\"primary\":{\"text\":\"Your destination will be on the right\",\"components\":[{\"text\":\"Your destination will be on the right\",\"type\":\"text\"}],\"type\":\"arrive\",\"modifier\":\"right\"}},{\"distanceAlongGeometry\":55.556,\"primary\":{\"text\":\"Your destination is on the right\",\"components\":[{\"text\":\"Your destination is on the right\",\"type\":\"text\"}],\"type\":\"arrive\",\"modifier\":\"right\"}}],\"driving_side\":\"right\",\"weight\":93.433,\"intersections\":[{\"weight\":19.562,\"turn_duration\":8.501,\"turn_weight\":15,\"duration\":12.225,\"location\":[-122.66886,45.645424],\"bearings\":[15,103,197,275],\"entry\":[true,true,false,true],\"in\":2,\"out\":3,\"lanes\":[{\"valid\":true,\"active\":true,\"valid_indication\":\"left\",\"indications\":[\"left\"]},{\"valid\":false,\"active\":false,\"indications\":[\"straight\"]},{\"valid\":false,\"active\":false,\"indications\":[\"straight\",\"right\"]}],\"geometry_index\":52,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"tertiary\"},\"traffic_signal\":true},{\"duration\":9.559,\"turn_weight\":0.75,\"weight\":12.459,\"location\":[-122.669242,45.645443],\"bearings\":[91,271],\"entry\":[false,true],\"in\":0,\"out\":1,\"geometry_index\":55,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"tertiary\"}},{\"duration\":1.426,\"turn_weight\":0.75,\"turn_duration\":0.021,\"weight\":2.471,\"location\":[-122.670232,45.645451],\"bearings\":[0,91,271],\"entry\":[true,false,true],\"in\":1,\"out\":2,\"geometry_index\":56,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"tertiary\"}},{\"duration\":1.689,\"turn_weight\":0.75,\"turn_duration\":0.021,\"weight\":2.794,\"location\":[-122.670443,45.645453],\"bearings\":[0,91,271],\"entry\":[false,false,true],\"in\":1,\"out\":2,\"geometry_index\":57,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"tertiary\"}},{\"duration\":0.284,\"turn_weight\":0.75,\"turn_duration\":0.021,\"weight\":1.073,\"location\":[-122.670687,45.645455],\"bearings\":[91,182,270],\"entry\":[false,false,true],\"in\":0,\"out\":2,\"geometry_index\":58,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"tertiary\"}},{\"duration\":9.746,\"turn_weight\":0.75,\"weight\":12.689,\"location\":[-122.67073,45.645455],\"bearings\":[90,270],\"entry\":[false,true],\"in\":0,\"out\":1,\"geometry_index\":59,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"tertiary\"}},{\"duration\":15.703,\"turn_weight\":1.5,\"turn_duration\":0.007,\"weight\":20.728,\"location\":[-122.672152,45.645455],\"bearings\":[1,90,180,271],\"entry\":[true,false,true,true],\"in\":1,\"out\":3,\"geometry_index\":60,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"tertiary\"}},{\"weight\":17.876,\"turn_duration\":2.021,\"turn_weight\":2,\"duration\":14.981,\"location\":[-122.673557,45.645468],\"bearings\":[0,91,182,271],\"entry\":[true,false,true,true],\"in\":1,\"out\":3,\"geometry_index\":63,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"tertiary\"},\"traffic_signal\":true},{\"turn_weight\":1.5,\"turn_duration\":0.021,\"location\":[-122.674941,45.64549],\"bearings\":[1,91,180,270],\"entry\":[true,false,true,true],\"in\":1,\"out\":3,\"geometry_index\":65,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"tertiary\"}}]},{\"weight_typical\":0,\"distance\":0.0,\"duration\":0.0,\"duration_typical\":0.0,\"speedLimitUnit\":\"mph\",\"speedLimitSign\":\"mutcd\",\"geometry\":\"gj~`vAzao~hF??\",\"name\":\"East 33rd Street\",\"mode\":\"driving\",\"maneuver\":{\"location\":[-122.675246,45.645492],\"bearing_before\":270.0,\"bearing_after\":0.0,\"instruction\":\"Your destination is on the right.\",\"type\":\"arrive\",\"modifier\":\"right\"},\"voiceInstructions\":[],\"bannerInstructions\":[],\"driving_side\":\"right\",\"weight\":0.0,\"intersections\":[{\"location\":[-122.675246,45.645492],\"bearings\":[90],\"entry\":[true],\"in\":0,\"geometry_index\":66,\"admin_index\":0}]}],\"annotation\":{\"distance\":[47.3,48.7,37.3,88.4,38.9,25.4,34.0,23.2,9.7,7.9,7.8,8.9,23.1,30.7,51.1,10.8,64.7,21.6,48.9,8.9,11.0,20.0,5.7,8.6,15.0,8.3,10.7,6.9,9.1,34.9,16.3,13.5,10.2,6.4,44.6,12.9,31.2,8.2,14.0,22.1,45.3,45.9,43.0,43.3,42.8,17.8,31.5,9.9,0.2,9.2,11.2,8.2,9.2,19.0,1.7,77.1,16.4,19.0,3.3,110.7,39.3,63.0,7.1,7.0,100.7,23.8],\"duration\":[7.364,8.552,3.937,9.337,4.274,2.746,3.728,2.874,1.641,1.324,2.971,1.088,2.779,4.005,6.819,1.399,8.263,2.659,9.315,1.694,6.449,7.968,2.4,3.607,2.019,1.067,1.17,0.743,0.882,3.307,1.535,1.177,0.921,0.51,3.466,1.084,2.536,0.644,1.086,1.722,3.247,3.268,2.996,3.115,3.244,1.528,2.686,1.733,0.039,1.504,1.874,1.383,9.647,2.365,0.213,9.559,1.426,1.689,0.284,9.746,5.649,9.038,1.017,2.864,12.117,1.922],\"speed\":[6.4,7.4,9.5,9.5,9.2,9.3,9.2,8.1,6.0,6.0,8.2,8.2,8.3,7.7,7.5,7.8,7.8,8.2,5.3,5.3,2.5,2.5,2.4,2.4,7.5,7.8,9.2,9.2,10.6,10.6,10.7,11.5,11.4,13.0,12.9,12.2,12.3,12.9,12.9,12.8,14.0,14.1,14.4,14.0,13.3,11.8,11.8,5.8,6.1,6.1,6.0,6.0,8.0,8.0,8.0,8.1,11.7,11.4,12.7,11.4,7.0,7.0,7.0,8.3,8.3,12.5],\"maxspeed\":[{\"speed\":32,\"unit\":\"km/h\"},{\"speed\":32,\"unit\":\"km/h\"},{\"speed\":32,\"unit\":\"km/h\"},{\"speed\":32,\"unit\":\"km/h\"},{\"speed\":32,\"unit\":\"km/h\"},{\"speed\":32,\"unit\":\"km/h\"},{\"speed\":32,\"unit\":\"km/h\"},{\"speed\":32,\"unit\":\"km/h\"},{\"speed\":32,\"unit\":\"km/h\"},{\"speed\":32,\"unit\":\"km/h\"},{\"speed\":32,\"unit\":\"km/h\"},{\"speed\":32,\"unit\":\"km/h\"},{\"speed\":32,\"unit\":\"km/h\"},{\"speed\":32,\"unit\":\"km/h\"},{\"speed\":32,\"unit\":\"km/h\"},{\"speed\":32,\"unit\":\"km/h\"},{\"speed\":32,\"unit\":\"km/h\"},{\"speed\":32,\"unit\":\"km/h\"},{\"speed\":32,\"unit\":\"km/h\"},{\"speed\":32,\"unit\":\"km/h\"},{\"speed\":48,\"unit\":\"km/h\"},{\"speed\":48,\"unit\":\"km/h\"},{\"speed\":48,\"unit\":\"km/h\"},{\"speed\":48,\"unit\":\"km/h\"},{\"speed\":48,\"unit\":\"km/h\"},{\"speed\":48,\"unit\":\"km/h\"},{\"speed\":48,\"unit\":\"km/h\"},{\"speed\":48,\"unit\":\"km/h\"},{\"speed\":48,\"unit\":\"km/h\"},{\"speed\":48,\"unit\":\"km/h\"},{\"speed\":48,\"unit\":\"km/h\"},{\"speed\":48,\"unit\":\"km/h\"},{\"speed\":48,\"unit\":\"km/h\"},{\"speed\":48,\"unit\":\"km/h\"},{\"speed\":48,\"unit\":\"km/h\"},{\"speed\":48,\"unit\":\"km/h\"},{\"speed\":48,\"unit\":\"km/h\"},{\"speed\":48,\"unit\":\"km/h\"},{\"speed\":48,\"unit\":\"km/h\"},{\"speed\":48,\"unit\":\"km/h\"},{\"speed\":48,\"unit\":\"km/h\"},{\"speed\":48,\"unit\":\"km/h\"},{\"speed\":48,\"unit\":\"km/h\"},{\"speed\":48,\"unit\":\"km/h\"},{\"speed\":48,\"unit\":\"km/h\"},{\"speed\":48,\"unit\":\"km/h\"},{\"speed\":48,\"unit\":\"km/h\"},{\"speed\":48,\"unit\":\"km/h\"},{\"speed\":48,\"unit\":\"km/h\"},{\"speed\":48,\"unit\":\"km/h\"},{\"speed\":48,\"unit\":\"km/h\"},{\"speed\":48,\"unit\":\"km/h\"},{\"speed\":40,\"unit\":\"km/h\"},{\"speed\":40,\"unit\":\"km/h\"},{\"speed\":40,\"unit\":\"km/h\"},{\"speed\":40,\"unit\":\"km/h\"},{\"speed\":40,\"unit\":\"km/h\"},{\"speed\":40,\"unit\":\"km/h\"},{\"speed\":40,\"unit\":\"km/h\"},{\"speed\":40,\"unit\":\"km/h\"},{\"speed\":40,\"unit\":\"km/h\"},{\"speed\":40,\"unit\":\"km/h\"},{\"speed\":40,\"unit\":\"km/h\"},{\"speed\":40,\"unit\":\"km/h\"},{\"speed\":40,\"unit\":\"km/h\"},{\"speed\":40,\"unit\":\"km/h\"}],\"congestion_numeric\":[0,24,9,4,4,0,0,null,6,6,0,0,null,null,0,0,0,11,0,0,null,null,null,null,null,null,null,null,6,6,null,null,null,null,8,1,1,1,1,1,0,0,0,0,1,6,6,0,0,0,0,0,20,20,20,20,null,0,0,0,0,0,0,8,8,6]}}],\"routeOptions\":{\"baseUrl\":\"https://api.mapbox.com\",\"user\":\"mapbox\",\"profile\":\"driving-traffic\",\"coordinates\":\"-122.6717523,45.6344513;-122.6752454,45.6455678\",\"alternatives\":true,\"language\":\"en\",\"layers\":\";\",\"continue_straight\":true,\"roundabout_exits\":true,\"geometries\":\"polyline6\",\"overview\":\"full\",\"steps\":true,\"annotations\":\"congestion_numeric,maxspeed,closure,speed,duration,distance\",\"voice_instructions\":true,\"banner_instructions\":true,\"voice_units\":\"imperial\",\"enable_refresh\":true},\"voiceLocale\":\"en-US\",\"requestUuid\":\"PuEXN4U6XkX5NOGg6UssKTN-Ipon4nZF7dok-zNkwT-ney88PBFDqA\\u003d\\u003d\"}"
        //val routeJson = "{\"routeIndex\":\"0\",\"distance\":728.195,\"duration\":178.495,\"geometry\":\"yxh`vAtyg~hFyXQiZO}SKsp@YEnd@I|_@|]HrTJxf@B~FgAPoj@UoK{@iJu@oIB_c@?uE@gM?QDoV?{@?gHB_V\",\"weight\":241.413,\"weight_name\":\"auto\",\"legs\":[{\"weight\":241.413,\"via_waypoints\":[],\"distance\":728.195,\"duration\":178.495,\"summary\":\"Main Street, West McLoughlin Boulevard\",\"admins\":[{\"iso_3166_1\":\"US\",\"iso_3166_1_alpha3\":\"USA\"}],\"steps\":[{\"distance\":220.325,\"duration\":29.108,\"geometry\":\"yxh`vAtyg~hFyXQiZO}SKsp@Y\",\"name\":\"Main Street\",\"mode\":\"driving\",\"maneuver\":{\"location\":[-122.671531,45.634461],\"bearing_before\":0.0,\"bearing_after\":1.0,\"instruction\":\"Drive north on Main Street.\",\"type\":\"depart\"},\"driving_side\":\"right\",\"weight\":35.461,\"intersections\":[{\"duration\":7.5,\"weight\":9,\"location\":[-122.671531,45.634461],\"bearings\":[1],\"entry\":[true],\"out\":0,\"geometry_index\":0,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"}},{\"weight\":9.34,\"turn_duration\":2.019,\"turn_weight\":1.5,\"duration\":8.552,\"location\":[-122.671522,45.634874],\"bearings\":[1,82,181,259],\"entry\":[true,true,false,true],\"in\":2,\"out\":0,\"geometry_index\":1,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"},\"traffic_signal\":true},{\"duration\":3.719,\"turn_weight\":0.5,\"turn_duration\":0.019,\"weight\":4.94,\"location\":[-122.671514,45.635311],\"bearings\":[1,90,181],\"entry\":[true,true,false],\"in\":2,\"out\":0,\"geometry_index\":2,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"}},{\"turn_weight\":1,\"turn_duration\":0.019,\"location\":[-122.671508,45.635646],\"bearings\":[1,92,181,249],\"entry\":[true,true,false,true],\"in\":2,\"out\":0,\"geometry_index\":3,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"secondary\"}}]},{\"distance\":87.726,\"duration\":31.082,\"geometry\":\"otl`vAlwg~hFEnd@I|_@\",\"name\":\"West 20th Street\",\"mode\":\"driving\",\"maneuver\":{\"location\":[-122.671495,45.63644],\"bearing_before\":1.0,\"bearing_after\":270.0,\"instruction\":\"Turn left onto West 20th Street.\",\"type\":\"turn\",\"modifier\":\"left\"},\"driving_side\":\"right\",\"weight\":45.681,\"intersections\":[{\"duration\":19.722,\"turn_weight\":12.5,\"turn_duration\":5.622,\"weight\":29.773,\"location\":[-122.671495,45.63644],\"bearings\":[1,104,181,270],\"entry\":[true,true,false,true],\"in\":2,\"out\":3,\"geometry_index\":4,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"street\"}},{\"turn_weight\":2,\"turn_duration\":0.007,\"location\":[-122.672095,45.636443],\"bearings\":[1,90,182,271],\"entry\":[true,false,true,true],\"in\":1,\"out\":3,\"geometry_index\":5,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"street\"}}]},{\"distance\":179.057,\"duration\":68.349,\"geometry\":\"_ul`vAz}i~hF|]HrTJxf@B~FgA\",\"name\":\"Washington Street\",\"mode\":\"driving\",\"maneuver\":{\"location\":[-122.672622,45.636448],\"bearing_before\":271.0,\"bearing_after\":180.0,\"instruction\":\"Turn left onto Washington Street.\",\"type\":\"turn\",\"modifier\":\"left\"},\"driving_side\":\"right\",\"weight\":88.24,\"intersections\":[{\"duration\":30.372,\"turn_weight\":10,\"turn_duration\":5.622,\"weight\":39.7,\"location\":[-122.672622,45.636448],\"bearings\":[1,91,180,270],\"entry\":[true,false,true,true],\"in\":1,\"out\":2,\"geometry_index\":6,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"street\"}},{\"duration\":17.558,\"turn_weight\":1,\"turn_duration\":0.008,\"weight\":22.06,\"location\":[-122.672627,45.635953],\"bearings\":[0,92,181],\"entry\":[false,true,true],\"in\":0,\"out\":2,\"geometry_index\":7,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"street\"}},{\"turn_weight\":2,\"turn_duration\":0.019,\"location\":[-122.672633,45.635607],\"bearings\":[1,92,180,270],\"entry\":[false,true,true,true],\"in\":0,\"out\":2,\"geometry_index\":8,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"street\"}}]},{\"distance\":241.086,\"duration\":49.955,\"geometry\":\"spi`vAl|i~hFPoj@UoK{@iJu@oIB_c@?uE@gM?QDoV?{@?gHB_V\",\"name\":\"West McLoughlin Boulevard\",\"mode\":\"driving\",\"maneuver\":{\"location\":[-122.672599,45.634842],\"bearing_before\":169.0,\"bearing_after\":91.0,\"instruction\":\"Turn left onto West McLoughlin Boulevard.\",\"type\":\"turn\",\"modifier\":\"left\"},\"driving_side\":\"right\",\"weight\":72.031,\"intersections\":[{\"duration\":22.522,\"turn_weight\":12.5,\"turn_duration\":3.622,\"weight\":35.18,\"location\":[-122.672599,45.634842],\"bearings\":[91,182,270,349],\"entry\":[true,true,true,false],\"in\":3,\"out\":0,\"geometry_index\":10,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"tertiary\"}},{\"weight\":14.931,\"turn_duration\":2.008,\"turn_weight\":3,\"duration\":11.951,\"location\":[-122.671522,45.634874],\"bearings\":[1,82,182,259],\"entry\":[true,true,true,false],\"in\":3,\"out\":1,\"geometry_index\":13,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"tertiary\"},\"traffic_signal\":true},{\"duration\":1.371,\"turn_weight\":0.75,\"weight\":2.396,\"location\":[-122.670778,45.634899],\"bearings\":[90,270],\"entry\":[true,false],\"in\":1,\"out\":0,\"geometry_index\":15,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"tertiary\"}},{\"duration\":3.086,\"turn_weight\":0.75,\"weight\":4.453,\"location\":[-122.670671,45.634899],\"bearings\":[90,270],\"entry\":[true,false],\"in\":1,\"out\":0,\"geometry_index\":16,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"tertiary\"}},{\"weight\":6.469,\"turn_duration\":2.008,\"turn_weight\":2,\"duration\":5.732,\"location\":[-122.670443,45.634898],\"bearings\":[1,91,182,270],\"entry\":[true,true,true,false],\"in\":3,\"out\":1,\"lanes\":[{\"valid\":false,\"active\":false,\"indications\":[\"left\"]},{\"valid\":true,\"active\":true,\"valid_indication\":\"straight\",\"indications\":[\"straight\",\"right\"]}],\"geometry_index\":17,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"tertiary\"},\"traffic_signal\":true},{\"duration\":0.248,\"turn_weight\":0.75,\"weight\":1.048,\"location\":[-122.670058,45.634895],\"bearings\":[90,271],\"entry\":[true,false],\"in\":1,\"out\":0,\"geometry_index\":19,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"tertiary\"}},{\"duration\":1.49,\"turn_weight\":0.75,\"weight\":2.538,\"location\":[-122.670028,45.634895],\"bearings\":[90,270],\"entry\":[true,false],\"in\":1,\"out\":0,\"geometry_index\":20,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"tertiary\"}},{\"turn_weight\":0.75,\"location\":[-122.66988,45.634895],\"bearings\":[90,270],\"entry\":[true,false],\"in\":1,\"out\":0,\"geometry_index\":21,\"is_urban\":true,\"admin_index\":0,\"mapbox_streets_v8\":{\"class\":\"tertiary\"}}]},{\"distance\":0.0,\"duration\":0.0,\"geometry\":\"ysi`vAn{c~hF??\",\"name\":\"West McLoughlin Boulevard\",\"mode\":\"driving\",\"maneuver\":{\"location\":[-122.669512,45.634893],\"bearing_before\":90.0,\"bearing_after\":0.0,\"instruction\":\"You have arrived at your destination.\",\"type\":\"arrive\"},\"driving_side\":\"right\",\"weight\":0.0,\"intersections\":[{\"location\":[-122.669512,45.634893],\"bearings\":[270],\"entry\":[true],\"in\":0,\"geometry_index\":22,\"admin_index\":0}]}],\"annotation\":{\"distance\":[46.0,48.6,37.3,88.4,46.7,41.1,55.1,38.5,70.9,14.5,54.2,15.6,14.5,13.4,44.8,8.4,17.7,0.7,29.3,2.3,11.5,28.7],\"congestion_numeric\":[0,null,4,4,null,null,null,null,null,null,0,0,0,4,4,4,4,1,1,1,1,1]}}],\"routeOptions\":{\"baseUrl\":\"https://api.mapbox.com\",\"user\":\"mapbox\",\"profile\":\"driving-traffic\",\"coordinates\":\"-122.6715815,45.6344615;-122.6716338,45.6364896;-122.6726954,45.6364085;-122.672707,45.6348712;-122.6695105,45.6350132\",\"geometries\":\"polyline6\",\"overview\":\"full\",\"steps\":true,\"annotations\":\"congestion_numeric,distance\",\"voice_instructions\":false,\"banner_instructions\":false,\"waypoints\":\"0;4\"},\"requestUuid\":\"mapmatching\"}"
        val route = DirectionsRoute.fromJson(routeJson).toNavigationRoute(RouterOrigin.Offboard)
        //val route = TestingUtil.loadNavigationRoute("short_route.json", "xyz")
        val granularDistances = MapboxRouteLineUtils.granularDistancesProvider(route)
        val vanishingRouteLine = VanishingRouteLine().also {
            it.setScope(InternalJobControlFactory.createMainScopeJobControl().scope)
        }
        vanishingRouteLine.setGranularDistances(granularDistances!!)

        // granularDistances.flatStepDistances.forEach {
        //     vanishingRouteLine.trimTree(it.point)
        // }
        //point Point{type=Point, bbox=null, coordinates=[-122.67146157204115, 45.638172892050875]} offset 0.11118330619475914
        val point = Point.fromLngLat(-122.67146157204115, 45.638172892050875)
        //val point = granularDistances.flatStepDistances.get(granularDistances.flatStepDistances.size / 2)

        //vanishingRouteLine.foobar(point.point)

        //vanishingRouteLine.checkPointInCurrentRange(Point.fromLngLat(-122.67147824336483, 45.63726680626696))

        listOf(
            //Point.fromLngLat(-122.67147824336483, 45.63726680626696),
            Point.fromLngLat(-122.67147742508963, 45.63731108636806),
            Point.fromLngLat(-122.67147676267612, 45.63734693217765),
            Point.fromLngLat(-122.67147611000415, 45.6373822508342),
            Point.fromLngLat(-122.67147545733246, 45.63741756947505),
            Point.fromLngLat(-122.67147493129485, 45.63744639668131),
            Point.fromLngLat(-122.67147436374492, 45.63747816134145),
            Point.fromLngLat(-122.671473796195, 45.637509926000696)
        ).forEach {
            val offset = vanishingRouteLine.getOffset(it)
            println(offset)
        }



    }

//0.12647115633449757
/*
point Point{type=Point, bbox=null, coordinates=[-122.67147824336483, 45.63726680626696]} offset 0.1264711563343266
point Point{type=Point, bbox=null, coordinates=[-122.67147742508963, 45.63731108636806]} offset 0.1264711563343266
point Point{type=Point, bbox=null, coordinates=[-122.67147676267612, 45.63734693217765]} offset 0.1264711563343266
point Point{type=Point, bbox=null, coordinates=[-122.67147611000415, 45.6373822508342]} offset 0.1264711563343266
point Point{type=Point, bbox=null, coordinates=[-122.67147545733246, 45.63741756947505]} offset 0.1264711563343266
point Point{type=Point, bbox=null, coordinates=[-122.67147493129485, 45.63744639668131]} offset 0.1264711563343266
point Point{type=Point, bbox=null, coordinates=[-122.67147436374492, 45.63747816134145]} offset 0.1264711563343266
point Point{type=Point, bbox=null, coordinates=[-122.671473796195, 45.637509926000696]} offset 0.1264711563343266
point Point{type=Point, bbox=null, coordinates=[-122.6714732286451, 45.63754169065995]} offset 0.1264711563343266
 */
}
