package com.mapbox.navigation.ui.puck

import com.mapbox.libnavigation.ui.R
import com.mapbox.navigation.base.trip.model.RouteProgressState

/**
 * Returns a default puck supplier associated with different RouteProgressState
 */
internal class DefaultMapboxPuckDrawableSupplier : PuckDrawableSupplier {

    /**
     * Returns drawable for puck depending on current [com.mapbox.navigation.base.trip.model.RouteProgressState]
     *
     * @param routeProgressState various progress state that can occur while navigating
     * @return drawable associated to the [com.mapbox.navigation.base.trip.model.RouteProgressState]
     */
    override fun getPuckDrawable(routeProgressState: RouteProgressState): Int = when (routeProgressState) {
        RouteProgressState.ROUTE_INVALID -> R.drawable.user_puck_icon_uncertain_location
        RouteProgressState.ROUTE_INITIALIZED -> R.drawable.user_puck_icon
        RouteProgressState.LOCATION_TRACKING -> R.drawable.user_puck_icon
        RouteProgressState.ROUTE_ARRIVED -> R.drawable.user_puck_icon_uncertain_location
        RouteProgressState.LOCATION_STALE -> R.drawable.user_puck_icon
        else -> R.drawable.user_puck_icon_uncertain_location
    }
}
