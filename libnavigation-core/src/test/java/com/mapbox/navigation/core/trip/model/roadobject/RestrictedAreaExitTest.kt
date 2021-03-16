package com.mapbox.navigation.core.trip.model.roadobject

import com.mapbox.geojson.Point
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectGeometry
import com.mapbox.navigation.core.trip.model.roadobject.restrictedarea.RestrictedAreaExit
import com.mapbox.navigation.testing.BuilderTest
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class RestrictedAreaExitTest : BuilderTest<RestrictedAreaExit, RestrictedAreaExit.Builder>() {

    override fun getImplementationClass() = RestrictedAreaExit::class

    override fun getFilledUpBuilder() = RestrictedAreaExit.Builder(
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
        val areaExit = RestrictedAreaExit.Builder(mockk())
            .distanceFromStartOfRoute(-1.0)
            .build()

        assertEquals(null, areaExit.distanceFromStartOfRoute)
    }

    @Test
    fun `distanceFromStartOfRoute is null if null passed`() {
        val areaExit = RestrictedAreaExit.Builder(mockk())
            .distanceFromStartOfRoute(null)
            .build()

        assertEquals(null, areaExit.distanceFromStartOfRoute)
    }

    @Test
    fun `distanceFromStartOfRoute not null if positive value passed`() {
        val areaExit = RestrictedAreaExit.Builder(mockk())
            .distanceFromStartOfRoute(1.0)
            .build()

        assertEquals(1.0, areaExit.distanceFromStartOfRoute)
    }
}
