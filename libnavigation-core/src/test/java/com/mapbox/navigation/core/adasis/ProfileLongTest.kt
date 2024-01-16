package com.mapbox.navigation.core.adasis

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.testing.BuilderTest

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class ProfileLongTest : BuilderTest<ProfileLong, ProfileLong.Builder>() {

    override fun getImplementationClass() = ProfileLong::class

    override fun getFilledUpBuilder(): ProfileLong.Builder {
        return ProfileLong.Builder()
            .options(
                AdasisConfigMessageOptions.Builder().enable(false).radiusMeters(123).build()
            )
            .types(
                AdasisConfigProfileLongTypeOptions.Builder().lat(false).build()
            )
    }

    override fun trigger() {
        // trigger, see KDoc
    }
}
