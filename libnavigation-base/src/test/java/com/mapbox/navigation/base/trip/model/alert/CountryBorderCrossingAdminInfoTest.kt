package com.mapbox.navigation.base.trip.model.alert

import com.mapbox.navigation.testing.BuilderTest
import org.junit.Test

class CountryBorderCrossingAdminInfoTest :
    BuilderTest<CountryBorderCrossingAdminInfo, CountryBorderCrossingAdminInfo.Builder>() {

    override fun getImplementationClass() = CountryBorderCrossingAdminInfo::class

    override fun getFilledUpBuilder() = CountryBorderCrossingAdminInfo.Builder(
        "US",
        "USA"
    )

    @Test
    override fun trigger() {
        // see docs
    }
}
