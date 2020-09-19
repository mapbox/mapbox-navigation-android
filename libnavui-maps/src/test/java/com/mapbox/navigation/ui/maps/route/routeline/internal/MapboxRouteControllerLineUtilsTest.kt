package com.mapbox.navigation.ui.maps.route.routeline.internal

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Point
import com.mapbox.navigation.ui.internal.route.RouteConstants
import com.mapbox.navigation.ui.maps.R
import com.mapbox.navigation.ui.maps.route.routeline.internal.MapboxRouteLineUtils.getRouteLineExpressionDataWithStreetClassOverride
import com.mapbox.navigation.ui.maps.route.routeline.internal.MapboxRouteLineUtils.getRouteLineScalingValues
import com.mapbox.navigation.ui.maps.route.routeline.internal.MapboxRouteLineUtils.getRouteLineTrafficExpressionData
import com.mapbox.navigation.ui.maps.route.routeline.internal.MapboxRouteLineUtils.getStyledColor
import com.mapbox.navigation.ui.maps.route.routeline.internal.MapboxRouteLineUtils.getStyledFloatArray
import com.mapbox.navigation.ui.maps.route.routeline.model.RouteLineExpressionData
import com.mapbox.navigation.ui.maps.utils.TestUtils.loadJsonFixture
import com.mapbox.navigation.ui.maps.utils.ThemeSwitcher

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(sdk = [Build.VERSION_CODES.O_MR1])
@RunWith(RobolectricTestRunner::class)
class MapboxRouteControllerLineUtilsTest {

    lateinit var ctx: Context
    var styleRes: Int = 0

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
        styleRes = ThemeSwitcher.retrieveAttrResourceId(
            ctx,
            R.attr.navigationViewRouteStyle,
            R.style.MapboxStyleNavigationMapRoute
        )
    }

    @Test
    fun getStyledColorTest() {
        val result = getStyledColor(
            R.styleable.MapboxStyleNavigationMapRoute_routeColor,
            R.color.mapbox_navigation_route_layer_blue,
            ctx,
            styleRes
        )

        assertEquals(-11097861, result)
    }

    @Test
    fun getTrafficLineExpressionTest() {
        val expectedExpression = "[step, [line-progress], [rgba, 0.0, 0.0, 0.0, 0.0], 0.0," +
            " [rgba, 86.0, 168.0, 251.0, 1.0], 0.015670907645820537, " +
            "[rgba, 86.0, 168.0, 251.0, 1.0], 0.11898525632162987, " +
            "[rgba, 86.0, 168.0, 251.0, 1.0]]"

        val expressionDatas = listOf(
            RouteLineExpressionData(0.0, -11097861),
            RouteLineExpressionData(0.015670907645820537, -11097861),
            RouteLineExpressionData(0.11898525632162987, -11097861)
        )

        val result = MapboxRouteLineUtils.getTrafficLineExpression(
            0.0,
            expressionDatas,
            -11097861
        )

        assertEquals(expectedExpression, result.toString())
    }

    @Test
    fun getVanishingRouteLineExpressionTest() {
        val expectedExpression = "[step, [line-progress], [rgba, 255.0, 77.0, 77.0, 1.0]" +
            ", 3.0, [rgba, 86.0, 168.0, 251.0, 1.0]]"

        val result = MapboxRouteLineUtils.getVanishingRouteLineExpression(3.0, -45747, -11097861)

        assertEquals(expectedExpression, result.toString())
    }

    @Test
    fun calculateDistance() {
        val result = MapboxRouteLineUtils.calculateDistance(
            Point.fromLngLat(-122.525212, 37.974092),
            Point.fromLngLat(-122.52509389295653, 37.974569579999944))

        assertEquals(0.0000017145850113848236, result, 0.0)
    }

    @Test
    fun getStyledFloatArrayTest() {
        val result = getStyledFloatArray(
            R.styleable.MapboxStyleNavigationMapRoute_routeLineScaleStops,
            ctx,
            styleRes,
            R.styleable.MapboxStyleNavigationMapRoute
        )

        assertEquals(6, result.size)
        assertEquals(4.0f, result[0])
        assertEquals(10.0f, result[1])
        assertEquals(13.0f, result[2])
        assertEquals(16.0f, result[3])
        assertEquals(19.0f, result[4])
        assertEquals(22.0f, result[5])
    }

    @Test
    fun getStyledFloatArrayWhenResourceNotFount() {
        val result = getStyledFloatArray(
            0,
            ctx,
            styleRes,
            R.styleable.MapboxStyleNavigationMapRoute
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun getRouteLineScalingValuesTest() {
        val result = getRouteLineScalingValues(
            styleRes,
            ctx,
            R.styleable.MapboxStyleNavigationMapRoute_routeLineScaleStops,
            R.styleable.MapboxStyleNavigationMapRoute_routeLineScaleMultipliers,
            R.styleable.MapboxStyleNavigationMapRoute_routeLineScales,
            R.styleable.MapboxStyleNavigationMapRoute
        )

        assertEquals(result.size, 6)
        assertEquals(4.0f, result[0].scaleStop)
        assertEquals(3.0f, result[0].scaleMultiplier)
        assertEquals(1.0f, result[0].scale)
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

        val result = getRouteLineTrafficExpressionData(route)

        assertEquals(distances.size, result.size)
        assertEquals(distances.first(), result[1].distanceFromOrigin, 0.0)
        assertEquals(result[0].roadClass, roadClasses!!.first())
        assertEquals(result[2].distanceFromOrigin, distances.subList(0, 2).sum(), 0.0)
        assertEquals(distancesSum, result.last().distanceFromOrigin, 0.0)
        assertEquals(RouteConstants.LOW_CONGESTION_VALUE, result.last().trafficCongestionIdentifier)
        assertEquals("service", result.last().roadClass)
    }

    @Test
    fun getRouteLineTrafficExpressionWithRoadClassesDuplicatesRemoved() {
        val routeAsJsonJson = loadJsonFixture("route-with-road-classes.txt")
        val route = DirectionsRoute.fromJson(routeAsJsonJson)

        val result = getRouteLineTrafficExpressionData(route)

        assertEquals(10, result.size)
        assertEquals(1300.0000000000002, result.last().distanceFromOrigin, 0.0)
        assertEquals(RouteConstants.LOW_CONGESTION_VALUE, result.last().trafficCongestionIdentifier)
        assertEquals("service", result.last().roadClass)
    }

    @Test
    fun getRouteLineTrafficExpressionDataWithSomeRoadClassesDuplicatesRemoved() {
        val routeAsJsonJson = loadJsonFixture("motorway-route-with-road-classes-mixed.json")
        val route = DirectionsRoute.fromJson(routeAsJsonJson)

        val result = getRouteLineTrafficExpressionData(route)

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
        val congestionColorProvider: (String, Boolean) -> Int = { trafficCongestion, _ ->
            when (trafficCongestion) {
                RouteConstants.UNKNOWN_CONGESTION_VALUE -> -9
                RouteConstants.LOW_CONGESTION_VALUE -> -1
                else -> 33
            }
        }
        val routeAsJsonJson = loadJsonFixture("motorway-route-with-road-classes.json")
        val route = DirectionsRoute.fromJson(routeAsJsonJson)

        val trafficExpressionData = getRouteLineTrafficExpressionData(route)
        val result = getRouteLineExpressionDataWithStreetClassOverride(
            trafficExpressionData,
            route.distance(),
            congestionColorProvider,
            true,
            listOf("motorway")
        )

        assertTrue(result.all { it.segmentColor == -1 })
        assertEquals(1, result.size)
    }

    @Test
    fun getRouteLineExpressionDataWithSomeRoadClassesDuplicatesRemoved() {
        val congestionColorProvider: (String, Boolean) -> Int = { trafficCongestion, _ ->
            when (trafficCongestion) {
                RouteConstants.UNKNOWN_CONGESTION_VALUE -> -9
                RouteConstants.LOW_CONGESTION_VALUE -> -1
                else -> 33
            }
        }
        val routeAsJsonJson = loadJsonFixture("motorway-route-with-road-classes-mixed.json")
        val route = DirectionsRoute.fromJson(routeAsJsonJson)

        val trafficExpressionData = getRouteLineTrafficExpressionData(route)
        val result = getRouteLineExpressionDataWithStreetClassOverride(
            trafficExpressionData,
            route.distance(),
            congestionColorProvider,
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
    fun getRouteLineTrafficExpressionDataWithOutStreetClassesDuplicatesRemoved() {
        val routeAsJsonJson = loadJsonFixture("route-with-traffic-no-street-classes.txt")
        val route = DirectionsRoute.fromJson(routeAsJsonJson)

        val result = getRouteLineTrafficExpressionData(route)

        assertEquals(5, result.size)
        assertEquals(1188.7000000000003, result.last().distanceFromOrigin, 0.0)
        assertEquals(RouteConstants.LOW_CONGESTION_VALUE, result.last().trafficCongestionIdentifier)
        assertNull(result.last().roadClass)
    }

    @Test
    fun getRouteLineTrafficExpressionDataWithStreetClassesDuplicatesRemoved() {
        val congestionColorProvider: (String, Boolean) -> Int = { trafficCongestion, _ ->
            when (trafficCongestion) {
                RouteConstants.UNKNOWN_CONGESTION_VALUE -> -9
                RouteConstants.LOW_CONGESTION_VALUE -> -1
                else -> 33
            }
        }
        val routeAsJsonJson = loadJsonFixture("route-with-road-classes.txt")
        val route = DirectionsRoute.fromJson(routeAsJsonJson)
        val trafficExpressionData = getRouteLineTrafficExpressionData(route)
        assertEquals("service", trafficExpressionData[0].roadClass)
        assertEquals("street", trafficExpressionData[1].roadClass)
        assertEquals(RouteConstants.UNKNOWN_CONGESTION_VALUE, trafficExpressionData[0].trafficCongestionIdentifier)
        assertEquals(RouteConstants.UNKNOWN_CONGESTION_VALUE, trafficExpressionData[1].trafficCongestionIdentifier)

        val result = getRouteLineExpressionDataWithStreetClassOverride(
            trafficExpressionData,
            route.distance(),
            congestionColorProvider,
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
        val congestionColorProvider: (String, Boolean) -> Int = { trafficCongestion, _ ->
            when (trafficCongestion) {
                RouteConstants.UNKNOWN_CONGESTION_VALUE -> -9
                RouteConstants.LOW_CONGESTION_VALUE -> -1
                else -> 33
            }
        }
        val routeAsJsonJson = loadJsonFixture("route-with-traffic-no-street-classes.txt")
        val route = DirectionsRoute.fromJson(routeAsJsonJson)
        val trafficExpressionData = getRouteLineTrafficExpressionData(route)

        val result = getRouteLineExpressionDataWithStreetClassOverride(
            trafficExpressionData,
            route.distance(),
            congestionColorProvider,
            true,
            listOf()
        )

        assertEquals(5, result.size)
        assertEquals(0.23460041526970057, result[1].offset, 0.0)
        assertEquals(-1, result[1].segmentColor)
    }

    @Test
    fun getTrafficExpressionWithStreetClassOverrideOnMotorwayWhenChangeOutsideOfIntersections() {
        val congestionColorProvider: (String, Boolean) -> Int = { trafficCongestion, _ ->
            when (trafficCongestion) {
                RouteConstants.UNKNOWN_CONGESTION_VALUE -> -9
                RouteConstants.LOW_CONGESTION_VALUE -> -1
                RouteConstants.SEVERE_CONGESTION_VALUE -> -2
                else -> 33
            }
        }
        val routeAsJsonJson = loadJsonFixture(
            "motorway-route-with-road-classes-unknown-not-on-intersection.json"
        )
        val route = DirectionsRoute.fromJson(routeAsJsonJson)

        val trafficExpressionData = getRouteLineTrafficExpressionData(route)
        val result = getRouteLineExpressionDataWithStreetClassOverride(
            trafficExpressionData,
            route.distance(),
            congestionColorProvider,
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
        val routeAsJsonJson = loadJsonFixture(
            "route-with-missing-road-classes.json"
        )
        val route = DirectionsRoute.fromJson(routeAsJsonJson)

        val result = getRouteLineTrafficExpressionData(route)

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
        val congestionColorProvider: (String, Boolean) -> Int = { trafficCongestion, _ ->
            when (trafficCongestion) {
                RouteConstants.UNKNOWN_CONGESTION_VALUE -> -9
                RouteConstants.LOW_CONGESTION_VALUE -> -1
                else -> 33
            }
        }
        val routeAsJsonJson = loadJsonFixture(
            "motorway-with-road-classes-multi-leg.json"
        )
        val route = DirectionsRoute.fromJson(routeAsJsonJson)

        val trafficExpressionData = getRouteLineTrafficExpressionData(route)
        val result = getRouteLineExpressionDataWithStreetClassOverride(
            trafficExpressionData,
            route.distance(),
            congestionColorProvider,
            true,
            listOf("motorway")
        )

        assertTrue(result.all { it.segmentColor == -1 })
        assertEquals(1, result.size)
    }

    private fun getDirectionsRoute(): DirectionsRoute {
        val tokenHere = "someToken"
        val directionsRouteAsJson = loadJsonFixture("vanish_point_test.txt")
            ?.replace("tokenHere", tokenHere)

        return DirectionsRoute.fromJson(directionsRouteAsJson)
    }

    private fun getRoute(): DirectionsRoute {
        val routeAsJson = loadJsonFixture("short_route.json")
        return DirectionsRoute.fromJson(routeAsJson)
    }
}
