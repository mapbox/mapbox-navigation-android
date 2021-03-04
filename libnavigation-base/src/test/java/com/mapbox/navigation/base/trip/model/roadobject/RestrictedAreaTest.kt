package com.mapbox.navigation.base.trip.model.roadobject

import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.trip.model.roadobject.restrictedarea.RestrictedArea
import com.mapbox.navigation.testing.BuilderTest
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class RestrictedAreaTest : BuilderTest<RestrictedArea, RestrictedArea.Builder>() {

    override fun getImplementationClass() = RestrictedArea::class

    override fun getFilledUpBuilder() = RestrictedArea.Builder(
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
        val area = RestrictedArea.Builder(mockk())
            .distanceFromStartOfRoute(-1.0)
            .build()

        assertEquals(null, area.distanceFromStartOfRoute)
    }

    @Test
    fun `distanceFromStartOfRoute is null if null passed`() {
        val area = RestrictedArea.Builder(mockk())
            .distanceFromStartOfRoute(null)
            .build()

        assertEquals(null, area.distanceFromStartOfRoute)
    }

    @Test
    fun `distanceFromStartOfRoute not null if positive value passed`() {
        val area = RestrictedArea.Builder(mockk())
            .distanceFromStartOfRoute(1.0)
            .build()

        assertEquals(1.0, area.distanceFromStartOfRoute)
    }
}
