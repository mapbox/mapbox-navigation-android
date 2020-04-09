package com.mapbox.navigation.core.trip.session

import com.mapbox.api.directions.v5.models.VoiceInstructions

interface VoiceInstructionsObserver {
    fun onNewVoiceInstructions(voiceInstructions: VoiceInstructions)
}
