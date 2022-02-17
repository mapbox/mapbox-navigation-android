package com.mapbox.navigation.dropin.component.sound

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.extensions.flowVoiceInstructions
import com.mapbox.navigation.dropin.lifecycle.UIComponent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.updateAndGet

@ExperimentalPreviewMapboxNavigationAPI
class MapboxAudioBehavior : UIComponent() {

    private val _audioState = MutableStateFlow(MapboxAudioState())
    val audioState = _audioState.asStateFlow()

    fun mute() {
        _audioState.updateAndGet { state -> state.copy(isMuted = true) }
    }

    fun unmute() {
        _audioState.updateAndGet { state -> state.copy(isMuted = false) }
    }

    fun toggle() {
        _audioState.updateAndGet { state -> state.copy(isMuted = !state.isMuted) }
    }

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        // TODO handle all of this in an api class.
        mapboxNavigation.flowVoiceInstructions().observe {
            _audioState.updateAndGet { state ->
                state.copy(voiceInstructions = it)
            }
        }
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)

        _audioState.updateAndGet { MapboxAudioState() }
    }
}
