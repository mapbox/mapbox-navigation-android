package com.mapbox.navigation.core.replay.route

import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.geojson.LineString
import com.mapbox.geojson.utils.PolylineUtils
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
        val points = PolylineUtils.decode(
            """anq_gAxdhmhFbZkA^?tDMUsF?m@WmKMoHOeF]eO?}@GiBcB}s@?{@McGoDLu@?cUlAqMj@qfAtE""",
            6,
        )

        val locations = replayRouteDriver.drivePointList(defaultOptions, points)

        locations.windowed(size = 2) {
            val deltaMillis = it.last().timeSeconds - it.first().timeSeconds
            assertEquals(1.0, deltaMillis, 0.2)
        }
    }

    @Test
    fun `should complete short routes`() {
        val points = PolylineUtils.decode("""qnq_gAxdhmhFuvBlJe@?qC^""", 6)
        val locations = replayRouteDriver.drivePointList(defaultOptions, points)

        assertTrue("${locations.size} < 20", locations.size < 20)
    }

    @Test
    fun `should have location every second for multiple routes`() {
        val options = defaultOptions.toBuilder().frequency(1.0).build()
        val firstPoints = PolylineUtils.decode(
            """anq_gAxdhmhFbZkA^?tDMUsF?m@WmKMoHOeF]eO?}@GiBcB}s@?{@McGoDLu@?cUlAqMj@qfAtE""",
            6,
        )
        val secondPoints = PolylineUtils.decode("""qnq_gAxdhmhFuvBlJe@?qC^""", 6)

        val firstLegLocations = replayRouteDriver.drivePointList(options, firstPoints)
        val secondLegLocations = replayRouteDriver.drivePointList(options, secondPoints)
        val flattenedLocations = listOf(firstLegLocations, secondLegLocations).flatten()

        flattenedLocations.windowed(size = 2) {
            val deltaMillis = it.last().timeSeconds - it.first().timeSeconds
            assertEquals(1.0, deltaMillis, 0.2)
        }
    }

    @Test
    fun `should have ten locations every second for multiple routes with higher frequency`() {
        val options = defaultOptions.toBuilder().frequency(10.0).build()
        val firstPoints = PolylineUtils.decode(
            """anq_gAxdhmhFbZkA^?tDMUsF?m@WmKMoHOeF]eO?}@GiBcB}s@?{@McGoDLu@?cUlAqMj@qfAtE""",
            6,
        )
        val secondPoints = PolylineUtils.decode("""qnq_gAxdhmhFuvBlJe@?qC^""", 6)

        val firstLegLocations = replayRouteDriver.drivePointList(options, firstPoints)
        val secondLegLocations = replayRouteDriver.drivePointList(options, secondPoints)
        val flattenedLocations = listOf(firstLegLocations, secondLegLocations).flatten()

        flattenedLocations.windowed(size = 2) {
            val deltaMillis = it.last().timeSeconds - it.first().timeSeconds
            assertEquals(0.1, deltaMillis, 0.02)
        }
    }

    @Test
    fun `should slow down at the end of a route`() {
        val points = PolylineUtils.decode("""qnq_gAxdhmhFuvBlJe@?qC^^`GD|@bBpq@pB~{@om@xCqL\""", 6)

        val locations = replayRouteDriver.drivePointList(defaultOptions, points)

        // This value is too high, need to slow down more
        locations.takeLast(3).map { it.speedMps }.fold(11.0) { lastSpeed, currentSpeed ->
            assertTrue("$currentSpeed < $lastSpeed", currentSpeed < lastSpeed)
            currentSpeed
        }
    }

    @Test
    fun `should complete the smallest trip`() {
        val points = PolylineUtils.decode("""ooq_gAbehmhFO@""", 6)

        val locations = replayRouteDriver.drivePointList(defaultOptions, points)

        val lastLocation = locations.last()
        assertTrue(locations.size >= 3)
        assertEquals(0.0, lastLocation.speedMps, 0.001)
        assertEquals(0.89, lastLocation.distance, 0.01)
        assertTrue(lastLocation.timeMillis < 3000L)
    }

    @Test
    fun `should travel along the route at each step`() {
        val points = PolylineUtils.decode(
            """inq_gAxdhmhF}vBlJe@?qC^mDLmcAfE]LqCNNpGF\`Bnr@pBp{@rBp{@bA|_@""",
            6,
        )

        val locations = replayRouteDriver.drivePointList(defaultOptions, points)

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
        val points = PolylineUtils.decode("""wt}ohAj||tfFoD`Sm_@iMcKgD""", 6)

        val locations = replayRouteDriver.drivePointList(defaultOptions, points)

        assertTrue("${locations.size} > 10", locations.size > 10)
    }

    @Test
    fun `should segment a ride with a u turn`() {
        val geometry = PolylineUtils.decode("""wt}ohAj||tfFoD`Sm_@iMcPeFbPdFl_@hMcKvl@""", 6)

        val locations = replayRouteDriver.drivePointList(defaultOptions, geometry)

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
        packageName: String = "com.mapbox.navigation.core.replay.route",
    ): String {
        val inputStream = javaClass.classLoader?.getResourceAsStream("$packageName/$name")
        return IOUtils.toString(inputStream, "UTF-8")
    }
}
