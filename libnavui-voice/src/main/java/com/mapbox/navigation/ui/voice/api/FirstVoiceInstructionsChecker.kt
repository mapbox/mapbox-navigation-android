package com.mapbox.navigation.ui.voice.api

import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.navigation.ui.voice.model.TypeAndAnnouncement

internal class FirstVoiceInstructionsChecker {

    private var expectedFirstInstruction: TypeAndAnnouncement? = null

    fun onNewFirstInstruction(value: VoiceInstructions?) {
        expectedFirstInstruction = value?.let { VoiceInstructionsParser.parse(it) }?.value
    }

    fun isFirstVoiceInstruction(voiceInstructions: VoiceInstructions): Boolean {
        val typeAndAnnouncement = VoiceInstructionsParser.parse(voiceInstructions)
        return expectedFirstInstruction != null &&
            typeAndAnnouncement.value == expectedFirstInstruction
    }
}
