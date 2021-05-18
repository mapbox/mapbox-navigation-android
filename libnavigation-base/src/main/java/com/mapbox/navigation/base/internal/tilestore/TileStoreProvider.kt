package com.mapbox.navigation.base.internal.tilestore

import com.mapbox.common.TileStore

object TileStoreProvider {

    fun getDefaultTileStoreInstance() = TileStore.getInstance()

    fun getTileStoreInstance(path: String) = TileStore.getInstance(path)
}
