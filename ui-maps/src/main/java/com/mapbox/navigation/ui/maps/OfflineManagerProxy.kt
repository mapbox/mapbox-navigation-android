package com.mapbox.navigation.ui.maps

import com.mapbox.common.TilesetDescriptor
import com.mapbox.maps.OfflineManager
import com.mapbox.maps.TilesetDescriptorOptions

internal class OfflineManagerProxy(
    private val offlineManager: OfflineManager,
) {

    fun createTilesetDescriptor(
        tilesetDescriptorOptions: TilesetDescriptorOptions,
    ): TilesetDescriptor {
        return offlineManager.createTilesetDescriptor(tilesetDescriptorOptions)
    }
}
