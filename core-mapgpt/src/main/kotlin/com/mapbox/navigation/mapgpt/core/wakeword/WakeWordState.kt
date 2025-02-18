package com.mapbox.navigation.mapgpt.core.wakeword

import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource

/**
 * Represents the states of the [WakeWordMiddleware].
 */
abstract class WakeWordState {
    /**
     * Indicates the wake word system is not ready or disconnected.
     * [WakeWordMiddleware.startListening] not work and trigger an [Error] state.
     */
    object Disconnected : WakeWordState() {
        override fun toString(): String = "Disconnected"
    }

    /**
     * Indicates the wake word system is ready.
     * [WakeWordMiddleware.startListening] is possible, potentially moving to the [Listening] state.
     */
    object Connected : WakeWordState() {
        override fun toString(): String = "Connected"
    }

    /**
     * The system is actively listening for wake words.
     * [WakeWordMiddleware.stopListening] returns to [Connected];
     * [WakeWordMiddleware.onDetached] moves to [Disconnected].
     */
    object Listening : WakeWordState() {
        override fun toString(): String = "Listening"
    }

    /**
     * A wake word has been detected, providing the time of detection and the associated action.
     *
     * @param action The specified action upon detection.
     */
    class Detected(val action: WakeWordAction) : WakeWordState() {

        /**
         * Monotonic time that makes every detection unequal. Note the time basis is undefined,
         * but it's guaranteed to be monotonic.
         */
        @OptIn(ExperimentalTime::class)
        val realtimeMillis: Long = firstTimeMark.elapsedNow().inWholeMilliseconds

        override fun toString(): String {
            return "Detected(action=$action, realtimeMillis=$realtimeMillis)"
        }
    }

    /**
     * Represents an error state where the system can't proceed to [Connected] or [Listening]
     * states.
     *
     * @param reason Human readable description of the error causing this state.
     */
    class Error(val reason: String) : WakeWordState() {
        override fun toString(): String = "Error(reason='$reason')"
    }

    companion object {
        @OptIn(ExperimentalTime::class)
        val firstTimeMark = TimeSource.Monotonic.markNow()
    }
}
