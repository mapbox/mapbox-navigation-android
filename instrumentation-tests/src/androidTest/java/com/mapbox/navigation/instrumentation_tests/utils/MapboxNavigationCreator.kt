package com.mapbox.navigation.instrumentation_tests.utils

import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import com.mapbox.bindgen.Value
import com.mapbox.common.TileDataDomain
import com.mapbox.common.TileStore
import com.mapbox.common.TileStoreOptions
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.options.LongRoutesOptimisationOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.options.RoutingTilesOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.instrumentation_tests.utils.history.MapboxHistoryTestRule
import com.mapbox.navigation.testing.ui.BaseCoreNoCleanUpTest
import com.mapbox.navigation.testing.ui.utils.coroutines.stopRecording
import java.net.URI

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
suspend inline fun BaseCoreNoCleanUpTest.withMapboxNavigation(
    useRealTiles: Boolean = false,
    tileStore: TileStore? = null,
    historyRecorderRule: MapboxHistoryTestRule? = null, // TODO: copy features to new infra
    longRoutesOptimisationOptions: LongRoutesOptimisationOptions =
        LongRoutesOptimisationOptions.NoOptimisations,
    block: (navigation: MapboxNavigation) -> Unit
) {
    val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
    val navigation = MapboxNavigation(
        NavigationOptions.Builder(InstrumentationRegistry.getInstrumentation().targetContext)
            .accessToken(
                getMapboxAccessTokenFromResources(
                    targetContext
                )
            ).apply {
                val routingTilesOptions = RoutingTilesOptions.Builder()
                    .apply {
                        if (!useRealTiles) {
                            tilesBaseUri(URI(mockWebServerRule.baseUrl))
                        }
                    }
                    .tileStore(tileStore)
                    .build()
                routingTilesOptions(routingTilesOptions)
            }
            .longRoutesOptimisationOptions(longRoutesOptimisationOptions)
            .build()
    )
    historyRecorderRule?.historyRecorder = navigation.historyRecorder
    navigation.historyRecorder.startRecording()
    try {
        block(navigation)
    } finally {
        val path = navigation.historyRecorder.stopRecording()
        Log.i("Test history file", "history file recorder: $path")
        navigation.onDestroy()
    }
}

fun createTileStore(): TileStore {
    val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
    val tileStore = TileStore.create()
    tileStore.setOption(
        TileStoreOptions.MAPBOX_ACCESS_TOKEN,
        TileDataDomain.NAVIGATION,
        Value.valueOf(getMapboxAccessTokenFromResources(targetContext))
    )
    return tileStore
}
