package com.mapbox.navigation.core.internal

import com.mapbox.common.TileStore
import com.mapbox.navigation.base.options.PredictiveCacheLocationOptions
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigatorImpl
import com.mapbox.navigator.PredictiveCacheController

object PredictiveCache {

    private val cachedNavigationPredictiveCacheControllers =
        mutableListOf<PredictiveCacheController>()
    private val cachedMapsPredictiveCacheControllers =
        mutableMapOf<String, PredictiveCacheController>()

    private val navPredictiveCacheLocationOptions =
        mutableListOf<PredictiveCacheLocationOptions>()
    private val mapsPredictiveCacheLocationOptions =
        mutableMapOf<String, Pair<TileStore, PredictiveCacheLocationOptions>>()

    init {
        // recreate controllers with the same options but with a new navigator instance
        MapboxNativeNavigatorImpl.setNativeNavigatorRecreationObserver {
            cachedNavigationPredictiveCacheControllers.clear()
            cachedMapsPredictiveCacheControllers.clear()

            navPredictiveCacheLocationOptions.forEach {
                createNavigationController(it)
            }
            mapsPredictiveCacheLocationOptions.forEach {
                createMapsController(
                    tileStore = it.value.first,
                    tileVariant = it.key,
                    predictiveCacheLocationOptions = it.value.second
                )
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
        tileStore: TileStore,
        tileVariant: String,
        predictiveCacheLocationOptions: PredictiveCacheLocationOptions =
            PredictiveCacheLocationOptions.Builder().build()
    ) {
        val predictiveCacheController =
            MapboxNativeNavigatorImpl.createMapsPredictiveCacheController(
                tileStore,
                tileVariant,
                predictiveCacheLocationOptions
            )
        cachedMapsPredictiveCacheControllers[tileVariant] = predictiveCacheController
        mapsPredictiveCacheLocationOptions[tileVariant] =
            Pair(tileStore, predictiveCacheLocationOptions)
    }

    fun currentMapsPredictiveCacheControllers(): List<String> =
        cachedMapsPredictiveCacheControllers.keys.toList()

    fun removeMapsController(tileVariant: String) {
        cachedMapsPredictiveCacheControllers.remove(tileVariant)
        mapsPredictiveCacheLocationOptions.remove(tileVariant)
    }

    fun clean() {
        cachedNavigationPredictiveCacheControllers.clear()
        cachedMapsPredictiveCacheControllers.clear()
        navPredictiveCacheLocationOptions.clear()
        mapsPredictiveCacheLocationOptions.clear()
    }
}
