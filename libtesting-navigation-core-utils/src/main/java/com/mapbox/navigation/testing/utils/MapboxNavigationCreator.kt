@file:OptIn(ExperimentalMapboxNavigationAPI::class)

package com.mapbox.navigation.testing.utils

import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import com.mapbox.common.TileStore
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.internal.reroute.setRepeatRerouteAfterOffRouteDelaySeconds
import com.mapbox.navigation.base.options.DeviceProfile
import com.mapbox.navigation.base.options.DeviceType
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.options.RerouteDisabled
import com.mapbox.navigation.base.options.RerouteOptions
import com.mapbox.navigation.base.options.RerouteStrategyForMapMatchedRoutes
import com.mapbox.navigation.base.options.RoutingTilesOptions
import com.mapbox.navigation.base.route.RouteRefreshOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.testing.utils.history.MapboxHistoryTestRule
import com.mapbox.navigation.testing.ui.BaseCoreNoCleanUpTest
import com.mapbox.navigation.testing.ui.utils.coroutines.stopRecording
import java.net.URI

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
suspend inline fun BaseCoreNoCleanUpTest.withMapboxNavigation(
    useRealTiles: Boolean = false,
    tileStore: TileStore? = null,
    tilesVersion: String? = null,
    deviceType: DeviceType = DeviceType.HANDHELD,
    historyRecorderRule: MapboxHistoryTestRule? = null, // TODO: copy features to new infra
    customConfig: String? = null,
    routeRefreshOptions: RouteRefreshOptions? = null,
    nativeRouteObject: Boolean = false,
    rerouteStrategyForMapMatchedRoutes: RerouteStrategyForMapMatchedRoutes = RerouteDisabled,
    block: (navigation: MapboxNavigation) -> Unit
) {
    val navigation = MapboxNavigationProvider.create(
        NavigationOptions.Builder(
            InstrumentationRegistry.getInstrumentation().targetContext
        ).apply {
            val routingTilesOptions = RoutingTilesOptions.Builder()
                .apply {
                    if (!useRealTiles) {
                        tilesBaseUri(URI(mockWebServerRule.baseUrl))
                    }
                    if (tilesVersion != null) {
                        tilesVersion(tilesVersion)
                    }
                }
                .tileStore(tileStore)
                .build()
            routingTilesOptions(routingTilesOptions)
            customConfig?.let {
                deviceProfile(
                    DeviceProfile.Builder()
                        .customConfig(customConfig)
                        .deviceType(deviceType)
                        .build()
                )
            }
            nativeRouteObject(nativeRouteObject)
            rerouteOptions(
                RerouteOptions.Builder()
                    .rerouteStrategyForMapMatchedRoutes(rerouteStrategyForMapMatchedRoutes)
                    .setRepeatRerouteAfterOffRouteDelaySeconds(-1)
                    .build()
            )
            if (routeRefreshOptions != null) {
                routeRefreshOptions(routeRefreshOptions)
            }
        }
            .build()
    )
    historyRecorderRule?.historyRecorder = navigation.historyRecorder
    navigation.historyRecorder.startRecording()
    try {
        block(navigation)
    } finally {
        val path = navigation.historyRecorder.stopRecording()
        Log.i("Test history file", "history file recorder: $path")
        MapboxNavigationProvider.destroy()
    }
}

fun createTileStore(): TileStore {
    return TileStore.create()
}
