package com.mapbox.navigation.ui.maps

import androidx.annotation.UiThread
import androidx.annotation.VisibleForTesting
import com.mapbox.common.Cancelable
import com.mapbox.common.MapboxOptions
import com.mapbox.common.TileStore
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.MapboxMapsOptions
import com.mapbox.maps.StyleLoadedCallback
import com.mapbox.maps.TilesetDescriptorOptions
import com.mapbox.maps.mapsOptions
import com.mapbox.navigation.base.options.PredictiveCacheMapsOptions
import com.mapbox.navigation.base.options.PredictiveCacheOptions
import com.mapbox.navigation.base.options.RoutingTilesOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.internal.PredictiveCache
import com.mapbox.navigation.utils.internal.logE
import com.mapbox.navigation.utils.internal.logW

private const val LOG_CATEGORY = "PredictiveCacheController"
private const val MAPBOX_URL_PREFIX = "mapbox://"

/**
 * Predictive caching is a system that downloads necessary visual map and guidance data resources
 * along the route upfront, before they are needed, in an attempt to provide a smooth experience
 * even when connectivity is lost while using the app.
 *
 * Once instantiated, the controller will immediately start caching guidance and search data.
 *
 * In order to start caching map data, provide an instance via [createStyleMapControllers].
 * To specify styles to cache, pass a list of Mapbox style URIs via [createStyleMapControllers].
 * Source id's should look like "mapbox://mapbox.satellite", "mapbox://mapbox.mapbox-terrain-v2".
 * The system only supports source hosted on Mapbox Services which URL starts with "mapbox://".
 * If no ids are passed all available style sources will be cached.
 *
 * The controller as well as [MapboxNavigation] instance it's holding can have
 * a different lifecycle than the [MapboxMap] instance, so make sure to call [removeMapControllers]
 * whenever the [MapView] is destroyed to avoid leaking references or downloading unnecessary
 * resources. When the map instance is recreated, set it back with [createStyleMapControllers].
 *
 * Also, note that [MapboxNavigation] instance should be created before [PredictiveCacheController]
 * and the lifecycle of the [MapboxNavigation] instance should be longer than that of the
 * [PredictiveCacheController]. Specifically, [MapboxNavigation] should not be destroyed before
 * [PredictiveCacheController.onDestroy] is called.
 *
 * The map instance has to be configured with the same [TileStore] instance that was provided to [RoutingTilesOptions.tileStore].
 * You need to call [TileStore.create] with a path and pass it to [MapboxMapsOptions.tileStore] or use the Maps SDK's tile store path XML attribute.
 *
 * Call [onDestroy] to cleanup all map, navigation and search state related references.
 * This can be called when navigation session finishes and predictive caching is not needed anymore.
 *
 * - When migrating please ensure you have cleaned up old navigation tiles cache folder to reclaim disk space.
 * Navigation SDK 2.0 caches navigation tiles in a default folder under `APP_FOLDER/mbx_nav/tiles/api.mapbox.com`.
 * Previous versions of Nav SDK used to cache tiles under a default folder `APP_FOLDER/Offline/api.mapbox.com/$tilesVersion/tiles`.
 * Old cache is not compatible with a new version of SDK 2.0.
 * It makes sense to delete any folders used previously for caching including a default one.
 * - `OnboardRouterOptions` enabled you to specify a path where nav-tiles will be saved and if a
 * custom directory was used, it should be cleared as well.
 */
@UiThread
class PredictiveCacheController @VisibleForTesting internal constructor(
    private val predictiveCacheOptions: PredictiveCacheOptions,
    private val predictiveCache: PredictiveCache,
) {

    /**
     * Predictive Cache Controller errors listener
     */
    @Volatile
    var predictiveCacheControllerErrorHandler: PredictiveCacheControllerErrorHandler? = null

    private var mapSubscriptions = mutableMapOf<MapboxMap, Cancelable>()

    /**
     * Constructs a new instance of the [PredictiveCacheController] using the provided
     * [MapboxNavigation] and [PredictiveCacheOptions].
     *
     * **Note:** The lifecycle of the [MapboxNavigation] instance should be longer than that of the
     * [PredictiveCacheController]. Specifically, [MapboxNavigation] should not be destroyed before
     * [PredictiveCacheController.onDestroy] is called.
     *
     * @param mapboxNavigation [MapboxNavigation] object which will be used as a source of active route
     * @param predictiveCacheOptions [PredictiveCacheOptions] options to instantiate instance of [PredictiveCacheController]
     */
    constructor(
        mapboxNavigation: MapboxNavigation,
        predictiveCacheOptions: PredictiveCacheOptions,
    ) : this(predictiveCacheOptions, PredictiveCache(mapboxNavigation))

    /**
     * Constructs a new instance of the [PredictiveCacheController] using the provided
     * [PredictiveCacheOptions]. Throws [IllegalStateException] if [MapboxNavigation]
     * was not instantiated before. Use [PredictiveCacheController] constructor which explicitly
     * accepts [MapboxNavigation] instance.
     *
     * @param predictiveCacheOptions [PredictiveCacheOptions] options to instantiate instance of [PredictiveCacheController]
     * @throws IllegalStateException if [MapboxNavigation] was not instantiated before
     */
    @Deprecated(
        "This constructor is deprecated",
        ReplaceWith("PredictiveCacheController(MapboxNavigation, PredictiveCacheOptions)"),
    )
    constructor(
        predictiveCacheOptions: PredictiveCacheOptions,
    ) : this(
        predictiveCacheOptions,
        PredictiveCache(tryToRetrieveMapboxNavigation()),
    )

    init {
        predictiveCache.createNavigationController(
            predictiveCacheOptions.predictiveCacheNavigationOptions,
        )
        createSearchControllers()
    }

    /**
     * Create Maps cache controllers for a map instance.
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
        styles: List<String> = emptyList(),
    ) {
        val tileStore = MapboxOptions.mapsOptions.tileStore
        if (tileStore == null) {
            handleError("TileStore instance not configured for the Map.")
            return
        }

        mapSubscriptions[map]?.let {
            logW(LOG_CATEGORY) {
                "MapboxMap instance = $map is already used. " +
                    "All predictive cache controllers for this instance will be recreated"
            }
            removeMapControllers(map)
        }

        styles.forEach { styleURI ->
            createMapsController(styleURI, map, tileStore)
        }

        val styleLoadedCallback = StyleLoadedCallback {
            if (cacheCurrentMapStyle) {
                map.style?.styleURI?.let { styleURI ->
                    createMapsController(styleURI, map, tileStore, null)
                }
            }
        }

        val subscription = map.subscribeStyleLoaded(styleLoadedCallback)
        mapSubscriptions[map] = subscription
    }

    internal fun createStyleMapControllers(
        map: MapboxMap,
        styles: List<String>,
        predictiveCacheMapOptions: List<PredictiveCacheMapsOptions>,
    ) {
        val tileStore = MapboxOptions.mapsOptions.tileStore
        if (tileStore == null) {
            handleError("TileStore instance not configured for the Map.")
            return
        }

        styles.forEach { styleUri ->
            createMapsController(styleUri, map, tileStore, predictiveCacheMapOptions)
        }
    }

    /**
     * Remove the map instance for Maps data. Call this whenever the [MapView] is destroyed
     * to avoid leaking references or downloading unnecessary resources.
     */
    fun removeMapControllers(map: MapboxMap) {
        mapSubscriptions[map]?.let {
            it.cancel()
            mapSubscriptions.remove(map)
        }
        predictiveCache.removeAllMapControllersFromDescriptors(map)
    }

    /**
     * Cleans up all map and navigation state related references.
     * This can be called when navigation session finishes
     * and predictive caching is not needed anymore.
     */
    fun onDestroy() {
        mapSubscriptions.forEach {
            it.value.cancel()
        }
        mapSubscriptions.clear()
        predictiveCache.clean()
    }

    private fun handleError(error: String?) {
        logE(error ?: "null", LOG_CATEGORY)
        predictiveCacheControllerErrorHandler?.onError(error)
    }

    private fun createMapsController(
        styleURI: String,
        map: MapboxMap,
        tileStore: TileStore,
        predictiveCacheMapOptions: List<PredictiveCacheMapsOptions>? = null,
    ) {
        val offlineManager = OfflineManagerProvider.provideOfflineManager()

        if (!styleURI.startsWith(MAPBOX_URL_PREFIX)) {
            val message =
                """
                    Style URI: "$styleURI" does not start with "$MAPBOX_URL_PREFIX".
                    Only styles hosted on Mapbox Services are supported.
                """.trimIndent()
            handleError(message)
        } else {
            val predictiveCacheControllerKeys =
                (
                    predictiveCacheMapOptions
                        ?: predictiveCacheOptions.predictiveCacheMapsOptionsList
                    ).map { options ->
                    val descriptorOptions = TilesetDescriptorOptions.Builder()
                        .styleURI(styleURI)
                        .minZoom(options.minZoom)
                        .maxZoom(options.maxZoom)
                        .extraOptions(options.extraOptions)
                        .build()

                    val tilesetDescriptor =
                        offlineManager.createTilesetDescriptor(descriptorOptions)

                    PredictiveCache.PredictiveCacheControllerKey(
                        styleURI,
                        tileStore,
                        tilesetDescriptor,
                        options.predictiveCacheLocationOptions,
                    )
                }

            predictiveCache.createMapsControllers(map, predictiveCacheControllerKeys)
        }
    }

    private fun createSearchControllers() {
        val tileStore = MapboxOptions.mapsOptions.tileStore
        if (tileStore == null) {
            handleError("TileStore instance not configured for the Map.")
            return
        }

        val searchOption = predictiveCacheOptions.predictiveCacheSearchOptionsList
        if (searchOption == null) {
            handleError("Search options are null")
            return
        }

        val descriptorsToOptions = searchOption.map { options ->
            options.searchTilesetDescriptor to options.predictiveCacheLocationOptions
        }

        predictiveCache.createSearchControllers(
            tileStore,
            descriptorsToOptions,
        )
    }

    private companion object {
        fun tryToRetrieveMapboxNavigation(): MapboxNavigation {
            return if (MapboxNavigationProvider.isCreated()) {
                MapboxNavigationProvider.retrieve()
            } else {
                error("Instantiate MapboxNavigation first")
            }
        }
    }
}
