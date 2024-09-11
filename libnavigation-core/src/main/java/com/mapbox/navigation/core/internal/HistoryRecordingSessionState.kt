package com.mapbox.navigation.core.internal

import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.history.MapboxHistoryRecorder

/**
 * Contains the various states that can occur during a navigation and be used for history recording.
 *
 * The [MapboxNavigation] implementation can enter into the following session states:
 * - [Idle]
 * - [FreeDrive]
 * - [ActiveGuidance]
 *
 * The SDK starts off in an [Idle] state.
 * Whenever the [MapboxNavigation.startTripSession] is called,
 * the SDK will enter the [FreeDrive] state.
 * If the session is stopped, the SDK will enter the [Idle] state.
 * If the SDK is in an [Idle] state, it stays in this same state
 * even when a primary route is available.
 * If the SDK is already in the [FreeDrive] mode or entering it,
 * whenever a primary route is being set the SDK will enter the [ActiveGuidance] mode instead.
 * Here 2 options are possible:
 * 1) Routes were set successfully: then the [ActiveGuidance] session will continue;
 * 2) Routes were rejected: then the SDK will transition back to [FreeDrive].
 * When the routes are manually cleared, the SDK automatically fall back
 * to either [Idle] or [FreeDrive] state.
 * When transitioning across states of a history recording session
 * the [sessionId] will change (empty when [Idle]).
 */
sealed class HistoryRecordingSessionState {

    /**
     * Random session UUID.
     * This is generated internally based on the current state within a history recording session.
     * I.e. will change when transitioning across states of a trip session. Empty when [Idle].
     *
     * Useful to use it in combination with the [MapboxHistoryRecorder].
     */
    abstract val sessionId: String

    /**
     * Idle state
     */
    object Idle : HistoryRecordingSessionState() {
        override val sessionId = ""
    }

    /**
     * Free Drive state
     */
    data class FreeDrive internal constructor(
        override val sessionId: String,
    ) : HistoryRecordingSessionState()

    /**
     * Active Guidance state
     */
    data class ActiveGuidance internal constructor(
        override val sessionId: String,
    ) : HistoryRecordingSessionState()
}
