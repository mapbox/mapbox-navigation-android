package com.mapbox.navigation.dropin.maneuver

import com.mapbox.navigation.dropin.navigationview.NavigationViewContext
import com.mapbox.navigation.ui.maneuver.internal.ManeuverComponentContract
import com.mapbox.navigation.ui.maneuver.view.MapboxManeuverViewState

internal class ManeuverComponentContractImpl(
    val context: NavigationViewContext
) : ManeuverComponentContract {

    override fun onManeuverViewStateChanged(state: MapboxManeuverViewState) {
        context.behavior.maneuverBehavior.updateBehavior(state)
    }

    override fun onManeuverViewVisibilityChanged(isVisible: Boolean) {
        context.behavior.maneuverBehavior.updateViewVisibility(isVisible)
    }
}
