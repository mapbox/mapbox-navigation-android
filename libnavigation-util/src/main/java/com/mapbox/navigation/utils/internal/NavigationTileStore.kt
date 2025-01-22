package com.mapbox.navigation.utils.internal

import com.mapbox.common.TileStore

interface NavigationTileStore {

    operator fun invoke(): TileStore
}
