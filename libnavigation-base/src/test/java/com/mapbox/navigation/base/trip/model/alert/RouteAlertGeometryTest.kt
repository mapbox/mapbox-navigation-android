package com.mapbox.navigation.base.trip.model.alert

import com.mapbox.geojson.Point
import com.mapbox.navigation.testing.BuilderTest
import org.junit.Test
import kotlin.reflect.KClass

class RouteAlertGeometryTest : BuilderTest<RouteAlertGeometry, RouteAlertGeometry.Builder>() {
    override fun getImplementationClass(): KClass<RouteAlertGeometry> = RouteAlertGeometry::class

    override fun getFilledUpBuilder() = RouteAlertGeometry.Builder(
        456.0,
        Point.fromLngLat(11.0, 22.0),
        1,
        Point.fromLngLat(33.0, 44.0),
        2
    )

    @Test
    override fun trigger() {
        // trigger, see KDoc
    }
}
