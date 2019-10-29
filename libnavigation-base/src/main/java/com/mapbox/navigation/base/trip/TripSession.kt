package com.mapbox.navigation.base.trip

import android.location.Location
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.navigation.base.route.model.Route

interface TripSession {

    val tripService: TripService
    val locationEngine: LocationEngine
    val locationEngineRequest: LocationEngineRequest
    var route: Route?

    fun getRawLocation(): Location?
    fun getEnhancedLocation(): Location?
    fun getRouteProgress(): RouteProgress?

    fun start()
    fun stop()

    fun registerLocationObserver(locationObserver: LocationObserver)
    fun unregisterLocationObserver(locationObserver: LocationObserver)

    fun registerRouteProgressObserver(routeProgressObserver: RouteProgressObserver)
    fun unregisterRouteProgressObserver(routeProgressObserver: RouteProgressObserver)

    interface LocationObserver {
        fun onRawLocationChanged(rawLocation: Location)
        fun onEnhancedLocationChanged(enhancedLocation: Location)
    }

    interface RouteProgressObserver {
        fun onRouteProgressChanged(routeProgress: RouteProgress)
    }
}
