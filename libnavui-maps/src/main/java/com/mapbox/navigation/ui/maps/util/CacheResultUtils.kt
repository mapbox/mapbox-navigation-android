package com.mapbox.navigation.ui.maps.util

import android.util.LruCache
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.base.route.NavigationRoute

internal object CacheResultUtils {
    private interface CacheResultCall<in F, out R> {
        operator fun invoke(f: F): R
    }

    data class CacheResultKey1<out P1, R>(val p1: P1) :
        CacheResultCall<(P1) -> R, R> {
        override fun invoke(f: (P1) -> R) = f(p1)
    }

    data class CacheResultKey2<out P1, P2, R>(val p1: P1, val p2: P2) :
        CacheResultCall<(P1, P2) -> R, R> {
        override fun invoke(f: (P1, P2) -> R) = f(p1, p2)
    }

    /**
     * Specialized cache key that can be used for managing results of processing related to a route.
     *
     * This key uses [NavigationRoute.id] instead of [NavigationRoute.hashCode] because the latter produces a hash of the full [DirectionsRoute]
     * which for longer routes and use cases that require very frequent access (multiple times per second) proves to be inefficient (refs https://github.com/mapbox/navigation-sdks/issues/1918).
     *
     * What has to be kept in mind, is that [NavigationRoute.id] does not guarantee uniqueness of the [NavigationRoute].
     * There can be small but impactful (depending on processing task) differences between two [NavigationRoute] instances that have the same ID.
     * For example, they can have different congestion annotations.
     *
     * That's why this cache key can be used for access during operations that are not impacted by changes in these untracked areas of the route.
     */
    data class CacheResultKeyRoute<R>(val route: NavigationRoute) :
        CacheResultCall<(NavigationRoute) -> R, R> {

        override fun invoke(f: (NavigationRoute) -> R) = f(route)
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as CacheResultKeyRoute<*>

            if (route.id != other.route.id) return false

            return true
        }

        override fun hashCode(): Int {
            return route.id.hashCode()
        }
    }

    data class CacheResultKeyRoute2<P1, R>(val route: NavigationRoute, val p1: P1) :
        CacheResultCall<(NavigationRoute, P1) -> R, R> {

        override fun invoke(f: (NavigationRoute, P1) -> R) = f(route, p1)

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is CacheResultKeyRoute2<*, *>) return false

            if (route.id != other.route.id) return false
            if (p1 != other.p1) return false

            return true
        }

        override fun hashCode(): Int {
            var result = route.id.hashCode()
            // result = 31 * result + route.directionsRoute.legs()?.map { it.annotation() }.hashCode()
            // result = 31 * result + route.directionsRoute.legs()?.map { it.closures() }.hashCode()
            // result = 31 * result + route.directionsRoute.legs()?.map { it.incidents() }.hashCode()
            result = 31 * result + (p1?.hashCode() ?: 0)
            return result
        }
    }

    fun <P1, R> ((P1) -> R).cacheResult(maxSize: Int): (P1) -> R {
        return object : (P1) -> R {
            val cache: LruCache<CacheResultKey1<P1, R>, R> = LruCache(maxSize)
            private val handler =
                CacheResultHandler<((P1) -> R), CacheResultKey1<P1, R>, R>(
                    this@cacheResult,
                    cache
                )
            override fun invoke(p1: P1) = handler(CacheResultKey1(p1))
        }
    }

    fun <P1, P2, R> ((P1, P2) -> R).cacheResult(maxSize: Int): (P1, P2) -> R {
        return object : (P1, P2) -> R {
            val cache: LruCache<CacheResultKey2<P1, P2, R>, R> = LruCache(maxSize)
            private val handler =
                CacheResultHandler<((P1, P2) -> R), CacheResultKey2<P1, P2, R>, R>(
                    this@cacheResult,
                    cache
                )
            override fun invoke(p1: P1, p2: P2) = handler(CacheResultKey2(p1, p2))
        }
    }

    fun <P1, R> ((P1) -> R).cacheResult(cache: LruCache<CacheResultKey1<P1, R>, R>): (P1) -> R {
        return object : (P1) -> R {
            private val handler =
                CacheResultHandler<((P1) -> R), CacheResultKey1<P1, R>, R>(
                    this@cacheResult,
                    cache
                )
            override fun invoke(p1: P1) = handler(CacheResultKey1(p1))
        }
    }

    fun <P1, P2, R> ((P1, P2) -> R).cacheResult(
        cache: LruCache<CacheResultKey2<P1, P2, R>, R>
    ): (P1, P2) -> R {
        return object : (P1, P2) -> R {
            private val handler =
                CacheResultHandler<((P1, P2) -> R), CacheResultKey2<P1, P2, R>, R>(
                    this@cacheResult,
                    cache
                )
            override fun invoke(p1: P1, p2: P2) = handler(CacheResultKey2(p1, p2))
        }
    }

    fun <R> ((NavigationRoute) -> R).cacheRouteResult(
        cache: LruCache<CacheResultKeyRoute<R>, R>
    ): (NavigationRoute) -> R {
        return object : (NavigationRoute) -> R {
            private val handler =
                CacheResultHandler(
                    this@cacheRouteResult,
                    cache
                )
            override fun invoke(route: NavigationRoute) = handler(CacheResultKeyRoute(route))
        }
    }

    fun <R, P1> ((NavigationRoute, P1) -> R).cacheRouteResult(
        cache: LruCache<CacheResultKeyRoute2<P1, R>, R>
    ): (NavigationRoute, P1) -> R {
        return object : (NavigationRoute, P1) -> R {
            private val handler =
                CacheResultHandler(
                    this@cacheRouteResult,
                    cache
                )
            override fun invoke(route: NavigationRoute, p1: P1) = handler(CacheResultKeyRoute2(route, p1))
        }
    }

    private class CacheResultHandler<F, K : CacheResultCall<F, R>, R>(
        val f: F,
        val cache: LruCache<K, R>
    ) {
        operator fun invoke(k: K): R {
            // this is synchronized per cache unit (so per task), and we can't have multiple operations being parallelized, they tend to wait for one another on this lock.
            return cache[k] ?: run {
                k(f).also {
                    cache.put(k, it)
                }
            }
        }
    }
}
