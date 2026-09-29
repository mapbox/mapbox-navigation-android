@file:Suppress("ForbiddenImport")

package com.mapbox.common.dispatchers

import android.util.Log
import androidx.annotation.RestrictTo
import com.mapbox.annotation.MapboxExperimental
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainCoroutineDispatcher

/**
 * Holds the coroutine dispatchers used internally by the whole Navigation SDK.
 *
 * Call [configure] in `Application.onCreate()` to control how much parallelism the SDK is
 * allowed to use, for example to reduce contention with the rest of the host application.
 * If [configure] is never called, [Default] and [IO] fall back to [SdkDispatchersConfig]'s
 * built-in defaults.
 *
 * [configure] can be called at any time, but code that has already read [Default] or [IO]
 * before [configure] runs will continue using the previously resolved dispatcher for its
 * current execution. Call [configure] as early as possible — ideally before any SDK entry
 * point is used — so that the configured dispatchers take effect for the whole SDK lifecycle.
 *
 * If [configure] is called multiple times the last call wins; this can happen, for example,
 * when re-creating the SDK in tests.
 */
@OptIn(MapboxExperimental::class)
object SdkDispatchers {

    private const val TAG = "SdkDispatchers"

    @Volatile
    private var config: SdkDispatchersConfig? = null

    @Volatile
    private var configuredExplicitly: Boolean = false

    @Volatile
    private var testDispatcher: CoroutineDispatcher? = null

    /**
     * Dispatcher used by the SDK for CPU-bound work.
     */
    @MapboxExperimental
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    val Default: CoroutineDispatcher
        get() = testDispatcher ?: resolvedConfig().defaultDispatcher

    /**
     * Dispatcher used by the SDK for blocking IO work.
     */
    @MapboxExperimental
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    val IO: CoroutineDispatcher
        get() = testDispatcher ?: resolvedConfig().ioDispatcher

    /**
     * The main thread dispatcher.
     */
    @MapboxExperimental
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    val Main: MainCoroutineDispatcher
        get() = Dispatchers.Main

    /**
     * Configures [Default] and [IO] for the whole SDK. Call this in `Application.onCreate()`
     * before using any SDK entry point, so the configured dispatchers take effect for the whole
     * SDK lifecycle. If called after [Default] or [IO] have already been read, those earlier
     * reads used the built-in defaults; all subsequent reads will use the configured values.
     */
    @MapboxExperimental
    @Synchronized
    fun configure(block: SdkDispatchersConfig.Builder.() -> Unit) {
        if (configuredExplicitly) {
            Log.e(TAG, "configure() called more than once. Previous config will be overwritten.")
        } else if (config != null) {
            Log.w(
                TAG,
                "configure() called after dispatchers were already resolved to built-in " +
                    "defaults. The configured values will take effect for all subsequent reads.",
            )
        }
        val builder = SdkDispatchersConfig.Builder()
        builder.block()
        config = builder.build()
        configuredExplicitly = true
    }

    private fun resolvedConfig(): SdkDispatchersConfig = config ?: synchronized(this) {
        config ?: SdkDispatchersConfig.default().also { config = it }
    }

    @MapboxExperimental
    internal fun setTestDispatcher(dispatcher: CoroutineDispatcher) {
        testDispatcher = dispatcher
    }

    @MapboxExperimental
    internal fun resetTestDispatcher() {
        testDispatcher = null
    }

    /**
     * Resets the resolved config so a fresh [SdkDispatchersConfig] can be applied.
     * Test-only: [SdkDispatchers] is a process-wide singleton, so tests exercising
     * [configure] must reset it before/after each test to avoid leaking state into other tests.
     */
    @MapboxExperimental
    internal fun resetConfigForTest() {
        config = null
        configuredExplicitly = false
    }
}
