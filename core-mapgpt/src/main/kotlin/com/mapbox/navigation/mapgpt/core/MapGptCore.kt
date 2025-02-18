package com.mapbox.navigation.mapgpt.core

/**
 * Access point for core functionality of the MapGpt SDK, including lifecycle management,
 * middleware registration, and surface capabilities.
 */
class MapGptCore : CapabilitiesMiddlewareRegistry<MapGptCoreContext>() {

    /**
     * Separate registry for registering and unregistering middleware without
     * affecting the core registry.
     */
    val externalRegistry = CapabilitiesMiddlewareRegistry<MiddlewareContext>()

    /**
     * Attaches the platform context to the MapGpt SDK with default core components.
     */
    fun attach(platformContext: PlatformContext) = apply {
        check(!isAttached) { "MapGptCore is already attached" }
        val mapGptContext = MapGptCoreContext.Builder(platformContext).build()
        onAttached(mapGptContext)
    }

    /**
     * Called to begin the lifecycle of the MapGpt SDK. This method can be called when the
     * [MapGptCoreContext] is constructed with custom components, otherwise use [attach].
     */
    override fun onAttached(middlewareContext: MapGptCoreContext) {
        super.onAttached(middlewareContext)
        externalRegistry.onAttached(middlewareContext)
    }

    /**
     * Called to end the lifecycle of the MapGpt SDK. Expected to be called by the same context
     * that called [onAttached]. Prefer using [detach] instead of [onDetached].
     */
    override fun onDetached(middlewareContext: MapGptCoreContext) {
        super.onDetached(middlewareContext)
        externalRegistry.onDetached(middlewareContext)
    }
}
