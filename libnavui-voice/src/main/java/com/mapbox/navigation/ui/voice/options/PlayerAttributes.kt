package com.mapbox.navigation.ui.voice.options

import android.media.AudioAttributes
import android.media.AudioAttributes.CONTENT_TYPE_MUSIC
import android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION
import android.media.AudioAttributes.CONTENT_TYPE_SPEECH
import android.media.AudioAttributes.USAGE_ALARM
import android.media.AudioAttributes.USAGE_ASSISTANCE_SONIFICATION
import android.media.AudioAttributes.USAGE_NOTIFICATION_RINGTONE
import android.media.AudioAttributes.USAGE_VOICE_COMMUNICATION
import android.media.AudioAttributes.USAGE_VOICE_COMMUNICATION_SIGNALLING
import android.media.AudioManager
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * PlayerAttributes
 * An API implements attributes that define how the audio system handles routing
 * and focus decisions for the specified source.
 */
sealed class PlayerAttributes {

    /**
     * Specifies which stream will be used for playing
     * Defaults to [AudioManager.STREAM_MUSIC]
     * See [AudioManager] for a list of stream types.
     * Backport for Oreo and later implementation
     */
    abstract val streamType: Int

    /**
     * An API implements attributes for API below Android O
     */
    data class PreOreoAttributes(
        /**
         * Specifies which stream will be used for playing
         * Defaults to [AudioManager.STREAM_MUSIC]
         * See [AudioManager] for a list of stream types.
         */
        override val streamType: Int = AudioManager.STREAM_MUSIC
    ) : PlayerAttributes()

    @RequiresApi(api = Build.VERSION_CODES.O)
    /**
     * An API implements attributes for API Android O and above
     */
    data class OreoAndLaterAttributes(
        /**
         * Specifies why the source is playing and controls routing, focus, and volume decisions.
         * Defaults to  [AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE]
         * See [AudioAttributes] for a list of usage types.
         */
        val usage: Int = AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE,

        /**
         * Specifies what the source is playing (music, movie, speech, sonification, unknown).
         * Defaults to  [AudioAttributes.CONTENT_TYPE_MUSIC]
         * See [AudioAttributes] for a list of content types.
         */
        val contentType: Int = CONTENT_TYPE_MUSIC
    ) : PlayerAttributes() {

        val audioAttributes: AudioAttributes = AudioAttributes.Builder()
            .setUsage(usage)
            .setContentType(contentType)
            .build()

        override val streamType: Int
            // Compatibility mappings
            // https://source.android.com/devices/audio/attributes#compatibility
            get() = when (contentType) {
                CONTENT_TYPE_SPEECH -> AudioManager.STREAM_VOICE_CALL
                CONTENT_TYPE_MUSIC -> AudioManager.STREAM_MUSIC
                CONTENT_TYPE_SONIFICATION -> when (usage) {
                    USAGE_VOICE_COMMUNICATION -> AudioManager.STREAM_VOICE_CALL
                    USAGE_ASSISTANCE_SONIFICATION -> AudioManager.STREAM_SYSTEM
                    USAGE_NOTIFICATION_RINGTONE -> AudioManager.STREAM_RING
                    USAGE_ALARM -> AudioManager.STREAM_ALARM
                    USAGE_VOICE_COMMUNICATION_SIGNALLING -> AudioManager.STREAM_DTMF
                    else -> AudioManager.STREAM_NOTIFICATION
                }

                else -> AudioManager.STREAM_MUSIC
            }
    }
}
