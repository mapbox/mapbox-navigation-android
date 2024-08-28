package com.mapbox.navigation.core.internal

import com.mapbox.common.TileStore
import com.mapbox.common.TilesetDescriptor
import com.mapbox.navigation.base.options.PredictiveCacheLocationOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigator.PredictiveCacheController

class PredictiveCache(private val mapboxNavigation: MapboxNavigation) {

    internal val cachedNavigationPredictiveCacheControllers =
        mutableSetOf<PredictiveCacheController>()
    internal val cachedMapsPredictiveCacheControllers =
        mutableMapOf<Any, List<Pair<TilesetDescriptor, PredictiveCacheController>>>()
    internal val cachedMapsPredictiveCacheControllersTileVariant =
        mutableMapOf<Any, MutableMap<String, PredictiveCacheController>>()

    internal val navPredictiveCacheLocationOptions =
        mutableSetOf<PredictiveCacheLocationOptions>()
    internal val mapsPredictiveCacheLocationOptions = mutableMapOf<Any,
        Pair<TileStore, List<Pair<TilesetDescriptor, PredictiveCacheLocationOptions>>>>()
    internal val mapsPredictiveCacheLocationOptionsTileVariant =
        mutableMapOf<Any, MutableMap<String, Pair<TileStore, PredictiveCacheLocationOptions>>>()

    init {
        // recreate controllers with the same options but with a new navigator instance
        mapboxNavigation.navigator.setNativeNavigatorRecreationObserver {
            val navOptions = navPredictiveCacheLocationOptions.toSet()
            val mapsOptions = mapsPredictiveCacheLocationOptions.toMap()
            val mapsOptionsTileVariant = mapsPredictiveCacheLocationOptionsTileVariant.toMap()

            clean()

            navOptions.forEach {
                createNavigationController(it)
            }
            mapsOptions.forEach { entry ->
                entry.value.let { (tileStore, descriptorsAndOptions) ->
                    createMapsControllers(
                        mapboxMap = entry.key,
                        tileStore = tileStore,
                        descriptorsAndOptions = descriptorsAndOptions
                    )
                }
            }
            mapsOptionsTileVariant.forEach { entry ->
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
            mapboxNavigation.navigator.createNavigationPredictiveCacheController(
                predictiveCacheLocationOptions
            )
        cachedNavigationPredictiveCacheControllers.add(predictiveCacheController)
    }

    @Deprecated(
        "Use createMapsControllers(mapboxMap, tileStore, descriptorsAndOptions) instead."
    )
    fun createMapsController(
        mapboxMap: Any,
        tileStore: TileStore,
        tileVariant: String,
        predictiveCacheLocationOptions: PredictiveCacheLocationOptions
    ) {
        val predictiveCacheController =
            mapboxNavigation.navigator.createMapsPredictiveCacheControllerTileVariant(
                tileStore,
                tileVariant,
                predictiveCacheLocationOptions
            )

        val cacheControllers =
            cachedMapsPredictiveCacheControllersTileVariant[mapboxMap] ?: mutableMapOf()
        cacheControllers[tileVariant] = predictiveCacheController
        cachedMapsPredictiveCacheControllersTileVariant[mapboxMap] = cacheControllers

        val locationOptions =
            mapsPredictiveCacheLocationOptionsTileVariant[mapboxMap] ?: mutableMapOf()
        locationOptions[tileVariant] = Pair(tileStore, predictiveCacheLocationOptions)
        mapsPredictiveCacheLocationOptionsTileVariant[mapboxMap] = locationOptions
    }

    fun createMapsControllers(
        mapboxMap: Any,
        tileStore: TileStore,
        descriptorsAndOptions: List<Pair<TilesetDescriptor, PredictiveCacheLocationOptions>>
    ) {
        val descriptorsToPredictiveCacheControllers = descriptorsAndOptions.map {
            it.first to mapboxNavigation.navigator.createMapsPredictiveCacheController(
                tileStore,
                it.first,
                it.second
            )
        }

        cachedMapsPredictiveCacheControllers[mapboxMap] = descriptorsToPredictiveCacheControllers
        mapsPredictiveCacheLocationOptions[mapboxMap] = tileStore to descriptorsAndOptions
    }

    @Deprecated("Will be removed with other TileVariant logic")
    fun currentMapsPredictiveCacheControllers(mapboxMap: Any): List<String> =
        cachedMapsPredictiveCacheControllersTileVariant[mapboxMap]?.keys?.toList() ?: emptyList()

    @Deprecated("Will be removed with other TileVariant logic")
    fun removeAllMapControllersFromTileVariants(mapboxMap: Any) {
        cachedMapsPredictiveCacheControllersTileVariant.remove(mapboxMap)
        mapsPredictiveCacheLocationOptionsTileVariant.remove(mapboxMap)
    }

    fun removeAllMapControllersFromDescriptors(mapboxMap: Any) {
        cachedMapsPredictiveCacheControllers.remove(mapboxMap)
        mapsPredictiveCacheLocationOptions.remove(mapboxMap)
    }

    @Deprecated("Will be removed with other TileVariant logic")
    fun removeMapControllers(mapboxMap: Any, tileVariant: String) {
        cachedMapsPredictiveCacheControllersTileVariant[mapboxMap]?.let {
            it.remove(tileVariant)
            if (it.isEmpty()) {
                cachedMapsPredictiveCacheControllersTileVariant.remove(mapboxMap)
            }
        }

        mapsPredictiveCacheLocationOptionsTileVariant[mapboxMap]?.let {
            it.remove(tileVariant)
            if (it.isEmpty()) {
                mapsPredictiveCacheLocationOptionsTileVariant.remove(mapboxMap)
            }
        }
    }

    fun clean() {
        cachedNavigationPredictiveCacheControllers.clear()
        cachedMapsPredictiveCacheControllers.clear()
        cachedMapsPredictiveCacheControllersTileVariant.clear()
        navPredictiveCacheLocationOptions.clear()
        mapsPredictiveCacheLocationOptions.clear()
        mapsPredictiveCacheLocationOptionsTileVariant.clear()
    }
}
