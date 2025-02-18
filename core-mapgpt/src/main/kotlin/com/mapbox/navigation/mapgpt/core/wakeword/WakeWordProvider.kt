package com.mapbox.navigation.mapgpt.core.wakeword

import com.mapbox.navigation.mapgpt.core.MiddlewareProvider

/**
 * Used to identify your [WakeWordMiddleware].
 *
 * @param key A unique key identifying the middleware.
 */
open class WakeWordProvider(key: String): MiddlewareProvider(key) {
    /**
     * When selected, the function from [WakeWordMiddleware] will not be functional.
     */
    object None : WakeWordProvider("")

    /**
     * Uses the Azure wake word system.
     */
    object Azure : WakeWordProvider("azure")
}
