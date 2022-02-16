package com.mapbox.navigation.dropin.binder.infopanel

import android.transition.Scene
import android.transition.TransitionManager
import android.view.ViewGroup
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.dropin.DropInNavigationViewContext
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.dropin.binder.UIBinder
import com.mapbox.navigation.dropin.binder.navigationListOf
import com.mapbox.navigation.dropin.component.infopanel.routepreview.InfoPanelRoutePreviewComponent
import com.mapbox.navigation.dropin.databinding.MapboxInfoPanelRoutePreviewLayoutBinding

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class RoutePreviewInfoPanelBinder(
    private val navigationViewContext: DropInNavigationViewContext
) : UIBinder {

    override fun bind(viewGroup: ViewGroup): MapboxNavigationObserver {
        val scene = Scene.getSceneForLayout(
            viewGroup,
            R.layout.mapbox_info_panel_route_preview_layout,
            viewGroup.context
        )
        TransitionManager.go(scene)
        val binding = MapboxInfoPanelRoutePreviewLayoutBinding.bind(viewGroup)
        return navigationListOf(
            InfoPanelRoutePreviewComponent(binding.root)
        )
    }
}
