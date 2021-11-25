package com.mapbox.navigation.dropin.component.speedlimit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mapbox.navigation.dropin.component.DropInViewModel
import com.mapbox.navigation.ui.speedlimit.api.MapboxSpeedLimitApi
import com.mapbox.navigation.ui.speedlimit.model.SpeedLimitFormatter

internal class SpeedLimitViewModel(
    private val speedLimitApi: MapboxSpeedLimitApi
) : DropInViewModel<SpeedLimitState, SpeedLimitAction>(
    initialState = SpeedLimitState.initial()
) {

    override suspend fun process(
        accumulator: SpeedLimitState,
        value: SpeedLimitAction
    ): SpeedLimitState {
        return when (value) {
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
            return SpeedLimitViewModel(MapboxSpeedLimitApi(formatter)) as T
        }
    }
}
