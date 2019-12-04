package com.mapbox.navigation.route.onboard

import com.mapbox.annotation.navigation.module.MapboxNavigationModule
import com.mapbox.annotation.navigation.module.MapboxNavigationModuleType
import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.base.route.model.RouteOptionsNavigation
import com.mapbox.navigation.navigator.MapboxNativeNavigator
import com.mapbox.navigation.navigator.MapboxNativeNavigatorImpl
import com.mapbox.navigation.route.onboard.model.Config
import com.mapbox.navigation.route.onboard.model.mapToRouteConfig
import java.io.File

@MapboxNavigationModule(MapboxNavigationModuleType.OnboardRouter, skipConfiguration = true)
class MapboxOnboardRouter : Router {

    companion object {
        private const val TILE_PATH_NAME = "tiles"
    }

    private val navigatorNative: MapboxNativeNavigator
    private val accessToken: String
    private val config: Config

    /**
     * Creates an offline router which uses the specified offline path for storing and retrieving
     * data.
     *
     * @param accessToken mapbox access token
     * @param config offline config
     */
    constructor(accessToken: String, config: Config) {
        val tileDir = File(config.tilePath, TILE_PATH_NAME)
        if (!tileDir.exists()) {
            tileDir.mkdirs()
        }

        this.navigatorNative = MapboxNativeNavigatorImpl
        this.accessToken = accessToken
        this.config = config
        MapboxNativeNavigatorImpl.configureRoute(config.mapToRouteConfig())
    }

    // Package private for testing purposes
    internal constructor(
        navigator: MapboxNativeNavigator,
        accessToken: String,
        config: Config
    ) {
        this.navigatorNative = navigator
        this.accessToken = accessToken
        this.config = config
    }

    override fun getRoute(
        routeOptions: RouteOptionsNavigation,
        callback: Router.Callback
    ) {
        // val offlineRouter = OfflineRoute.builder(
        //     RouteUrl(
        //         accessToken = accessToken,
        //         orgin = origin,
        //         waypoints = waypoints,
        //         destination = destination
        //     )
        // ).build()
        //
        // OfflineRouteRetrievalTask(navigatorNative, object : OnOfflineRouteFoundCallback {
        //     override fun onRouteFound(routes: List<Route>) {
        //         callback.onRouteReady(routes)
        //     }
        //
        //     override fun onError(error: OfflineError) {
        //         callback.onFailure(NavigationException(error.message))
        //     }
        // })
        //     .execute(offlineRouter.buildUrl())
    }

    override fun cancel() = Unit
}
