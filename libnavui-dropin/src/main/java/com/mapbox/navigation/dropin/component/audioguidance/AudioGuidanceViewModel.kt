package com.mapbox.navigation.dropin.component.audioguidance

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.component.navigation.NavigationState
import com.mapbox.navigation.dropin.lifecycle.UIComponent
import com.mapbox.navigation.dropin.model.Action
import com.mapbox.navigation.dropin.model.Reducer
import com.mapbox.navigation.dropin.model.State
import com.mapbox.navigation.dropin.model.Store
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

/**
 * This class is responsible for playing voice instructions. Use the [AudioAction] to turning the
 * audio on or off.
 */
@ExperimentalPreviewMapboxNavigationAPI
@OptIn(ExperimentalCoroutinesApi::class)
internal class AudioGuidanceViewModel(
    val store: Store
) : UIComponent(), Reducer {
    init {
        store.register(this)
    }

    override fun process(state: State, action: Action): State {
        if (action is AudioAction) {
            val audioState = state.audio
            return state.copy(audio = processAudioAction(audioState, action))
        }
        return state
    }

    private fun processAudioAction(
        state: AudioGuidanceState,
        action: AudioAction
    ): AudioGuidanceState {
        return when (action) {
            is AudioAction.Mute -> state.copy(isMuted = true)
            is AudioAction.Unmute -> state.copy(isMuted = false)
            is AudioAction.Toggle -> state.copy(isMuted = !state.isMuted)
        }
    }

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        val audioGuidanceApi = AudioGuidanceApi.create(mapboxNavigation, AudioGuidanceServices())
        coroutineScope.launch {
            flowSpeakInstructions().flatMapLatest { speakInstructions ->
                if (speakInstructions) {
                    audioGuidanceApi.speakVoiceInstructions()
                } else {
                    emptyFlow()
                }
            }.collect()
        }
    }

    private fun flowSpeakInstructions(): Flow<Boolean> = combine(
        store.select { it.navigation },
        store.select { it.audio },
    ) { navigationState, audioGuidanceState ->
        navigationState is NavigationState.ActiveNavigation &&
            !audioGuidanceState.isMuted
    }
}
