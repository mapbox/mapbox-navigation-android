package com.mapbox.navigation.base.middleware

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * A [Middleware] that can switch between different [Middleware] instances.
 *
 * @param default The default [Middleware] implementation.
 */
@ExperimentalPreviewMapboxNavigationAPI
abstract class MiddlewareSwitcher<C : MiddlewareContext, M : Middleware<C>>(
    private val default: M,
) : CoroutineMiddleware<C>() {

    private val _middlewareState = MutableStateFlow(default)

    /**
     * Active middleware state
     */
    val middlewareState: StateFlow<M> = _middlewareState.asStateFlow()

    /**
     * @see CoroutineMiddleware.onAttached
     */
    override fun onAttached(middlewareContext: C) {
        super.onAttached(middlewareContext)
        middlewareState.value.onAttached(middlewareContext)
    }

    /**
     * @see CoroutineMiddleware.onAttached
     */
    override fun onDetached(middlewareContext: C) {
        super.onDetached(middlewareContext)
        middlewareState.value.onDetached(middlewareContext)
    }

    /**
     * Registers a new middleware instance.
     *
     * If the provided [middleware] is already registered, this function does nothing. Otherwise,
     * it detaches the currently active middleware (if any), updates the state to the new middleware,
     * and attaches it to the current context if available.
     *
     * @param middleware The middleware instance to register.
     */
    fun registerMiddleware(middleware: M) {
        if (middlewareState.value === middleware) {
            // Ignore if already registered
            return
        }
        middlewareContext?.let {
            middlewareState.value.onDetached(it)
        }
        _middlewareState.value = middleware
        middlewareContext?.let {
            middlewareState.value.onAttached(it)
        }
    }

    /**
     * Unregisters the currently active middleware and restores the default middleware.
     *
     * If the current middleware is not the default one, this function detaches the active middleware,
     * resets the middleware state to the default, and attaches the default middleware to the context.
     */
    fun unregisterMiddleware() {
        if (middlewareState.value !== default) {
            middlewareContext?.let {
                middlewareState.value.onDetached(it)
            }
            _middlewareState.value = default
            middlewareContext?.let {
                default.onAttached(it)
            }
        }
    }

    /**
     * Creates a [StateFlow] that maps the current middleware state to a derived state of type [S].
     *
     * This function allows you to observe a specific property or computation based on the current
     * middleware instance. When the middleware state changes, the resulting [StateFlow] will
     * emit updated values accordingly.
     *
     * @param S The type of the state value derived from the middleware.
     * @param started The [SharingStarted] strategy to control when the flow starts and stops collecting.
     *                Defaults to [SharingStarted.Eagerly], meaning the flow is always active.
     * @param func A lambda function that extracts the desired state from the middleware instance.
     * @return A [StateFlow] that emits the derived state from the current middleware.
     */
    fun <S> stateFlowOf(
        started: SharingStarted = SharingStarted.Eagerly,
        func: M.() -> S,
    ): StateFlow<S> {
        val default = middlewareState.value.func()
        return middlewareState.map { it.func() }.stateIn(stateScope, started, default)
    }
}
