package com.mapbox.navigation.base.middleware

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * A generic interface representing a middleware component that can be attached and detached.
 *
 * Middleware components provide functionality that can be dynamically activated or deactivated,
 * often handling background tasks, event listeners, or lifecycle-dependent operations.
 *
 * @param C The type of [MiddlewareContext] that this middleware interacts with.
 */
@ExperimentalPreviewMapboxNavigationAPI
interface Middleware<C : MiddlewareContext> {

    /**
     * Invoked when the middleware is active.
     * Use this method to initialize or set functionality like coroutine scopes, listeners, etc.
     *
     * @param middlewareContext Provides context about the current state and configuration.
     */
    fun onAttached(middlewareContext: C)

    /**
     * Invoked when the middleware is no longer active.
     * Use this method to cleanup or release resources that were initialized or used
     * during the active period. No further calls to the middleware will be made after this
     * method is invoked, but the object can become active after another [onAttached] call.
     *
     * @param middlewareContext Provides context about the current state and configuration.
     */
    fun onDetached(middlewareContext: C)
}
