package com.mapbox.navigation.mapgpt.core.wakeword

import com.mapbox.navigation.mapgpt.core.MiddlewareSwitcher
import com.mapbox.navigation.mapgpt.core.MapGptCoreContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class WakeWordMiddlewareSwitcher(
    default: WakeWordMiddleware,
) : MiddlewareSwitcher<MapGptCoreContext, WakeWordMiddleware>(default),
    WakeWordMiddleware {

    private val _state = MutableStateFlow<WakeWordState>(WakeWordState.Disconnected)

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun onAttached(middlewareContext: MapGptCoreContext) {
        super.onAttached(middlewareContext)
        middlewareState.flatMapLatest { it.state }.onEach { state ->
            _state.value = state
        }.launchIn(mainScope)
    }

    override fun onDetached(middlewareContext: MapGptCoreContext) {
        super.onDetached(middlewareContext)
        _state.value = WakeWordState.Disconnected
    }

    override val provider: WakeWordProvider
        get() = middlewareState.value.provider

    override val state: StateFlow<WakeWordState> = _state
    override fun startListening() {
        middlewareState.value.startListening()
    }

    override fun stopListening() {
        middlewareState.value.stopListening()
    }
}
