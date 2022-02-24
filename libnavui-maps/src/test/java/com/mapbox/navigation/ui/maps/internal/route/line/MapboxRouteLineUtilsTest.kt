package com.mapbox.navigation.ui.maps.internal.route.line

import android.content.Context
import android.graphics.Color
import androidx.appcompat.content.res.AppCompatResources
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.core.constants.Constants
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.Layer
import com.mapbox.maps.extension.style.layers.getLayer
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility
import com.mapbox.navigation.base.route.toNavigationRoute
import com.mapbox.navigation.testing.FileUtils
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.ALTERNATIVE_ROUTE1_CASING_LAYER_ID
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.ALTERNATIVE_ROUTE1_LAYER_ID
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.ALTERNATIVE_ROUTE1_SOURCE_ID
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.ALTERNATIVE_ROUTE1_TRAFFIC_LAYER_ID
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.ALTERNATIVE_ROUTE2_CASING_LAYER_ID
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.ALTERNATIVE_ROUTE2_LAYER_ID
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.ALTERNATIVE_ROUTE2_SOURCE_ID
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.ALTERNATIVE_ROUTE2_TRAFFIC_LAYER_ID
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.CLOSURE_CONGESTION_VALUE
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.HEAVY_CONGESTION_VALUE
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LOW_CONGESTION_VALUE
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.MODERATE_CONGESTION_VALUE
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.PRIMARY_ROUTE_CASING_LAYER_ID
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.PRIMARY_ROUTE_LAYER_ID
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.PRIMARY_ROUTE_SOURCE_ID
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.PRIMARY_ROUTE_TRAFFIC_LAYER_ID
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.RESTRICTED_CONGESTION_VALUE
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.RESTRICTED_ROAD_LAYER_ID
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.SEVERE_CONGESTION_VALUE
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.TOP_LEVEL_ROUTE_LINE_LAYER_ID
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.UNKNOWN_CONGESTION_VALUE
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.WAYPOINT_SOURCE_ID
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineColorResources
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineExpressionData
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineScaleValue
import com.mapbox.navigation.ui.maps.route.line.model.RouteStyleDescriptor
import com.mapbox.navigation.ui.maps.testing.TestingUtil.loadRoute
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MapboxRouteLineUtilsTest {

    private val ctx: Context = mockk()

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
            every { styleSourceExists(PRIMARY_ROUTE_SOURCE_ID) } returns true
            every { styleSourceExists(ALTERNATIVE_ROUTE1_SOURCE_ID) } returns true
            every { styleSourceExists(ALTERNATIVE_ROUTE2_SOURCE_ID) } returns true
            every { styleLayerExists(PRIMARY_ROUTE_LAYER_ID) } returns true
            every {
                styleLayerExists(PRIMARY_ROUTE_TRAFFIC_LAYER_ID)
            } returns true
            every {
                styleLayerExists(PRIMARY_ROUTE_CASING_LAYER_ID)
            } returns true
            every { styleLayerExists(ALTERNATIVE_ROUTE1_LAYER_ID) } returns true
            every { styleLayerExists(ALTERNATIVE_ROUTE2_LAYER_ID) } returns true
            every {
                styleLayerExists(ALTERNATIVE_ROUTE1_CASING_LAYER_ID)
            } returns true
            every {
                styleLayerExists(ALTERNATIVE_ROUTE2_CASING_LAYER_ID)
            } returns true
            every {
                styleLayerExists(ALTERNATIVE_ROUTE1_TRAFFIC_LAYER_ID)
            } returns true
            every {
                styleLayerExists(ALTERNATIVE_ROUTE2_TRAFFIC_LAYER_ID)
            } returns true
            every {
                styleLayerExists(RESTRICTED_ROAD_LAYER_ID)
            } returns true
            every {
                styleLayerExists(TOP_LEVEL_ROUTE_LINE_LAYER_ID)
            } returns true
        }

        val result = MapboxRouteLineUtils.layersAreInitialized(style, options)

        assertTrue(result)
        verify { style.styleSourceExists(PRIMARY_ROUTE_SOURCE_ID) }
        verify { style.styleSourceExists(ALTERNATIVE_ROUTE1_SOURCE_ID) }
        verify { style.styleSourceExists(ALTERNATIVE_ROUTE2_SOURCE_ID) }
        verify { style.styleLayerExists(PRIMARY_ROUTE_LAYER_ID) }
        verify { style.styleLayerExists(PRIMARY_ROUTE_TRAFFIC_LAYER_ID) }
        verify { style.styleLayerExists(PRIMARY_ROUTE_CASING_LAYER_ID) }
        verify { style.styleLayerExists(ALTERNATIVE_ROUTE1_LAYER_ID) }
        verify { style.styleLayerExists(ALTERNATIVE_ROUTE2_LAYER_ID) }
        verify { style.styleLayerExists(ALTERNATIVE_ROUTE1_CASING_LAYER_ID) }
        verify { style.styleLayerExists(ALTERNATIVE_ROUTE2_CASING_LAYER_ID) }
        verify { style.styleLayerExists(ALTERNATIVE_ROUTE1_TRAFFIC_LAYER_ID) }
        verify { style.styleLayerExists(ALTERNATIVE_ROUTE2_TRAFFIC_LAYER_ID) }
        verify { style.styleLayerExists(RESTRICTED_ROAD_LAYER_ID) }
    }

    @Test
    fun `layersAreInitialized without restricted roads`() {
        val options = mockk<MapboxRouteLineOptions> {
            every { displayRestrictedRoadSections } returns false
        }
        val style = mockk<Style> {
            every { styleSourceExists(PRIMARY_ROUTE_SOURCE_ID) } returns true
            every { styleSourceExists(ALTERNATIVE_ROUTE1_SOURCE_ID) } returns true
            every { styleSourceExists(ALTERNATIVE_ROUTE2_SOURCE_ID) } returns true
            every { styleLayerExists(PRIMARY_ROUTE_LAYER_ID) } returns true
            every {
                styleLayerExists(PRIMARY_ROUTE_TRAFFIC_LAYER_ID)
            } returns true
            every {
                styleLayerExists(PRIMARY_ROUTE_CASING_LAYER_ID)
            } returns true
            every { styleLayerExists(ALTERNATIVE_ROUTE1_LAYER_ID) } returns true
            every { styleLayerExists(ALTERNATIVE_ROUTE2_LAYER_ID) } returns true
            every {
                styleLayerExists(ALTERNATIVE_ROUTE1_CASING_LAYER_ID)
            } returns true
            every {
                styleLayerExists(ALTERNATIVE_ROUTE2_CASING_LAYER_ID)
            } returns true
            every {
                styleLayerExists(ALTERNATIVE_ROUTE1_TRAFFIC_LAYER_ID)
            } returns true
            every {
                styleLayerExists(ALTERNATIVE_ROUTE2_TRAFFIC_LAYER_ID)
            } returns true
            every {
                styleLayerExists(RESTRICTED_ROAD_LAYER_ID)
            } returns false
            every {
                styleLayerExists(TOP_LEVEL_ROUTE_LINE_LAYER_ID)
            } returns true
        }

        val result = MapboxRouteLineUtils.layersAreInitialized(style, options)

        assertTrue(result)
        verify { style.styleSourceExists(PRIMARY_ROUTE_SOURCE_ID) }
        verify { style.styleSourceExists(ALTERNATIVE_ROUTE1_SOURCE_ID) }
        verify { style.styleSourceExists(ALTERNATIVE_ROUTE2_SOURCE_ID) }
        verify { style.styleLayerExists(PRIMARY_ROUTE_LAYER_ID) }
        verify { style.styleLayerExists(PRIMARY_ROUTE_TRAFFIC_LAYER_ID) }
        verify { style.styleLayerExists(PRIMARY_ROUTE_CASING_LAYER_ID) }
        verify { style.styleLayerExists(ALTERNATIVE_ROUTE1_LAYER_ID) }
        verify { style.styleLayerExists(ALTERNATIVE_ROUTE2_LAYER_ID) }
        verify { style.styleLayerExists(ALTERNATIVE_ROUTE1_CASING_LAYER_ID) }
        verify { style.styleLayerExists(ALTERNATIVE_ROUTE2_CASING_LAYER_ID) }
        verify { style.styleLayerExists(ALTERNATIVE_ROUTE1_TRAFFIC_LAYER_ID) }
        verify { style.styleLayerExists(ALTERNATIVE_ROUTE2_TRAFFIC_LAYER_ID) }
        verify(exactly = 0) {
            style.styleLayerExists(RESTRICTED_ROAD_LAYER_ID)
        }
    }

    @Test
    fun initializeLayers_whenLayersAreInitialized() {
        val options = MapboxRouteLineOptions.Builder(ctx).build()
        val style = mockk<Style> {
            every { styleLayers } returns listOf()
            every { styleSourceExists(PRIMARY_ROUTE_SOURCE_ID) } returns true
            every { styleSourceExists(ALTERNATIVE_ROUTE1_SOURCE_ID) } returns true
            every { styleSourceExists(ALTERNATIVE_ROUTE2_SOURCE_ID) } returns true
            every { styleLayerExists(PRIMARY_ROUTE_LAYER_ID) } returns true
            every {
                styleLayerExists(PRIMARY_ROUTE_TRAFFIC_LAYER_ID)
            } returns true
            every {
                styleLayerExists(PRIMARY_ROUTE_CASING_LAYER_ID)
            } returns true
            every { styleLayerExists(ALTERNATIVE_ROUTE1_LAYER_ID) } returns true
            every { styleLayerExists(ALTERNATIVE_ROUTE2_LAYER_ID) } returns true
            every {
                styleLayerExists(ALTERNATIVE_ROUTE1_CASING_LAYER_ID)
            } returns true
            every {
                styleLayerExists(ALTERNATIVE_ROUTE2_CASING_LAYER_ID)
            } returns true
            every {
                styleLayerExists(ALTERNATIVE_ROUTE1_TRAFFIC_LAYER_ID)
            } returns true
            every {
                styleLayerExists(ALTERNATIVE_ROUTE2_TRAFFIC_LAYER_ID)
            } returns true
            every {
                styleLayerExists(RESTRICTED_ROAD_LAYER_ID)
            } returns true
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
                { it.isInRestrictedSection },
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
    fun getRestrictedRouteLegRangesTest() {
        val route = loadRoute("route-with-restrictions.json")
        val coordinates = LineString.fromPolyline(
            route.geometry() ?: "",
            Constants.PRECISION_6
        )

        val result = MapboxRouteLineUtils.getRestrictedRouteLegRanges(route.legs()!!.first())

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
            CLOSURE_CONGESTION_VALUE,
            false,
            resources
        )

        assertEquals(resources.alternativeRouteClosureColor, result)
    }

    @Test
    fun buildWayPointFeatureCollection() {
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
        val route = loadRoute("multileg_route.json")

        val result = MapboxRouteLineUtils.parseRoutePoints(route)!!

        assertEquals(128, result.flatList.size)
        assertEquals(15, result.nestedList.flatten().size)
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
        assertFalse(result[36].isInRestrictedSection)
        assertTrue(result[37].isInRestrictedSection)
        assertEquals(result[37].roadClass, "tertiary")
        assertFalse(result[38].isInRestrictedSection)
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
