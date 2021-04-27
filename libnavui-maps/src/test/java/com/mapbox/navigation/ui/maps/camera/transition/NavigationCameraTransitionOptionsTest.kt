package com.mapbox.navigation.ui.maps.camera.transition

import com.mapbox.navigation.testing.BuilderTest
import org.junit.Test

class NavigationCameraTransitionOptionsTest :
    BuilderTest<NavigationCameraTransitionOptions, NavigationCameraTransitionOptions.Builder>() {
    override fun getImplementationClass() = NavigationCameraTransitionOptions::class

    override fun getFilledUpBuilder() = NavigationCameraTransitionOptions.Builder()
        .maxDuration(1234L)

    @Test
    override fun trigger() {
        // see docs
    }
}
