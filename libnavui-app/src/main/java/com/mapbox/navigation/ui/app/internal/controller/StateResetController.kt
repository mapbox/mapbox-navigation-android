package com.mapbox.navigation.ui.app.internal.controller

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.core.trip.session.TripSessionStateObserver
import com.mapbox.navigation.ui.app.internal.Store
import com.mapbox.navigation.ui.app.internal.destination.DestinationAction
import com.mapbox.navigation.ui.app.internal.navigation.NavigationState
import com.mapbox.navigation.ui.app.internal.navigation.NavigationStateAction
import com.mapbox.navigation.ui.app.internal.routefetch.RoutePreviewAction
import com.mapbox.navigation.ui.app.internal.routefetch.RoutesAction
import com.mapbox.navigation.ui.base.lifecycle.UIComponent
import java.util.concurrent.atomic.AtomicBoolean

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class StateResetController(
    private val store: Store,
    private val ignoreTripSessionUpdates: AtomicBoolean
) : UIComponent() {

    private var prevState: TripSessionState? = null

    private val tripSessionStateObserver = TripSessionStateObserver { newState ->
        // we only reset Store state when TripSessionState switches from STARTED to STOPPED.
        if (!ignoreTripSessionUpdates.get() &&
            prevState == TripSessionState.STARTED &&
            newState == TripSessionState.STOPPED
        ) {
            store.dispatch(RoutesAction.SetRoutes(emptyList()))
            store.dispatch(RoutePreviewAction.Ready(emptyList()))
            store.dispatch(DestinationAction.SetDestination(null))
            store.dispatch(NavigationStateAction.Update(NavigationState.FreeDrive))
        }
        prevState = newState
    }

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)
        prevState = mapboxNavigation.getTripSessionState()
        mapboxNavigation.registerTripSessionStateObserver(tripSessionStateObserver)
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)
        mapboxNavigation.unregisterTripSessionStateObserver(tripSessionStateObserver)

        // we reset Store state every time MapboxNavigation gets destroyed
        store.reset()
    }
}
