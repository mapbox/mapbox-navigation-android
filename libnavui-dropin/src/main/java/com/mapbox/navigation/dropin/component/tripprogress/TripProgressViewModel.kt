package com.mapbox.navigation.dropin.component.tripprogress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mapbox.navigation.dropin.component.DropInViewModel
import com.mapbox.navigation.ui.tripprogress.api.MapboxTripProgressApi
import com.mapbox.navigation.ui.tripprogress.model.TripProgressUpdateFormatter

internal class TripProgressViewModel(
    private val tripProgressApi: MapboxTripProgressApi
) : DropInViewModel<TripProgressState, TripProgressAction>(
    initialState = TripProgressState.initial()
) {

    override suspend fun process(
        accumulator: TripProgressState,
        value: TripProgressAction
    ): TripProgressState {
        return when (value) {
            is TripProgressAction.UpdateNavigationState -> {
                val visibilityResult = TripProgressProcessor.ProcessVisibility(
                    value.navigationState
                ).process()
                val navigationStateResult = TripProgressProcessor.ProcessNavigationState(
                    value.navigationState
                ).process()
                accumulator.copy(
                    isVisible = visibilityResult.isVisible,
                    navigationState = navigationStateResult.navigationState,
                )
            }
        }
    }

    class Factory(private val tripProgressFormatter: TripProgressUpdateFormatter) :
        ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TripProgressViewModel(MapboxTripProgressApi(tripProgressFormatter)) as T
        }
    }
}
