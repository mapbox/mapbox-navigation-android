package com.mapbox.navigation.core.adasis

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.testing.BuilderTest

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class AdasisConfigProfileLongTypeOptionsTest :
    BuilderTest<AdasisConfigProfileLongTypeOptions, AdasisConfigProfileLongTypeOptions.Builder>() {

    override fun getImplementationClass() = AdasisConfigProfileLongTypeOptions::class

    override fun getFilledUpBuilder(): AdasisConfigProfileLongTypeOptions.Builder {
        return AdasisConfigProfileLongTypeOptions.Builder()
            .lat(false)
            .lon(false)
            .trafficSign(true)
    }

    override fun trigger() {
        // trigger, see KDoc
    }
}
