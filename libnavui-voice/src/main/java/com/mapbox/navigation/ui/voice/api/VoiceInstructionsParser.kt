package com.mapbox.navigation.ui.voice.api

import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory.createError
import com.mapbox.bindgen.ExpectedFactory.createValue
import com.mapbox.navigation.ui.voice.model.TypeAndAnnouncement

internal object VoiceInstructionsParser {

    private const val SSML_TYPE = "ssml"
    private const val TEXT_TYPE = "text"

    fun parse(voiceInstructions: VoiceInstructions): Expected<Throwable, TypeAndAnnouncement> {
        val announcement = voiceInstructions.announcement()
        val ssmlAnnouncement = voiceInstructions.ssmlAnnouncement()
        val (type, instruction) =
            if (ssmlAnnouncement != null && !ssmlAnnouncement.isNullOrBlank()) {
                Pair(SSML_TYPE, ssmlAnnouncement)
            } else if (announcement != null && !announcement.isNullOrBlank()) {
                Pair(TEXT_TYPE, announcement)
            } else {
                return createError(invalidInstructionsError())
            }
        return createValue(TypeAndAnnouncement(type, instruction))
    }

    private fun invalidInstructionsError() =
        Error("VoiceInstructions ssmlAnnouncement / announcement can't be null or blank")
}
