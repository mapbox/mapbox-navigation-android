package com.mapbox.navigation.dropin.binder.action

import android.transition.Scene
import android.view.ViewGroup
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.dropin.binder.UIBinder
import com.mapbox.navigation.dropin.binder.navigationListOf
import com.mapbox.navigation.dropin.component.cameramode.CameraModeButtonComponent
import com.mapbox.navigation.dropin.component.recenter.RecenterButtonComponent
import com.mapbox.navigation.dropin.databinding.MapboxActionFreeDriveLayoutBinding

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class FreeDriveActionBinder : UIBinder {
    override fun bind(viewGroup: ViewGroup): MapboxNavigationObserver {
        Scene.getSceneForLayout(
            viewGroup,
            R.layout.mapbox_action_free_drive_layout,
            viewGroup.context
        ).enter()

        val binding = MapboxActionFreeDriveLayoutBinding.bind(viewGroup)

        return navigationListOf(
            CameraModeButtonComponent(binding.cameraModeButton),
            RecenterButtonComponent(binding.recenterButton),
            // TODO add other actions here
        )
    }
}
