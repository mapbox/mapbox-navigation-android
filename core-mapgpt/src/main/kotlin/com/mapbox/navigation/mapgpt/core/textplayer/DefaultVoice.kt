package com.mapbox.navigation.mapgpt.core.textplayer

/**
 * When the TTS engine can choose a voice, the default voice is used.
 *
 * Internal use only. This class takes the place of a null [Voice] value.
 */
internal object DefaultVoice : Voice {
    override fun toString(): String = "DefaultVoice"
}
