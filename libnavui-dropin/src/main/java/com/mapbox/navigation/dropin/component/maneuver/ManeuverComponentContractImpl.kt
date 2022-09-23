package com.mapbox.navigation.dropin.component.maneuver

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.dropin.NavigationViewContext
import com.mapbox.navigation.ui.maneuver.internal.ManeuverComponentContract
import com.mapbox.navigation.ui.maneuver.view.MapboxManeuverViewState

@ExperimentalPreviewMapboxNavigationAPI
internal class ManeuverComponentContractImpl(
    val context: NavigationViewContext
) : ManeuverComponentContract {

    override fun onManeuverViewStateChanged(state: MapboxManeuverViewState) {
        context.maneuverBehavior.updateBehavior(state)
    }

    override fun onManeuverViewHeightChanged(newHeight: Int) {
        context.maneuverBehavior.updateViewHeight(newHeight)
    }
}
