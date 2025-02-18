package com.mapbox.navigation.mapgpt.core.wakeword

import com.mapbox.navigation.mapgpt.core.MapGptCoreContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * A [WakeWordMiddleware] that does not provide any wake word functionality.
 */
object NoWakeWordMiddleware : WakeWordMiddleware {
    override val provider: WakeWordProvider = WakeWordProvider.None
    override val state: StateFlow<WakeWordState> = MutableStateFlow(WakeWordState.Disconnected)
    override fun onAttached(middlewareContext: MapGptCoreContext) = Unit
    override fun onDetached(middlewareContext: MapGptCoreContext) = Unit
    override fun startListening() = Unit
    override fun stopListening() = Unit
}
