package com.mapbox.navigation.mapgpt.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A [Middleware] that uses coroutines. Extending the class removes the boilerplate to manage
 * coroutines inside the service.
 */
open class CoroutineMiddleware<C : MiddlewareContext> : Middleware<C> {
    protected val stateScope = MainScope()
    protected lateinit var mainScope: CoroutineScope
    protected lateinit var ioScope: CoroutineScope

    private val _middlewareContextFlow = MutableStateFlow<C?>(null)
    protected val middlewareContextFlow = _middlewareContextFlow.asStateFlow()
    protected val middlewareContext: C?
        get() { return _middlewareContextFlow.value }

    val isAttached: Boolean get() = middlewareContext != null

    override fun onAttached(middlewareContext: C) {
        mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        _middlewareContextFlow.value = middlewareContext
    }

    override fun onDetached(middlewareContext: C) {
        if (isAttached) {
            mainScope.cancel()
            ioScope.cancel()
        }
        _middlewareContextFlow.value = null
    }

    fun detach() {
        middlewareContext?.let { onDetached(it) }
    }
}
