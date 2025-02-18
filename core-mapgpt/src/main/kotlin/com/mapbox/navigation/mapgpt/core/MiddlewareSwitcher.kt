package com.mapbox.navigation.mapgpt.core

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
abstract class MiddlewareSwitcher<C : MiddlewareContext, M : Middleware<C>>(
    private val default: M,
) : CoroutineMiddleware<C>() {
    private val _middlewareState = MutableStateFlow(default)
    val middlewareState: StateFlow<M> = _middlewareState.asStateFlow()

    override fun onAttached(middlewareContext: C) {
        super.onAttached(middlewareContext)
        middlewareState.value.onAttached(middlewareContext)
    }

    override fun onDetached(middlewareContext: C) {
        super.onDetached(middlewareContext)
        middlewareState.value.onDetached(middlewareContext)
    }

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

    fun <S> stateFlowOf(
        started: SharingStarted = SharingStarted.Eagerly,
        func: M.() -> S,
    ): StateFlow<S> {
        val default = middlewareState.value.func()
        return middlewareState.map { it.func() }.stateIn(stateScope, started, default)
    }
}
