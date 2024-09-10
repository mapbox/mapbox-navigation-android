package com.mapbox.navigation.voice.api

import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory.createError
import com.mapbox.bindgen.ExpectedFactory.createValue
import com.mapbox.navigation.voice.model.SpeechAnnouncement
import com.mapbox.navigation.voice.model.TypeAndAnnouncement

internal object VoiceInstructionsParser {

    private const val SSML_TYPE = "ssml"
    private const val TEXT_TYPE = "text"

    fun parse(speechAnnouncement: SpeechAnnouncement): Expected<Throwable, TypeAndAnnouncement> {
        return parse(speechAnnouncement.announcement, speechAnnouncement.ssmlAnnouncement)
    }

    fun parse(voiceInstructions: VoiceInstructions): Expected<Throwable, TypeAndAnnouncement> {
        return parse(voiceInstructions.announcement(), voiceInstructions.ssmlAnnouncement())
    }

    private fun parse(
        announcement: String?,
        ssmlAnnouncement: String?,
    ): Expected<Throwable, TypeAndAnnouncement> {
        val (type, instruction) =
            if (!ssmlAnnouncement.isNullOrBlank()) {
                Pair(SSML_TYPE, ssmlAnnouncement)
            } else if (!announcement.isNullOrBlank()) {
                Pair(TEXT_TYPE, announcement)
            } else {
                return createError(invalidInstructionsError())
            }
        return createValue(TypeAndAnnouncement(type, instruction))
    }

    private fun invalidInstructionsError() =
        Error("VoiceInstructions ssmlAnnouncement / announcement can't be null or blank")
}
