package com.mapbox.navigation.voicefeedback.internal

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * Connection state with the ASR service.
 *
 * There can only be one connected session at a time.
 */
@ExperimentalPreviewMapboxNavigationAPI
internal sealed class AsrSessionState {

    /**
     * Represents a state where ASR is connected and can be interacted with.
     *
     * @param apiHost The host that the service is currently connected to.
     * @param sessionId Unique ID of the current session.
     * The ID can be stored and used with ASR to reconnect to a previously established session.
     */
    data class Connected(
        val apiHost: String,
        val sessionId: String,
    ) : AsrSessionState()

    /**
     * Represents a state where ASR is connecting.
     *
     * @param apiHost The host that the service is connecting to.
     * @param reconnectSessionId Unique ID of the session the service is trying to connect to.
     * If `null`, a new ID will be generated.
     */
    data class Connecting(
        val apiHost: String,
        val reconnectSessionId: String?,
    ) : AsrSessionState()

    /**
     * Represents a state where ASR is disconnected.
     */
    object Disconnected : AsrSessionState()
}
