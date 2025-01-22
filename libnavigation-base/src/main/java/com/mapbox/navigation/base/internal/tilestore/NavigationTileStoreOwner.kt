package com.mapbox.navigation.base.internal.tilestore

import com.mapbox.common.TileStore
import com.mapbox.navigation.utils.internal.NavigationTileStore
import com.mapbox.navigation.utils.internal.logD

/**
 * Keep [TileStore] for navigation related components.
 * If no [TileStore] is set, a default one will be created.
 * This [TileStore] is not used by Maps by default, so to have only single instance of a
 * [TileStore] please set it in [MapsOptions]
 */
object NavigationTileStoreOwner : NavigationTileStore {

    private var value: TileStore? = null

    private fun createDefault(): TileStore {
        logD(TAG, "No initial TileStore set. Creating default. Accessing too early?")
        return TileStore.create()
    }

    fun init(provider: Provider) {
        return synchronized(this) {
            value = provider.get() ?: createDefault()
        }
    }

    override operator fun invoke(): TileStore {
        return synchronized(this) {
            value ?: createDefault().also {
                value = it
            }
        }
    }

    private const val TAG = "NavigationTileStore"

    interface Provider {
        fun get(): TileStore?
    }
}
