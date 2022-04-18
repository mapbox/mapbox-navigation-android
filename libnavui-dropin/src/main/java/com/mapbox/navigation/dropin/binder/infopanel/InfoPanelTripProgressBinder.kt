package com.mapbox.navigation.dropin.binder.infopanel

import android.transition.Scene
import android.transition.TransitionManager
import android.view.ViewGroup
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.dropin.NavigationViewContext
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.dropin.binder.UIBinder
import com.mapbox.navigation.dropin.component.tripprogress.TripProgressComponent
import com.mapbox.navigation.dropin.databinding.MapboxInfoPanelTripProgressLayoutBinding
import com.mapbox.navigation.dropin.internal.extensions.reloadOnChange

@ExperimentalPreviewMapboxNavigationAPI
internal class InfoPanelTripProgressBinder(
    private val navigationViewContext: NavigationViewContext
) : UIBinder {

    override fun bind(viewGroup: ViewGroup): MapboxNavigationObserver {
        val scene = Scene.getSceneForLayout(
            viewGroup,
            R.layout.mapbox_info_panel_trip_progress_layout,
            viewGroup.context
        )
        TransitionManager.go(scene)

        val binding = MapboxInfoPanelTripProgressLayoutBinding.bind(viewGroup)
        return reloadOnChange(
            navigationViewContext.styles.tripProgressStyle
        ) { styles ->
            TripProgressComponent(
                styles = styles,
                tripProgressView = binding.tripProgressView
            )
        }
    }
}
