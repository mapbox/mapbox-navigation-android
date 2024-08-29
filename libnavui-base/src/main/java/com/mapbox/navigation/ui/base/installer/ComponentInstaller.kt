package com.mapbox.navigation.ui.base.installer

import androidx.annotation.UiThread
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.extensions.MapboxNavigationObserverChain
import com.mapbox.navigation.core.internal.extensions.attachCreated
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.ui.base.lifecycle.UIComponent

/**
 * UIComponents installer interface.
 */
@ExperimentalPreviewMapboxNavigationAPI
sealed interface ComponentInstaller {
    /**
     * Install given UIComponent and manage its lifecycle alongside MapboxNavigation lifecycle.
     */
    fun component(component: UIComponent): Installation = components(component)

    /**
     * Install multiple UIComponents and manage their lifecycle alongside MapboxNavigation lifecycle.
     */
    fun components(vararg components: UIComponent): Installation

    /**
     * Find installed component that matches given [predicate].
     */
    fun <T> findComponent(predicate: (Any) -> Boolean): T?
}

/**
 * Installed component handle that allows early component removal.
 */
@ExperimentalPreviewMapboxNavigationAPI
fun interface Installation {
    /**
     * Remove installed component.
     * If the component has already been attached to a MapboxNavigation instance
     * it will be also detached.
     */
    fun uninstall()
}

/**
 * Install UI components in the default [MapboxNavigation] instance.
 *
 * Each component will be attached to the default [MapboxNavigation] instance on [lifecycleOwner]
 * [Lifecycle.Event.ON_CREATE] event and detached on [Lifecycle.Event.ON_DESTROY].
 *
 * Example usage:
 * ```
 * class MyNavigationActivity : AppCompatActivity() {
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *         super.onCreate(savedInstanceState)
 *         setContentView(R.layout.my_nav_activity)
 *
 *         // ...
 *
 *         val soundButton = findViewById<MapboxAudioGuidanceButton>(R.id.soundButton)
 *         val mapView = findViewById<MapView>(R.id.mapView)
 *
 *         MapboxNavigationApp.installComponents(this) {
 *             audioGuidanceButton(soundButton)
 *             routeLine(mapView)
 *             routeArrow(mapView)
 *
 *             component(MyCustomUIComponent())
 *         }
 *     }
 * }
 * ```
 */
@ExperimentalPreviewMapboxNavigationAPI
@Suppress("unused")
@UiThread
fun MapboxNavigationApp.installComponents(
    lifecycleOwner: LifecycleOwner,
    config: ComponentInstaller.() -> Unit,
) {
    val installer = NavigationComponents().apply(config)
    lifecycleOwner.attachCreated(installer)
}

/**
 * Install UI components in receiving [MapboxNavigation] instance.
 *
 * Each component will be attached to receiving [MapboxNavigation] instance on [lifecycleOwner]
 * [Lifecycle.Event.ON_CREATE] event and detached on [Lifecycle.Event.ON_DESTROY].
 *
 * Example usage:
 * ```
 * class MyNavigationActivity : AppCompatActivity() {
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *         super.onCreate(savedInstanceState)
 *         setContentView(R.layout.my_nav_activity)
 *
 *         // ...
 *
 *         val soundButton = findViewById<MapboxAudioGuidanceButton>(R.id.soundButton)
 *         val mapView = findViewById<MapView>(R.id.mapView)
 *
 *         val mapboxNavigation = MapboxNavigationProvider.retrieve()
 *         mapboxNavigation.installComponents(this) {
 *             audioGuidanceButton(soundButton)
 *             routeLine(mapView)
 *             routeArrow(mapView)
 *
 *             component(MyCustomUIComponent())
 *         }
 *     }
 * }
 * ```
 */
@ExperimentalPreviewMapboxNavigationAPI
@UiThread
fun MapboxNavigation.installComponents(
    lifecycleOwner: LifecycleOwner,
    config: ComponentInstaller.() -> Unit,
) {
    val components = NavigationComponents().apply(config)
    lifecycleOwner.attachCreated(this, components)
}

@ExperimentalPreviewMapboxNavigationAPI
internal class NavigationComponents(
    private val componentsChain: MapboxNavigationObserverChain = MapboxNavigationObserverChain(),
) : ComponentInstaller, MapboxNavigationObserver by componentsChain {

    override fun components(vararg components: UIComponent): Installation {
        componentsChain.addAll(*components)
        return Installation { componentsChain.removeAndDetach(*components) }
    }

    override fun <T> findComponent(predicate: (Any) -> Boolean): T? {
        return componentsChain.toList().firstOrNull(predicate) as? T
    }
}

/**
 * Find installed component of type T.
 *
 * Shorthand for `findComponent { it is T } as? T`
 */
@ExperimentalPreviewMapboxNavigationAPI
inline fun <reified T> ComponentInstaller.findComponent(): T? {
    return findComponent { it is T } as? T
}
