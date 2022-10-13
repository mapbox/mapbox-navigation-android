package com.mapbox.navigation.core.directions.session

import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.MapboxNavigation
import java.util.concurrent.CopyOnWriteArraySet

class RoutesPreviewController(
    private val mapboxNavigation: MapboxNavigation
) {
    private var routesPreview: RoutesPreview = RoutesPreview.Builder().build()
    private val routesPreviewObservers = CopyOnWriteArraySet<RoutesPreviewObserver>()

    fun getRoutesPreview(): RoutesPreview = routesPreview

    fun setRoutePreviewRoutes(navigationRoutes: List<NavigationRoute>) {
        setRoutePreview(routesPreview.toBuilder()
            .selectedIndex(0)
            .navigationRoutes(navigationRoutes)
            .build()
        )
    }

    fun setRoutePreviewIndex(index: Int) {
        setRoutePreview(routesPreview.toBuilder()
            .selectedIndex(index).build())
    }

    fun setRoutePreview(routesPreview: RoutesPreview) {
        if (this.routesPreview != routesPreview) {
            this.routesPreview = routesPreview
            routesPreviewObservers.forEach { it.onRoutesPreviewChanged(routesPreview) }
        }
    }

    fun startActiveGuidance() {
        check (routesPreview.navigationRoutes.isNotEmpty()) {
            "Cannot start active guidance when the route preview is empty."
        }
    }

    fun registerObserver(observer: RoutesPreviewObserver) = apply {
        routesPreviewObservers.add(observer)
        observer.onRoutesPreviewChanged(routesPreview)
    }

    fun unregisterObserver(observer: RoutesPreviewObserver) = apply {
        routesPreviewObservers.remove(observer)
    }
}
