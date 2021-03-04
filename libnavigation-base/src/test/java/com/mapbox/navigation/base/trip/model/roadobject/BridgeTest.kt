package com.mapbox.navigation.base.trip.model.roadobject

import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.trip.model.roadobject.bridge.Bridge
import com.mapbox.navigation.testing.BuilderTest
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class BridgeTest :
    BuilderTest<Bridge, Bridge.Builder>() {

    override fun getImplementationClass() = Bridge::class

    override fun getFilledUpBuilder() = Bridge.Builder(
        RoadObjectGeometry.Builder(
            456.0,
            LineString.fromLngLats(
                listOf(Point.fromLngLat(10.0, 20.0), Point.fromLngLat(33.0, 44.0))
            ),
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
        val bridge = Bridge.Builder(mockk())
            .distanceFromStartOfRoute(-1.0)
            .build()

        assertEquals(null, bridge.distanceFromStartOfRoute)
    }

    @Test
    fun `distanceFromStartOfRoute is null if null passed`() {
        val bridge = Bridge.Builder(mockk())
            .distanceFromStartOfRoute(null)
            .build()

        assertEquals(null, bridge.distanceFromStartOfRoute)
    }

    @Test
    fun `distanceFromStartOfRoute not null if positive value passed`() {
        val bridge = Bridge.Builder(mockk())
            .distanceFromStartOfRoute(1.0)
            .build()

        assertEquals(1.0, bridge.distanceFromStartOfRoute)
    }
}
