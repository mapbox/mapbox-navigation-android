package com.mapbox.navigation.base.trip.model.roadobject

import com.mapbox.geojson.Point
import com.mapbox.navigation.base.trip.model.roadobject.bridge.BridgeExit
import com.mapbox.navigation.testing.BuilderTest
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class BridgeExitTest : BuilderTest<BridgeExit, BridgeExit.Builder>() {

    override fun getImplementationClass() = BridgeExit::class

    override fun getFilledUpBuilder() = BridgeExit.Builder(
        RoadObjectGeometry.Builder(
            456.0,
            Point.fromLngLat(10.0, 20.0),
            1,
            2
        ).build()
    ).distanceFromStartOfRoute(123.0)

    @Test
    override fun trigger() {
        // trigger, see KDoc
    }

    @Test
    fun `distanceFromStartOfRoute is null if negative value passed`() {
        val bridgeExit = BridgeExit.Builder(mockk())
            .distanceFromStartOfRoute(-1.0)
            .build()

        assertEquals(null, bridgeExit.distanceFromStartOfRoute)
    }

    @Test
    fun `distanceFromStartOfRoute is null if null passed`() {
        val bridgeExit = BridgeExit.Builder(mockk())
            .distanceFromStartOfRoute(null)
            .build()

        assertEquals(null, bridgeExit.distanceFromStartOfRoute)
    }

    @Test
    fun `distanceFromStartOfRoute not null if positive value passed`() {
        val bridgeExit = BridgeExit.Builder(mockk())
            .distanceFromStartOfRoute(1.0)
            .build()

        assertEquals(1.0, bridgeExit.distanceFromStartOfRoute)
    }
}
