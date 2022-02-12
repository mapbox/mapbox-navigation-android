package com.mapbox.navigation.dropin.statebinder

import android.transition.Scene
import android.transition.TransitionManager
import android.view.ViewGroup
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.dropin.DropInNavigationViewContext
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.dropin.component.camera.DropInCameraMode
import com.mapbox.navigation.dropin.component.camera.DropInNavigationCamera
import com.mapbox.navigation.dropin.component.location.DropInLocationPuck
import com.mapbox.navigation.dropin.component.location.DropInLocationState
import com.mapbox.navigation.dropin.component.replay.DropInReplayButton
import com.mapbox.navigation.dropin.component.routeline.DropInRouteLine
import com.mapbox.navigation.dropin.component.speedlimit.DropInSpeedLimitComponent
import com.mapbox.navigation.dropin.databinding.DropInStateFreeDriveBinding
import com.mapbox.navigation.dropin.lifecycle.DropInViewBinder
import com.mapbox.navigation.dropin.lifecycle.navigationListOf

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class FreeDriveViewBinder(
    private val navigationViewContext: DropInNavigationViewContext
) : DropInViewBinder {

    val cameraState = navigationViewContext.viewModel.cameraState

    override fun bind(viewGroup: ViewGroup): MapboxNavigationObserver {
        val scene = Scene.getSceneForLayout(
            viewGroup,
            R.layout.drop_in_state_free_drive,
            viewGroup.context
        )
        TransitionManager.go(scene)
        val binding = DropInStateFreeDriveBinding.bind(viewGroup)

        cameraState.cameraMode.value = DropInCameraMode.OVERVIEW

        val locationState = DropInLocationState()
        return navigationListOf(
            locationState,
            DropInRouteLine(navigationViewContext, locationState),
            DropInNavigationCamera(
                locationState,
                navigationViewContext.viewModel.cameraState,
                navigationViewContext.mapView
            ),
            DropInSpeedLimitComponent(binding.speedLimitView),
            DropInLocationPuck(locationState, navigationViewContext.mapView),
            DropInReplayButton(navigationViewContext, binding.startNavigation)
        )
    }
}
