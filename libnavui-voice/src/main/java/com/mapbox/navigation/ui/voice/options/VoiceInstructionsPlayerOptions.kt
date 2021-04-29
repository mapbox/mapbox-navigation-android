package com.mapbox.navigation.ui.voice.options

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
     * Defines how the audio system handles routing
     * and focus decisions for the specified source.
     */
    val playerAttributes: PlayerAttributes,
) {

    /**
     * Get a builder to customize a subset of current options.
     */
    fun toBuilder(): Builder = Builder().apply {
        focusGain(focusGain)
        playerAttributes(playerAttributes)
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VoiceInstructionsPlayerOptions

        if (focusGain != other.focusGain) return false
        if (playerAttributes != other.playerAttributes) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = focusGain
        result = 31 * result + playerAttributes.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "VoiceInstructionsPlayerOptions(focusGain=$focusGain, " +
            "playerAttributes=$playerAttributes)"
    }

    /**
     * Build a new [VoiceInstructionsPlayerOptions]
     */
    class Builder {

        private var focusGain: Int = AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
        private var playerAttributes: PlayerAttributes =
            PlayerAttributesProvider.retrievePlayerAttributes()

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
         * Specifies how the audio system handles routing
         * and focus decisions for the specified source.
         */
        fun playerAttributes(playerAttributes: PlayerAttributes): Builder =
            apply {
                this.playerAttributes = playerAttributes
            }

        /**
         * Build the [VoiceInstructionsPlayerOptions]
         */
        fun build(): VoiceInstructionsPlayerOptions {
            return VoiceInstructionsPlayerOptions(
                focusGain = focusGain,
                playerAttributes = playerAttributes,
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
