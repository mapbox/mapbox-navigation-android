package com.mapbox.navigation.core.replay.history

import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
import org.junit.Test

class RouteSmootherTest {

    val routeSmoother = RouteSmoother()

    @Test
    fun `should print geometry`() {
        val geometry = """inq_gAxdhmhFjZkA^?tDMUsF?m@WmKMoHOeF]eO?}@GiBcB}s@?{@McGoDLu@?cUlAuKj@"""
        val points = LineString.fromPolyline(geometry, 6)
        points.coordinates().forEach {
            println("Point.fromLngLat(${it.longitude()}, ${it.latitude()}),")
        }

        assertTrue(false)
    }

    @Test
    fun `should turn straight road into a segment`() {
        // Geometry used qnq_gAxdhmhFuvBlJe@?qC^
        val coordinates = listOf<Point>(
            Point.fromLngLat(-122.393181, 37.758201),
            Point.fromLngLat(-122.393364, 37.760116),
            Point.fromLngLat(-122.393364, 37.760135),
            Point.fromLngLat(-122.39338, 37.760208)
        )

        val smoothedRoutes = routeSmoother.smoothRoute(coordinates, 10.0)

        assertEquals(2, smoothedRoutes.size)
        assertEquals(smoothedRoutes[0].longitude(), -122.393181)
        assertEquals(smoothedRoutes[0].latitude(), 37.758201)
        assertEquals(smoothedRoutes[1].longitude(), -122.39338)
        assertEquals(smoothedRoutes[1].latitude(), 37.760208)
    }

    @Test
    fun `should turn a turn into three points`() {
        // Geometry used inq_gAxdhmhFjZkA^?tDMUsF?m@WmKMoHOeF]eO?}@GiBcB}s@?{@McGoDLu@?cUlAuKj@
        val coordinates = listOf<Point>(
            Point.fromLngLat(-122.393181, 37.758197),
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
            Point.fromLngLat(-122.391243, 37.758438)
        )

        val smoothedRoutes = routeSmoother.smoothRoute(coordinates, 5.0)

        smoothedRoutes.forEach {
            println("smoothed(${it.latitude()}, ${it.longitude()})")
        }


        assertEquals(4, smoothedRoutes.size)
        assertEquals(smoothedRoutes[0].longitude(), -122.393181)
        assertEquals(smoothedRoutes[0].latitude(), 37.758197)
        assertEquals(smoothedRoutes[1].longitude(), -122.393136)
        assertEquals(smoothedRoutes[1].latitude(), 37.757652)
    }
}