package com.mapbox.navigation.base.trip.model.alert

import com.mapbox.geojson.Point
import com.mapbox.navigation.testing.BuilderTest
import io.mockk.mockk
import org.junit.Test

class RestStopAlertTest : BuilderTest<RestStopAlert, RestStopAlert.Builder>() {
    override fun getImplementationClass() = RestStopAlert::class

    override fun getFilledUpBuilder() = RestStopAlert.Builder(
        RestStopAlert.Metadata.Builder().type(RestStopType.RestArea).build(),
        Point.fromLngLat(1.0, 2.0),
        123.0
    ).alertGeometry(mockk(relaxed = true))

    @Test
    override fun trigger() {
        // see docs
    }
}
