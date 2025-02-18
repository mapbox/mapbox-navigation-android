package com.mapbox.navigation.mapgpt.core

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

/**
 * Registry for surfacing middleware that has [MapGptCapability]. When a middleware is registered
 * that implements [MapGptCapabilities], this registry will surface the capabilities to the
 * [MapGptCapability] consumer.
 *
 * [MapGptCapability] communicate to the MapGPT backend that the implementation will respond to
 * specific events, such as music player events, car control events, or any other event the MapGPT
 * backend is able to send. This registry also ensures that the capabilities are surfaced only
 * when the middleware is attached.
 */
open class CapabilitiesMiddlewareRegistry<Context : MiddlewareContext> : MiddlewareRegistry<Context>(),
    MapGptCapabilities {

    private val _capabilities: MutableStateFlow<Set<MapGptCapability>> = MutableStateFlow(emptySet())
    override val capabilities: Flow<Set<MapGptCapability>> = _capabilities

    fun setCapabilityMiddleware(
        middlewares: Set<MapGptCapabilitiesMiddleware<out Context>>,
    ) {
        val previous = this.middlewares.value
            .filterIsInstance<MapGptCapabilitiesMiddleware<out Context>>()
            .toSet()
        val added = middlewares - previous
        val removed = previous - middlewares
        removed.forEach { middleware ->
            unregisterCapabilities(middleware)
        }
        added.forEach { middleware ->
            registerCapabilities(middleware)
        }
    }

    fun <C : Context> registerCapabilities(middleware: MapGptCapabilitiesMiddleware<C>) = apply {
        register(middleware) { middleware.provideContext(this) }
    }

    fun <C : Context> unregisterCapabilities(middleware: MapGptCapabilitiesMiddleware<C>) = apply {
        unregister(middleware)
    }

    override fun onAttached(middlewareContext: Context) {
        super.onAttached(middlewareContext)

        @OptIn(FlowPreview::class)
        middlewares
            .map { it.filterIsInstance<MapGptCapabilities>().toSet() }
            .map { MapGptCapabilitiesProvider(mainScope, it) }
            .flatMapMerge { it.capabilities }
            .onEach { _capabilities.value = it }
            .launchIn(mainScope)
    }

    override fun onDetached(middlewareContext: Context) {
        super.onDetached(middlewareContext)
        _capabilities.value = emptySet()
    }
}
