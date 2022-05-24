package com.mapbox.navigation.ui.voice.internal.ui

import androidx.annotation.StyleRes
import androidx.core.view.isVisible
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.ui.base.lifecycle.UIComponent
import com.mapbox.navigation.ui.utils.internal.Provider
import com.mapbox.navigation.ui.utils.internal.extensions.slice
import com.mapbox.navigation.ui.voice.R
import com.mapbox.navigation.ui.voice.internal.MapboxAudioGuidance
import com.mapbox.navigation.ui.voice.view.MapboxAudioGuidanceButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@ExperimentalPreviewMapboxNavigationAPI
interface AudioComponentContract {
    val isMuted: StateFlow<Boolean>
    val isVisible: StateFlow<Boolean>

    fun mute()
    fun unMute()
}

@ExperimentalPreviewMapboxNavigationAPI
internal class MapboxAudioComponentContract(
    scope: CoroutineScope,
    private val audioGuidance: MapboxAudioGuidance
) : AudioComponentContract {

    override val isMuted: StateFlow<Boolean> =
        audioGuidance.stateFlow().slice(scope) { it.isMuted }

    override val isVisible: StateFlow<Boolean>
        get() = MutableStateFlow(true)

    override fun mute() {
        audioGuidance.mute()
    }

    override fun unMute() {
        audioGuidance.unmute()
    }
}

@ExperimentalPreviewMapboxNavigationAPI
class AudioGuidanceButtonComponent(
    private val audioGuidanceButton: MapboxAudioGuidanceButton,
    @StyleRes var audioGuidanceButtonStyle: Int = R.style.MapboxStyleAudioGuidanceButton
) : UIComponent() {

    var contractProvider: Provider<AudioComponentContract> = Provider {
        val audioGuidance = MapboxNavigationApp.getObserver(MapboxAudioGuidance::class)
        MapboxAudioComponentContract(coroutineScope, audioGuidance)
    }

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)
        audioGuidanceButton.updateStyle(audioGuidanceButtonStyle)

        val contract = contractProvider.get()
        contract.isMuted.observe {
            if (it) audioGuidanceButton.mute()
            else audioGuidanceButton.unMute()
        }

        contract.isVisible.observe {
            audioGuidanceButton.isVisible = it
        }

        audioGuidanceButton.setOnClickListener {
            if (contract.isMuted.value) contract.unMute()
            else contract.mute()
        }
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)
        audioGuidanceButton.setOnClickListener(null)
    }
}
