package com.mapbox.navigation.base.options

import com.mapbox.navigation.testing.BuilderTest
import org.junit.Test

class PredictiveCacheLocationOptionsTest :
    BuilderTest<PredictiveCacheLocationOptions, PredictiveCacheLocationOptions.Builder>() {

    override fun getImplementationClass() = PredictiveCacheLocationOptions::class

    override fun getFilledUpBuilder() = PredictiveCacheLocationOptions.Builder()
        .currentLocationRadiusInMeters(25)
        .routeBufferRadiusInMeters(15)
        .destinationLocationRadiusInMeters(55)

    @Test
    override fun trigger() {
        // trigger, see KDoc
    }
}
