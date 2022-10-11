package com.mapbox.navigation.dropin.infopanel

import android.transition.Scene
import android.transition.TransitionManager
import android.view.ViewGroup
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.dropin.databinding.MapboxPoiTextViewLayoutBinding
import com.mapbox.navigation.dropin.map.geocoding.POINameComponent
import com.mapbox.navigation.dropin.navigationview.NavigationViewContext
import com.mapbox.navigation.ui.base.lifecycle.UIBinder

internal class InfoPanelPoiNameBinder(
    private val context: NavigationViewContext
) : UIBinder {

    override fun bind(viewGroup: ViewGroup): MapboxNavigationObserver {
        val scene = Scene.getSceneForLayout(
            viewGroup,
            R.layout.mapbox_poi_text_view_layout,
            viewGroup.context
        )
        TransitionManager.go(scene)
        val binding = MapboxPoiTextViewLayoutBinding.bind(viewGroup)

        return POINameComponent(
            context.store,
            binding.poiName,
            context.styles.poiNameTextAppearance
        )
    }
}
