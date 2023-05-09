package com.mapbox.navigation.instrumentation_tests.utils

import android.util.Log
import com.mapbox.common.TileRegionLoadOptions
import com.mapbox.geojson.Geometry
import com.mapbox.navigation.core.MapboxNavigation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun loadRegion(navigation: MapboxNavigation, geometry: Geometry) {

    val navTilesetDescriptor = navigation.tilesetDescriptorFactory.getLatest()

    val tileRegionLoadOptions = TileRegionLoadOptions.Builder()
        .geometry(geometry)
        .descriptors(listOf(navTilesetDescriptor))
        .build()
    val tileStore = navigation.navigationOptions.routingTilesOptions.tileStore!!

    suspendCancellableCoroutine<Unit> { continuation ->
        tileStore.loadTileRegion(
            "test",
            tileRegionLoadOptions,
            { progress ->
                Log.d("loadRegion", progress.toString())
            },
            { expected ->
                if (expected.isValue) {
                    continuation.resume(Unit)
                } else {
                    continuation.resumeWithException(Throwable(expected.error!!.message))
                }
            }
        )
    }

}