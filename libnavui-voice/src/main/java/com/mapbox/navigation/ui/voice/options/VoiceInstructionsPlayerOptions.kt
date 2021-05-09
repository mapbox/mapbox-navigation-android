package com.mapbox.navigation.ui.voice.options

import android.media.AudioAttributes
import android.media.AudioManager

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
     * Supports pre Oreo and above implementations
     */
    val streamType: Int,

    /**
     * Defines why the source is playing and controls routing, focus, and volume decisions.
     * Defaults to [AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE]
     * See [AudioAttributes] for a list of usage types.
     */
    val usage: Int,

    /**
     * Defines what source is playing (music, movie, speech, sonification, unknown).
     * Defaults to [AudioAttributes.CONTENT_TYPE_MUSIC]
     * See [AudioAttributes] for a list of content types.
     */
    val contentType: Int,

    /**
     * Specifies attributes as inferred from the legacy stream types.
     * Defaults to False
     * Warning: When this value is true any other attributes such as
     * usage, content type, flags or haptic control will ignore.
     */
    val useLegacyApi: Boolean,
) {

    /**
     * Get a builder to customize a subset of current options.
     */
    fun toBuilder(): Builder = Builder().apply {
        focusGain(focusGain)
        streamType(streamType)
        usage(usage)
        contentType(contentType)
        useLegacyApi(useLegacyApi)
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
        if (usage != other.usage) return false
        if (contentType != other.contentType) return false
        if (useLegacyApi != other.useLegacyApi) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = focusGain
        result = 31 * result + streamType
        result = 31 * result + usage
        result = 31 * result + contentType
        result = 31 * result + useLegacyApi.hashCode()
        return result
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun toString(): String {
        return "VoiceInstructionsPlayerOptions(focusGain=$focusGain, " +
            "streamType=$streamType, " +
            "usage=$usage, " +
            "contentType=$contentType, " +
            "useLegacyApi=$useLegacyApi)"
    }

    /**
     * Build a new [VoiceInstructionsPlayerOptions]
     */
    class Builder {

        private var focusGain: Int = AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
        private var streamType: Int = AudioManager.STREAM_MUSIC
        private var usage: Int = AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE
        private var contentType: Int = AudioAttributes.CONTENT_TYPE_MUSIC
        private var useLegacyApi: Boolean = false

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
         * Supports pre Oreo and above implementations
         */
        fun streamType(streamType: Int): Builder =
            apply {
                this.streamType = streamType
            }

        /**
         * Specifies which stream will be used for playing
         * Defaults to [AudioManager.STREAM_MUSIC]
         * See [AudioManager] for a list of stream types.
         * Supports pre Oreo and above implementations
         */
        fun usage(usage: Int): Builder =
            apply {
                this.usage = usage
            }

        /**
         * Specifies what source is playing (music, movie, speech, sonification, unknown).
         * Defaults to [AudioAttributes.CONTENT_TYPE_MUSIC]
         * See [AudioAttributes] for a list of content types.
         */
        fun contentType(contentType: Int): Builder =
            apply {
                this.contentType = contentType
            }

        /**
         * Specifies attributes as inferred from the legacy stream types.
         * Defaults to false
         * Warning: When this value is true any other attributes such as
         * usage, content type, flags or haptic control will ignore
         */
        fun useLegacyApi(useLegacyApi: Boolean): Builder =
            apply {
                this.useLegacyApi = useLegacyApi
            }

        /**
         * Build the [VoiceInstructionsPlayerOptions]
         */
        fun build(): VoiceInstructionsPlayerOptions {
            return VoiceInstructionsPlayerOptions(
                focusGain = focusGain,
                streamType = streamType,
                usage = usage,
                contentType = contentType,
                useLegacyApi = useLegacyApi,
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
