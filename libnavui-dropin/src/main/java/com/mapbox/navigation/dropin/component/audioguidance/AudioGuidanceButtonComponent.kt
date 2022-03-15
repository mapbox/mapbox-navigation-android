package com.mapbox.navigation.dropin.component.audioguidance

import androidx.core.view.isVisible
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.dropin.component.navigation.NavigationStateViewModel
import com.mapbox.navigation.dropin.component.navigationstate.NavigationState
import com.mapbox.navigation.dropin.lifecycle.UIComponent
import com.mapbox.navigation.dropin.view.MapboxExtendableButton
import com.mapbox.navigation.dropin.view.MapboxExtendableButton.State

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class AudioGuidanceButtonComponent(
    private val audioGuidanceViewModel: AudioGuidanceViewModel,
    private val navigationStateViewModel: NavigationStateViewModel,
    private val soundButton: MapboxExtendableButton,
) : UIComponent() {

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)
        audioGuidanceViewModel.state.observe {
            if (it.isMuted) soundButton.setState(MUTED)
            else soundButton.setState(UN_MUTED)
        }

        navigationStateViewModel.state.observe {
            soundButton.isVisible = it == NavigationState.ActiveNavigation
        }

        soundButton.setOnClickListener {
            audioGuidanceViewModel.invoke(AudioAction.Toggle)
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
