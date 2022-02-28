package com.mapbox.navigation.dropin.binder.infopanel

import android.transition.Scene
import android.transition.TransitionManager
import android.view.ViewGroup
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.dropin.binder.UIBinder
import com.mapbox.navigation.dropin.binder.navigationListOf
import com.mapbox.navigation.dropin.component.infopanel.InfoPanelFreeDriveComponent
import com.mapbox.navigation.dropin.databinding.MapboxInfoPanelFreeDriveLayoutBinding
import javax.inject.Inject

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class FreeDriveInfoPanelBinder @Inject constructor(
    private val freeDriveComponentFactory: InfoPanelFreeDriveComponent.Factory
) : UIBinder {

    override fun bind(viewGroup: ViewGroup): MapboxNavigationObserver {
        val scene = Scene.getSceneForLayout(
            viewGroup,
            R.layout.mapbox_info_panel_free_drive_layout,
            viewGroup.context
        )
        TransitionManager.go(scene)

        val binding = MapboxInfoPanelFreeDriveLayoutBinding.bind(viewGroup)
        return navigationListOf(
            freeDriveComponentFactory.create(binding)
        )
    }
}
