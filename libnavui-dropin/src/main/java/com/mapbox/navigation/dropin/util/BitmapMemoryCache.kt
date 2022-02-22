package com.mapbox.navigation.dropin.util

import android.graphics.Bitmap
import android.util.LruCache

/**
 * Bitmap memory cache backed by LruCache.
 *
 * @param cacheSize Cache size in BYTES.
 */
class BitmapMemoryCache(val cacheSize: Int) {

    private val memoryCache: LruCache<String, Bitmap>

    init {
        memoryCache = object : LruCache<String, Bitmap>(cacheSize) {
            override fun sizeOf(key: String, bitmap: Bitmap): Int {
                return bitmap.byteCount
            }
        }
    }

    fun add(key: String, bitmap: Bitmap) {
        memoryCache.put(key, bitmap)
    }

    fun get(key: String): Bitmap? {
        return try {
            memoryCache.get(key)
        } catch (ex: Exception) {
            null
        }
    }

    companion object {
        const val MB_IN_BYTES = 1024 * 1024
    }
}
