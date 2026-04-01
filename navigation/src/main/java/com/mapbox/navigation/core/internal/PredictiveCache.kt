package com.mapbox.navigation.core.internal

import com.mapbox.common.TileStore
import com.mapbox.common.TilesetDescriptor
import com.mapbox.navigation.base.options.PredictiveCacheLocationOptions
import com.mapbox.navigation.base.options.PredictiveCacheMapsOptions
import com.mapbox.navigation.base.options.PredictiveCacheNavigationOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.utils.internal.logD
import com.mapbox.navigation.utils.internal.logW
import com.mapbox.navigator.PredictiveCacheControllerInterface

class PredictiveCache(private val mapboxNavigation: MapboxNavigation) {

    internal val cachedNavigationPredictiveCacheControllers =
        mutableSetOf<PredictiveCacheControllerInterface>()
    internal val cachedMapsPredictiveCacheControllers = mutableMapOf<
        Any, MutableMap<PredictiveCacheControllerKey, PredictiveCacheControllerInterface>,
        >()
    internal val cachedSearchPredictiveCacheControllers = mutableSetOf<
        Pair<TilesetDescriptor, PredictiveCacheControllerInterface>,
        >()

    internal val navPredictiveCacheOptions = mutableSetOf<PredictiveCacheNavigationOptions>()
    internal val searchPredictiveCacheLocationOptions = mutableSetOf<
        Pair<TileStore, List<Pair<TilesetDescriptor, PredictiveCacheLocationOptions>>>,
        >()

    init {
        // recreate controllers with the same options but with a new navigator instance
        mapboxNavigation.navigator.addNativeNavigatorRecreationObserver {
            val navOptions = navPredictiveCacheOptions.toSet()
            val mapControllersData = cachedMapsPredictiveCacheControllers
                .map { (mapboxMap, predictiveCacheControllersMap) ->
                    mapboxMap to predictiveCacheControllersMap.keys.toList()
                }

            val searchOptions =
                searchPredictiveCacheLocationOptions.toMap()

            clean()

            navOptions.forEach {
                createNavigationController(it)
            }

            mapControllersData.forEach { (mapboxMap, predictiveCacheControllerKeys) ->
                createMapsControllers(
                    mapboxMap,
                    predictiveCacheControllerKeys,
                )
            }

            searchOptions.forEach {
                it.let { (tileStore, searchDescriptorsAndOptions) ->
                    createSearchControllers(
                        tileStore = tileStore,
                        searchDescriptorsAndOptions = searchDescriptorsAndOptions,
                    )
                }
            }
        }
    }

    fun createNavigationController(navOptions: PredictiveCacheNavigationOptions) {
        navPredictiveCacheOptions.add(navOptions)
        val controllers = mapboxNavigation.navigator.createNavigationPredictiveCacheController(
            navOptions,
        )
        cachedNavigationPredictiveCacheControllers.addAll(controllers)
    }

    fun createMapsControllers(
        mapboxMap: Any,
        predictiveCacheControllerKeys: List<PredictiveCacheControllerKey>,
    ) {
        val controllersMap = cachedMapsPredictiveCacheControllers.getOrPut(mapboxMap) {
            mutableMapOf()
        }

        predictiveCacheControllerKeys.forEach { key ->
            if (!controllersMap.containsKey(key)) {
                val controller = mapboxNavigation.navigator.createMapsPredictiveCacheController(
                    key.tileStore,
                    key.tilesetDescriptor,
                    key.options.predictiveCacheLocationOptions,
                )

                if (controller != null) {
                    controllersMap[key] = controller
                } else {
                    logW("Unable to create predictive cache controller for $key", LOG_CATEGORY)
                }
            } else {
                logD(LOG_CATEGORY) {
                    "Skip creating predictive cache controller for map: $key"
                }
            }
        }
    }

    fun createSearchControllers(
        tileStore: TileStore,
        searchDescriptorsAndOptions:
            List<Pair<TilesetDescriptor, PredictiveCacheLocationOptions>>,
    ) {
        val searchDescriptorsToPredictiveCacheControllers =
            searchDescriptorsAndOptions.mapNotNull {
                val controller = mapboxNavigation.navigator.createSearchPredictiveCacheController(
                    tileStore,
                    it.first,
                    it.second,
                )

                if (controller != null) {
                    it.first to controller
                } else {
                    logW("Unable to create predictive cache controller for $it", LOG_CATEGORY)
                    null
                }
            }

        searchDescriptorsToPredictiveCacheControllers.forEach {
            cachedSearchPredictiveCacheControllers.add(it)
        }
        searchPredictiveCacheLocationOptions.add(
            tileStore to searchDescriptorsAndOptions,
        )
    }

    fun removeAllMapControllersFromDescriptors(mapboxMap: Any) {
        cachedMapsPredictiveCacheControllers.remove(mapboxMap)
    }

    fun clean() {
        cachedNavigationPredictiveCacheControllers.clear()
        cachedMapsPredictiveCacheControllers.clear()
        cachedSearchPredictiveCacheControllers.clear()
        navPredictiveCacheOptions.clear()
        searchPredictiveCacheLocationOptions.clear()
    }

    data class PredictiveCacheControllerKey(
        val styleUri: String,
        val tileStore: TileStore,
        val tilesetDescriptor: TilesetDescriptor,
        val options: PredictiveCacheMapsOptions,
    ) {

        // TODO(NAVAND-7127) skipping tile store in equals/hashCode can potentially lead to bugs
        //  if TileStore's have different paths.
        //  We should ensure that we work with the same instance of TileStore
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as PredictiveCacheControllerKey

            if (styleUri != other.styleUri) return false
            if (options != other.options) return false

            return true
        }

        override fun hashCode(): Int {
            var result = styleUri.hashCode()
            result = 31 * result + options.hashCode()
            return result
        }
    }

    private companion object {
        const val LOG_CATEGORY = "PredictiveCache"
    }
}
