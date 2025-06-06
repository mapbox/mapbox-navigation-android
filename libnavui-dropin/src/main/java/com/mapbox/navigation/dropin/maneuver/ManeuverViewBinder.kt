package com.mapbox.navigation.dropin.maneuver

import android.view.LayoutInflater
import android.view.ViewGroup
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.dropin.databinding.MapboxManeuverGuidanceLayoutBinding
import com.mapbox.navigation.dropin.internal.extensions.reloadOnChange
import com.mapbox.navigation.dropin.navigationview.NavigationViewContext
import com.mapbox.navigation.ui.base.lifecycle.UIBinder
import com.mapbox.navigation.ui.maneuver.internal.ManeuverComponent
import com.mapbox.navigation.ui.maps.internal.extensions.getStyleId
import com.mapbox.navigation.ui.maps.internal.extensions.getUserId

internal class ManeuverViewBinder(
    private val context: NavigationViewContext
) : UIBinder {

    override fun bind(viewGroup: ViewGroup): MapboxNavigationObserver {
        viewGroup.removeAllViews()
        val binding = MapboxManeuverGuidanceLayoutBinding.inflate(
            LayoutInflater.from(viewGroup.context),
            viewGroup
        )

        return reloadOnChange(
            context.mapStyleLoader.loadedMapStyle,
            context.styles.maneuverViewOptions,
            context.options.distanceFormatterOptions
        ) { mapStyle, options, distanceFormatterOptions ->
            if (mapStyle != null) {
                ManeuverComponent(
                    maneuverView = binding.maneuverView,
                    userId = mapStyle.getUserId(),
                    styleId = mapStyle.getStyleId(),
                    options = options,
                    formatterOptions = distanceFormatterOptions,
                    contract = { ManeuverComponentContractImpl(context) }
                )
            } else {
                null
            }
        }
    }
}
