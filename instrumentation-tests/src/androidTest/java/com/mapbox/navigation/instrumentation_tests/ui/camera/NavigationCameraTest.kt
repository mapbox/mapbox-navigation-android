package com.mapbox.navigation.instrumentation_tests.ui.camera

import com.mapbox.navigation.instrumentation_tests.ui.SimpleMapViewNavigationTest
import com.mapbox.navigation.instrumentation_tests.utils.runOnMainSync
import org.junit.Test

class NavigationCameraTest : SimpleMapViewNavigationTest() {

    /**
     * https://github.com/mapbox/mapbox-navigation-android/issues/4453
     * https://github.com/mapbox/mapbox-navigation-android/pull/4575
     * https://cs.android.com/android/_/android/platform/frameworks/base/+/3dbaae1ef4f221b3626810f4ba19eec068dd6304
     *
     * Checks for an issue where Android Animators where running into a cancellation loop
     * on some API levels (definitely 21-24, maybe later) and caused stack overflow.
     */
    @Test
    fun navigation_camera_mode_changes_completes() {
        addNavigationCamera()

        runOnMainSync {
            navigationCamera.requestNavigationCameraToFollowing()
        }

        runOnMainSync {
            navigationCamera.requestNavigationCameraToOverview()
        }

        runOnMainSync {
            navigationCamera.requestNavigationCameraToFollowing()
        }
    }
}
