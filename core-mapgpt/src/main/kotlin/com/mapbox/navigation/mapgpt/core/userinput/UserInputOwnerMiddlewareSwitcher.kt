package com.mapbox.navigation.mapgpt.core.userinput

import com.mapbox.navigation.mapgpt.core.MiddlewareSwitcher
import com.mapbox.navigation.mapgpt.core.audiofocus.AudioFocusOwner
import com.mapbox.navigation.mapgpt.core.common.SharedLog
import com.mapbox.navigation.mapgpt.core.language.Language
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

internal class UserInputOwnerMiddlewareSwitcher constructor(
    default: UserInputOwnerMiddleware,
) : MiddlewareSwitcher<UserInputMiddlewareContext, UserInputOwnerMiddleware>(default),
    UserInputOwner {

    private val _state = MutableStateFlow<UserInputState>(UserInputState.Idle)
    override val state = _state

    private val _availableLanguages = MutableStateFlow(default.availableLanguages.value)
    override val availableLanguages: StateFlow<Set<Language>> = _availableLanguages

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun onAttached(middlewareContext: UserInputMiddlewareContext) {
        super.onAttached(middlewareContext)
        middlewareState.flatMapLatest { it.state }.onEach { userInputState ->
            _state.value = userInputState
        }.launchIn(mainScope)
        middlewareState.flatMapLatest { it.availableLanguages }.onEach { availableLanguages ->
            _availableLanguages.value = availableLanguages
        }.launchIn(mainScope)
    }

    override fun onDetached(middlewareContext: UserInputMiddlewareContext) {
        super.onDetached(middlewareContext)
        _state.value = UserInputState.Idle
    }

    override fun startListening() {
        SharedLog.d(TAG) { "startListening request AudioFocus" }
        middlewareContext?.audioFocusManager?.request(AudioFocusOwner.SpeechToText) { granted ->
            if (granted) {
                SharedLog.d(TAG) { "audioFocusCallback granted" }
                middlewareState.value.startListening()
            } else {
                SharedLog.e(TAG) { "Failed to request AudioFocus" }
            }
        }
    }

    override fun stopListening() {
        SharedLog.d(TAG) { "stopListening request AudioFocus" }
        middlewareContext?.audioFocusManager?.abandon(AudioFocusOwner.SpeechToText) { granted ->
            if (granted) {
                SharedLog.d(TAG) { "abandon AudioFocus granted" }
            } else {
                SharedLog.e(TAG) { "Failed to abandon AudioFocus" }
            }
        }
        middlewareState.value.stopListening()
    }

    private companion object {
        private const val TAG = "UserInputOwnerMiddlewareSwitcher"
    }
}
