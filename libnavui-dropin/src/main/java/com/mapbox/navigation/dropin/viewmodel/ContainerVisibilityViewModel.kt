package com.mapbox.navigation.dropin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mapbox.navigation.dropin.contract.ContainerVisibilityAction
import com.mapbox.navigation.dropin.contract.ContainerVisibilityResult
import com.mapbox.navigation.dropin.contract.toProcessor
import com.mapbox.navigation.dropin.contract.toResult
import com.mapbox.navigation.dropin.state.ContainerVisibilityState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
class ContainerVisibilityViewModel : ViewModel() {
    private val _containerVisibilityState: MutableStateFlow<ContainerVisibilityState> =
        MutableStateFlow(ContainerVisibilityState.initial())

    internal fun containerVisibilityState(): Flow<ContainerVisibilityState> =
        _containerVisibilityState

    internal suspend fun processAction(action: Flow<ContainerVisibilityAction>) {
        viewModelScope.launch {
            action
                .toProcessor()
                .toResult()
                .reduce()
                .onEach { _containerVisibilityState.value = it }
                .stateIn(viewModelScope)
        }
    }

    private fun Flow<ContainerVisibilityResult>.reduce(): Flow<ContainerVisibilityState> =
        scan(_containerVisibilityState.value) { accumulator, result ->
            when (result) {
                is ContainerVisibilityResult.ForEmpty -> {
                    accumulator.copy(
                        volumeContainerVisible = result.volumeContainerVisible,
                        recenterContainerVisible = result.recenterContainerVisible,
                        maneuverContainerVisible = result.maneuverContainerVisible,
                        infoPanelContainerVisible = result.infoPanelContainerVisible,
                        speedLimitContainerVisible = result.speedLimitContainerVisible,
                        routeOverviewContainerVisible = result.routeOverviewContainerVisible
                    )
                }
                is ContainerVisibilityResult.ForFreeDrive -> {
                    accumulator.copy(
                        volumeContainerVisible = result.volumeContainerVisible,
                        recenterContainerVisible = result.recenterContainerVisible,
                        maneuverContainerVisible = result.maneuverContainerVisible,
                        infoPanelContainerVisible = result.infoPanelContainerVisible,
                        speedLimitContainerVisible = result.speedLimitContainerVisible,
                        routeOverviewContainerVisible = result.routeOverviewContainerVisible
                    )
                }
                is ContainerVisibilityResult.ForRoutePreview -> {
                    accumulator.copy(
                        volumeContainerVisible = result.volumeContainerVisible,
                        recenterContainerVisible = result.recenterContainerVisible,
                        maneuverContainerVisible = result.maneuverContainerVisible,
                        infoPanelContainerVisible = result.infoPanelContainerVisible,
                        speedLimitContainerVisible = result.speedLimitContainerVisible,
                        routeOverviewContainerVisible = result.routeOverviewContainerVisible
                    )
                }
                is ContainerVisibilityResult.ForActiveNavigation -> {
                    accumulator.copy(
                        volumeContainerVisible = result.volumeContainerVisible,
                        recenterContainerVisible = result.recenterContainerVisible,
                        maneuverContainerVisible = result.maneuverContainerVisible,
                        infoPanelContainerVisible = result.infoPanelContainerVisible,
                        speedLimitContainerVisible = result.speedLimitContainerVisible,
                        routeOverviewContainerVisible = result.routeOverviewContainerVisible
                    )
                }
                is ContainerVisibilityResult.ForArrival -> {
                    accumulator.copy(
                        volumeContainerVisible = result.volumeContainerVisible,
                        recenterContainerVisible = result.recenterContainerVisible,
                        maneuverContainerVisible = result.maneuverContainerVisible,
                        infoPanelContainerVisible = result.infoPanelContainerVisible,
                        speedLimitContainerVisible = result.speedLimitContainerVisible,
                        routeOverviewContainerVisible = result.routeOverviewContainerVisible
                    )
                }
            }
        }
}
