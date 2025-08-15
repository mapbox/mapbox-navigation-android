package com.mapbox.navigation.ui.base.util.internal.resource

import androidx.annotation.RestrictTo
import com.mapbox.common.ReachabilityFactory
import com.mapbox.navigation.base.internal.tilestore.NavigationTileStoreOwner
import com.mapbox.navigation.ui.utils.internal.resource.DefaultResourceLoader
import com.mapbox.navigation.ui.utils.internal.resource.ResourceLoader

/**
 * Factory for accessing ResourceLoader instance.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
object ResourceLoaderFactory {

    private val sharedLoader: DefaultResourceLoader by lazy {
        DefaultResourceLoader(
            NavigationTileStoreOwner,
            ReachabilityFactory.reachability(null),
        )
    }

    /**
     * Returns default ResourceLoader.
     */
    fun getInstance(): ResourceLoader = sharedLoader
}
