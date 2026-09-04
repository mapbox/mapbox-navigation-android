package com.mapbox.navigation.ui.maps.internal.camera

import com.mapbox.geojson.Point
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PointsOverviewTransitionDurationTest {

    private val origin = Point.fromLngLat(0.0, 0.0)

    // A point roughly 111 km east of the origin: far enough that the formula lands between the
    // floor and the ceiling, so the distance term is what is under test.
    private val farPoint = Point.fromLngLat(1.0, 0.0)

    @Test
    fun `nearby target is held at the floor`() {
        // ~1.1 km away, where the raw formula yields well under the floor.
        val target = Point.fromLngLat(0.01, 0.0)

        assertEquals(1500L, duration(target))
    }

    @Test
    fun `distant target is held at the ceiling`() {
        val target = Point.fromLngLat(90.0, 0.0)

        assertEquals(4000L, duration(target))
    }

    @Test
    fun `mid-range target scales with distance`() {
        val near = duration(Point.fromLngLat(0.5, 0.0))
        val far = duration(farPoint)

        assertTrue("$near should sit inside the clamp range", near in 1501L..3999L)
        assertTrue("$far ($far) should exceed $near for the longer distance", far > near)
    }

    @Test
    fun `maxDuration caps the computed duration`() {
        assertEquals(800L, duration(farPoint, maxDuration = 800L))
    }

    @Test
    fun `maxDuration above the computed duration does not extend it`() {
        val uncapped = duration(farPoint, maxDuration = Long.MAX_VALUE)

        assertEquals(uncapped, duration(farPoint, maxDuration = 4000L))
    }

    @Test
    fun `null target falls back to the ceiling`() {
        assertEquals(4000L, duration(targetCenter = null))
    }

    @Test
    fun `null target is still capped by maxDuration`() {
        assertEquals(2000L, duration(targetCenter = null, maxDuration = 2000L))
    }

    private fun duration(targetCenter: Point?, maxDuration: Long = 4000L) =
        pointsOverviewTransitionDuration(
            currentCenter = origin,
            targetCenter = targetCenter,
            maxDuration = maxDuration,
        )
}
