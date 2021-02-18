package com.mapbox.navigation.core.internal

import com.mapbox.common.TileStore
import com.mapbox.navigation.base.options.PredictiveCacheLocationOptions
import com.mapbox.navigation.base.options.RoutingTilesOptions
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigatorImpl
import com.mapbox.navigator.PredictiveCacheController

object PredictiveCache {

    private var cachedNavigationPredictiveCacheControllers =
        mutableListOf<PredictiveCacheController>()
    private var cachedMapsPredictiveCacheControllers =
        mutableMapOf<String, PredictiveCacheController>()

    fun createNavigationController(
        routingTilesOptions: RoutingTilesOptions,
        predictiveCacheLocationOptions: PredictiveCacheLocationOptions
    ) {
        val predictiveCacheController =
            MapboxNativeNavigatorImpl.createNavigationPredictiveCacheController(
                routingTilesOptions,
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
    }

    fun currentMapsPredictiveCacheControllers(): List<String> =
        cachedMapsPredictiveCacheControllers.keys.toList()

    fun removeMapsController(
        tileVariant: String,
    ) {
        cachedMapsPredictiveCacheControllers.remove(tileVariant)
    }

    fun clean() {
        cachedNavigationPredictiveCacheControllers = mutableListOf()
        cachedMapsPredictiveCacheControllers = mutableMapOf()
    }
}
