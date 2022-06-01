package com.mapbox.navigation.ui.base

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.extensions.attachCreated
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.ui.base.lifecycle.UIComponent
import java.util.concurrent.ConcurrentLinkedQueue

@ExperimentalPreviewMapboxNavigationAPI
fun MapboxNavigationApp.installComponents(
    lifecycleOwner: LifecycleOwner,
    config: ComponentConfig.() -> Unit
) {
    val components = NavigationComponents().apply(config)
    lifecycleOwner.attachCreated(components)
}

@ExperimentalPreviewMapboxNavigationAPI
fun MapboxNavigation.installComponents(
    lifecycleOwner: LifecycleOwner,
    config: ComponentConfig.() -> Unit
) {
    val components = NavigationComponents().apply(config)
    lifecycleOwner.lifecycle.addObserver(AttachOnCreate(this, components))
}

@ExperimentalPreviewMapboxNavigationAPI
sealed interface ComponentConfig {
    fun component(component: UIComponent)
}

@ExperimentalPreviewMapboxNavigationAPI
internal class NavigationComponents(
    private val components: MapboxNavigationObserverChain = MapboxNavigationObserverChain()
) : ComponentConfig, MapboxNavigationObserver by components {

    override fun component(component: UIComponent) = components.add(component)
}

@ExperimentalPreviewMapboxNavigationAPI
class AttachOnCreate(
    private val mapboxNavigation: MapboxNavigation,
    private val observer: MapboxNavigationObserver
) : DefaultLifecycleObserver {

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        observer.onAttached(mapboxNavigation)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        observer.onDetached(mapboxNavigation)
    }
}

@ExperimentalPreviewMapboxNavigationAPI
open class MapboxNavigationObserverChain : MapboxNavigationObserver {
    private val observers = ConcurrentLinkedQueue<MapboxNavigationObserver>()

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        observers.forEach { it.onAttached(mapboxNavigation) }
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        observers.forEach { it.onDetached(mapboxNavigation) }
    }

    fun add(observer: MapboxNavigationObserver) {
        observers.add(observer)
    }

    fun remove(observer: MapboxNavigationObserver) {
        observers.remove(observer)
    }
}
