package com.mapbox.navigation.core.adasis

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.testing.BuilderTest

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class ProfileShortTest : BuilderTest<ProfileShort, ProfileShort.Builder>() {

    override fun getImplementationClass() = ProfileShort::class

    override fun getFilledUpBuilder(): ProfileShort.Builder {
        return ProfileShort.Builder()
            .options(
                AdasisConfigMessageOptions.Builder().enable(false).radiusMeters(123).build()
            )
            .types(
                AdasisConfigProfileShortTypeOptions.Builder().slopeStep(true).build()
            )
    }

    override fun trigger() {
        // trigger, see KDoc
    }
}
