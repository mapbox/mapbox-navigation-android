package com.mapbox.navigation.instrumentation_tests.utils.tiles

import android.util.Log
import com.mapbox.common.TileRegionLoadOptions
import com.mapbox.geojson.Geometry
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.instrumentation_tests.utils.createTileStore
import com.mapbox.navigation.instrumentation_tests.utils.history.MapboxHistoryTestRule
import com.mapbox.navigation.instrumentation_tests.utils.withMapboxNavigation
import com.mapbox.navigation.testing.ui.BaseCoreNoCleanUpTest
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assume.assumeTrue
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

const val TILES_LOADING_TIMEOUT = 40_000L

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

suspend inline fun BaseCoreNoCleanUpTest.withMapboxNavigationAndOfflineTilesForRegion(
    region: OfflineRegion,
    historyRecorderRule: MapboxHistoryTestRule? = null,
    block: (MapboxNavigation) -> Unit
) {
    withMapboxNavigation(
        useRealTiles = true, // TODO: use mocked tiles instead of real NAVAND-1351
        tileStore = createTileStore(),
        historyRecorderRule = historyRecorderRule
    ) { navigation ->
        val loadSuccessful = withTimeoutOrNull(TILES_LOADING_TIMEOUT) {
            loadRegion(navigation, region)
            true
        } ?: false
        assumeTrue(
            "Wasn't able to load ${region.id} in $TILES_LOADING_TIMEOUT ms",
            loadSuccessful
        )
        block(navigation)
    }
}
