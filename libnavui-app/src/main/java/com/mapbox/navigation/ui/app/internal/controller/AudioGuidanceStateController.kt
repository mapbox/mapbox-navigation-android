package com.mapbox.navigation.ui.app.internal.controller

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.ui.app.internal.Action
import com.mapbox.navigation.ui.app.internal.State
import com.mapbox.navigation.ui.app.internal.Store
import com.mapbox.navigation.ui.app.internal.audioguidance.AudioAction
import com.mapbox.navigation.ui.app.internal.audioguidance.AudioGuidanceState
import com.mapbox.navigation.ui.voice.internal.MapboxAudioGuidance

/**
 * This class is responsible for playing voice instructions. Use the [AudioAction] to turning the
 * audio on or off.
 */
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class AudioGuidanceStateController(
    private val store: Store
) : StateController() {
    init {
        store.register(this)
    }

    override fun process(state: State, action: Action): State {
        if (action is AudioAction) {
            val audioState = state.audio
            return state.copy(audio = processAudioAction(audioState, action))
        }
        if (action is SetAudioState) {
            return state.copy(audio = action.state)
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

        val audioGuidance = MapboxNavigationApp.getObserver(MapboxAudioGuidance::class)
        audioGuidance.stateFlow().observe {
            if (it.isMuted != store.state.value.audio.isMuted) {
                val newState = AudioGuidanceState(it.isMuted)
                store.dispatch(SetAudioState(newState))
            }
        }
        store.select { it.audio }.observe {
            if (it.isMuted != audioGuidance.stateFlow().value.isMuted) {
                if (it.isMuted) {
                    audioGuidance.mute()
                } else {
                    audioGuidance.unmute()
                }
            }
        }
    }

    private data class SetAudioState(val state: AudioGuidanceState) : Action
}
