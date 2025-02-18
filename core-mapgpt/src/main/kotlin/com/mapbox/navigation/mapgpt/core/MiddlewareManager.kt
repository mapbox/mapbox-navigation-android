package com.mapbox.navigation.mapgpt.core

abstract class MiddlewareManager<C : MiddlewareContext, T : Middleware<C>> : CoroutineMiddleware<C>() {

    protected abstract val middlewareSwitchers: Set<MiddlewareSwitcher<C, T>>

    override fun onAttached(middlewareContext: C) {
        super.onAttached(middlewareContext)
        middlewareSwitchers.forEach {
            it.onAttached(middlewareContext)
        }
    }

    override fun onDetached(middlewareContext: C) {
        super.onDetached(middlewareContext)
        middlewareSwitchers.forEach {
            it.onDetached(middlewareContext)
        }
    }
}
