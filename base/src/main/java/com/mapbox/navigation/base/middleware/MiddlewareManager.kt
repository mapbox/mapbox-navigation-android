package com.mapbox.navigation.base.middleware

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * An abstract class for managing multiple middleware instances.
 *
 * This class extends [CoroutineMiddleware] and ensures that all registered middleware switchers
 * are attached and detached appropriately based on the lifecycle of the manager.
 *
 * @param C The type of [MiddlewareContext] that the middleware operates on.
 * @param T The type of [Middleware] that this manager handles.
 */
@ExperimentalPreviewMapboxNavigationAPI
abstract class MiddlewareManager<C : MiddlewareContext, T : Middleware<C>> :
    CoroutineMiddleware<C>() {

    /**
     * A set of middleware switchers that manage individual middleware instances.
     */
    protected abstract val middlewareSwitchers: Set<MiddlewareSwitcher<C, T>>

    /**
     * Called when the middleware manager is attached.
     *
     * This method initializes all middleware switchers by invoking their [onAttached] method.
     *
     * @param middlewareContext The context providing information about the middleware state.
     */
    override fun onAttached(middlewareContext: C) {
        super.onAttached(middlewareContext)
        middlewareSwitchers.forEach {
            it.onAttached(middlewareContext)
        }
    }

    /**
     * Called when the middleware manager is detached.
     *
     * This method cleans up all middleware switchers by invoking their [onDetached] method.
     *
     * @param middlewareContext The context providing information about the middleware state.
     */
    override fun onDetached(middlewareContext: C) {
        super.onDetached(middlewareContext)
        middlewareSwitchers.forEach {
            it.onDetached(middlewareContext)
        }
    }
}
