package com.mapbox.navigation.core.infra.recorders

import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.navigation.core.trip.session.VoiceInstructionsObserver

class VoiceInstructionsObserverRecorder : VoiceInstructionsObserver {

    private val _records = mutableListOf<VoiceInstructions>()
    val records: List<VoiceInstructions> get() = _records

    override fun onNewVoiceInstructions(voiceInstructions: VoiceInstructions) {
        _records.add(voiceInstructions)
    }
}
