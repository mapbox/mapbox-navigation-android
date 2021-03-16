package com.mapbox.navigation.core.trip.model.roadobject

import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectGeometry
import com.mapbox.navigation.core.trip.model.roadobject.tunnel.Tunnel
import com.mapbox.navigation.testing.BuilderTest
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class TunnelTest : BuilderTest<Tunnel, Tunnel.Builder>() {

    override fun getImplementationClass() = Tunnel::class

    override fun getFilledUpBuilder() = Tunnel.Builder(
        RoadObjectGeometry.Builder(
            456.0,
            LineString.fromLngLats(
                listOf(
                    Point.fromLngLat(10.0, 20.0),
                    Point.fromLngLat(33.0, 44.0)
                )
            ),
            1,
            2
        ).build()
    ).distanceFromStartOfRoute(123.0)
        .info(mockk(relaxed = true))

    @Test
    override fun trigger() {
        // trigger, see KDoc
    }

    @Test
    fun `distanceFromStartOfRoute is null if negative value passed`() {
        val tunnel = Tunnel.Builder(mockk())
            .distanceFromStartOfRoute(-1.0)
            .info(mockk())
            .build()

        assertEquals(null, tunnel.distanceFromStartOfRoute)
    }

    @Test
    fun `distanceFromStartOfRoute is null if null passed`() {
        val tunnel = Tunnel.Builder(mockk())
            .distanceFromStartOfRoute(null)
            .info(mockk())
            .build()

        assertEquals(null, tunnel.distanceFromStartOfRoute)
    }

    @Test
    fun `distanceFromStartOfRoute not null if positive value passed`() {
        val tunnel = Tunnel.Builder(mockk())
            .distanceFromStartOfRoute(1.0)
            .info(mockk())
            .build()

        assertEquals(1.0, tunnel.distanceFromStartOfRoute)
    }
}
