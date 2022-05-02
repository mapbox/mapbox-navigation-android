package com.mapbox.androidauto.car.preview

import com.mapbox.androidauto.car.routes.RoutesListener
import com.mapbox.androidauto.car.routes.RoutesProvider
import com.mapbox.navigation.base.route.NavigationRoute
import java.util.concurrent.CopyOnWriteArraySet

internal class PreviewRoutesProvider(routes: List<NavigationRoute>) : RoutesProvider {

    var routes = routes
        set(value) {
            if (field.isEmpty() && value.isEmpty()) {
                return
            }
            field = value
            for (listener in routesListeners) {
                listener.onRoutesChanged(value)
            }
        }

    private val routesListeners = CopyOnWriteArraySet<RoutesListener>()

    override fun registerRoutesListener(listener: RoutesListener) {
        routesListeners.add(listener)
        if (routes.isNotEmpty()) {
            listener.onRoutesChanged(routes)
        }
    }

    override fun unregisterRoutesListener(listener: RoutesListener) {
        routesListeners.remove(listener)
    }
}
