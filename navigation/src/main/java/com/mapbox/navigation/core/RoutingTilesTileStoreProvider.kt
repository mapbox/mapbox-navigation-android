package com.mapbox.navigation.core

import com.mapbox.navigation.base.internal.tilestore.NavigationTileStoreOwner
import com.mapbox.navigation.base.options.RoutingTilesOptions

internal class RoutingTilesTileStoreProvider(
    private val routingTilesOptions: RoutingTilesOptions,
) : NavigationTileStoreOwner.Provider {
    override fun get() = routingTilesOptions.tileStore
}
