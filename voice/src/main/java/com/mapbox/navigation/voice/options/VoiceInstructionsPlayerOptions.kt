package com.mapbox.navigation.voice.options

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
     * Defines which stream will be used for playing TTS
     * Defaults to [AudioManager.STREAM_MUSIC]
     * See [AudioManager] for a list of stream types.
     * Supports pre Oreo and above implementations
     */
    val ttsStreamType: Int,

    /**
     * Defines the context in which the stream is used, providing information about
     * why the sound is playing and what the sound is used for.
     * Usage information is more expressive than a stream type and allows platforms
     * or routing policies to refine volume or routing decisions
     * Defaults to [AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE]
     * @see <a href="https://source.android.com/devices/audio/attributes#using">Using attributes</a>
     */
    val usage: Int,

    /**
     * Defines what the sound is and expresses the general category of the content such as movie,
     * speech, or beep/ringtone. The audio framework uses content type information to selectively
     * configure audio post-processing blocks. While supplying the content type is optional,
     * you should include type information whenever the content type is known
     * Defaults to [AudioAttributes.CONTENT_TYPE_MUSIC]
     * @see <a href="https://source.android.com/devices/audio/attributes#content-type">Content type</a>
     */
    val contentType: Int,

    /**
     * Specifies attributes as inferred from the legacy stream types.
     * Defaults to False
     * Warning: When this value is true any other attributes such as
     * usage, content type, flags or haptic control will be ignored.
     * @see <a href="https://developer.android.com/reference/android/media/AudioAttributes.Builder#setLegacyStreamType(int)">Legacy stream type documentation</a>
     */
    val useLegacyApi: Boolean,

    /**
     * Checks if the specified language as represented by the Locale is available and supported by TTS.
     * Defaults to True
     * @see <a href="https://developer.android.com/reference/android/speech/tts/TextToSpeech#isLanguageAvailable(java.util.Locale)">TextToSpeech documentation</a>
     */
    val checkIsLanguageAvailable: Boolean,

    /**
     * Delay in milliseconds until the player abandons audio focus after playing all queued voice instructions.
     * Defaults to 0.
     */
    val abandonFocusDelay: Long,
) {

    /**
     * Get a builder to customize a subset of current options.
     */
    fun toBuilder(): Builder = Builder().apply {
        focusGain(focusGain)
        streamType(streamType)
        ttsStreamType(ttsStreamType)
        usage(usage)
        contentType(contentType)
        useLegacyApi(useLegacyApi)
        checkIsLanguageAvailable(checkIsLanguageAvailable)
        abandonFocusDelay(abandonFocusDelay)
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
        if (ttsStreamType != other.ttsStreamType) return false
        if (usage != other.usage) return false
        if (contentType != other.contentType) return false
        if (useLegacyApi != other.useLegacyApi) return false
        if (checkIsLanguageAvailable != other.checkIsLanguageAvailable) return false
        if (abandonFocusDelay != other.abandonFocusDelay) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = focusGain
        result = 31 * result + streamType
        result = 31 * result + ttsStreamType
        result = 31 * result + usage
        result = 31 * result + contentType
        result = 31 * result + useLegacyApi.hashCode()
        result = 31 * result + checkIsLanguageAvailable.hashCode()
        result = 31 * result + abandonFocusDelay.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "VoiceInstructionsPlayerOptions(focusGain=$focusGain, " +
            "streamType=$streamType, " +
            "ttsStreamType=$ttsStreamType, " +
            "usage=$usage, " +
            "contentType=$contentType, " +
            "useLegacyApi=$useLegacyApi, " +
            "checkIsLanguageAvailable=$checkIsLanguageAvailable, " +
            "abandonFocusDelay=$abandonFocusDelay)"
    }

    /**
     * Build a new [VoiceInstructionsPlayerOptions]
     */
    class Builder {

        private var focusGain: Int = AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
        private var streamType: Int = AudioManager.STREAM_MUSIC
        private var ttsStreamType: Int = AudioManager.STREAM_MUSIC
        private var usage: Int = AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE
        private var contentType: Int = AudioAttributes.CONTENT_TYPE_SPEECH
        private var useLegacyApi: Boolean = false
        private var checkIsLanguageAvailable: Boolean = true
        private var abandonFocusDelay: Long = 0L

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
                            "AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE.",
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
         * Specifies which stream will be used for playing TTS
         * Defaults to [AudioManager.STREAM_MUSIC]
         * See [AudioManager] for a list of stream types.
         * Supports pre Oreo and above implementations
         */
        fun ttsStreamType(streamType: Int): Builder =
            apply {
                this.ttsStreamType = streamType
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
         * Defaults to False
         * Warning: When this value is true any other attributes such as
         * usage, content type, flags or haptic control will be ignored
         */
        fun useLegacyApi(useLegacyApi: Boolean): Builder =
            apply {
                this.useLegacyApi = useLegacyApi
            }

        /**
         * Checks if the specified language as represented by the Locale is available and supported by TTS.
         * Defaults to True
         */
        fun checkIsLanguageAvailable(checkIsLanguageAvailable: Boolean): Builder =
            apply {
                this.checkIsLanguageAvailable = checkIsLanguageAvailable
            }

        /**
         * Delay in milliseconds until the player abandons audio focus after playing all queued voice instructions.
         * Defaults to 0.
         */
        fun abandonFocusDelay(milliseconds: Long): Builder =
            apply {
                this.abandonFocusDelay = milliseconds
            }

        /**
         * Build the [VoiceInstructionsPlayerOptions]
         */
        fun build(): VoiceInstructionsPlayerOptions {
            return VoiceInstructionsPlayerOptions(
                focusGain = focusGain,
                streamType = streamType,
                ttsStreamType = ttsStreamType,
                usage = usage,
                contentType = contentType,
                useLegacyApi = useLegacyApi,
                checkIsLanguageAvailable = checkIsLanguageAvailable,
                abandonFocusDelay = abandonFocusDelay,
            )
        }

        private companion object {

            private val validFocusGainValues = listOf(
                AudioManager.AUDIOFOCUS_GAIN,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE,
            )
        }
    }
}
