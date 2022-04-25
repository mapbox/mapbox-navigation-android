package com.mapbox.navigation.ui.utils.resource

import com.mapbox.common.TileStore

/**
 * Factory for accessing ResourceLoader instance.
 */
object ResourceLoaderFactory {

    private val sharedLoader: DefaultResourceLoader by lazy {
        DefaultResourceLoader(TileStore.create())
    }

    /**
     * Returns default ResourceLoader.
     */
    fun getInstance(): ResourceLoader = sharedLoader
}
