package com.mapbox.navigation.mapgpt.core.userinput

import com.mapbox.navigation.mapgpt.core.language.Language
import kotlinx.coroutines.flow.StateFlow

/**
 * This middleware can be used to inject new systems for Speech To Text recognition. The Dash SDK
 * will call [startListening] and it is expected that the middleware will update the [state] to
 * [UserInputState.Listening] and then [UserInputState.Result] when the user is done speaking.
 * When there is an error encountered, the state can be updated to [UserInputState.Error].
 */
interface UserInputOwner {
    /**
     * Gives access to the state of the user input. When implementing, make sure to update the
     * state when the user input changes.
     */
    val state: StateFlow<UserInputState>

    /**
     * Provides the available languages for the user input. This is used to determine which
     * languages are available for the user to speak in.
     */
    val availableLanguages: StateFlow<Set<Language>>

    /**
     * Triggered when the user is expected to start speaking. Once called the state should be
     * updated to [UserInputState.Listening], and any new words should be updated in the state.
     * When the user is done speaking, the state should be updated to [UserInputState.Result].
     */
    fun startListening()

    /**
     * Triggered when the user input is expected to stop or be canceled. If the user mic is open
     * and listening, it should be closed and the state should be updated to [UserInputState.Idle].
     */
    fun stopListening()
}
