package com.mapbox.navigation.core.telemetry.events

/**
 * Custom metadata that can be used to associate app state with feedback events in the telemetry pipeline.
 */
data class AppMetadata constructor(
    /**
     * Name of the application. Value should be non-empty.
     */
    val name: String, // min length 1

    /**
     * Version of the application. Value should be non-empty.
     */
    val version: String, // min length 1

    /**
     * User ID relevant for the application context.
     */
    val userId: String?,

    /**
     * Session ID relevant for the application context.
     */
    val sessionId: String?
) {

    /**
     * Get a builder to customize a subset of current options.
     */
    fun toBuilder() = Builder(name, version).apply {
        userId(userId)
        sessionId(sessionId)
    }

    /**
     * [AppMetadata] builder.
     */
    class Builder(
        /**
         * Name of the application. Value should be non-empty.
         */
        private val name: String, // min length 1

        /**
         * Version of the application. Value should be non-empty.
         */
        private val version: String // min length 1
    ) {
        private var userId: String? = null
        private var sessionId: String? = null

        /**
         * User ID relevant for the application context.
         */
        fun userId(userId: String?) = apply {
            this.userId = userId
        }

        /**
         * Session ID relevant for the application context.
         */
        fun sessionId(sessionId: String?) = apply {
            this.sessionId = sessionId
        }

        /**
         * Builds new [AppMetadata] instance.
         */
        fun build() = AppMetadata(
            name = name,
            version = version,
            userId = userId,
            sessionId = sessionId
        )
    }
}
