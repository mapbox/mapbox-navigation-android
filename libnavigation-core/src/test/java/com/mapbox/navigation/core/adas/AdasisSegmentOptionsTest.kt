package com.mapbox.navigation.core.adas

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.testing.BuilderTest

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class AdasisSegmentOptionsTest :
    BuilderTest<AdasisSegmentOptions, AdasisSegmentOptions.Builder>() {

    override fun getImplementationClass() = AdasisSegmentOptions::class

    override fun getFilledUpBuilder(): AdasisSegmentOptions.Builder {
        return AdasisSegmentOptions.Builder()
            .options(AdasisConfigMessageOptions.Builder().radiusMeters(12345).build())
    }

    override fun trigger() {
        // trigger, see KDoc
    }
}
