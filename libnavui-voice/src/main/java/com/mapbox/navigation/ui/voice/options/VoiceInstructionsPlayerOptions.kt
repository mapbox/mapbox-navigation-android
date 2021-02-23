package com.mapbox.navigation.ui.voice.options

import android.media.AudioManager

/**
 * VoiceInstructionsPlayerOptions.
 *
 * @param focusGain specifies how audio focus will be requested.
 * Valid values for focus requests are
 * [AudioManager.AUDIOFOCUS_GAIN], [AudioManager.AUDIOFOCUS_GAIN_TRANSIENT],
 * [AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK] and
 * [AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE].
 * [AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK] is used by default.
 */
class VoiceInstructionsPlayerOptions private constructor(
    val focusGain: Int
) {

    /**
     * Get a builder to customize a subset of current options.
     */
    fun toBuilder(): Builder = Builder().apply {
        focusGain(focusGain)
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VoiceInstructionsPlayerOptions

        if (focusGain != other.focusGain) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        return focusGain.hashCode()
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "VoiceInstructionsPlayerOptions(" +
            "focusGain=$focusGain" +
            ")"
    }

    /**
     * Build a new [VoiceInstructionsPlayerOptions]
     */
    class Builder {

        private var focusGain: Int = AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK

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
         * Build the [VoiceInstructionsPlayerOptions]
         */
        fun build(): VoiceInstructionsPlayerOptions {
            return VoiceInstructionsPlayerOptions(
                focusGain = focusGain
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
