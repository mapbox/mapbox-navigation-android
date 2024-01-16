package com.mapbox.navigation.core.adasis

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.testing.BuilderTest

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class AdasisConfigProfileShortTypeOptionsTest :
    BuilderTest<
        AdasisConfigProfileShortTypeOptions, AdasisConfigProfileShortTypeOptions.Builder>() {

    override fun getImplementationClass() = AdasisConfigProfileShortTypeOptions::class

    override fun getFilledUpBuilder(): AdasisConfigProfileShortTypeOptions.Builder {
        return AdasisConfigProfileShortTypeOptions.Builder()
            .slopeStep(true)
            .curvature(false)
            .roadCondition(false)
            .variableSpeedSign(true)
            .headingChange(false)
    }

    override fun trigger() {
        // trigger, see KDoc
    }
}
