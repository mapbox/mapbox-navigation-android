package com.mapbox.navigation.core

import com.mapbox.common.TileStore
import com.mapbox.navigation.base.options.RoutingTilesOptions

/**
 *  An interface which enables listening to navigation tiles version switch.
 *  Navigator might be switched to a fallback tiles version when there are no enough tiles of the
 *  current version to navigate on. It might happen when network is not available and tiles can't
 *  be loaded. When connection is restored, navigator will switch back to the target version
 *  specified in [RoutingTilesOptions] (the latest available version if no version is specified).
 *  To create additional fallback tiles versions, use the [TileStore] to create and download
 *  offline regions. Otherwise, fallback candidates will only be the tiles versions from the ambient
 *  cache.
 */
interface NavigationVersionSwitchObserver {

    /**
     * Invoked as soon as navigation switched to a fallback tiles version.
     *
     * @param tilesVersion tiles version used for navigation.
     */
    fun onSwitchToFallbackVersion(tilesVersion: String?)

    /**
     * Invoked as soon as navigation switched to a tiles version specified in [RoutingTilesOptions]
     * (the latest available version if no version is specified).
     *
     * @param tilesVersion tiles version used for navigation.
     */
    fun onSwitchToTargetVersion(tilesVersion: String?)
}
