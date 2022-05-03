package com.mapbox.navigation.dropin.controller

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.dropin.component.destination.DestinationAction
import com.mapbox.navigation.dropin.model.Action
import com.mapbox.navigation.dropin.model.State
import com.mapbox.navigation.dropin.model.Store
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalPreviewMapboxNavigationAPI
@OptIn(ExperimentalCoroutinesApi::class)
internal class DestinationStateController(
    store: Store
) : StateController() {
    init {
        store.register(this)
    }

    override fun process(state: State, action: Action): State {
        when (action) {
            is DestinationAction.SetDestination -> {
                return state.copy(destination = action.destination)
            }
            is DestinationAction.DidReverseGeocode -> {
                if (state.destination?.point == action.point) {
                    val destWithFeatures = state.destination.copy(
                        features = action.features
                    )
                    return state.copy(destination = destWithFeatures)
                }
            }
        }
        return state
    }
}
