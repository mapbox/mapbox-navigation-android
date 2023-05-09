package com.mapbox.navigation.instrumentation_tests.utils

import android.util.Log
import com.mapbox.common.TileRegionLoadOptions
import com.mapbox.geojson.Geometry
import com.mapbox.navigation.core.MapboxNavigation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class OfflineRegion(
    val id: String,
    val geometry: Geometry
)
suspend fun loadRegion(navigation: MapboxNavigation, region: OfflineRegion) {

    val navTilesetDescriptor = navigation.tilesetDescriptorFactory.getLatest()

    val tileRegionLoadOptions = TileRegionLoadOptions.Builder()
        .geometry(region.geometry)
        .descriptors(listOf(navTilesetDescriptor))
        .build()
    val tileStore = navigation.navigationOptions.routingTilesOptions.tileStore!!

    suspendCancellableCoroutine<Unit> { continuation ->
        tileStore.loadTileRegion(
            region.id,
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