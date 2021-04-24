package com.mapbox.navigation.ui.voice.options

import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * VoiceInstructionsPlayerOptions.
 */
class VoiceInstructionsPlayerOptions private constructor(
    /**
     * Defines how audio focus will be requested.
     * Defaults to [AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK]
     * Valid values for focus requests are
     * [AudioManager.AUDIOFOCUS_GAIN], [AudioManager.AUDIOFOCUS_GAIN_TRANSIENT],
     * [AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK] and
     * [AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE].
     * [AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK] is used by default.
     */
    val focusGain: Int,

    /**
     * Defines which stream will be used for playing
     * Defaults to [AudioManager.STREAM_MUSIC]
     * See [AudioManager] for a list of stream types.
     */
    @RequiresApi(api = Build.VERSION_CODES.BASE)
    @Deprecated(
        message = "Deprecated in API level 26",
        replaceWith = ReplaceWith("audioAttributes")
    )
    val streamType: Int,

    /**
     * Defines a collections of attributes describing information about an audio stream.
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    val audioAttributes: AudioAttributes,
) {

    /**
     * Get a builder to customize a subset of current options.
     */
    fun toBuilder(): Builder = Builder().apply {
        focusGain(focusGain)
        streamType(streamType)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioAttributes(audioAttributes)
        }
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "VoiceInstructionsPlayerOptions(" +
            "focusGain=$focusGain, " +
            "streamType=$streamType, " +
            "audioAttributes=$audioAttributes" +
            ")"
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VoiceInstructionsPlayerOptions

        if (focusGain != other.focusGain) return false
        if (streamType != other.streamType) return false
        if (audioAttributes != other.audioAttributes) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = focusGain
        result = 31 * result + streamType
        result = 31 * result + audioAttributes.hashCode()
        return result
    }

    /**
     * Build a new [VoiceInstructionsPlayerOptions]
     */
    class Builder {

        private var focusGain: Int = AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
        private var streamType: Int = AudioManager.STREAM_MUSIC
        private var audioAttributes: AudioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        /**
         * Specifies how audio focus will be requested.
         * Defaults to [AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK]
         * Valid values for focus requests are
         * [AudioManager.AUDIOFOCUS_GAIN],
         * [AudioManager.AUDIOFOCUS_GAIN_TRANSIENT],
         * [AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK] and
         * [AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE].
         */
        fun focusGain(focusGain: Int): Builder =
            apply {
                if (!validFocusGainValues.contains(focusGain)) {
                    throw IllegalArgumentException(
                        "Valid values for focus requests are AudioManager.AUDIOFOCUS_GAIN, " +
                            "AudioManager.AUDIOFOCUS_GAIN_TRANSIENT, " +
                            "AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK and " +
                            "AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE."
                    )
                }
                this.focusGain = focusGain
            }

        /**
         * Specifies which stream will be used for playing
         * Defaults to [AudioManager.STREAM_MUSIC]
         * See [AudioManager] for a list of stream types.
         */
        @RequiresApi(api = Build.VERSION_CODES.BASE)
        @Deprecated(
            message = "Deprecated in API level 26",
            replaceWith = ReplaceWith("audioAttributes()")
        )
        fun streamType(streamType: Int): Builder =
            apply {
                this.streamType = streamType
            }

        /**
         * Specifies a collections of attributes describing information about an audio stream.
         */
        @RequiresApi(api = Build.VERSION_CODES.O)
        fun audioAttributes(audioAttributes: AudioAttributes): Builder =
            apply {
                this.audioAttributes = audioAttributes
            }

        /**
         * Build the [VoiceInstructionsPlayerOptions]
         */
        fun build(): VoiceInstructionsPlayerOptions {
            return VoiceInstructionsPlayerOptions(
                focusGain = focusGain,
                streamType = streamType,
                audioAttributes = audioAttributes,
            )
        }

        private companion object {

            private val validFocusGainValues = listOf(
                AudioManager.AUDIOFOCUS_GAIN,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE
            )
        }
    }
}
