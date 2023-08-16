package com.mapbox.navigation.ui.maps

import androidx.annotation.UiThread
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.Value
import com.mapbox.common.TileStore
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.ResourceOptions
import com.mapbox.maps.TilesetDescriptorOptions
import com.mapbox.maps.plugin.delegates.listeners.OnStyleLoadedListener
import com.mapbox.navigation.base.options.PredictiveCacheLocationOptions
import com.mapbox.navigation.base.options.PredictiveCacheMapsOptions
import com.mapbox.navigation.base.options.PredictiveCacheNavigationOptions
import com.mapbox.navigation.base.options.PredictiveCacheOptions
import com.mapbox.navigation.base.options.RoutingTilesOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.PredictiveCache
import com.mapbox.navigation.ui.maps.internal.offline.OfflineManagerProvider
import com.mapbox.navigation.utils.internal.logE

private const val LOG_CATEGORY = "PredictiveCacheController"
private const val MAPBOX_URL_PREFIX = "mapbox://"
private const val VECTOR_SOURCE_TYPE = "vector"
private const val RASTER_SOURCE_TYPE = "raster"

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
 * @param predictiveCacheOptions [PredictiveCacheOptions] options to instantiate instance of [PredictiveCacheController]
 */
@UiThread
class PredictiveCacheController constructor(
    private val predictiveCacheOptions: PredictiveCacheOptions,
) {

    /**
     * Predictive Cache Controller errors listener
     */
    @Volatile
    var predictiveCacheControllerErrorHandler: PredictiveCacheControllerErrorHandler? = null

    private var mapListeners = mutableMapOf<MapboxMap, OnStyleLoadedListener>()

    /**
     * Constructor of [PredictiveCacheController]
     *
     * @param predictiveCacheLocationOptions [PredictiveCacheLocationOptions] location configuration for visual map predictive caching (optional)
     * @param predictiveCacheControllerErrorHandler [PredictiveCacheControllerErrorHandler] listener (optional)
     * Noting that `predictiveCacheLocationOptions` is used as `predictiveCacheGuidanceLocationOptions` when constructing `PredictiveCacheController` to retain backwards compatibility
     */
    @Deprecated(
        "Use PredictiveCacheController(PredictiveCacheOptions) and " +
            "PredictiveCacheController#predictiveCacheControllerErrorHandler instead"
    )
    constructor(
        predictiveCacheLocationOptions: PredictiveCacheLocationOptions =
            PredictiveCacheLocationOptions.Builder().build(),
        predictiveCacheControllerErrorHandler: PredictiveCacheControllerErrorHandler? =
            null,
    ) : this(
        predictiveCacheLocationOptions,
        predictiveCacheLocationOptions,
        predictiveCacheControllerErrorHandler
    )

    /**
     * Constructor of [PredictiveCacheController]
     *
     * @param predictiveCacheLocationOptions [PredictiveCacheLocationOptions] location configuration for visual map predictive caching (optional)
     * @param predictiveCacheGuidanceLocationOptions [PredictiveCacheLocationOptions] location configuration for guidance predictive caching (optional)
     * @param predictiveCacheControllerErrorHandler [PredictiveCacheControllerErrorHandler] listener (optional)
     */
    @JvmOverloads
    @Deprecated(
        "Use PredictiveCacheController(PredictiveCacheOptions) and " +
            "PredictiveCacheController#predictiveCacheControllerErrorHandler instead"
    )
    constructor(
        predictiveCacheLocationOptions: PredictiveCacheLocationOptions =
            PredictiveCacheLocationOptions.Builder().build(),
        predictiveCacheGuidanceLocationOptions: PredictiveCacheLocationOptions =
            PredictiveCacheLocationOptions.Builder().build(),
        predictiveCacheControllerErrorHandler: PredictiveCacheControllerErrorHandler? =
            null,
    ) : this(
        PredictiveCacheOptions.Builder().apply {
            predictiveCacheNavigationOptions(
                PredictiveCacheNavigationOptions.Builder().apply {
                    predictiveCacheLocationOptions(predictiveCacheGuidanceLocationOptions)
                }.build()
            )
            predictiveCacheMapsOptions(
                PredictiveCacheMapsOptions.Builder().apply {
                    predictiveCacheLocationOptions(predictiveCacheLocationOptions)
                }.build()
            )
        }.build(),
    ) {
        this.predictiveCacheControllerErrorHandler = predictiveCacheControllerErrorHandler
    }

    init {
        PredictiveCache.init()
        PredictiveCache.createNavigationController(
            predictiveCacheOptions.predictiveCacheNavigationOptions.predictiveCacheLocationOptions
        )
    }

    /**
     * Create cache controllers for a map instance.
     * Call when a new map instance is available.
     *
     * @param map an instance of [MapboxMap]
     * @param sourceIdsToCache a list of sources to cache.
     * Source id's should look like "mapbox://mapbox.satellite", "mapbox://mapbox.mapbox-terrain-v2".
     * The system only supports source hosted on Mapbox Services which URL starts with "mapbox://".
     * If no ids are passed all available style sources will be cached.
     *
     * Note: This method does not handle correct styles with volatile sources and caches them.
     * Use [createStyleMapControllers] instead.
     */
    @Deprecated(
        message = "Use createStyleMapControllers(map, styles) instead."
    )
    @JvmOverloads
    fun createMapControllers(
        map: MapboxMap,
        sourceIdsToCache: List<String> = emptyList()
    ) {
        val tileStore = map.getResourceOptions().tileStore
        if (tileStore == null) {
            handleError("TileStore instance not configured for the Map.")
            return
        }

        mapListeners[map]?.let {
            removeMapControllers(map)
        }

        traverseMapSources(map, sourceIdsToCache) { tileVariant ->
            PredictiveCache.createMapsController(
                map,
                tileStore,
                tileVariant,
                predictiveCacheOptions.predictiveCacheMapsOptions.predictiveCacheLocationOptions,
            )
        }

        val onStyleLoadedListener = OnStyleLoadedListener {
            val currentMapSources = mutableListOf<String>()
            traverseMapSources(map, sourceIdsToCache) { tileVariant ->
                currentMapSources.add(tileVariant)
            }
            updateMapsControllers(
                map,
                currentMapSources,
                PredictiveCache.currentMapsPredictiveCacheControllers(map),
                tileStore
            )
        }

        map.addOnStyleLoadedListener(onStyleLoadedListener)
        mapListeners[map] = onStyleLoadedListener
    }

    /**
     * Create cache controllers for a map instance.
     * Call when a new map instance is available.
     *
     * @param map an instance of [MapboxMap]
     * @param cacheCurrentMapStyle flag to indicate if the current map style should be cached
     * or not. Current map style is cached by default and shouldn't be added to the list of styles.
     * @param styles a list of Mapbox style URIs to cache.
     * If no styles are passed current map's style will be cached.
     * Only styles hosted on Mapbox Services are supported.
     * Only non-volatile styles will be cached.
     */
    @JvmOverloads
    fun createStyleMapControllers(
        map: MapboxMap,
        cacheCurrentMapStyle: Boolean = true,
        styles: List<String> = emptyList()
    ) {
        val tileStore = map.getResourceOptions().tileStore
        if (tileStore == null) {
            handleError("TileStore instance not configured for the Map.")
            return
        }

        mapListeners[map]?.let {
            removeMapControllers(map)
        }

        styles.forEach { styleURI ->
            createMapsController(styleURI, map, tileStore)
        }

        val onStyleLoadedListener = OnStyleLoadedListener {
            if (cacheCurrentMapStyle) {
                map.getStyle()?.styleURI?.let { styleURI ->
                    createMapsController(styleURI, map, tileStore)
                }
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
            if (map.isValid()) {
                map.removeOnStyleLoadedListener(it)
            }
            mapListeners.remove(map)
        }
        PredictiveCache.removeAllMapControllersFromTileVariants(map)
        PredictiveCache.removeAllMapControllersFromDescriptors(map)
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
        logE(error ?: "null", LOG_CATEGORY)
        predictiveCacheControllerErrorHandler?.onError(error)
    }

    private fun traverseMapSources(
        map: MapboxMap,
        sourceIdsToCache: List<String>,
        fn: (String) -> Unit
    ) {
        val sourceIds = sourceIdsToCache.ifEmpty {
            val filteredSources = map.getStyle()?.styleSources
                ?.filter { it.type == VECTOR_SOURCE_TYPE || it.type == RASTER_SOURCE_TYPE }
                ?: emptyList()

            filteredSources.map { it.id }
        }

        for (sourceId in sourceIds) {
            val properties: Expected<String, Value>? =
                map.getStyle()?.getStyleSourceProperties(sourceId)

            if (properties != null) {
                if (properties.isError) {
                    handleError(properties.error)
                } else {
                    val contentsDictionary = properties.value!!.contents as HashMap<String, Value>
                    val url = contentsDictionary["url"].toString()
                    if (!url.startsWith(MAPBOX_URL_PREFIX)) {
                        val message =
                            """
                                Source URL: "$url" does not start with "$MAPBOX_URL_PREFIX".
                                Only sources hosted on Mapbox Services are supported.
                            """.trimIndent()
                        handleError(message)
                        continue
                    } else {
                        fn(url.removePrefix(MAPBOX_URL_PREFIX))
                    }
                }
            }
        }
    }

    private fun updateMapsControllers(
        map: MapboxMap,
        currentMapSources: List<String>,
        attachedMapSources: List<String>,
        tileStore: TileStore,
    ) {
        attachedMapSources
            .filterNot { currentMapSources.contains(it) }
            .forEach { PredictiveCache.removeMapControllers(map, it) }

        currentMapSources
            .filterNot { attachedMapSources.contains(it) }
            .forEach {
                PredictiveCache.createMapsController(
                    map,
                    tileStore,
                    it,
                    predictiveCacheOptions.predictiveCacheMapsOptions
                        .predictiveCacheLocationOptions,
                )
            }
    }

    private fun createMapsController(
        styleURI: String,
        map: MapboxMap,
        tileStore: TileStore
    ) {
        val offlineManager = OfflineManagerProvider.provideOfflineManager(map.getResourceOptions())

        if (!styleURI.startsWith(MAPBOX_URL_PREFIX)) {
            val message =
                """
                    Style URI: "$styleURI" does not start with "$MAPBOX_URL_PREFIX".
                    Only styles hosted on Mapbox Services are supported.
                """.trimIndent()
            handleError(message)
        } else {
            val descriptorsToOptions =
                predictiveCacheOptions.predictiveCacheMapsOptionsList.map { options ->
                    val descriptorOptions = TilesetDescriptorOptions.Builder()
                        .styleURI(styleURI)
                        .minZoom(options.minZoom)
                        .maxZoom(options.maxZoom)
                        .build()

                    val tilesetDescriptor =
                        offlineManager.createTilesetDescriptor(descriptorOptions)
                    tilesetDescriptor to options.predictiveCacheLocationOptions
                }

            PredictiveCache.createMapsControllers(
                map,
                tileStore,
                descriptorsToOptions
            )
        }
    }
}
