package com.mapbox.navigation.core.trip.session

import androidx.annotation.UiThread
import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.navigation.core.MapboxNavigation

/**
 * Voice instruction interface, which can be registered via [MapboxNavigation.registerVoiceInstructionsObserver]
 */
@UiThread
fun interface VoiceInstructionsObserver {
    /**
     * Called every time on a new [VoiceInstructions] is applicable. Only new voice instructions
     * are available via this observer because the timing of delivering and playing out
     * an instruction is critical for correct navigation experience.
     * See {@link com.mapbox.navigation.base.trip.model.RouteProgress.voiceInstructions} to get
     * a current voice instruction.
     */
    fun onNewVoiceInstructions(voiceInstructions: VoiceInstructions)
}
