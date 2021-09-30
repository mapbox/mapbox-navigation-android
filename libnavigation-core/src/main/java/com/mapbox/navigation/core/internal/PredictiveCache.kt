package com.mapbox.navigation.core.internal

import com.mapbox.common.TileStore
import com.mapbox.common.TilesetDescriptor
import com.mapbox.navigation.base.options.PredictiveCacheLocationOptions
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigatorImpl
import com.mapbox.navigator.PredictiveCacheController

object PredictiveCache {

    internal val cachedNavigationPredictiveCacheControllers =
        mutableSetOf<PredictiveCacheController>()
    internal val cachedMapsPredictiveCacheControllers =
        mutableMapOf<Any, Pair<TilesetDescriptor, PredictiveCacheController>>()

    internal val navPredictiveCacheLocationOptions =
        mutableSetOf<PredictiveCacheLocationOptions>()
    internal val mapsPredictiveCacheLocationOptions =
        mutableMapOf<Any, Triple<TilesetDescriptor, TileStore, PredictiveCacheLocationOptions>>()

    fun init() {
        // recreate controllers with the same options but with a new navigator instance
        MapboxNativeNavigatorImpl.setNativeNavigatorRecreationObserver {
            val navOptions = navPredictiveCacheLocationOptions.toSet()
            val mapsOptions = mapsPredictiveCacheLocationOptions.toMap()

            clean()

            navOptions.forEach {
                createNavigationController(it)
            }
            mapsOptions.forEach { entry ->
                entry.value.let { (descriptor, tileStore, options) ->
                    createMapsController(
                        mapboxMap = entry.key,
                        tilesetDescriptor = descriptor,
                        tileStore = tileStore,
                        predictiveCacheLocationOptions = options
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
        tilesetDescriptor: TilesetDescriptor,
        predictiveCacheLocationOptions: PredictiveCacheLocationOptions
    ) {
        val predictiveCacheController =
            MapboxNativeNavigatorImpl.createMapsPredictiveCacheController(
                tileStore,
                tilesetDescriptor,
                predictiveCacheLocationOptions
            )

        cachedMapsPredictiveCacheControllers[mapboxMap] =
            Pair(tilesetDescriptor, predictiveCacheController)
        mapsPredictiveCacheLocationOptions[mapboxMap] =
            Triple(tilesetDescriptor, tileStore, predictiveCacheLocationOptions)
    }

    fun removeAllMapControllers(mapboxMap: Any) {
        cachedMapsPredictiveCacheControllers.remove(mapboxMap)
        mapsPredictiveCacheLocationOptions.remove(mapboxMap)
    }

    fun clean() {
        cachedNavigationPredictiveCacheControllers.clear()
        cachedMapsPredictiveCacheControllers.clear()
        navPredictiveCacheLocationOptions.clear()
        mapsPredictiveCacheLocationOptions.clear()
    }
}
