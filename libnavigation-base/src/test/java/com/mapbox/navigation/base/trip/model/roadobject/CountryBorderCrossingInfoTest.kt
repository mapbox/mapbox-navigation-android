package com.mapbox.navigation.base.trip.model.roadobject

import com.mapbox.navigation.base.trip.model.roadobject.border.CountryBorderCrossingInfo
import com.mapbox.navigation.testing.BuilderTest
import io.mockk.mockk
import org.junit.Test

class CountryBorderCrossingInfoTest :
    BuilderTest<CountryBorderCrossingInfo, CountryBorderCrossingInfo.Builder>() {

    override fun getImplementationClass() = CountryBorderCrossingInfo::class

    override fun getFilledUpBuilder() = CountryBorderCrossingInfo.Builder(
        mockk(relaxed = true),
        mockk(relaxed = true)
    )

    @Test
    override fun trigger() {
        // only used to trigger JUnit4 to run this class if all test cases come from the parent
    }
}
