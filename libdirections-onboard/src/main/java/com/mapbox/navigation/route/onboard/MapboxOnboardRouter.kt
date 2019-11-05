package com.mapbox.navigation.route.onboard

import com.mapbox.annotation.navigation.module.MapboxNavigationModule
import com.mapbox.annotation.navigation.module.MapboxNavigationModuleType
import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.navigator.MapboxNativeNavigator
import com.mapbox.navigation.navigator.MapboxNativeNavigatorImpl
import com.mapbox.navigation.route.onboard.task.OfflineRouteRetrievalTask
import java.io.File

@MapboxNavigationModule(MapboxNavigationModuleType.OnboardRouter, skipConfiguration = true)
class MapboxOnboardRouter(private val navigator: MapboxNativeNavigator) : Router {

    companion object {

        private const val TILE_PATH_NAME = "tiles"
    }

    private val tilePath: String
    private val offlineTileVersions: OfflineTileVersions
    private val navigatorNative: MapboxNativeNavigator

    /**
     * Creates an offline router which uses the specified offline path for storing and retrieving
     * data.
     *
     * @param offlinePath directory path where the offline data is located
     */
    constructor(offlinePath: String) {
        val tileDir = File(offlinePath, TILE_PATH_NAME)
        if (!tileDir.exists()) {
            tileDir.mkdirs()
        }

        this.tilePath = tileDir.absolutePath
        offlineTileVersions = OfflineTileVersions()
        this.navigatorNative = MapboxNativeNavigatorImpl
    }

    // Package private for testing purposes
    internal constructor(
        tilePath: String,
        offlineTileVersions: OfflineTileVersions,
        navigator: MapboxNativeNavigator
    ) {
        this.tilePath = tilePath
        this.offlineTileVersions = offlineTileVersions
        this.navigatorNative = navigator
    }

    override fun getRoute(
        routeOptions: RouteOptionsNavigation,
        callback: Router.Callback
    ) = Unit

    override fun cancel() = Unit

    class Config {
        fun compile(): String = TODO("not implemented")
    }
}
