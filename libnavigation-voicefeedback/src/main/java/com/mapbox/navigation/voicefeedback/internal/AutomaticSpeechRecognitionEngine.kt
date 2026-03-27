package com.mapbox.navigation.voicefeedback.internal

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.voicefeedback.ASRState
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface representing an automatic speech recognition (ASR) engine.
 *
 * This interface provides a contract for components that perform speech recognition
 * and expose their current recognition state as a reactive stream.
 *
 * Implementations are expected to manage the lifecycle of audio listening
 * and recognition, including starting and stopping the recognition process.
 */
@ExperimentalPreviewMapboxNavigationAPI
internal interface AutomaticSpeechRecognitionEngine {
    /**
     * A [StateFlow] that emits updates about the current state of the ASR engine.
     *
     * Observers can collect this flow to reactively respond to changes in
     * the recognition process, such as when it starts listening, detects speech,
     * processes results, or encounters errors. A null value means that the engine is not
     * connected.
     */
    val state: StateFlow<ASRState?>

    /**
     * Starts the speech recognition process.
     *
     * This typically activates the microphone and begins listening for audio input.
     * Recognition results and state updates should be reflected through [state].
     */
    fun startListening()

    /**
     * Stops the speech recognition process.
     *
     * This typically deactivates the microphone and finalizes any ongoing recognition.
     * After calling this, the ASR engine should transition to an idle or stopped state.
     */
    fun stopListening()

    fun connect(token: String)

    fun disconnect()

    /**
     * Triggered when the user terminate conversation manually.
     */
    fun interruptListening()
}
