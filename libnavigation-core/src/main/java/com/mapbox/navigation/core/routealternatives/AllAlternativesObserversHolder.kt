package com.mapbox.navigation.core.routealternatives

import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.route.toDirectionsRoutes
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.utils.internal.logE
import java.util.concurrent.CopyOnWriteArraySet

internal interface FirstAndLastObserverListener {

    fun onFirstObserver()

    fun onLastObserver()
}

internal class AllAlternativesObserversHolder :
    NavigationRouteAlternativesObserver,
    OffboardRoutesObserver {

    private val firstAndLastObserverListeners = mutableSetOf<FirstAndLastObserverListener>()
    private val offboardRoutesObservers = CopyOnWriteArraySet<OffboardRoutesObserver>()
    private val alternativesObservers = CopyOnWriteArraySet<NavigationRouteAlternativesObserver>()

    private val legacyObserversMap =
        hashMapOf<RouteAlternativesObserver, NavigationRouteAlternativesObserver>()

    fun register(routeAlternativesObserver: RouteAlternativesObserver) {
        if (routeAlternativesObserver !in legacyObserversMap) {
            val observer = object : NavigationRouteAlternativesObserver {
                override fun onRouteAlternatives(
                    routeProgress: RouteProgress,
                    alternatives: List<NavigationRoute>,
                    routerOrigin: RouterOrigin
                ) {
                    routeAlternativesObserver.onRouteAlternatives(
                        routeProgress,
                        alternatives.toDirectionsRoutes(),
                        routerOrigin
                    )
                }

                override fun onRouteAlternativesError(error: RouteAlternativesError) {
                    logE("Error: ${error.message}", RouteAlternativesController.LOG_CATEGORY)
                }
            }
            legacyObserversMap[routeAlternativesObserver] = observer
            register(observer)
        }
    }

    fun unregister(routeAlternativesObserver: RouteAlternativesObserver) {
        val observer = legacyObserversMap.remove(routeAlternativesObserver)
        if (observer != null) {
            unregister(observer)
        }
    }

    fun register(routeAlternativesObserver: NavigationRouteAlternativesObserver) {
        register(routeAlternativesObserver, alternativesObservers)
    }

    fun unregister(routeAlternativesObserver: NavigationRouteAlternativesObserver) {
        unregister(routeAlternativesObserver, alternativesObservers)
    }

    fun register(observer: OffboardRoutesObserver) {
        register(observer, offboardRoutesObservers)
    }

    fun unregister(observer: OffboardRoutesObserver) {
        unregister(observer, offboardRoutesObservers)
    }

    private fun <T> register(observer: T, set: MutableSet<T>) {
        val prevCount = getAllObserversCount()
        val added = set.add(observer)
        if (prevCount == 0 && added) {
            firstAndLastObserverListeners.forEach { it.onFirstObserver() }
        }
    }

    private fun <T> unregister(observer: T, set: MutableSet<T>) {
        val prevCount = getAllObserversCount()
        val removed = set.remove(observer)
        if (prevCount == 1 && removed) {
            firstAndLastObserverListeners.forEach { it.onLastObserver() }
        }
    }

    fun clear() {
        alternativesObservers.clear()
        legacyObserversMap.clear()
        offboardRoutesObservers.clear()
    }

    fun addFirstAndLastObserverListener(listener: FirstAndLastObserverListener) {
        firstAndLastObserverListeners.add(listener)
    }

    override fun onRouteAlternatives(
        routeProgress: RouteProgress,
        alternatives: List<NavigationRoute>,
        routerOrigin: RouterOrigin
    ) {
        alternativesObservers.forEach {
            it.onRouteAlternatives(routeProgress, alternatives, routerOrigin)
        }
    }

    override fun onRouteAlternativesError(error: RouteAlternativesError) {
        alternativesObservers.forEach { it.onRouteAlternativesError(error) }
    }

    override fun onOffboardRoutesAvailable(routes: List<NavigationRoute>) {
        offboardRoutesObservers.forEach { it.onOffboardRoutesAvailable(routes) }
    }

    private fun getAllObserversCount(): Int =
        alternativesObservers.size + offboardRoutesObservers.size
}
