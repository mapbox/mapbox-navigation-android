package com.mapbox.navigation.mapgpt.core.microphone

import com.mapbox.navigation.mapgpt.core.MiddlewareProvider

/**
 * Used to identify the [PlatformMicrophone].
 *
 * @param key A unique key identifying the middleware.
 */
open class MicrophoneProvider(key: String): MiddlewareProvider(key)
