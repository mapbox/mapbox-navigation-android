package com.mapbox.navigation.mapgpt.core.microphone

import com.mapbox.navigation.mapgpt.core.CoroutineMiddleware
import com.mapbox.navigation.mapgpt.core.Middleware
import com.mapbox.navigation.mapgpt.core.MapGptCoreContext
import kotlin.coroutines.CoroutineContext

/**
 * Implement this interface to provide a platform-specific microphone implementation.
 *
 * The microphone interactions are defined in the [PlatformMicrophone] interface.
 * The lifecycle events are defined [Middleware].
 * The context to the platform and some SDK settings are provided in [MapGptCoreContext].
 */
interface PlatformMicrophoneMiddleware : PlatformMicrophone, Middleware<MapGptCoreContext>

/**
 * Preferred way to implement a microphone middleware. Alternatively, you can implement
 * [PlatformMicrophoneMiddleware] directly.
 *
 * Extending the [CoroutineMiddleware] class gives access to [CoroutineContext] bound to the
 * [Middleware] lifecycle.
 */
abstract class MicrophoneMiddleware :
    PlatformMicrophoneMiddleware,
    CoroutineMiddleware<MapGptCoreContext>()
