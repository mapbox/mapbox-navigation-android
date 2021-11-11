package com.mapbox.navigation.dropin.component.sound

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
class SoundButtonViewModel : ViewModel() {

    private val _soundButtonState: MutableStateFlow<SoundButtonState> = MutableStateFlow(
        SoundButtonState.initial()
    )

    internal fun soundButtonState(): Flow<SoundButtonState> = _soundButtonState

    internal fun consumeAction(action: Flow<SoundButtonAction>) {
        viewModelScope.launch {
            action
                .reduce()
                .onEach { _soundButtonState.value = it }
                .stateIn(viewModelScope)
        }
    }

    private fun Flow<SoundButtonAction>.reduce(): Flow<SoundButtonState> =
        scan(_soundButtonState.value) { accumulator, value ->
            when (value) {
                is SoundButtonAction.UpdateNavigationState -> {
                    val visibilityResult = SoundButtonProcessor.ProcessVisibility(
                        value.navigationState
                    ).process()
                    val navigationStateResult = SoundButtonProcessor.ProcessNavigationState(
                        value.navigationState
                    ).process()
                    accumulator.copy(
                        isVisible = visibilityResult.isVisible,
                        navigationState = navigationStateResult.navigationState,
                        volume = accumulator.volume,
                        isMute = accumulator.isMute,
                    )
                }
                is SoundButtonAction.UpdateVolume -> {
                    val volumeResult = SoundButtonProcessor.ProcessVolumeUpdates(
                        value.volume
                    ).process()
                    accumulator.copy(
                        isVisible = accumulator.isVisible,
                        navigationState = accumulator.navigationState,
                        volume = volumeResult.volume,
                        isMute = volumeResult.isMute,
                    )
                }
            }
        }
}
