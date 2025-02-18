package com.mapbox.navigation.mapgpt.core

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
