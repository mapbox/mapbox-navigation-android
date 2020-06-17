package com.mapbox.navigation.core.replay.route

import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReplayRouteSmootherTest {

    private val recommendedThresholdMeters = 3.0
    private val cmAccuracy = 0.0000001

    private val routeSmoother = ReplayRouteSmoother()

    @Test
    fun `should turn straight road into a segment`() {
        val coordinates = listOf<Point>(
            Point.fromLngLat(-122.393181, 37.758201),
            Point.fromLngLat(-122.393364, 37.760116),
            Point.fromLngLat(-122.393364, 37.760135),
            Point.fromLngLat(-122.39338, 37.760208)
        )

        val smoothedRoutes = routeSmoother.smoothRoute(coordinates, recommendedThresholdMeters)

        assertEquals(2, smoothedRoutes.size)
        assertEquals(-122.393181, smoothedRoutes[0].point.longitude(), cmAccuracy)
        assertEquals(37.758201, smoothedRoutes[0].point.latitude(), cmAccuracy)
        assertEquals(-122.39338, smoothedRoutes[1].point.longitude(), cmAccuracy)
        assertEquals(37.760208, smoothedRoutes[1].point.latitude(), cmAccuracy)
    }

    @Test
    fun `should turn a turn into three points`() {
        val coordinates = listOf<Point>(
            Point.fromLngLat(-122.393181, 37.758193),
            Point.fromLngLat(-122.393143, 37.757759),
            Point.fromLngLat(-122.393143, 37.757743),
            Point.fromLngLat(-122.393136, 37.757652),
            Point.fromLngLat(-122.393014, 37.757663),
            Point.fromLngLat(-122.392991, 37.757663),
            Point.fromLngLat(-122.392792, 37.757675),
            Point.fromLngLat(-122.39264, 37.757682),
            Point.fromLngLat(-122.392525, 37.75769),
            Point.fromLngLat(-122.392266, 37.757705),
            Point.fromLngLat(-122.392235, 37.757705),
            Point.fromLngLat(-122.392182, 37.757709),
            Point.fromLngLat(-122.391335, 37.757759),
            Point.fromLngLat(-122.391305, 37.757759),
            Point.fromLngLat(-122.391175, 37.757766),
            Point.fromLngLat(-122.391182, 37.757854),
            Point.fromLngLat(-122.391182, 37.757881),
            Point.fromLngLat(-122.391221, 37.758235),
            Point.fromLngLat(-122.391243, 37.758468),
            Point.fromLngLat(-122.39135, 37.759613)
        )

        val smoothedRoutes = routeSmoother.smoothRoute(coordinates, recommendedThresholdMeters)

        assertEquals(4, smoothedRoutes.size)
        assertEquals(-122.393181, smoothedRoutes[0].point.longitude(), cmAccuracy)
        assertEquals(37.758193, smoothedRoutes[0].point.latitude(), cmAccuracy)
        assertEquals(-122.393136, smoothedRoutes[1].point.longitude(), cmAccuracy)
        assertEquals(37.757652, smoothedRoutes[1].point.latitude(), cmAccuracy)
        assertEquals(-122.391175, smoothedRoutes[2].point.longitude(), cmAccuracy)
        assertEquals(37.757766, smoothedRoutes[2].point.latitude(), cmAccuracy)
        assertEquals(-122.39135, smoothedRoutes[3].point.longitude(), cmAccuracy)
        assertEquals(37.759613, smoothedRoutes[3].point.latitude(), cmAccuracy)
    }

    @Test
    fun `should keep u-turns in the route`() {
        val coordinates = listOf<Point>(
            Point.fromLngLat(-122.393181, 37.758193),
            Point.fromLngLat(-122.393364, 37.760116),
            Point.fromLngLat(-122.393364, 37.760135),
            Point.fromLngLat(-122.39338, 37.760208),
            Point.fromLngLat(-122.393265, 37.760219),
            Point.fromLngLat(-122.393227, 37.760223),
            Point.fromLngLat(-122.392861, 37.760242),
            Point.fromLngLat(-122.390091, 37.760387),
            Point.fromLngLat(-122.389649, 37.760406),
            Point.fromLngLat(-122.389618, 37.760406),
            Point.fromLngLat(-122.389504, 37.760437),
            Point.fromLngLat(-122.389603, 37.760463),
            Point.fromLngLat(-122.389618, 37.760463),
            Point.fromLngLat(-122.390358, 37.760429),
            Point.fromLngLat(-122.390381, 37.760425),
            Point.fromLngLat(-122.390458, 37.760425),
            Point.fromLngLat(-122.390465, 37.760498),
            Point.fromLngLat(-122.390511, 37.760978)
        )

        val smoothedRoutes = routeSmoother.smoothRoute(coordinates, recommendedThresholdMeters)

        assertEquals(5, smoothedRoutes.size)
    }

    @Test
    fun `should keep u-turns in the route 2`() {
        val coordinates = listOf<Point>(
            Point.fromLngLat(-121.469903, 38.550876),
            Point.fromLngLat(-121.470231, 38.550964),
            Point.fromLngLat(-121.470002, 38.551483),
            Point.fromLngLat(-121.469887, 38.551753),
            Point.fromLngLat(-121.470002, 38.551483),
            Point.fromLngLat(-121.470231, 38.550964),
            Point.fromLngLat(-121.470978, 38.551158)
        )

        val smoothedRoutes = routeSmoother.smoothRoute(coordinates, recommendedThresholdMeters)

        assertEquals(5, smoothedRoutes.size)
    }

    @Test
    fun `should detect side of road`() {
        val segmentStart = Point.fromLngLat(-122.393408, 37.760368)
        val segmentEnd = Point.fromLngLat(-122.393515, 37.761443)
        val rightSide = Point.fromLngLat(-122.393320, 37.760822)
        val leftSide = Point.fromLngLat(-122.393570, 37.760828)

        val rightSideDistance = routeSmoother.distanceToSegment(segmentStart, rightSide, segmentEnd)
        val leftSideDistance = routeSmoother.distanceToSegment(segmentStart, leftSide, segmentEnd)

        assertTrue("$rightSideDistance < 0", rightSideDistance!! < 0)
        assertTrue("$leftSideDistance > 0", leftSideDistance!! > 0)
    }

    @Test
    fun `should keep points for wide freeway turns`() {
        val routeSmoother = ReplayRouteSmoother()
        val geometry =
            """u{l~fA|kenhF`]vChRl@hRL`Wk@fN_@~HMnILfINvI\dJjAdJzAvIxBnIhC~HvCfIvDfIrEpHrF~HbG~H`H~I~IrJhLvIfNd@|@nDrF~CrFfDpG~CrGpC`GxGtPnCpGhCpGnCrGdj@xjAxLdYhCrGvC`HhCpGfC`HhC`HhC`HxB`H`C`HxBnHpB`HhB`IbB`HzK|gAt@tOd@rP?vN?bp@WpRsAzi@?hCMxa@"""
        val coordinates = LineString.fromPolyline(geometry, 6).coordinates()

        val smoothedRoutes = routeSmoother.smoothRoute(coordinates, 3.0)

        // Note that this should be 15. Creating a lower bound as we try to improve
        // the solution for tracking wide turns
        assertTrue("${smoothedRoutes.size} == 20", smoothedRoutes.size >= 9)
    }

    @Test
    fun `should segment route and include segment points`() {
        val coordinates = listOf(
            Point.fromLngLat(-121.46991, 38.550876),
            Point.fromLngLat(-121.470231, 38.550964),
            Point.fromLngLat(-121.470002, 38.551483),
            Point.fromLngLat(-121.469918, 38.551677)
        )

        val segment = routeSmoother.segmentRoute(coordinates, startIndex = 0, endIndex = 1)

        assertEquals(2, segment.size)
    }
}
