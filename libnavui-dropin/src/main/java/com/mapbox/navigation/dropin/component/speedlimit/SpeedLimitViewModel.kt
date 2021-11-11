package com.mapbox.navigation.dropin.component.speedlimit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mapbox.navigation.ui.speedlimit.api.MapboxSpeedLimitApi
import com.mapbox.navigation.ui.speedlimit.model.SpeedLimitFormatter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class SpeedLimitViewModel(
    speedLimitFormatter: SpeedLimitFormatter
) : ViewModel() {

    private val speedLimitApi = MapboxSpeedLimitApi(speedLimitFormatter)
    private val _speedLimitState: MutableStateFlow<SpeedLimitState> = MutableStateFlow(
        SpeedLimitState.initial()
    )

    internal fun speedLimitState(): Flow<SpeedLimitState> = _speedLimitState

    internal fun consumeAction(action: Flow<SpeedLimitAction>) {
        viewModelScope.launch {
            action
                .reduce()
                .onEach { _speedLimitState.value = it }
                .stateIn(viewModelScope)
        }
    }

    private fun Flow<SpeedLimitAction>.reduce(): Flow<SpeedLimitState> =
        scan(_speedLimitState.value) { accumulator, value ->
            when (value) {
                is SpeedLimitAction.UpdateNavigationState -> {
                    val visibilityResult = SpeedLimitProcessor.ProcessVisibility(
                        value.navigationState
                    ).process()
                    val navigationStateResult = SpeedLimitProcessor.ProcessNavigationState(
                        value.navigationState
                    ).process()
                    accumulator.copy(
                        isVisible = visibilityResult.isVisible,
                        navigationState = navigationStateResult.navigationState,
                        speedLimit = accumulator.speedLimit
                    )
                }
                is SpeedLimitAction.UpdateLocationMatcher -> {
                    val speedLimitResult = SpeedLimitProcessor.ProcessLocationMatcher(
                        value.locationMatcher,
                        speedLimitApi
                    ).process()
                    accumulator.copy(
                        isVisible = accumulator.isVisible,
                        navigationState = accumulator.navigationState,
                        speedLimit = speedLimitResult.speedLimit
                    )
                }
            }
        }

    class Factory(private val formatter: SpeedLimitFormatter) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SpeedLimitViewModel(formatter) as T
        }
    }
}
