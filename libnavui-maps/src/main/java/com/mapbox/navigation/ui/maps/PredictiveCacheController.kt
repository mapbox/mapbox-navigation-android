package com.mapbox.navigation.ui.maps

import android.util.Log
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.Value
import com.mapbox.common.TileStore
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.plugin.delegates.listeners.OnStyleLoadedListener
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.PredictiveCache
import java.util.HashMap

private const val TAG = "MbxPredictiveCache"
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
 * In order to start caching map data, provide an instance via [setMapInstance].
 * At the moment, there can only be one instance of the map caching resources at a time.
 * The controller as well as [MapboxNavigation] instance it's holding can have
 * a different lifecycle than the [MapboxMap] instance, so make sure to call [removeMapInstance]
 * whenever the [MapView] is destroyed to avoid leaking references or downloading unnecessary
 * resources. When the map instance is recreated, set it back with [setMapInstance].
 *
 * The system only supports map styles that do not use source compositing,
 * make sure to disable source compositing in Mapbox Studio.
 * See https://docs.mapbox.com/studio-manual/reference/styles/#source-compositing
 * for more information.
 *
 * The system only supports source hosted on Mapbox Services which URL starts with "mapbox://".
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
 * @param navigation [MapboxNavigation] instance
 * @param predictiveCacheControllerErrorHandler [PredictiveCacheControllerErrorHandler] listener (optional)
 */
class PredictiveCacheController @JvmOverloads constructor(
    private val navigation: MapboxNavigation,
    private val predictiveCacheControllerErrorHandler: PredictiveCacheControllerErrorHandler? = null
) {
    private var map: MapboxMap? = null

    private val onStyleLoadedListener = object : OnStyleLoadedListener {
        override fun onStyleLoaded() {
            map?.let { map ->
                val tileStorePath = map.getResourceOptions().tileStorePath
                val tileStore = retrieveTileStore(tileStorePath)
                val currentMapSources = mutableListOf<String>()
                traverseMapSources(map) { tileVariant ->
                    currentMapSources.add(tileVariant)
                }
                updateMapsControllers(
                    currentMapSources,
                    PredictiveCache.currentMapsPredictiveCacheControllers(),
                    tileStore
                )
            }
        }
    }

    init {
        PredictiveCache.createNavigationController(
            navigation.navigationOptions.predictiveCacheLocationOptions
        )
    }

    /**
     * Call when a new map instance is available. Only one map instance at a time is supported.
     */
    fun setMapInstance(map: MapboxMap) {
        removeMapInstance()
        val tileStorePath = map.getResourceOptions().tileStorePath
        val tileStore = retrieveTileStore(tileStorePath)
        traverseMapSources(map) { tileVariant ->
            PredictiveCache.createMapsController(tileStore, tileVariant)
        }
        map.addOnStyleLoadedListener(onStyleLoadedListener)
        this.map = map
    }

    /**
     * Remove the map instance. Call this whenever the [MapView] is destroyed
     * to avoid leaking references or downloading unnecessary resources.
     */
    fun removeMapInstance() {
        map?.removeOnStyleLoadedListener(onStyleLoadedListener)
        PredictiveCache.currentMapsPredictiveCacheControllers().forEach { tileVariant ->
            PredictiveCache.removeMapsController(tileVariant)
        }
        this.map = null
    }

    /**
     * Cleans up all map and navigation state related references.
     * This can be called when navigation session finishes
     * and predictive caching is not needed anymore.
     */
    fun onDestroy() {
        removeMapInstance()
        PredictiveCache.clean()
    }

    private fun retrieveTileStore(path: String?): TileStore {
        return if (path == null) {
            TileStore.getInstance()
        } else {
            TileStore.getInstance(path)
        }
    }

    private fun traverseMapSources(map: MapboxMap, fn: (String) -> Unit) {
        val filteredSources = map.getStyle()?.styleSources
            ?.filter { it.type == VECTOR_SOURCE_TYPE || it.type == RASTER_SOURCE_TYPE }
            ?: emptyList()

        for (source in filteredSources) {
            val properties: Expected<Value, String>? = map.getStyle()?.getStyleSourceProperties(
                source.id
            )
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
        currentMapSources: List<String>,
        attachedMapSources: List<String>,
        tileStore: TileStore
    ) {
        attachedMapSources
            .filterNot { currentMapSources.contains(it) }
            .forEach { PredictiveCache.removeMapsController(it) }

        currentMapSources
            .filterNot { attachedMapSources.contains(it) }
            .forEach { PredictiveCache.createMapsController(tileStore, it) }
    }

    private fun handleError(error: String?) {
        Log.e(TAG, error)
        predictiveCacheControllerErrorHandler?.onError(error)
    }
}
