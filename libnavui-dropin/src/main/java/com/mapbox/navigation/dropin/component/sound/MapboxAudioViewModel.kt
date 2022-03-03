package com.mapbox.navigation.dropin.component.sound

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.lifecycle.UIViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

sealed class AudioAction {
    object Mute : AudioAction()
    object Unmute : AudioAction()
    object Toggle : AudioAction()
}

@ExperimentalPreviewMapboxNavigationAPI
class MapboxAudioViewModel(
    default: MapboxAudioState = MapboxAudioState()
) : UIViewModel<MapboxAudioState, AudioAction>(default) {

    override fun process(
        mapboxNavigation: MapboxNavigation,
        state: MapboxAudioState,
        action: AudioAction
    ): MapboxAudioState {
        return when (action) {
            is AudioAction.Mute -> state.copy(isMuted = true)
            is AudioAction.Unmute -> state.copy(isMuted = false)
            is AudioAction.Toggle -> state.copy(isMuted = !state.isMuted)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        mainJobControl.scope.launch {
            state.map { it.isMuted }.flatMapLatest { isMuted ->
                if (isMuted) {
                    emptyFlow()
                } else {
                    MapboxAudioApi.create(mapboxNavigation).speakVoiceInstructions()
                }
            }.collect()
        }
    }
}
