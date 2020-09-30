package com.mapbox.navigation.base.trip.model.alert

import com.mapbox.geojson.Point
import com.mapbox.navigation.testing.BuilderTest
import io.mockk.mockk
import org.junit.Test

class CountryBorderCrossingAlertTest :
    BuilderTest<CountryBorderCrossingAlert, CountryBorderCrossingAlert.Builder>() {

    override fun getImplementationClass() = CountryBorderCrossingAlert::class

    override fun getFilledUpBuilder() = CountryBorderCrossingAlert.Builder(
        Point.fromLngLat(0.0, 0.0),
        1.0
    ).alertGeometry(mockk(relaxed = true)).from(mockk(relaxed = true)).to(mockk(relaxed = true))

    @Test
    override fun trigger() {
        // only used to trigger JUnit4 to run this class if all test cases come from the parent
    }
}
