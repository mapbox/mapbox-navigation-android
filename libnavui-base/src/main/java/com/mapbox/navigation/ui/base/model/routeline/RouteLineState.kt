package com.mapbox.navigation.ui.base.model.routeline

import android.util.LruCache
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.ui.base.MapboxState

sealed class RouteLineState(val vanishingRouteLineInhibited: Boolean): MapboxState {

    companion object {
        val distanceRemainingCache = LruCache<DirectionsRoute, Float>(2)
    }

    class UpdateDistanceRemainingState(vanishingRouteLineInhibited: Boolean): RouteLineState(vanishingRouteLineInhibited)
    //class NoOpState(vanishingRouteLineInhibited: Boolean): RouteLineState(vanishingRouteLineInhibited)


    fun updateDistanceRemaining(distanceRemaining: Float, route: DirectionsRoute) {
        distanceRemainingCache.put(route, distanceRemaining)
    }
}
