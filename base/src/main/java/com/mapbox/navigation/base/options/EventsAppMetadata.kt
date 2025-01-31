package com.mapbox.navigation.base.options

/**
 * Custom metadata that can be used to associate app state with events in the telemetry pipeline.
 */
class EventsAppMetadata private constructor(
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
    val sessionId: String?,
) {

    /**
     * Get a builder to customize a subset of current options.
     */
    fun toBuilder(): Builder = Builder(name, version).apply {
        userId(userId)
        sessionId(sessionId)
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EventsAppMetadata

        if (name != other.name) return false
        if (version != other.version) return false
        if (userId != other.userId) return false
        if (sessionId != other.sessionId) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + version.hashCode()
        result = 31 * result + userId.hashCode()
        result = 31 * result + sessionId.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "EventsAppMetadata(" +
            "name='$name', version='$version', userId='$userId', sessionId='$sessionId'" +
            ")"
    }

    /**
     * [EventsAppMetadata] builder.
     */
    class Builder(
        /**
         * Name of the application. Value should be non-empty.
         */
        private val name: String, // min length 1

        /**
         * Version of the application. Value should be non-empty.
         */
        private val version: String, // min length 1
    ) {

        private var userId: String? = null
        private var sessionId: String? = null

        /**
         * User ID relevant for the application context.
         */
        fun userId(userId: String?): Builder = apply {
            this.userId = userId
        }

        /**
         * Session ID relevant for the application context.
         */
        fun sessionId(sessionId: String?): Builder = apply {
            this.sessionId = sessionId
        }

        /**
         * Builds new [EventsAppMetadata] instance.
         */
        fun build(): EventsAppMetadata {
            check(name.isNotEmpty() || version.isNotEmpty()) {
                "Value should be non-empty."
            }
            return EventsAppMetadata(
                name = name,
                version = version,
                userId = userId,
                sessionId = sessionId,
            )
        }
    }
}
