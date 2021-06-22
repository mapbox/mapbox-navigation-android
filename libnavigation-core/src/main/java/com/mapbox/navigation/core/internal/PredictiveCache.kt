package com.mapbox.navigation.core.internal

import com.mapbox.common.TileStore
import com.mapbox.navigation.base.options.PredictiveCacheLocationOptions
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigatorImpl
import com.mapbox.navigator.PredictiveCacheController

object PredictiveCache {

    private val cachedNavigationPredictiveCacheControllers =
        mutableListOf<PredictiveCacheController>()
    private val cachedMapsPredictiveCacheControllers =
        mutableMapOf<Any, MutableMap<String, PredictiveCacheController>>()

    private val navPredictiveCacheLocationOptions =
        mutableListOf<PredictiveCacheLocationOptions>()
    private val mapsPredictiveCacheLocationOptions =
        mutableMapOf<Any, MutableMap<String, Pair<TileStore, PredictiveCacheLocationOptions>>>()

    init {
        // recreate controllers with the same options but with a new navigator instance
        MapboxNativeNavigatorImpl.setNativeNavigatorRecreationObserver {
            cachedNavigationPredictiveCacheControllers.clear()
            cachedMapsPredictiveCacheControllers.clear()

            navPredictiveCacheLocationOptions.forEach {
                createNavigationController(it)
            }
            mapsPredictiveCacheLocationOptions.forEach { entry ->
                entry.value.forEach {
                    createMapsController(
                        mapboxMap = entry.key,
                        tileVariant = it.key,
                        tileStore = it.value.first,
                        predictiveCacheLocationOptions = it.value.second
                    )
                }
            }
        }
    }

    fun createNavigationController(
        predictiveCacheLocationOptions: PredictiveCacheLocationOptions
    ) {
        navPredictiveCacheLocationOptions.add(predictiveCacheLocationOptions)
        val predictiveCacheController =
            MapboxNativeNavigatorImpl.createNavigationPredictiveCacheController(
                predictiveCacheLocationOptions
            )
        cachedNavigationPredictiveCacheControllers.add(predictiveCacheController)
    }

    fun createMapsController(
        mapboxMap: Any,
        tileStore: TileStore,
        tileVariant: String,
        predictiveCacheLocationOptions: PredictiveCacheLocationOptions
    ) {
        val predictiveCacheController =
            MapboxNativeNavigatorImpl.createMapsPredictiveCacheController(
                tileStore,
                tileVariant,
                predictiveCacheLocationOptions
            )

        val cacheControllers = cachedMapsPredictiveCacheControllers[mapboxMap] ?: mutableMapOf()
        cacheControllers[tileVariant] = predictiveCacheController
        cachedMapsPredictiveCacheControllers[mapboxMap] = cacheControllers

        val locationOptions = mapsPredictiveCacheLocationOptions[mapboxMap] ?: mutableMapOf()
        locationOptions[tileVariant] = Pair(tileStore, predictiveCacheLocationOptions)
        mapsPredictiveCacheLocationOptions[mapboxMap] = locationOptions
    }

    fun currentMapsPredictiveCacheControllers(mapboxMap: Any): List<String> =
        cachedMapsPredictiveCacheControllers[mapboxMap]?.keys?.toList() ?: emptyList()

    fun removeAllMapControllers(mapboxMap: Any) {
        cachedMapsPredictiveCacheControllers.remove(mapboxMap)
        mapsPredictiveCacheLocationOptions.remove(mapboxMap)
    }

    fun removeMapControllers(mapboxMap: Any, tileVariant: String) {
        cachedMapsPredictiveCacheControllers[mapboxMap]?.let {
            it.remove(tileVariant)
            if (it.isEmpty()) {
                cachedMapsPredictiveCacheControllers.remove(mapboxMap)
            }
        }

        mapsPredictiveCacheLocationOptions[mapboxMap]?.let {
            it.remove(tileVariant)
            if (it.isEmpty()) {
                mapsPredictiveCacheLocationOptions.remove(mapboxMap)
            }
        }
    }

    fun clean() {
        cachedNavigationPredictiveCacheControllers.clear()
        cachedMapsPredictiveCacheControllers.clear()
        navPredictiveCacheLocationOptions.clear()
        mapsPredictiveCacheLocationOptions.clear()
    }
}
