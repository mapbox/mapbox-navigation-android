package com.mapbox.navigation.dropin.coordinator

import android.view.View
import android.view.ViewGroup
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.dropin.NavigationViewContext
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.dropin.component.map.ScalebarPlaceholderComponent
import com.mapbox.navigation.dropin.databinding.MapboxScalebarPlaceholderLayoutBinding
import com.mapbox.navigation.ui.base.lifecycle.UIBinder

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class ScalebarPlaceholderBinder(
    private val context: NavigationViewContext,
) : UIBinder {

    override fun bind(viewGroup: ViewGroup): MapboxNavigationObserver {
        val binding = inflateLayout(viewGroup)
        return ScalebarPlaceholderComponent(
            binding.scalebarPlaceholder,
            context.styles.mapScalebarParams,
            context.maneuverBehavior.maneuverViewHeight
        )
    }

    private fun inflateLayout(viewGroup: ViewGroup): MapboxScalebarPlaceholderLayoutBinding {
        viewGroup.removeAllViews()
        View.inflate(viewGroup.context, R.layout.mapbox_scalebar_placeholder_layout, viewGroup)
        return MapboxScalebarPlaceholderLayoutBinding.bind(viewGroup)
    }
}
