package com.mapbox.navigation.ui.puck

import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.ui.R

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
    override fun getPuckDrawable(routeProgressState: RouteProgressState): Int =
        when (routeProgressState) {
            RouteProgressState.ROUTE_INVALID -> R.drawable.mapbox_ic_user_puck_uncertain_location
            RouteProgressState.ROUTE_INITIALIZED -> R.drawable.mapbox_ic_user_puck
            RouteProgressState.LOCATION_TRACKING -> R.drawable.mapbox_ic_user_puck
            RouteProgressState.ROUTE_COMPLETE -> R.drawable.mapbox_ic_user_puck_uncertain_location
            RouteProgressState.LOCATION_STALE -> R.drawable.mapbox_ic_user_puck
            else -> R.drawable.mapbox_ic_user_puck_uncertain_location
        }
}
