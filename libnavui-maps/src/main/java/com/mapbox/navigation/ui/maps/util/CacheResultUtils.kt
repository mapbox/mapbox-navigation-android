package com.mapbox.navigation.ui.maps.util

import android.util.LruCache
import com.mapbox.api.directions.v5.models.DirectionsRoute

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

    data class CacheResultKeyRoute<P1, R>(val p1: DirectionsRoute, val p2: P1) :
        CacheResultCall<(DirectionsRoute, P1) -> R, R> {

        override fun invoke(f: (DirectionsRoute, P1) -> R) = f(p1, p2)
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as CacheResultKeyRoute<*, *>

            if (p1.requestUuid() != other.p1.requestUuid()) return false
            if (p2 != other.p2) return false

            return true
        }

        override fun hashCode(): Int {
            var result = p1.requestUuid().hashCode()
            result = 31 * result + (p2?.hashCode() ?: 0)
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

    fun <P2, R> ((DirectionsRoute, P2) -> R).cacheRouteResult(
        cache: LruCache<CacheResultKeyRoute<P2, R>, R>
    ): (DirectionsRoute, P2) -> R {
        return object : (DirectionsRoute, P2) -> R {
            private val handler =
                CacheResultHandler<((DirectionsRoute, P2) -> R), CacheResultKeyRoute<P2, R>, R>(
                    this@cacheRouteResult,
                    cache
                )
            override fun invoke(p1: DirectionsRoute, p2: P2) = handler(CacheResultKeyRoute(p1, p2))
        }
    }

    private class CacheResultHandler<F, K : CacheResultCall<F, R>, R>(
        val f: F,
        val cache: LruCache<K, R>
    ) {
        operator fun invoke(k: K): R {
            synchronized(cache) {
                return cache[k] ?: run {
                    val r = k(f)
                    if (cache.get(k) == null) {
                        cache.put(k, r)
                    }
                    r
                }
            }
        }
    }
}
