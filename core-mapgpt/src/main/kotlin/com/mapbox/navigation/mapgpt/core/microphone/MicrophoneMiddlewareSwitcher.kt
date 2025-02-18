package com.mapbox.navigation.mapgpt.core.microphone

import com.mapbox.navigation.mapgpt.core.MiddlewareSwitcher
import com.mapbox.navigation.mapgpt.core.MapGptCoreContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Gives the ability to switch between different microphone implementations at runtime.
 * When using the [PlatformMicrophone] from application code, the [MicrophoneMiddlewareSwitcher]
 * should be used instead of an individual [PlatformMicrophone] implementation.
 *
 * @param default The default microphone implementation to use.
 */
internal class MicrophoneMiddlewareSwitcher(
    default: PlatformMicrophoneMiddleware,
) : MiddlewareSwitcher<MapGptCoreContext, PlatformMicrophoneMiddleware>(default),
    PlatformMicrophone {

    override val provider: MicrophoneProvider get() = middlewareState.value.provider
    override val config: PlatformMicrophone.Config get() = _config.value
    private val _state = MutableStateFlow<PlatformMicrophone.State>(
        PlatformMicrophone.State.Disconnected,
    )
    override val state: StateFlow<PlatformMicrophone.State> = _state

    private val _config = stateFlowOf { config }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun onAttached(middlewareContext: MapGptCoreContext) {
        super.onAttached(middlewareContext)
        middlewareState.flatMapLatest { it.state }.onEach { state ->
            _state.value = state
        }.launchIn(mainScope)
    }

    override fun onDetached(middlewareContext: MapGptCoreContext) {
        super.onDetached(middlewareContext)
        middlewareState.value.stop()
        _state.value = PlatformMicrophone.State.Disconnected
    }

    override fun hasPermission(): Boolean {
        return middlewareState.value.hasPermission()
    }

    override suspend fun stream(consumer: (PlatformMicrophone.State.Streaming) -> Unit) {
        middlewareState.value.stream(consumer)
    }

    override fun stop() {
        middlewareState.value.stop()
    }
}
