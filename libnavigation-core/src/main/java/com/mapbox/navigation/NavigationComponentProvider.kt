package com.mapbox.navigation

import android.os.Handler
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.base.trip.TripNotification
import com.mapbox.navigation.directions.session.DirectionsSession
import com.mapbox.navigation.directions.session.MapboxDirectionsSession
import com.mapbox.navigation.navigator.MapboxNativeNavigator
import com.mapbox.navigation.trip.service.MapboxTripService
import com.mapbox.navigation.trip.service.TripService
import com.mapbox.navigation.trip.session.MapboxTripSession
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi

@InternalCoroutinesApi
@ExperimentalCoroutinesApi
internal object NavigationComponentProvider {
    fun createDirectionsSession(router: Router, routeObserver: DirectionsSession.RouteObserver) =
        MapboxDirectionsSession(router, routeObserver)

    fun createTripService(tripNotification: TripNotification, initializeLambda: () -> Unit) =
        MapboxTripService(tripNotification, initializeLambda)

    fun createTripSession(
        tripService: TripService,
        locationEngine: LocationEngine,
        locationEngineRequest: LocationEngineRequest,
        navigator: MapboxNativeNavigator,
        mainHandler: Handler,
        workerHandler: Handler
    ) = MapboxTripSession(
        tripService,
        locationEngine,
        locationEngineRequest,
        navigator
    )
}
