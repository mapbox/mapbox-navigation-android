package com.mapbox.navigation.base.trip.model.alert

import com.mapbox.geojson.Point
import com.mapbox.navigation.testing.BuilderTest
import io.mockk.mockk
import org.junit.Test

class TollCollectionAlertTest : BuilderTest<TollCollectionAlert, TollCollectionAlert.Builder>() {
    override fun getImplementationClass() = TollCollectionAlert::class

    override fun getFilledUpBuilder() = TollCollectionAlert.Builder(
        Point.fromLngLat(1.0, 2.0),
        123.0
    ).alertGeometry(mockk(relaxed = true)).tollCollectionType(TollCollectionType.TollBooth)

    @Test
    override fun trigger() {
        // see docs
    }
}
