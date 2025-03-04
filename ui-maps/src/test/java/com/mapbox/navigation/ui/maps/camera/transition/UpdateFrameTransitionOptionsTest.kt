package com.mapbox.navigation.ui.maps.camera.transition

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.testing.BuilderTest

@ExperimentalPreviewMapboxNavigationAPI
internal class UpdateFrameTransitionOptionsTest :
    BuilderTest<UpdateFrameTransitionOptions, UpdateFrameTransitionOptions.Builder>() {

    override fun getImplementationClass() = UpdateFrameTransitionOptions::class

    override fun getFilledUpBuilder(): UpdateFrameTransitionOptions.Builder {
        return UpdateFrameTransitionOptions.Builder()
            .nonSimultaneousAnimatorsDependency(true)
    }

    override fun trigger() {
        // trigger, see KDoc
    }
}
