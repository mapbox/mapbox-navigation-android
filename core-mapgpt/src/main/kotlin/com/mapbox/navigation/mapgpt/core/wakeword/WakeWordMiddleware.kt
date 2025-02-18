package com.mapbox.navigation.mapgpt.core.wakeword

import com.mapbox.navigation.mapgpt.core.Middleware
import com.mapbox.navigation.mapgpt.core.MapGptCoreContext
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface for integrating a custom wake word detection system into the MapGPT SDK.
 *
 * "Wake word" is a specific word or phrase used to activate voice commands.
 * Popular examples include "Hey Google", "Hey Siri", and "Alexa".
 * The MapGPT SDK allows for wake word customization to suit specific application needs.
 * Implement this interface to integrate your own wake word detection system with the SDK,
 * or use a pre-built option provided by the SDK.
 *
 * Implementers of this interface should provide the logic for wake word detection allowing the SDK
 * to start and stop listening for wake words based on application state and user input.
 */
interface WakeWordMiddleware : Middleware<MapGptCoreContext> {
    /**
     * Provides the identifier for the wake word middleware service.
     */
    val provider: WakeWordProvider

    /**
     * The current state of the wake word system.
     */
    val state: StateFlow<WakeWordState>

    /**
     * Start listening for wake words. This function can be called after the [state] is
     * [WakeWordState.Connected].
     */
    fun startListening()

    /**
     * Stop listening for wake words. This function can be called while the [state] is
     * [WakeWordState.Listening].
     */
    fun stopListening()
}
