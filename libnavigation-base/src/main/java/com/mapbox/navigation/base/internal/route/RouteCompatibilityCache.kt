package com.mapbox.navigation.base.internal.route

import android.util.LruCache
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.base.route.NavigationRoute

/**
 * **Internal** cache object that aims to improve the performance performance of compatibility functions
 * that transform the [DirectionsRoute] route into a [NavigationRoute].
 *
 * It caches up to 3 randomly created [NavigationRoute] instances,
 * as well as all instances tracked by the internal `MapboxDirectionsSession` (which notifies the `RoutesObserver`).
 *
 * The cache is cleared whenever the `MapboxDirectionsSession` changes (and when it's cleared).
 * This should be relatively efficient since we treat `MapboxDirectionsSession` as the source of truth and `MapboxNavigation` will clear it on its `onDestroy` as well.
 */
object RouteCompatibilityCache {
    private val directionsSessionCache = mutableListOf<NavigationRoute>()
    private val creationCache = LruCache<DirectionsRoute, NavigationRoute>(3)
    private val lock = Any()

    /**
     * Use to put a result of a random route creation to cache.
     */
    fun cacheCreationResult(routes: List<NavigationRoute>) {
        synchronized(lock) {
            routes.forEach {
                creationCache.put(it.directionsRoute, it)
            }
        }
    }

    /**
     * Use to put all routes tracked by `MapboxDirectionsSession` to cache (and clear everything else).
     */
    fun setDirectionsSessionResult(routes: List<NavigationRoute>) {
        synchronized(lock) {
            creationCache.evictAll()
            directionsSessionCache.clear()
            directionsSessionCache.addAll(routes)
        }
    }

    /**
     * Get a cached [NavigationRoute] if there is one that wraps the provided [DirectionsRoute] (based on equality) or `null`.
     */
    fun getFor(directionsRoute: DirectionsRoute): NavigationRoute? {
        synchronized(lock) {
            return directionsSessionCache.find { it.directionsRoute == directionsRoute }
                ?: creationCache.get(directionsRoute)
        }
    }
}
