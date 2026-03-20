package com.mapbox.navigation.voicefeedback

import androidx.annotation.RestrictTo
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * Wrapper that defines the properties of endpoints required to be used with Feedback Agent.
 *
 * @property name describes the flavor of endpoint
 * @property streamingApiHost host for streaming data
 * @property streamingAsrApiHost host for Automatic Speech Recognition
 */
@ExperimentalPreviewMapboxNavigationAPI
class FeedbackAgentEndpoint private constructor(
    val name: String,
    val streamingApiHost: String,
    val streamingAsrApiHost: String,
) {
    companion object {
        const val PRODUCTION = "production"

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        const val TESTING = "testing"

        /**
         * Standard production endpoints.
         */
        val Production = FeedbackAgentEndpoint(
            name = PRODUCTION,
            streamingApiHost = "wss://mapgpt-production-ws.mapbox.com",
            streamingAsrApiHost = "wss://api-navgptasr-production.mapbox.com",
        )

        /**
         * Testing endpoints.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        val Testing = FeedbackAgentEndpoint(
            name = TESTING,
            streamingApiHost = "wss://mapgpt-testing-ws.tilestream.net",
            streamingAsrApiHost = "wss://api-navgptasr-staging.tilestream.net",
        )

        /**
         * Custom endpoint type.
         *
         * @param name describes the kind of endpoint
         * @param streamingApiHost api host to stream to
         * @param streamingAsrApiHost api host for Automatic Speech Recognition
         */
        fun custom(name: String, streamingApiHost: String, streamingAsrApiHost: String) =
            FeedbackAgentEndpoint(name, streamingApiHost, streamingAsrApiHost)
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "FeedbackAgentEndpoint(" +
            "name=$name, " +
            "streamingApiHost=$streamingApiHost," +
            "streamingAsrApiHost=$streamingAsrApiHost)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FeedbackAgentEndpoint

        if (name != other.name) return false
        if (streamingApiHost != other.streamingApiHost) return false
        if (streamingAsrApiHost != other.streamingAsrApiHost) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + streamingApiHost.hashCode()
        result = 31 * result + streamingAsrApiHost.hashCode()
        return result
    }
}
