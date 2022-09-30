package com.mapbox.navigation.dropin.tripprogress

import android.transition.Scene
import android.transition.TransitionManager
import android.view.ViewGroup
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.dropin.databinding.MapboxTripProgressViewLayoutBinding
import com.mapbox.navigation.dropin.internal.extensions.reloadOnChange
import com.mapbox.navigation.dropin.navigationview.NavigationViewContext
import com.mapbox.navigation.ui.base.lifecycle.UIBinder

@ExperimentalPreviewMapboxNavigationAPI
internal class TripProgressBinder(
    private val navigationViewContext: NavigationViewContext
) : UIBinder {

    override fun bind(viewGroup: ViewGroup): MapboxNavigationObserver {
        val scene = Scene.getSceneForLayout(
            viewGroup,
            R.layout.mapbox_trip_progress_view_layout,
            viewGroup.context
        )
        TransitionManager.go(scene)

        val binding = MapboxTripProgressViewLayoutBinding.bind(viewGroup)
        return reloadOnChange(
            navigationViewContext.styles.tripProgressStyle,
            navigationViewContext.options.distanceFormatterOptions
        ) { styles, formatterOptions ->
            TripProgressComponent(
                store = navigationViewContext.store,
                styles = styles,
                distanceFormatterOptions = formatterOptions,
                tripProgressView = binding.tripProgressView
            )
        }
    }
}
