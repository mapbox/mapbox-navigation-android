package com.mapbox.navigation.core.replay.route

import com.mapbox.geojson.Point
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReplayRouteInterpolatorTest {

    private val defaultOptions = ReplayRouteOptions.Builder().build()
    private val routeInterpolator = ReplayRouteInterpolator()

    @Test
    fun `should accelerate to max speed`() {
        val startSpeedMps = 0.0
        val maxSpeedMps = defaultOptions.maxSpeedMps
        val distanceMeters = 200.0

        val segment = routeInterpolator.interpolateSpeed(
            defaultOptions,
            startSpeedMps,
            maxSpeedMps,
            distanceMeters
        )

        var currentSpeed = 0.0
        segment.steps.forEach {
            val speedMps = it.speedMps
            assertTrue("$currentSpeed <= $speedMps", currentSpeed <= speedMps)
            assertTrue("$currentSpeed <= $maxSpeedMps", currentSpeed <= maxSpeedMps)
            currentSpeed = speedMps
        }
        assertEquals(segment.steps.last().speedMps, defaultOptions.maxSpeedMps, 0.001)
        assertTrue("${segment.steps.size} < 14", segment.steps.size < 14)
    }

    @Test
    fun `should not exceed max acceleration`() {
        val startSpeedMps = 0.0
        val maxSpeedMps = defaultOptions.maxSpeedMps
        val distanceMeters = 200.0

        val segment = routeInterpolator.interpolateSpeed(
            defaultOptions,
            startSpeedMps,
            maxSpeedMps,
            distanceMeters
        )

        segment.steps.forEach {
            assertTrue(it.acceleration <= defaultOptions.maxAcceleration)
        }
    }

    @Test
    fun `should start and end at the same speed`() {
        val startSpeedMps = 20.0
        val endSpeedMps = 20.0
        val distanceMeters = 200.0

        val segment = routeInterpolator.interpolateSpeed(
            defaultOptions,
            startSpeedMps,
            endSpeedMps,
            distanceMeters
        )

        segment.steps.apply {
            assertTrue(size > 5)
            assertTrue(first().speedMps <= startSpeedMps)
            assertEquals(last().speedMps, endSpeedMps, 0.001)
        }
        assertTrue("${segment.steps.size} < 14", segment.steps.size < 11)
    }

    @Test
    fun `should come close to a stop from high speed`() {
        val startSpeedMps = 30.0
        val endSpeedMps = 0.0
        val distanceMeters = 166.123

        val segment = routeInterpolator.interpolateSpeed(
            defaultOptions,
            startSpeedMps,
            endSpeedMps,
            distanceMeters
        )

        segment.steps.apply {
            assertTrue(first().speedMps <= startSpeedMps)
            assertTrue(last().speedMps < 0.001)
        }
        assertTrue("${segment.steps.size} < 12", segment.steps.size < 12)
    }

    @Test
    fun `should come close to stop from low speed`() {
        val startSpeedMps = 6.0
        val endSpeedMps = 0.0
        val distanceMeters = 107.197

        val segment = routeInterpolator.interpolateSpeed(
            defaultOptions,
            startSpeedMps,
            endSpeedMps,
            distanceMeters
        )

        segment.steps.apply {
            assertTrue(first().speedMps <= startSpeedMps)
            assertTrue(last().speedMps < 0.001)
        }
        assertTrue("${segment.steps.size} < 13", segment.steps.size < 13)
    }

    @Test
    fun `should reach end at correct distance`() {
        val startSpeedMps = 9.54496
        val endSpeedMps = 9.3481
        val distanceMeters = 361.637

        val segment = routeInterpolator.interpolateSpeed(
            defaultOptions,
            startSpeedMps,
            endSpeedMps,
            distanceMeters
        )

        segment.steps.apply {
            assertTrue(size > 5)
            assertEquals(last().positionMeters, 361.637, 0.001)
        }
        assertTrue("${segment.steps.size} < 20", segment.steps.size < 20)
    }

    @Test
    fun `should handle distances at low precision`() {
        val startSpeedMps = 0.0
        val endSpeedMps = 3.0
        val distanceMeters = 223.96630390737917

        val segment = routeInterpolator.interpolateSpeed(
            defaultOptions,
            startSpeedMps,
            endSpeedMps,
            distanceMeters
        )

        assertEquals(223.96630390737917, segment.steps.last().positionMeters, 0.00001)
        assertEquals(3.0, segment.endSpeedMps, 0.0001)
    }

    @Test
    fun `should create speed for each point`() {
        val coordinates = listOf(
            Point.fromLngLat(-121.46991, 38.550876),
            Point.fromLngLat(-121.470231, 38.550964),
            Point.fromLngLat(-121.468834, 38.550765)
        )

        val speedProfile = routeInterpolator.createSpeedProfile(defaultOptions, coordinates)

        assertEquals(speedProfile.size, coordinates.size)
    }

    @Test
    fun `should profile u turns to be very slow`() {
        val coordinates = listOf(
            Point.fromLngLat(-121.470231, 38.550964),
            Point.fromLngLat(-121.469887, 38.551753),
            Point.fromLngLat(-121.470231, 38.550964)
        )

        val speedProfile = routeInterpolator.createSpeedProfile(defaultOptions, coordinates)

        assertEquals(1.0, speedProfile[1].speedMps, defaultOptions.uTurnSpeedMps)
    }

    @Test
    fun `should slow down for end of route`() {
        val coordinates = listOf(
            Point.fromLngLat(-122.444359, 37.736351),
            Point.fromLngLat(-122.444359, 37.736347),
            Point.fromLngLat(-122.444375, 37.736293),
            Point.fromLngLat(-122.444413, 37.736213),
            Point.fromLngLat(-122.444428, 37.736152),
            Point.fromLngLat(-122.444443, 37.736091),
            Point.fromLngLat(-122.444451, 37.736011),
            Point.fromLngLat(-122.444481, 37.735916),
            Point.fromLngLat(-122.444489, 37.735832),
            Point.fromLngLat(-122.444497, 37.735752),
            Point.fromLngLat(-122.444489, 37.735679),
            Point.fromLngLat(-122.444474, 37.735614),
            Point.fromLngLat(-122.444436, 37.735553),
            Point.fromLngLat(-122.444367, 37.735511),
            Point.fromLngLat(-122.444336, 37.735549),
            Point.fromLngLat(-122.444306, 37.735576),
            Point.fromLngLat(-122.444275, 37.735595),
            Point.fromLngLat(-122.44423, 37.735614),
            Point.fromLngLat(-122.444245, 37.735626),
            Point.fromLngLat(-122.444298, 37.73571),
            Point.fromLngLat(-122.444336, 37.735809),
            Point.fromLngLat(-122.444359, 37.735897),
            Point.fromLngLat(-122.444359, 37.73598),
            Point.fromLngLat(-122.444352, 37.73608),
            Point.fromLngLat(-122.444367, 37.736129),
            Point.fromLngLat(-122.444375, 37.736141)
        )

        val speedProfile = routeInterpolator.createSpeedProfile(defaultOptions, coordinates)

        speedProfile.forEach {
            assertTrue("${it.speedMps} < 20.0", it.speedMps < 20.0)
        }
    }

    @Test
    fun `should slow down for upcoming turns`() {
        val coordinates = listOf(
            Point.fromLngLat(-122.445946, 37.737075),
            Point.fromLngLat(-122.446511, 37.737594),
            Point.fromLngLat(-122.447785, 37.738033),
            Point.fromLngLat(-122.447999, 37.738063)
        )

        val speedProfile = routeInterpolator.createSpeedProfile(defaultOptions, coordinates)

        // All coordinates should be used for this speed profile
        assertEquals(4, speedProfile.size)
        // Assume -4.0 minAcceleration for this test
        assertEquals(-4.0, defaultOptions.minAcceleration, 0.001)
        // To stop in 19 meters, you need to be going about 12mps
        assertEquals(19.0, speedProfile[2].distance, 0.5)
        assertEquals(12.0, speedProfile[2].speedMps, 0.5)
    }
}
