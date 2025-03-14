package com.mapbox.navigation.ui.maps.camera.transition

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.testing.BuilderTest

@ExperimentalPreviewMapboxNavigationAPI
internal class UpdateFrameAnimatorsOptionsTest :
    BuilderTest<UpdateFrameAnimatorsOptions, UpdateFrameAnimatorsOptions.Builder>() {

    override fun getImplementationClass() = UpdateFrameAnimatorsOptions::class

    override fun getFilledUpBuilder(): UpdateFrameAnimatorsOptions.Builder {
        return UpdateFrameAnimatorsOptions.Builder()
            .useSimplifiedAnimatorsDependency(true)
    }

    override fun trigger() {
        // trigger, see KDoc
    }
}
