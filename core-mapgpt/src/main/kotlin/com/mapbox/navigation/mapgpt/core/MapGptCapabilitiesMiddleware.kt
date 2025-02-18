package com.mapbox.navigation.mapgpt.core

import com.mapbox.navigation.mapgpt.core.api.activeConversationEvents

/**
 * Middleware that surfaces [MapGptCapability] to MapGpt. With a [MapGptCapability] the
 * implementation is expected to handle the events from [activeConversationEvents].
 */
interface MapGptCapabilitiesMiddleware<Context : MiddlewareContext> : MapGptCapabilities, Middleware<Context> {

    /**
     * Return the context that will be used to attach the middleware. This makes it possible to
     * use custom contexts into the middleware. The context returned will be used to attach the
     * middleware. @see [Middleware.onAttached]
     */
    fun <Parent : MiddlewareContext> provideContext(parent: Parent): Context
}
