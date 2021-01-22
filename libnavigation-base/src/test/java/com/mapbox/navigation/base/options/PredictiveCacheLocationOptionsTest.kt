package com.mapbox.navigation.base.options

import com.mapbox.navigation.testing.BuilderTest
import org.junit.Test

class PredictiveCacheLocationOptionsTest :
    BuilderTest<PredictiveCacheLocationOptions, PredictiveCacheLocationOptions.Builder>() {

    override fun getImplementationClass() = PredictiveCacheLocationOptions::class

    override fun getFilledUpBuilder() = PredictiveCacheLocationOptions.Builder()
        .currentLocationRadiusInMeters(25000)
        .routeBufferRadiusInMeters(15000)
        .destinationLocationRadiusInMeters(55000)

    @Test
    override fun trigger() {
        // trigger, see KDoc
    }
}
