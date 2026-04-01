package com.mapbox.navigation.voicefeedback

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.toDuration

/**
 * Represents the various states of an Automatic Speech Recognition (ASR) engine.
 *
 * This sealed interface defines a finite set of possible states that describe
 * the lifecycle and result of a speech recognition session. It is intended to be
 * used with reactive streams (e.g. [StateFlow]) to observe and respond to recognition state changes.
 */
@ExperimentalPreviewMapboxNavigationAPI
sealed interface ASRState {
    /**
     * Indicates that the ASR engine is idle and not actively listening or processing audio.
     */
    object Idle : ASRState

    /**
     * Indicates that the ASR engine is currently listening to the user's speech.
     *
     * @param text The partial or live transcription of the spoken input.
     */
    class Listening(val text: String) : ASRState {
        private val startNanos = System.nanoTime()

        /**
         * Timestamp indicating when the listening state was detected. This is used to measure the
         * durations where text is not changing.
         */
        @OptIn(ExperimentalTime::class)
        internal fun elapsedTime(): Duration {
            val elapsedNs = System.nanoTime() - startNanos
            return elapsedNs.toDuration(DurationUnit.NANOSECONDS)
        }

        /**
         * Indicates whether some other object is "equal to" this one.
         */
        override fun equals(other: Any?): Boolean = other is Listening && text == other.text

        /**
         * Returns a hash code value for the object.
         */
        override fun hashCode(): Int = text.hashCode()

        /**
         * Returns a string representation of the object.
         */
        override fun toString(): String = "ASRState.Listening(text=$text)"
    }

    /**
     * Indicates that an error has occurred during the speech recognition process.
     *
     * @param error A [Throwable] describing the cause of the failure.
     */
    class Error(val error: Throwable) : ASRState {
        /**
         * Indicates whether some other object is "equal to" this one.
         */
        override fun equals(other: Any?): Boolean = other is Error && error == other.error

        /**
         * Returns a hash code value for the object.
         */
        override fun hashCode(): Int = error.hashCode()

        /**
         * Returns a string representation of the object.
         */
        override fun toString(): String = "ASRState.Error(error=$error)"
    }

    /**
     * Indicates that the user has finished speaking and the engine is
     * now waiting for the final recognition result.
     */
    object SpeechFinishedWaitingForResult : ASRState

    /**
     * Indicates that the final recognition result is available.
     *
     * @param text The recognized speech converted into text.
     * @param feedbackType The category or type of feedback (e.g., bug report, suggestion).
     */
    class Result(
        val text: String,
        val feedbackType: String,
    ) : ASRState {
        /**
         * Indicates whether some other object is "equal to" this one.
         */
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Result) return false

            if (text != other.text) return false
            if (feedbackType != other.feedbackType) return false

            return true
        }

        /**
         * Returns a hash code value for the object.
         */
        override fun hashCode(): Int {
            var result = text.hashCode()
            result = 31 * result + feedbackType.hashCode()
            return result
        }

        /**
         * Returns a string representation of the object.
         */
        override fun toString(): String = "ASRState.Result(text=$text, feedbackType=$feedbackType)"
    }

    /**
     * Indicates that no recognizable speech was detected during the session.
     */
    object NoResult : ASRState

    /**
     * Indicates that the recognition was interrupted unexpectedly
     * (e.g., by external factors such as app lifecycle events).
     */
    object Interrupted : ASRState

    /**
     * Indicates that the recognition session was interrupted due to a timeout.
     */
    object InterruptedByTimeout : ASRState
}
