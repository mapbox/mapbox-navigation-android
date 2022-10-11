package com.mapbox.navigation.ui.app.internal.controller

import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.core.trip.session.TripSessionStateObserver
import com.mapbox.navigation.ui.app.internal.Store
import com.mapbox.navigation.ui.app.internal.endNavigation
import com.mapbox.navigation.ui.app.internal.extension.dispatch
import com.mapbox.navigation.ui.base.lifecycle.UIComponent
import java.util.concurrent.atomic.AtomicBoolean

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
            store.dispatch(endNavigation())
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
