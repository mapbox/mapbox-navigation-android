package com.mapbox.navigation.dropin

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.mapbox.maps.MapView
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.extensions.attachCreated
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.ui.app.internal.SharedApp
import com.mapbox.navigation.ui.base.lifecycle.UIComponent
import com.mapbox.navigation.ui.maps.internal.ui.RouteArrowComponent
import com.mapbox.navigation.ui.maps.internal.ui.RouteLineComponent
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineResources
import com.mapbox.navigation.ui.voice.internal.ui.AudioGuidanceButtonComponent
import com.mapbox.navigation.ui.voice.view.MapboxAudioGuidanceButton
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
interface ComponentConfig {
    fun audioGuidanceButtonComponent(button: MapboxAudioGuidanceButton)
    fun routeLineComponent(mapView: MapView, config: RouteLineComponentConfig.() -> Unit = {})
    fun routeArrowComponent(mapView: MapView, config: RouteArrowComponentConfig.() -> Unit = {})

    fun component(component: UIComponent)
}

@ExperimentalPreviewMapboxNavigationAPI
internal class NavigationComponents(
    private val components: MapboxNavigationObserverChain = MapboxNavigationObserverChain()
) : ComponentConfig, MapboxNavigationObserver by components {

    override fun audioGuidanceButtonComponent(button: MapboxAudioGuidanceButton) {
        SharedApp.setup(button.context)
        component(AudioGuidanceButtonComponent(button))
    }

    override fun routeLineComponent(
        mapView: MapView,
        config: RouteLineComponentConfig.() -> Unit
    ) {
        val componentConfig = RouteLineComponentConfig(mapView.context).apply(config)
        component(RouteLineComponent(mapView, componentConfig.options))
    }

    override fun routeArrowComponent(
        mapView: MapView,
        config: RouteArrowComponentConfig.() -> Unit
    ) {
        val componentConfig = RouteArrowComponentConfig(mapView.context).apply(config)
        component(RouteArrowComponent(mapView, componentConfig.options))
    }

    override fun component(component: UIComponent) = components.add(component)
}

class RouteLineComponentConfig(context: Context) {
    var options = MapboxRouteLineOptions.Builder(context)
        .withRouteLineResources(RouteLineResources.Builder().build())
        .withRouteLineBelowLayerId("road-label-navigation")
        .withVanishingRouteLineEnabled(true)
        .build()
}

class RouteArrowComponentConfig(context: Context) {
    var options = RouteArrowOptions.Builder(context)
        .withAboveLayerId(RouteLayerConstants.TOP_LEVEL_ROUTE_LINE_LAYER_ID)
        .build()
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
