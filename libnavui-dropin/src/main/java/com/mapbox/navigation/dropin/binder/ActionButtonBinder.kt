package com.mapbox.navigation.dropin.binder

import android.transition.Scene
import android.transition.Slide
import android.transition.TransitionManager
import android.view.Gravity
import android.view.ViewGroup
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.dropin.NavigationViewContext
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.dropin.component.audioguidance.AudioGuidanceButtonComponent
import com.mapbox.navigation.dropin.component.cameramode.CameraModeButtonComponent
import com.mapbox.navigation.dropin.component.recenter.RecenterButtonComponent
import com.mapbox.navigation.dropin.databinding.MapboxActionButtonsLayoutBinding

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class ActionButtonBinder(
    private val navigationViewContext: NavigationViewContext
) : UIBinder {

    override fun bind(viewGroup: ViewGroup): MapboxNavigationObserver {
        val scene = Scene.getSceneForLayout(
            viewGroup,
            R.layout.mapbox_action_buttons_layout,
            viewGroup.context
        )
        TransitionManager.go(scene, Slide(Gravity.RIGHT))

        val binding = MapboxActionButtonsLayoutBinding.bind(viewGroup)
        return navigationListOf(
            AudioGuidanceButtonComponent(
                navigationViewContext.viewModel.audioGuidanceViewModel,
                navigationViewContext.viewModel.navigationStateViewModel,
                binding.soundButton,
            ),
            CameraModeButtonComponent(
                navigationViewContext.viewModel.cameraViewModel,
                navigationViewContext.viewModel.navigationStateViewModel,
                binding.cameraModeButton
            ),
            RecenterButtonComponent(
                navigationViewContext.viewModel.cameraViewModel,
                navigationViewContext.viewModel.navigationStateViewModel,
                binding.recenterButton
            )
        )
    }
}
