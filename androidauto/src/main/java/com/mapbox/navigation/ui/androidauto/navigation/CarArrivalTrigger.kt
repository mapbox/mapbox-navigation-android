package com.mapbox.navigation.ui.androidauto.navigation

import com.mapbox.maps.extension.androidauto.MapboxCarMapObserver
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.arrival.ArrivalObserver
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.ui.androidauto.internal.logAndroidAuto
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreen
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreenManager

/**
 * When attached this will observe when the final destination is reached and change the car app
 * state to the [MapboxScreen.ARRIVAL]. This is not a [MapboxCarMapObserver] because
 * arrival can happen even when the map is not showing.
 */
class CarArrivalTrigger : MapboxNavigationObserver {

    private val arrivalObserver = object : ArrivalObserver {

        override fun onFinalDestinationArrival(routeProgress: RouteProgress) {
            triggerArrival()
        }

        override fun onNextRouteLegStart(routeLegProgress: RouteLegProgress) {
            // not implemented
        }

        override fun onWaypointArrival(routeProgress: RouteProgress) {
            // not implemented
        }
    }

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        mapboxNavigation.registerArrivalObserver(arrivalObserver)
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        mapboxNavigation.unregisterArrivalObserver(arrivalObserver)
    }

    /**
     * Manually trigger the arrival.
     */
    fun triggerArrival() {
        logAndroidAuto("CarArrivalTrigger triggerArrival")
        MapboxScreenManager.replaceTop(MapboxScreen.ARRIVAL)
    }
}
