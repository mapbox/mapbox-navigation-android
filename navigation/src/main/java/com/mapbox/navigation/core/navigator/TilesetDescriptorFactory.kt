package com.mapbox.navigation.core.navigator

import com.mapbox.common.TilesetDescriptor
import com.mapbox.navigation.base.options.RoutingTilesOptions
import com.mapbox.navigator.CacheHandle

private typealias NativeTilesetDescriptorFactory = com.mapbox.navigator.TilesetDescriptorFactory

/**
 * A factory to build navigation [TilesetDescriptor]
 */
class TilesetDescriptorFactory internal constructor(
    private val routingTilesOptions: RoutingTilesOptions,
    private val cache: CacheHandle,
    private val nativeFactoryWrapper: NativeFactoryWrapper = NativeFactoryWrapperImpl(),
) {

    /**
     * Creates TilesetDescriptor using the specified dataset, profile and version.
     *
     * @param tilesDataset string built out of `<account>[.<graph>]` variables.
     * Account can be `mapbox` for default datasets or your username for other.
     * Graph can be left blank if you don't target any custom datasets.
     * If null [RoutingTilesOptions.tilesDataset] will be used.
     * @param tilesProfile profile of the dataset.
     * If null [RoutingTilesOptions.tilesProfile] will be used.
     * @param tilesVersion tiles version
     * If null [RoutingTilesOptions.tilesVersion] will be used.
     */
    @JvmOverloads
    fun build(
        tilesDataset: String? = null,
        tilesProfile: String? = null,
        tilesVersion: String? = null,
    ): TilesetDescriptor =
        nativeFactoryWrapper.build(
            combineDatasetWithProfile(tilesDataset, tilesProfile),
            tilesVersion ?: routingTilesOptions.tilesVersion,
        )

    /**
     * Gets TilesetDescriptor which corresponds to the currently used routing tiles dataset
     * and the specified `tilesVersion`.
     * @param tilesVersion TilesetDescriptor version
     */
    fun getSpecificVersion(tilesVersion: String): TilesetDescriptor =
        nativeFactoryWrapper.getSpecificVersion(cache, tilesVersion)

    /**
     * Gets TilesetDescriptor which corresponds to the latest available version of routing tiles.
     */
    fun getLatest(): TilesetDescriptor = nativeFactoryWrapper.getLatest(cache)

    private fun combineDatasetWithProfile(tilesDataset: String?, tilesProfile: String?): String {
        val dataset: String = tilesDataset ?: routingTilesOptions.tilesDataset
        val profile: String = tilesProfile ?: routingTilesOptions.tilesProfile

        return StringBuilder().apply {
            append(dataset)
            append("/")
            append(profile)
        }.toString()
    }

    // need the wrapper to avoid UnsatisfiedLinkError in unit tests
    internal interface NativeFactoryWrapper {
        fun getSpecificVersion(
            cache: CacheHandle,
            tilesVersion: String,
        ): TilesetDescriptor

        fun getLatest(cache: CacheHandle): TilesetDescriptor

        fun build(
            tilesDatasetAndProfile: String,
            tilesVersion: String,
        ): TilesetDescriptor
    }

    internal class NativeFactoryWrapperImpl : NativeFactoryWrapper {

        override fun getSpecificVersion(
            cache: CacheHandle,
            tilesVersion: String,
        ): TilesetDescriptor {
            return NativeTilesetDescriptorFactory.getSpecificVersion(
                cache,
                tilesVersion,
            )
        }

        override fun getLatest(cache: CacheHandle): TilesetDescriptor {
            return NativeTilesetDescriptorFactory.getLatest(cache)
        }

        override fun build(
            tilesDatasetAndProfile: String,
            tilesVersion: String,
        ): TilesetDescriptor {
            return NativeTilesetDescriptorFactory.build(
                tilesDatasetAndProfile,
                tilesVersion,
            )
        }
    }
}
