package com.mapbox.navigation.core.adasis

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
            .stub(Stub.Builder().options(messageOptions).build())
            .segment(Segment.Builder().options(messageOptions).build())
            .profileShort(ProfileShort.Builder().options(messageOptions).build())
            .profileLong(ProfileLong.Builder().options(messageOptions).build())
    }

    override fun trigger() {
        // trigger, see KDoc
    }
}
