package com.mapbox.navigation.dropin.component.sound

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.extensions.flowVoiceInstructions
import com.mapbox.navigation.dropin.lifecycle.UIComponent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch

@ExperimentalPreviewMapboxNavigationAPI
class MapboxAudioBehavior : UIComponent() {

    private val mutableStateFlow = MutableStateFlow(MapboxAudioState())

    fun stateFlow(): StateFlow<MapboxAudioState> = mutableStateFlow

    fun mute() {
        mutableStateFlow.updateAndGet { state -> state.copy(isMuted = true) }
    }

    fun unmute() {
        mutableStateFlow.updateAndGet { state -> state.copy(isMuted = false) }
    }

    fun toggle() {
        mutableStateFlow.updateAndGet { state -> state.copy(isMuted = !state.isMuted) }
    }

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        // TODO handle all of this in an api class.
        coroutineScope.launch {
            mapboxNavigation.flowVoiceInstructions().collect {
                mutableStateFlow.updateAndGet { state ->
                    state.copy(voiceInstructions = it)
                }
            }
        }
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)

        mutableStateFlow.updateAndGet { MapboxAudioState() }
    }
}
