package com.mapbox.navigation.ui.maps.camera

import android.animation.AnimatorSet

interface NavigationStateTransitionProvider {
    fun transitionToVehicleFollowing(
        transitionOptions: NavigationStateTransitionOptionsToFollowing
    ): AnimatorSet

    fun transitionToRouteOverview(
        transitionOptions: NavigationStateTransitionOptionsToRouteOverview
    ): AnimatorSet

    fun updateMapFrameForFollowing(transitionOptions: NavigationStateTransitionOptionsToFollowing
    ): AnimatorSet

    fun updateMapFrameForOverview(transitionOptions: NavigationStateTransitionOptionsToRouteOverview
    ): AnimatorSet
}
