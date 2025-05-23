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

    /**
     * [android.animation.ValueAnimator] allocates equal time for each value transition, consider
     * default animation timeline:
     *   |----------------|----------------|----------------|          // animation progress from 0.0 to 1.0
     * 0.0, 0.1        0.5, 0.5          0.5, 0.5          1.0, 1.0   // location value
     * 1/3 of the animation time, location will be 0.5 in that case.
     * Desired timeline for constant velocity in that case is:
     *   |------------------------|-------------------------|
     * 0.0, 0.1                0.5, 0.5                 1.0, 1.0
     * I.e. when animation completed on 25%, location should be (0.25, 0.25) as on the
     * timeline bellow instead of (0.5, 0.5) as on the timeline above.
     *
     * To achieve desired animation timeline, we need to interpolate(remap) animation progress from:
     * 0.25 to 1/(3*2)
     * 0.499 (i.e. ~ 1/2) to 0.32 (i.e. ~1/3)
     * etc
     * so that it pointed to the right location on the top timeline.
     */
    @Test
    fun `0 distance key points interpolated to happen immediate`() {
        val p0 = Point.fromLngLat(0.0, 0.0)
        val keyPoints = arrayOf(
            Point.fromLngLat(0.5, 0.5),
            Point.fromLngLat(0.5, 0.5),
            Point.fromLngLat(1.0, 1.0),
        )
        val sut = ConstantVelocityInterpolator(p0, keyPoints)

        assertEquals(0.166f, sut.getInterpolation(0.25f), 0.001f)
        assertEquals(0.329f, sut.getInterpolation(0.499f), 0.01f)
        assertEquals(0.667f, sut.getInterpolation(0.501f), 0.01f)
        assertEquals(0.833f, sut.getInterpolation(0.75f), 0.01f)
    }

    @Test
    fun `should use linear interpolation for key points at zero distance`() {
        val p0 = Point.fromLngLat(1.0, 1.0)

        assertEquals(
            0.7f,
            ConstantVelocityInterpolator(p0, emptyArray()).getInterpolation(0.7f),
        )
        assertEquals(
            0.3f,
            ConstantVelocityInterpolator(p0, arrayOf(p0)).getInterpolation(0.3f),
        )
    }

    @Test
    fun `should use linear interpolation for single key point`() {
        val p0 = Point.fromLngLat(1.0, 1.0)
        val p1 = Point.fromLngLat(2.0, 2.0)

        assertEquals(
            0.3f,
            ConstantVelocityInterpolator(p0, arrayOf(p1)).getInterpolation(0.3f),
        )
    }

    @Test
    fun `should not crash because of error accumulation`() { // caused by NAVAND-925
        val startPoint = Point.fromLngLat(48.36654772857354, 11.13222120183356)
        val keyPoints = arrayOf(
            Point.fromLngLat(48.416596, 11.027287),
            Point.fromLngLat(48.416520999999996, 11.027080999999999),
            Point.fromLngLat(48.416281, 11.02683),
            Point.fromLngLat(48.416156, 11.02656),
            Point.fromLngLat(48.416146999999995, 11.026156),
            Point.fromLngLat(48.416205999999995, 11.02605),
            Point.fromLngLat(48.416253, 11.025912),
            Point.fromLngLat(48.416194, 11.025749),
            Point.fromLngLat(48.416028, 11.025495999999999),
            Point.fromLngLat(48.415786, 11.025307),
            Point.fromLngLat(48.415642999999996, 11.025171),
            Point.fromLngLat(48.4156, 11.025034),
            Point.fromLngLat(48.415535999999996, 11.024955),
            Point.fromLngLat(48.415535999999996, 11.024955000000004),
        )
        // no crash
        ConstantVelocityInterpolator(startPoint, keyPoints)
    }

    private fun calculatePathValues(p0: Point, vararg points: Point): List<Pair<Float, Float>> {
        val distances = mutableListOf<Double>()
        var totalDistance = 0.0
        points.fold(p0) { a, b ->
            val d = distance(a, b)
            distances.add(d)
            totalDistance += d
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
                (b.longitude() - a.longitude()).pow(2.0),
        )
}
