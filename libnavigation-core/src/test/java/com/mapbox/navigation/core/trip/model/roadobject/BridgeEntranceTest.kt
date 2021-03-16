package com.mapbox.navigation.core.trip.model.roadobject

import com.mapbox.geojson.Point
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectGeometry
import com.mapbox.navigation.core.trip.model.roadobject.bridge.BridgeEntrance
import com.mapbox.navigation.testing.BuilderTest
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class BridgeEntranceTest : BuilderTest<BridgeEntrance, BridgeEntrance.Builder>() {

    override fun getImplementationClass() = BridgeEntrance::class

    override fun getFilledUpBuilder() = BridgeEntrance.Builder(
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
        val bridgeEntrance = BridgeEntrance.Builder(mockk())
            .distanceFromStartOfRoute(-1.0)
            .build()

        assertEquals(null, bridgeEntrance.distanceFromStartOfRoute)
    }

    @Test
    fun `distanceFromStartOfRoute is null if null passed`() {
        val bridgeEntrance = BridgeEntrance.Builder(mockk())
            .distanceFromStartOfRoute(null)
            .build()

        assertEquals(null, bridgeEntrance.distanceFromStartOfRoute)
    }

    @Test
    fun `distanceFromStartOfRoute not null if positive value passed`() {
        val bridgeEntrance = BridgeEntrance.Builder(mockk())
            .distanceFromStartOfRoute(1.0)
            .build()

        assertEquals(1.0, bridgeEntrance.distanceFromStartOfRoute)
    }
}
