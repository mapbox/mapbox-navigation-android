package com.mapbox.navigation.dropin.component.recenter

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
class RecenterViewModel : ViewModel() {

    private val _recenterState: MutableStateFlow<RecenterState> = MutableStateFlow(
        RecenterState.initial()
    )

    internal fun recenterState(): Flow<RecenterState> = _recenterState

    internal fun consumeAction(action: Flow<RecenterButtonAction>) {
        viewModelScope.launch {
            action
                .reduce()
                .onEach { _recenterState.value = it }
                .stateIn(viewModelScope)
        }
    }

    private fun Flow<RecenterButtonAction>.reduce(): Flow<RecenterState> =
        scan(_recenterState.value) { accumulator, value ->
            when (value) {
                is RecenterButtonAction.UpdateNavigationState -> {
                    val visibilityResult = RecenterButtonProcessor.ProcessVisibilityState(
                        value.navigationState,
                        accumulator.cameraState
                    ).process()
                    val navigationStateResult = RecenterButtonProcessor.ProcessNavigationState(
                        value.navigationState
                    ).process()
                    accumulator.copy(
                        isVisible = visibilityResult.isVisible,
                        navigationState = navigationStateResult.navigationState,
                        cameraState = accumulator.cameraState
                    )
                }
                is RecenterButtonAction.UpdateCameraState -> {
                    val visibilityResult = RecenterButtonProcessor.ProcessVisibilityState(
                        accumulator.navigationState,
                        value.cameraState
                    ).process()
                    val cameraStateResult = RecenterButtonProcessor.ProcessCameraState(
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
