package com.mapbox.navigation.dropin.component.audioguidance

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

/**
 * This class is responsible for playing voice instructions. Use the [AudioAction] to turning the
 * audio on or off.
 */
@ExperimentalPreviewMapboxNavigationAPI
class AudioGuidanceViewModel(
    default: AudioGuidanceState = AudioGuidanceState()
) : UIViewModel<AudioGuidanceState, AudioAction>(default) {

    override fun process(
        mapboxNavigation: MapboxNavigation,
        state: AudioGuidanceState,
        action: AudioAction
    ): AudioGuidanceState {
        return when (action) {
            is AudioAction.Mute -> state.copy(isMuted = true)
            is AudioAction.Unmute -> state.copy(isMuted = false)
            is AudioAction.Toggle -> state.copy(isMuted = !state.isMuted)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        val audioGuidanceApi = AudioGuidanceApi.create(mapboxNavigation, AudioGuidanceServices())
        mainJobControl.scope.launch {
            state.map { it.isMuted }.flatMapLatest { isMuted ->
                if (isMuted) {
                    emptyFlow()
                } else {
                    audioGuidanceApi.speakVoiceInstructions()
                }
            }.collect()
        }
    }
}
