package com.mapbox.navigation.core.replay.route

import com.mapbox.geojson.Point
import org.junit.Assert.assertTrue
import org.junit.Test

class ReplayRouteBenderTest {

    private val routeBender = ReplayRouteBender()

    @Test
    fun `should create five locations for right turn`() {
        val locations = listOf<Point>(
            Point.fromLngLat(-121.469564, 38.550752),
            Point.fromLngLat(-121.470253, 38.550968),
            Point.fromLngLat(-121.470024, 38.551490)
        ).mapIndexed { index, point -> ReplayRouteLocation(index, point) }

        val bentRoute = routeBender.bendRoute(locations)
        val maxBearingDelta = routeBender.maxBearingsDelta(bentRoute.map { it.point })

        assertTrue("$maxBearingDelta < ${ReplayRouteBender.MAX_BEARING_DELTA}",
            maxBearingDelta < ReplayRouteBender.MAX_BEARING_DELTA)
    }

    @Test
    fun `should create curves for routes with curves`() {
        val locations = listOf<Point>(
            Point.fromLngLat(-122.445946, 37.737075),
            Point.fromLngLat(-122.445954, 37.737083),
            Point.fromLngLat(-122.445992, 37.737106),
            Point.fromLngLat(-122.446198, 37.737266),
            Point.fromLngLat(-122.446328, 37.737361),
            Point.fromLngLat(-122.446396, 37.737422),
            Point.fromLngLat(-122.446435, 37.737457),
            Point.fromLngLat(-122.446457, 37.737495),
            Point.fromLngLat(-122.446488, 37.737541),
            Point.fromLngLat(-122.446511, 37.737594),
            Point.fromLngLat(-122.446518, 37.737644),
            Point.fromLngLat(-122.446518, 37.737667),
            Point.fromLngLat(-122.446618, 37.737659),
            Point.fromLngLat(-122.446648, 37.737655),
            Point.fromLngLat(-122.446694, 37.737651),
            Point.fromLngLat(-122.446724, 37.737651),
            Point.fromLngLat(-122.446755, 37.737655),
            Point.fromLngLat(-122.446816, 37.737682),
            Point.fromLngLat(-122.44693, 37.737735),
            Point.fromLngLat(-122.447053, 37.737789),
            Point.fromLngLat(-122.447205, 37.737857),
            Point.fromLngLat(-122.447388, 37.73793),
            Point.fromLngLat(-122.447518, 37.737972),
            Point.fromLngLat(-122.447655, 37.738006),
            Point.fromLngLat(-122.447785, 37.738033),
            Point.fromLngLat(-122.447922, 37.738056),
            Point.fromLngLat(-122.447999, 37.738063)
        ).mapIndexed { index, point -> ReplayRouteLocation(index, point) }

        val bentRoute = routeBender.bendRoute(locations)
        val maxBearingDelta = routeBender.maxBearingsDelta(bentRoute.map { it.point })

        assertTrue("$maxBearingDelta < ${ReplayRouteBender.MAX_BEARING_DELTA}",
            maxBearingDelta < ReplayRouteBender.MAX_BEARING_DELTA)
    }

//    @Test
//    fun `should profile u turns to be very slow`() {
//        val coordinates = listOf(
//            Point.fromLngLat(-121.470231, 38.550964),
//            Point.fromLngLat(-121.469887, 38.551753),
//            Point.fromLngLat(-121.470231, 38.550964)
//        )
//
//        val speedProfile = routeInterpolator.createSpeedProfile(defaultOptions, coordinates)
//
//        assertEquals(1.0, speedProfile[1].speedMps, defaultOptions.uTurnSpeedMps)
//    }
//
//    @Test
//    fun `should slow down for end of route`() {
//        val coordinates = listOf(
//            Point.fromLngLat(-122.444359, 37.736351),
//            Point.fromLngLat(-122.444359, 37.736347),
//            Point.fromLngLat(-122.444375, 37.736293),
//            Point.fromLngLat(-122.444413, 37.736213),
//            Point.fromLngLat(-122.444428, 37.736152),
//            Point.fromLngLat(-122.444443, 37.736091),
//            Point.fromLngLat(-122.444451, 37.736011),
//            Point.fromLngLat(-122.444481, 37.735916),
//            Point.fromLngLat(-122.444489, 37.735832),
//            Point.fromLngLat(-122.444497, 37.735752),
//            Point.fromLngLat(-122.444489, 37.735679),
//            Point.fromLngLat(-122.444474, 37.735614),
//            Point.fromLngLat(-122.444436, 37.735553),
//            Point.fromLngLat(-122.444367, 37.735511),
//            Point.fromLngLat(-122.444336, 37.735549),
//            Point.fromLngLat(-122.444306, 37.735576),
//            Point.fromLngLat(-122.444275, 37.735595),
//            Point.fromLngLat(-122.44423, 37.735614),
//            Point.fromLngLat(-122.444245, 37.735626),
//            Point.fromLngLat(-122.444298, 37.73571),
//            Point.fromLngLat(-122.444336, 37.735809),
//            Point.fromLngLat(-122.444359, 37.735897),
//            Point.fromLngLat(-122.444359, 37.73598),
//            Point.fromLngLat(-122.444352, 37.73608),
//            Point.fromLngLat(-122.444367, 37.736129),
//            Point.fromLngLat(-122.444375, 37.736141)
//        )
//
//        val speedProfile = routeInterpolator.createSpeedProfile(defaultOptions, coordinates)
//
//        speedProfile.forEach {
//            assertTrue("${it.speedMps} < 20.0", it.speedMps < 20.0)
//        }
//    }
//
//    @Test
//    fun `should slow down for upcoming turns`() {
//        val coordinates = listOf(
//            Point.fromLngLat(-122.445946, 37.737075),
//            Point.fromLngLat(-122.446511, 37.737594),
//            Point.fromLngLat(-122.447785, 37.738033),
//            Point.fromLngLat(-122.447999, 37.738063)
//        )
//
//        val speedProfile = routeInterpolator.createSpeedProfile(defaultOptions, coordinates)
//
//        // All coordinates should be used for this speed profile
//        assertEquals(4, speedProfile.size)
//        // Assume -4.0 minAcceleration for this test
//        assertEquals(-4.0, defaultOptions.minAcceleration, 0.001)
//        // To stop in 19 meters, you need to be going about 12mps
//        assertEquals(19.0, speedProfile[2].distance, 0.5)
//        assertEquals(12.0, speedProfile[2].speedMps, 0.5)
//    }
}
