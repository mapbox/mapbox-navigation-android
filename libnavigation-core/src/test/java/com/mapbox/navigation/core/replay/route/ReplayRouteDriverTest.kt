package com.mapbox.navigation.core.replay.route

import com.mapbox.api.directions.v5.models.RouteLeg
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
            val distance = TurfMeasurement.distance(previous.point, current.point, TurfConstants.UNIT_METERS)
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
        val lineStringJson =
            """{"type":"LineString","coordinates":[[-121.469918,38.55088],[-121.470231,38.550964],[-121.470231,38.550964],[-121.470002,38.551483],[-121.469788,38.551998],[-121.469559,38.55252],[-121.469506,38.552646],[-121.46946,38.552745],[-121.469338,38.553028],[-121.469109,38.553565],[-121.468888,38.554073],[-121.468659,38.554592],[-121.468659,38.554592],[-121.468766,38.554622],[-121.468766,38.554622],[-121.468766,38.554622]]}"""
        val points = LineString.fromJson(lineStringJson)

        val locations = replayRouteDriver.drivePointList(defaultOptions, points.coordinates())

        assertEquals(points.coordinates().first(), locations.first().point)
        assertEquals(points.coordinates().last(), locations.last().point)
    }

    @Test
    fun `should look ahead for future slow downs`() {
        val lineStringJson =
            """{"type":"LineString","coordinates":[[-122.445946,37.737075],[-122.445954,37.737083],[-122.445992,37.737106],[-122.446198,37.737266],[-122.446328,37.737361],[-122.446396,37.737422],[-122.446435,37.737457],[-122.446457,37.737495],[-122.446488,37.737541],[-122.446511,37.737594],[-122.446518,37.737644],[-122.446518,37.737667],[-122.446618,37.737659],[-122.446648,37.737655],[-122.446694,37.737651],[-122.446724,37.737651],[-122.446755,37.737655],[-122.446816,37.737682],[-122.44693,37.737735],[-122.447053,37.737789],[-122.447205,37.737857],[-122.447388,37.73793],[-122.447518,37.737972],[-122.447655,37.738006],[-122.447785,37.738033],[-122.447922,37.738056],[-122.447999,37.738063]]}"""
        val points = LineString.fromJson(lineStringJson)

        val locations = replayRouteDriver.drivePointList(defaultOptions, points.coordinates())

        assertEquals(0.0, locations.first().speedMps, 0.1)
        assertEquals(0.0, locations.last().speedMps, 0.1)
    }

    @Test
    fun `mapRouteLegAnnotation should successfully map route leg annotations`() {
        val routeLeg: RouteLeg = RouteLeg.fromJson("""{"distance":214.4,"duration":105.5,"summary":"S Street, 32nd Street","steps":[{"distance":95.0,"duration":52.9,"geometry":"_kuphAd}vtfF|`@vMW|A~I|CfBl@","name":"","mode":"driving","maneuver":{"location":[-121.466851,38.563008],"bearing_before":0.0,"bearing_after":199.0,"instruction":"Head south","type":"depart","modifier":"right"},"voiceInstructions":[{"distanceAlongGeometry":95.0,"announcement":"Head south, then turn right onto S Street","ssmlAnnouncement":"\u003cspeak\u003e\u003camazon:effect name\u003d\"drc\"\u003e\u003cprosody rate\u003d\"1.08\"\u003eHead south, then turn right onto S Street\u003c/prosody\u003e\u003c/amazon:effect\u003e\u003c/speak\u003e"},{"distanceAlongGeometry":26.9,"announcement":"Turn right onto S Street, then turn left onto 32nd Street","ssmlAnnouncement":"\u003cspeak\u003e\u003camazon:effect name\u003d\"drc\"\u003e\u003cprosody rate\u003d\"1.08\"\u003eTurn right onto S Street, then turn left onto \u003csay-as interpret-as\u003d\"address\"\u003e32nd\u003c/say-as\u003e Street\u003c/prosody\u003e\u003c/amazon:effect\u003e\u003c/speak\u003e"}],"bannerInstructions":[{"distanceAlongGeometry":95.0,"primary":{"text":"S Street","components":[{"text":"S Street","type":"text","abbr":"S St","abbr_priority":0}],"type":"turn","modifier":"right"}},{"distanceAlongGeometry":26.9,"primary":{"text":"S Street","components":[{"text":"S Street","type":"text","abbr":"S St","abbr_priority":0}],"type":"turn","modifier":"right"},"sub":{"text":"32nd Street","components":[{"text":"32nd Street","type":"text","abbr":"32nd St","abbr_priority":0}],"type":"turn","modifier":"left"}}],"driving_side":"right","weight":185.7,"intersections":[{"location":[-121.466851,38.563008],"bearings":[199],"entry":[true],"out":0},{"location":[-121.467087,38.562465],"bearings":[15,105,240],"entry":[false,true,true],"in":0,"out":2}]},{"distance":57.4,"duration":13.0,"geometry":"q{sphAfuwtfFmI~e@","name":"S Street","mode":"driving","maneuver":{"location":[-121.467236,38.562249],"bearing_before":198.0,"bearing_after":288.0,"instruction":"Turn right onto S Street","type":"end of road","modifier":"right"},"voiceInstructions":[{"distanceAlongGeometry":57.4,"announcement":"Turn left onto 32nd Street, then turn left onto S Street Serra Way Alley","ssmlAnnouncement":"\u003cspeak\u003e\u003camazon:effect name\u003d\"drc\"\u003e\u003cprosody rate\u003d\"1.08\"\u003eTurn left onto \u003csay-as interpret-as\u003d\"address\"\u003e32nd\u003c/say-as\u003e Street, then turn left onto S Street Serra Way Alley\u003c/prosody\u003e\u003c/amazon:effect\u003e\u003c/speak\u003e"}],"bannerInstructions":[{"distanceAlongGeometry":57.4,"primary":{"text":"32nd Street","components":[{"text":"32nd Street","type":"text","abbr":"32nd St","abbr_priority":0}],"type":"turn","modifier":"left"},"sub":{"text":"S Street Serra Way Alley","components":[{"text":"S Street","type":"text","abbr":"S St","abbr_priority":0},{"text":"Serra Way Alley","type":"text","abbr":"Serra Way Aly","abbr_priority":0}],"type":"turn","modifier":"left"}}],"driving_side":"right","weight":45.4,"intersections":[{"location":[-121.467236,38.562249],"bearings":[15,105,285],"entry":[false,true,true],"in":0,"out":2}]},{"distance":42.5,"duration":30.8,"geometry":"_ftphAf|xtfFrUxH","name":"32nd Street","mode":"driving","maneuver":{"location":[-121.46786,38.562416],"bearing_before":288.0,"bearing_after":198.0,"instruction":"Turn left onto 32nd Street","type":"turn","modifier":"left"},"voiceInstructions":[{"distanceAlongGeometry":20.7,"announcement":"Turn left onto S Street Serra Way Alley, then you will arrive at your destination","ssmlAnnouncement":"\u003cspeak\u003e\u003camazon:effect name\u003d\"drc\"\u003e\u003cprosody rate\u003d\"1.08\"\u003eTurn left onto S Street Serra Way Alley, then you will arrive at your destination\u003c/prosody\u003e\u003c/amazon:effect\u003e\u003c/speak\u003e"}],"bannerInstructions":[{"distanceAlongGeometry":42.5,"primary":{"text":"S Street Serra Way Alley","components":[{"text":"S Street","type":"text","abbr":"S St","abbr_priority":0},{"text":"Serra Way Alley","type":"text","abbr":"Serra Way Aly","abbr_priority":0}],"type":"turn","modifier":"left"}}],"driving_side":"right","weight":62.9,"intersections":[{"location":[-121.46786,38.562416],"bearings":[15,105,195,285],"entry":[true,false,true,true],"in":1,"out":2}]},{"distance":19.6,"duration":8.8,"geometry":"kosphA`fytfFrBiL","name":"S Street Serra Way Alley","mode":"driving","maneuver":{"location":[-121.468017,38.562054],"bearing_before":198.0,"bearing_after":108.0,"instruction":"Turn left onto S Street Serra Way Alley","type":"turn","modifier":"left"},"voiceInstructions":[{"distanceAlongGeometry":11.1,"announcement":"You have arrived at your destination, on the right","ssmlAnnouncement":"\u003cspeak\u003e\u003camazon:effect name\u003d\"drc\"\u003e\u003cprosody rate\u003d\"1.08\"\u003eYou have arrived at your destination, on the right\u003c/prosody\u003e\u003c/amazon:effect\u003e\u003c/speak\u003e"}],"bannerInstructions":[{"distanceAlongGeometry":19.6,"primary":{"text":"You will arrive","components":[{"text":"You will arrive","type":"text"}],"type":"arrive","modifier":"right"}},{"distanceAlongGeometry":11.1,"primary":{"text":"You have arrived","components":[{"text":"You have arrived","type":"text"}],"type":"arrive","modifier":"right"}}],"driving_side":"right","weight":8.8,"intersections":[{"location":[-121.468017,38.562054],"bearings":[15,105,195,285],"entry":[false,true,true,true],"in":0,"out":1}]},{"distance":0.0,"duration":0.0,"geometry":"wksphAvxxtfF","name":"S Street Serra Way Alley","mode":"driving","maneuver":{"location":[-121.467804,38.561996],"bearing_before":109.0,"bearing_after":0.0,"instruction":"You have arrived at your destination, on the right","type":"arrive","modifier":"right"},"voiceInstructions":[],"bannerInstructions":[],"driving_side":"right","weight":0.0,"intersections":[{"location":[-121.467804,38.561996],"bearings":[289],"entry":[true],"in":0}]}],"annotation":{"distance":[63.788258828152905,4.300030987331445,20.74656338784123,6.119912374175893,57.36079720958969,42.51621717661491,19.61608648662988],"speed":[2.2,2.3,2.2,0.6,7.5,1.7,2.2],"congestion":["unknown","unknown","unknown","unknown","low","unknown","unknown"]}}""")

        val replayEvents = replayRouteDriver.driveRouteLeg(routeLeg)

        assertTrue("${replayEvents.size} >= 50", replayEvents.size >= 50)
    }
}
