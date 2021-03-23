package com.mapbox.navigation.core.trip.model.roadobject

import com.mapbox.geojson.Point
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectGeometry
import com.mapbox.navigation.core.trip.model.roadobject.tunnel.TunnelEntrance
import com.mapbox.navigation.testing.BuilderTest
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class TunnelEntranceTest : BuilderTest<TunnelEntrance, TunnelEntrance.Builder>() {

    override fun getImplementationClass() = TunnelEntrance::class

    override fun getFilledUpBuilder() = TunnelEntrance.Builder(
        RoadObjectGeometry.Builder(
            456.0,
            Point.fromLngLat(10.0, 20.0),
            1,
            2
        ).build(),
        mockk(relaxed = true)
    ).distanceFromStartOfRoute(123.0)

    @Test
    override fun trigger() {
        // trigger, see KDoc
    }

    @Test
    fun `distanceFromStartOfRoute is null if negative value passed`() {
        val tunnelEntrance = TunnelEntrance.Builder(mockk(), mockk())
            .distanceFromStartOfRoute(-1.0)
            .build()

        assertEquals(null, tunnelEntrance.distanceFromStartOfRoute)
    }

    @Test
    fun `distanceFromStartOfRoute is null if null passed`() {
        val tunnelEntrance = TunnelEntrance.Builder(mockk(), mockk())
            .distanceFromStartOfRoute(null)
            .build()

        assertEquals(null, tunnelEntrance.distanceFromStartOfRoute)
    }

    @Test
    fun `distanceFromStartOfRoute not null if positive value passed`() {
        val tunnelEntrance = TunnelEntrance.Builder(mockk(), mockk())
            .distanceFromStartOfRoute(1.0)
            .build()

        assertEquals(1.0, tunnelEntrance.distanceFromStartOfRoute)
    }
}
