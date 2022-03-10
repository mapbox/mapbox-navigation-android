package com.mapbox.navigation.dropin.binder.infopanel

import android.transition.Scene
import android.view.ViewGroup
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.dropin.binder.UIBinder
import com.mapbox.navigation.dropin.binder.navigationListOf
import com.mapbox.navigation.dropin.databinding.MapboxInfoPanelLayoutBinding

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class InfoPanelBinder(
    private val headerBinder: UIBinder,
    private val contentBinder: UIBinder?
) : UIBinder {

    override fun bind(viewGroup: ViewGroup): MapboxNavigationObserver {
        Scene.getSceneForLayout(
            viewGroup,
            R.layout.mapbox_info_panel_layout,
            viewGroup.context
        ).enter()

        val binding = MapboxInfoPanelLayoutBinding.bind(viewGroup)

        val binders = mutableListOf(
            headerBinder.bind(binding.infoPanelHeader)
        )
        if (contentBinder != null) {
            binders.add(contentBinder.bind(binding.infoPanelContent))
        }
        return navigationListOf(*binders.toTypedArray())
    }
}
