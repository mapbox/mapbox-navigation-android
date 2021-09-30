package com.mapbox.navigation.ui.maps

import com.mapbox.base.common.logger.model.Message
import com.mapbox.base.common.logger.model.Tag
import com.mapbox.common.TileStore
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.OfflineManager
import com.mapbox.maps.ResourceOptions
import com.mapbox.maps.TilesetDescriptorOptions
import com.mapbox.maps.plugin.delegates.listeners.OnStyleLoadedListener
import com.mapbox.navigation.base.options.PredictiveCacheLocationOptions
import com.mapbox.navigation.base.options.RoutingTilesOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.PredictiveCache
import com.mapbox.navigation.utils.internal.LoggerProvider

private const val TAG = "MbxPredictiveCache"
private const val MIN_ZOOM = 0.toByte()
private const val MAX_ZOOM = 16.toByte()

/**
 * Predictive caching is a system that downloads necessary visual map and guidance data resources
 * along the route upfront, before they are needed, in an attempt to provide a smooth experience
 * even when connectivity is lost while using the app.
 *
 * Once instantiated, the controller will immediately start caching guidance data.
 *
 * In order to start caching map data, provide an instance via [createMapControllers].
 * To specify sources to cache, pass a list of id's via [createMapControllers].
 * Source id's should look like "mapbox://mapbox.satellite", "mapbox://mapbox.mapbox-terrain-v2".
 * The system only supports source hosted on Mapbox Services which URL starts with "mapbox://".
 * If no ids are passed all available style sources will be cached.
 *
 * The controller as well as [MapboxNavigation] instance it's holding can have
 * a different lifecycle than the [MapboxMap] instance, so make sure to call [removeMapControllers]
 * whenever the [MapView] is destroyed to avoid leaking references or downloading unnecessary
 * resources. When the map instance is recreated, set it back with [createMapControllers].
 *
 * The map instance has to be configured with the same [TileStore] instance that was provided to [RoutingTilesOptions.tileStore].
 * You need to call [TileStore.create] with a path and pass it to [ResourceOptions.tileStore] or use the Maps SDK's tile store path XML attribute.
 *
 * Call [onDestroy] to cleanup all map and navigation state related references.
 * This can be called when navigation session finishes and predictive caching is not needed anymore.
 *
 * - When migrating please ensure you have cleaned up old navigation tiles cache folder to reclaim disk space.
 * Navigation SDK 2.0 caches navigation tiles in a default folder under `APP_FOLDER/mbx_nav/tiles/api.mapbox.com`.
 * Previous versions of Nav SDK used to cache tiles under a default folder `APP_FOLDER/Offline/api.mapbox.com/$tilesVersion/tiles`.
 * Old cache is not compatible with a new version of SDK 2.0.
 * It makes sense to delete any folders used previously for caching including a default one.
 * - `OnboardRouterOptions` enabled you to specify a path where nav-tiles will be saved and if a
 * custom directory was used, it should be cleared as well.
 *
 * @param predictiveCacheLocationOptions [PredictiveCacheLocationOptions] location configuration for navigation predictive caching
 * @param predictiveCacheControllerErrorHandler [PredictiveCacheControllerErrorHandler] listener (optional)
 */
class PredictiveCacheController @JvmOverloads constructor(
    private val predictiveCacheLocationOptions: PredictiveCacheLocationOptions =
        PredictiveCacheLocationOptions.Builder().build(),
    private val predictiveCacheControllerErrorHandler: PredictiveCacheControllerErrorHandler? = null
) {
    private var mapListeners = mutableMapOf<MapboxMap, OnStyleLoadedListener>()

    init {
        PredictiveCache.init()
        PredictiveCache.createNavigationController(predictiveCacheLocationOptions)
    }

    /**
     * Create cache controllers for a map instance.
     * Call when a new map instance is available.
     *
     * @param map an instance of [MapboxMap]
     * Source id's should look like "mapbox://mapbox.satellite", "mapbox://mapbox.mapbox-terrain-v2".
     * The system only supports source hosted on Mapbox Services which URL starts with "mapbox://".
     * If no ids are passed all available style sources will be cached.
     */
    fun createMapControllers(map: MapboxMap) {
        val tileStore = map.getResourceOptions().tileStore
        if (tileStore == null) {
            handleError("TileStore instance not configured for the Map.")
            return
        }

        val offlineManager = OfflineManager(map.getResourceOptions())

        mapListeners[map]?.let {
            removeMapControllers(map)
        }

        val onStyleLoadedListener = OnStyleLoadedListener {
            map.getStyle()?.styleURI?.let { styleURI ->
                val descriptorOptions = TilesetDescriptorOptions.Builder()
                    .styleURI(styleURI)
                    .minZoom(MIN_ZOOM)
                    .maxZoom(MAX_ZOOM)
                    .build()

                val tilesetDescriptor = offlineManager.createTilesetDescriptor(descriptorOptions)

                PredictiveCache.createMapsController(
                    map,
                    tileStore,
                    tilesetDescriptor,
                    predictiveCacheLocationOptions
                )
            }
        }

        map.addOnStyleLoadedListener(onStyleLoadedListener)
        mapListeners[map] = onStyleLoadedListener
    }

    /**
     * Remove the map instance. Call this whenever the [MapView] is destroyed
     * to avoid leaking references or downloading unnecessary resources.
     */
    fun removeMapControllers(map: MapboxMap) {
        mapListeners[map]?.let {
            map.removeOnStyleLoadedListener(it)
            mapListeners.remove(map)
        }
        PredictiveCache.removeAllMapControllers(map)
    }

    /**
     * Cleans up all map and navigation state related references.
     * This can be called when navigation session finishes
     * and predictive caching is not needed anymore.
     */
    fun onDestroy() {
        mapListeners.forEach {
            it.key.removeOnStyleLoadedListener(it.value)
        }
        mapListeners.clear()
        PredictiveCache.clean()
    }

    private fun handleError(error: String?) {
        LoggerProvider.logger.e(Tag(TAG), Message(error ?: "null"))
        predictiveCacheControllerErrorHandler?.onError(error)
    }
}
