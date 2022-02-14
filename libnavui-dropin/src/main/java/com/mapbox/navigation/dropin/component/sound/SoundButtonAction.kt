package com.mapbox.navigation.dropin.component.sound

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.dropin.lifecycle.UIComponent
import com.mapbox.navigation.ui.voice.view.MapboxSoundButton
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class SoundButtonAction(
    private val soundButton: MapboxSoundButton,
) : UIComponent() {

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)
        val audioGuidance = MapboxNavigationApp.getObserver(MapboxAudioBehavior::class)

        coroutineScope.launch {
            audioGuidance.stateFlow().collect { state ->
                when (state.isMuted) {
                    true -> soundButton.mute()
                    else -> soundButton.unmute()
                }
            }
        }
        soundButton.setOnClickListener {
            audioGuidance.toggle()
        }
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)
        soundButton.setOnClickListener(null)
    }
}
