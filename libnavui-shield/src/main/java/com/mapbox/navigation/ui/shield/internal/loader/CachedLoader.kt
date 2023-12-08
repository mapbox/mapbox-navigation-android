package com.mapbox.navigation.ui.shield.internal.loader

import android.util.LruCache
import com.mapbox.bindgen.Expected

/**
 * Loader backed by LruCache
 */
internal class CachedLoader<Input, Output>(
    cacheSize: Int,
    private val loader: Loader<Input, Output>
) : Loader<Input, Output> {

    private val cache = LruCache<Input, Expected<Error, Output>>(cacheSize)

    override suspend fun load(input: Input): Expected<Error, Output> {
        var value = cache.get(input)
        if (value != null) {
            return value
        }

        value = loader.load(input)
        cache.put(input, value)
        return value
    }
}
