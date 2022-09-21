package com.mapbox.navigation.ui.maps.internal.location

import com.mapbox.geojson.Point
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.math.pow
import kotlin.math.sqrt

@RunWith(RobolectricTestRunner::class)
class ConstantVelocityInterpolatorTest {

    @Test
    fun `should calculate timings for constant velocity`() {
        val p0 = Point.fromLngLat(0.0, 0.0)
        val keyPoints = arrayOf(
            Point.fromLngLat(0.3, 0.4),
            Point.fromLngLat(0.6, 0.8),
            Point.fromLngLat(1.0, 1.0),
        )

        val sut = ConstantVelocityInterpolator(p0, keyPoints)

        val path = calculatePathValues(p0, *keyPoints)
        assertEquals(keyPoints.size, path.size)
        path.forEach { (outTime, inTime) ->
            assertEquals(outTime, sut.getInterpolation(inTime))
        }
    }

    @Test
    fun `should not use keypoints that are at ZERO distance from previous point`() {
        val p0 = Point.fromLngLat(0.0, 0.0)
        val keyPoints = arrayOf(
            Point.fromLngLat(0.5, 0.5),
            Point.fromLngLat(0.5, 0.5),
            Point.fromLngLat(1.0, 1.0),
        )

        val sut = ConstantVelocityInterpolator(p0, keyPoints)

        val path = calculatePathValues(p0, *keyPoints)
        assertEquals(keyPoints.size - 1, path.size)
        path.forEach { (outTime, inTime) ->
            assertEquals(outTime, sut.getInterpolation(inTime))
        }
    }

    @Test
    fun `should use linear interpolation for key points at zero distance`() {
        val p0 = Point.fromLngLat(1.0, 1.0)

        assertEquals(
            0.7f,
            ConstantVelocityInterpolator(p0, emptyArray()).getInterpolation(0.7f)
        )
        assertEquals(
            0.3f,
            ConstantVelocityInterpolator(p0, arrayOf(p0)).getInterpolation(0.3f)
        )
    }

    @Test
    fun `should use linear interpolation for single key point`() {
        val p0 = Point.fromLngLat(1.0, 1.0)
        val p1 = Point.fromLngLat(2.0, 2.0)

        assertEquals(
            0.3f,
            ConstantVelocityInterpolator(p0, arrayOf(p1)).getInterpolation(0.3f)
        )
    }

    private fun calculatePathValues(p0: Point, vararg points: Point): List<Pair<Float, Float>> {
        val distances = mutableListOf<Double>()
        var totalDistance = 0.0
        points.fold(p0) { a, b ->
            val d = distance(a, b)
            if (0 < d) {
                distances.add(d)
                totalDistance += d
            }
            b
        }

        val timeStep = 1.0f / distances.size
        val velocities = mutableListOf<Double>()
        val path = mutableListOf<Pair<Float, Float>>() // animationTime to scaledTime
        var pathTime = 0.0
        distances.forEachIndexed { i, d ->
            val dt = d / totalDistance
            velocities.add(d / dt)
            pathTime += dt
            path.add(timeStep * (i + 1) to pathTime.toFloat())
        }

        // verify constant velocity
        for (i in 1 until distances.size) {
            assertEquals(velocities[i - 1], velocities[i], 0.0001)
        }

        return path
    }

    private fun distance(a: Point, b: Point) =
        sqrt(
            (b.latitude() - a.latitude()).pow(2.0) +
                (b.longitude() - a.longitude()).pow(2.0)
        )
}
