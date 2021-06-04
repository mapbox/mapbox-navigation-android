package com.mapbox.navigation.ui.maps.util

import android.util.LruCache

internal object CacheResultUtils {
    private interface CacheResultCall<in F, out R> {
        operator fun invoke(f: F): R
    }

    private data class CacheResultKey1<out P1, R>(val p1: P1) :
        CacheResultCall<(P1) -> R, R> {
        override fun invoke(f: (P1) -> R) = f(p1)
    }

    private data class CacheResultKey2<out P1, P2, R>(val p1: P1, val p2: P2) :
        CacheResultCall<(P1, P2) -> R, R> {
        override fun invoke(f: (P1, P2) -> R) = f(p1, p2)
    }

    fun <P1, R> ((P1) -> R).cacheResult(maxSize: Int): (P1) -> R {
        return object : (P1) -> R {
            private val handler =
                CacheResultHandler<((P1) -> R), CacheResultKey1<P1, R>, R>(
                    this@cacheResult,
                    maxSize
                )
            override fun invoke(p1: P1) = handler(CacheResultKey1(p1))
        }
    }

    fun <P1, P2, R> ((P1, P2) -> R).cacheResult(maxSize: Int): (P1, P2) -> R {
        return object : (P1, P2) -> R {
            private val handler =
                CacheResultHandler<((P1, P2) -> R), CacheResultKey2<P1, P2, R>, R>(
                    this@cacheResult,
                    maxSize
                )
            override fun invoke(p1: P1, p2: P2) = handler(CacheResultKey2(p1, p2))
        }
    }

    private class CacheResultHandler<F, in K : CacheResultCall<F, R>, out R>(
        val f: F,
        maxSize: Int
    ) {
        private val cache = LruCache<K, R>(maxSize)
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
