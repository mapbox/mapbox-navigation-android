package com.mapbox.navigation.core.adas

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.testing.BuilderTest

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class AdasisConfigProfileShortTypeOptionsTest :
    BuilderTest<
        AdasisConfigProfileShortTypeOptions, AdasisConfigProfileShortTypeOptions.Builder,>() {

    override fun getImplementationClass() = AdasisConfigProfileShortTypeOptions::class

    override fun getFilledUpBuilder(): AdasisConfigProfileShortTypeOptions.Builder {
        return AdasisConfigProfileShortTypeOptions.Builder()
            .slopeStep(true)
            .curvature(false)
            .roadCondition(false)
            .variableSpeedSign(true)
            .headingChange(false)
            .historyAverageSpeed(false)
    }

    override fun trigger() {
        // trigger, see KDoc
    }
}
