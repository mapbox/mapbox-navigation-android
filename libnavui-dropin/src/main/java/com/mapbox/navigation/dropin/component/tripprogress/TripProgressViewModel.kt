package com.mapbox.navigation.dropin.component.tripprogress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mapbox.navigation.ui.tripprogress.api.MapboxTripProgressApi
import com.mapbox.navigation.ui.tripprogress.model.TripProgressUpdateFormatter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class TripProgressViewModel(
    private val tripProgressApi: MapboxTripProgressApi
) : ViewModel() {

    private val _tripProgressState: MutableStateFlow<TripProgressState> =
        MutableStateFlow(TripProgressState.initial())

    internal fun tripProgressState(): Flow<TripProgressState> = _tripProgressState

    internal fun consumeAction(action: Flow<TripProgressAction>) {
        viewModelScope.launch {
            action
                .reduce()
                .onEach { _tripProgressState.value = it }
                .stateIn(viewModelScope)
        }
    }

    private fun Flow<TripProgressAction>.reduce(): Flow<TripProgressState> =
        scan(_tripProgressState.value) { accumulator, value ->
            when (value) {
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
