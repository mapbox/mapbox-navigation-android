package com.mapbox.navigation.ui.maps.internal.camera

import android.animation.ValueAnimator
import androidx.annotation.RestrictTo
import com.mapbox.maps.CameraOptions
import com.mapbox.navigation.ui.maps.camera.transition.NavigationCameraTransitionOptions

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
interface SimplifiedUpdateFrameTransitionProvider {

    fun updateFollowingFrame(
        cameraOptions: CameraOptions,
        transitionOptions: NavigationCameraTransitionOptions,
    ): List<ValueAnimator>

    fun updateOverviewFrame(
        cameraOptions: CameraOptions,
        transitionOptions: NavigationCameraTransitionOptions,
    ): List<ValueAnimator>
}
