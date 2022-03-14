package com.mapbox.navigation.dropin.component.destination

import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.lifecycle.UIViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
internal class DestinationViewModel(
    initialState: DestinationState = DestinationState()
) : UIViewModel<DestinationState, DestinationAction>(initialState) {

    override fun process(
        mapboxNavigation: MapboxNavigation,
        state: DestinationState,
        action: DestinationAction
    ): DestinationState {
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
