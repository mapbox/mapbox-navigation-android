package com.mapbox.navigation.base.internal.route

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.base.route.NavigationRoute
import java.util.concurrent.ConcurrentHashMap

/**
 * **Internal** cache object that aims to improve the performance of compatibility functions
 * that transform the [DirectionsRoute] route into a [NavigationRoute].
 *
 * It caches up to 3 randomly created [NavigationRoute] instances,
 * as well as all instances tracked by the internal `MapboxDirectionsSession` (which notifies the `RoutesObserver`).
 *
 * The cache is cleared whenever the `MapboxDirectionsSession` changes (and when it's cleared).
 * This should be relatively efficient since we treat `MapboxDirectionsSession` as the source of truth and `MapboxNavigation` will clear it on its `onDestroy` as well.
 */
object RouteCompatibilityCache {
    private const val MAX_CACHE_SIZE = 3
    private val creationCache =
        ConcurrentHashMap<DirectionsRoute, NavigationRoutePackage>(MAX_CACHE_SIZE)

    /**
     * Use to put a result of a random route creation to cache.
     */
    fun cacheCreationResult(routes: List<NavigationRoute>) {
        routes.forEach {
            creationCache[it.directionsRoute] = NavigationRoutePackage(it, System.nanoTime())
        }
        maintainCacheSize()
    }

    /**
     * Use to put all routes tracked by `MapboxDirectionsSession` to cache (and clear everything else).
     */
    fun setDirectionsSessionResult(routes: List<NavigationRoute>) {
        creationCache.clear()
        routes.forEach {
            creationCache[it.directionsRoute] = NavigationRoutePackage(it, System.nanoTime())
        }
        maintainCacheSize()
    }

    /**
     * Get a cached [NavigationRoute] if there is one that wraps the provided [DirectionsRoute] (based on equality) or `null`.
     */
    fun getFor(directionsRoute: DirectionsRoute): NavigationRoute? {
        return creationCache[directionsRoute]?.route
    }

    /**
     * Sorts the items in the cache by timestamp and removes the oldest items in order to maintain
     * a size no greater than MAX_CACHE_SIZE.
     */
    private fun maintainCacheSize() {
        if (creationCache.size > MAX_CACHE_SIZE) {
            val items = creationCache.map {
                Pair(it.key, it.value)
            }.sortedBy { it.second.timestamp }
            val overSize = items.size - MAX_CACHE_SIZE
            items.take(overSize).forEach { creationCache.remove(it.first) }
        }
    }
    private data class NavigationRoutePackage(val route: NavigationRoute, val timestamp: Long)
}
