package com.mapbox.navigation.utils.internal

import android.util.LruCache

object MemoizeUtils {
    private interface MemoizeCall<in F, out R> {
        operator fun invoke(func: F): R
    }

    // If you don't need the LRU cache then duplicate what is below using a ConcurrentHashMap
    private class MemoizeHandlerLimited<F, in K : MemoizeCall<F, R>, out R>(
        val func: F,
        cacheSize: Int
    ) {
        private val callMap = LruCache<K, R>(cacheSize)
        operator fun invoke(key: K): R {
            return callMap[key] ?: run {
                val result = key(func)
                synchronized(callMap) {
                    callMap.put(key, result)
                    result
                }
            }
        }
    }

    private data class MemoizeKey1<out P1, R>(val p1: P1) :
        MemoizeCall<(P1) -> R, R> {
        override fun invoke(func: (P1) -> R) = func(p1)
    }

    fun <P1, R> ((P1) -> R).memoize(cacheSize: Int): (P1) -> R {
        return object : (P1) -> R {
            private val handler = MemoizeHandlerLimited<((P1) -> R), MemoizeKey1<P1, R>, R>(
                this@memoize,
                cacheSize
            )

            override fun invoke(p1: P1) = handler(MemoizeKey1(p1))
        }
    }

    private data class MemoizeKey2<out P1, out P2, R>(val p1: P1, val p2: P2) :
        MemoizeCall<(P1, P2) -> R, R> {
        override fun invoke(func: (P1, P2) -> R) = func(p1, p2)
    }

    fun <P1, P2, R> ((P1, P2) -> R).memoize(cacheSize: Int): (P1, P2) -> R {
        return object : (P1, P2) -> R {
            private val handler = MemoizeHandlerLimited<((P1, P2) -> R), MemoizeKey2<P1, P2, R>, R>(
                this@memoize,
                cacheSize
            )

            override fun invoke(p1: P1, p2: P2) = handler(MemoizeKey2(p1, p2))
        }
    }
}
