package com.mapbox.navigation.dropin.component.sound

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.dropin.lifecycle.UIComponent
import com.mapbox.navigation.dropin.view.MapboxExtendableButton
import com.mapbox.navigation.dropin.view.MapboxExtendableButton.State

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class SoundButtonAction(
    private val soundButton: MapboxExtendableButton,
) : UIComponent() {

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)
        val audioGuidance = MapboxNavigationApp.getObserver(MapboxAudioBehavior::class)

        audioGuidance.audioState.observe {
            if (it.isMuted) soundButton.setState(MUTED)
            else soundButton.setState(UN_MUTED)
        }

        soundButton.setOnClickListener {
            audioGuidance.toggle()
        }
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)
        soundButton.setOnClickListener(null)
    }

    companion object ButtonStates {
        private val UN_MUTED = State(R.drawable.mapbox_ic_sound_on)
        private val MUTED = State(R.drawable.mapbox_ic_sound_off)
    }
}
