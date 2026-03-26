package com.mapbox.navigation.voicefeedback

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.voicefeedback.internal.audio.microphone.AudioRecordMicrophone
import java.util.Locale

/**
 * Options for configuring the Feedback Agent.
 *
 * @param language The assigned language for user input.
 * @param endpoint Environment configuration.
 */
@ExperimentalPreviewMapboxNavigationAPI
class FeedbackAgentOptions private constructor(
    val language: Locale,
    val endpoint: FeedbackAgentEndpoint,
    internal val microphone: Microphone = AudioRecordMicrophone(),
) {
    /**
     * Builder for creating a new instance of [FeedbackAgentOptions].
     */
    class Builder {
        private var language: Locale = Locale.getDefault()

        private var endpoint: FeedbackAgentEndpoint = FeedbackAgentEndpoint.Production

        private var microphone: Microphone = AudioRecordMicrophone()

        /**
         * @param language The assigned language for user input. Default is the device's locale.
         */
        fun language(language: Locale): Builder = apply { this.language = language }

        /**
         * @param endpoint Environment configuration. Default is production.
         */
        fun endpoint(endpoint: FeedbackAgentEndpoint): Builder = apply { this.endpoint = endpoint }

        /**
         * @param microphone A custom audio source input.
         */
        fun microphone(microphone: Microphone): Builder =
            apply { this.microphone = microphone }

        /**
         * Build the [FeedbackAgentOptions].
         */
        fun build() = FeedbackAgentOptions(language, endpoint, microphone)
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FeedbackAgentOptions

        if (language != other.language) return false
        if (endpoint != other.endpoint) return false
        if (microphone != other.microphone) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = language.hashCode()
        result = 31 * result + endpoint.hashCode()
        result = 31 * result + microphone.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "FeedbackAgentOptions(endpoint=$endpoint, language=$language)"
    }
}
