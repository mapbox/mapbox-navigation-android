package com.mapbox.navigation.core.adasis

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.testing.BuilderTest

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class AdasisProfileLongOptionsTest :
    BuilderTest<AdasisProfileLongOptions, AdasisProfileLongOptions.Builder>() {

    override fun getImplementationClass() = AdasisProfileLongOptions::class

    override fun getFilledUpBuilder(): AdasisProfileLongOptions.Builder {
        return AdasisProfileLongOptions.Builder()
            .options(
                AdasisConfigMessageOptions.Builder().enable(false).radiusMeters(123).build()
            )
            .profileOptions(
                AdasisConfigProfileLongTypeOptions.Builder().lat(false).build()
            )
    }

    override fun trigger() {
        // trigger, see KDoc
    }
}
