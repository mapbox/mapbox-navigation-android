package com.mapbox.navigation.ui.shield.internal.loader

import android.util.LruCache
import com.mapbox.bindgen.Expected

/**
 * Resource Loader backed by LruCache
 */
internal class CachedResourceLoader<Argument, Resource>(
    cacheSize: Int,
    private val loader: ResourceLoader<Argument, Resource>
) : ResourceLoader<Argument, Resource> {

    private val cache = LruCache<Argument, Expected<String, Resource>>(cacheSize)

    override suspend fun load(argument: Argument): Expected<String, Resource> {
        var value = cache.get(argument)
        if (value != null) {
            return value
        }

        value = loader.load(argument)
        cache.put(argument, value)
        return value
    }
}
