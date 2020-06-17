package com.mapbox.navigation.core.replay.route

import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.geojson.LineString
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMeasurement
import org.apache.commons.io.IOUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReplayRouteDriverTest {

    private val defaultOptions = ReplayRouteOptions.Builder().build()
    private val replayRouteDriver = ReplayRouteDriver()

    @Test
    fun `should have location every second`() {
        val geometry =
            """anq_gAxdhmhFbZkA^?tDMUsF?m@WmKMoHOeF]eO?}@GiBcB}s@?{@McGoDLu@?cUlAqMj@qfAtE"""

        val locations = replayRouteDriver.driveGeometry(defaultOptions, geometry)

        var time = 0L
        locations.forEach {
            assertEquals(time, it.timeMillis)
            time += 1000L
        }
    }

    @Test
    fun `should have location every second for multiple routes`() {
        val firstGeometry =
            """anq_gAxdhmhFbZkA^?tDMUsF?m@WmKMoHOeF]eO?}@GiBcB}s@?{@McGoDLu@?cUlAqMj@qfAtE"""
        val secondGeometry =
            """qnq_gAxdhmhFuvBlJe@?qC^"""

        val firstLegLocations = replayRouteDriver.driveGeometry(defaultOptions, firstGeometry)
        val secondLegLocations = replayRouteDriver.driveGeometry(defaultOptions, secondGeometry)

        var time = 0L
        firstLegLocations.forEach {
            assertEquals(time, it.timeMillis)
            time += 1000L
        }
        secondLegLocations.forEach {
            assertEquals(time, it.timeMillis)
            time += 1000L
        }
    }

    @Test
    fun `should slow down at the end of a route`() {
        val geometry =
            """qnq_gAxdhmhFuvBlJe@?qC^^`GD|@bBpq@pB~{@om@xCqL\"""

        val locations = replayRouteDriver.driveGeometry(defaultOptions, geometry)

        // This value is too high, need to slow down more
        locations.takeLast(3).map { it.speedMps }.fold(11.0) { lastSpeed, currentSpeed ->
            assertTrue("$currentSpeed < $lastSpeed", currentSpeed < lastSpeed)
            currentSpeed
        }
    }

    @Test
    fun `should not crash for smallest trip`() {
        val geometry =
            """ooq_gAbehmhFO@"""

        val locations = replayRouteDriver.driveGeometry(defaultOptions, geometry)

        assertEquals(2, locations.size)
    }

    @Test
    fun `should travel along the route at each step`() {
        val geometry =
            """inq_gAxdhmhF}vBlJe@?qC^mDLmcAfE]LqCNNpGF\`Bnr@pBp{@rBp{@bA|_@"""

        val locations = replayRouteDriver.driveGeometry(defaultOptions, geometry)

        var previous = locations[0]
        for (i in 1 until locations.size - 1) {
            val current = locations[i]
            val distance = TurfMeasurement
                .distance(previous.point, current.point, TurfConstants.UNIT_METERS)
            assertTrue("$i $distance > 0.0", distance > 0.0)
            previous = current
        }
    }

    @Test
    fun `should segment a short route`() {
        val geometry =
            """wt}ohAj||tfFoD`Sm_@iMcKgD"""

        val locations = replayRouteDriver.driveGeometry(defaultOptions, geometry)

        assertTrue("${locations.size} > 10", locations.size > 10)
    }

    @Test
    fun `should segment a ride with a u turn`() {
        val geometry =
            """wt}ohAj||tfFoD`Sm_@iMcPeFbPdFl_@hMcKvl@"""

        val locations = replayRouteDriver.driveGeometry(defaultOptions, geometry)

        assertTrue(locations.size > 10)
    }

    @Test
    fun `should not be weighted by duplicates`() {
        val points =
            LineString.fromJson(resourceAsString("not_be_weighted_by_duplicates_test.txt"))

        val locations = replayRouteDriver.drivePointList(defaultOptions, points.coordinates())

        assertEquals(points.coordinates().first(), locations.first().point)
        assertEquals(points.coordinates().last(), locations.last().point)
    }

    @Test
    fun `should look ahead for future slow downs`() {
        val points =
            LineString.fromJson(resourceAsString("look_ahead_for_future_slow_downs_test.txt"))

        val locations = replayRouteDriver.drivePointList(defaultOptions, points.coordinates())

        assertEquals(0.0, locations.first().speedMps, 0.1)
        assertEquals(0.0, locations.last().speedMps, 0.1)
    }

    @Test
    fun `mapRouteLegAnnotation should successfully map route leg annotations`() {
        val routeLeg: RouteLeg =
            RouteLeg.fromJson(resourceAsString("map_route_leg_annotation_test.txt"))

        val replayEvents = replayRouteDriver.driveRouteLeg(routeLeg)

        assertTrue("${replayEvents.size} >= 50", replayEvents.size >= 50)
    }

    private fun resourceAsString(
        name: String,
        packageName: String = "com.mapbox.navigation.core.replay.route"
    ): String {
        val inputStream = javaClass.classLoader?.getResourceAsStream("$packageName/$name")
        return IOUtils.toString(inputStream, "UTF-8")
    }
}
