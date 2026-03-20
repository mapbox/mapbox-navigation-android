package com.mapbox.navigation.voicefeedback.internal.audio.microphone

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import kotlinx.coroutines.flow.StateFlow

/**
 * [Microphone] interface allows the platform to define a source for audio.
 * The microphone can be used to stream audio data from the platform to the Mapbox SDK.
 */
@ExperimentalPreviewMapboxNavigationAPI
internal interface Microphone : MapboxNavigationObserver {

    /**
     * Configuration setting for the microphone.
     */
    val config: Config

    /**
     * The current state of the microphone. Implementations should update this as the microphone
     * changes state. Example:
     *
     * ```
     * val state = MutableStateFlow<State>(State.Disconnected)
     * override val state: StateFlow<State> = state
     * ```
     */
    val state: StateFlow<State>

    /**
     * Starts streaming audio from the microphone. The audio is streamed in chunks and surfaced
     * through the [consumer] lambda as well as the [state] flow. The consumer lambda will use
     * [State.Streaming] to process the audio data. The [state] flow can be used to share the
     * streaming bytes for multiple uses.
     *
     * The stream will block while streaming. The stream will stop when [stop] is called, or when
     * some event causes the stream to stop streaming.
     */
    suspend fun stream(consumer: (State.Streaming) -> Unit)

    /**
     * Stops streaming audio from the microphone and causes [stream] to exit.
     * The [state] flow will move to [State.Idle].
     */
    fun stop()

    /**
     * Defines the specific details from the microphone.
     */
    data class Config(
        /**
         * The number of audio samples per second.
         * Many speech-to-text services perform best with 16000 Hz audio.
         */
        val sampleRateHz: Int = 16000,
    )

    /**
     * Represent current microphone state
     */
    sealed class State {

        /**
         * Before the MicrophoneMiddleware has been attached it is in the disconnected state.
         * This state is used to indicate that the microphone is not available.
         */
        object Disconnected : State() {

            /**
             * @return a string representation of the object.
             */
            override fun toString(): String = "Disconnected"
        }

        /**
         * The microphone is not currently available to stream audio.
         */
        object Idle : State() {

            /**
             * @return a string representation of the object.
             */
            override fun toString(): String = "Idle"
        }

        /**
         * The microphone is currently streaming audio. Collectively, the streaming states
         * can be concatenated to form an audio file. The consumer of the audio data is able to
         * process this audio data in real-time.
         *
         * @param chunkId unique number for each chunk of audio.
         * @param byteArray An array containing the audio data.
         * @param bytesRead the number of bytes the buffer contains of unique audio.
         */
        class Streaming(
            val chunkId: Int,
            val byteArray: ByteArray,
            val bytesRead: Int,
        ) : State() {

            /**
             * @return a string representation of the object.
             */
            override fun toString(): String = "Streaming(chunkId=$chunkId, bytesRead=$bytesRead)"
        }

        /**
         * The microphone has encountered a recoverable error that should be surfaced to the user.
         * The error state is used to give the user an opportunity to retry actions with the
         * microphone.
         *
         * @param reason debuggable reason for error. Is not shown to the user.
         */
        class Error(val reason: String) : State() {

            /**
             * @return a string representation of the object.
             */
            override fun toString(): String = "Error(reason=$reason)"
        }
    }
}
