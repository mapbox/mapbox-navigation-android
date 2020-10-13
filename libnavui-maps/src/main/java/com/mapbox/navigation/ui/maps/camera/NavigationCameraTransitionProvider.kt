package com.mapbox.navigation.ui.maps.camera

import android.animation.AnimatorSet

interface NavigationCameraTransitionProvider {
    fun transitionFromLowZoomToHighZoom(
        transitionOptions: NavigationCameraZoomTransitionOptions): AnimatorSet

    fun transitionFromHighZoomToLowZoom(
        transitionOptions: NavigationCameraZoomTransitionOptions): AnimatorSet

    fun transitionLinear(
        transitionOptions: NavigationCameraLinearTransitionOptions): AnimatorSet
}
