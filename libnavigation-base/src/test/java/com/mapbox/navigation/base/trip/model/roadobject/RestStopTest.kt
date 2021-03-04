package com.mapbox.navigation.base.trip.model.roadobject

import com.mapbox.navigation.base.trip.model.roadobject.reststop.RestStop
import com.mapbox.navigation.base.trip.model.roadobject.reststop.RestStopType
import com.mapbox.navigation.testing.BuilderTest
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class RestStopTest : BuilderTest<RestStop, RestStop.Builder>() {
    override fun getImplementationClass() = RestStop::class

    override fun getFilledUpBuilder() = RestStop.Builder((mockk(relaxed = true)))
        .distanceFromStartOfRoute(123.0)
        .restStopType(RestStopType.RestArea)

    @Test
    override fun trigger() {
        // see docs
    }

    @Test
    fun `distanceFromStartOfRoute is null if negative value passed`() {
        val restStop = RestStop.Builder(mockk())
            .distanceFromStartOfRoute(-1.0)
            .build()

        assertEquals(null, restStop.distanceFromStartOfRoute)
    }

    @Test
    fun `distanceFromStartOfRoute is null if null passed`() {
        val restStop = RestStop.Builder(mockk())
            .distanceFromStartOfRoute(null)
            .build()

        assertEquals(null, restStop.distanceFromStartOfRoute)
    }

    @Test
    fun `distanceFromStartOfRoute not null if positive value passed`() {
        val restStop = RestStop.Builder(mockk())
            .distanceFromStartOfRoute(1.0)
            .build()

        assertEquals(1.0, restStop.distanceFromStartOfRoute)
    }
}
