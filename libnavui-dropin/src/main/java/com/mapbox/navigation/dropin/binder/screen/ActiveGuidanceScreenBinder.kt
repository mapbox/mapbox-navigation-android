package com.mapbox.navigation.dropin.binder.screen

import android.transition.Scene
import android.transition.TransitionManager
import android.view.ViewGroup
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.dropin.DropInNavigationViewContext
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.dropin.binder.UIBinder
import com.mapbox.navigation.dropin.binder.action.ActiveGuidanceActionBinder
import com.mapbox.navigation.dropin.binder.navigationListOf
import com.mapbox.navigation.dropin.component.camera.DropInCameraMode
import com.mapbox.navigation.dropin.component.camera.DropInNavigationCamera
import com.mapbox.navigation.dropin.component.location.DropInLocationPuck
import com.mapbox.navigation.dropin.component.location.DropInLocationState
import com.mapbox.navigation.dropin.component.routeline.DropInRouteLine
import com.mapbox.navigation.dropin.databinding.MapboxScreenActiveGuidanceLayoutBinding

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class ActiveGuidanceScreenBinder(
    val navigationViewContext: DropInNavigationViewContext
) : UIBinder {

    private val cameraState = navigationViewContext.viewModel.cameraState
    private val maneuverBinder = navigationViewContext.uiBinders.maneuver
    private val speedLimitBinder = navigationViewContext.uiBinders.speedLimit
    private val roadNameBinder = navigationViewContext.uiBinders.roadName

    override fun bind(viewGroup: ViewGroup): MapboxNavigationObserver {
        val scene = Scene.getSceneForLayout(
            viewGroup,
            R.layout.mapbox_screen_active_guidance_layout,
            viewGroup.context
        )
        TransitionManager.go(scene)
        val binding = MapboxScreenActiveGuidanceLayoutBinding.bind(viewGroup)

        cameraState.cameraMode.value = DropInCameraMode.FOLLOWING

        val locationState = DropInLocationState()
        return navigationListOf(
            locationState,
            ActiveGuidanceActionBinder().bind(binding.actionList),
            DropInRouteLine(
                navigationViewContext.mapView,
                navigationViewContext.routeLineOptions,
                locationState
            ),
            DropInNavigationCamera(
                locationState,
                cameraState,
                navigationViewContext.mapView
            ),
            maneuverBinder.bind(binding.guidanceLayout),
            speedLimitBinder.bind(binding.speedLimitLayout),
            DropInLocationPuck(locationState, navigationViewContext.mapView),
            roadNameBinder.bind(binding.roadNameLayout)
        )
    }
}
