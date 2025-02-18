package com.mapbox.navigation.mapgpt.core.api

/**
 * Connection state with the MapGPT service.
 *
 * There can only be one connected session at a time.
 */
sealed class SessionState {

    /**
     * Represents a state where [MapGptService] is connected and can be interacted with.
     *
     * @param apiHost The host that the service is currently connected to.
     * @param sessionId Unique ID of the current session.
     * The ID can be stored and used with [MapGptService.connect] to reconnect to a previously established session.
     */
    data class Connected(
        val apiHost: String,
        val sessionId: String,
    ) : SessionState()

    /**
     * Represents a state where [MapGptService] is connecting.
     *
     * @param apiHost The host that the service is connecting to.
     * @param reconnectSessionId Unique ID of the session the service is trying to connect to.
     * If `null`, a new ID will be generated.
     */
    data class Connecting(
        val apiHost: String,
        val reconnectSessionId: String?,
    ) : SessionState()

    /**
     * Represents a state where [MapGptService] is disconnected.
     */
    object Disconnected : SessionState() {
        override fun toString(): String = "Disconnected"
    }
}
