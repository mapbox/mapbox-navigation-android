package com.mapbox.navigation.base.middleware

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
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
@ExperimentalPreviewMapboxNavigationAPI
open class CoroutineMiddleware<C : MiddlewareContext> : Middleware<C> {

    protected val stateScope = MainScope()
    protected lateinit var mainScope: CoroutineScope
    protected lateinit var ioScope: CoroutineScope

    private val _middlewareContextFlow = MutableStateFlow<C?>(null)
    protected val middlewareContextFlow = _middlewareContextFlow.asStateFlow()
    protected val middlewareContext: C?
        get() {
            return _middlewareContextFlow.value
        }

    /**
     * Returns current state of middleware, true if already attached, false otherwise
     */
    val isAttached: Boolean get() = middlewareContext != null

    /**
     * Called when the middleware is attached to the given context.
     *
     * This method is triggered when the middleware becomes active within the application lifecycle.
     * It allows for initialization, event listeners setup, or other context-related preparations.
     *
     * @param middlewareContext The context to which the middleware is being attached
     */
    override fun onAttached(middlewareContext: C) {
        mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        _middlewareContextFlow.value = middlewareContext
    }

    /**
     * Called when the middleware is detached from the given context.
     *
     * This method is triggered when the middleware is removed or no longer needed.
     * It should be used to clean up resources, remove listeners, or reset states to avoid memory leaks.
     *
     * @param middlewareContext The context from which the middleware is being detached
     */
    override fun onDetached(middlewareContext: C) {
        if (isAttached) {
            mainScope.cancel()
            ioScope.cancel()
        }
        _middlewareContextFlow.value = null
    }

    /**
     * Function called to detach middleware context if it exists
     */
    fun detach() {
        middlewareContext?.let { onDetached(it) }
    }
}
