package com.mapbox.navigation.dropin.component.routeoverview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class RouteOverviewViewModel : ViewModel() {

    private val _routeOverviewState: MutableStateFlow<RouteOverviewState> = MutableStateFlow(
        RouteOverviewState.initial()
    )

    internal fun routeOverviewState(): Flow<RouteOverviewState> = _routeOverviewState

    internal fun consumeAction(action: Flow<RouteOverviewButtonAction>) {
        viewModelScope.launch {
            action
                .reduce()
                .onEach { _routeOverviewState.value = it }
                .stateIn(viewModelScope)
        }
    }

    private fun Flow<RouteOverviewButtonAction>.reduce(): Flow<RouteOverviewState> =
        scan(_routeOverviewState.value) { accumulator, value ->
            when (value) {
                is RouteOverviewButtonAction.UpdateNavigationState -> {
                    val visibilityResult = RouteOverviewButtonProcessor.ProcessVisibilityState(
                        value.navigationState,
                        accumulator.cameraState
                    ).process()
                    val navigationStateResult = RouteOverviewButtonProcessor.ProcessNavigationState(
                        value.navigationState
                    ).process()
                    accumulator.copy(
                        isVisible = visibilityResult.isVisible,
                        navigationState = navigationStateResult.navigationState,
                        cameraState = accumulator.cameraState
                    )
                }
                is RouteOverviewButtonAction.UpdateCameraState -> {
                    val visibilityResult = RouteOverviewButtonProcessor.ProcessVisibilityState(
                        accumulator.navigationState,
                        value.cameraState
                    ).process()
                    val cameraStateResult = RouteOverviewButtonProcessor.ProcessCameraState(
                        value.cameraState
                    ).process()
                    accumulator.copy(
                        isVisible = visibilityResult.isVisible,
                        navigationState = accumulator.navigationState,
                        cameraState = cameraStateResult.cameraState
                    )
                }
            }
        }
}
