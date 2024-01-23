package com.mapbox.navigation.core.adas

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.testing.BuilderTest

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class AdasisStubOptionsTest : BuilderTest<AdasisStubOptions, AdasisStubOptions.Builder>() {

    override fun getImplementationClass() = AdasisStubOptions::class

    override fun getFilledUpBuilder(): AdasisStubOptions.Builder {
        return AdasisStubOptions.Builder()
            .options(AdasisConfigMessageOptions.Builder().radiusMeters(12345).build())
    }

    override fun trigger() {
        // trigger, see KDoc
    }
}
