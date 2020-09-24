package com.mapbox.navigation.base.trip.model.alert

import com.mapbox.geojson.Point
import com.mapbox.navigation.testing.BuilderTest
import io.mockk.mockk
import org.junit.Test

class BorderCrossingAlertTest : BuilderTest<BorderCrossingAlert, BorderCrossingAlert.Builder>() {
    override fun getImplementationClass() = BorderCrossingAlert::class

    override fun getFilledUpBuilder() = BorderCrossingAlert.Builder(
        Point.fromLngLat(0.0, 0.0),
        1.0
    ).alertGeometry(mockk(relaxed = true)).from(mockk(relaxed = true)).to(mockk(relaxed = true))

    @Test
    override fun trigger() {
        // only used to trigger JUnit4 to run this class if all test cases come from the parent
    }
}
