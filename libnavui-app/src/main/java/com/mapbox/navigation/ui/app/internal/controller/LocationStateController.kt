package com.mapbox.navigation.ui.app.internal.controller

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.extensions.flowLocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.ui.app.internal.Action
import com.mapbox.navigation.ui.app.internal.State
import com.mapbox.navigation.ui.app.internal.Store
import com.mapbox.navigation.ui.app.internal.location.LocationAction

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class LocationStateController(
    private val store: Store
) : StateController() {
    init {
        store.register(this)
    }

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        mapboxNavigation.flowLocationMatcherResult().observe {
            store.dispatch(LocationAction.Update(it))
        }
    }

    override fun process(state: State, action: Action): State {
        if (action is LocationAction) {
            return state.copy(location = processLocationAction(state.location, action))
        }
        return state
    }

    private fun processLocationAction(
        state: LocationMatcherResult?,
        action: LocationAction
    ): LocationMatcherResult {
        return when (action) {
            is LocationAction.Update -> action.result
        }
    }
}
