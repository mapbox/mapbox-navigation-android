package com.mapbox.navigation.base.trip.model.roadobject

import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.navigation.testing.BuilderTest
import org.junit.Test
import kotlin.reflect.KClass

class RoadObjectGeometryTest : BuilderTest<RoadObjectGeometry, RoadObjectGeometry.Builder>() {
    override fun getImplementationClass(): KClass<RoadObjectGeometry> = RoadObjectGeometry::class

    override fun getFilledUpBuilder() = RoadObjectGeometry.Builder(
        456.0,
        LineString.fromLngLats(
            listOf(Point.fromLngLat(10.0, 20.0), Point.fromLngLat(33.0, 44.0))
        ),
        1,
        2
    )

    @Test
    override fun trigger() {
        // trigger, see KDoc
    }
}
