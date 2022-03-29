package com.mapbox.navigation.dropin.component.audioguidance

import androidx.annotation.StyleRes
import androidx.core.view.isVisible
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.component.navigation.NavigationState
import com.mapbox.navigation.dropin.lifecycle.UIComponent
import com.mapbox.navigation.dropin.model.Store
import com.mapbox.navigation.dropin.view.MapboxAudioGuidanceButton

@ExperimentalPreviewMapboxNavigationAPI
internal class AudioGuidanceButtonComponent(
    private val store: Store,
    private val audioGuidanceButton: MapboxAudioGuidanceButton,
    @StyleRes val audioGuidanceButtonStyle: Int,
) : UIComponent() {

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)
        audioGuidanceButton.updateStyle(audioGuidanceButtonStyle)
        store.select { it.audio }.observe {
            if (it.isMuted) audioGuidanceButton.mute()
            else audioGuidanceButton.unMute()
        }

        store.select { it.navigation }.observe {
            audioGuidanceButton.isVisible = it == NavigationState.ActiveNavigation
        }

        audioGuidanceButton.setOnClickListener {
            store.dispatch(AudioAction.Toggle)
        }
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)
        audioGuidanceButton.setOnClickListener(null)
    }
}
