package com.mapbox.navigation.mapgpt.core.userinput

import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource

/**
 * Captures different phases of the user input lifecycle.
 *
 * @see [UserInputOwner]
 */
sealed class UserInputState {

    override fun toString(): String = this::class.simpleName ?: ""

    /**
     * State representing that the system is not expecting any user input.
     */
    object Idle : UserInputState()

    /**
     * State representing that the system is actively listening for user input.
     *
     * @param text Current text that has been detected. Null if the no words have been detected.
     */
    data class Listening(val text: String?) : UserInputState() {
        /**
         * Timestamp indicating when the listening state was detected. This is used to measure the
         * durations where text is not changing.
         */
        @OptIn(ExperimentalTime::class)
        val timeMark: TimeSource.Monotonic.ValueTimeMark = TimeSource.Monotonic.markNow()

        override fun toString(): String = "${super.toString()}(text=$text)"
    }

    /**
     * State representing an error condition that prevents the user from providing input.
     * @param reason Description of the error encountered for debugging purposes.
     */
    data class Error(val reason: String) : UserInputState() {

        override fun toString(): String = "${super.toString()}(reason=$reason)"
    }

    /**
     * State representing the final result of the user input after they have finished providing it.
     * @param text The final text that was provided by the user. Must not be blank.
     * @param conversationProcessed flag indicates that result already processed by conversation
     * API and no need to duplicate call conversation API with user input text
     *
     * @throws [IllegalArgumentException] if [text] is blank.
     */
    data class Result(
        val text: String,
        val conversationProcessed: Boolean = false,
    ) : UserInputState() {
        init {
            require(text.isNotBlank()) { "Text must not be blank" }
        }

        override fun toString(): String {
            return "Result(text='$text', conversationProcessed=$conversationProcessed)"
        }
    }
}
