package com.mapbox.navigation.dropin.component.maneuver

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mapbox.navigation.base.formatter.DistanceFormatter
import com.mapbox.navigation.dropin.component.DropInViewModel
import com.mapbox.navigation.ui.maneuver.api.MapboxManeuverApi
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
internal class ManeuverViewModel(
    private val maneuverApi: MapboxManeuverApi
) : DropInViewModel<ManeuverState, ManeuverAction>(
    initialState = ManeuverState.initial()
) {

    override suspend fun process(accumulator: ManeuverState, value: ManeuverAction): ManeuverState {
        return when (value) {
            is ManeuverAction.UpdateNavigationState -> {
                val navigationStateResult = ManeuverProcessor.ProcessNavigationState(
                    value.navigationState
                ).process()
                val visibilityResult = ManeuverProcessor.ProcessVisibility(
                    navigationState = value.navigationState,
                    maneuver = accumulator.maneuver
                ).process()
                accumulator.copy(
                    isVisible = visibilityResult.isVisible,
                    navigationState = navigationStateResult.navigationState,
                    maneuver = accumulator.maneuver
                )
            }
            is ManeuverAction.UpdateRouteProgress -> {
                val routeProgressResult = ManeuverProcessor.ProcessRouteProgress(
                    value.routeProgress,
                    maneuverApi
                ).process()
                val visibilityResult = ManeuverProcessor.ProcessVisibility(
                    navigationState = accumulator.navigationState,
                    maneuver = routeProgressResult.maneuver
                ).process()
                accumulator.copy(
                    isVisible = visibilityResult.isVisible,
                    navigationState = accumulator.navigationState,
                    maneuver = routeProgressResult.maneuver
                )
            }
        }
    }

    class Factory(private val distanceFormatter: DistanceFormatter) :
        ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ManeuverViewModel(MapboxManeuverApi(distanceFormatter)) as T
        }
    }
}
