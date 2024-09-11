package com.mapbox.navigation.core.adas

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.testing.BuilderTest

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class AdasisConfigMessageOptionsTest :
    BuilderTest<AdasisConfigMessageOptions, AdasisConfigMessageOptions.Builder>() {

    override fun getImplementationClass() = AdasisConfigMessageOptions::class

    override fun getFilledUpBuilder(): AdasisConfigMessageOptions.Builder {
        return AdasisConfigMessageOptions.Builder()
            .enable(false)
            .radiusMeters(12345)
    }

    override fun trigger() {
        // trigger, see KDoc
    }
}
