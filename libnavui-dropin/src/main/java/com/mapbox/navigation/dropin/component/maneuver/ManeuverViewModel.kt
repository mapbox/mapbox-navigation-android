package com.mapbox.navigation.dropin.component.maneuver

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mapbox.navigation.base.formatter.DistanceFormatter
import com.mapbox.navigation.ui.maneuver.api.MapboxManeuverApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ManeuverViewModel(
    private val maneuverApi: MapboxManeuverApi
) : ViewModel() {

    private val _maneuverState: MutableStateFlow<ManeuverState> =
        MutableStateFlow(ManeuverState.initial())

    internal fun maneuverState(): Flow<ManeuverState> = _maneuverState

    internal fun consumeAction(action: Flow<ManeuverAction>) {
        viewModelScope.launch {
            action
                .reduce()
                .onEach { _maneuverState.value = it }
                .stateIn(viewModelScope)
        }
    }

    private fun Flow<ManeuverAction>.reduce(): Flow<ManeuverState> =
        scan(_maneuverState.value) { accumulator, value ->
            when (value) {
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

    class Factory(private val distanceFormatter: DistanceFormatter) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ManeuverViewModel(MapboxManeuverApi(distanceFormatter)) as T
        }
    }
}
