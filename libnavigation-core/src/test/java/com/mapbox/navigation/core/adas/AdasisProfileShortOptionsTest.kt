package com.mapbox.navigation.core.adas

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.testing.BuilderTest

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class AdasisProfileShortOptionsTest :
    BuilderTest<AdasisProfileShortOptions, AdasisProfileShortOptions.Builder>() {

    override fun getImplementationClass() = AdasisProfileShortOptions::class

    override fun getFilledUpBuilder(): AdasisProfileShortOptions.Builder {
        return AdasisProfileShortOptions.Builder()
            .options(
                AdasisConfigMessageOptions.Builder().enable(false).radiusMeters(123).build(),
            )
            .profileOptions(
                AdasisConfigProfileShortTypeOptions.Builder().slopeStep(true).build(),
            )
    }

    override fun trigger() {
        // trigger, see KDoc
    }
}
