package com.mapbox.navigation.ui.app.internal.controller

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.extensions.flowTripSessionState
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.ui.app.internal.Store
import com.mapbox.navigation.ui.base.lifecycle.UIComponent
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class, FlowPreview::class)
internal class StateResetController(private val store: Store) : UIComponent() {

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        // TripSessionState might change in quick succession.
        // Debouncing it by 100 ms ensures we skip transient values.
        var prevState = mapboxNavigation.getTripSessionState()
        mapboxNavigation.flowTripSessionState().debounce(100).observe { newState ->
            if (prevState == TripSessionState.STARTED && newState == TripSessionState.STOPPED) {
                // we only reset Store state when TripSessionState switches from STARTED to STOPPED.
                mapboxNavigation.setNavigationRoutes(emptyList())
                store.reset()
            }
            prevState = newState
        }
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)

        // we reset Store state every time MapboxNavigation gets destroyed
        store.reset()
    }
}
