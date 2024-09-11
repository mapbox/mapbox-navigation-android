package com.mapbox.navigation.ui.utils.internal.resource

import com.mapbox.common.ReachabilityFactory
import com.mapbox.common.TileStore

/**
 * Factory for accessing ResourceLoader instance.
 */
object ResourceLoaderFactory {

    private val sharedLoader: DefaultResourceLoader by lazy {
        DefaultResourceLoader(
            TileStore.create(),
            ReachabilityFactory.reachability(null),
        )
    }

    /**
     * Returns default ResourceLoader.
     */
    fun getInstance(): ResourceLoader = sharedLoader
}
