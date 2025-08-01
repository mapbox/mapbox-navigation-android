package com.mapbox.navigation.core.internal

import com.mapbox.common.TileStore
import com.mapbox.common.TilesetDescriptor
import com.mapbox.navigation.base.options.PredictiveCacheLocationOptions
import com.mapbox.navigation.base.options.PredictiveCacheNavigationOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigator.PredictiveCacheController

class PredictiveCache(private val mapboxNavigation: MapboxNavigation) {

    internal val cachedNavigationPredictiveCacheControllers =
        mutableSetOf<PredictiveCacheController>()
    internal val cachedMapsPredictiveCacheControllers =
        mutableMapOf<Any, MutableMap<PredictiveCacheControllerKey, PredictiveCacheController>>()
    internal val cachedSearchPredictiveCacheControllers =
        mutableSetOf<Pair<TilesetDescriptor, PredictiveCacheController>>()

    internal val navPredictiveCacheOptions = mutableSetOf<PredictiveCacheNavigationOptions>()
    internal val searchPredictiveCacheLocationOptions = mutableSetOf<
        Pair<TileStore, List<Pair<TilesetDescriptor, PredictiveCacheLocationOptions>>>,>()

    init {
        // recreate controllers with the same options but with a new navigator instance
        mapboxNavigation.navigator.setNativeNavigatorRecreationObserver {
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
                controllersMap[key] =
                    mapboxNavigation.navigator.createMapsPredictiveCacheController(
                        key.tileStore,
                        key.tilesetDescriptor,
                        key.locationOptions,
                    )
            }
        }
    }

    fun createSearchControllers(
        tileStore: TileStore,
        searchDescriptorsAndOptions:
            List<Pair<TilesetDescriptor, PredictiveCacheLocationOptions>>,
    ) {
        val searchDescriptorsToPredictiveCacheControllers =
            searchDescriptorsAndOptions.map {
                it.first to mapboxNavigation.navigator.createSearchPredictiveCacheController(
                    tileStore,
                    it.first,
                    it.second,
                )
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
        val locationOptions: PredictiveCacheLocationOptions,
    )
}
