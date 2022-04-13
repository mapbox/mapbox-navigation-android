package com.mapbox.navigation.dropin.component.destination

import com.mapbox.api.geocoding.v5.models.CarmenFeature
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.lifecycle.UIViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * Defines actions responsible to mutate the [DestinationState].
 */
@ExperimentalPreviewMapboxNavigationAPI
sealed class DestinationAction {
    /**
     * The action is used to set the destination for the trip
     * @property destination
     */
    data class SetDestination(val destination: Destination?) : DestinationAction()

    /**
     * The action informs if reverse geocoding was successful for a given [point]
     * @property point
     * @property features
     */
    data class DidReverseGeocode(
        val point: Point,
        val features: List<CarmenFeature>
    ) : DestinationAction()
}

@ExperimentalPreviewMapboxNavigationAPI
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
