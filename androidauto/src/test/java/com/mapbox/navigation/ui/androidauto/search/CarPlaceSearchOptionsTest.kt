package com.mapbox.navigation.ui.androidauto.search

import com.mapbox.navigation.testing.BuilderTest
import org.junit.Test

class CarPlaceSearchOptionsTest :
    BuilderTest<CarPlaceSearchOptions, CarPlaceSearchOptions.Builder>() {

    override fun getImplementationClass() = CarPlaceSearchOptions::class

    override fun getFilledUpBuilder(): CarPlaceSearchOptions.Builder {
        return CarPlaceSearchOptions.Builder()
            .accessToken("pk.test124")
    }

    @Test
    override fun trigger() {
        // only used to trigger JUnit4 to run this class if all test cases come from the parent
    }
}
