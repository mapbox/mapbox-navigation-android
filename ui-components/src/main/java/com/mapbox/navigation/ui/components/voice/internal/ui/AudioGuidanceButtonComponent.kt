package com.mapbox.navigation.ui.components.voice.internal.ui

import androidx.core.view.isVisible
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.ui.base.lifecycle.UIComponent
import com.mapbox.navigation.ui.components.voice.view.MapboxAudioGuidanceButton
import com.mapbox.navigation.ui.utils.internal.Provider
import com.mapbox.navigation.ui.utils.internal.extensions.slice
import com.mapbox.navigation.voice.api.MapboxAudioGuidance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface AudioComponentContract {
    val isMuted: StateFlow<Boolean>
    val isVisible: StateFlow<Boolean>

    fun mute()
    fun unMute()
}

internal class MapboxAudioComponentContract(
    scope: CoroutineScope,
    private val audioGuidance: MapboxAudioGuidance,
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

class AudioGuidanceButtonComponent(
    private val audioGuidanceButton: MapboxAudioGuidanceButton,
    contractProvider: Provider<AudioComponentContract>? = null,
) : UIComponent() {

    private val contractProvider: Provider<AudioComponentContract>

    init {
        this.contractProvider = contractProvider ?: Provider {
            val mapboxAudioGuidance = MapboxAudioGuidance.getRegisteredInstance()
            MapboxAudioComponentContract(coroutineScope, mapboxAudioGuidance)
        }
    }

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        val contract = contractProvider.get()
        contract.isMuted.observe {
            if (it) {
                audioGuidanceButton.mute()
            } else {
                audioGuidanceButton.unmute()
            }
        }

        contract.isVisible.observe {
            audioGuidanceButton.isVisible = it
        }

        audioGuidanceButton.setOnClickListener {
            if (contract.isMuted.value) {
                contract.unMute()
            } else {
                contract.mute()
            }
        }
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)
        audioGuidanceButton.setOnClickListener(null)
    }
}
