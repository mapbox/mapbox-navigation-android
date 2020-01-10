package com.mapbox.navigation.route.onboard

import com.mapbox.annotation.navigation.module.MapboxNavigationModule
import com.mapbox.annotation.navigation.module.MapboxNavigationModuleType
import com.mapbox.navigation.base.logger.Logger
import com.mapbox.navigation.base.route.RouteUrl
import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.base.route.model.Route
import com.mapbox.navigation.base.route.model.RouteOptionsNavigation
import com.mapbox.navigation.navigator.MapboxNativeNavigator
import com.mapbox.navigation.navigator.MapboxNativeNavigatorImpl
import com.mapbox.navigation.route.onboard.model.Config
import com.mapbox.navigation.route.onboard.model.OfflineError
import com.mapbox.navigation.route.onboard.model.mapToRouteConfig
import com.mapbox.navigation.route.onboard.task.OfflineRouteRetrievalTask
import com.mapbox.navigation.utils.exceptions.NavigationException
import java.io.File

@MapboxNavigationModule(MapboxNavigationModuleType.OnboardRouter, skipConfiguration = true)
class MapboxOnboardRouter : Router {

    companion object {
        private const val TILES_DIR_NAME = "tiles"
    }

    private val navigatorNative: MapboxNativeNavigator
    private val config: Config
    private val logger: Logger?

    /**
     * Creates an offline router which uses the specified offline path for storing and retrieving
     * data.
     *
     * @param config offline config
     */
    constructor(config: Config, logger: Logger?) {
        val tileDir = File(config.tilePath, TILES_DIR_NAME)
        if (!tileDir.exists()) {
            tileDir.mkdirs()
        }

        this.navigatorNative = MapboxNativeNavigatorImpl
        this.config = config
        this.logger = logger
        MapboxNativeNavigatorImpl.configureRouter(config.mapToRouteConfig())
    }

    // Package private for testing purposes
    internal constructor(
        navigator: MapboxNativeNavigator,
        config: Config
    ) {
        this.navigatorNative = navigator
        this.config = config
        this.logger = null
    }

    override fun getRoute(
        routeOptions: RouteOptionsNavigation,
        callback: Router.Callback
    ) {
        val offlineRouter = OfflineRoute.builder(
            RouteUrl(
                accessToken = routeOptions.accessToken ?: "",
                user = routeOptions.user,
                profile = routeOptions.profile,
                orgin = routeOptions.origin.point,
                waypoints = routeOptions.waypoints.map { it.point },
                destination = routeOptions.destination.point,
                steps = routeOptions.steps,
                voiceIntruction = routeOptions.voiceInstructions,
                bannerIntruction = routeOptions.bannerInstructions,
                roundaboutExits = routeOptions.roundaboutExits
            )
        ).build()

        OfflineRouteRetrievalTask(navigatorNative, logger, object : OnOfflineRouteFoundCallback {
            override fun onRouteFound(routes: List<Route>) {
                callback.onResponse(routes)
            }

            override fun onError(error: OfflineError) {
                callback.onFailure(NavigationException(error.message))
            }
        }).execute(offlineRouter.buildUrl())
    }

    override fun cancel() = Unit
}
