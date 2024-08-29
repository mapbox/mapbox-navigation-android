package com.mapbox.navigation.core.adas

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.testing.BuilderTest

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class AdasisConfigPathOptionsTest :
    BuilderTest<AdasisConfigPathOptions, AdasisConfigPathOptions.Builder>() {

    override fun getImplementationClass() = AdasisConfigPathOptions::class

    override fun getFilledUpBuilder(): AdasisConfigPathOptions.Builder {
        val messageOptions = AdasisConfigMessageOptions.Builder()
            .enable(false)
            .radiusMeters(12345)
            .build()

        return AdasisConfigPathOptions.Builder()
            .stubOptions(
                AdasisStubOptions.Builder().options(messageOptions).build(),
            )
            .segmentOptions(
                AdasisSegmentOptions.Builder().options(messageOptions).build(),
            )
            .profileShortOptions(
                AdasisProfileShortOptions.Builder().options(messageOptions).build(),
            )
            .profileLongOptions(
                AdasisProfileLongOptions.Builder().options(messageOptions).build(),
            )
    }

    override fun trigger() {
        // trigger, see KDoc
    }
}
