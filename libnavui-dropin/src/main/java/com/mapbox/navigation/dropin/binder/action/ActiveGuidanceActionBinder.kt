package com.mapbox.navigation.dropin.binder.action

import android.transition.Scene
import android.transition.Slide
import android.transition.TransitionManager
import android.view.Gravity
import android.view.ViewGroup
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.dropin.binder.UIBinder
import com.mapbox.navigation.dropin.binder.navigationListOf
import com.mapbox.navigation.dropin.component.cameramode.CameraModeButtonComponent
import com.mapbox.navigation.dropin.component.sound.SoundButtonAction
import com.mapbox.navigation.dropin.databinding.MapboxActionActiveGuidanceLayoutBinding

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class ActiveGuidanceActionBinder : UIBinder {

    override fun bind(viewGroup: ViewGroup): MapboxNavigationObserver {
        val scene = Scene.getSceneForLayout(
            viewGroup,
            R.layout.mapbox_action_active_guidance_layout,
            viewGroup.context
        )
        TransitionManager.go(scene, Slide(Gravity.RIGHT))

        val binding = MapboxActionActiveGuidanceLayoutBinding.bind(viewGroup)
        return navigationListOf(
            SoundButtonAction(binding.soundButton),
            CameraModeButtonComponent(binding.cameraModeButton),
            // TODO add other actions here
        )
    }
}
