@file:JvmName("RequireMapboxNavigation")

package com.mapbox.navigation.core.lifecycle

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp.lifecycleOwner
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Extension function to make it simple to create the [RequireMapboxNavigationDelegate].
 * Below are a couple examples of how you may use the delegate.
 *
 * Default can be used when [MapboxNavigationApp] is setup elsewhere.
 * ```
 * val mapboxNavigation by requireMapboxNavigation()
 * ```
 *
 * Initialize the [MapboxNavigationApp] when you are ready to use it
 * ```
 * val mapboxNavigation by requireMapboxNavigation {
 *   MapboxNavigationApp.setup(..)
 * }
 * ```
 *
 * Register subscriptions and setup MapboxNavigationApp
 * ```
 * private val mapboxNavigation by requireMapboxNavigation(
 *   onResumedObserver = object : MapboxNavigationObserver {
 *     override fun onAttached(mapboxNavigation: MapboxNavigation) {
 *       mapboxNavigation.registerLocationObserver(locationObserver)
 *       mapboxNavigation.registerRoutesObserver(routesObserver)
 *     }
 *     override fun onDetached(mapboxNavigation: MapboxNavigation) {
 *       mapboxNavigation.unregisterLocationObserver(locationObserver)
 *       mapboxNavigation.unregisterRoutesObserver(routesObserver)
 *     }
 *   }
 * ) {
 *   MapboxNavigationApp.setup(
 *       NavigationOptions.Builder(this)
 *           .build()
 *     )
 * }
 * ```
 *
 * @see [RequireMapboxNavigationDelegate] for more details.
 */
fun LifecycleOwner.requireMapboxNavigation(
    onCreatedObserver: MapboxNavigationObserver? = null,
    onStartedObserver: MapboxNavigationObserver? = null,
    onResumedObserver: MapboxNavigationObserver? = null,
    onInitialize: (() -> Unit)? = null,
): ReadOnlyProperty<Any, MapboxNavigation> = RequireMapboxNavigationDelegate(
    lifecycleOwner = this,
    onCreatedObserver = onCreatedObserver,
    onStartedObserver = onStartedObserver,
    onResumedObserver = onResumedObserver,
    onInitialize = onInitialize,
)

/**
 * Attaches a [LifecycleOwner] to [MapboxNavigationApp] and provides access to [MapboxNavigation].
 *
 * You can choose to call [MapboxNavigationApp.setup] in the [onInitialize]. You can also setup in
 * the onCreate calls, or any call that happens before this delegate is accessed. The delegate will
 * crash if accessed when the app is not setup or an attached lifecycle has not been created.
 *
 * You can use the observers parameter to setup any subscriptions. This is important because the
 * [MapboxNavigation] instance can be re-created with [MapboxNavigationApp.disable], or if all
 * [MapboxNavigationApp.attach] lifecycles are destroyed.
 *
 * @param lifecycleOwner: LifecycleOwner
 * @param onCreatedObserver registered to the [Lifecycle.State.CREATED] lifecycle
 * @param onStartedObserver registered to the [Lifecycle.State.STARTED] lifecycle
 * @param onResumedObserver registered to the [Lifecycle.State.RESUMED] lifecycle
 * @param onInitialize called when the [lifecycleOwner] is [Lifecycle.State.CREATED]
 */
internal class RequireMapboxNavigationDelegate(
    lifecycleOwner: LifecycleOwner,
    private val onCreatedObserver: MapboxNavigationObserver? = null,
    private val onStartedObserver: MapboxNavigationObserver? = null,
    private val onResumedObserver: MapboxNavigationObserver? = null,
    private val onInitialize: (() -> Unit)? = null,
) : ReadOnlyProperty<Any, MapboxNavigation> {

    private val lifecycleObserver = object : DefaultLifecycleObserver {
        override fun onCreate(owner: LifecycleOwner) {
            onInitialize?.invoke()
            onCreatedObserver?.let { MapboxNavigationApp.registerObserver(it) }
        }

        override fun onDestroy(owner: LifecycleOwner) {
            onCreatedObserver?.let { MapboxNavigationApp.unregisterObserver(it) }
        }

        override fun onStart(owner: LifecycleOwner) {
            onStartedObserver?.let { MapboxNavigationApp.registerObserver(it) }
        }

        override fun onStop(owner: LifecycleOwner) {
            onStartedObserver?.let { MapboxNavigationApp.unregisterObserver(it) }
        }

        override fun onResume(owner: LifecycleOwner) {
            onResumedObserver?.let { MapboxNavigationApp.registerObserver(it) }
        }

        override fun onPause(owner: LifecycleOwner) {
            onResumedObserver?.let { MapboxNavigationApp.unregisterObserver(it) }
        }
    }

    init {
        MapboxNavigationApp.attach(lifecycleOwner)
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
    }

    /**
     * Returns an instance of [MapboxNavigation]. If [MapboxNavigationApp.isSetup] is false after
     * all observers and initializers, this property getter will crash.
     *
     * @param thisRef - the [LifecycleOwner] that needs access to [MapboxNavigation].
     * @param property - ignored
     */
    override fun getValue(thisRef: Any, property: KProperty<*>): MapboxNavigation {
        val mapboxNavigation = MapboxNavigationApp.current()
        checkNotNull(mapboxNavigation) {
            "MapboxNavigation cannot be null. Ensure that MapboxNavigationApp is setup and an" +
                " attached lifecycle is at least CREATED."
        }
        return mapboxNavigation
    }
}
