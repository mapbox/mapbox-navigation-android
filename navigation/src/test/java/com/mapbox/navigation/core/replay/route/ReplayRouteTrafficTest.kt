package com.mapbox.navigation.core.replay.route

import com.mapbox.geojson.Point
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReplayRouteTrafficTest {

    private val replayRouteTraffic = ReplayRouteTraffic()

    @Test
    fun `should make all traffic locations on route`() {
        val coordinates = listOf(
            Point.fromLngLat(-121.466851, 38.563008),
            Point.fromLngLat(-121.467087, 38.562465),
            Point.fromLngLat(-121.467134, 38.562477),
            Point.fromLngLat(-121.467213, 38.562301),
            Point.fromLngLat(-121.467236, 38.562249),
            Point.fromLngLat(-121.46786, 38.562416),
            Point.fromLngLat(-121.468017, 38.562054),
            Point.fromLngLat(-121.467804, 38.561996),
        )
        val distances = listOf(
            63.788258828152905,
            4.300030987331445,
            20.74656338784123,
            6.119912374175893,
            57.36079720958969,
            42.51621717661491,
            19.61608648662988,
        )
        val speeds = listOf(2.2, 2.3, 2.2, 0.6, 7.5, 1.7, 2.2)

        val trafficLocations = replayRouteTraffic.trafficLocations(coordinates, distances, speeds)

        assertEquals(distances.size, trafficLocations.size)
        trafficLocations.forEach { assertTrue(coordinates.contains(it.point)) }
    }

    @Test
    fun `should make traffic locations at every each distance`() {
        val coordinates = listOf(
            Point.fromLngLat(-121.466851, 38.563008),
            Point.fromLngLat(-121.467087, 38.562465),
            Point.fromLngLat(-121.467134, 38.562477),
            Point.fromLngLat(-121.467213, 38.562301),
            Point.fromLngLat(-121.467236, 38.562249),
            Point.fromLngLat(-121.46786, 38.562416),
            Point.fromLngLat(-121.468017, 38.562054),
            Point.fromLngLat(-121.467804, 38.561996),
        )
        val distances = listOf(
            63.788258828152905,
            4.300030987331445,
            20.74656338784123,
            6.119912374175893,
            57.36079720958969,
            42.51621717661491,
            19.61608648662988,
        )
        val speeds = listOf(2.2, 2.3, 2.2, 0.6, 7.5, 1.7, 2.2)

        val trafficLocations = replayRouteTraffic.trafficLocations(coordinates, distances, speeds)

        distances.forEachIndexed { index, d ->
            assertEquals(
                d,
                trafficLocations[index].distance,
                0.1,
            )
        }
        speeds.forEachIndexed { index, s ->
            assertEquals(
                s,
                trafficLocations[index].speedMps,
                0.01,
            )
        }
    }

    @Test
    fun `should make all traffic locations on route even with duplicate coordinates`() {
        val coordinates = listOf(
            Point.fromLngLat(-121.466851, 38.563008),
            Point.fromLngLat(-121.467087, 38.562465),
            Point.fromLngLat(-121.467087, 38.562465),
            Point.fromLngLat(-121.467087, 38.562465),
            Point.fromLngLat(-121.467134, 38.562477),
            Point.fromLngLat(-121.467213, 38.562301),
            Point.fromLngLat(-121.467236, 38.562249),
            Point.fromLngLat(-121.467236, 38.562249),
            Point.fromLngLat(-121.467236, 38.562249),
            Point.fromLngLat(-121.46786, 38.562416),
            Point.fromLngLat(-121.468017, 38.562054),
            Point.fromLngLat(-121.468017, 38.562054),
            Point.fromLngLat(-121.467804, 38.561996),
        )
        val distances = listOf(
            63.788258828152905,
            4.300030987331445,
            20.74656338784123,
            6.119912374175893,
            57.36079720958969,
            42.51621717661491,
            19.61608648662988,
        )
        val speeds = listOf(2.2, 2.3, 2.2, 0.6, 7.5, 1.7, 2.2)

        val trafficLocations = replayRouteTraffic.trafficLocations(coordinates, distances, speeds)

        assertEquals(distances.size, trafficLocations.size)
        trafficLocations.forEach { assertTrue(coordinates.contains(it.point)) }
    }

    @Test
    fun `should make traffic locations when there are less distance values than route points`() {
        val coordinates = listOf(
            Point.fromLngLat(-121.466851, 38.563008),
            Point.fromLngLat(-121.467087, 38.562465),
            Point.fromLngLat(-121.467134, 38.562477),
            Point.fromLngLat(-121.467213, 38.562301),
            Point.fromLngLat(-121.467236, 38.562249),
            Point.fromLngLat(-121.46786, 38.562416),
            Point.fromLngLat(-121.468017, 38.562054),
            Point.fromLngLat(-121.467804, 38.561996),
        )
        val distances = listOf(63.788258828152905, 25.0465943752, 63.4807095838, 62.1323036632)
        val speeds = listOf(2.2, 2.3, 7.5, 2.2)

        val trafficLocations = replayRouteTraffic.trafficLocations(coordinates, distances, speeds)

        assertEquals(distances.size, trafficLocations.size)
        trafficLocations.forEach { assertTrue(coordinates.contains(it.point)) }
    }
}
