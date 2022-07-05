package com.mapbox.navigation.core.trip.roadobject.reststop

import com.mapbox.geojson.Point
import com.mapbox.navigation.testing.BuilderTest
import io.mockk.mockk
import org.junit.Test
import kotlin.reflect.KClass

class RestStopFromIntersectionTest :
    BuilderTest<RestStopFromIntersection, RestStopFromIntersection.Builder>() {
    override fun getImplementationClass(): KClass<RestStopFromIntersection> {
        return RestStopFromIntersection::class
    }

    override fun getFilledUpBuilder(): RestStopFromIntersection.Builder {
        return RestStopFromIntersection
            .Builder(Point.fromLngLat(111.0, 222.0))
            .name("ServiceArea")
            .type("ServiceAreaType")
            .amenities(listOf(mockk()))
    }

    @Test
    override fun trigger() {
        // see comments
    }
}
