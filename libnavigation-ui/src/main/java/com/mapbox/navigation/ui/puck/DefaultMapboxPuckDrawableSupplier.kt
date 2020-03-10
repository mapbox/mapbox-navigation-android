package com.mapbox.navigation.ui.puck

import com.mapbox.libnavigation.ui.R
import com.mapbox.navigation.base.trip.model.RouteProgressState

class DefaultMapboxPuckDrawableSupplier : PuckDrawableSupplier {
    override fun getPuckDrawable(routeProgressState: RouteProgressState): Int = when (routeProgressState) {
        RouteProgressState.ROUTE_INVALID -> R.drawable.user_puck_icon_uncertain_location
        RouteProgressState.ROUTE_INITIALIZED -> R.drawable.user_puck_icon
        RouteProgressState.LOCATION_TRACKING -> R.drawable.user_puck_icon
        RouteProgressState.ROUTE_ARRIVED -> R.drawable.user_puck_icon_uncertain_location
        RouteProgressState.LOCATION_STALE -> R.drawable.user_puck_icon
        else -> R.drawable.user_puck_icon_uncertain_location
    }
}
