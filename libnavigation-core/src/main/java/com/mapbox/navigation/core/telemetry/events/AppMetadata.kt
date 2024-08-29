package com.mapbox.navigation.core.telemetry.events

/**
 * Custom metadata that can be used to associate app state with feedback events in the telemetry pipeline.
 */
internal data class AppMetadata(
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
)
