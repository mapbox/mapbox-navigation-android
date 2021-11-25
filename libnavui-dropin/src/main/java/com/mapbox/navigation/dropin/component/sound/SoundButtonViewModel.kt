package com.mapbox.navigation.dropin.component.sound

import com.mapbox.navigation.dropin.component.DropInViewModel

internal class SoundButtonViewModel : DropInViewModel<SoundButtonState, SoundButtonAction>(
    initialState = SoundButtonState.initial()
) {

    override suspend fun process(
        accumulator: SoundButtonState,
        value: SoundButtonAction
    ): SoundButtonState {
        return when (value) {
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
