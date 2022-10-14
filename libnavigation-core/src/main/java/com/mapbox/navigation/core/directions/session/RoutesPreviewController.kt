package com.mapbox.navigation.core.directions.session

import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.concurrent.CopyOnWriteArraySet

class RoutesPreviewController private constructor() : MapboxNavigationObserver {
    private var routePreview: RoutePreview = RoutePreview.Builder().build()
    private val routesPreviewObservers = CopyOnWriteArraySet<RoutesPreviewObserver>()
    private var mapboxNavigation: MapboxNavigation? = null
    private val routesObserver = RoutesObserver { TODO("Not yet implemented") }
    private val isActiveRouteSyncEnabled = MutableStateFlow(false)

    fun enableActiveRouteSync(enable: Boolean) {
        isActiveRouteSyncEnabled.value = enable
    }

    fun getRoutesPreview(): RoutePreview = routePreview

    fun setRoutePreviewRoutes(navigationRoutes: List<NavigationRoute>) {
        setRoutePreview(routePreview.toBuilder()
            .selectedIndex(0)
            .navigationRoutes(navigationRoutes)
            .build()
        )
    }

    fun setRoutePreviewIndex(index: Int) {
        setRoutePreview(routePreview.toBuilder()
            .selectedIndex(index).build())
    }

    fun setRoutePreview(routePreview: RoutePreview) {
        if (this.routePreview != routePreview) {
            this.routePreview = routePreview
            routesPreviewObservers.forEach { it.onRoutesPreviewChanged(routePreview) }
        }
    }

    fun startActiveGuidance() {
        check (routePreview.navigationRoutes.isNotEmpty()) {
            "Cannot start active guidance when the route preview is empty."
        }
        // order list where selected index is first
        mapboxNavigation.setNavigationRoutes(routePreview.navigationRoutes)
    }

    fun registerObserver(observer: RoutesPreviewObserver) = apply {
        routesPreviewObservers.add(observer)
        observer.onRoutesPreviewChanged(routePreview)
    }

    fun unregisterObserver(observer: RoutesPreviewObserver) = apply {
        routesPreviewObservers.remove(observer)
    }

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        this.mapboxNavigation
        c
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        this.mapboxNavigation = null
    }

    companion object {

        /**
         * Get the registered instance or create one and register it to [MapboxNavigationApp].
         */
        @JvmStatic
        fun getRegisteredInstance(): RoutesPreviewController = MapboxNavigationApp
            .getObservers(RoutesPreviewController::class)
            .firstOrNull() ?: RoutesPreviewController().also {
                MapboxNavigationApp.registerObserver(it)
            }
    }
}
