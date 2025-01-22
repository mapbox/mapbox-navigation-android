package com.mapbox.navigation.ui.maps.camera.lifecycle

import com.mapbox.navigation.testing.BuilderTest
import com.mapbox.navigation.ui.maps.R
import io.mockk.every
import io.mockk.mockk
import org.junit.Test

class NavigationScaleGestureHandlerOptionsTest :
    BuilderTest<NavigationScaleGestureHandlerOptions,
        NavigationScaleGestureHandlerOptions.Builder,>() {
    override fun getImplementationClass() = NavigationScaleGestureHandlerOptions::class

    override fun getFilledUpBuilder() = NavigationScaleGestureHandlerOptions.Builder(
        mockk {
            every { resources } returns mockk {
                every {
                    getDimension(R.dimen.mapbox_navigationCamera_trackingInitialMoveThreshold)
                } returns 10f
                every {
                    getDimension(R.dimen.mapbox_navigationCamera_trackingMultiFingerMoveThreshold)
                } returns 20f
            }
        },
    )
        .followingInitialMoveThreshold(123f)
        .followingMultiFingerMoveThreshold(456f)
        .followingMultiFingerProtectedMoveArea(mockk())
        .followingRotationAngleThreshold(120f)

    @Test
    override fun trigger() {
        // see comments
    }
}
