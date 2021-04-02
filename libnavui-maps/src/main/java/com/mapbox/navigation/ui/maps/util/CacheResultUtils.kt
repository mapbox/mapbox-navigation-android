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
