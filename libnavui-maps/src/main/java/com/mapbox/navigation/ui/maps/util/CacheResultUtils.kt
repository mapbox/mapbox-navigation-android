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

    data class CacheResultKey3<out P1, P2, P3, R>(val p1: P1, val p2: P2, val p3: P3) :
        CacheResultCall<(P1, P2, P3) -> R, R> {
        override fun invoke(f: (P1, P2, P3) -> R) = f(p1, p2, p3)
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

    fun <P1, P2, P3, R> ((P1, P2, P3) -> R).cacheResult(
        cache: LruCache<CacheResultKey3<P1, P2, P3, R>, R>
    ): (P1, P2, P3) -> R {
        return object : (P1, P2, P3) -> R {
            private val handler =
                CacheResultHandler<((P1, P2, P3) -> R), CacheResultKey3<P1, P2, P3, R>, R>(
                    this@cacheResult,
                    cache
                )
            override fun invoke(p1: P1, p2: P2, p3: P3) = handler(CacheResultKey3(p1, p2, p3))
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

    private class CacheResultHandler<F, K : CacheResultCall<F, R>, R>(
        val f: F,
        val cache: LruCache<K, R>
    ) {
        operator fun invoke(k: K): R {
            synchronized(cache) {
                return cache[k] ?: run {
                    k(f).also {
                        cache.put(k, it)
                    }
                }
            }
        }
    }
}
