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
    private val nativeFactoryWrapper: NativeFactoryWrapper = NativeFactoryWrapperImpl()
) {

    /**
     * Creates TilesetDescriptor using the specified dataset and version.
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
        tilesVersion: String? = null
    ): TilesetDescriptor =
        nativeFactoryWrapper.build(
            combineDatasetWithProfile(tilesDataset, tilesProfile),
            tilesVersion ?: routingTilesOptions.tilesVersion
        )

    /**
     * Creates TilesetDescriptor using the specified dataset and latest locally available version.
     *
     * @param tilesDataset string built out of `<account>[.<graph>]` variables.
     * Account can be `mapbox` for default datasets or your username for other.
     * Graph can be left blank if you don't target any custom datasets.
     * If null [RoutingTilesOptions.tilesDataset] will be used.
     * @param tilesProfile profile of the dataset.
     * If null [RoutingTilesOptions.tilesProfile] will be used.
     */
    @JvmOverloads
    fun buildLatestLocal(
        tilesDataset: String? = null,
        tilesProfile: String? = null
    ): TilesetDescriptor =
        nativeFactoryWrapper.buildLatestLocal(
            cache,
            combineDatasetWithProfile(tilesDataset, tilesProfile)
        )

    /**
     * Creates TilesetDescriptor using the specified dataset and latest version retrieved from
     * the server.
     *
     * @param tilesDataset string built out of `<account>[.<graph>]` variables.
     * Account can be `mapbox` for default datasets or your username for other.
     * Graph can be left blank if you don't target any custom datasets.
     * If null [RoutingTilesOptions.tilesDataset] will be used.
     * @param tilesProfile profile of the dataset.
     * If null [RoutingTilesOptions.tilesProfile] will be used.
     */
    @JvmOverloads
    fun buildLatestServer(
        tilesDataset: String? = null,
        tilesProfile: String? = null
    ): TilesetDescriptor =
        nativeFactoryWrapper.buildLatestServer(
            cache,
            combineDatasetWithProfile(tilesDataset, tilesProfile)
        )

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
        fun build(tilesDatasetAndProfile: String, tilesVersion: String): TilesetDescriptor
        fun buildLatestLocal(cache: CacheHandle, tilesDatasetAndProfile: String): TilesetDescriptor
        fun buildLatestServer(cache: CacheHandle, tilesDatasetAndProfile: String): TilesetDescriptor
    }

    internal class NativeFactoryWrapperImpl : NativeFactoryWrapper {
        override fun build(
            tilesDatasetAndProfile: String,
            tilesVersion: String
        ): TilesetDescriptor {
            return NativeTilesetDescriptorFactory.build(tilesDatasetAndProfile, tilesVersion)
        }

        override fun buildLatestLocal(
            cache: CacheHandle,
            tilesDatasetAndProfile: String
        ): TilesetDescriptor {
            return NativeTilesetDescriptorFactory.buildLatestLocal(cache, tilesDatasetAndProfile)
        }

        override fun buildLatestServer(
            cache: CacheHandle,
            tilesDatasetAndProfile: String
        ): TilesetDescriptor {
            return NativeTilesetDescriptorFactory.buildLatestServer(cache, tilesDatasetAndProfile)
        }
    }
}
