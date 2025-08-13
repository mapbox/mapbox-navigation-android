package com.mapbox.navigation.ui.maps.camera.transition

import android.animation.AnimatorSet
import androidx.annotation.RestrictTo
import com.mapbox.maps.CameraOptions

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
interface NavigationCameraStateTransitionProvider {

    fun transitionToFollowing(
        cameraOptions: CameraOptions,
        transitionOptions: NavigationCameraTransitionOptions,
    ): AnimatorSet

    fun transitionToOverview(
        cameraOptions: CameraOptions,
        transitionOptions: NavigationCameraTransitionOptions,
    ): AnimatorSet
}
