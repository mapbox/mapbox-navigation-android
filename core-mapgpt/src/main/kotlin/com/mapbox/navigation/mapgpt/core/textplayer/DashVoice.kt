package com.mapbox.navigation.mapgpt.core.textplayer

import kotlinx.serialization.Serializable

interface Voice

/**
 * Describes the voice to be used with the default [Player] instance.
 *
 * See [voice1] and [voice2] for available defaults.
 *
 * @param personaVoiceId the unique ID of the voice
 * @param personaName the readable name of the voice
 */
@Serializable
data class DashVoice(
    val personaVoiceId: String,
    val personaName: String,
) : Voice {
    companion object {
        private const val PERSONA_VOICE_1 = "Voice 1"
        private const val PERSONA_VOICE_2 = "Voice 2"
        private const val PERSONA_VOICE_1_ID = "EXAVITQu4vr4xnSDxMaL"
        private const val PERSONA_VOICE_2_ID = "pNInz6obpgDQGcFmaJgB"

        /**
         * Default female voice.
         */
        val voice1 = DashVoice(
            personaVoiceId = PERSONA_VOICE_1_ID,
            personaName = PERSONA_VOICE_1,
        )

        /**
         * Default male voice.
         */
        val voice2 = DashVoice(
            personaVoiceId = PERSONA_VOICE_2_ID,
            personaName = PERSONA_VOICE_2,
        )
    }
}
