package com.mapbox.navigation.core.replay.route

import com.mapbox.geojson.LineString
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMeasurement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReplayRouteDriverTest {

    private val defaultOptions = ReplayRouteOptions.Builder().build()
    private val replayRouteDriver = ReplayRouteDriver()

    @Test
    fun `should have location every second`() {
        val geometry = """anq_gAxdhmhFbZkA^?tDMUsF?m@WmKMoHOeF]eO?}@GiBcB}s@?{@McGoDLu@?cUlAqMj@qfAtE"""

        val locations = replayRouteDriver.driveGeometry(defaultOptions, geometry)

        var time = 0L
        locations.forEach {
            assertEquals(time, it.timeMillis)
            time += 1000L
        }
    }

    @Test
    fun `should have location every second for multiple routes`() {
        val firstGeometry = """anq_gAxdhmhFbZkA^?tDMUsF?m@WmKMoHOeF]eO?}@GiBcB}s@?{@McGoDLu@?cUlAqMj@qfAtE"""
        val secondGeometry = """qnq_gAxdhmhFuvBlJe@?qC^"""

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
        val geometry = """qnq_gAxdhmhFuvBlJe@?qC^^`GD|@bBpq@pB~{@om@xCqL\"""

        val locations = replayRouteDriver.driveGeometry(defaultOptions, geometry)

        // This value is too high, need to slow down more
        locations.takeLast(3).map { it.speedMps }.fold(11.0) { lastSpeed, currentSpeed ->
            assertTrue("$currentSpeed < $lastSpeed", currentSpeed < lastSpeed)
            currentSpeed
        }
    }

    @Test
    fun `should not crash for smallest trip`() {
        val geometry = """ooq_gAbehmhFO@"""

        val locations = replayRouteDriver.driveGeometry(defaultOptions, geometry)

        assertEquals(2, locations.size)
    }

    @Test
    fun `should travel along the route at each step`() {
        val geometry = """inq_gAxdhmhF}vBlJe@?qC^mDLmcAfE]LqCNNpGF\`Bnr@pBp{@rBp{@bA|_@"""

        val locations = replayRouteDriver.driveGeometry(defaultOptions, geometry)

        var previous = locations[0]
        for (i in 1 until locations.size - 1) {
            val current = locations[i]
            val distance = TurfMeasurement.distance(previous.point, current.point, TurfConstants.UNIT_METERS)
            assertTrue("$i $distance > 0.0", distance > 0.0)
            previous = current
        }
    }

    @Test
    fun `should segment a short route`() {
        val geometry = """wt}ohAj||tfFoD`Sm_@iMcKgD"""

        val locations = replayRouteDriver.driveGeometry(defaultOptions, geometry)

        assertTrue("${locations.size} > 10", locations.size > 10)
    }

    @Test
    fun `should segment a ride with a u turn`() {
        val geometry = """wt}ohAj||tfFoD`Sm_@iMcPeFbPdFl_@hMcKvl@"""

        val locations = replayRouteDriver.driveGeometry(defaultOptions, geometry)

        assertTrue(locations.size > 10)
    }

    @Test
    fun `should not be weighted by duplicates`() {
        val lineStringJson = """{"type":"LineString","coordinates":[[-121.469918,38.55088],[-121.470231,38.550964],[-121.470231,38.550964],[-121.470002,38.551483],[-121.469788,38.551998],[-121.469559,38.55252],[-121.469506,38.552646],[-121.46946,38.552745],[-121.469338,38.553028],[-121.469109,38.553565],[-121.468888,38.554073],[-121.468659,38.554592],[-121.468659,38.554592],[-121.468766,38.554622],[-121.468766,38.554622],[-121.468766,38.554622]]}"""
        val points = LineString.fromJson(lineStringJson)

        val locations = replayRouteDriver.drivePointList(defaultOptions, points.coordinates())

        assertEquals(points.coordinates().first(), locations.first().point)
        assertEquals(points.coordinates().last(), locations.last().point)
    }

    @Test
    fun `should look ahead for future slow downs`() {
        val lineStringJson = """{"type":"LineString","coordinates":[[-122.445946,37.737075],[-122.445954,37.737083],[-122.445992,37.737106],[-122.446198,37.737266],[-122.446328,37.737361],[-122.446396,37.737422],[-122.446435,37.737457],[-122.446457,37.737495],[-122.446488,37.737541],[-122.446511,37.737594],[-122.446518,37.737644],[-122.446518,37.737667],[-122.446618,37.737659],[-122.446648,37.737655],[-122.446694,37.737651],[-122.446724,37.737651],[-122.446755,37.737655],[-122.446816,37.737682],[-122.44693,37.737735],[-122.447053,37.737789],[-122.447205,37.737857],[-122.447388,37.73793],[-122.447518,37.737972],[-122.447655,37.738006],[-122.447785,37.738033],[-122.447922,37.738056],[-122.447999,37.738063]]}"""
        val points = LineString.fromJson(lineStringJson)

        val locations = replayRouteDriver.drivePointList(defaultOptions, points.coordinates())

        assertEquals(0.0, locations.first().speedMps, 0.1)
        assertEquals(0.0, locations.last().speedMps, 0.1)
    }
}
